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
 * Metrics SPI for {@code juneau-rest-server} &mdash; per-request observability hook backed by a
 * caller-supplied {@link org.apache.juneau.rest.server.metrics.MetricsRecorder} bean.
 *
 * <h5 class='topic'>Opt-in</h5>
 *
 * <p>
 * The SPI is off by default. A fresh {@code RestContext} built without any
 * {@code @Bean MetricsRecorder} entry uses {@link org.apache.juneau.rest.server.metrics.NoOpMetricsRecorder}
 * &mdash; no downstream registry is touched, no per-request allocation is made. Consumers opt in by
 * supplying a bridge implementation:
 *
 * <ul>
 * 	<li>{@code juneau-rest-server-metrics-micrometer} ships {@code MicrometerMetricsRecorder} backed by an
 * 		{@code io.micrometer.core.instrument.MeterRegistry}.
 * 	<li>Custom backends (Dropwizard {@code MetricRegistry}, application-specific stat counters, etc.)
 * 		can implement {@link org.apache.juneau.rest.server.metrics.MetricsRecorder} directly &mdash; the SPI
 * 		intentionally has no Micrometer / OpenTelemetry dependency on the core surface.
 * </ul>
 *
 * <h5 class='topic'>Event shape</h5>
 *
 * <p>
 * Every {@code @RestOp} invocation fires exactly one
 * {@link org.apache.juneau.rest.server.metrics.MetricsRecorder#record(String,String,String,int,java.time.Duration,Throwable,String,String) record(...)} call &mdash; happy path
 * and exception path alike. The event carries the op name, the HTTP method, the {@code @RestOp} path
 * template (so consumers can use it as a bounded-cardinality {@code uri} tag), the response status
 * code, the wall-clock elapsed time, and the thrown {@code Throwable} (or {@code null} on success).
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.metrics.MetricsRecorder}
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.metrics.NoOpMetricsRecorder}
 * 	<li class='jp'>{@link org.apache.juneau.rest.server.tracing.TracerHook}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerObservability">REST Server &mdash; Observability (Micrometer + OpenTelemetry)</a>
 * </ul>
 *
 * @since 10.0.0
 */
package org.apache.juneau.rest.server.metrics;
