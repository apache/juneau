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

import static java.util.EnumSet.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

/**
 * Validates the zero-downtime readiness gate surfaced by {@link HealthAggregator}.
 *
 * <p>
 * When the shared {@link ReadinessState} flips {@link ReadinessState#markOutOfService() out of service} (as the
 * embedded-server shutdown hooks do at the very start of shutdown), {@code /readyz} must return {@code 503} so a
 * load balancer / Kubernetes stops routing new traffic, while {@code /livez} (so the pod is not killed mid-drain)
 * and {@code /healthz} stay unaffected.  A registered {@code ReadinessState} bean (the per-context resolution
 * winner over the process-wide shared instance) drives the gate here so the test is isolated.
 *
 * @since 10.0.0
 */
class ReadinessGate_Test extends TestBase {

	static final ReadinessState READINESS = new ReadinessState();

	@Rest
	public static class A extends HealthServlet {
		private static final long serialVersionUID = 1L;

		@Bean
		public ReadinessState readinessState() {
			return READINESS;
		}

		@Override
		protected Map<String,HealthIndicator> indicators() {
			return Map.of(
				"liveOnly", new HealthIndicator() {
					@Override public Health check() { return Health.up("liveOnly").build(); }
					@Override public EnumSet<HealthProbe> probes() { return of(HealthProbe.LIVE); }
				},
				"readyOnly", new HealthIndicator() {
					@Override public Health check() { return Health.up("readyOnly").build(); }
					@Override public EnumSet<HealthProbe> probes() { return of(HealthProbe.READY); }
				}
			);
		}
	}

	private static final MockRestClient c = MockRestClient.buildLax(A.class);

	@BeforeEach void resetReadiness() {
		READINESS.markReady();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// A.  Regression: default (non-shutdown) probe behavior is unchanged.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_default_readyzLivezHealthzAllHealthy() throws Exception {
		c.get("/readyz").accept("application/json").run().assertStatus(200);
		c.get("/livez").accept("application/json").run().assertStatus(200);
		c.get("/healthz").accept("application/json").run().assertStatus(200);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// B.  Shutdown gate: readyz flips to 503; livez and healthz stay healthy.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_outOfService_readyzFlips503() throws Exception {
		READINESS.markOutOfService();
		c.get("/readyz").accept("application/json").run()
			.assertStatus(503)
			.assertContent().asString().isContains("OUT_OF_SERVICE");
	}

	@Test void b02_outOfService_livezStaysHealthy() throws Exception {
		READINESS.markOutOfService();
		c.get("/livez").accept("application/json").run().assertStatus(200);
	}

	@Test void b03_outOfService_healthzUnaffected() throws Exception {
		READINESS.markOutOfService();
		c.get("/healthz").accept("application/json").run().assertStatus(200);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// C.  Recovery: flipping back ready restores readyz.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_readyAgain_readyzRecovers() throws Exception {
		READINESS.markOutOfService();
		c.get("/readyz").accept("application/json").run().assertStatus(503);
		READINESS.markReady();
		c.get("/readyz").accept("application/json").run().assertStatus(200);
	}
}
