package com.ghiloufi.aicode.core.infrastructure.observability;

import java.util.Map;
import java.util.function.Function;
import org.reactivestreams.Subscription;
import org.slf4j.MDC;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;

public final class MdcContextLifter {

  private static final String MDC_CONTEXT_REACTOR_KEY = "mdc-context-lifter";

  private MdcContextLifter() {}

  public static void registerMdcContextLifter() {
    Hooks.onEachOperator(
        MDC_CONTEXT_REACTOR_KEY,
        Operators.lift((scannable, subscriber) -> new MdcLifter<>(subscriber)));
  }

  public static void unregisterMdcContextLifter() {
    Hooks.resetOnEachOperator(MDC_CONTEXT_REACTOR_KEY);
  }

  public static <T> Mono<T> withMdc(final Mono<T> mono, final Map<String, String> mdcContext) {
    return mono.contextWrite(Context.of(mdcContext));
  }

  public static <T> Flux<T> withMdc(final Flux<T> flux, final Map<String, String> mdcContext) {
    return flux.contextWrite(Context.of(mdcContext));
  }

  public static <T> Function<Mono<T>, Mono<T>> addCorrelationId(final String correlationId) {
    return mono ->
        mono.contextWrite(ctx -> CorrelationIdHolder.withCorrelationId(ctx, correlationId));
  }

  private static final class MdcLifter<T> implements CoreSubscriber<T> {

    private final CoreSubscriber<T> delegate;

    MdcLifter(final CoreSubscriber<T> delegate) {
      this.delegate = delegate;
    }

    @Override
    public void onSubscribe(final Subscription subscription) {
      copyContextToMdc();
      delegate.onSubscribe(subscription);
    }

    @Override
    public void onNext(final T value) {
      copyContextToMdc();
      delegate.onNext(value);
    }

    @Override
    public void onError(final Throwable throwable) {
      copyContextToMdc();
      delegate.onError(throwable);
    }

    @Override
    public void onComplete() {
      copyContextToMdc();
      delegate.onComplete();
    }

    @Override
    public Context currentContext() {
      return delegate.currentContext();
    }

    private void copyContextToMdc() {
      final Context context = delegate.currentContext();
      CorrelationIdHolder.getCorrelationId(context)
          .ifPresent(id -> MDC.put(CorrelationIdHolder.CORRELATION_ID_KEY, id));

      context.getOrEmpty("repository").ifPresent(repo -> MDC.put("repository", (String) repo));
      context
          .getOrEmpty("changeRequestId")
          .ifPresent(crId -> MDC.put("changeRequestId", (String) crId));
      context.getOrEmpty("provider").ifPresent(prov -> MDC.put("provider", (String) prov));
    }
  }
}
