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
package org.apache.juneau.rest.health;

import static java.util.concurrent.TimeUnit.*;
import static org.apache.juneau.commons.utils.StringUtils.*;

import java.time.*;
import java.util.*;
import java.util.Map.*;
import java.util.concurrent.*;

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.servlet.*;

/**
 * Built-in health/readiness/liveness probe resource.
 *
 * @since 9.5.0
 */
@Rest(paths={"/healthz","/readyz","/livez"})
public class BasicHealthResource extends BasicRestServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * Health probe endpoint.
	 *
	 * @param res The response.
	 * @return Aggregated health payload.
	 */
	@RestGet(path="/healthz")
	public HealthResponse healthz(RestResponse res) {
		return aggregate(null, res);
	}

	/**
	 * Readiness probe endpoint.
	 *
	 * @param res The response.
	 * @return Aggregated health payload.
	 */
	@RestGet(path="/readyz")
	public HealthResponse readyz(RestResponse res) {
		return aggregate(HealthProbe.READY, res);
	}

	/**
	 * Liveness probe endpoint.
	 *
	 * @param res The response.
	 * @return Aggregated health payload.
	 */
	@RestGet(path="/livez")
	public HealthResponse livez(RestResponse res) {
		return aggregate(HealthProbe.LIVE, res);
	}

	private HealthResponse aggregate(HealthProbe probe, RestResponse res) {
		var out = new LinkedHashMap<String,ComponentHealth>();
		var timeout = getContext().getBeanStore().getBean(HealthProbeSettings.class).map(HealthProbeSettings::getTimeout).orElse(Duration.ofSeconds(1));
		var timeoutMillis = Math.max(1L, timeout.toMillis());

		for (var e : indicators().entrySet()) {
			var indicator = e.getValue();
			if (probe != null && !indicator.probes().contains(probe))
				continue;
			var componentName = firstNonEmpty(e.getKey(), indicator.getClass().getSimpleName());
			var component = runIndicator(componentName, indicator, timeoutMillis);
			out.put(componentName, component);
		}

		var status = summarize(out.values());
		res.setStatus(status == HealthStatus.DOWN ? 503 : 200);
		return new HealthResponse(status, out);
	}

	/**
	 * Returns indicators to evaluate for this request.
	 *
	 * @return Indicator map keyed by bean name.
	 */
	protected Map<String,HealthIndicator> indicators() {
		return getContext().getBeanStore().getBeansOfType(HealthIndicator.class);
	}

	private static ComponentHealth runIndicator(String name, HealthIndicator indicator, long timeoutMillis) {
		var future = CompletableFuture.supplyAsync(indicator::check);
		try {
			var h = future.get(timeoutMillis, MILLISECONDS);
			return ComponentHealth.from(h);
		} catch (TimeoutException e) {
			future.cancel(true);
			return ComponentHealth.from(Health.down(name, e).detail("error", "Health check timed out after " + timeoutMillis + "ms").build());
		} catch (ExecutionException e) {
			return ComponentHealth.from(Health.down(name, e.getCause()).build());
		} catch (Throwable e) {
			return ComponentHealth.from(Health.down(name, e).build());
		}
	}

	private static HealthStatus summarize(Collection<ComponentHealth> components) {
		var hasUnknown = false;
		for (var c : components) {
			if (c.status == HealthStatus.DOWN)
				return HealthStatus.DOWN;
			if (c.status == HealthStatus.UNKNOWN)
				hasUnknown = true;
		}
		return hasUnknown ? HealthStatus.UNKNOWN : HealthStatus.UP;
	}

	/**
	 * Probe response payload.
	 */
	public static class HealthResponse {
		private final HealthStatus status;
		private final Map<String,ComponentHealth> components;
		HealthResponse(HealthStatus status, Map<String,ComponentHealth> components) {
			this.status = status;
			this.components = components;
		}
		public HealthStatus getStatus() { return status; }
		public Map<String,ComponentHealth> getComponents() { return components; }
	}

	/**
	 * Component payload in the response.
	 */
	public static class ComponentHealth {
		private final HealthStatus status;
		private final Map<String,Object> details;
		ComponentHealth(HealthStatus status, Map<String,Object> details) {
			this.status = status;
			this.details = details;
		}
		static ComponentHealth from(Health h) {
			var details = new LinkedHashMap<>(h.getDetails());
			if (h.getError() != null)
				details.putIfAbsent("error", h.getError().toString());
			return new ComponentHealth(h.getStatus(), details);
		}
		public HealthStatus getStatus() { return status; }
		public Map<String,Object> getDetails() { return details; }
	}
}
