package com.ghiloufi.aicode.gateway;

import com.ghiloufi.aicode.core.config.ContextRetrievalConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(
    scanBasePackages = {"com.ghiloufi.aicode.gateway", "com.ghiloufi.aicode.core"})
@EnableJpaRepositories(
    basePackages = "com.ghiloufi.aicode.core.infrastructure.persistence.repository")
@EntityScan(basePackages = "com.ghiloufi.aicode.core.infrastructure.persistence.entity")
@EnableConfigurationProperties(ContextRetrievalConfig.class)
public class ApiGatewayApplication {

  public static void main(String[] args) {
    SpringApplication.run(ApiGatewayApplication.class, args);
  }
}
