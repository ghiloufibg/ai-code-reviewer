package com.ghiloufi.aicode.gateway.async;

import com.ghiloufi.aicode.core.domain.model.ReviewMode;
import com.ghiloufi.aicode.core.domain.model.async.AsyncReviewRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewModeRouter {

  @Getter
  public enum StreamKey {
    DIFF_REQUESTS("review:requests"),
    AGENT_REQUESTS("review:agent-requests");

    private final String key;

    StreamKey(final String key) {
      this.key = key;
    }
  }

  public StreamKey route(final AsyncReviewRequest request) {
    final ReviewMode mode = request.reviewMode();
    final StreamKey streamKey = resolveStreamKey(mode);

    log.debug(
        "Routing request {} with mode {} to stream {}",
        request.requestId(),
        mode,
        streamKey.getKey());

    return streamKey;
  }

  public StreamKey route(final ReviewMode mode) {
    return resolveStreamKey(mode);
  }

  private StreamKey resolveStreamKey(final ReviewMode mode) {
    return mode == ReviewMode.AGENTIC ? StreamKey.AGENT_REQUESTS : StreamKey.DIFF_REQUESTS;
  }
}
