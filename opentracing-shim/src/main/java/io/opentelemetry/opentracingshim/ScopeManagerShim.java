/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opentracingshim;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import javax.annotation.Nullable;

final class ScopeManagerShim extends BaseShimObject implements ScopeManager {
  private static final SpanShim NOOP_SPANSHIM =
      new SpanShim(
          new TelemetryInfo(
              OpenTelemetry.noop().getTracer("noop"), OpenTracingPropagators.builder().build()),
          io.opentelemetry.api.trace.Span.getInvalid());

  public ScopeManagerShim(TelemetryInfo telemetryInfo) {
    super(telemetryInfo);
  }

  @Override
  @Nullable
  public Span activeSpan() {
    SpanShim spanShim = SpanShim.current();
    io.opentelemetry.api.trace.Span span = null;
    if (spanShim == null) {
      span = io.opentelemetry.api.trace.Span.current();
    } else {
      span = spanShim.getSpan();
    }

    // As OpenTracing simply returns null when no active instance is available,
    // we need to do map an invalid OpenTelemetry span to null here.
    if (!span.getSpanContext().isValid()) {
      return null;
    }

    // If there's a SpanShim for the *actual* active Span, simply return it.
    if (spanShim != null && span == io.opentelemetry.api.trace.Span.current()) {
      return spanShim;
    }

    // Span was activated from outside the Shim layer unfortunately.
    return new SpanShim(telemetryInfo(), span);
  }

  @Override
  @SuppressWarnings("MustBeClosedChecker")
  public Scope activate(@Nullable Span span) {
    if (span == null) {
      return new ScopeShim(Context.current().with(NOOP_SPANSHIM).makeCurrent());
    }
    return new ScopeShim(Context.current().with(ShimUtil.getSpanShim(span)).makeCurrent());
  }
}
