package ar.com.facturacion.infrastructure.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuración propia (prefijo {@code app}). Se valida al arrancar: si falta algo, la app no levanta.
 *
 * @param bucket          bucket S3 de los PDFs ({@code APP_BUCKET})
 * @param retention       cuánto vive una factura antes de purgarse ({@code APP_RETENTION})
 * @param purgeInterval   cada cuánto corre la purga
 * @param purgeBatchSize  facturas vencidas que se borran por lote
 * @param presignDuration validez de la URL firmada de descarga
 * @param zonaHoraria     zona para las fechas del PDF y la carpeta en S3
 */
@Validated
@ConfigurationProperties("app")
public record AppProperties(
        @NotBlank String bucket,
        @NotNull Duration retention,
        @NotNull Duration purgeInterval,
        @Positive int purgeBatchSize,
        @NotNull Duration presignDuration,
        @NotNull ZoneId zonaHoraria,
        @Valid @DefaultValue Cors cors,
        @Valid @DefaultValue S3 s3) {

    /** @param allowedOrigins orígenes del frontend habilitados para CORS (vacío = CORS deshabilitado) */
    public record Cors(@DefaultValue List<String> allowedOrigins) {
    }

    /**
     * Solo se completa en local. En AWS queda vacío y el SDK resuelve región y credenciales solo.
     *
     * @param region         región; si falta, cadena por defecto del SDK ({@code AWS_REGION})
     * @param endpoint       endpoint alternativo (LocalStack)
     * @param publicEndpoint endpoint para las URLs firmadas, cuando el navegador no ve el mismo host que el backend
     * @param pathStyle      acceso path-style (LocalStack)
     * @param accessKey      credencial estática, solo para LocalStack
     * @param secretKey      credencial estática, solo para LocalStack
     */
    public record S3(String region, URI endpoint, URI publicEndpoint, boolean pathStyle, String accessKey,
                     String secretKey) {
    }
}
