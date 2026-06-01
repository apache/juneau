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

import static java.util.EnumSet.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Validates the {@link HealthResource} child-resource flavor mounted via {@code @Rest(children=...)}
 * under a host.
 *
 * <p>
 * The child mounts at the subtree {@code /health} (ops {@code /healthz}, {@code /readyz},
 * {@code /livez}) and delegates to a shared {@link HealthAggregator} worker &mdash; the same logic
 * the {@link HealthServlet} servlet and {@link HealthMixin} mixin flavors use. Indicators are
 * resolved from the child's bean store. This mirrors {@link HealthServlet_Test} but exercises the
 * routed-child deployment. Cases:
 * <ul>
 * 	<li>{@code /health/healthz} returns {@code 503} when any component is down.
 * 	<li>{@code /health/livez} and {@code /health/readyz} filter indicators by probe type.
 * </ul>
 *
 * @since 9.5.0
 */
class HealthResource_Test extends TestBase {

	/**
	 * Child subclass registering indicator beans into the child's bean store.
	 *
	 * <p>
	 * {@link HealthResource} now extends {@link BasicRestResource}, so the
	 * {@link HealthAggregator.HealthResponse} bean is serialized out of the box via the
	 * {@code BasicUniversalConfig} serializer set &mdash; no explicit {@code serializers=...} is needed
	 * (parity with the servlet flavor's {@link BasicRestServlet} base).
	 */
	@Rest(path="/health")
	public static class HealthChild extends HealthResource {
		@Bean(name="db")
		public HealthIndicator dbIndicator() {
			return new HealthIndicator() {
				@Override public Health check() { return Health.up("db").detail("ok", true).build(); }
				@Override public EnumSet<HealthProbe> probes() { return of(HealthProbe.LIVE); }
			};
		}

		@Bean(name="cache")
		public HealthIndicator cacheIndicator() {
			return new HealthIndicator() {
				@Override public Health check() { return Health.down("cache", new IllegalStateException("offline")).build(); }
				@Override public EnumSet<HealthProbe> probes() { return of(HealthProbe.READY); }
			};
		}
	}

	@Rest(children={HealthChild.class})
	public static class A extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	private static final MockRestClient c = MockRestClient.buildLax(A.class);

	@Test void a01_healthzReturns503WhenAnyComponentDown() throws Exception {
		c.get("/health/healthz").accept("application/json").run()
			.assertStatus(503)
			.assertContent().asString().isContains("\"status\":\"DOWN\"");
	}

	@Test void a02_livezAndReadyzFilterByProbeType() throws Exception {
		c.get("/health/livez").accept("application/json").run().assertStatus(200);
		c.get("/health/readyz").accept("application/json").run().assertStatus(503);
	}
}
