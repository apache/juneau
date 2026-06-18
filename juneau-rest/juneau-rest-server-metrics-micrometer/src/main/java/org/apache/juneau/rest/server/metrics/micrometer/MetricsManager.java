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

import org.apache.juneau.rest.server.*;

import io.micrometer.core.instrument.*;

/**
 * Shared worker for the {@code /metrics} management endpoint:  resolves a consumer-provided Micrometer
 * {@link MeterRegistry} from the host bean store and renders a scrape payload.
 *
 * <p>
 * Consistent with {@code micrometer-core} being a {@code provided} dependency and Juneau's
 * explicit-over-magic stance, this adapter <b>strictly consumes a consumer-provided registry</b> &mdash; it
 * never auto-registers a default.  When no {@link MeterRegistry} bean is present it degrades cleanly
 * (returns {@code null}, which the endpoints surface as HTTP 501).
 *
 * <p>
 * A Prometheus text scrape is produced when the resolved registry is a {@code PrometheusMeterRegistry}.
 * That type lives in the separate {@code micrometer-registry-prometheus} artifact which this module does
 * <b>not</b> depend on, so its {@code scrape()} method is invoked <b>reflectively</b> &mdash; keeping the
 * registry choice entirely the consumer's, with no extra dependency leaked here.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link MicrometerMetricsRecorder}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ManagementSurface">Management Surface</a>
 * </ul>
 *
 * @since 10.0.0
 */
public class MetricsManager {

	/**
	 * Resolves the consumer-provided {@link MeterRegistry} from the host context's bean store.
	 *
	 * @param context The REST context whose bean store is searched.  May be <jk>null</jk>.
	 * @return The registry, or <jk>null</jk> if none is registered (or the context is <jk>null</jk>).
	 */
	@SuppressWarnings({
		"resource" // The MeterRegistry is a shared, consumer-owned bean borrowed from the bean store; this adapter must not close it.
	})
	public MeterRegistry resolveRegistry(RestContext context) {
		if (context == null)
			return null;
		return context.getBeanStore().getBean(MeterRegistry.class).orElse(null);
	}

	/**
	 * Renders a Prometheus text scrape from the registry, if it is a {@code PrometheusMeterRegistry}.
	 *
	 * <p>
	 * Invoked reflectively to avoid a hard dependency on {@code micrometer-registry-prometheus}.  Returns
	 * <jk>null</jk> when the registry is <jk>null</jk>, is not a Prometheus registry, or does not expose a
	 * no-arg {@code scrape()} method &mdash; the endpoints surface that as HTTP 501.
	 *
	 * @param registry The resolved registry, or <jk>null</jk>.
	 * @return The Prometheus exposition text, or <jk>null</jk> if no scrape is available.
	 */
	public String scrape(MeterRegistry registry) {
		if (registry == null)
			return null;
		try {
			var m = registry.getClass().getMethod("scrape");
			var out = m.invoke(registry);
			return out == null ? null : out.toString();
		} catch (@SuppressWarnings("unused") NoSuchMethodException e) {
			return null;  // Not a scrapeable (Prometheus) registry — caller degrades to 501.
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to scrape MeterRegistry " + registry.getClass().getName(), e);
		}
	}
}
