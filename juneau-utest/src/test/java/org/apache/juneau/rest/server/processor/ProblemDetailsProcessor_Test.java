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
package org.apache.juneau.rest.server.processor;

import org.apache.juneau.*;
import org.apache.juneau.bean.rfc7807.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

/**
 * Tests {@link ProblemDetailsProcessor} on the success (return-from-handler) path. Throw-path coverage lives in
 * {@code Rest_ProblemDetails_OptIn_Test}; the processor is always in the default chain regardless of opt-in.
 */
class ProblemDetailsProcessor_Test extends TestBase {

	// -----------------------------------------------------------------------------------------------------------------
	// A: Problem return → application/problem+json with bean JSON
	// -----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {
		@RestGet
		public Problem a() {
			return Problem.fromStatus(403, "Insufficient credit", "Your balance is 30, but that costs 50.");
		}
	}

	@Test
	void a01_returnProblem_emitsProblemJson() throws Exception {
		var a = MockRestClient.buildLax(A.class);
		a.get("/a")
			.accept("application/problem+json")
			.run()
			.assertStatus(403)
			.assertHeader("Content-Type").isContains("application/problem+json")
			.assertContent().isContains("\"status\":403", "\"title\":\"Insufficient credit\"", "\"detail\":\"Your balance is 30, but that costs 50.\"");
	}

	@Test
	void a02_returnProblem_wildcardAccept_emitsProblemJson() throws Exception {
		var a = MockRestClient.buildLax(A.class);
		a.get("/a")
			.accept("*/*")
			.run()
			.assertStatus(403)
			.assertHeader("Content-Type").isContains("application/problem+json");
	}

	@Test
	void a03_returnProblem_noAccept_emitsProblemJson() throws Exception {
		var a = MockRestClient.buildLax(A.class);
		a.get("/a")
			.run()
			.assertStatus(403)
			.assertHeader("Content-Type").isContains("application/problem+json");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: Q5(B) — success-path Accept policy. If Accept doesn't match problem+json or */*, processor passes through.
	// -----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_returnProblem_acceptHtml_passesThrough() throws Exception {
		var a = MockRestClient.buildLax(A.class);
		// Accept: text/html — processor returns NEXT, so the default chain runs.
		// The downstream serializer chain doesn't carry text/html, so we expect a 406 Not Acceptable.
		a.get("/a")
			.accept("text/html")
			.run()
			.assertStatus(406);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: Non-Problem returns are unchanged — the processor must return NEXT.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class C {
		@RestGet
		public String a() { return "hello"; }
	}

	@Test
	void c01_returnString_processorPassesThrough() throws Exception {
		var c = MockRestClient.buildLax(C.class);
		// String body should flow through the rest of the chain regardless of whether problem+json was in the
		// Accept set — the processor's pass-through is what we're verifying here.
		c.get("/a")
			.run()
			.assertStatus(200)
			.assertContent("hello");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// D: Q6 — Problem.status precedence. When non-null, processor calls setStatus(problem.getStatus()) — even on
	//    handlers that would otherwise produce 200.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class D {
		@RestGet
		public Problem a() {
			return Problem.fromStatus(429, "Too Many Requests", "Slow down");
		}
		@RestGet
		public Problem b() {
			return new Problem().setTitle("No status set");
		}
	}

	@Test
	void d01_statusFromProblem_wins() throws Exception {
		var d = MockRestClient.buildLax(D.class);
		d.get("/a")
			.accept("application/problem+json")
			.run()
			.assertStatus(429)
			.assertHeader("Content-Type").isContains("application/problem+json");
	}

	@Test
	void d02_nullStatusOnProblem_leavesDefault() throws Exception {
		var d = MockRestClient.buildLax(D.class);
		// Q6(C) — null Problem.status leaves existing status alone; RestSession normalizes 0 → 200.
		d.get("/b")
			.accept("application/problem+json")
			.run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("application/problem+json")
			.assertContent().isContains("\"title\":\"No status set\"")
			.assertContent().isNotContains("\"status\"");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// E: Q7 — Problem.type default. Null type field must be omitted from JSON (not synthesized as about:blank).
	// -----------------------------------------------------------------------------------------------------------------

	@Test
	void e01_nullType_omittedFromJson() throws Exception {
		var a = MockRestClient.buildLax(A.class);
		a.get("/a")
			.accept("application/problem+json")
			.run()
			.assertStatus(403)
			.assertContent().isNotContains("\"type\"", "about:blank");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// F: ProblemException return-value round-trip — the processor unwraps via instanceof ProblemException.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class F {
		@RestGet
		public Object a() {
			// Return (not throw) — exercises the success path's ProblemException unwrap branch.
			return new ProblemException(Problem.fromStatus(409, "Conflict", "Version mismatch"));
		}
	}

	@Test
	void f01_returnProblemException_unwrapsToProblemJson() throws Exception {
		var f = MockRestClient.buildLax(F.class);
		f.get("/a")
			.accept("application/problem+json")
			.run()
			.assertStatus(409)
			.assertHeader("Content-Type").isContains("application/problem+json")
			.assertContent().isContains("\"status\":409", "\"title\":\"Conflict\"", "\"detail\":\"Version mismatch\"");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// G: Extension fields round-trip through the bean as-is.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class G {
		@RestGet
		public Problem a() {
			return Problem.fromStatus(403, "Insufficient credit", "Balance 30 < cost 50")
				.set("balance", 30);
		}
	}

	@Test
	void g01_extensionFields_serialized() throws Exception {
		var g = MockRestClient.buildLax(G.class);
		g.get("/a")
			.accept("application/problem+json")
			.run()
			.assertStatus(403)
			.assertContent().isContains("\"balance\":30");
	}
}
