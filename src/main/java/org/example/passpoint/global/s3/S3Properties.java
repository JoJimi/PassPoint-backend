package org.example.passpoint.global.s3;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cloud.aws.s3")
public record S3Properties(
        String endpoint,
        String bucket,
        String region,
        String accessKey,
        String secretKey,
        boolean pathStyle,
        int presignedUrlExpiryMinutes,
        String publicEndpoint  // 에뮬레이터용(10.0.2.2:9000), 미설정이면 endpoint 그대로 사용
) {
}
