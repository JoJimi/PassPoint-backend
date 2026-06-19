package org.example.passpoint.global.firebase;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "firebase")
public record FirebaseProperties(
        String credentialsJson  // 서비스 계정 키 JSON 원문 (로컬: .env, 운영: SSM Parameter Store)
) {
}
