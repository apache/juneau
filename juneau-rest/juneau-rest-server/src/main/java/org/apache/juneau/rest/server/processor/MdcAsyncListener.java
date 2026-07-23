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
package org.apache.juneau.rest.server.processor;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;
import java.util.logging.*;

/**
 * Bridges SLF4J MDC context from the request (dispatch) thread to the {@link java.util.concurrent.CompletableFuture}
 * completion thread.
 *
 * <h5 class='topic'>Problem</h5>
 * <p>
 * When a {@code @RestOp} method returns a {@link java.util.concurrent.CompletableFuture}, the {@link AsyncResponseProcessor}
 * registers a {@link java.util.concurrent.CompletionStage#whenComplete(BiConsumer) whenComplete} callback that runs on
 * whatever thread completes the future &mdash; typically a different thread from the one that processed the request.
 * SLF4J's {@code MDC} is backed by a {@link ThreadLocal}, so any diagnostic context set by an upstream filter
 * (e.g. {@code requestId}, {@code userId}) is invisible to log statements emitted inside that callback.
 *
 * <h5 class='topic'>Solution</h5>
 * <p>
 * {@code MdcAsyncListener} provides a single factory method, {@link #wrap(BiConsumer, Map)}, that wraps a
 * {@code whenComplete} callback with the standard MDC snapshot-restore-clear pattern:
 * <ol>
 *   <li>Snapshot the request thread's MDC map via {@code MDC.getCopyOfContextMap()} at dispatch time.
 *   <li>On entry to the completion callback: save the completion thread's current MDC (if any) and install
 *       the request-thread snapshot via {@code MDC.setContextMap(snapshot)}.
 *   <li>Callback body runs with the request-thread MDC visible.
 *   <li>In the {@code finally} block: restore the completion thread's original MDC state (if it had one) or
 *       call {@code MDC.clear()} &mdash; this removes exactly the keys the listener installed without disturbing
 *       any MDC state the completion thread itself had before the callback.
 * </ol>
 *
 * <h5 class='topic'>Lazy skip</h5>
 * <p>
 * {@link #snapshot()} returns {@code null} when the request thread's MDC is empty or when SLF4J is absent.
 * {@link #wrap(BiConsumer, Map)} returns the original callback unchanged when the snapshot is {@code null}, so no
 * work is done on the completion thread for the common case of no MDC state.
 *
 * <h5 class='topic'>SLF4J detection</h5>
 * <p>
 * {@code MdcAsyncListener} uses {@link Class#forName(String)} to probe for {@code org.slf4j.MDC} at class-load
 * time. SLF4J is NOT a compile-time dependency of {@code juneau-rest-server}. If SLF4J is absent from the
 * runtime classpath, the class gracefully degrades: {@link #isAvailable()} returns {@code false},
 * {@link #snapshot()} returns {@code null}, and {@link #wrap(BiConsumer, Map)} returns the original callback.
 *
 * <h5 class='topic'>Limitation</h5>
 * <p>
 * This class bridges SLF4J MDC only. Users of Log4j2's {@code ThreadContext} directly (without the SLF4J facade)
 * will not benefit from this listener. That is documented as a v1 limitation; the SLF4J MDC is the lingua franca
 * for structured logging context propagation in the JVM ecosystem.
 *
 * @see AsyncResponseProcessor
 * @see org.apache.juneau.rest.server.RestContext#isMdcAsyncPropagation()
 * @since 10.0.0
 */
public final class MdcAsyncListener {

	private static final Logger LOG = Logger.getLogger(MdcAsyncListener.class.getName());

	/** {@code true} if {@code org.slf4j.MDC} was found on the classpath at class-load time. */
	private static final boolean AVAILABLE;

	private static final Method GET_COPY_OF_CONTEXT_MAP;
	private static final Method SET_CONTEXT_MAP;
	private static final Method CLEAR;

	/** MDC keys carrying the active trace / span id &mdash; the convention used by the OTel logback / log4j appenders. */
	static final String MDC_TRACE_ID = "trace_id";
	static final String MDC_SPAN_ID = "span_id";

	/** {@code true} if {@code io.opentelemetry.api.trace.Span} was found on the classpath at class-load time. */
	private static final boolean OTEL_AVAILABLE;

	private static final Method SPAN_CURRENT;
	private static final Method SPAN_GET_SPAN_CONTEXT;
	private static final Method CTX_IS_VALID;
	private static final Method CTX_GET_TRACE_ID;
	private static final Method CTX_GET_SPAN_ID;

	static {
		boolean available = false;
		Method copyOf = null;
		Method set = null;
		Method clr = null;
		try {
			Class<?> mdc = Class.forName("org.slf4j.MDC");
			copyOf = mdc.getMethod("getCopyOfContextMap");
			set = mdc.getMethod("setContextMap", Map.class);
			clr = mdc.getMethod("clear");
			available = true;
		} catch (Exception e) {
			LOG.fine(() -> "SLF4J MDC not found on classpath — MdcAsyncListener is a no-op: " + e.getMessage());
		}
		AVAILABLE = available;
		GET_COPY_OF_CONTEXT_MAP = copyOf;
		SET_CONTEXT_MAP = set;
		CLEAR = clr;

		boolean otel = false;
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
			otel = true;
		} catch (Exception e) {
			LOG.fine(() -> "OpenTelemetry API not found on classpath — trace/span id is not propagated across async boundaries: " + e.getMessage());
		}
		OTEL_AVAILABLE = otel;
		SPAN_CURRENT = spanCurrent;
		SPAN_GET_SPAN_CONTEXT = getSpanContext;
		CTX_IS_VALID = isValid;
		CTX_GET_TRACE_ID = getTraceId;
		CTX_GET_SPAN_ID = getSpanId;
	}

	private MdcAsyncListener() {}

	/**
	 * Returns whether SLF4J MDC is available on the runtime classpath.
	 *
	 * <p>
	 * When {@code false}, all methods on this class are no-ops.
	 *
	 * @return {@code true} if {@code org.slf4j.MDC} was found at class-load time.
	 */
	public static boolean isAvailable() {
		return AVAILABLE;
	}

	/**
	 * Takes a snapshot of the current thread's SLF4J MDC map.
	 *
	 * <p>
	 * Call this on the request/dispatch thread before submitting async work. The returned map is passed to
	 * {@link #wrap(BiConsumer, Map)} to decorate the completion callback.
	 *
	 * <p>
	 * Returns {@code null} when:
	 * <ul>
	 *   <li>SLF4J is absent from the classpath, or
	 *   <li>The current thread's MDC is {@code null} or empty (lazy-skip contract — no work is done on the
	 *       completion thread when the request had no MDC context).
	 * </ul>
	 *
	 * @return A copy of the current MDC map, or {@code null}.
	 */
	@SuppressWarnings({
		"unchecked", // Type erasure on reflective/generic cast; element type is verified at call site
		"java:S1168" // null is a meaningful lazy-skip sentinel: wrap() treats null as "no MDC to propagate"; an empty map would force unnecessary MDC installation.
	})
	public static Map<String,String> snapshot() {
		if (!AVAILABLE)
			return null;
		try {
			Map<String,String> map = (Map<String,String>) GET_COPY_OF_CONTEXT_MAP.invoke(null);
			// Enrich the snapshot with the active OTel trace/span id (under the conventional OTel
			// appender keys) so log correlation survives the async-completion hop even when the
			// completion thread has lost the OTel context. Only when an OTel span is actually active.
			map = enrichWithTraceContext(map);
			return (map == null || map.isEmpty()) ? null : map;
		} catch (Exception e) {
			LOG.fine(() -> "MDC.getCopyOfContextMap() failed: " + e.getMessage());
			return null;
		}
	}

	@SuppressWarnings({
		"java:S3011", // Reflective access to the OTel trace API — intentional; OpenTelemetry is not a compile dep.
		"java:S1168"  // null preserves the lazy-skip contract: a null map with no active trace must stay null (no MDC to propagate).
	})
	private static Map<String,String> enrichWithTraceContext(Map<String,String> map) {
		if (!OTEL_AVAILABLE)
			return map;
		try {
			Object span = SPAN_CURRENT.invoke(null);
			if (span == null)
				return map;
			Object ctx = SPAN_GET_SPAN_CONTEXT.invoke(span);
			if (ctx == null || ! ((Boolean) CTX_IS_VALID.invoke(ctx)).booleanValue())
				return map;
			var out = (map == null) ? new HashMap<String,String>(4) : new HashMap<>(map);
			out.put(MDC_TRACE_ID, String.valueOf(CTX_GET_TRACE_ID.invoke(ctx)));
			out.put(MDC_SPAN_ID, String.valueOf(CTX_GET_SPAN_ID.invoke(ctx)));
			return out;
		} catch (Exception e) {
			LOG.fine(() -> "OTel trace-context enrichment failed: " + e.getMessage());
			return map;
		}
	}

	/**
	 * Wraps a {@link BiConsumer} (typically a {@link java.util.concurrent.CompletableFuture#whenComplete
	 * whenComplete} callback) so the completion thread sees the request thread's MDC context.
	 *
	 * <p>
	 * The wrapping logic:
	 * <ol>
	 *   <li>Save the completion thread's current MDC map (usually {@code null} for pool / virtual threads).
	 *   <li>Install {@code requestMdc} on the completion thread via {@code MDC.setContextMap(requestMdc)}.
	 *   <li>Invoke {@code action} inside a {@code try} block.
	 *   <li>{@code finally}: if the completion thread had its own MDC, restore it; otherwise call
	 *       {@code MDC.clear()} — either way only the keys the listener installed are removed.
	 * </ol>
	 *
	 * <p>
	 * If {@code requestMdc} is {@code null} (e.g. request thread had no MDC, or SLF4J absent), the original
	 * {@code action} is returned unchanged — zero overhead on the completion thread.
	 *
	 * @param <T>        The future result type.
	 * @param action     The callback to wrap.
	 * @param requestMdc The MDC snapshot taken on the dispatch thread (from {@link #snapshot()}), or {@code null}.
	 * @return A wrapped callback, or {@code action} itself when wrapping is not needed.
	 */
	public static <T> BiConsumer<T,Throwable> wrap(BiConsumer<T,Throwable> action, Map<String,String> requestMdc) {
		if (!AVAILABLE || requestMdc == null)
			return action;

		return (value, error) -> {
			// Save the completion thread's pre-existing MDC (so we can restore it, not wipe it).
			var before = snapshot();
			try {
				setContextMap(requestMdc);
				action.accept(value, error);
			} finally {
				if (before != null)
					setContextMap(before);
				else
					mdcClear();
			}
		};
	}

	@SuppressWarnings({
		"java:S3011" // Reflective access to MDC methods — intentional; SLF4J is not a compile dep.
	})
	static void setContextMap(Map<String,String> map) {
		try {
			SET_CONTEXT_MAP.invoke(null, map);
		} catch (Exception e) {
			LOG.fine(() -> "MDC.setContextMap() failed: " + e.getMessage());
		}
	}

	@SuppressWarnings({
		"java:S3011" // Reflective access to MDC methods — intentional.
	})
	static void mdcClear() {
		try {
			CLEAR.invoke(null);
		} catch (Exception e) {
			LOG.fine(() -> "MDC.clear() failed: " + e.getMessage());
		}
	}
}
