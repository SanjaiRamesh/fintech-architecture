package com.payment.provider.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI providerIntegrationOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Provider Integration API")
                        .version("1.0.0")
                        .description("Adapter layer for payment providers — Deutsche Bank, JPMorgan, Visa, Mastercard, PayPal"));
    }
}
