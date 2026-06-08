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
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

@SuppressWarnings("resource")  // MockRestClient instances are short-lived test fixtures; the mock framework manages lifecycle.
class HealthServlet_Test extends TestBase {

	@Rest
	public static class A extends HealthServlet {
		private static final long serialVersionUID = 1L;
		@Override
		protected Map<String,HealthIndicator> indicators() {
			return Map.of(
				"db", (HealthIndicator)() -> Health.up("db").detail("validationQueryMs", 12).build(),
				"cache", (HealthIndicator)() -> Health.down("cache", new IllegalStateException("offline")).build()
			);
		}
	}

	@Rest
	public static class B extends HealthServlet {
		private static final long serialVersionUID = 1L;
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

	@Test void a01_healthzReturns503WhenAnyComponentDown() throws Exception {
		var c = MockRestClient.create(A.class).ignoreErrors().json().build();
		c.get("/healthz").run().assertStatus(503)
			.assertContent().asString().isContains("\"status\":\"DOWN\"");
	}

	@Test void a02_livezAndReadyzFilterByProbeType() throws Exception {
		var c = MockRestClient.buildLax(B.class);
		c.get("/livez").run().assertStatus(200).assertContent().asString().isContains("liveOnly");
		c.get("/readyz").run().assertStatus(200).assertContent().asString().isContains("readyOnly");
	}
}
