package ar.com.facturacion.infrastructure.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import ar.com.facturacion.domain.model.Cuit;
import ar.com.facturacion.domain.model.Empresa;
import ar.com.facturacion.domain.model.Factura;
import ar.com.facturacion.domain.model.Item;
import ar.com.facturacion.domain.model.Moneda;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.core.io.DefaultResourceLoader;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/** Además de verificar, deja un PDF de ejemplo en {@code target/factura-ejemplo.pdf} para revisarlo a ojo. */
class ThymeleafGeneradorPdfTest {

    @Test
    void generaUnPdfConLosDatosDeLaFactura() throws Exception {
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        var engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        var generador = new ThymeleafGeneradorPdf(engine, Mappers.getMapper(FacturaPdfVistaMapper.class),
                new DefaultResourceLoader(), "classpath:/templates/", ZoneId.of("America/Argentina/Buenos_Aires"));

        var emision = Instant.parse("2026-10-15T19:30:00Z");
        var factura = Factura.emitir(
                new Empresa(new Cuit("20123456786"), "Ñandú & Cía. S.A."),
                new Empresa(new Cuit("30712345671"), "Compradora <S.R.L.>"),
                Moneda.ARS,
                List.of(new Item("Servicio de mantenimiento", 2, new BigDecimal("1500.50")),
                        new Item("Repuesto", 1, new BigDecimal("999.99"))),
                emision, Duration.ofDays(30));

        byte[] pdf = generador.generar(factura);

        Path salida = Path.of("target", "factura-ejemplo.pdf");
        Files.createDirectories(salida.getParent());
        Files.write(salida, pdf);

        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        try (var doc = Loader.loadPDF(pdf)) {
            String texto = new PDFTextStripper().getText(doc);
            assertThat(texto)
                    .contains(factura.getId().toString())
                    .contains("20-12345678-6", "Ñandú & Cía. S.A.")
                    .contains("30-71234567-1", "Compradora <S.R.L.>")
                    .contains("Servicio de mantenimiento", "3.001,00")
                    .contains("ARS 4.000,99")
                    .contains("15/10/2026 16:30")
                    .contains("14/11/2026 16:30");
        }
    }
}
