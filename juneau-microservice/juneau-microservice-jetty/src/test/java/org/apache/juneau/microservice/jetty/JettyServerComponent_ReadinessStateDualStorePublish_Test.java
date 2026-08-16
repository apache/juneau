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
package org.apache.juneau.microservice.jetty;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.server.health.*;
import org.eclipse.jetty.ee11.servlet.*;
import org.eclipse.jetty.server.*;
import org.junit.jupiter.api.*;

/**
 * READY-394: verifies that {@link JettyServerComponent} publishes one lifecycle-owned {@link ReadinessState}
 * instance into both {@code ms.getBeanStore()} and the auto-mounted {@link HealthServlet}'s own
 * {@code RestContext} bean store, instead of every default-configured microservice falling back to the
 * JVM-wide {@link ReadinessState#shared()} singleton.
 *
 * <h5 class='section'>Regression covered:</h5>
 * <p>
 * Before this fix, {@code JettyServerComponent.onStart/onStop} flipped {@code ReadinessState.resolve(store)}
 * without ever registering a bean back into {@code store}, and {@link HealthAggregator} read from the probe
 * servlet's own (unrelated) bean store.  Both sides therefore always fell back to {@link ReadinessState#shared()}
 * on the default (no app {@code @Bean ReadinessState}) path, so stopping any one default-configured
 * microservice flipped {@code /readyz} for every other one in the same JVM.
 *
 * <p>
 * These tests deliberately verify the bean-store publish and the exact {@link ReadinessState#resolve(BeanStore)}
 * resolution that {@link HealthAggregator#aggregate} performs, rather than round-tripping real HTTP requests
 * through the Jetty connector: this module's {@code @Rest(paths={"/healthz","/readyz","/livez"})} multi-path
 * auto-mount does not currently route those requests to the matching {@code @RestGet} operation (a pre-existing
 * mount/dispatch issue unrelated to READY-394 -- see the READY-394 final report). Testing the resolution
 * directly still exercises the exact mechanism the probe depends on, without that unrelated flakiness.
 *
 * @since 10.0.0
 */
@org.apache.juneau.testing.JettyMicroserviceTest
@SuppressWarnings("resource")  // Microservice/Server instances are test fixtures managed by the test lifecycle; explicit close is not needed for these assertions.
class JettyServerComponent_ReadinessStateDualStorePublish_Test extends TestBase {

	private static Microservice create(Class<?>... configurations) throws Exception {
		var classes = new Class<?>[configurations.length + 1];
		System.arraycopy(configurations, 0, classes, 0, configurations.length);
		classes[configurations.length] = JettyConfiguration.class;
		return Microservice.create().configurations(classes).build();
	}

	private static Server ephemeralServer() {
		var server = new Server();
		var connector = new ServerConnector(server);
		connector.setPort(0);
		server.addConnector(connector);
		var ctx = new ServletContextHandler();
		ctx.setContextPath("/");
		server.setAttribute("ServletContextHandler", ctx);
		server.setHandler(ctx);
		server.setStopTimeout(0L);
		return server;
	}

	private static void stopQuietly(Microservice ms) {
		try {
			ms.stop();
		} catch (@SuppressWarnings("unused") Exception e) {
			// Best-effort cleanup; the test has already asserted or failed by this point.
		}
	}

	private static HealthServlet findHealthServlet(Microservice ms) {
		for (var servlet : ms.getBeanStore().getBeansOfType(jakarta.servlet.Servlet.class).values())
			if (servlet instanceof HealthServlet hs)
				return hs;
		throw new AssertionError("Expected HealthProbeConfiguration to register a HealthServlet bean");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// A.  Two default-configured microservices in one JVM must not share a ReadinessState instance.
	//-----------------------------------------------------------------------------------------------------------------

	@Configuration
	static class A_Config {
		@Bean Server jettyServer() { return ephemeralServer(); }
	}

	@Test
	void a01_twoServices_defaultPath_readinessNotSharedAcrossServices() throws Exception {
		var msA = create(A_Config.class);
		var msB = create(A_Config.class);
		var aStopped = false;
		try {
			msA.start();
			msB.start();

			var rsA = msA.getBeanStore().getBean(ReadinessState.class)
				.orElseThrow(() -> new AssertionError("Expected onStart() to publish a ReadinessState bean into msA's bean store"));
			var rsB = msB.getBeanStore().getBean(ReadinessState.class)
				.orElseThrow(() -> new AssertionError("Expected onStart() to publish a ReadinessState bean into msB's bean store"));
			assertNotSame(rsA, rsB, "Two default-configured microservices in the same JVM must not share one ReadinessState instance");
			assertTrue(rsA.isReady(), "A should be ready after its own start()");
			assertTrue(rsB.isReady(), "B should be ready after its own start()");

			msA.stop();
			aStopped = true;
			assertFalse(rsA.isReady(), "A should be out of service after its own stop()");
			assertTrue(rsB.isReady(), "B must be unaffected by A's stop() -- no cross-service leakage via shared()");
		} finally {
			if (! aStopped)
				stopQuietly(msA);
			stopQuietly(msB);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// B.  The health-probe servlet's own RestContext bean store holds the SAME instance as ms.getBeanStore(),
	//     and resolve() against it reflects flips made through ms.getBeanStore() -- the dual-store publish itself.
	//-----------------------------------------------------------------------------------------------------------------

	@Configuration
	static class B_Config {
		@Bean Server jettyServer() { return ephemeralServer(); }
	}

	@Test
	void b01_healthServletBeanStore_holdsSameInstance_asMicroserviceBeanStore() throws Exception {
		var ms = create(B_Config.class, HealthProbeConfiguration.class);
		try {
			ms.start();
			var rsFromMsStore = ms.getBeanStore().getBean(ReadinessState.class)
				.orElseThrow(() -> new AssertionError("Expected onStart() to publish a ReadinessState bean into ms's bean store"));

			var probeBeanStore = findHealthServlet(ms).getContext().getBeanStore();
			var rsFromProbeStore = probeBeanStore.getBean(ReadinessState.class)
				.orElseThrow(() -> new AssertionError("Expected the probe servlet's own RestContext bean store to hold a ReadinessState bean"));
			assertSame(rsFromMsStore, rsFromProbeStore,
				"The probe servlet's RestContext bean store must hold the identical instance published into ms.getBeanStore()");
		} finally {
			stopQuietly(ms);
		}
	}

	@Test
	void b02_probeResolution_reflectsInstanceFlippedViaMicroserviceBeanStore() throws Exception {
		var ms = create(B_Config.class, HealthProbeConfiguration.class);
		try {
			ms.start();
			var probeBeanStore = findHealthServlet(ms).getContext().getBeanStore();

			// This is exactly what HealthAggregator.aggregate() calls for the READY probe.
			assertTrue(ReadinessState.resolve(probeBeanStore).isReady(), "Expected the probe to resolve as ready right after start()");

			// Flip the SAME per-service instance published into ms.getBeanStore().  If the probe servlet's own
			// RestContext bean store didn't also receive this instance, resolve() here would still fall through
			// to shared()'s untouched state and stay ready.
			ms.getBeanStore().getBean(ReadinessState.class).orElseThrow().markOutOfService();

			assertFalse(ReadinessState.resolve(probeBeanStore).isReady(),
				"Expected the probe's own bean store to resolve the instance flipped via ms.getBeanStore()");
		} finally {
			stopQuietly(ms);
		}
	}
}
