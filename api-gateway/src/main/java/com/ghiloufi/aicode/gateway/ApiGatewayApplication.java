package com.ghiloufi.aicode.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(
    scanBasePackages = {"com.ghiloufi.aicode.gateway", "com.ghiloufi.aicode.core"})
@EnableJpaRepositories(
    basePackages = "com.ghiloufi.aicode.core.infrastructure.persistence.repository")
@EntityScan(basePackages = "com.ghiloufi.aicode.core.infrastructure.persistence.entity")
public class ApiGatewayApplication {

  public static void main(String[] args) {
    SpringApplication.run(ApiGatewayApplication.class, args);
  }
}
