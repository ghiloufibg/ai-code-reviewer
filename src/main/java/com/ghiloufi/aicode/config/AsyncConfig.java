package com.ghiloufi.aicode.config;

import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** Configuration for asynchronous processing in the analysis service. */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {

  /** Thread pool executor for analysis processing */
  @Bean(name = "analysisTaskExecutor")
  public Executor analysisTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(5);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("Analysis-");
    executor.setRejectedExecutionHandler(
        (runnable, executor1) -> {
          log.warn("Analysis task rejected - queue full");
          throw new RuntimeException("Analysis queue is full");
        });
    executor.initialize();

    log.info(
        "Analysis task executor configured: core={}, max={}, queue={}",
        executor.getCorePoolSize(),
        executor.getMaxPoolSize(),
        executor.getQueueCapacity());

    return executor;
  }
}
