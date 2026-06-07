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
import org.junit.jupiter.api.*;

import jakarta.validation.*;
import jakarta.validation.constraints.*;

/**
 * Coverage for the &quot;happy / sad&quot; pairs of common Jakarta Bean Validation constraints when applied to
 * {@code @Content}-bound request beans on opted-in {@code @RestOp} handlers. Each block exercises one
 * constraint type so a regression in the dispatch-or-render path surfaces with a clear test name.
 */
class RestValidation_Content_Test extends TestBase {

	// -----------------------------------------------------------------------------------------------------------------
	// A: @NotBlank — String must contain non-whitespace content.
	// -----------------------------------------------------------------------------------------------------------------

	public static class ANameBean {
		@NotBlank
		public String name;
	}

	@Rest(serializers=JsonSerializer.class, parsers=JsonParser.class, defaultAccept="application/json")
	public static class A {
		@RestPost("/echo")
		public String echo(@Valid @Content ANameBean bean) {
			return "ok:" + bean.name;
		}
	}

	@Test
	void a01_notBlank_valid() throws Exception {
		var a = MockRestClient.buildLax(A.class);
		a.post("/echo", "{\"name\":\"alice\"}")
			.contentType("application/json")
			.run()
			.assertStatus(200)
			.assertContent("\"ok:alice\"");
	}

	@Test
	void a02_notBlank_violated() throws Exception {
		var a = MockRestClient.buildLax(A.class);
		a.post("/echo", "{\"name\":\"\"}")
			.contentType("application/json")
			.run()
			.assertStatus(400)
			.assertContent().isContains("\"path\":\"name\"");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: @Size — String length range.
	// -----------------------------------------------------------------------------------------------------------------

	public static class BCodeBean {
		@Size(min=3, max=5)
		public String code;
	}

	@Rest(serializers=JsonSerializer.class, parsers=JsonParser.class, defaultAccept="application/json")
	public static class B {
		@RestPost("/code")
		public String submit(@Valid @Content BCodeBean bean) {
			return "ok:" + bean.code;
		}
	}

	@Test
	void b01_size_violated_tooShort() throws Exception {
		var b = MockRestClient.buildLax(B.class);
		b.post("/code", "{\"code\":\"ab\"}")
			.contentType("application/json")
			.run()
			.assertStatus(400)
			.assertContent().isContains("\"path\":\"code\"");
	}

	@Test
	void b02_size_violated_tooLong() throws Exception {
		var b = MockRestClient.buildLax(B.class);
		b.post("/code", "{\"code\":\"abcdef\"}")
			.contentType("application/json")
			.run()
			.assertStatus(400)
			.assertContent().isContains("\"path\":\"code\"");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: @Min / @Max numeric range — multi-field bean with two distinct constraints.
	// -----------------------------------------------------------------------------------------------------------------

	public static class CRangeBean {
		@Min(1)
		public int qty;
		@Max(100)
		public int pct;
	}

	@Rest(serializers=JsonSerializer.class, parsers=JsonParser.class, defaultAccept="application/json")
	public static class C {
		@RestPost("/range")
		public String submit(@Valid @Content CRangeBean bean) {
			return "ok";
		}
	}

	@Test
	void c01_minViolated_listsPath() throws Exception {
		var c = MockRestClient.buildLax(C.class);
		c.post("/range", "{\"qty\":0,\"pct\":50}")
			.contentType("application/json")
			.run()
			.assertStatus(400)
			.assertContent().isContains("\"path\":\"qty\"");
	}

	@Test
	void c02_maxViolated_listsPath() throws Exception {
		var c = MockRestClient.buildLax(C.class);
		c.post("/range", "{\"qty\":1,\"pct\":101}")
			.contentType("application/json")
			.run()
			.assertStatus(400)
			.assertContent().isContains("\"path\":\"pct\"");
	}

	@Test
	void c03_bothViolated_payloadCarriesBothErrors() throws Exception {
		var c = MockRestClient.buildLax(C.class);
		c.post("/range", "{\"qty\":0,\"pct\":101}")
			.contentType("application/json")
			.run()
			.assertStatus(400)
			.assertContent().isContains("\"path\":\"pct\"", "\"path\":\"qty\"");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// D: @Pattern — regex must match.
	// -----------------------------------------------------------------------------------------------------------------

	public static class DPatternBean {
		@Pattern(regexp="^[A-Z]{3}$")
		public String code;
	}

	@Rest(serializers=JsonSerializer.class, parsers=JsonParser.class, defaultAccept="application/json")
	public static class D {
		@RestPost("/pattern")
		public String submit(@Valid @Content DPatternBean bean) {
			return "ok";
		}
	}

	@Test
	void d01_pattern_violated() throws Exception {
		var d = MockRestClient.buildLax(D.class);
		d.post("/pattern", "{\"code\":\"abc\"}")
			.contentType("application/json")
			.run()
			.assertStatus(400)
			.assertContent().isContains("\"path\":\"code\"");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// E: @NotNull — required field absent.
	// -----------------------------------------------------------------------------------------------------------------

	public static class ERequiredBean {
		@NotNull
		public String value;
	}

	@Rest(serializers=JsonSerializer.class, parsers=JsonParser.class, defaultAccept="application/json")
	public static class E {
		@RestPost("/required")
		public String submit(@Valid @Content ERequiredBean bean) {
			return "ok";
		}
	}

	@Test
	void e01_notNull_violated_absent() throws Exception {
		var e = MockRestClient.buildLax(E.class);
		e.post("/required", "{}")
			.contentType("application/json")
			.run()
			.assertStatus(400)
			.assertContent().isContains("\"path\":\"value\"");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// F: invalidValue suppression — payload must NOT echo the offending value back to the client by default.
	// -----------------------------------------------------------------------------------------------------------------

	public static class FSecretBean {
		@Size(min=10)
		public String creditCard;
	}

	@Rest(serializers=JsonSerializer.class, parsers=JsonParser.class, defaultAccept="application/json")
	public static class F {
		@RestPost("/secret")
		public String submit(@Valid @Content FSecretBean bean) {
			return "ok";
		}
	}

	@Test
	void f01_invalidValue_omittedByDefault() throws Exception {
		var f = MockRestClient.buildLax(F.class);
		f.post("/secret", "{\"creditCard\":\"1234\"}")
			.contentType("application/json")
			.run()
			.assertStatus(400)
			// The offending value "1234" must NOT appear in the response body.
			.assertContent().isNotContains("\"invalidValue\"", "1234");
	}
}
