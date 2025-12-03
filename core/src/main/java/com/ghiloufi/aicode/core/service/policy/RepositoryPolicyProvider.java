package com.ghiloufi.aicode.core.service.policy;

import com.ghiloufi.aicode.core.config.ContextRetrievalConfig;
import com.ghiloufi.aicode.core.domain.model.PolicyDocument;
import com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier;
import com.ghiloufi.aicode.core.domain.model.RepositoryPolicies;
import com.ghiloufi.aicode.core.domain.port.output.SCMPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public final class RepositoryPolicyProvider {

  private final SCMPort scmPort;
  private final ContextRetrievalConfig config;

  public Mono<RepositoryPolicies> getPolicies(final RepositoryIdentifier repo) {
    if (!config.isPoliciesEnabled()) {
      log.debug("Repository policy injection disabled");
      return Mono.just(RepositoryPolicies.empty());
    }

    final var policiesConfig = config.policies();
    final List<String> files = policiesConfig.files();

    if (files.isEmpty()) {
      log.debug("No policy files configured");
      return Mono.just(RepositoryPolicies.empty());
    }

    return Flux.fromIterable(files)
        .flatMap(path -> fetchFile(repo, path, policiesConfig.maxContentChars()))
        .collectList()
        .map(RepositoryPolicies::new)
        .doOnNext(
            policies ->
                log.debug("Loaded {} policy documents for {}", policies.policyCount(), repo));
  }

  private Mono<PolicyDocument> fetchFile(
      final RepositoryIdentifier repo, final String path, final int maxChars) {
    return scmPort
        .getFileContent(repo, path)
        .map(content -> createDocument(path, content, maxChars))
        .onErrorResume(
            error -> {
              log.trace("Policy file not found: {}", path);
              return Mono.empty();
            });
  }

  private PolicyDocument createDocument(
      final String path, final String content, final int maxChars) {
    final String name = extractFileName(path);
    final boolean truncated = content.length() > maxChars;
    final String finalContent =
        truncated ? content.substring(0, maxChars) + "\n... (truncated)" : content;

    return new PolicyDocument(name, path, finalContent, truncated);
  }

  private String extractFileName(final String path) {
    final int lastSlash = path.lastIndexOf('/');
    return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
  }
}
