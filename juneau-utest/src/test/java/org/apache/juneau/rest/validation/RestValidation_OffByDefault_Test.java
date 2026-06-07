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
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

import jakarta.validation.constraints.*;

/**
 * Tests the non-negotiable &quot;validation is opt-in and disabled by default&quot; contract for
 * {@link BeanValidator} integration with Juneau REST.
 *
 * <p>
 * Every test here pairs a request bean carrying Jakarta constraint annotations with a {@code @RestOp} handler
 * that does <b>not</b> opt in via {@code @Valid}. The expectation is that the violating bean reaches the
 * handler body unmodified &mdash; not blocked by validation. A second test in each pair adds {@code @Valid} to
 * the parameter to prove validation does run when opted in, providing the positive-control bar for the
 * negative assertion.
 */
class RestValidation_OffByDefault_Test extends TestBase {

	// -----------------------------------------------------------------------------------------------------------------
	// Shared request bean with strict constraints. Any of these violated should produce a 400 — but only when @Valid is
	// present on the parameter. Without @Valid the validator is never invoked.
	// -----------------------------------------------------------------------------------------------------------------

	public static class Order {
		@NotBlank
		public String sku;
		@Min(1)
		public int quantity;
	}

	// -----------------------------------------------------------------------------------------------------------------
	// A: @Content without @Valid — violating bean must reach the handler (validation off by default).
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(serializers=JsonSerializer.class, parsers=JsonParser.class, defaultAccept="application/json")
	public static class A {
		@RestPost("/order")
		public String submit(@Content Order order) {
			return "got sku=[" + order.sku + "] qty=" + order.quantity;
		}
	}

	@Test
	void a01_content_noValid_violatingBean_reachesHandler() throws Exception {
		var a = MockRestClient.buildLax(A.class);
		a.post("/order", "{\"sku\":\"\",\"quantity\":0}")
			.contentType("application/json")
			.run()
			.assertStatus(200)
			.assertContent("\"got sku=[] qty=0\"");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: @Content with @Valid — positive-control. Now the same violating bean is blocked with 400.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(serializers=JsonSerializer.class, parsers=JsonParser.class, defaultAccept="application/json")
	public static class B {
		@RestPost("/order")
		public String submit(@jakarta.validation.Valid @Content Order order) {
			return "should not reach handler";
		}
	}

	@Test
	void b01_content_validPresent_violatingBean_isBlocked() throws Exception {
		var b = MockRestClient.buildLax(B.class);
		b.post("/order", "{\"sku\":\"\",\"quantity\":0}")
			.contentType("application/json")
			.run()
			.assertStatus(400)
			.assertContent().isContains("\"status\":400", "\"errors\":[");
	}

	@Test
	void b02_content_validPresent_validBean_reachesHandler() throws Exception {
		var b = MockRestClient.buildLax(B.class);
		// Bean satisfies all constraints — even with @Valid, validation passes and the handler runs (and throws an
		// assertion since the handler returns a hard-coded "should not reach" string — which IS what we expect on
		// success, since the handler isn't really wired to do useful work in this minimal test).
		b.post("/order", "{\"sku\":\"WIDGET-1\",\"quantity\":3}")
			.contentType("application/json")
			.run()
			.assertStatus(200)
			.assertContent("\"should not reach handler\"");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: Multiple handlers in the same resource — adding @Valid to ONE method must not enable validation on the OTHER
	// (proves per-parameter granularity).
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(serializers=JsonSerializer.class, parsers=JsonParser.class, defaultAccept="application/json")
	public static class C {
		@RestPost("/strict")
		public String strict(@jakarta.validation.Valid @Content Order order) {
			return "strict-ok";
		}
		@RestPost("/lax")
		public String lax(@Content Order order) {
			return "lax-ok sku=[" + order.sku + "] qty=" + order.quantity;
		}
	}

	@Test
	void c01_perOpGranularity_strict_blocks() throws Exception {
		var c = MockRestClient.buildLax(C.class);
		c.post("/strict", "{\"sku\":\"\",\"quantity\":0}")
			.contentType("application/json")
			.run()
			.assertStatus(400);
	}

	@Test
	void c02_perOpGranularity_lax_passesThrough() throws Exception {
		var c = MockRestClient.buildLax(C.class);
		// Same violating bean, sibling handler without @Valid — the validator must NOT be invoked, and the bean
		// must reach the lax handler unchanged. This is the non-negotiable "off by default" contract.
		c.post("/lax", "{\"sku\":\"\",\"quantity\":0}")
			.contentType("application/json")
			.run()
			.assertStatus(200)
			.assertContent("\"lax-ok sku=[] qty=0\"");
	}
}
