/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.microservice.resources;

import java.lang.reflect.*;
import java.util.logging.*;

/**
 * Reflective accessor for the currently-active OpenTelemetry trace / span id, used by
 * {@link LogEntryFormatter} to populate the optional {@code {traceId}} / {@code {spanId}} log fields.
 *
 * <h5 class='topic'>Optional / reflective coupling</h5>
 *
 * <p>
 * OpenTelemetry is <b>not</b> a compile-time dependency of {@code juneau-microservice}. This class
 * probes for {@code io.opentelemetry.api.trace.Span} via {@link Class#forName(String)} at class-load
 * time &mdash; mirroring how {@link org.apache.juneau.rest.server.processor.MdcAsyncListener} reflectively
 * handles SLF4J. When OTel is absent from the runtime classpath the class gracefully degrades:
 * {@link #isAvailable()} returns {@code false} and {@link #currentTraceId()} / {@link #currentSpanId()}
 * return the empty string, so the {@code {traceId}} / {@code {spanId}} placeholders render empty.
 *
 * <h5 class='topic'>Where the id comes from</h5>
 *
 * <p>
 * The id is read from {@code Span.current().getSpanContext()} &mdash; the OTel context active on the
 * calling thread. Inside a {@code @RestOp} handler (or a custom
 * {@link org.apache.juneau.rest.server.observation.Observations observation}) wrapped by the OTel
 * {@code TracerHook}, that is the request / observation span. An invalid (no active trace) context
 * yields the empty string.
 *
 * @since 10.0.0
 */
public final class TraceContext {

	private static final Logger LOG = Logger.getLogger(TraceContext.class.getName());

	/** {@code true} if {@code io.opentelemetry.api.trace.Span} was found on the classpath at class-load time. */
	private static final boolean AVAILABLE;

	private static final Method SPAN_CURRENT;
	private static final Method SPAN_GET_SPAN_CONTEXT;
	private static final Method CTX_IS_VALID;
	private static final Method CTX_GET_TRACE_ID;
	private static final Method CTX_GET_SPAN_ID;

	static {
		boolean available = false;
		Method spanCurrent = null;
		Method getSpanContext = null;
		Method isValid = null;
		Method getTraceId = null;
		Method getSpanId = null;
		try {
			Class<?> span = Class.forName("io.opentelemetry.api.trace.Span");
			Class<?> spanContext = Class.forName("io.opentelemetry.api.trace.SpanContext");
			spanCurrent = span.getMethod("current");
			getSpanContext = span.getMethod("getSpanContext");
			isValid = spanContext.getMethod("isValid");
			getTraceId = spanContext.getMethod("getTraceId");
			getSpanId = spanContext.getMethod("getSpanId");
			available = true;
		} catch (Exception e) {
			LOG.fine(() -> "OpenTelemetry API not found on classpath — trace-id log fields render empty: " + e.getMessage());
		}
		AVAILABLE = available;
		SPAN_CURRENT = spanCurrent;
		SPAN_GET_SPAN_CONTEXT = getSpanContext;
		CTX_IS_VALID = isValid;
		CTX_GET_TRACE_ID = getTraceId;
		CTX_GET_SPAN_ID = getSpanId;
	}

	private TraceContext() {}

	/**
	 * Returns whether the OpenTelemetry trace API is available on the runtime classpath.
	 *
	 * <p>
	 * When {@code false}, {@link #currentTraceId()} and {@link #currentSpanId()} always return the empty
	 * string.
	 *
	 * @return {@code true} if {@code io.opentelemetry.api.trace.Span} was found at class-load time.
	 */
	public static boolean isAvailable() {
		return AVAILABLE;
	}

	/**
	 * Returns the trace id of the OTel span active on the current thread.
	 *
	 * @return The 32-char hex trace id, or the empty string when OTel is absent or no valid span is active.
	 */
	public static String currentTraceId() {
		return field(CTX_GET_TRACE_ID);
	}

	/**
	 * Returns the span id of the OTel span active on the current thread.
	 *
	 * @return The 16-char hex span id, or the empty string when OTel is absent or no valid span is active.
	 */
	public static String currentSpanId() {
		return field(CTX_GET_SPAN_ID);
	}

	@SuppressWarnings({
		"java:S3011" // Reflective access to the OTel trace API — intentional; OpenTelemetry is not a compile dep.
	})
	private static String field(Method accessor) {
		if (! AVAILABLE)
			return "";
		try {
			Object span = SPAN_CURRENT.invoke(null);
			if (span == null)
				return "";
			Object ctx = SPAN_GET_SPAN_CONTEXT.invoke(span);
			if (ctx == null || ! ((Boolean) CTX_IS_VALID.invoke(ctx)).booleanValue())
				return "";
			Object v = accessor.invoke(ctx);
			return v == null ? "" : v.toString();
		} catch (Exception e) {
			LOG.fine(() -> "OTel Span.current() trace-context read failed: " + e.getMessage());
			return "";
		}
	}
}
