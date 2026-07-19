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
package org.apache.juneau.bean.rfc7807;

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;

import org.apache.juneau.http.header.*;
import org.apache.juneau.marshall.json.*;
import org.junit.jupiter.api.*;

/**
 * Round-trip tests for {@link Problem} (RFC 7807 {@code application/problem+json}).
 */
class Problem_RoundTrip_Test {

	private static final JsonSerializer SER = JsonSerializer.DEFAULT;
	private static final JsonSerializer READABLE = JsonSerializer.DEFAULT_READABLE;
	private static final JsonParser PAR = JsonParser.DEFAULT;

	@Nested
	class A_basicTests {

		@Test
		void a01_basicProperties() throws Exception {
			var a = new Problem()
				.setType(new URI("https://example.com/probs/out-of-credit"))
				.setTitle("You do not have enough credit.")
				.setStatus(403)
				.setDetail("Your current balance is 30, but that costs 50.")
				.setInstance(new URI("/account/12345/msgs/abc"));

			assertEquals(new URI("https://example.com/probs/out-of-credit"), a.getType());
			assertEquals("You do not have enough credit.", a.getTitle());
			assertEquals(Integer.valueOf(403), a.getStatus());
			assertEquals("Your current balance is 30, but that costs 50.", a.getDetail());
			assertEquals(new URI("/account/12345/msgs/abc"), a.getInstance());
		}

		@Test
		void a02_fluentSetters_returnThis() {
			var a = new Problem();
			assertSame(a, a.setType(null));
			assertSame(a, a.setTitle(null));
			assertSame(a, a.setStatus(null));
			assertSame(a, a.setDetail(null));
			assertSame(a, a.setInstance(null));
			assertSame(a, a.set("k", "v"));
		}

		@Test
		void a03_typeNullByDefault() {
			var a = new Problem();
			assertNull(a.getType());
			assertNull(a.getTitle());
			assertNull(a.getStatus());
			assertNull(a.getDetail());
			assertNull(a.getInstance());
		}

		@Test
		void a04_typeOrDefault_returnsAboutBlank_whenUnset() {
			var a = new Problem();
			assertEquals(URI.create("about:blank"), a.getTypeOrDefault());
			assertEquals(Problem.DEFAULT_TYPE, a.getTypeOrDefault());
		}

		@Test
		void a05_typeOrDefault_returnsSetValue_whenPresent() throws Exception {
			var a = new Problem().setType(new URI("https://example.com/x"));
			assertEquals(new URI("https://example.com/x"), a.getTypeOrDefault());
		}

		@Test
		void a06_fromStatus_populatesThreeFields() {
			var a = Problem.fromStatus(404, "Not Found", "Resource missing");
			assertNull(a.getType());
			assertEquals("Not Found", a.getTitle());
			assertEquals(Integer.valueOf(404), a.getStatus());
			assertEquals("Resource missing", a.getDetail());
			assertNull(a.getInstance());
		}

		@Test
		void a07_fromStatus_handlesNullStrings() {
			var a = Problem.fromStatus(500, null, null);
			assertEquals(Integer.valueOf(500), a.getStatus());
			assertNull(a.getTitle());
			assertNull(a.getDetail());
		}
	}

	@Nested
	class B_serialization {

		@Test
		void b01_allFiveFields_roundTripReadable() throws Exception {
			var a = new Problem()
				.setType(new URI("https://example.com/probs/out-of-credit"))
				.setTitle("You do not have enough credit.")
				.setStatus(403)
				.setDetail("Your current balance is 30, but that costs 50.")
				.setInstance(new URI("/account/12345/msgs/abc"));

			var j1 = READABLE.write(a);
			var copy = PAR.read(j1, Problem.class);
			assertEquals(a.getType(), copy.getType());
			assertEquals(a.getTitle(), copy.getTitle());
			assertEquals(a.getStatus(), copy.getStatus());
			assertEquals(a.getDetail(), copy.getDetail());
			assertEquals(a.getInstance(), copy.getInstance());
			var j2 = READABLE.write(copy);
			assertEquals(j1, j2);
		}

		@Test
		void b02_allFiveFields_roundTripCompact() throws Exception {
			var a = new Problem()
				.setType(new URI("https://example.com/x"))
				.setTitle("t")
				.setStatus(400)
				.setDetail("d")
				.setInstance(new URI("/i"));

			var j1 = SER.write(a);
			assertEquals(
				"{\"detail\":\"d\",\"instance\":\"/i\",\"status\":400,\"title\":\"t\",\"type\":\"https://example.com/x\"}",
				j1
			);
			var copy = PAR.read(j1, Problem.class);
			assertEquals(j1, SER.write(copy));
		}

		@Test
		void b03_emptyProblem_serializesAsEmptyObject() {
			var a = new Problem();
			assertEquals("{}", SER.write(a));
		}

		@Test
		void b04_typeAbsent_doesNotAppearInJson() {
			var a = new Problem().setTitle("t").setStatus(500);
			var json = SER.write(a);
			assertFalse(json.contains("type"), () -> "JSON should not contain 'type': " + json);
			assertFalse(json.contains("about:blank"), () -> "JSON should not contain 'about:blank': " + json);
		}

		@Test
		void b05_typeOrDefault_doesNotMutateField() {
			var a = new Problem().setTitle("t");
			a.getTypeOrDefault();
			assertNull(a.getType(), "getTypeOrDefault() must not eagerly populate the type field");
			assertFalse(SER.write(a).contains("type"));
		}

		@Test
		void b06_fromStatus_exactJson_404() {
			var a = Problem.fromStatus(404, "Not Found", "Resource missing");
			assertEquals(
				"{\"detail\":\"Resource missing\",\"status\":404,\"title\":\"Not Found\"}",
				SER.write(a)
			);
		}

		@Test
		void b07_fromStatus_exactJson_500() {
			var a = Problem.fromStatus(500, "Internal Server Error", "boom");
			assertEquals(
				"{\"detail\":\"boom\",\"status\":500,\"title\":\"Internal Server Error\"}",
				SER.write(a)
			);
		}

		@Test
		void b08_statusOmitted_whenNull() {
			var a = new Problem().setTitle("t");
			var json = SER.write(a);
			assertEquals("{\"title\":\"t\"}", json);
		}

		@Test
		void b09_contentType_constant() {
			assertEquals("application/problem+json", ContentType.APPLICATION_PROBLEM_JSON.getValue());
			assertEquals("application/problem+xml", ContentType.APPLICATION_PROBLEM_XML.getValue());
		}

		@Test
		void b10_contentType_roundTripsWhenSetExplicitly() {
			var ct = ContentType.of("application/problem+json");
			assertEquals(ContentType.APPLICATION_PROBLEM_JSON.getValue(), ct.getValue());
		}
	}

	@Nested
	class C_extraProperties {

		@Test
		void c01_extensionsFlatten_intoTopLevelJson() throws Exception {
			var a = new Problem()
				.setType(new URI("https://example.com/x"))
				.setTitle("t")
				.set("balance", 30)
				.set("accounts", new String[]{"/account/12345", "/account/67890"});

			var json = SER.write(a);
			assertTrue(json.contains("\"balance\":30"), () -> "Missing flat balance: " + json);
			assertTrue(json.contains("\"accounts\":[\"/account/12345\",\"/account/67890\"]"),
				() -> "Missing flat accounts: " + json);
			assertFalse(json.contains("\"extra\""), () -> "Extension members must not nest under 'extra': " + json);
			assertFalse(json.contains("\"extensions\""),
				() -> "Extension members must not nest under 'extensions': " + json);
		}

		@Test
		void c02_extensionsRoundTrip_backIntoExtraMap() throws Exception {
			var a = new Problem()
				.setType(new URI("https://example.com/x"))
				.setTitle("You do not have enough credit.")
				.setStatus(403)
				.set("balance", 30)
				.set("accountId", "abc");

			var json = SER.write(a);
			var copy = PAR.read(json, Problem.class);

			assertEquals(30, ((Number) copy.get("balance")).intValue());
			assertEquals("abc", copy.get("accountId"));
			assertTrue(copy.extraKeys().contains("balance"));
			assertTrue(copy.extraKeys().contains("accountId"));
			assertEquals(json, SER.write(copy));
		}

		@Test
		void c03_extraKeys_emptyOnFreshBean() {
			var a = new Problem();
			assertTrue(a.extraKeys().isEmpty());
			assertNull(a.get("anything"));
		}

		@Test
		void c04_extensionsRoundTripFromJson_intoExtra() throws Exception {
			var json = "{\"type\":\"https://example.com/x\",\"title\":\"t\",\"status\":403,"
				+ "\"detail\":\"d\",\"instance\":\"/i\",\"balance\":30,\"answer\":42}";
			var a = PAR.read(json, Problem.class);

			assertEquals(new URI("https://example.com/x"), a.getType());
			assertEquals("t", a.getTitle());
			assertEquals(Integer.valueOf(403), a.getStatus());
			assertEquals("d", a.getDetail());
			assertEquals(new URI("/i"), a.getInstance());
			assertEquals(30, ((Number) a.get("balance")).intValue());
			assertEquals(42, ((Number) a.get("answer")).intValue());
		}

		@Test
		void c05_extensionsSetTwice_overwritesPreviousValue() {
			var a = new Problem().set("k", "v1").set("k", "v2");
			assertEquals("v2", a.get("k"));
		}
	}
}
