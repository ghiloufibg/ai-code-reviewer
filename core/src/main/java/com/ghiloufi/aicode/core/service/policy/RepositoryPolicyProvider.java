package com.ghiloufi.aicode.core.service.policy;

import com.ghiloufi.aicode.core.config.ContextRetrievalConfig;
import com.ghiloufi.aicode.core.domain.model.PolicyDocument;
import com.ghiloufi.aicode.core.domain.model.PolicyType;
import com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier;
import com.ghiloufi.aicode.core.domain.model.RepositoryPolicies;
import com.ghiloufi.aicode.core.domain.port.output.SCMPort;
import java.util.List;
import java.util.Optional;
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

    return Mono.zip(
            fetchContributing(repo, policiesConfig),
            fetchCodeOfConduct(repo, policiesConfig),
            fetchPrTemplate(repo, policiesConfig),
            fetchSecurity(repo, policiesConfig))
        .map(
            tuple ->
                new RepositoryPolicies(
                    tuple.getT1().orElse(null),
                    tuple.getT2().orElse(null),
                    tuple.getT3().orElse(null),
                    tuple.getT4().orElse(null)));
  }

  private Mono<Optional<PolicyDocument>> fetchContributing(
      final RepositoryIdentifier repo,
      final ContextRetrievalConfig.RepositoryPoliciesConfig policiesConfig) {
    if (!policiesConfig.includeContributing()) {
      return Mono.just(Optional.empty());
    }
    return fetchFirstAvailable(
        repo, policiesConfig.getContributingPaths(), PolicyType.CONTRIBUTING);
  }

  private Mono<Optional<PolicyDocument>> fetchCodeOfConduct(
      final RepositoryIdentifier repo,
      final ContextRetrievalConfig.RepositoryPoliciesConfig policiesConfig) {
    if (!policiesConfig.includeCodeOfConduct()) {
      return Mono.just(Optional.empty());
    }
    return fetchFirstAvailable(
        repo, policiesConfig.getCodeOfConductPaths(), PolicyType.CODE_OF_CONDUCT);
  }

  private Mono<Optional<PolicyDocument>> fetchPrTemplate(
      final RepositoryIdentifier repo,
      final ContextRetrievalConfig.RepositoryPoliciesConfig policiesConfig) {
    if (!policiesConfig.includePrTemplate()) {
      return Mono.just(Optional.empty());
    }
    return fetchFirstAvailable(repo, policiesConfig.getPrTemplatePaths(), PolicyType.PR_TEMPLATE);
  }

  private Mono<Optional<PolicyDocument>> fetchSecurity(
      final RepositoryIdentifier repo,
      final ContextRetrievalConfig.RepositoryPoliciesConfig policiesConfig) {
    if (!policiesConfig.includeSecurity()) {
      return Mono.just(Optional.empty());
    }
    return fetchFirstAvailable(repo, policiesConfig.getSecurityPaths(), PolicyType.SECURITY);
  }

  private Mono<Optional<PolicyDocument>> fetchFirstAvailable(
      final RepositoryIdentifier repo, final List<String> paths, final PolicyType type) {
    return Flux.fromIterable(paths)
        .concatMap(path -> fetchPolicy(repo, path, type))
        .filter(Optional::isPresent)
        .next()
        .defaultIfEmpty(Optional.empty());
  }

  private Mono<Optional<PolicyDocument>> fetchPolicy(
      final RepositoryIdentifier repo, final String path, final PolicyType type) {
    return scmPort
        .getFileContent(repo, path)
        .map(content -> Optional.of(createPolicyDocument(path, content, type)))
        .onErrorResume(
            error -> {
              log.trace("Policy file not found: {}", path);
              return Mono.just(Optional.empty());
            });
  }

  private PolicyDocument createPolicyDocument(
      final String path, final String content, final PolicyType type) {
    final var policiesConfig = config.policies();
    final String name = extractFileName(path);
    final boolean truncated = content.length() > policiesConfig.maxContentChars();
    final String finalContent =
        truncated
            ? content.substring(0, policiesConfig.maxContentChars()) + "\n... (truncated)"
            : content;

    return new PolicyDocument(name, path, finalContent, type, truncated);
  }

  private String extractFileName(final String path) {
    final int lastSlash = path.lastIndexOf('/');
    return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
  }
}
