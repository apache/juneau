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

import java.time.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.server.health.*;
import org.eclipse.jetty.ee11.servlet.*;
import org.eclipse.jetty.server.*;
import org.junit.jupiter.api.*;

/**
 * Graceful-shutdown / readiness-gating tests for {@link JettyServerComponent}.
 *
 * <p>
 * Verifies the zero-downtime shutdown contract on the Jetty side:
 * <ul>
 * 	<li>A sensible default {@code stopTimeout} ({@link JettyServerComponent#DEFAULT_STOP_TIMEOUT}) is applied to the
 * 		Jetty server when neither {@link JettySettings#getStopTimeout()} nor the {@code Jetty/stopTimeout} config
 * 		entry supplies one, so {@code server.stop()} drains in-flight requests before the connector closes.
 * 	<li>A programmatic {@link JettySettings#stopTimeout(Duration)} override wins over the default.
 * 	<li>On stop, readiness flips out of service <i>before</i> the connector stops; on start it is marked ready.
 * </ul>
 *
 * @since 10.0.0
 */
@org.apache.juneau.testing.annotations.JettyMicroserviceTest
@SuppressWarnings("resource")  // Microservice/Server instances are test fixtures managed by the test lifecycle; explicit close is not needed for these assertions.
class JettyGracefulShutdown_Test extends TestBase {

	private static Microservice create(Class<?>... configurations) throws Exception {
		var classes = new Class<?>[configurations.length + 1];
		System.arraycopy(configurations, 0, classes, 0, configurations.length);
		classes[configurations.length] = JettyConfiguration.class;
		return Microservice.create().configurations(classes).build();
	}

	// An ephemeral-port Jetty server with stopTimeout pre-set to 0 so the component default/override is observable.
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

	//-----------------------------------------------------------------------------------------------------------------
	// A.  Default stopTimeout applied when nothing else supplies one.
	//-----------------------------------------------------------------------------------------------------------------

	@Configuration
	static class A_DefaultConfig {
		@Bean Server jettyServer() { return ephemeralServer(); }
		@Bean ReadinessState readinessState() { return new ReadinessState(); }
	}

	@Test void a01_defaultStopTimeoutApplied() throws Exception {
		var ms = create(A_DefaultConfig.class);
		try {
			ms.start();
			var server = ms.getBeanStore().getBean(JettyServerComponent.class).orElseThrow().getServer();
			assertEquals(JettyServerComponent.DEFAULT_STOP_TIMEOUT.toMillis(), server.getStopTimeout());
		} finally {
			ms.stop();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// B.  Programmatic JettySettings.stopTimeout override wins over the default.
	//-----------------------------------------------------------------------------------------------------------------

	@Configuration
	static class B_OverrideConfig {
		@Bean Server jettyServer() { return ephemeralServer(); }
		@Bean JettySettings jettySettings() { return JettySettings.create().stopTimeout(Duration.ofSeconds(5)).build(); }
		@Bean ReadinessState readinessState() { return new ReadinessState(); }
	}

	@Test void b01_settingsStopTimeoutOverridesDefault() throws Exception {
		var ms = create(B_OverrideConfig.class);
		try {
			ms.start();
			var server = ms.getBeanStore().getBean(JettyServerComponent.class).orElseThrow().getServer();
			assertEquals(5000L, server.getStopTimeout());
		} finally {
			ms.stop();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// C.  Readiness flips out of service on stop, ready on start.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_readinessFlipsOutOfServiceOnStop() throws Exception {
		var ms = create(A_DefaultConfig.class);
		ReadinessState rs;
		boolean readyAfterStart;
		try {
			ms.start();
			rs = ms.getBeanStore().getBean(ReadinessState.class).orElseThrow();
			readyAfterStart = rs.isReady();
		} finally {
			ms.stop();
		}
		assertTrue(readyAfterStart, "Service should be ready after start");
		assertFalse(rs.isReady(), "Service should be out of service after stop");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// D.  Settings round-trip.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_settingsRoundTrip() {
		var s = JettySettings.create().stopTimeout(Duration.ofSeconds(7)).shutdownSettleDelay(Duration.ofSeconds(2)).build();
		assertEquals(Duration.ofSeconds(7), s.getStopTimeout());
		assertEquals(Duration.ofSeconds(2), s.getShutdownSettleDelay());
	}
}
