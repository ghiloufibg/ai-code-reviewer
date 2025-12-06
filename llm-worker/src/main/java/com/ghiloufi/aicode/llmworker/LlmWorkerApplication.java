package com.ghiloufi.aicode.llmworker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class LlmWorkerApplication {

  public static void main(final String[] args) {
    SpringApplication.run(LlmWorkerApplication.class, args);
  }
}
