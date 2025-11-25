package com.ghiloufi.aicode.core.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromptPropertiesFactory {

  private final PromptVariantProperties variantProperties;
  private final PromptProperties currentPromptProperties;
  private final OptimizedPromptProperties optimizedPromptProperties;

  public PromptPropertiesAdapter getActivePromptProperties() {
    final PromptVariantProperties.Variant activeVariant = variantProperties.getVariant();

    return switch (activeVariant) {
      case CURRENT -> {
        log.info("Using CURRENT prompt variant");
        yield new CurrentPromptAdapter(currentPromptProperties);
      }
      case OPTIMIZED -> {
        log.info("Using OPTIMIZED prompt variant");
        yield new OptimizedPromptAdapter(optimizedPromptProperties);
      }
    };
  }

  public interface PromptPropertiesAdapter {
    String getSystem();

    String getFixGeneration();

    String getConfidence();

    String getSchema();

    String getOutputRequirements();
  }

  private record CurrentPromptAdapter(PromptProperties delegate)
      implements PromptPropertiesAdapter {
    @Override
    public String getSystem() {
      return delegate.getSystem();
    }

    @Override
    public String getFixGeneration() {
      return delegate.getFixGeneration();
    }

    @Override
    public String getConfidence() {
      return delegate.getConfidence();
    }

    @Override
    public String getSchema() {
      return delegate.getSchema();
    }

    @Override
    public String getOutputRequirements() {
      return delegate.getOutputRequirements();
    }
  }

  private record OptimizedPromptAdapter(OptimizedPromptProperties delegate)
      implements PromptPropertiesAdapter {
    @Override
    public String getSystem() {
      return delegate.getSystem();
    }

    @Override
    public String getFixGeneration() {
      return delegate.getFixGeneration();
    }

    @Override
    public String getConfidence() {
      return delegate.getConfidence();
    }

    @Override
    public String getSchema() {
      return delegate.getSchema();
    }

    @Override
    public String getOutputRequirements() {
      return delegate.getOutputRequirements();
    }
  }
}
