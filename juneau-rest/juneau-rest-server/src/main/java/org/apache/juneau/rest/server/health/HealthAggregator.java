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

import static java.util.concurrent.TimeUnit.*;
import static org.apache.juneau.commons.utils.StringUtils.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.rest.server.*;

/**
 * Shared health-probe aggregation worker bean.
 *
 * <p>
 * Holds the capability logic for evaluating {@link HealthIndicator} beans and summarizing them into a
 * probe response. The {@link HealthServlet} (servlet flavor) and {@link HealthMixin} (mixin flavor) are
 * independent {@code @Rest} classes that each delegate to a {@code HealthAggregator}, so the two
 * deployment forms cannot drift.
 *
 * @since 10.0.0
 */
public class HealthAggregator {

	/**
	 * Resolves the {@link HealthIndicator} beans registered in the given context's bean store.
	 *
	 * @param context The REST context whose bean store supplies the indicators.
	 * @return The indicator beans keyed by bean name.
	 */
	@SuppressWarnings({
		"resource" // BeanStore is not owned here; its lifecycle is managed by RestContext.
	})
	public Map<String,HealthIndicator> indicators(RestContext context) {
		return context.getBeanStore().getBeansOfType(HealthIndicator.class);
	}

	/**
	 * Evaluates the supplied indicators, summarizes their statuses, and sets the response status.
	 *
	 * @param context The REST context (supplies the {@link HealthProbeSettings} timeout).
	 * @param indicators The indicators to evaluate.
	 * @param probe The probe filter, or {@code null} to evaluate all indicators.
	 * @param res The response whose status code is set (200 healthy, 503 down).
	 * @return The aggregated health payload.
	 */
	@SuppressWarnings({
		"resource" // BeanStore is not owned here; its lifecycle is managed by RestContext.
	})
	public HealthResponse aggregate(RestContext context, Map<String,HealthIndicator> indicators, HealthProbe probe, RestResponse res) {
		var out = new LinkedHashMap<String,ComponentHealth>();
		var timeout = context.getBeanStore().getBean(HealthProbeSettings.class).map(HealthProbeSettings::getTimeout).orElse(Duration.ofSeconds(1));
		var timeoutMillis = Math.max(1L, timeout.toMillis());

		for (var e : indicators.entrySet()) {
			var indicator = e.getValue();
			if (probe != null && !indicator.probes().contains(probe))
				continue;
			var componentName = firstNonEmpty(e.getKey(), indicator.getClass().getSimpleName());
			out.put(componentName, runIndicator(componentName, indicator, timeoutMillis));
		}

		var status = summarize(out.values());
		res.setStatus(status == HealthStatus.DOWN ? 503 : 200);
		return new HealthResponse(status, out);
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
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return ComponentHealth.from(Health.down(name, e).build());
		} catch (Exception e) {
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

		/**
		 * @return The aggregated probe status.
		 */
		public HealthStatus getStatus() { return status; }

		/**
		 * @return The per-component health entries.
		 */
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

		/**
		 * @return The component status.
		 */
		public HealthStatus getStatus() { return status; }

		/**
		 * @return The component detail map.
		 */
		public Map<String,Object> getDetails() { return details; }
	}
}
