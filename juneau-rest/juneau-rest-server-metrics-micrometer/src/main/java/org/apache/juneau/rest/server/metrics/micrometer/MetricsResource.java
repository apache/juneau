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
 * Child-resource flavor of the {@code /metrics} endpoint.
 *
 * <p>
 * Mounts as a routed child via {@link Rest#children() @Rest(children=MetricsResource.class)} under the
 * subtree {@code /metrics} and delegates to a shared {@link MetricsManager} worker &mdash; the same logic
 * the {@link MetricsMixin mixin} flavor uses, so the two forms cannot drift.  Degrades to HTTP 501 when no
 * scrapeable registry bean is present.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link MetricsMixin}
 * 	<li class='jc'>{@link MetricsManager}
 * </ul>
 *
 * @since 10.0.0
 */
@Rest(path="/metrics")
public class MetricsResource extends BasicRestResource {

	private final MetricsManager manager = new MetricsManager();

	/**
	 * [GET /] - Prometheus scrape of the consumer-provided MeterRegistry.
	 *
	 * @param req The HTTP request.
	 * @return The Prometheus exposition text.
	 * @throws NotImplemented If no scrapeable {@link io.micrometer.core.instrument.MeterRegistry} bean is registered.
	 */
	@RestGet(
		path="/*",
		summary="Metrics scrape",
		description="Renders a Prometheus text scrape from a consumer-provided Micrometer MeterRegistry bean."
	)
	public String getMetrics(RestRequest req) {
		// A routed child's bean store inherits from its parent, so the consumer's MeterRegistry bean is
		// reachable via this context's bean store.
		var registry = manager.resolveRegistry(req.getContext());
		var scrape = manager.scrape(registry);
		if (scrape == null)
			throw new NotImplemented("No scrapeable MeterRegistry is registered (a PrometheusMeterRegistry bean is required).");
		return scrape;
	}
}
