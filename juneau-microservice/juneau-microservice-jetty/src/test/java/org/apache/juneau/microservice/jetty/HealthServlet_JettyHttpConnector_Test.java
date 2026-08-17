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

import java.net.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.server.health.*;
import org.eclipse.jetty.ee11.servlet.*;
import org.eclipse.jetty.server.*;
import org.junit.jupiter.api.*;

/**
 * [TODO-401] Real-HTTP-connector coverage for the auto-mounted {@link HealthServlet} probe paths under a Jetty
 * microservice booted from {@link HealthProbeConfiguration} (the DualStore boot recipe), instead of the in-process
 * {@code ReadinessState.resolve(BeanStore)} shortcut that
 * {@link JettyServerComponent_ReadinessStateDualStorePublish_Test} deliberately used to route around the
 * pre-existing multi-path dispatch bug.
 *
 * <h5 class='section'>Regression covered (both stacked bugs):</h5>
 * <p>
 * Before the fix, a bare {@code GET /readyz} against the exact-match servlet mount arrived with a zero-segment
 * {@code pathInfo}, which matched none of {@code HealthServlet}'s three 1-segment {@code @RestGet} operations, so
 * {@code findOperation} threw {@code NotFound} (Bug A).  A real container's response defaults to status {@code 200}
 * (not the mock's {@code 0}), so the old {@code RestSession} sentinel skipped its {@code 404} default and
 * {@code handleNotFound} mis-mapped the miss to a {@code 500} (Bug B).  The combined observable failure on an
 * unmodified tree is therefore {@code 500} for all three probes.  After both fixes, {@code HealthServlet} matches
 * every probe via a single {@code @RestGet(path="/*")} operation and dispatches on the last path segment, so a
 * freshly-started (ready) service returns {@code 200}.
 *
 * @since 10.0.0
 */
@org.apache.juneau.testing.JettyMicroserviceTest
@SuppressWarnings("resource")  // Microservice/Server instances are test fixtures managed by the test lifecycle; explicit close is not needed for these assertions.
class HealthServlet_JettyHttpConnector_Test extends TestBase {

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

	private static int boundPort(Microservice ms) {
		var jsc = ms.getBeanStore().getBean(JettyServerComponent.class).orElseThrow();
		for (var c : jsc.getServer().getConnectors())
			if (c instanceof ServerConnector sc)
				return sc.getLocalPort();
		throw new AssertionError("Could not locate a bound ServerConnector on the Jetty server");
	}

	private static int statusOf(int port, String path) throws Exception {
		var url = URI.create("http://localhost:" + port + path).toURL();
		var conn = (HttpURLConnection)url.openConnection();
		try {
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");
			conn.setConnectTimeout(5000);
			conn.setReadTimeout(5000);
			return conn.getResponseCode();
		} finally {
			conn.disconnect();
		}
	}

	@Configuration
	static class Config {
		@Bean Server jettyServer() { return ephemeralServer(); }
	}

	@Test
	void a01_probesReturn200OverRealHttpConnector() throws Exception {
		var ms = create(Config.class, HealthProbeConfiguration.class);
		try {
			ms.start();
			var port = boundPort(ms);

			assertEquals(200, statusOf(port, "/healthz"), "GET /healthz should resolve to the overall aggregate and be 200 for a fresh service");
			assertEquals(200, statusOf(port, "/readyz"), "GET /readyz should resolve to the READY probe and be 200 for a fresh (ready) service");
			assertEquals(200, statusOf(port, "/livez"), "GET /livez should resolve to the LIVE probe and be 200 for a fresh service");
		} finally {
			stopQuietly(ms);
		}
	}

	@Test
	void a02_subPathUnderExactMountIsCleanContainer404() throws Exception {
		// Container-level pin (not Bug B coverage): a sub-path under the exact-match /healthz mount is 404ed by
		// Jetty before Juneau ever sees it.  Pins the expectation against a future mount-mechanism change.
		var ms = create(Config.class, HealthProbeConfiguration.class);
		try {
			ms.start();
			var port = boundPort(ms);
			assertEquals(404, statusOf(port, "/healthz/nonexistent"), "A sub-path under the exact-match /healthz mount must be a clean container-level 404");
		} finally {
			stopQuietly(ms);
		}
	}
}
