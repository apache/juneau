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
 * Micrometer bridge for the {@code juneau-rest-server}
 * {@link org.apache.juneau.rest.server.metrics.MetricsRecorder} SPI.
 *
 * <p>
 * This module is opt-in and ships a single class &mdash;
 * {@link org.apache.juneau.rest.server.metrics.micrometer.MicrometerMetricsRecorder} &mdash; that bridges
 * per-request metric events from {@code juneau-rest-server} to a Micrometer
 * {@link io.micrometer.core.instrument.MeterRegistry MeterRegistry}. Consumers wire one as a
 * {@code @Bean MetricsRecorder} on their resource and pick the Micrometer registry that matches
 * their downstream (Prometheus, StatsD, JMX, Datadog, NewRelic, etc.).
 *
 * <h5 class='topic'>Containment</h5>
 *
 * <p>
 * The {@code io.micrometer:micrometer-core} dependency is declared in {@code provided} scope on
 * this module's POM. Consumers that want Micrometer-backed metrics must add {@code micrometer-core}
 * themselves at the version of their choice along with their preferred {@code MeterRegistry}. This
 * way:
 * <ul>
 * 	<li>The core {@code juneau-rest-server} jar stays Micrometer-free.
 * 	<li>Services that never opt in pay zero classpath cost.
 * 	<li>Services that DO opt in pick their own Micrometer version (security upgrades, CVE patches)
 * 		without waiting for a Juneau release.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.metrics.micrometer.MicrometerMetricsRecorder}
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.metrics.MetricsRecorder}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerObservability">REST Server &mdash; Observability (Micrometer + OpenTelemetry)</a>
 * 	<li class='link'><a class="doclink" href="https://micrometer.io/">Micrometer</a>
 * </ul>
 *
 * @since 10.0.0
 */
package org.apache.juneau.rest.server.metrics.micrometer;
