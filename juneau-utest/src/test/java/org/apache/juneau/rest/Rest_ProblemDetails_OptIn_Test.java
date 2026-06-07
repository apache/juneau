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
 * End-to-end tests for the {@code @Rest(problemDetails="true")} opt-in:
 *
 * <ul>
 * 	<li>Thrown {@link org.apache.juneau.http.response.BasicHttpException} on opted-in resource produces
 * 		{@code application/problem+json} regardless of client {@code Accept} (Q5(A)).
 * 	<li>Returned {@link Problem} from a handler produces {@code application/problem+json}.
 * 	<li>Resource without opt-in still produces legacy {@code text/plain} (regression bar).
 * </ul>
 */
class Rest_ProblemDetails_OptIn_Test extends TestBase {

	// -----------------------------------------------------------------------------------------------------------------
	// A: Opt-in resource — thrown exception produces application/problem+json
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(problemDetails="true")
	public static class A {
		@RestGet("/order/{id}")
		public String order(@Path("id") int id) {
			throw new NotFound("Order {0} not found", id);
		}
	}

	@Test
	void a01_throwBasicHttpException_emitsProblemJson() throws Exception {
		var a = MockRestClient.buildLax(A.class);
		a.get("/order/42")
			.run()
			.assertStatus(404)
			.assertHeader("Content-Type").isContains("application/problem+json")
			.assertContent().isContains("\"status\":404", "\"title\":\"Not Found\"", "\"detail\":\"Order 42 not found\"");
	}



	@Test
	void a02_throwBasicHttpException_acceptHtml_stillEmitsProblemJson_Q5A() throws Exception {
		var a = MockRestClient.buildLax(A.class);
		a.get("/order/99")
			.accept("text/html")
			.run()
			.assertStatus(404)
			.assertHeader("Content-Type").isContains("application/problem+json");
	}

	@Test
	void a03_problem_typeAbsent_omittedFromJson_Q7() throws Exception {
		var a = MockRestClient.buildLax(A.class);
		a.get("/order/7")
			.run()
			.assertStatus(404)
			.assertContent().isNotContains("\"type\"", "about:blank");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: Opt-in resource — returned Problem with custom status + extension fields
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(problemDetails="true")
	public static class B {
		@RestGet
		public Problem creditCheck() {
			return Problem.fromStatus(403, "Insufficient credit", "Balance 30 < cost 50")
				.set("balance", 30);
		}
	}

	@Test
	void b01_returnProblem_emitsProblemJsonWithCustomStatusAndExtension() throws Exception {
		var b = MockRestClient.buildLax(B.class);
		b.get("/creditCheck")
			.accept("application/problem+json")
			.run()
			.assertStatus(403)
			.assertHeader("Content-Type").isContains("application/problem+json")
			.assertContent().isContains("\"status\":403", "\"title\":\"Insufficient credit\"", "\"balance\":30");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: Resource without opt-in — thrown exception still produces legacy text/plain (regression bar).
	// -----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class C {
		@RestGet("/order/{id}")
		public String order(@Path("id") int id) {
			throw new NotFound("Order {0} not found", id);
		}
	}

	@Test
	void c01_noOptIn_throwBasicHttpException_emitsTextPlain() throws Exception {
		var c = MockRestClient.buildLax(C.class);
		// Without opt-in, the BasicHttpException flows through the default chain (ProblemDetailsProcessor passes
		// through, PlainTextPojoProcessor renders the exception's body / message as text/plain).
		c.get("/order/42")
			.run()
			.assertStatus(404)
			.assertHeader("Content-Type").isContains("text/plain")
			.assertContent().isContains("Order 42 not found");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// D: Explicit problemDetails="false" — same behavior as no annotation (negative-control parity).
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(problemDetails="false")
	public static class D {
		@RestGet("/order/{id}")
		public String order(@Path("id") int id) {
			throw new NotFound("Order {0} not found", id);
		}
	}

	@Test
	void d01_explicitFalse_throwBasicHttpException_emitsTextPlain() throws Exception {
		var d = MockRestClient.buildLax(D.class);
		d.get("/order/42")
			.run()
			.assertStatus(404)
			.assertHeader("Content-Type").isContains("text/plain");
	}
}
