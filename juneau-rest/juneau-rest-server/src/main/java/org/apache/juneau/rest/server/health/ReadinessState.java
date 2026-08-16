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

import org.apache.juneau.commons.inject.*;

/**
 * Shared, observable readiness flag consumed by {@link HealthAggregator} to gate the readiness probe.
 *
 * <p>
 * This is the building block for zero-downtime shutdown.  When a microservice begins shutting down it flips
 * the readiness state {@link #markOutOfService() out of service} <i>before</i> the listening connector stops.
 * The {@link HealthAggregator} consults this state for the {@link HealthProbe#READY READY} probe so
 * {@code /readyz} returns {@code 503} (out of service) as soon as shutdown begins &mdash; this lets a load
 * balancer / Kubernetes stop routing new traffic to the pod while in-flight requests drain.  The
 * {@link HealthProbe#LIVE LIVE} probe ({@code /livez}) is intentionally <b>not</b> gated, so Kubernetes does
 * not kill the pod mid-drain.
 *
 * <h5 class='section'>Resolution:</h5>
 * <p>
 * {@link HealthAggregator} and the embedded-server shutdown hooks resolve the state via
 * {@link #resolve(BeanStore)}: a {@code ReadinessState} bean registered in the relevant {@link BeanStore} wins;
 * otherwise the process-wide {@link #shared() shared} instance is used.  In the standalone Jetty/Tomcat
 * microservice runtime, the embedded-server lifecycle component (e.g. {@code JettyServerComponent}) constructs
 * one per-service instance on {@code start()} &mdash; reusing an app/test-supplied {@code @Bean ReadinessState}
 * if one is already registered &mdash; and explicitly publishes that <b>same</b> instance into both the
 * microservice's bean store and the auto-mounted health-probe servlet's own bean store.  The two sides then
 * observe each other's flips without ever touching {@link #shared()}, so stopping one default-configured
 * microservice does not affect any other microservice's readiness in the same JVM.  {@link #shared()} remains
 * the last-resort fallback for embeddings that skip that dual-store publish (a plain servlet container, Spring
 * Boot without this integration, or unit tests that never register a bean).
 *
 * <p>
 * This class is thread-safe; the flag is {@code volatile}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link HealthAggregator}
 * </ul>
 *
 * @since 10.0.0
 */
public final class ReadinessState {

	private static final ReadinessState SHARED = new ReadinessState();

	/**
	 * Returns the process-wide shared readiness state.
	 *
	 * <p>
	 * Used as the fallback when no {@code ReadinessState} bean is registered in the relevant {@link BeanStore}.
	 * This is <b>not</b> the standalone Jetty/Tomcat microservice path: that runtime constructs a per-service
	 * instance and publishes it into both the microservice's bean store and the health-probe servlet's bean
	 * store (see the class-level <i>Resolution</i> notes above), so two default-configured microservices in the
	 * same JVM never share readiness.  This shared instance exists only for embeddings that skip that publish
	 * step.
	 *
	 * @return The shared instance.  Never <jk>null</jk>.
	 */
	public static ReadinessState shared() {
		return SHARED;
	}

	/**
	 * Resolves the readiness state from the given bean store, falling back to the {@link #shared() shared} instance.
	 *
	 * @param beanStore The bean store to consult.  Can be <jk>null</jk> (resolves to the shared instance).
	 * @return The resolved readiness state.  Never <jk>null</jk>.
	 */
	public static ReadinessState resolve(BeanStore beanStore) {
		if (beanStore == null)
			return SHARED;
		return beanStore.getBean(ReadinessState.class).orElse(SHARED);
	}

	private volatile boolean ready = true;

	/**
	 * Constructor.
	 *
	 * <p>
	 * The new state starts {@link #isReady() ready}.
	 */
	public ReadinessState() {
		// Intentionally empty — the volatile `ready` field defaults to true.
	}

	/**
	 * Returns whether this service is currently ready to receive traffic.
	 *
	 * @return <jk>true</jk> if ready, <jk>false</jk> if out of service.
	 */
	public boolean isReady() {
		return ready;
	}

	/**
	 * Marks this service as ready to receive traffic.
	 *
	 * <p>
	 * Called when an embedded server starts so a freshly-(re)started service is ready.
	 *
	 * @return This object.
	 */
	public ReadinessState markReady() {
		ready = true;
		return this;
	}

	/**
	 * Marks this service as out of service so the readiness probe ({@code /readyz}) returns {@code 503}.
	 *
	 * <p>
	 * Called at the very beginning of shutdown, before the listening connector stops.
	 *
	 * @return This object.
	 */
	public ReadinessState markOutOfService() {
		ready = false;
		return this;
	}
}
