package ar.com.facturacion.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.com.facturacion.application.port.in.PurgarFacturasVencidasUseCase;
import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;

/** De punta a punta: HTTP → casos de uso → Postgres + S3 reales (Testcontainers). */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class FacturaApiTest {

    private static final String BUCKET = "facturacion-test";

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));

    @Container
    static LocalStackContainer localstack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:4.14"));

    @DynamicPropertySource
    static void s3(DynamicPropertyRegistry registry) throws Exception {
        localstack.execInContainer("awslocal", "s3", "mb", "s3://" + BUCKET);
        registry.add("app.bucket", () -> BUCKET);
        registry.add("app.purge-interval", () -> "PT1H");
        registry.add("app.s3.endpoint", () -> localstack.getEndpoint().toString());
        registry.add("app.s3.region", localstack::getRegion);
        registry.add("app.s3.access-key", localstack::getAccessKey);
        registry.add("app.s3.secret-key", localstack::getSecretKey);
        registry.add("app.s3.path-style", () -> "true");
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired S3Client s3;
    @Autowired PurgarFacturasVencidasUseCase purga;

    @Test
    void emiteConsultaYDescargaElPdf() throws Exception {
        String vendedor = cuitAleatorio();
        String comprador = cuitAleatorio();

        var alta = mvc.perform(post("/api/facturas").contentType(MediaType.APPLICATION_JSON)
                        .content(factura(vendedor, comprador, "ARS", """
                                [{"nombre": "Tornillos", "cantidad": 3, "precio": 10.10},
                                 {"nombre": "Tuercas", "cantidad": 2, "precio": 0.05}]
                                """, ", \"total\": 999999")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/facturas/")))
                .andExpect(jsonPath("$.total").value(30.40))   // el total que manda el cliente se ignora
                .andExpect(jsonPath("$.moneda").value("ARS"))
                .andExpect(jsonPath("$.expiresAt").exists())
                .andReturn();
        String id = JsonPath.read(alta.getResponse().getContentAsString(), "$.id");

        mvc.perform(get("/api/facturas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendedor.cuit").value(vendedor))
                .andExpect(jsonPath("$.comprador.nombre").value("Comprador " + comprador))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].subtotal").value(30.30));

        mvc.perform(get("/api/facturas").param("cuitVendedor", vendedor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItem(id)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0));

        mvc.perform(get("/api/facturas").param("cuitComprador", comprador).param("cuitVendedor", vendedor))
                .andExpect(jsonPath("$.totalElements").value(1));

        String url = mvc.perform(get("/api/facturas/{id}/pdf", id))
                .andExpect(status().isFound())
                .andReturn().getResponse().getHeader("Location");

        try (var http = HttpClient.newHttpClient()) {
            var pdf = http.send(HttpRequest.newBuilder(URI.create(url)).build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            assertThat(pdf.statusCode()).isEqualTo(200);
            assertThat(new String(pdf.body(), 0, 5)).isEqualTo("%PDF-");
            assertThat(pdf.headers().firstValue("Content-Disposition"))
                    .hasValue("attachment; filename=\"factura-" + id + ".pdf\"");
        }
    }

    @Test
    void elNombreDeLaEmpresaLoDefineElUltimoAlta() throws Exception {
        String vendedor = cuitAleatorio();
        crear(vendedor, cuitAleatorio(), "Nombre Viejo");
        String segunda = crear(vendedor, cuitAleatorio(), "Nombre Nuevo");

        mvc.perform(get("/api/facturas").param("cuitVendedor", vendedor))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].id").value(segunda))
                .andExpect(jsonPath("$.content[0].vendedor.nombre").value("Nombre Nuevo"))
                .andExpect(jsonPath("$.content[1].vendedor.nombre").value("Nombre Nuevo"));
    }

    @Test
    void validaCuitPartesItemsYPrecios() throws Exception {
        String a = cuitAleatorio();
        String b = cuitAleatorio();
        String item = "[{\"nombre\": \"x\", \"cantidad\": 1, \"precio\": 1}]";

        rechaza(factura("20123456787", b, "ARS", item, ""), "vendedor.cuit");
        rechaza(factura(a, a, "ARS", item, ""), "partesDistintas");
        rechaza(factura(a, b, "ARS", "[]", ""), "items");
        rechaza(factura(a, b, "ARS", "[{\"nombre\": \"x\", \"cantidad\": 0, \"precio\": 1}]", ""),
                "items[0].cantidad");
        rechaza(factura(a, b, "ARS", "[{\"nombre\": \"x\", \"cantidad\": 1, \"precio\": 1.005}]", ""),
                "items[0].precio");
        rechaza(factura(a, b, "ARS", "[{\"nombre\": \"x\", \"cantidad\": 1, \"precio\": -5}]", ""),
                "items[0].precio");
        rechaza(factura(a, b, "ARS", "[{\"nombre\": \"\", \"cantidad\": 1, \"precio\": 1}]", ""),
                "items[0].nombre");

        var cincuentaYUno = new StringBuilder("[");
        for (int i = 0; i < 51; i++) {
            cincuentaYUno.append(i == 0 ? "" : ",").append("{\"nombre\": \"x\", \"cantidad\": 1, \"precio\": 1}");
        }
        rechaza(factura(a, b, "ARS", cincuentaYUno + "]", ""), "items");
    }

    @Test
    void unValorDeTipoIncorrectoIndicaElCampo() throws Exception {
        String cuerpo = factura(cuitAleatorio(), cuitAleatorio(), "ARS",
                "[{\"nombre\": \"x\", \"cantidad\": 1.5, \"precio\": 1}]", "");
        mvc.perform(post("/api/facturas").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].campo").value("items[0].cantidad"));
    }

    @Test
    void losMensajesDeValidacionEstanEnCastellano() throws Exception {
        String cuerpo = factura(cuitAleatorio(), cuitAleatorio(), "ARS", "[]", "");
        mvc.perform(post("/api/facturas").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(jsonPath("$.errors[0].mensaje", containsString("entre 1 y 50")));
    }

    @Test
    void jsonMalFormadoTambienEsProblemDetail() throws Exception {
        String a = cuitAleatorio();
        String b = cuitAleatorio();
        for (var cuerpo : new String[] {
                factura(a, b, "EUR", "[{\"nombre\": \"x\", \"cantidad\": 1, \"precio\": 1}]", ""),
                factura(a, b, "ARS", "[{\"nombre\": \"x\", \"cantidad\": 1.5, \"precio\": 1}]", ""),
                "{no es json"}) {
            mvc.perform(post("/api/facturas").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE));
        }
    }

    @Test
    void laBusquedaExigeAlMenosUnFiltroValido() throws Exception {
        mvc.perform(get("/api/facturas"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("al menos un filtro")));

        mvc.perform(get("/api/facturas").param("cuitVendedor", "123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].campo").value("cuitVendedor"));

        mvc.perform(get("/api/facturas").param("cuitVendedor", cuitAleatorio()).param("size", "500"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void facturaInexistenteDa404() throws Exception {
        var id = UUID.randomUUID();
        mvc.perform(get("/api/facturas/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE));
        mvc.perform(get("/api/facturas/{id}/pdf", id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/facturas/no-es-uuid")).andExpect(status().isBadRequest());
    }

    @Test
    void lasVencidasNoSeVenYLaPurgaBorraTodo() throws Exception {
        String vendedor = cuitAleatorio();
        String comprador = cuitAleatorio();
        var id = UUID.fromString(crear(vendedor, comprador, "Vendedor"));
        String key = jdbc.queryForObject("select s3_key from factura where id = ?", String.class, id);
        assertThat(s3.headObject(h -> h.bucket(BUCKET).key(key)).contentType()).isEqualTo("application/pdf");

        jdbc.update("update factura set expires_at = ? where id = ?",
                Timestamp.from(Instant.now().minusSeconds(1)), id);

        mvc.perform(get("/api/facturas/{id}", id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/facturas/{id}/pdf", id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/facturas").param("cuitVendedor", vendedor))
                .andExpect(jsonPath("$.totalElements").value(0));

        purga.purgar();

        assertThat(jdbc.queryForObject("select count(*) from factura where id = ?", Integer.class, id)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from factura_item where factura_id = ?", Integer.class,
                id)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from empresa where cuit in (?, ?)", Integer.class,
                vendedor, comprador)).isZero();
        assertThatThrownBy(() -> s3.headObject(h -> h.bucket(BUCKET).key(key))).isInstanceOf(S3Exception.class);
    }

    private String crear(String vendedor, String comprador, String nombreVendedor) throws Exception {
        String cuerpo = factura(vendedor, comprador, "USD",
                "[{\"nombre\": \"Item\", \"cantidad\": 1, \"precio\": 100}]", "")
                .replace("Vendedor " + vendedor, nombreVendedor);
        var res = mvc.perform(post("/api/facturas").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(res.getResponse().getContentAsString(), "$.id");
    }

    private void rechaza(String cuerpo, String campo) throws Exception {
        mvc.perform(post("/api/facturas").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.errors[*].campo", hasItem(campo)));
    }

    private static String factura(String vendedor, String comprador, String moneda, String items, String extra) {
        return """
                {"vendedor": {"cuit": "%s", "nombre": "Vendedor %s"},
                 "comprador": {"cuit": "%s", "nombre": "Comprador %s"},
                 "moneda": "%s", "items": %s %s}
                """.formatted(vendedor, vendedor, comprador, comprador, moneda, items, extra);
    }

    /** CUIT aleatorio válido, para que los tests no se pisen entre sí. */
    private static String cuitAleatorio() {
        int[] pesos = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
        while (true) {
            String base = "20" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
            int suma = 0;
            for (int i = 0; i < 10; i++) {
                suma += (base.charAt(i) - '0') * pesos[i];
            }
            int dv = 11 - (suma % 11);
            if (dv != 10) {
                return base + (dv == 11 ? 0 : dv);
            }
        }
    }
}
