package com.payment.routing.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI routingEngineOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Routing Engine API")
                        .version("1.0.0")
                        .description("Selects the optimal payment provider and rail based on currency, country, and method"));
    }
}
