package com.ghiloufi.aicode.core.application.service.audit;

import com.ghiloufi.aicode.core.domain.model.EnrichedDiffAnalysisBundle;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class ContextAuditHolder {

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

  public void clear() {
    enrichedDiff = null;
    promptText = null;
    strategyResults.clear();
  }
}
