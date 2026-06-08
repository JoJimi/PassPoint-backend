package org.example.passpoint;

import org.example.passpoint.global.jwt.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties(JwtProperties.class)
@SpringBootApplication
public class PassPointApplication {

    public static void main(String[] args) {
        SpringApplication.run(PassPointApplication.class, args);
    }
}
