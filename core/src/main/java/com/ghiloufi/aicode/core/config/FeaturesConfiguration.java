package com.ghiloufi.aicode.core.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
  SummaryCommentProperties.class,
  PromptProperties.class,
  OptimizedPromptProperties.class,
  PromptVariantProperties.class
})
public class FeaturesConfiguration {}
