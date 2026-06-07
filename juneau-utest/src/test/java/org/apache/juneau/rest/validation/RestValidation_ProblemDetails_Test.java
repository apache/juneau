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
package org.apache.juneau.rest.validation;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.validation.*;
import org.junit.jupiter.api.*;

import jakarta.validation.*;
import jakarta.validation.constraints.*;

/**
 * Verifies the wire-shape contract for {@link ValidationException} under both
 * {@code @Rest(problemDetails="true")} (RFC 7807) and the default (no opt-in) modes.
 */
class RestValidation_ProblemDetails_Test extends TestBase {

	public static class PaymentBean {
		@NotBlank
		public String account;
		@Min(1)
		public int amount;
	}

	// -----------------------------------------------------------------------------------------------------------------
	// A: problemDetails OFF (default) — application/json + { "status":400, "errors":[...] } envelope.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(serializers=JsonSerializer.class, parsers=JsonParser.class, defaultAccept="application/json")
	public static class A {
		@RestPost("/pay")
		public String pay(@Valid @Content PaymentBean bean) {
			return "ok";
		}
	}

	@Test
	void a01_noProblemDetails_emitsPlainJsonEnvelope() throws Exception {
		var a = MockRestClient.buildLax(A.class);
		a.post("/pay", "{\"account\":\"\",\"amount\":0}")
			.contentType("application/json")
			.run()
			.assertStatus(400)
			.assertHeader("Content-Type").isContains("application/json")
			.assertContent().isContains("\"status\":400", "\"errors\":[");
	}

	@Test
	void a02_noProblemDetails_envelopeIsNotProblemJson() throws Exception {
		var a = MockRestClient.buildLax(A.class);
		a.post("/pay", "{\"account\":\"\",\"amount\":0}")
			.contentType("application/json")
			.run()
			.assertHeader("Content-Type").isNotContains("application/problem+json");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: problemDetails ON — application/problem+json with RFC 7807 standard members + errors[] extension.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(problemDetails="true", serializers=JsonSerializer.class, parsers=JsonParser.class, defaultAccept="application/json")
	public static class B {
		@RestPost("/pay")
		public String pay(@Valid @Content PaymentBean bean) {
			return "ok";
		}
	}

	@Test
	void b01_problemDetailsOn_emitsProblemJson_withErrorsExtension() throws Exception {
		var b = MockRestClient.buildLax(B.class);
		b.post("/pay", "{\"account\":\"\",\"amount\":0}")
			.contentType("application/json")
			.run()
			.assertStatus(400)
			.assertHeader("Content-Type").isContains("application/problem+json")
			.assertContent().isContains(
				"\"status\":400",
				"\"title\":\"Bad Request\"",
				"\"errors\":[",
				"\"path\":\"account\"",
				"\"path\":\"amount\"");
	}

	@Test
	void b02_problemDetailsOn_acceptIgnoredOnErrorPath() throws Exception {
		var b = MockRestClient.buildLax(B.class);
		// Per RFC 7807 §3 and Juneau's existing problem-details opt-in: error path ignores Accept and always emits
		// problem+json on opted-in resources. Validation errors must follow that same rule.
		b.post("/pay", "{\"account\":\"\",\"amount\":0}")
			.contentType("application/json")
			.accept("text/html")
			.run()
			.assertStatus(400)
			.assertHeader("Content-Type").isContains("application/problem+json");
	}

	@Test
	void b03_problemDetailsOn_validBean_handlerStillRuns() throws Exception {
		// Sanity: opting in to problemDetails must not break the happy path.
		var b = MockRestClient.buildLax(B.class);
		b.post("/pay", "{\"account\":\"ACCT-1\",\"amount\":100}")
			.contentType("application/json")
			.run()
			.assertStatus(200)
			.assertContent("\"ok\"");
	}
}
