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
package org.apache.juneau.rest.server.health;

import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.health.HealthAggregator.*;
import org.apache.juneau.rest.server.servlet.*;

/**
 * Mixin flavor of the built-in health/readiness/liveness probe capability.
 *
 * <p>
 * Composes the {@code /healthz}, {@code /readyz}, and {@code /livez} probe endpoints into a host
 * resource via {@link Rest#mixins() @Rest(mixins=HealthMixin.class)}. The ops self-pin their absolute
 * paths so they merge into the host's namespace. Delegates to a shared {@link HealthAggregator} worker
 * bean &mdash; the same logic the {@link HealthServlet} servlet flavor uses.
 *
 * <p>
 * Indicators are resolved from the host's bean store, so a host registers its own
 * {@link HealthIndicator} beans (e.g. via Spring {@code @Bean} or a Juneau {@code BeanStore}).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link HealthServlet}
 * 	<li class='jc'>{@link HealthAggregator}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Composition (mixins, paths)</a>
 * </ul>
 *
 * @since 10.0.0
 */
@Rest
public class HealthMixin extends RestMixin {

	private final HealthAggregator aggregator = new HealthAggregator();

	/**
	 * Health probe endpoint.
	 *
	 * @param req The request.
	 * @param res The response.
	 * @return Aggregated health payload.
	 */
	@RestGet(path="/healthz")
	public HealthResponse healthz(RestRequest req, RestResponse res) {
		return aggregator.aggregate(req.getContext(), aggregator.indicators(req.getContext()), null, res);
	}

	/**
	 * Readiness probe endpoint.
	 *
	 * @param req The request.
	 * @param res The response.
	 * @return Aggregated health payload.
	 */
	@RestGet(path="/readyz")
	public HealthResponse readyz(RestRequest req, RestResponse res) {
		return aggregator.aggregate(req.getContext(), aggregator.indicators(req.getContext()), HealthProbe.READY, res);
	}

	/**
	 * Liveness probe endpoint.
	 *
	 * @param req The request.
	 * @param res The response.
	 * @return Aggregated health payload.
	 */
	@RestGet(path="/livez")
	public HealthResponse livez(RestRequest req, RestResponse res) {
		return aggregator.aggregate(req.getContext(), aggregator.indicators(req.getContext()), HealthProbe.LIVE, res);
	}
}
