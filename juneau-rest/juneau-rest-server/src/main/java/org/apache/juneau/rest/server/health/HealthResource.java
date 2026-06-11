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
 * Child-resource flavor of the built-in health/readiness/liveness probe capability.
 *
 * <p>
 * Mounts as a <b>routed child</b> via {@link Rest#children() @Rest(children=HealthResource.class)}
 * under a parent at the subtree {@code /health} and serves the {@code /healthz}, {@code /readyz}, and
 * {@code /livez} probe endpoints by delegating to a shared {@link HealthAggregator} worker bean &mdash;
 * the same logic the {@link HealthServlet servlet} and {@link HealthMixin mixin} flavors use, so the
 * three forms cannot drift.
 *
 * <p>
 * Indicators are resolved from the host's bean store, so a host registers its own
 * {@link HealthIndicator} beans (e.g. via Spring {@code @Bean} or a Juneau {@code BeanStore}).
 *
 * <p>
 * Extends {@link BasicRestResource} &mdash; the child-flavor mirror of {@link BasicRestServlet} that the
 * {@link HealthServlet} servlet flavor extends &mdash; so the {@link HealthAggregator.HealthResponse}
 * bean returned by the probe ops is serialized out of the box via the {@code BasicUniversalConfig}
 * serializer set (JSON/XML/HTML/&hellip;). A bare {@code RestResource} child would carry no serializers
 * and force every consumer to re-declare one; deriving from {@code BasicRestResource} keeps the child
 * flavor at parity with its servlet twin.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link HealthServlet}
 * 	<li class='jc'>{@link HealthMixin}
 * 	<li class='jc'>{@link HealthAggregator}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerCompositionMixinsAndPaths">REST Server &mdash; Mixins and Multi-Mount Paths</a>
 * </ul>
 *
 * @since 10.0.0
 */
@Rest(path="/health")
public class HealthResource extends BasicRestResource {

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
