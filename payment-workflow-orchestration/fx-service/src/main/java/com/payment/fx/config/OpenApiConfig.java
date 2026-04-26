package com.payment.fx.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI fxServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FX Service API")
                        .version("1.0.0")
                        .description("Foreign exchange conversion — applies mid-market rates with a 0.5% spread"));
    }
}
