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
package org.apache.juneau.rest;

import org.apache.juneau.*;
import org.apache.juneau.bean.rfc7807.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

/**
 * End-to-end tests for the per-operation {@code problemDetails} attribute on {@code @RestOp} / {@code @RestGet} /
 * {@code @RestPost} / {@code @RestPut} / {@code @RestPatch} / {@code @RestDelete} / {@code @RestOptions}.
 *
 * <p>
 * Tri-state inheritance:
 * <ul>
 * 	<li>{@code "true"} on the op &mdash; opt this operation in (overrides resource).
 * 	<li>{@code "false"} on the op &mdash; opt this operation out (overrides an opted-in resource).
 * 	<li>{@code ""} on the op &mdash; inherit from {@code @Rest(problemDetails)}.
 * </ul>
 */
class Rest_ProblemDetails_PerOp_Test extends TestBase {

	// -----------------------------------------------------------------------------------------------------------------
	// A: Resource opted-in, op opted-out → text/plain (op overrides resource).
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(problemDetails="true")
	public static class A {
		@RestGet(path="/order/{id}", problemDetails="false")
		public String order(@Path("id") int id) {
			throw new NotFound("Order {0} not found", id);
		}
	}

	@Test
	void a01_resourceIn_opOut_emitsTextPlain() throws Exception {
		var a = MockRestClient.buildLax(A.class);
		a.get("/order/42")
			.run()
			.assertStatus(404)
			.assertHeader("Content-Type").isContains("text/plain")
			.assertContent().isContains("Order 42 not found");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: Resource opted-out (default), op opted-in → application/problem+json.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B {
		@RestGet(path="/order/{id}", problemDetails="true")
		public String order(@Path("id") int id) {
			throw new NotFound("Order {0} not found", id);
		}
	}

	@Test
	void b01_resourceOut_opIn_emitsProblemJson() throws Exception {
		var b = MockRestClient.buildLax(B.class);
		b.get("/order/42")
			.run()
			.assertStatus(404)
			.assertHeader("Content-Type").isContains("application/problem+json")
			.assertContent().isContains("\"status\":404", "\"title\":\"Not Found\"", "\"detail\":\"Order 42 not found\"");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: Resource and op both default ("" / "") → text/plain (no opt-in anywhere).
	// -----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class C {
		@RestGet("/order/{id}")
		public String order(@Path("id") int id) {
			throw new NotFound("Order {0} not found", id);
		}
	}

	@Test
	void c01_bothDefault_emitsTextPlain() throws Exception {
		var c = MockRestClient.buildLax(C.class);
		c.get("/order/42")
			.run()
			.assertStatus(404)
			.assertHeader("Content-Type").isContains("text/plain");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// D: Resource opted-in, op default ("") → inherit, application/problem+json.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(problemDetails="true")
	public static class D {
		@RestGet("/order/{id}")
		public String order(@Path("id") int id) {
			throw new NotFound("Order {0} not found", id);
		}
	}

	@Test
	void d01_resourceIn_opInherit_emitsProblemJson() throws Exception {
		var d = MockRestClient.buildLax(D.class);
		d.get("/order/42")
			.run()
			.assertStatus(404)
			.assertHeader("Content-Type").isContains("application/problem+json");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// E: Per-op opt-in across all HTTP method annotations (@RestPost / @RestPut / @RestPatch / @RestDelete /
	//    @RestOptions / @RestOp).
	// -----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class E {
		@RestPost(path="/post", problemDetails="true")
		public String post() {
			throw new NotFound("post-fail");
		}

		@RestPut(path="/put", problemDetails="true")
		public String put() {
			throw new NotFound("put-fail");
		}

		@RestPatch(path="/patch", problemDetails="true")
		public String patch() {
			throw new NotFound("patch-fail");
		}

		@RestDelete(path="/delete", problemDetails="true")
		public String delete() {
			throw new NotFound("delete-fail");
		}

		@RestOptions(path="/options", problemDetails="true")
		public String options() {
			throw new NotFound("options-fail");
		}

		@RestOp(method="GET", path="/op", problemDetails="true")
		public String op() {
			throw new NotFound("op-fail");
		}
	}

	@Test
	void e01_perOp_post_emitsProblemJson() throws Exception {
		var e = MockRestClient.buildLax(E.class);
		e.post("/post", "")
			.run()
			.assertStatus(404)
			.assertHeader("Content-Type").isContains("application/problem+json")
			.assertContent().isContains("\"detail\":\"post-fail\"");
	}

	@Test
	void e02_perOp_put_emitsProblemJson() throws Exception {
		var e = MockRestClient.buildLax(E.class);
		e.put("/put", "")
			.run()
			.assertStatus(404)
			.assertHeader("Content-Type").isContains("application/problem+json")
			.assertContent().isContains("\"detail\":\"put-fail\"");
	}

	@Test
	void e03_perOp_patch_emitsProblemJson() throws Exception {
		var e = MockRestClient.buildLax(E.class);
		e.patch("/patch", "")
			.run()
			.assertStatus(404)
			.assertHeader("Content-Type").isContains("application/problem+json")
			.assertContent().isContains("\"detail\":\"patch-fail\"");
	}

	@Test
	void e04_perOp_delete_emitsProblemJson() throws Exception {
		var e = MockRestClient.buildLax(E.class);
		e.delete("/delete")
			.run()
			.assertStatus(404)
			.assertHeader("Content-Type").isContains("application/problem+json")
			.assertContent().isContains("\"detail\":\"delete-fail\"");
	}

	@Test
	void e05_perOp_options_emitsProblemJson() throws Exception {
		var e = MockRestClient.buildLax(E.class);
		e.options("/options")
			.run()
			.assertStatus(404)
			.assertHeader("Content-Type").isContains("application/problem+json")
			.assertContent().isContains("\"detail\":\"options-fail\"");
	}

	@Test
	void e06_perOp_restOp_emitsProblemJson() throws Exception {
		var e = MockRestClient.buildLax(E.class);
		e.get("/op")
			.run()
			.assertStatus(404)
			.assertHeader("Content-Type").isContains("application/problem+json")
			.assertContent().isContains("\"detail\":\"op-fail\"");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// F: Per-op opt-out beats opted-in resource for non-GET verbs as well.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(problemDetails="true")
	public static class F {
		@RestPost(path="/post", problemDetails="false")
		public String post() {
			throw new NotFound("post-fail");
		}

		@RestDelete(path="/delete", problemDetails="false")
		public String delete() {
			throw new NotFound("delete-fail");
		}
	}

	@Test
	void f01_resourceIn_postOut_emitsTextPlain() throws Exception {
		var f = MockRestClient.buildLax(F.class);
		f.post("/post", "")
			.run()
			.assertStatus(404)
			.assertHeader("Content-Type").isContains("text/plain");
	}

	@Test
	void f02_resourceIn_deleteOut_emitsTextPlain() throws Exception {
		var f = MockRestClient.buildLax(F.class);
		f.delete("/delete")
			.run()
			.assertStatus(404)
			.assertHeader("Content-Type").isContains("text/plain");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// G: ProblemException thrown from an opted-in op (per-op only) → user-supplied status + extension fields.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class G {
		@RestGet(path="/throttle", problemDetails="true")
		public String throttle() {
			throw new ProblemException(
				Problem.fromStatus(429, "Too Many Requests", "Slow down")
					.set("retry-after", 30));
		}
	}

	@Test
	void g01_throwProblemException_perOpOptIn_emitsProblemJsonWithStatusAndExtension() throws Exception {
		var g = MockRestClient.buildLax(G.class);
		g.get("/throttle")
			.run()
			.assertStatus(429)
			.assertHeader("Content-Type").isContains("application/problem+json")
			.assertContent().isContains(
				"\"status\":429",
				"\"title\":\"Too Many Requests\"",
				"\"detail\":\"Slow down\"",
				"\"retry-after\":30");
	}

	@Test
	void g02_throwProblemException_perOpOptIn_acceptHtml_stillEmitsProblemJson_Q5A() throws Exception {
		var g = MockRestClient.buildLax(G.class);
		// Per Q5(A): on an opted-in op, the throw path ignores Accept and emits problem+json.
		g.get("/throttle")
			.accept("text/html")
			.run()
			.assertStatus(429)
			.assertHeader("Content-Type").isContains("application/problem+json");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// H: ProblemException thrown from a NOT opted-in op → success-path Accept-honoring semantics retained
	//    (Phase 1 Accept-honoring fallback for non-opted-in ProblemException return values).
	// -----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class H {
		@RestGet("/throttle")
		public String throttle() {
			throw new ProblemException(Problem.fromStatus(429, "Too Many Requests", "Slow down"));
		}
	}

	@Test
	void h01_throwProblemException_noOptIn_acceptProblemJson_emitsProblemJson() throws Exception {
		var h = MockRestClient.buildLax(H.class);
		h.get("/throttle")
			.accept("application/problem+json")
			.run()
			.assertStatus(429)
			.assertHeader("Content-Type").isContains("application/problem+json")
			.assertContent().isContains("\"status\":429", "\"title\":\"Too Many Requests\"");
	}
}
