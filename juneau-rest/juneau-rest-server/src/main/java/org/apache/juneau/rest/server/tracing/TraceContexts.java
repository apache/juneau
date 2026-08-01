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

import static org.apache.juneau.commons.utils.AssertionUtils.*;

/**
 * Neutral, dependency-free helper for stamping a caller's current trace context into an arbitrary
 * {@link TraceContextCarrier}.
 *
 * <p>
 * {@code juneau-rest-server} has no first-party call site for this helper &mdash; it exists so a
 * future MCP-client (or any other non-HTTP caller) adapter can inject the active trace context into
 * whatever carrier it builds (for example a {@code RequestMeta} being sent as a JSON-RPC request's
 * {@code params._meta}) without that adapter depending on OpenTelemetry. The adapter depends only on
 * this class and the installed {@link TracerHook}.
 *
 * <h5 class='topic'>Off-by-default contract</h5>
 *
 * <p>
 * {@link #inject(TracerHook, TraceContextCarrier)} does nothing but null-check its arguments and
 * delegate to {@link TracerHook#inject(TraceContextCarrier)}. Against {@link NoOpTracerHook} (or any
 * bridge that does not override {@code inject}), the default no-op implementation writes nothing to
 * the carrier &mdash; only a bridge with an actual trace context to render (for example the
 * OpenTelemetry bridge in {@code juneau-rest-server-tracing-otel}) writes {@code traceparent} /
 * {@code tracestate} / {@code baggage} keys.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link TraceContextCarrier}
 * 	<li class='jc'>{@link TracerHook}
 * 	<li class='link'><a class="doclink" href="https://www.w3.org/TR/trace-context/">W3C Trace Context</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerObservability">REST Server &mdash; Observability (Micrometer + OpenTelemetry)</a>
 * </ul>
 *
 * @since 10.0.0
 */
public final class TraceContexts {

	private TraceContexts() {}

	/**
	 * Injects the caller's current trace context (as understood by <c>tracer</c>) into <c>carrier</c>.
	 *
	 * @param tracer The {@link TracerHook} whose active trace context (if any) should be rendered.
	 * 	Must not be <jk>null</jk>.
	 * @param carrier The {@link TraceContextCarrier} to write into. Must not be <jk>null</jk>.
	 */
	public static void inject(TracerHook tracer, TraceContextCarrier carrier) {
		assertArgNotNull("tracer", tracer);
		assertArgNotNull("carrier", carrier);
		tracer.inject(carrier);
	}
}
