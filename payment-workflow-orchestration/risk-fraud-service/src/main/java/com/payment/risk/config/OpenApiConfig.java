package com.payment.risk.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI riskFraudServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Risk & Fraud Service API")
                        .version("1.0.0")
                        .description("Rule-based risk scoring — returns APPROVED, REVIEW, or BLOCKED with a 0-100 risk score"));
    }
}
