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

/**
 * Tracing SPI for {@code juneau-rest-server} &mdash; per-request span hook backed by a
 * caller-supplied {@link org.apache.juneau.rest.tracing.TracerHook} bean.
 *
 * <h5 class='topic'>Opt-in</h5>
 *
 * <p>
 * The SPI is off by default. A fresh {@code RestContext} built without any
 * {@code @Bean TracerHook} entry uses {@link org.apache.juneau.rest.tracing.NoOpTracerHook}
 * &mdash; no tracer is reached for, no trace-context headers are parsed, no spans are opened. Consumers
 * opt in by supplying a bridge implementation:
 *
 * <ul>
 * 	<li>{@code juneau-rest-server-otel} ships {@code OtelTracerHook} backed by an
 * 		{@link io.opentelemetry.api.OpenTelemetry OpenTelemetry} instance &mdash; per-request span
 * 		creation, OTel HTTP semantic-convention attributes, and W3C {@code traceparent} /
 * 		{@code tracestate} context propagation.
 * 	<li>Custom tracers (Brave, application-specific span beans, etc.) can implement
 * 		{@link org.apache.juneau.rest.tracing.TracerHook} directly &mdash; the SPI intentionally has no
 * 		OpenTelemetry dependency on the core surface.
 * </ul>
 *
 * <h5 class='topic'>Lifecycle</h5>
 *
 * <p>
 * For every {@code @RestOp} invocation, the framework opens a {@link org.apache.juneau.rest.tracing.Scope}
 * via {@link org.apache.juneau.rest.tracing.TracerHook#startSpan startSpan(...)} immediately before
 * parameter resolution and closes it in a {@code finally} block after the handler returns or throws.
 * Status code and exception are reported via {@link org.apache.juneau.rest.tracing.Scope#setStatusCode}
 * and {@link org.apache.juneau.rest.tracing.Scope#setError} before {@link org.apache.juneau.rest.tracing.Scope#close}.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link org.apache.juneau.rest.tracing.TracerHook}
 * 	<li class='jc'>{@link org.apache.juneau.rest.tracing.Scope}
 * 	<li class='jc'>{@link org.apache.juneau.rest.tracing.NoOpTracerHook}
 * 	<li class='jp'>{@link org.apache.juneau.rest.metrics}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerObservability">REST Server &mdash; Observability (Micrometer + OpenTelemetry)</a>
 * </ul>
 *
 * @since 9.5.0
 */
package org.apache.juneau.rest.tracing;
