package org.example.passpoint.global.config;

import org.example.passpoint.global.s3.S3Properties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class S3Config {

    @Bean
    public S3Presigner s3Presigner(S3Properties props) {
        var credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(props.accessKey(), props.secretKey())
        );
        var s3Config = S3Configuration.builder()
                .pathStyleAccessEnabled(props.pathStyle())
                .build();
        var builder = S3Presigner.builder()
                .region(Region.of(props.region()))
                .credentialsProvider(credentials)
                .serviceConfiguration(s3Config);
        // presigned URL은 클라이언트(Android)가 직접 접근하므로 publicEndpoint로 서명해야
        // 서명에 포함된 Host와 실제 요청 Host가 일치한다. publicEndpoint 미설정 시 endpoint 사용.
        String presignEndpoint = StringUtils.hasText(props.publicEndpoint())
            ? props.publicEndpoint()
            : props.endpoint();

        if (StringUtils.hasText(presignEndpoint)) {
            builder.endpointOverride(URI.create(presignEndpoint));
        }
        return builder.build();
    }

    @Bean
    public S3Client s3Client(S3Properties props) {
        var credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(props.accessKey(), props.secretKey())
        );
        var s3Config = S3Configuration.builder()
                .pathStyleAccessEnabled(props.pathStyle())
                .build();
        var builder = S3Client.builder()
                .region(Region.of(props.region()))
                .credentialsProvider(credentials)
                .serviceConfiguration(s3Config);
        if (props.endpoint() != null && !props.endpoint().isBlank()) {
            builder.endpointOverride(URI.create(props.endpoint()));
        }
        return builder.build();
    }
}
