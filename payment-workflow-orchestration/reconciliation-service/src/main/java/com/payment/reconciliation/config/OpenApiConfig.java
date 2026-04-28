package com.payment.reconciliation.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI reconciliationServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Reconciliation Service API")
                        .version("1.0.0")
                        .description("Compares internal payment records against provider settlement files — detects missing or mismatched transactions"));
    }
}
