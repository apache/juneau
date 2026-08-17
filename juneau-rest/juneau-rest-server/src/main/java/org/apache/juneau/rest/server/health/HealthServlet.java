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
	 * Health/readiness/liveness probe endpoint.
	 *
	 * <p>
	 * [TODO-401] Serves all three mount paths ({@code /healthz}, {@code /readyz}, {@code /livez}) through one
	 * {@code /*} operation, dispatching on the request's last path segment.  A single zero-part,
	 * {@code hasRemainder=true} matcher is required because this servlet is auto-mounted at three <i>exact-match</i>
	 * top-level path-specs: a real container delivers a bare {@code GET /readyz} with a zero-segment
	 * {@code pathInfo} that no 1-segment matcher can satisfy, so three separate {@code @RestGet(path="/{probe}")}
	 * operations all 404.  The last-path-segment key ({@link #probeFor}) is the only thing that differs per mount,
	 * so it is the disambiguator.
	 *
	 * <p>
	 * This replaces the former {@code healthz(...)}/{@code readyz(...)}/{@code livez(...)} entry points; subclasses
	 * that overrode any of those must override {@link #probeFor(RestRequest)} (or this method) instead.
	 *
	 * @param req The request (its last path segment selects the probe).
	 * @param res The response.
	 * @return Aggregated health payload.
	 */
	@RestGet(path="/*")  // Sibling-servlet pin (matches FaviconServlet/VersionServlet): a zero-part, hasRemainder=true
	                     // matcher that resolves BOTH a container's zero-segment bare-mount hit AND a mock/remount remainder.
	public HealthResponse probe(RestRequest req, RestResponse res) {
		return aggregator.aggregate(getContext(), indicators(), probeFor(req), res);
	}

	/**
	 * Resolves which probe this request is for, keyed off the LAST PATH SEGMENT.
	 *
	 * <p>
	 * Prefers {@link RestRequest#getPathInfo()} when it carries a real (non-root) value &mdash; this is what
	 * {@code MockRestClient} and any remainder-based remount populate ({@code servletPath="", pathInfo="/readyz"}).
	 * Falls back to {@link RestRequest#getServletPath()} for a real container's bare exact-match hit, where
	 * {@code pathInfo} is {@code null} ({@code servletPath="/readyz", pathInfo=null}).
	 *
	 * @param req The request.
	 * @return The resolved probe, or {@code null} for {@code /healthz} (the overall aggregate).
	 */
	protected HealthProbe probeFor(RestRequest req) {
		var hint = req.getPathInfo();
		if (hint == null || hint.isEmpty() || "/".equals(hint))
			hint = req.getServletPath();
		var last = lastSegment(hint);
		if ("readyz".equals(last) || "ready".equals(last))
			return HealthProbe.READY;
		if ("livez".equals(last) || "live".equals(last))
			return HealthProbe.LIVE;
		return null; // "/healthz" (or anything else this instance is mounted at) -- overall aggregate.
	}

	private static String lastSegment(String path) {
		if (path == null)
			return "";
		var p = path;
		while (p.length() > 1 && p.endsWith("/"))
			p = p.substring(0, p.length() - 1);
		var i = p.lastIndexOf('/');
		return i < 0 ? p : p.substring(i + 1);
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
