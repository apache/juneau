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
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

class HealthMixin_Test extends TestBase {

	@Rest(mixins={HealthMixin.class})
	public static class A extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

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

	@Test void a01_healthEndpointsResolveViaMixin() throws Exception {
		var c = MockRestClient.buildLax(A.class);
		var r = c.get("/healthz").accept("application/json").run().cacheContent();
		if (r.getStatusCode() != 503)
			fail("Expected 503 but got " + r.getStatusCode() + " with body: " + r.getContent().asString());
		r.assertContent().asString().isContains("\"status\":\"DOWN\"");
		c.get("/livez").accept("application/json").run().assertStatus(200);
		c.get("/readyz").accept("application/json").run().assertStatus(503);
	}
}
