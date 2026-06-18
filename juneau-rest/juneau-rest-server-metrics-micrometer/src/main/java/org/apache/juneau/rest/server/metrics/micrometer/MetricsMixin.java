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
package org.apache.juneau.rest.server.metrics.micrometer;

import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;

/**
 * Mixin flavor of the {@code /metrics} endpoint:  renders a Prometheus scrape from a consumer-provided
 * Micrometer {@link io.micrometer.core.instrument.MeterRegistry} resolved from the host bean store.
 *
 * <p>
 * Composes into a host resource via {@link Rest#mixins() @Rest(mixins=MetricsMixin.class)} and delegates to
 * a shared {@link MetricsManager} worker.  When no registry bean is present, or the registry is not a
 * Prometheus registry, the endpoint degrades to HTTP 501 (Not Implemented) rather than failing &mdash;
 * consistent with {@code micrometer-core} being {@code provided} and the registry choice being the
 * consumer's.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link MetricsManager}
 * 	<li class='jc'>{@link MicrometerMetricsRecorder}
 * </ul>
 *
 * @since 10.0.0
 */
@Rest
public class MetricsMixin extends RestMixin {

	private final MetricsManager manager = new MetricsManager();

	/**
	 * [GET /metrics] - Prometheus scrape of the consumer-provided MeterRegistry.
	 *
	 * @param req The HTTP request.
	 * @return The Prometheus exposition text.
	 * @throws NotImplemented If no scrapeable {@link io.micrometer.core.instrument.MeterRegistry} bean is registered.
	 */
	@RestGet(
		path="/metrics",
		summary="Metrics scrape",
		description="Renders a Prometheus text scrape from a consumer-provided Micrometer MeterRegistry bean."
	)
	public String getMetrics(RestRequest req) {
		var registry = manager.resolveRegistry(req.getContext());
		var scrape = manager.scrape(registry);
		if (scrape == null)
			throw new NotImplemented("No scrapeable MeterRegistry is registered (a PrometheusMeterRegistry bean is required).");
		return scrape;
	}
}
