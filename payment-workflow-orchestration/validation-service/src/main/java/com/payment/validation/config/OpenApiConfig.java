package com.payment.validation.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI validationServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Validation Service API")
                        .version("1.0.0")
                        .description("Stateless payment validation — IBAN checksums, currency codes, compliance rules"));
    }
}
