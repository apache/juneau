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
 * OpenTelemetry bridge for the {@code juneau-rest-server}
 * {@link org.apache.juneau.rest.tracing.TracerHook} SPI.
 *
 * <p>
 * This module is opt-in and ships
 * {@link org.apache.juneau.rest.tracing.otel.OtelTracerHook} &mdash; a bridge that creates one
 * OpenTelemetry {@link io.opentelemetry.api.trace.Span Span} per {@code @RestOp} call, populates the
 * canonical HTTP semantic-convention attributes ({@code http.request.method},
 * {@code http.response.status_code}, {@code http.route}), and continues an inbound W3C trace context
 * carried on the {@code traceparent} / {@code tracestate} request headers.
 *
 * <h5 class='topic'>Containment</h5>
 *
 * <p>
 * The {@code io.opentelemetry:opentelemetry-api} dependency is declared in {@code provided} scope on
 * this module's POM. Consumers that want OpenTelemetry-backed tracing must add
 * {@code opentelemetry-api} themselves at the version of their choice along with an SDK / exporter
 * combination matching their downstream tracing backend (Jaeger, Zipkin, OTLP, etc.). This way:
 * <ul>
 * 	<li>The core {@code juneau-rest-server} jar stays OpenTelemetry-free.
 * 	<li>Services that never opt in pay zero classpath cost.
 * 	<li>Services that DO opt in pick their own OpenTelemetry version (semantic-convention upgrades,
 * 		security patches) without waiting for a Juneau release.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link org.apache.juneau.rest.tracing.otel.OtelTracerHook}
 * 	<li class='jc'>{@link org.apache.juneau.rest.tracing.TracerHook}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerObservability">REST Server &mdash; Observability (Micrometer + OpenTelemetry)</a>
 * 	<li class='link'><a class="doclink" href="https://opentelemetry.io/docs/specs/semconv/http/http-spans/">OpenTelemetry HTTP semantic conventions</a>
 * 	<li class='link'><a class="doclink" href="https://www.w3.org/TR/trace-context/">W3C Trace Context</a>
 * </ul>
 *
 * @since 10.0.0
 */
package org.apache.juneau.rest.tracing.otel;
