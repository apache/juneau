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
import org.apache.juneau.marshall.msgpack.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

/**
 * End-to-end integration tests for next-generation client content negotiation, run in-process against a JSONL
 * {@code @Rest} server via {@link MockRestClient}.
 *
 * <p>
 * Each test builds a next-generation {@link RestClient} over the mock transport (mirroring the harness wiring used by
 * {@code RemoteCursorBinding_NextGen_Test}) and configures it with a serializer and/or parser set so the negotiation
 * paths are exercised:
 * <ul>
 * 	<li>{@code a01} — outbound {@code Content-Type} comes from the client's default serializer.
 * 	<li>{@code a02} — outbound default {@code Accept} advertises the client's configured parsers.
 * 	<li>{@code a03} — inbound cursor parser is negotiated from the response {@code Content-Type}.
 * 	<li>{@code a04} — a plain {@code @Remote} return is parsed by the negotiated parser (no per-call marshaller).
 * </ul>
 */
@SuppressWarnings({
	"resource" // MockRestClient and the negotiating RestClient are closed by their try-with-resources blocks.
})
class NextGenContentNegotiation_Test {

	public static class Bean {
		public String name;
		public int age;
		public Bean() {}
		public Bean(String name, int age) { this.name = name; this.age = age; }
	}

	@Rest(serializers = JsonlSerializer.class, parsers = JsonlParser.class, defaultAccept = "application/jsonl")
	public static class JsonlServer {
		@RestGet(path = "/feed")
		public List<Bean> feed() {
			return List.of(new Bean("a", 1), new Bean("b", 2), new Bean("c", 3));
		}
		@RestGet(path = "/one")
		public Bean one() {
			return new Bean("solo", 7);
		}
		@RestGet(path = "/echoAccept")
		public String echoAccept(@Header("Accept") String accept) { return accept; }
		@RestPost(path = "/echoContentType")
		public String echoContentType(@Header("Content-Type") String ct) { return ct; }
	}

	@Remote
	public interface JsonlApi {
		@RemoteGet("/feed")
		JsonlTokenReader getFeed();
	}

	@Test
	void a01_outboundContentTypeFromDefaultSerializer() throws Exception {
		var sset = SerializerSet.create().add(JsonlSerializer.DEFAULT).build();
		try (var mock = MockRestClient.create(JsonlServer.class)) {
			// A registered set is no longer an implicit default — designate the default serializer explicitly.
			try (var nc = RestClient.builder().transport(mock.getClient().getTransport()).serializers(sset).defaultSerializer(JsonlSerializer.DEFAULT).build()) {
				var rawBody = nc.post("/echoContentType").body(new Bean("a", 1)).run().body().asString();
				var echoed = JsonParser.DEFAULT.read(rawBody, String.class);
				assertTrue(echoed.startsWith("application/jsonl"), () -> "Echoed Content-Type was: " + echoed);
			}
		}
	}

	@Test
	void a08_outboundBinaryBodyContentType() throws Exception {
		try (var mock = MockRestClient.create(JsonlServer.class)) {
			try (var nc = RestClient.builder().transport(mock.getClient().getTransport()).defaultSerializer(MsgPackSerializer.DEFAULT).build()) {
				var rawBody = nc.post("/echoContentType").body(new Bean("a", 1)).run().body().asString();
				var echoed = JsonParser.DEFAULT.read(rawBody, String.class);
				assertTrue(echoed.startsWith("application/msgpack"), () -> "Echoed Content-Type was: " + echoed);
			}
		}
	}

	@Test
	void a02_defaultAcceptAdvertisesParsers() throws Exception {
		var pset = ParserSet.create().add(JsonlParser.DEFAULT).build();
		try (var mock = MockRestClient.create(JsonlServer.class)) {
			try (var nc = RestClient.builder().transport(mock.getClient().getTransport()).parsers(pset).build()) {
				var rawBody = nc.get("/echoAccept").run().body().asString();
				var echoed = JsonParser.DEFAULT.read(rawBody, String.class);
				assertTrue(echoed.startsWith("application/jsonl"), () -> "Echoed Accept was: " + echoed);
			}
		}
	}

	@Test
	void a07_loneDefaultParserAdvertisesAccept() throws Exception {
		// A client configured with only a default parser (no parser set) must still advertise an Accept header.
		try (var mock = MockRestClient.create(JsonlServer.class)) {
			try (var nc = RestClient.builder().transport(mock.getClient().getTransport()).defaultParser(JsonlParser.DEFAULT).build()) {
				var rawBody = nc.get("/echoAccept").run().body().asString();
				var echoed = JsonParser.DEFAULT.read(rawBody, String.class);
				assertTrue(echoed.contains("application/jsonl"), () -> "Echoed Accept was: " + echoed);
			}
		}
	}

	@Test
	void a03_inboundCursorNegotiation() throws Exception {
		var pset = ParserSet.create().add(JsonParser.DEFAULT, JsonlParser.DEFAULT).build();
		try (var mock = MockRestClient.create(JsonlServer.class)) {
			try (var nc = RestClient.builder().transport(mock.getClient().getTransport()).parsers(pset).build()) {
				try (var resp = nc.get("/feed").run();
					 JsonlTokenReader r = resp.body().asCursor(JsonlTokenReader.class)) {
					var got = new ArrayList<Bean>();
					while (r.canRead())
						got.add(r.read(Bean.class));
					assertEquals(3, got.size());
					assertEquals("a", got.get(0).name);
					assertEquals(3, got.get(2).age);
				}
			}
		}
	}

	@Test
	void a04_jsonlRemoteReturn() throws Exception {
		var pset = ParserSet.create().add(JsonParser.DEFAULT, JsonlParser.DEFAULT).build();
		try (var mock = MockRestClient.create(JsonlServer.class)) {
			try (var nc = RestClient.builder().transport(mock.getClient().getTransport()).parsers(pset).build()) {
				var api = nc.remote(JsonlApi.class);
				try (JsonlTokenReader r = api.getFeed()) {
					var got = new ArrayList<Bean>();
					while (r.canRead())
						got.add(r.read(Bean.class));
					assertEquals(3, got.size());
				}
			}
		}
	}

	@Test
	void a05_inboundAsNegotiated() throws Exception {
		// JSON is first in the set, so a working as(Bean.class) proves negotiation actively picked JSONL from the
		// response Content-Type (the JSON parser cannot read the JSONL line).
		var pset = ParserSet.create().add(JsonParser.DEFAULT, JsonlParser.DEFAULT).build();
		try (var mock = MockRestClient.create(JsonlServer.class)) {
			try (var nc = RestClient.builder().transport(mock.getClient().getTransport()).parsers(pset).build()) {
				try (var resp = nc.get("/one").run()) {
					var b = resp.body().as(Bean.class);
					assertEquals("solo", b.name);
					assertEquals(7, b.age);
				}
			}
		}
	}

	@Test
	void a06_inboundAsForcedParser() throws Exception {
		// No parser set configured: proves the forced-parser overload bypasses negotiation entirely.
		try (var mock = MockRestClient.create(JsonlServer.class)) {
			try (var nc = RestClient.builder().transport(mock.getClient().getTransport()).build()) {
				try (var resp = nc.get("/one").run()) {
					var b = resp.body().as(JsonlParser.DEFAULT, Bean.class);
					assertEquals("solo", b.name);
					assertEquals(7, b.age);
				}
			}
		}
	}
}
