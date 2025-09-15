package com.ghiloufi.aicode.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI configuration for the AI Code Reviewer application.
 *
 * <p>This configuration customizes the OpenAPI documentation that will be
 * available at /swagger-ui.html and /v3/api-docs endpoints.
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private int serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI Code Reviewer API")
                        .description("""
                            API for the AI Code Reviewer application that provides automated code review
                            using LLM and static analysis tools for GitHub Pull Requests and local Git commits.

                            This API allows you to:
                            - Trigger code reviews for GitHub PRs or local commits
                            - Get review status and results
                            - Configure review parameters
                            - Access review history
                            """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("AI Code Reviewer")
                                .email("support@aicodereview.com"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Development server"),
                        new Server()
                                .url("https://api.aicodereview.com")
                                .description("Production server")
                ));
    }
}