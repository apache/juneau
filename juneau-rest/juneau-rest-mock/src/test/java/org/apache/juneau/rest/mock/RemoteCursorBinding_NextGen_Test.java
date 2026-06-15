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

import java.io.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.stream.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

/**
 * Integration tests for next-generation client-side cursor binding on {@code @Remote} interfaces and the
 * {@link RestRequest#streamBodyEntity(RecordStreamBody)} convenience.
 *
 * <p>
 * Verifies that {@code RecordReader} / {@code TokenReader} (and concrete subtypes like {@code JsonTokenReader}) work
 * as {@code @RemoteOp} return types, and that {@code RecordStreamBody} works as a {@code @Content} parameter and as a
 * directly-set request body.  The next-generation client performs no {@code Content-Type} negotiation, so cursors use
 * the default JSON marshaller.
 */
class RemoteCursorBinding_NextGen_Test {

	public static class Bean {
		public String name;
		public int age;
		public Bean() {}
		public Bean(String name, int age) { this.name = name; this.age = age; }
	}

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

	// ==========================================================================
	// a — return-type cursor binding
	// ==========================================================================

	@Test
	@SuppressWarnings({
		"resource" // Inner client returned by getClient() is owned by the MockRestClient, not closed separately.
	})
	void a01_recordReaderReturnType() throws Exception {
		try (var client = MockRestClient.create(JsonServer.class)) {
			var api = client.getClient().remote(JsonClientApi.class);
			try (RecordReader r = api.getBean()) {
				var b = r.read(Bean.class);
				assertEquals("alice", b.name);
				assertEquals(30, b.age);
			}
		}
	}

	@Test
	@SuppressWarnings({
		"resource" // Inner client returned by getClient() is owned by the MockRestClient, not closed separately.
	})
	void a02_tokenReaderReturnType() throws Exception {
		try (var client = MockRestClient.create(JsonServer.class)) {
			var api = client.getClient().remote(JsonClientApi.class);
			try (TokenReader r = api.getBeanAsTokens()) {
				var b = r.read(Bean.class);
				assertEquals("alice", b.name);
				assertEquals(30, b.age);
			}
		}
	}

	@Test
	@SuppressWarnings({
		"resource" // Inner client returned by getClient() is owned by the MockRestClient, not closed separately.
	})
	void a03_concreteCursorReturnType() throws Exception {
		try (var client = MockRestClient.create(JsonServer.class)) {
			var api = client.getClient().remote(JsonClientApi.class);
			try (JsonTokenReader r = api.getBeanAsJsonTokens()) {
				var b = r.read(Bean.class);
				assertEquals("alice", b.name);
			}
		}
	}

	// ==========================================================================
	// b — RecordStreamBody @Content parameter
	// ==========================================================================

	@Test
	@SuppressWarnings({
		"resource" // Fluent writer calls return the caller-owned writer for chaining; nothing new to close.
	})
	void b01_recordStreamBody_record() throws Exception {
		try (var client = MockRestClient.create(JsonServer.class)) {
			var api = client.getClient().remote(JsonClientApi.class);
			Bean got = api.echo(RecordStreamBody.records(w -> {
				try {
					w.write(new Bean("dave", 99));
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}));
			assertEquals("dave", got.name);
			assertEquals(99, got.age);
		}
	}

	@Test
	@SuppressWarnings({
		"resource" // Fluent writer calls return the caller-owned writer for chaining; nothing new to close.
	})
	void b02_recordStreamBody_token() throws Exception {
		try (var client = MockRestClient.create(JsonServer.class)) {
			var api = client.getClient().remote(JsonClientApi.class);
			Bean got = api.echo(RecordStreamBody.token(w -> {
				try {
					w.startObject();
					w.fieldName("name");
					w.string("eve");
					w.fieldName("age");
					w.number(45);
					w.endObject();
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}));
			assertEquals("eve", got.name);
			assertEquals(45, got.age);
		}
	}

	// ==========================================================================
	// c — streamBodyEntity(...) directly on a request
	// ==========================================================================

	@Test
	@SuppressWarnings({
		"resource" // Fluent writer calls return the caller-owned writer for chaining; nothing new to close.
	})
	void c01_streamBodyEntity_direct() throws Exception {
		try (var client = MockRestClient.create(JsonServer.class)) {
			try (var resp = client.post("/echo").streamBodyEntity(RecordStreamBody.records(w -> {
				try {
					w.write(new Bean("frank", 12));
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			})).run()) {
				var got = JsonParser.DEFAULT.parse(resp.body().asString(), Bean.class);
				assertEquals("frank", got.name);
				assertEquals(12, got.age);
			}
		}
	}
}
