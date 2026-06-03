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
package org.apache.juneau.rest.tracing.otel;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.tracing.*;
import org.apache.juneau.rest.tracing.Scope;

import io.opentelemetry.api.*;
import io.opentelemetry.api.common.*;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.*;
import io.opentelemetry.context.propagation.*;

/**
 * {@link TracerHook} bridge that opens an OpenTelemetry {@link Span} for every {@code @RestOp} call.
 *
 * <h5 class='topic'>Span shape</h5>
 *
 * <p>
 * Each request becomes a single span with kind {@link SpanKind#SERVER}, named after the HTTP method
 * (e.g. {@code "GET"}, {@code "POST"}) per the OpenTelemetry HTTP semantic conventions. Three OTel
 * HTTP semantic-convention attributes are populated:
 * <ul>
 * 	<li><b>{@code http.request.method}</b> &mdash; uppercased HTTP method.
 * 	<li><b>{@code http.response.status_code}</b> &mdash; HTTP response status (set right before the
 * 		span is closed, so error paths get the framework's resolved status).
 * 	<li><b>{@code http.route}</b> &mdash; the {@code @RestOp} path template (e.g.
 * 		{@code "/users/{id}"}), not the raw concrete URI. Keeps trace cardinality bounded for the same
 * 		reason the Micrometer bridge uses the template as its {@code uri} tag.
 * </ul>
 *
 * <p>
 * On the exception path, the throwable is recorded via {@link Span#recordException(Throwable)}, the
 * span status is set to {@link StatusCode#ERROR}, and an {@code exception.type} attribute carries the
 * exception's simple-name.
 *
 * <h5 class='topic'>W3C trace context propagation</h5>
 *
 * <p>
 * Incoming {@code traceparent} / {@code tracestate} request headers are extracted via the configured
 * {@link TextMapPropagator} (default: {@link io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator}) so the server span continues
 * a caller-supplied distributed trace. Downstream HTTP calls made from inside the handler will pick
 * up the active span's context automatically when the consumer's outbound HTTP client honors the OTel
 * {@link Context#current() current context} (the standard OTel client instrumentations do).
 *
 * <h5 class='topic'>Wiring</h5>
 *
 * <p class='bjava'>
 * 	<ja>@Configuration</ja>
 * 	<jk>public class</jk> ObservabilityConfig {
 *
 * 		<jc>// Option A: rely on the JVM-wide GlobalOpenTelemetry (recommended).</jc>
 * 		<ja>@Bean</ja>
 * 		<jk>public</jk> TracerHook tracerHook() {
 * 			<jk>return new</jk> OtelTracerHook();
 * 		}
 *
 * 		<jc>// Option B: pass a specific OpenTelemetry instance (e.g. for tests or multi-tenant setups).</jc>
 * 		<ja>@Bean</ja>
 * 		<jk>public</jk> TracerHook tracerHook(OpenTelemetry <jv>otel</jv>) {
 * 			<jk>return new</jk> OtelTracerHook(<jv>otel</jv>);
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link TracerHook}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerObservability">REST Server &mdash; Observability (Micrometer + OpenTelemetry)</a>
 * 	<li class='link'><a class="doclink" href="https://opentelemetry.io/docs/specs/semconv/http/http-spans/">OpenTelemetry HTTP semantic conventions</a>
 * 	<li class='link'><a class="doclink" href="https://www.w3.org/TR/trace-context/">W3C Trace Context</a>
 * </ul>
 *
 * @since 9.5.0
 */
@SuppressWarnings({
	"java:S115" // ARG_xxx constants use camelCase after prefix intentionally (constructor arg name keys, not enum-style constants)
})
public class OtelTracerHook implements TracerHook {

	/** Default tracer instrumentation scope name. */
	public static final String DEFAULT_INSTRUMENTATION_NAME = "org.apache.juneau.rest";

	/** OTel HTTP semantic-convention attribute key {@code http.request.method}. */
	public static final AttributeKey<String> ATTR_HTTP_REQUEST_METHOD = AttributeKey.stringKey("http.request.method");

	/** OTel HTTP semantic-convention attribute key {@code http.response.status_code}. */
	public static final AttributeKey<Long> ATTR_HTTP_RESPONSE_STATUS_CODE = AttributeKey.longKey("http.response.status_code");

	/** OTel HTTP semantic-convention attribute key {@code http.route}. */
	public static final AttributeKey<String> ATTR_HTTP_ROUTE = AttributeKey.stringKey("http.route");

	/** OTel attribute key {@code exception.type} used when recording a thrown exception. */
	public static final AttributeKey<String> ATTR_EXCEPTION_TYPE = AttributeKey.stringKey("exception.type");

	private static final String ARG_openTelemetry = "openTelemetry";
	private static final String ARG_tracer = "tracer";
	private static final String ARG_propagator = "propagator";

	private final Tracer tracer;
	private final TextMapPropagator propagator;

	/**
	 * Constructor using {@link GlobalOpenTelemetry#get()} as the {@link OpenTelemetry} source.
	 *
	 * <p>
	 * Convenient when the deployment installs a single global {@link OpenTelemetry} (the dominant
	 * pattern with the OTel auto-instrumentation agent). Equivalent to
	 * {@code new OtelTracerHook(GlobalOpenTelemetry.get())}.
	 */
	public OtelTracerHook() {
		this(GlobalOpenTelemetry.get());
	}

	/**
	 * Constructor with an explicit {@link OpenTelemetry} instance.
	 *
	 * <p>
	 * The instance is used to obtain both the {@link Tracer} (named
	 * {@value #DEFAULT_INSTRUMENTATION_NAME}) and the {@link TextMapPropagator} used for incoming /
	 * outgoing W3C trace-context header transfer.
	 *
	 * @param openTelemetry The {@link OpenTelemetry} instance. Must not be <jk>null</jk>.
	 */
	public OtelTracerHook(OpenTelemetry openTelemetry) {
		assertArgNotNull(ARG_openTelemetry, openTelemetry);
		this.tracer = openTelemetry.getTracer(DEFAULT_INSTRUMENTATION_NAME);
		this.propagator = openTelemetry.getPropagators().getTextMapPropagator();
	}

	/**
	 * Constructor with an explicit {@link Tracer} and {@link TextMapPropagator}.
	 *
	 * <p>
	 * Useful for tests where in-memory tracer / propagator instances are wired directly without going
	 * through a full {@link OpenTelemetry} SDK assembly.
	 *
	 * @param tracer The {@link Tracer} used to build per-request spans. Must not be <jk>null</jk>.
	 * @param propagator The {@link TextMapPropagator} used for incoming / outgoing W3C trace-context
	 * 	header transfer. Must not be <jk>null</jk>.
	 */
	public OtelTracerHook(Tracer tracer, TextMapPropagator propagator) {
		this.tracer = assertArgNotNull(ARG_tracer, tracer);
		this.propagator = assertArgNotNull(ARG_propagator, propagator);
	}

	@Override /* TracerHook */
	public Scope startSpan(RestRequest request) {
		Context extracted = propagator.extract(Context.current(), request, RestRequestTextMapGetter.INSTANCE);

		String httpMethod = defaultIfBlank(request.getMethod(), "UNKNOWN");
		String route = resolveRoute(request);

		var spanBuilder = tracer.spanBuilder(httpMethod)
			.setSpanKind(SpanKind.SERVER)
			.setParent(extracted)
			.setAttribute(ATTR_HTTP_REQUEST_METHOD, httpMethod);
		if (! route.isEmpty())
			spanBuilder.setAttribute(ATTR_HTTP_ROUTE, route);

		Span span = spanBuilder.startSpan();
		Context spanContext = extracted.with(span);

		// Render the W3C trace-context headers from the server-started span's context (the only
		// point where it's reliably active) and stash them as request attributes for
		// TraceContextResponseProcessor to write on the response. The configured propagator only injects
		// traceparent when the span context is valid, and tracestate only when non-empty, so both guards are
		// handled here for free.
		stashTraceContext(request, spanContext);

		io.opentelemetry.context.Scope otelScope = spanContext.makeCurrent();

		return new OtelScope(span, otelScope);
	}

	private void stashTraceContext(RestRequest request, Context spanContext) {
		var carrier = new HashMap<String,String>(4);
		propagator.inject(spanContext, carrier, MAP_SETTER);
		var traceparent = carrier.get("traceparent");
		if (nn(traceparent))
			request.setAttribute(TraceContextResponseProcessor.ATTR_TRACEPARENT, traceparent);
		var tracestate = carrier.get("tracestate");
		if (nn(tracestate) && ! tracestate.isEmpty())
			request.setAttribute(TraceContextResponseProcessor.ATTR_TRACESTATE, tracestate);
	}

	private static final TextMapSetter<HashMap<String,String>> MAP_SETTER = (carrier, key, value) -> {
		if (nn(carrier))
			carrier.put(key, value);
	};

	/**
	 * Returns the {@link Tracer} this hook publishes spans to.
	 *
	 * @return The {@link Tracer}.
	 */
	public Tracer getTracer() { return tracer; }

	/**
	 * Returns the {@link TextMapPropagator} used to extract W3C trace-context headers from
	 * incoming requests.
	 *
	 * @return The {@link TextMapPropagator}.
	 */
	public TextMapPropagator getPropagator() { return propagator; }

	private static String resolveRoute(RestRequest request) {
		try {
			var opCtx = request.getOpContext();
			return opCtx == null ? "" : defaultIfBlank(opCtx.getPathPattern(), "");
		} catch (RuntimeException e) {
			// Defensive: opCtx.getPathPattern() reaches into the path-matcher array; protect against NPE / array bounds in odd routing setups.
			return "";
		}
	}

	private static String defaultIfBlank(String value, String fallback) {
		return isEmpty(value) ? fallback : value;
	}

	private static final class OtelScope implements Scope {

		private final Span span;
		private final io.opentelemetry.context.Scope otelScope;

		OtelScope(Span span, io.opentelemetry.context.Scope otelScope) {
			this.span = span;
			this.otelScope = otelScope;
		}

		@Override /* Scope */
		public void setStatusCode(int statusCode) {
			span.setAttribute(ATTR_HTTP_RESPONSE_STATUS_CODE, (long) statusCode);
			if (statusCode >= 500)
				span.setStatus(StatusCode.ERROR);
		}

		@Override /* Scope */
		public void setError(Throwable error) {
			if (nn(error)) {
				span.recordException(error);
				span.setAttribute(ATTR_EXCEPTION_TYPE, error.getClass().getSimpleName());
				span.setStatus(StatusCode.ERROR);
			}
		}

		@Override /* Scope */
		public void close() {
			try {
				otelScope.close();
			} finally {
				span.end();
			}
		}
	}
}
