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
package org.apache.juneau.rest.client.classic;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.concurrent.*;

import org.apache.http.*;
import org.apache.juneau.marshall.cbor.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.jsonl.*;
import org.apache.juneau.marshall.stream.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Exercises the private {@code ResponseContent#asCursor(ClassMeta)} helper (reached via
 * {@code ResponseContent#as(Class)} for a {@link RecordReader}/{@link TokenReader} target type) -- entirely
 * uncovered previously, since none of the {@code juneau-integration-tests} suites request a cursor return type.
 * Mirrors the equivalent non-classic {@code ResponseBody_Cursor_Test}.
 */
@SuppressWarnings({
	"resource" // jsonContent() returns a ResponseContent while the underlying client (and its still-unconsumed response) must stay open for the caller to read the returned cursor; Eclipse JDT's @Owning warning is by design.
})
class ResponseContent_Cursor_Test {

	public static class Bean {
		public String name;
		public int age;
	}

	private static HttpServer server;
	private static ExecutorService executor;
	private static int port;
	private static volatile byte[] responseBody = new byte[0];
	private static volatile String responseContentType = "application/json";
	private static volatile int responseStatus = 200;

	@BeforeAll
	static void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(0), 0);
		port = server.getAddress().getPort();
		// Without an explicit executor, exchanges run on HttpServer's single internal dispatch thread, which
		// starves under -T1C reactor-level parallel test load and can fail with "server failed to respond".
		executor = Executors.newCachedThreadPool();
		server.setExecutor(executor);
		server.createContext("/echo", exchange -> {
			exchange.getRequestBody().readAllBytes();
			if (responseContentType != null)
				exchange.getResponseHeaders().add("Content-Type", responseContentType);
			if (responseBody.length == 0 && responseStatus == 204) {
				exchange.sendResponseHeaders(204, -1);
			} else {
				exchange.sendResponseHeaders(responseStatus, responseBody.length);
				exchange.getResponseBody().write(responseBody);
			}
			exchange.close();
		});
		server.start();
	}

	@AfterAll
	static void stopServer() {
		if (server != null)
			server.stop(0);
		if (executor != null)
			executor.shutdownNow();
	}

	private static String url() {
		return "http://localhost:" + port + "/echo";
	}

	private static ResponseContent jsonContent(String json) throws Exception {
		responseBody = json.getBytes(StandardCharsets.UTF_8);
		responseContentType = "application/json";
		responseStatus = 200;
		var client = RestClient.create().json().build();
		return client.get(url()).run().getContent();
	}

	// ==========================================================================
	// a - record / token cursors
	// ==========================================================================

	@Test void a01_recordCursor_explicitParser() throws Exception {
		try (RecordReader r = jsonContent("{\"name\":\"alice\",\"age\":30}").parser(JsonParser.DEFAULT).as(RecordReader.class)) {
			var b = r.read(Bean.class);
			assertEquals("alice", b.name);
			assertEquals(30, b.age);
		}
	}

	@Test void a02_recordCursor_negotiatedFromClient() throws Exception {
		try (RecordReader r = jsonContent("{\"name\":\"bob\",\"age\":40}").as(RecordReader.class)) {
			var b = r.read(Bean.class);
			assertEquals("bob", b.name);
			assertEquals(40, b.age);
		}
	}

	@Test void a03_tokenCursor() throws Exception {
		try (TokenReader r = jsonContent("{\"name\":\"carol\",\"age\":50}").parser(JsonParser.DEFAULT).as(TokenReader.class)) {
			var b = r.read(Bean.class);
			assertEquals("carol", b.name);
			assertEquals(50, b.age);
		}
	}

	@Test void a04_concreteCursorType() throws Exception {
		try (JsonTokenReader r = jsonContent("{\"name\":\"dan\",\"age\":60}").parser(JsonParser.DEFAULT).as(JsonTokenReader.class)) {
			var b = r.read(Bean.class);
			assertEquals("dan", b.name);
		}
	}

	// ==========================================================================
	// b - error paths
	// ==========================================================================

	@Test void b01_cursorTypeNotAssignable() throws Exception {
		// JsonParser produces a JsonTokenReader, which is not assignable to JsonlTokenReader.
		var content = jsonContent("{\"name\":\"x\",\"age\":1}").parser(JsonParser.DEFAULT);
		var e = assertThrows(RestCallException.class, () -> content.as(JsonlTokenReader.class));
		assertTrue(e.getMessage().contains("not assignable"), "Unexpected message: " + e.getMessage());
	}

	@Test void b02_tokenCursorRequestedFromRecordOnlyParser_unsupported() throws Exception {
		// IniParser implements RecordReadable but not TokenReadable, so a TokenReader cursor is unsupported
		// (TokenReadable extends RecordReadable, so the reverse combination -- RecordReader from a TokenReadable
		// parser -- is always supported and cannot exercise this branch).
		responseBody = "a=1".getBytes(StandardCharsets.UTF_8);
		responseContentType = "text/ini";
		responseStatus = 200;
		try (var client = RestClient.create().build()) {
			try (var req = client.get(url()); var res = req.run()) {
				var content = res.getContent().parser(org.apache.juneau.marshall.ini.IniParser.DEFAULT);
				var e = assertThrows(RestCallException.class, () -> content.as(TokenReader.class));
				assertTrue(e.getMessage().contains("token-reader"), "Unexpected message: " + e.getMessage());
			}
		}
	}

	@Test void b02b_recordCursorRequestedFromNeitherCapableParser_unsupported() throws Exception {
		// ProtobufParser implements neither RecordReadable nor TokenReadable, covering the "record-reader" side
		// of the exception message's isToken ternary (isToken==false here since RecordReader is requested).
		responseBody = "a=1".getBytes(StandardCharsets.UTF_8);
		responseContentType = "application/x-protobuf";
		responseStatus = 200;
		try (var client = RestClient.create().build()) {
			try (var req = client.get(url()); var res = req.run()) {
				var content = res.getContent().parser(org.apache.juneau.marshall.protobuf.ProtobufParser.create().build());
				var e = assertThrows(RestCallException.class, () -> content.as(RecordReader.class));
				assertTrue(e.getMessage().contains("record-reader"), "Unexpected message: " + e.getMessage());
			}
		}
	}

	@Test void b02c_ioExceptionOpeningStream_wrappedAsRestCallException() throws Exception {
		// A RestCallInterceptor#onConnect substitution of the live entity's getContent() (see the analogous
		// technique in ResponseContent_Coverage_Test) makes asInputStream() throw IOException, which asCursor()'s
		// own try block must wrap as a RestCallException rather than letting it escape unwrapped.
		var itcp = new BasicRestCallInterceptor() {
			@Override
			public void onConnect(RestRequest req, RestResponse res) {
				res.asHttpResponse().setEntity(new HttpEntity() {
					@Override public boolean isRepeatable() { return false; }
					@Override public boolean isChunked() { return false; }
					@Override public long getContentLength() { return -1; }
					@Override public Header getContentType() { return null; }
					@Override public Header getContentEncoding() { return null; }
					@Override public InputStream getContent() { throw new UnsupportedOperationException("Simulated: entity content not available."); }
					@Override public void writeTo(OutputStream outstream) { throw new UnsupportedOperationException("Not used by this test."); }
					@Override public boolean isStreaming() { return false; }
					@Override public void consumeContent() { /* no-op */ }
				});
			}
		};
		responseBody = "{}".getBytes(StandardCharsets.UTF_8);
		responseContentType = "application/json";
		responseStatus = 200;
		try (var client = RestClient.create().interceptors(itcp).build()) {
			try (var req = client.get(url()); var res = req.run()) {
				var content = res.getContent().parser(JsonParser.DEFAULT);
				var e = assertThrows(RestCallException.class, () -> content.as(TokenReader.class));
				assertTrue(e.getMessage().contains("Could not open cursor"), "Unexpected message: " + e.getMessage());
			}
		}
	}

	@Test void b03_noMatchingParser_throwsRestCallException() throws Exception {
		responseBody = "{}".getBytes(StandardCharsets.UTF_8);
		responseContentType = "application/x-no-such-type";
		responseStatus = 200;
		try (var client = RestClient.create().build()) {
			try (var req = client.get(url()); var res = req.run()) {
				var content = res.getContent();
				var e = assertThrows(RestCallException.class, () -> content.as(TokenReader.class));
				assertTrue(e.getMessage().contains("No registered parser"), "Unexpected message: " + e.getMessage());
			}
		}
	}

	// ==========================================================================
	// c - InputStreamParser (non-reader) path: parser.isReaderParser()==false
	// ==========================================================================

	@Test void c01_tokenCursor_inputStreamParser() throws Exception {
		// CborParser is an InputStreamParser (isReaderParser()==false) that implements TokenReadable, exercising
		// asCursor's "input = stream" (as opposed to "new InputStreamReader(stream, UTF_8)") branch.
		var bean = new Bean();
		bean.name = "eve";
		bean.age = 70;
		responseBody = CborSerializer.DEFAULT.write(bean);
		responseContentType = "application/cbor";
		responseStatus = 200;
		try (var client = RestClient.create().build()) {
			try (var req = client.get(url()); var res = req.run()) {
				var content = res.getContent();
				try (TokenReader r = content.parser(CborParser.DEFAULT).as(TokenReader.class)) {
					var b = r.read(Bean.class);
					assertEquals("eve", b.name);
					assertEquals(70, b.age);
				}
			}
		}
	}
}
