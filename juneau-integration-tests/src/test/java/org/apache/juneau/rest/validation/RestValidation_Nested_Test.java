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

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

import jakarta.validation.*;
import jakarta.validation.constraints.*;

/**
 * Cascading-validation coverage. Jakarta Validation's contract is that {@code @Valid} on a nested
 * property tells the validator to recurse into that property and re-evaluate its constraints &mdash;
 * but only if the parent's {@code @Valid} has triggered validation in the first place.
 *
 * <p>
 * These tests verify two contracts:
 * <ol>
 * <li>When the parent parameter is opted in via {@code @Valid}, cascading {@code @Valid} on a
 *     nested property surfaces violations from the nested bean's fields.
 * <li>The cascade still respects the off-by-default contract: if the parent has no {@code @Valid},
 *     the nested {@code @Valid} alone does <i>not</i> trigger validation &mdash; the validator is
 *     never invoked on the root and so the nested bean is never inspected either.
 * </ol>
 */
class RestValidation_Nested_Test extends TestBase {

	// -----------------------------------------------------------------------------------------------------------------
	// Domain model: Customer has-many Address. Address.zip is constrained; Customer.addresses is
	// marked @Valid so the validator recurses into each Address.
	// -----------------------------------------------------------------------------------------------------------------

	public static class Address {

		@Pattern(regexp = "\\d{5}")
		public String zip;
	}

	public static class Customer {

		@NotBlank
		public String name;

		@Valid
		public List<Address> addresses;
	}

	// -----------------------------------------------------------------------------------------------------------------
	// A: parent opted in via @Valid &rarr; nested @Valid cascades into Address.zip.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class A {

		@RestPost("/customer")
		public String create(@Valid @Content Customer c) {
			return "ok:" + c.name;
		}
	}

	@Test
	void a01_validParent_violatingChild_isBlocked() throws Exception {
		var a = MockRestClient.buildLax(A.class);
		// Customer.name is fine but Address[0].zip violates the @Pattern. Cascading should pick this up
		// and the path should reflect the nested location (addresses[0].zip), proving recursion happened.
		a.post("/customer", "{\"name\":\"alice\",\"addresses\":[{\"zip\":\"abc\"}]}")
			.contentType("application/json")
			.run()
			.assertStatus(400)
			.assertContent().isContains("\"status\":400", "\"errors\":[", "addresses[0].zip", "Pattern");
	}

	@Test
	void a02_validParent_violatingParent_isBlocked() throws Exception {
		var a = MockRestClient.buildLax(A.class);
		// Sanity check &mdash; if the parent itself violates a constraint, the parent's path surfaces too.
		a.post("/customer", "{\"name\":\"\",\"addresses\":[{\"zip\":\"12345\"}]}")
			.contentType("application/json")
			.run()
			.assertStatus(400)
			.assertContent().isContains("\"status\":400", "\"errors\":[", "\"name\"", "NotBlank");
	}

	@Test
	void a03_validParent_validChild_passesThrough() throws Exception {
		var a = MockRestClient.buildLax(A.class);
		a.post("/customer", "{\"name\":\"alice\",\"addresses\":[{\"zip\":\"12345\"}]}")
			.contentType("application/json")
			.run()
			.assertStatus(200)
			.assertContent("\"ok:alice\"");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: parent NOT opted in &rarr; nested @Valid alone is inert. This is the cross-cutting off-by-default
	// invariant: cascading is a property of the validation pass, not a side-channel that bypasses it.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class B {

		@RestPost("/customer")
		public String create(@Content Customer c) {
			return "lax:" + (c.name == null ? "" : c.name);
		}
	}

	@Test
	void b01_noParentValid_nestedValidAlone_isInert() throws Exception {
		var b = MockRestClient.buildLax(B.class);
		// Same violating payload as a01. Without @Valid on the parameter, the validator is never invoked,
		// so even Address.zip's nested @Pattern is ignored. Bean reaches handler unchanged.
		b.post("/customer", "{\"name\":\"\",\"addresses\":[{\"zip\":\"abc\"}]}")
			.contentType("application/json")
			.run()
			.assertStatus(200)
			.assertContent("\"lax:\"");
	}
}
