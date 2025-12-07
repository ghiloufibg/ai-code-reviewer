package com.ghiloufi.aicode.llmworker;

import com.ghiloufi.aicode.core.application.service.AIReviewStreamingService;
import com.ghiloufi.aicode.core.application.service.ReviewManagementService;
import com.ghiloufi.aicode.core.infrastructure.persistence.PostgresReviewRepository;
import com.ghiloufi.aicode.core.service.accumulator.ReviewChunkAccumulator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(
    scanBasePackages = {"com.ghiloufi.aicode.llmworker", "com.ghiloufi.aicode.core"})
@EnableScheduling
@ConfigurationPropertiesScan(
    basePackages = {"com.ghiloufi.aicode.llmworker", "com.ghiloufi.aicode.core"})
@ComponentScan(
    basePackages = {"com.ghiloufi.aicode.llmworker", "com.ghiloufi.aicode.core"},
    excludeFilters =
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = {
              ReviewManagementService.class,
              PostgresReviewRepository.class,
              AIReviewStreamingService.class,
              ReviewChunkAccumulator.class
            }))
public class LlmWorkerApplication {

  public static void main(final String[] args) {
    SpringApplication.run(LlmWorkerApplication.class, args);
  }
}
