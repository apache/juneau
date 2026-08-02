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
package org.apache.juneau.rest.server.tracing;

import org.apache.juneau.http.tracing.TraceContextCarrier;
import org.apache.juneau.rest.server.*;

/**
 * SPI for receiving per-request tracing events from {@code juneau-rest-server}.
 *
 * <p>
 * {@link #startSpan(RestRequest)} fires immediately before every {@code @RestOp} handler runs &mdash;
 * the returned {@link Scope} is closed by the framework after the handler completes (including the
 * exception path). Bridge implementations create whatever the downstream tracer expects (an
 * OpenTelemetry {@code io.opentelemetry.api.trace.Span}, a Brave {@code Span}, an application
 * tracing object, etc.) and surface the close / status / error transitions through the {@code Scope}.
 *
 * <h5 class='topic'>Off-by-default contract</h5>
 *
 * <p>
 * {@code juneau-rest-server} resolves the {@code TracerHook} via
 * {@code RestContext.getBeanStore().getBean(TracerHook.class)}. When no bean is supplied,
 * {@link NoOpTracerHook#INSTANCE} is used &mdash; the framework never reaches for a tracer, never
 * inspects headers for W3C trace context, and adds no per-request cost beyond a single static-field
 * read plus a single {@code AutoCloseable.close()} on a singleton noop scope. To opt in, the consumer
 * registers a {@code TracerHook} bean &mdash; typically via {@code @Bean TracerHook} on the resource
 * or its parent.
 *
 * <h5 class='topic'>Usage</h5>
 *
 * <p class='bjava'>
 * 	<ja>@Rest</ja>
 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServlet {
 *
 * 		<ja>@Bean</ja>
 * 		<jk>public</jk> TracerHook tracer(OpenTelemetry <jv>otel</jv>) {
 * 			<jk>return new</jk> OtelTracerHook(<jv>otel</jv>);
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='topic'>Lifecycle</h5>
 *
 * <p>
 * For each {@code @RestOp} invocation, the framework:
 * <ol>
 * 	<li>Calls {@link #startSpan(RestRequest)} just before parameter resolution, passing the in-flight
 * 		{@link RestRequest} so the bridge can read trace-context headers
 * 		({@code traceparent} / {@code tracestate}) and set request-derived attributes.
 * 	<li>Runs the handler (which may throw).
 * 	<li>Calls {@link Scope#setStatusCode(int)} with the resolved response status, then
 * 		{@link Scope#setError(Throwable)} if the handler threw, then {@link Scope#close()} &mdash;
 * 		always in a {@code finally} block so the span closes even on exception.
 * </ol>
 *
 * <p>
 * Implementations <b>must</b> be thread-safe &mdash; the same {@code TracerHook} bean is invoked
 * concurrently from every request thread for the lifetime of the {@code RestContext}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link NoOpTracerHook}
 * 	<li class='jc'>{@link Scope}
 * 	<li class='jc'>{@link TraceContextCarrier}
 * 	<li class='jc'>{@link TraceContextExtractor}
 * 	<li class='jc'>{@link TraceOperation}
 * 	<li class='jc'>{@link TraceContexts}
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.metrics.MetricsRecorder}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerObservability">REST Server &mdash; Observability (Micrometer + OpenTelemetry)</a>
 * </ul>
 *
 * @since 10.0.0
 */
@FunctionalInterface
@SuppressWarnings({
	"resource" // Scope is returned to the caller for try-with-resources/finally management, not closed here; Eclipse JDT @Owning warning is by design.
})
public interface TracerHook {

	/**
	 * Opens a new span for the in-flight request.
	 *
	 * @param request The in-flight {@link RestRequest}. Never <jk>null</jk>. Bridges may read headers
	 * 	for distributed-tracing context propagation (e.g. W3C {@code traceparent} / {@code tracestate}),
	 * 	the HTTP method, the path, etc. Bridges <b>must not</b> mutate the request.
	 * @return The opened {@link Scope}. Never <jk>null</jk>. Returning {@code null} is a contract
	 * 	violation &mdash; bridges that cannot open a span (e.g. because the trace context is invalid)
	 * 	should return {@link NoOpTracerHook.NoOpScope#INSTANCE} so the framework's close-in-finally
	 * 	contract still holds.
	 */
	Scope startSpan(RestRequest request);

	/**
	 * Opens a new span for a custom (non-request) observation.
	 *
	 * <p>
	 * Unlike {@link #startSpan(RestRequest)}, this entry point is <b>not</b> tied to an in-flight
	 * {@link RestRequest} &mdash; it is the substrate for the explicit programmatic observation API
	 * ({@link org.apache.juneau.rest.server.observation.Observations}) so application code can trace an
	 * arbitrary block of work that has no associated HTTP request. Bridge implementations typically open
	 * a span of kind {@code INTERNAL} (rather than {@code SERVER}) named {@code spanName}.
	 *
	 * <p>
	 * The default implementation returns {@link NoOpTracerHook.NoOpScope#INSTANCE} &mdash; a bridge that
	 * does not override this method simply does not trace custom observations, and the no-backend path
	 * stays zero-allocation. Both shipped bridges (the OpenTelemetry {@code TracerHook}) override it.
	 *
	 * @param spanName The span name (e.g. {@code "loadOrder"}). Never <jk>null</jk>; never blank.
	 * @return The opened {@link Scope}. Never <jk>null</jk> &mdash; implementations that cannot open a
	 * 	span must return {@link NoOpTracerHook.NoOpScope#INSTANCE} so the close-in-finally contract holds.
	 */
	default Scope startSpan(String spanName) {
		return NoOpTracerHook.NoOpScope.INSTANCE;
	}

	/**
	 * Opens a new span for the in-flight request, given a pre-extracted non-HTTP
	 * {@link TraceContextCarrier} and derived {@link TraceOperation}.
	 *
	 * <p>
	 * Added as a source-compatible default overload so every existing {@link TracerHook}
	 * implementation &mdash; including a bare lambda implementing only
	 * {@link #startSpan(RestRequest)} &mdash; keeps compiling unchanged. The default implementation
	 * simply delegates to {@link #startSpan(RestRequest)}, ignoring <c>carrier</c> and
	 * <c>operation</c>; a bridge only needs to override this overload if it wants to honor a
	 * non-HTTP carrier (for example MCP's {@code params._meta}) or name the span after
	 * <c>operation</c> instead of its own HTTP-derived default.
	 *
	 * @param request The in-flight {@link RestRequest}. Never <jk>null</jk>.
	 * @param carrier The {@link TraceContextCarrier} an active {@code TraceContextExtractor}
	 * 	recognized among the resolved operation arguments, or <jk>null</jk> if none was recognized
	 * 	(the bridge should then fall back to HTTP headers only).
	 * @param operation The {@link TraceOperation} an active {@code TraceContextExtractor} derived
	 * 	for this invocation. Never <jk>null</jk>; {@link TraceOperation#DEFAULT} when the extractor
	 * 	supplied no override.
	 * @return The opened {@link Scope}. Never <jk>null</jk>.
	 */
	default Scope startSpan(RestRequest request, TraceContextCarrier carrier, TraceOperation operation) {
		return startSpan(request);
	}

	/**
	 * Renders this hook's active trace context (if any) into <c>carrier</c>.
	 *
	 * <p>
	 * The substrate for {@link TraceContexts#inject(TracerHook, TraceContextCarrier)}. The default
	 * implementation is a no-op &mdash; {@link NoOpTracerHook} and any bridge that does not override
	 * this method write nothing. A bridge with an actual trace context to render (for example the
	 * OpenTelemetry bridge in {@code juneau-rest-server-tracing-otel}) overrides this to write
	 * {@code traceparent} / {@code tracestate} / {@code baggage} keys via {@link TraceContextCarrier#set(String, String)}.
	 *
	 * @param carrier The {@link TraceContextCarrier} to write into. Never <jk>null</jk>.
	 */
	default void inject(TraceContextCarrier carrier) {
		// Intentionally empty; see class-level javadoc.
	}
}
