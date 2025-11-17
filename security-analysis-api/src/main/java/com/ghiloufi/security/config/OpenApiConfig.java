package com.ghiloufi.security.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI securityAnalysisOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Security Analysis API")
                .description(
                    "REST API for analyzing Java code security vulnerabilities using SpotBugs and Find Security Bugs")
                .version("0.0.2")
                .contact(
                    new Contact().name("Security Analysis Team").email("security@ghiloufi.com")));
  }
}
