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

import java.util.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.health.HealthAggregator.*;
import org.apache.juneau.rest.server.servlet.*;

/**
 * Servlet flavor of the built-in health/readiness/liveness probe capability.
 *
 * <p>
 * Mounts as a standalone top-level servlet at {@code /healthz}, {@code /readyz}, and {@code /livez},
 * delegating to a shared {@link HealthAggregator} worker bean. The {@link HealthMixin} mixin flavor
 * delegates to the same bean, so the two forms cannot drift.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link HealthMixin}
 * 	<li class='jc'>{@link HealthAggregator}
 * </ul>
 *
 * @serial exclude
 * @since 10.0.0
 */
@Rest(paths={"/healthz","/readyz","/livez"})
public class HealthServlet extends BasicRestServlet {
	private static final long serialVersionUID = 1L;

	private final transient HealthAggregator aggregator = new HealthAggregator();
	private transient volatile ReadinessState readinessState;

	/**
	 * Publishes the lifecycle-owned {@link ReadinessState} that this probe should observe.
	 *
	 * <p>
	 * Called by the embedded-server lifecycle component (e.g. {@code JettyServerComponent},
	 * {@code TomcatServerComponent}) before the server starts, so the per-service instance that component flips
	 * on shutdown is registered into this resource's own bean store &mdash; see {@link #initReadinessState}
	 * &mdash; closing the gap between the microservice's bean store and this servlet's bean store that
	 * {@link HealthAggregator} consults.
	 *
	 * @param state The per-service readiness state.  Ignored if <jk>null</jk>.
	 * @return This object.
	 */
	public HealthServlet publishReadinessState(ReadinessState state) {
		this.readinessState = state;
		return this;
	}

	/**
	 * Registers the {@linkplain #publishReadinessState(ReadinessState) published} readiness state (if any) into
	 * this resource's own bean store, so {@link HealthAggregator#aggregate} resolves the lifecycle-owned
	 * instance instead of falling back to {@link ReadinessState#shared()}.
	 *
	 * @param beanStore This resource's bean store.
	 */
	@RestInit
	@SuppressWarnings({
		"resource" // addBean returns this; the discarded return is the store the caller already holds
	})
	public void initReadinessState(WritableBeanStore beanStore) {
		if (readinessState != null)
			beanStore.addBean(ReadinessState.class, readinessState);
	}

	/**
	 * Health probe endpoint.
	 *
	 * @param res The response.
	 * @return Aggregated health payload.
	 */
	@RestGet(path="/healthz")
	public HealthResponse healthz(RestResponse res) {
		return aggregator.aggregate(getContext(), indicators(), null, res);
	}

	/**
	 * Readiness probe endpoint.
	 *
	 * @param res The response.
	 * @return Aggregated health payload.
	 */
	@RestGet(path="/readyz")
	public HealthResponse readyz(RestResponse res) {
		return aggregator.aggregate(getContext(), indicators(), HealthProbe.READY, res);
	}

	/**
	 * Liveness probe endpoint.
	 *
	 * @param res The response.
	 * @return Aggregated health payload.
	 */
	@RestGet(path="/livez")
	public HealthResponse livez(RestResponse res) {
		return aggregator.aggregate(getContext(), indicators(), HealthProbe.LIVE, res);
	}

	/**
	 * Returns the indicators to evaluate for this request.
	 *
	 * <p>
	 * Defaults to every {@link HealthIndicator} bean registered in this resource's bean store.
	 * Subclasses may override to supply a fixed indicator set.
	 *
	 * @return Indicator map keyed by bean name.
	 */
	protected Map<String,HealthIndicator> indicators() {
		return aggregator.indicators(getContext());
	}
}
