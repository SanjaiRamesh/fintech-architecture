package com.payment.ledger.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ledgerServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ledger Service API")
                        .version("1.0.0")
                        .description("Immutable financial record keeper — insert-only transactions and double-entry ledger entries"));
    }
}
