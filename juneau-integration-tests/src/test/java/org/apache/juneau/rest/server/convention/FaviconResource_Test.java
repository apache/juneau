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
package org.apache.juneau.rest.server.convention;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Validates the {@link FaviconResource} child-resource flavor mounted via
 * {@code @Rest(children=...)} under a host.
 *
 * <p>
 * The child mounts at the subtree {@code /favicon.ico} (op pinned at {@code /*}) and delegates to a
 * shared flavor-neutral {@link FaviconProvider} worker. Cases:
 * <ul>
 * 	<li>{@code GET /favicon.ico} serves the default favicon with the expected content-type and
 * 		cache headers.
 * 	<li>The host's own endpoints are unaffected by the mounted child.
 * </ul>
 *
 * @since 10.0.0
 */
class FaviconResource_Test extends TestBase {

	@Rest(children={FaviconResource.class})
	public static class A extends RestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/items") public String items() { return "items"; }
	}

	private static final MockRestClient c = MockRestClient.buildLax(A.class);

	@Test void a01_faviconServedAsChild() throws Exception {
		var body = c.get("/favicon.ico")
			.run()
			.assertStatus(200)
			.assertHeader("Content-Type").is("image/x-icon")
			.assertHeader("Cache-Control").is("max-age=2592000, public")
			.getContent().asBytes();
		Assertions.assertTrue(body.length > 0, "Default favicon body must be non-empty");
		Assertions.assertEquals(0x01, body[2] & 0xFF, "ICO magic byte 2 (type=ICO)");
	}

	@Test void a02_hostEndpointStillReachable() throws Exception {
		c.get("/items")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("items");
	}
}
