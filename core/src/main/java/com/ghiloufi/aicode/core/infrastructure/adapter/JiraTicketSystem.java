package com.ghiloufi.aicode.core.infrastructure.adapter;

import com.ghiloufi.aicode.core.domain.model.TicketBusinessContext;
import com.ghiloufi.aicode.core.domain.port.output.TicketSystemPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@ConditionalOnProperty(
    prefix = "ticket-system.jira",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public final class JiraTicketSystem implements TicketSystemPort {

  private final WebClient jiraClient;

  public JiraTicketSystem(
      @Value("${ticket-system.jira.base-url}") final String baseUrl,
      @Value("${ticket-system.jira.token}") final String token) {

    this.jiraClient =
        WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Authorization", "Bearer " + token)
            .build();

    log.info("Jira ticket system initialized: {}", baseUrl);
  }

  @Override
  public Mono<TicketBusinessContext> getTicketContext(final String ticketId) {
    if (!supportsTicketId(ticketId)) {
      return Mono.empty();
    }

    log.debug("Fetching ticket context for: {}", ticketId);

    return jiraClient
        .get()
        .uri("/rest/api/2/issue/{ticketId}", ticketId)
        .retrieve()
        .bodyToMono(JiraIssue.class)
        .map(this::toBusinessContext)
        .doOnSuccess(context -> log.debug("Fetched ticket: {}", ticketId))
        .doOnError(
            error -> log.error("Failed to fetch ticket {}: {}", ticketId, error.getMessage()))
        .onErrorResume(error -> Mono.empty());
  }

  private TicketBusinessContext toBusinessContext(final JiraIssue issue) {
    final String description =
        issue.fields().description() != null ? issue.fields().description() : "";
    return new TicketBusinessContext(issue.key(), issue.fields().summary(), description);
  }

  record JiraIssue(String id, String key, Fields fields) {}

  record Fields(String summary, String description) {}
}
