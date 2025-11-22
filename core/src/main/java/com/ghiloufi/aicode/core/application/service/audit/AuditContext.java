package com.ghiloufi.aicode.core.application.service.audit;

import com.ghiloufi.aicode.core.domain.model.EnrichedDiffAnalysisBundle;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
final class AuditContext {
  private EnrichedDiffAnalysisBundle enrichedDiff;
  private String promptText;
  private final List<ContextAuditService.StrategyExecutionResult> strategyResults =
      new ArrayList<>();

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
