package org.example.passpoint;

import org.example.passpoint.global.jwt.JwtProperties;
import org.example.passpoint.global.s3.S3Properties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties({JwtProperties.class, S3Properties.class})
@SpringBootApplication
public class PassPointApplication {

    public static void main(String[] args) {
        SpringApplication.run(PassPointApplication.class, args);
    }
}
