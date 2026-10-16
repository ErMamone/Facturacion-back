package ar.com.facturacion.infrastructure.pdf;

import ar.com.facturacion.application.port.out.GeneradorPdf;
import ar.com.facturacion.domain.model.Factura;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.slf4j.Slf4jLogger;
import com.openhtmltopdf.util.XRLog;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.ZoneId;
import java.util.Locale;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Plantilla Thymeleaf → HTML → PDF (OpenHTMLtoPDF sobre PDFBox), todo en memoria.
 * Todo el diseño vive en {@code templates/pdf/factura.html}; los datos que recibe están en {@link FacturaPdfVista}.
 */
@Component
public class ThymeleafGeneradorPdf implements GeneradorPdf {

    static final String PLANTILLA = "pdf/factura";
    private static final Locale ES_AR = Locale.forLanguageTag("es-AR");

    static {
        XRLog.setLoggerImpl(new Slf4jLogger());
    }

    private final ITemplateEngine templateEngine;
    private final FacturaPdfVistaMapper mapper;
    private final ZoneId zona;
    private final String baseUri;

    public ThymeleafGeneradorPdf(ITemplateEngine templateEngine, FacturaPdfVistaMapper mapper,
                                 ResourceLoader resourceLoader,
                                 @Value("${spring.thymeleaf.prefix}") String prefijoPlantillas,
                                 @Value("${app.zona-horaria}") ZoneId zona) {
        this.templateEngine = templateEngine;
        this.mapper = mapper;
        this.zona = zona;
        this.baseUri = baseUri(resourceLoader, prefijoPlantillas);
    }

    @Override
    public byte[] generar(Factura factura) {
        var vista = mapper.toVista(factura, zona);
        var ctx = new Context(ES_AR);
        ctx.setVariable("factura", vista);
        ctx.setVariable("vendedor", vista.getVendedor());
        ctx.setVariable("comprador", vista.getComprador());
        ctx.setVariable("items", vista.getItems());
        ctx.setVariable("total", vista.getTotal());
        ctx.setVariable("moneda", vista.getMoneda());
        ctx.setVariable("emitidaEl", vista.getEmitidaEl());
        ctx.setVariable("venceEl", vista.getVenceEl());

        String html = templateEngine.process(PLANTILLA, ctx);
        // jsoup tolera HTML5 "suelto": quien diseñe la plantilla no necesita escribir XHTML estricto
        var documento = new W3CDom().fromJsoup(Jsoup.parse(html, baseUri));

        try (var out = new ByteArrayOutputStream()) {
            new PdfRendererBuilder()
                    .useFastMode()
                    .withW3cDocument(documento, baseUri)
                    .toStream(out)
                    .run();
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el PDF de la factura " + factura.getId(), e);
        }
    }

    /**
     * URL de la propia plantilla: las rutas relativas (logos, imágenes) se resuelven contra ella, o sea contra
     * {@code templates/pdf/}. Funciona igual en el classpath, dentro del jar o en una carpeta montada.
     */
    private static String baseUri(ResourceLoader resourceLoader, String prefijo) {
        var plantilla = resourceLoader.getResource(prefijo + PLANTILLA + ".html");
        try {
            // en disco se pasa a ruta absoluta: un "file:src/..." relativo OpenHTMLtoPDF no lo resuelve bien
            return plantilla.isFile()
                    ? plantilla.getFile().getAbsoluteFile().toURI().toString()
                    : plantilla.getURL().toExternalForm();
        } catch (IOException e) {
            throw new UncheckedIOException("No se encontró la plantilla PDF en " + plantilla.getDescription(), e);
        }
    }
}
