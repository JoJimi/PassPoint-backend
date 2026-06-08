package org.example.passpoint.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 접근: http://localhost:8080/swagger-ui.html
 * Security 설정에서 Swagger 관련 경로(/swagger-ui/**, /v3/api-docs/**)를 인증 없이 접근 허용(permitAll)해줘야 합니다.
 */
@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI passPointOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PassPoint API")
                        .description("AI 기반 CS/기술 면접 연습 앱 API 명세")
                        .version("1.0.0"))
                // JWT 인증 스킴 등록
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
