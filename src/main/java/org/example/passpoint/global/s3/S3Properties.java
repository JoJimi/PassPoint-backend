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
        int presignedUrlExpiryMinutes
) {
}
