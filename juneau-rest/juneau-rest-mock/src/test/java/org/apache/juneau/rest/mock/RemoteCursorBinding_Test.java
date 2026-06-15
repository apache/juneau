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

import java.util.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.jsonl.*;
import org.apache.juneau.marshall.stream.*;
import org.apache.juneau.rest.client.classic.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

/**
 * Integration tests for client-side cursor binding on {@code @Remote} interfaces.
 *
 * <p>
 * Verifies that {@code TokenReader} / {@code RecordReader} (and concrete subtypes like
 * {@code JsonlTokenReader}) work as {@code @RemoteOp} return types, and that
 * {@code RecordStreamBody} works as a {@code @Content} parameter.
 */
class RemoteCursorBinding_Test {

	public static class Bean {
		public String name;
		public int age;
		public Bean() {}
		public Bean(String name, int age) { this.name = name; this.age = age; }
	}

	// ==========================================================================
	// Server side — produces JSON or JSONL
	// ==========================================================================

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class JsonServer {
		@RestGet(path = "/bean")
		public Bean get() {
			return new Bean("alice", 30);
		}

		@RestPost(path = "/echo")
		public Bean echo(@Content Bean b) {
			return b;
		}
	}

	@Rest(serializers = JsonlSerializer.class, parsers = JsonlParser.class, defaultAccept = "application/jsonl")
	public static class JsonlServer {
		@RestGet(path = "/feed")
		public List<Bean> feed() {
			return List.of(new Bean("a", 1), new Bean("b", 2), new Bean("c", 3));
		}
	}

	// ==========================================================================
	// Remote interfaces
	// ==========================================================================

	@Remote
	public interface JsonClientApi {
		@RemoteGet("/bean")
		RecordReader getBean();

		@RemoteGet("/bean")
		TokenReader getBeanAsTokens();

		@RemoteGet("/bean")
		JsonTokenReader getBeanAsJsonTokens();

		@RemotePost("/echo")
		Bean echo(@Content RecordStreamBody body);
	}

	@Remote
	public interface JsonlClientApi {
		@RemoteGet("/feed")
		JsonlTokenReader getFeed();
	}

	// ==========================================================================
	// Tests — return-type cursor binding
	// ==========================================================================

	@Test
	void a01_recordReaderReturnType() throws Exception {
		try (var client = org.apache.juneau.rest.mock.classic.MockRestClient.buildJson(JsonServer.class)) {
			var api = client.getRemote(JsonClientApi.class);
			try (RecordReader r = api.getBean()) {
				var b = r.read(Bean.class);
				assertEquals("alice", b.name);
				assertEquals(30, b.age);
			}
		}
	}

	@Test
	void a02_tokenReaderReturnType() throws Exception {
		try (var client = org.apache.juneau.rest.mock.classic.MockRestClient.buildJson(JsonServer.class)) {
			var api = client.getRemote(JsonClientApi.class);
			try (TokenReader r = api.getBeanAsTokens()) {
				var b = r.read(Bean.class);
				assertEquals("alice", b.name);
				assertEquals(30, b.age);
			}
		}
	}

	@Test
	void a03_concreteCursorReturnType() throws Exception {
		try (var client = org.apache.juneau.rest.mock.classic.MockRestClient.buildJson(JsonServer.class)) {
			var api = client.getRemote(JsonClientApi.class);
			try (JsonTokenReader r = api.getBeanAsJsonTokens()) {
				var b = r.read(Bean.class);
				assertEquals("alice", b.name);
			}
		}
	}

	@Test
	void a04_jsonlMultiRecordReturnType() throws Exception {
		try (var client = org.apache.juneau.rest.mock.classic.MockRestClient.create(JsonlServer.class)
				.parser(JsonlParser.DEFAULT)
				.serializer(JsonlSerializer.DEFAULT)
				.contentType("application/jsonl")
				.accept("application/jsonl")
				.build()) {
			var api = client.getRemote(JsonlClientApi.class);
			try (JsonlTokenReader r = api.getFeed()) {
				var got = new ArrayList<Bean>();
				while (r.canRead())
					got.add(r.read(Bean.class));
				assertEquals(3, got.size());
				assertEquals("a", got.get(0).name);
				assertEquals(3, got.get(2).age);
			}
		}
	}

	// ==========================================================================
	// Tests — RecordStreamBody parameter
	// ==========================================================================

	@Test
	@SuppressWarnings({
		"resource" // Fluent writer calls return the caller-owned writer for chaining; nothing new to close. Eclipse JDT resource-leak warning is by design.
	})
	void b01_recordStreamBody_record() throws Exception {
		try (var client = org.apache.juneau.rest.mock.classic.MockRestClient.buildJson(JsonServer.class)) {
			var api = client.getRemote(JsonClientApi.class);
			Bean got = api.echo(RecordStreamBody.record(w -> {
				try {
					w.write(new Bean("dave", 99));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}));
			assertEquals("dave", got.name);
			assertEquals(99, got.age);
		}
	}

	@Test
	@SuppressWarnings({
		"resource" // Fluent writer calls return the caller-owned writer for chaining; nothing new to close. Eclipse JDT resource-leak warning is by design.
	})
	void b02_recordStreamBody_token() throws Exception {
		try (var client = org.apache.juneau.rest.mock.classic.MockRestClient.buildJson(JsonServer.class)) {
			var api = client.getRemote(JsonClientApi.class);
			// Build a JSON object via structural events.
			Bean got = api.echo(RecordStreamBody.token(w -> {
				try {
					w.startObject();
					w.fieldName("name");
					w.string("eve");
					w.fieldName("age");
					w.number(45);
					w.endObject();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}));
			assertEquals("eve", got.name);
			assertEquals(45, got.age);
		}
	}
}
