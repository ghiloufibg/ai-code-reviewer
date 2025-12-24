package com.ghiloufi.aicode.core.domain.port.output;

import com.ghiloufi.aicode.core.domain.model.AgentAction;
import com.ghiloufi.aicode.core.domain.model.AgentConfiguration;
import com.ghiloufi.aicode.core.domain.model.AgentState;
import com.ghiloufi.aicode.core.domain.model.PrioritizedFindings;
import com.ghiloufi.aicode.core.domain.model.ReviewResult;
import com.ghiloufi.aicode.core.domain.model.Suggestion;
import java.util.List;
import reactor.core.publisher.Mono;

public interface AgentDecisionEnginePort {

  Mono<AgentAction> decideNextAction(AgentState state, AnalysisContext context);

  Mono<PrioritizedFindings> prioritizeFindings(
      List<ReviewResult.Issue> findings, AgentConfiguration config);

  Mono<Boolean> shouldContinueAnalysis(AgentState state, AgentConfiguration config);

  Mono<List<Suggestion>> generateSuggestions(List<ReviewResult.Issue> findings);

  record AnalysisContext(
      String repositoryPath,
      String commitHash,
      String baseBranch,
      String targetBranch,
      List<String> changedFiles,
      boolean testsEnabled,
      boolean testsExecuted,
      int totalFindings,
      double currentConfidence) {

    public static AnalysisContext initial(
        final String repositoryPath, final String commitHash, final String baseBranch) {
      return new AnalysisContext(
          repositoryPath, commitHash, baseBranch, null, List.of(), false, false, 0, 0.0);
    }

    public AnalysisContext withChangedFiles(final List<String> files) {
      return new AnalysisContext(
          repositoryPath,
          commitHash,
          baseBranch,
          targetBranch,
          List.copyOf(files),
          testsEnabled,
          testsExecuted,
          totalFindings,
          currentConfidence);
    }

    public AnalysisContext withTestsEnabled(final boolean enabled) {
      return new AnalysisContext(
          repositoryPath,
          commitHash,
          baseBranch,
          targetBranch,
          changedFiles,
          enabled,
          testsExecuted,
          totalFindings,
          currentConfidence);
    }

    public AnalysisContext withTestsExecuted(final boolean executed) {
      return new AnalysisContext(
          repositoryPath,
          commitHash,
          baseBranch,
          targetBranch,
          changedFiles,
          testsEnabled,
          executed,
          totalFindings,
          currentConfidence);
    }

    public AnalysisContext withFindings(final int findings, final double confidence) {
      return new AnalysisContext(
          repositoryPath,
          commitHash,
          baseBranch,
          targetBranch,
          changedFiles,
          testsEnabled,
          testsExecuted,
          findings,
          confidence);
    }

    public boolean hasChangedFiles() {
      return changedFiles != null && !changedFiles.isEmpty();
    }

    public int changedFilesCount() {
      return changedFiles != null ? changedFiles.size() : 0;
    }
  }
}
