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
package org.apache.juneau.rest.server;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.matcher.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.http.*;

/**
 * Pins that every {@code 405} the framework sends carries an {@code Allow} header (RFC 9110 15.5.6), and that other
 * error statuses don't.
 */
class Rest_AllowHeader_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Resources
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {
		@RestGet("/a")
		public String get() {
			return "GET";
		}

		@RestPut("/a")
		public String put() {
			return "PUT";
		}
	}

	@Remote
	public interface Echo {
		String echo(String x);
	}

	@Rest
	public static class B extends A {
		@RestOp(method="RRPC", path="/a/*")
		public Echo proxy() {
			return x -> x;
		}

		@RestOptions("/*")
		public String options() {
			return "OPTIONS";
		}
	}

	@Rest(problemDetails="true")
	public static class C extends A {}

	/** A start-call hook that leaves the status at {@code 405}. */
	@Rest
	public static class D extends A {
		@RestStartCall
		public void start(HttpServletResponse res) {
			res.setStatus(405);
		}
	}

	public static class Never extends RestMatcher {
		@Override
		public boolean matches(HttpServletRequest req) {
			return false;
		}
	}

	@Rest
	public static class E extends A {
		@RestGet(path="/p", matchers=Never.class)
		public String p() {
			return "P";
		}
	}

	private static MockRestClient client(Class<?> c) {
		return MockRestClient.create(c).ignoreErrors().build();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Tests
	//------------------------------------------------------------------------------------------------------------------

	@Test void q01_pathMatchesOtherMethod_allow() throws Exception {
		var r = client(A.class).delete("/a").run();
		r.assertStatus(405);
		assertEquals("GET, PUT", r.getStringHeader("Allow").orElse(null));
	}

	@Test void q02_pathMatchesOtherMethod_rrpcAndSlashStar() throws Exception {
		var r = client(B.class).delete("/a").run();
		r.assertStatus(405);
		var allow = r.getStringHeader("Allow").orElse(null);
		assertEquals("GET, OPTIONS, POST, PUT", allow);
		assertFalse(allow.contains("RRPC"), allow);
	}

	@Test void q03_pathMatchesOtherMethod_problemDetails() throws Exception {
		var r = client(C.class).delete("/a").accept("application/problem+json").run();
		r.assertStatus(405);
		r.assertHeader("Content-Type").isContains("application/problem+json");
		assertEquals("GET, PUT", r.getStringHeader("Allow").orElse(null));
	}

	@Test void q04_handleNotFound405_allow() throws Exception {
		var r = client(D.class).get("/nothing").run();
		r.assertStatus(405);
		// Nothing answers on the path, which an empty Allow says exactly.
		assertEquals("", r.getStringHeader("Allow").orElse(null));
	}

	@Test void q05_notFoundAndPrecondition_noAllow() throws Exception {
		var r = client(E.class).get("/nothing").run();
		r.assertStatus(404);
		assertFalse(r.getStringHeader("Allow").isPresent());
		r = client(E.class).get("/p").run();
		r.assertStatus(412);
		assertFalse(r.getStringHeader("Allow").isPresent());
	}
}
