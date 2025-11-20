package com.ghiloufi.aicode.core.infrastructure.persistence;

import com.ghiloufi.aicode.core.application.service.audit.ContextAuditService;
import com.ghiloufi.aicode.core.infrastructure.persistence.mapper.ContextMapper;
import com.ghiloufi.aicode.core.infrastructure.persistence.repository.ContextRetrievalSessionRepository;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootConfiguration
@EnableAutoConfiguration
@EnableJpaRepositories(
    basePackages = "com.ghiloufi.aicode.core.infrastructure.persistence.repository")
@EntityScan(basePackages = "com.ghiloufi.aicode.core.infrastructure.persistence.entity")
public class TestConfig {

  @TestConfiguration
  static class RepositoryTestConfig {
    @Bean
    public PostgresReviewRepository postgresReviewRepository(
        final com.ghiloufi.aicode.core.infrastructure.persistence.repository.ReviewJpaRepository
            jpaRepository) {
      return new PostgresReviewRepository(jpaRepository);
    }

    @Bean
    public ContextMapper contextMapper() {
      return new ContextMapper();
    }

    @Bean
    public ContextAuditService contextAuditService(
        final ContextRetrievalSessionRepository sessionRepository, final ContextMapper mapper) {
      return new ContextAuditService(sessionRepository, mapper);
    }
  }
}
