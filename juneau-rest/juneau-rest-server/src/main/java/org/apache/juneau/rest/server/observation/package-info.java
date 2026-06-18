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
 * Explicit programmatic observation API for {@code juneau-rest-server} &mdash; custom (non-request)
 * metrics + tracing around an arbitrary block of work.
 *
 * <h5 class='topic'>Why explicit (no {@code @Observed} annotation)</h5>
 *
 * <p>
 * Juneau has no method-interception / AOP substrate (no dynamic proxying of arbitrary beans, no
 * bytecode weaving, no annotation-processor agent). Rather than introduce one to support an
 * {@code @Observed}-style annotation, custom observations are <b>explicit</b>: application code wraps a
 * block in a try-with-resources over {@link org.apache.juneau.rest.server.observation.Observations#observe(String, String)}.
 * This is consistent with the framework's explicit-over-magic philosophy &mdash; the same reason the
 * curated dependency bundles register nothing automatically.
 *
 * <h5 class='topic'>Reuses the request-path SPIs</h5>
 *
 * <p>
 * Observations are recorded through the very same two SPIs the request path uses &mdash;
 * {@link org.apache.juneau.rest.server.metrics.MetricsRecorder} (via its non-request
 * {@code record(name, tags, elapsed, error)} default method) and
 * {@link org.apache.juneau.rest.server.tracing.TracerHook} (via its non-request {@code startSpan(name)}
 * default method) &mdash; and the same comma-separated {@code metricName} / {@code metricTags} model.
 * The shipped bridges ({@code MicrometerMetricsRecorder}, {@code OtelTracerHook}) override those default
 * methods; bridges that do not simply skip custom observations.
 *
 * <h5 class='topic'>No-backend contract</h5>
 *
 * <p>
 * When no backend is installed, {@link org.apache.juneau.rest.server.observation.Observations#observe(String, String)}
 * returns {@link org.apache.juneau.rest.server.observation.Observation#NOOP} &mdash; no timestamp taken,
 * no span opened, zero allocation beyond the try-with-resources scaffolding.
 *
 * @since 10.0.0
 */
package org.apache.juneau.rest.server.observation;
