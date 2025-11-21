package com.ghiloufi.aicode.core.application.service.audit;

import com.ghiloufi.aicode.core.domain.model.EnrichedDiffAnalysisBundle;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ContextAuditHolder {

  private final ThreadLocal<AuditContext> contextHolder = new ThreadLocal<>();

  public void setEnrichedDiff(final EnrichedDiffAnalysisBundle enrichedDiff) {
    getOrCreateContext().setEnrichedDiff(enrichedDiff);
  }

  public EnrichedDiffAnalysisBundle getEnrichedDiff() {
    final AuditContext context = contextHolder.get();
    return context != null ? context.getEnrichedDiff() : null;
  }

  public void setPromptText(final String promptText) {
    getOrCreateContext().setPromptText(promptText);
  }

  public String getPromptText() {
    final AuditContext context = contextHolder.get();
    return context != null ? context.getPromptText() : null;
  }

  public void addStrategyResult(final ContextAuditService.StrategyExecutionResult result) {
    getOrCreateContext().addStrategyResult(result);
  }

  public List<ContextAuditService.StrategyExecutionResult> getStrategyResults() {
    final AuditContext context = contextHolder.get();
    return context != null ? context.getStrategyResults() : List.of();
  }

  public boolean hasContext() {
    final AuditContext context = contextHolder.get();
    return context != null && context.hasContext();
  }

  public void clear() {
    contextHolder.remove();
  }

  private AuditContext getOrCreateContext() {
    AuditContext context = contextHolder.get();
    if (context == null) {
      context = new AuditContext();
      contextHolder.set(context);
    }
    return context;
  }

  private static final class AuditContext {
    private EnrichedDiffAnalysisBundle enrichedDiff;
    private String promptText;
    private final List<ContextAuditService.StrategyExecutionResult> strategyResults =
        new ArrayList<>();

    public void setEnrichedDiff(final EnrichedDiffAnalysisBundle enrichedDiff) {
      this.enrichedDiff = enrichedDiff;
    }

    public EnrichedDiffAnalysisBundle getEnrichedDiff() {
      return enrichedDiff;
    }

    public void setPromptText(final String promptText) {
      this.promptText = promptText;
    }

    public String getPromptText() {
      return promptText;
    }

    public void addStrategyResult(final ContextAuditService.StrategyExecutionResult result) {
      strategyResults.add(result);
    }

    public List<ContextAuditService.StrategyExecutionResult> getStrategyResults() {
      return List.copyOf(strategyResults);
    }

    public boolean hasContext() {
      return enrichedDiff != null && enrichedDiff.hasContext();
    }
  }
}
