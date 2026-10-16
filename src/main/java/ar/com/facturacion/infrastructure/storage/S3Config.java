package ar.com.facturacion.infrastructure.storage;

import ar.com.facturacion.infrastructure.config.AppProperties;
import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration(proxyBeanMethods = false)
public class S3Config {

    @Bean
    AwsCredentialsProvider awsCredentialsProvider(AppProperties props) {
        var s3 = props.s3();
        if (StringUtils.hasText(s3.accessKey()) && StringUtils.hasText(s3.secretKey())) {
            return StaticCredentialsProvider.create(AwsBasicCredentials.create(s3.accessKey(), s3.secretKey()));
        }
        // IRSA (EKS) / task role (ECS) / instance profile / ~/.aws: lo resuelve el SDK. Nada hardcodeado.
        return DefaultCredentialsProvider.builder().build();
    }

    @Bean
    S3Client s3Client(AppProperties props, AwsCredentialsProvider credentials) {
        var s3 = props.s3();
        var builder = S3Client.builder()
                .credentialsProvider(credentials)
                .forcePathStyle(s3.pathStyle());
        if (StringUtils.hasText(s3.region())) {
            builder.region(Region.of(s3.region()));
        }
        if (s3.endpoint() != null) {
            builder.endpointOverride(s3.endpoint());
        }
        return builder.build();
    }

    @Bean
    S3Presigner s3Presigner(AppProperties props, AwsCredentialsProvider credentials) {
        var s3 = props.s3();
        var builder = S3Presigner.builder()
                .credentialsProvider(credentials)
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(s3.pathStyle()).build());
        if (StringUtils.hasText(s3.region())) {
            builder.region(Region.of(s3.region()));
        }
        URI endpoint = s3.publicEndpoint() != null ? s3.publicEndpoint() : s3.endpoint();
        if (endpoint != null) {
            builder.endpointOverride(endpoint);
        }
        return builder.build();
    }
}
