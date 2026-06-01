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
package org.apache.juneau.rest.convention;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Validates the {@link VersionResource} child-resource flavor mounted via
 * {@code @Rest(children=...)} under a host.
 *
 * <p>
 * The child mounts at the subtree {@code /version} (op pinned at {@code /*}) and delegates to a
 * shared {@link VersionMixin} worker. Cases:
 * <ul>
 * 	<li>{@code GET /version} serves the deployment-metadata JSON map with the JVM
 * 		{@code javaVersion} present.
 * 	<li>The host's own endpoints are unaffected by the mounted child.
 * </ul>
 *
 * @since 9.5.0
 */
class VersionResource_Test extends TestBase {

	@Rest(children={VersionResource.class})
	public static class A extends RestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/items") public String items() { return "items"; }
	}

	private static final MockRestClient c = MockRestClient.buildLax(A.class);

	@Test void a01_versionServedAsChild() throws Exception {
		var body = c.get("/version")
			.run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("application/json")
			.getContent().asString();
		var parsed = JsonParser.DEFAULT.parse(body, Map.class);
		Assertions.assertNotNull(parsed.get("javaVersion"), "javaVersion present");
	}

	@Test void a02_hostEndpointStillReachable() throws Exception {
		c.get("/items")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("items");
	}
}
