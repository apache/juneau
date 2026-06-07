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
package org.apache.juneau.rest.server.ops;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.guard.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Validates the {@link AdminResource} child-resource flavor mounted via {@code @Rest(children=...)}
 * under a host.
 *
 * <p>
 * The child mounts at the subtree {@code /admin} (ops at the bare endpoint names) and delegates to a
 * shared {@link AdminMixin} worker. Cases:
 * <ul>
 * 	<li>Default-deny: the inherited {@link org.apache.juneau.rest.server.server.guard.DenyAllGuard DenyAllGuard}
 * 		returns {@code 403 Forbidden} on {@code /admin/threads}.
 * 	<li>The host's own endpoints are unaffected by the mounted child.
 * 	<li>With an allow-all {@code @Bean RestGuardList} on the child, {@code /admin/threads} serves.
 * </ul>
 *
 * @since 10.0.0
 */
class AdminResource_Test extends TestBase {

	// -----------------------------------------------------------------------------------------
	// Default-deny child.
	// -----------------------------------------------------------------------------------------

	@Rest(children={AdminResource.class})
	public static class P extends RestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/items") public String items() { return "items"; }
	}

	private static final MockRestClient cp = MockRestClient.buildLax(P.class);

	@Test void a01_threadsDeniedByDefault() throws Exception {
		cp.get("/admin/threads").run().assertStatus(403);
	}

	@Test void a02_hostEndpointStillReachable() throws Exception {
		cp.get("/items").run().assertStatus(200).assertContent().asString().isContains("items");
	}

	// -----------------------------------------------------------------------------------------
	// Allow-all child (guard replaced via @Bean RestGuardList).
	// -----------------------------------------------------------------------------------------

	/** Child subclass that replaces the inherited deny-all guard with an allow-all chain. */
	public static class AdminChild extends AdminResource {
		@Bean public RestGuardList guards(BeanStore bs) {
			return RestGuardList.create(bs).build();
		}
	}

	@Rest(children={AdminChild.class})
	public static class Q extends RestServlet {
		private static final long serialVersionUID = 1L;
	}

	private static final MockRestClient cq = MockRestClient.buildLax(Q.class);

	@Test void b01_threadsServesWhenGuardReplaced() throws Exception {
		cq.get("/admin/threads")
			.run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("application/json");
	}
}
