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
package org.apache.juneau.rest.server.filter;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.http.*;

@SuppressWarnings({
	"resource" // MockRestClient instances are short-lived test fixtures; the mock framework manages lifecycle.
})
class RequestIdFilter_Honor_Test extends TestBase {

	@Rest
	public static class A extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		private static final RequestIdFilter FILTER = RequestIdFilter.create().build();
		@RestStartCall
		public void stamp(HttpServletRequest req, HttpServletResponse res) {
			FILTER.apply(req, res);
		}
		@RestGet(path="/a")
		public String a(RestRequest req) {
			return req.getAttribute(RestServerConstants.REQUEST_ID).asString().orElse("");
		}
	}

	@Test void a01_honorsValidIncomingId() throws Exception {
		var c = MockRestClient.create(A.class).ignoreErrors().json().build();
		c.get("/a").header("X-Request-Id", "550e8400-e29b-41d4-a716-446655440000").run()
			.assertStatus(200)
			.assertHeader("X-Request-Id").is("550e8400-e29b-41d4-a716-446655440000")
			.assertContent().asString().isContains("550e8400-e29b-41d4-a716-446655440000");
	}

	@Test void a02_honorsValidShortId() throws Exception {
		var c = MockRestClient.create(A.class).ignoreErrors().json().build();
		c.get("/a").header("X-Request-Id", "abc123_-").run()
			.assertStatus(200)
			.assertHeader("X-Request-Id").is("abc123_-")
			.assertContent().asString().isContains("abc123_-");
	}
}
