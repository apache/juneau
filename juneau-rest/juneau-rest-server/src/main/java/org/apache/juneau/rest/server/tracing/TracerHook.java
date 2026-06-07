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
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.metrics.MetricsRecorder}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerObservability">REST Server &mdash; Observability (Micrometer + OpenTelemetry)</a>
 * </ul>
 *
 * @since 10.0.0
 */
@FunctionalInterface
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
}
