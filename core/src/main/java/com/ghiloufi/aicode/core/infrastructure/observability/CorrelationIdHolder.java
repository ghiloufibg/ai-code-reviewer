package com.ghiloufi.aicode.core.infrastructure.observability;

import java.util.Optional;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

public final class CorrelationIdHolder {

  public static final String CORRELATION_ID_KEY = "correlationId";

  private CorrelationIdHolder() {}

  public static Context withCorrelationId(final Context context, final String correlationId) {
    return context.put(CORRELATION_ID_KEY, correlationId);
  }

  public static Optional<String> getCorrelationId(final ContextView context) {
    return context.getOrEmpty(CORRELATION_ID_KEY);
  }

  public static String getCorrelationIdOrDefault(
      final ContextView context, final String defaultValue) {
    return context.getOrDefault(CORRELATION_ID_KEY, defaultValue);
  }
}
