package ar.com.facturacion.infrastructure.storage;

import ar.com.facturacion.application.port.out.AlmacenamientoPdf;
import ar.com.facturacion.domain.model.Factura;
import ar.com.facturacion.infrastructure.config.AppProperties;
import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.S3Error;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/** PDFs en S3 bajo {@code facturas/{yyyy}/{MM}/{id}.pdf}. La DB solo guarda la key. */
@Slf4j
@Component
@RequiredArgsConstructor
public class S3AlmacenamientoPdf implements AlmacenamientoPdf {

    private static final DateTimeFormatter CARPETA = DateTimeFormatter.ofPattern("yyyy/MM");
    private static final String CONTENT_TYPE_PDF = "application/pdf";

    private final S3Client s3;
    private final S3Presigner presigner;
    private final AppProperties props;

    @Override
    public String guardar(Factura factura, byte[] pdf) {
        String key = "facturas/" + CARPETA.format(factura.getEmitidaEl().atZone(props.zonaHoraria())) + "/"
                + factura.getId() + ".pdf";
        s3.putObject(b -> b.bucket(props.bucket()).key(key).contentType(CONTENT_TYPE_PDF),
                RequestBody.fromBytes(pdf));
        return key;
    }

    @Override
    public URI urlDescarga(String clave, String nombreArchivo) {
        var firmada = presigner.presignGetObject(p -> p
                .signatureDuration(props.presignDuration())
                .getObjectRequest(g -> g
                        .bucket(props.bucket())
                        .key(clave)
                        .responseContentType(CONTENT_TYPE_PDF)
                        .responseContentDisposition("attachment; filename=\"" + nombreArchivo + "\"")));
        return URI.create(firmada.url().toString());
    }

    /** Un solo request para todo el lote (S3 admite hasta 1000 keys). */
    @Override
    public Set<String> borrar(Collection<String> claves) {
        if (claves.isEmpty()) {
            return Set.of();
        }
        var objetos = claves.stream().map(k -> ObjectIdentifier.builder().key(k).build()).toList();
        var respuesta = s3.deleteObjects(b -> b.bucket(props.bucket()).delete(d -> d.objects(objetos).quiet(true)));
        respuesta.errors().forEach(e -> log.warn("No se pudo borrar {} de S3: {} {}", e.key(), e.code(), e.message()));
        return respuesta.errors().stream().map(S3Error::key).collect(Collectors.toUnmodifiableSet());
    }
}
