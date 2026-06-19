package org.example.passpoint.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.example.passpoint.global.firebase.FirebaseProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseProperties props) throws IOException {
        GoogleCredentials credentials = GoogleCredentials.fromStream(
                new ByteArrayInputStream(props.credentialsJson().getBytes(StandardCharsets.UTF_8))
        );
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();
        // 테스트에서 컨텍스트가 여러 번 뜰 때 "FirebaseApp already exists" 방지
        FirebaseApp app = FirebaseApp.getApps().isEmpty()
                ? FirebaseApp.initializeApp(options)
                : FirebaseApp.getInstance();
        return FirebaseMessaging.getInstance(app);
    }
}
