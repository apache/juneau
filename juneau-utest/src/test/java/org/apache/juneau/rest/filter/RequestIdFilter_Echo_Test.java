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
package org.apache.juneau.rest.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.http.*;

class RequestIdFilter_Echo_Test extends TestBase {

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

	@Test void a01_responseHeaderMatchesRequestAttribute() throws Exception {
		var c = MockRestClient.create(A.class).ignoreErrors().json().build();
		var res = c.get("/a").run().assertStatus(200);
		var echoed = res.getHeader("X-Request-Id").asString().orElseThrow();
		res.assertContent().asString().isContains(echoed);
	}

	@Test void a02_eachRequestGetsIndependentId() throws Exception {
		var c = MockRestClient.create(A.class).ignoreErrors().json().build();
		var id1 = c.get("/a").run().assertStatus(200).getHeader("X-Request-Id").asString().orElseThrow();
		var id2 = c.get("/a").run().assertStatus(200).getHeader("X-Request-Id").asString().orElseThrow();
		assertNotEquals(id1, id2);
	}
}
