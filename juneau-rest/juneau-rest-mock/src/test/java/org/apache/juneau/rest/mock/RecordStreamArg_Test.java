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
package org.apache.juneau.rest.mock;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.http.entity.*;
import org.apache.juneau.marshall.cbor.*;
import org.apache.juneau.marshall.csv.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.jsonl.*;
import org.apache.juneau.marshall.stream.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

/**
 * Integration tests for the Phase-4 {@code RecordReaderArg} / {@code TokenReaderArg} /
 * {@code RecordWriterArg} / {@code TokenWriterArg} REST argument resolvers.
 *
 * <p>
 * Verifies the two-layer resolution: declared parameter type narrows eligible parsers, and
 * the request {@code Content-Type} negotiates within that filtered set.
 */
class RecordStreamArg_Test {

	public static class Bean {
		public String name;
		public int age;
		public Bean() {}
		public Bean(String name, int age) { this.name = name; this.age = age; }
	}

	// ===========================================================================
	// Resource that accepts a RecordReader (abstract) — any parser eligible
	// ===========================================================================
	@Rest(serializers = org.apache.juneau.marshall.plaintext.PlainTextSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class A_RecordReaderResource {
		@RestPost
		public String post(RecordReader r) throws Exception {
			var b = r.read(Bean.class);
			return b.name + "/" + b.age;
		}
	}

	@Test
	void a01_recordReader_acceptsAnyParser() throws Exception {
		try (var client = MockRestClient.create(A_RecordReaderResource.class)) {
			try (var response = client.post("/").header("Accept", "text/plain").body(StringBody.of(
					"{\"name\":\"alice\",\"age\":30}", "application/json")).run()) {
				assertEquals(200, response.getStatusCode());
				assertEquals("alice/30", response.getBodyAsString());
			}
		}
	}

	// ===========================================================================
	// Resource that accepts a TokenReader (abstract) — only FULL parsers eligible
	// ===========================================================================
	@Rest(serializers = org.apache.juneau.marshall.plaintext.PlainTextSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class B_TokenReaderResource {
		@RestPost
		public String post(TokenReader r) throws Exception {
			var b = r.read(Bean.class);
			return b.name + "/" + b.age;
		}
	}

	@Test
	void b01_tokenReader_acceptsFullParser() throws Exception {
		try (var client = MockRestClient.create(B_TokenReaderResource.class)) {
			try (var response = client.post("/").header("Accept", "text/plain").body(StringBody.of(
					"{\"name\":\"bob\",\"age\":25}", "application/json")).run()) {
				assertEquals(200, response.getStatusCode());
				assertEquals("bob/25", response.getBodyAsString());
			}
		}
	}

	// ===========================================================================
	// Resource that accepts a JsonTokenReader (concrete) — only JsonParser eligible
	// ===========================================================================
	@Rest(serializers = org.apache.juneau.marshall.plaintext.PlainTextSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class C_JsonTokenReaderResource {
		@RestPost
		public String post(JsonTokenReader r) throws Exception {
			var b = r.read(Bean.class);
			return "json:" + b.name;
		}
	}

	@Test
	void c01_concreteCursor_jsonParser() throws Exception {
		try (var client = MockRestClient.create(C_JsonTokenReaderResource.class)) {
			try (var response = client.post("/").header("Accept", "text/plain").body(StringBody.of(
					"{\"name\":\"charlie\",\"age\":40}", "application/json")).run()) {
				assertEquals(200, response.getStatusCode());
				assertEquals("json:charlie", response.getBodyAsString());
			}
		}
	}

	// ===========================================================================
	// Concrete subtype + wrong parser → 415
	// Resource declares JsonlTokenReader param but only JsonParser is registered.
	// ===========================================================================
	@Rest(serializers = org.apache.juneau.marshall.plaintext.PlainTextSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class D_WrongCursorTypeResource {
		@RestPost
		public String post(JsonlTokenReader r) {
			return "should-not-reach:" + r;
		}
	}

	@Test
	void d01_concreteCursor_wrongParserReturns415() throws Exception {
		try (var client = MockRestClient.create(D_WrongCursorTypeResource.class)) {
			try (var response = client.post("/").header("Accept", "text/plain").body(StringBody.of(
					"{\"name\":\"dave\"}", "application/json")).run()) {
				// JsonParser produces JsonTokenReader, not JsonlTokenReader → resolver throws
				// UnsupportedMediaType → 415.
				assertEquals(415, response.getStatusCode());
				var body = response.getBodyAsString();
				assertFalse(body != null && body.contains("should-not-reach"),
					"Method must not be invoked when cursor type is incompatible: " + body);
			}
		}
	}

	// ===========================================================================
	// Concrete subtype matched via JsonlParser
	// ===========================================================================
	@Rest(serializers = org.apache.juneau.marshall.plaintext.PlainTextSerializer.class, parsers = JsonlParser.class, defaultAccept = "application/jsonl")
	public static class E_JsonlTokenReaderResource {
		@RestPost
		public String post(JsonlTokenReader r) throws Exception {
			var sb = new StringBuilder();
			while (r.canRead()) {
				var b = r.read(Bean.class);
				sb.append(b.name).append(",");
			}
			return sb.toString();
		}
	}

	@Test
	void e01_jsonlTokenReader_matched() throws Exception {
		try (var client = MockRestClient.create(E_JsonlTokenReaderResource.class)) {
			try (var response = client.post("/").header("Accept", "text/plain").body(StringBody.of(
					"{\"name\":\"a\",\"age\":1}\n{\"name\":\"b\",\"age\":2}\n",
					"application/jsonl")).run()) {
				assertEquals(200, response.getStatusCode());
				assertEquals("a,b,", response.getBodyAsString());
			}
		}
	}

	// ===========================================================================
	// F — Multi-parser: concrete cursor type drives parser selection.
	// Resource registers BOTH JsonParser and JsonlParser; declared param is
	// JsonlTokenReader; only Jsonl content negotiation lands here.
	// ===========================================================================
	@Rest(serializers = org.apache.juneau.marshall.plaintext.PlainTextSerializer.class,
		parsers = {JsonParser.class, JsonlParser.class}, defaultAccept = "application/jsonl")
	public static class F_JsonlConcreteResource {
		@RestPost
		public String post(JsonlTokenReader r) throws Exception {
			var sb = new StringBuilder();
			while (r.canRead()) {
				var b = r.read(Bean.class);
				sb.append(b.name).append(",");
			}
			return sb.toString();
		}
	}

	@Test
	void f01_concreteCursor_picksJsonlParser_amongMany() throws Exception {
		try (var client = MockRestClient.create(F_JsonlConcreteResource.class)) {
			try (var response = client.post("/").header("Accept", "text/plain").body(StringBody.of(
					"{\"name\":\"a\",\"age\":1}\n{\"name\":\"b\",\"age\":2}\n",
					"application/jsonl")).run()) {
				assertEquals(200, response.getStatusCode());
				assertEquals("a,b,", response.getBodyAsString());
			}
		}
	}

	@Test
	void f02_concreteCursor_jsonContentTypeProducesJsonReader_returns415() throws Exception {
		try (var client = MockRestClient.create(F_JsonlConcreteResource.class)) {
			try (var response = client.post("/").header("Accept", "text/plain").body(StringBody.of(
					"{\"name\":\"a\",\"age\":1}", "application/json")).run()) {
				// JsonParser matches Content-Type but produces JsonTokenReader (not JsonlTokenReader)
				// → resolver throws 415.
				assertEquals(415, response.getStatusCode());
			}
		}
	}

	// ===========================================================================
	// G — Abstract TokenReader picks the right concrete parser by Content-Type
	// ===========================================================================
	@Rest(serializers = org.apache.juneau.marshall.plaintext.PlainTextSerializer.class,
		parsers = {JsonParser.class, CborParser.class}, defaultAccept = "application/json")
	public static class G_AbstractTokenReaderResource {
		@RestPost
		public String post(TokenReader r) throws Exception {
			var b = r.read(Bean.class);
			return r.getClass().getSimpleName() + ":" + b.name;
		}
	}

	@Test
	void g01_abstractTokenReader_jsonContentType() throws Exception {
		try (var client = MockRestClient.create(G_AbstractTokenReaderResource.class)) {
			try (var response = client.post("/").header("Accept", "text/plain").body(StringBody.of(
					"{\"name\":\"x\",\"age\":1}", "application/json")).run()) {
				assertEquals(200, response.getStatusCode());
				assertEquals("JsonTokenReader:x", response.getBodyAsString());
			}
		}
	}

	@Test
	void g02_abstractTokenReader_cborContentType() throws Exception {
		try (var client = MockRestClient.create(G_AbstractTokenReaderResource.class)) {
			var cbor = CborSerializer.DEFAULT.write(new Bean("y", 2));
			try (var response = client.post("/").header("Accept", "text/plain").body(
					ByteArrayBody.of(cbor, "application/cbor")).run()) {
				assertEquals(200, response.getStatusCode());
				assertEquals("CborTokenReader:y", response.getBodyAsString());
			}
		}
	}

	// ===========================================================================
	// H — TokenReader with PARTIAL-only parser (Csv) → 415
	// ===========================================================================
	@Rest(serializers = org.apache.juneau.marshall.plaintext.PlainTextSerializer.class,
		parsers = CsvParser.class, defaultAccept = "text/csv")
	public static class H_TokenReaderWithPartialOnly {
		@RestPost
		public String post(TokenReader r) {
			return "should-not-reach:" + r;
		}
	}

	@Test
	void h01_tokenReader_partialOnlyParser_returns415() throws Exception {
		try (var client = MockRestClient.create(H_TokenReaderWithPartialOnly.class)) {
			try (var response = client.post("/").header("Accept", "text/plain").body(StringBody.of(
					"name,age\nfoo,1\n", "text/csv")).run()) {
				// CsvParser is record-only (not a TokenReadable) → 415.
				assertEquals(415, response.getStatusCode());
			}
		}
	}

	// ===========================================================================
	// I — RecordReader with PARTIAL-only parser (Csv) — works (record surface
	// IS what PARTIAL formats expose).
	// ===========================================================================
	@Rest(serializers = org.apache.juneau.marshall.plaintext.PlainTextSerializer.class,
		parsers = CsvParser.class, defaultAccept = "text/csv")
	public static class I_RecordReaderWithPartialOnly {
		@RestPost
		public String post(RecordReader r) throws Exception {
			var sb = new StringBuilder();
			while (r.canRead())
				sb.append(r.read(Bean.class).name).append(",");
			return sb.toString();
		}
	}

	@Test
	void i01_recordReader_partialOnlyParser_streamsRows() throws Exception {
		try (var client = MockRestClient.create(I_RecordReaderWithPartialOnly.class)) {
			try (var response = client.post("/").header("Accept", "text/plain").body(StringBody.of(
					"name,age\nfoo,1\nbar,2\n", "text/csv")).run()) {
				assertEquals(200, response.getStatusCode());
				assertEquals("foo,bar,", response.getBodyAsString());
			}
		}
	}
}
