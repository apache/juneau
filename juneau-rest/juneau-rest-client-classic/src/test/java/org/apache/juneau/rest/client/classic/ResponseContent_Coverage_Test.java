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

import org.apache.http.*;
import org.apache.juneau.http.classic.entity.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Exercises remaining {@link ResponseContent} branches not reached by {@code juneau-integration-tests}:
 * the {@code entity instanceof BasicHttpEntity} fast path in {@code asBytes()}, the
 * {@code UnsupportedOperationException}-to-{@code IOException} wrapping branch of {@code asInputStream()}, the
 * {@code asHex()}/{@code asSpacedHex()} convenience methods, and the {@code assertBytes()}/{@code assertObject(...)}/
 * {@code assertString()} fluent-assertion shortcuts.
 */
@SuppressWarnings({
	"resource" // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
})
class ResponseContent_Coverage_Test {

	private static HttpServer server;
	private static int port;

	@BeforeAll
	static void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(0), 0);
		port = server.getAddress().getPort();
		server.createContext("/echo", exchange -> {
			exchange.getRequestBody().readAllBytes();
			var body = "hello".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "text/plain");
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		server.createContext("/truncated", exchange -> {
			exchange.getRequestBody().readAllBytes();
			var body = "hi".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "text/plain");
			// Declares a larger Content-Length than the bytes actually written, so reading the full declared length
			// triggers an IOException partway through -- exercises asString()/toString()'s catch(IOException) branch.
			exchange.sendResponseHeaders(200, body.length + 1000);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		server.start();
	}

	@AfterAll
	static void stopServer() {
		if (server != null)
			server.stop(0);
	}

	private static String url() {
		return "http://localhost:" + port + "/echo";
	}

	private static String truncatedUrl() {
		return "http://localhost:" + port + "/truncated";
	}

	@Test void a01_asBytes_basicHttpEntityFastPath() throws Exception {
		// An org.apache.http.HttpResponseInterceptor runs as part of the HttpClient's own response-processing
		// pipeline -- before RestClient wraps the raw HttpResponse into a RestResponse/ResponseContent -- so
		// replacing the entity here (unlike via a RestCallInterceptor#onConnect, which runs too late) is visible to
		// ResponseContent's captured `entity` field, exercising the `entity instanceof BasicHttpEntity<?>` fast path.
		var content = "fast-path".getBytes(StandardCharsets.UTF_8);
		HttpResponseInterceptor itcp = (response, context) -> response.setEntity(new ByteArrayEntity(null, content));
		try (var client = RestClient.create().interceptors(itcp).build();
				var req = client.get(url());
				var res = req.run()) {
			assertArrayEquals(content, res.getContent().asBytes());
		}
	}

	@Test void a02_asInputStream_unsupportedOperationException_wrappedAsIOException() throws Exception {
		// asInputStream()'s non-cached branch re-fetches the *live* entity from the raw HttpResponse on every call
		// (unlike asBytes(), which uses the field captured at construction), so a RestCallInterceptor#onConnect
		// substitution -- which runs after ResponseContent is constructed but before the caller reads the body --
		// is late enough to be visible here.
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
		try (var client = RestClient.create().interceptors(itcp).build();
				var req = client.get(url());
				var res = req.run()) {
			assertThrows(IOException.class, () -> res.getContent().asInputStream());
		}
	}

	@Test void a03_asHex() throws Exception {
		try (var client = RestClient.create().build();
				var req = client.get(url());
				var res = req.run()) {
			assertEquals("68656C6C6F", res.getContent().asHex());
		}
	}

	@Test void a04_asSpacedHex() throws Exception {
		try (var client = RestClient.create().build();
				var req = client.get(url());
				var res = req.run()) {
			assertEquals("68 65 6C 6C 6F", res.getContent().asSpacedHex());
		}
	}

	@Test void a05_assertBytes() throws Exception {
		try (var client = RestClient.create().build();
				var req = client.get(url());
				var res = req.run()) {
			res.getContent().assertBytes().asHex().is("68656C6C6F");
		}
	}

	@Test void a06_assertObject_class() throws Exception {
		try (var client = RestClient.create().build();
				var req = client.get(url());
				var res = req.run()) {
			res.getContent().assertObject(String.class).is("hello");
		}
	}

	@Test void a07_assertObject_typeAndArgs() throws Exception {
		try (var client = RestClient.create().build();
				var req = client.get(url());
				var res = req.run()) {
			res.getContent().assertObject((java.lang.reflect.Type)String.class).is("hello");
		}
	}

	@Test void a08_assertString() throws Exception {
		try (var client = RestClient.create().build();
				var req = client.get(url());
				var res = req.run()) {
			res.getContent().assertString().is("hello");
		}
	}

	@Test void a09_toString_ioException_returnsLocalizedMessage() throws Exception {
		try (var client = RestClient.create().build();
				var req = client.get(truncatedUrl());
				var res = req.run()) {
			var s = res.getContent().toString();
			assertNotNull(s);
			assertFalse(s.contains("hi"), "Expected an error message, not the (truncated) body content: " + s);
		}
	}

	@Test void a10_pipeTo_writer_byLines() throws Exception {
		try (var client = RestClient.create().build();
				var req = client.get(url());
				var res = req.run()) {
			var w = new StringWriter();
			res.getContent().pipeTo(w, StandardCharsets.UTF_8, true);
			// pipeLines() re-terminates each line it writes, even though the source line lacked a trailing newline.
			assertEquals("hello", w.toString().strip());
		}
	}

	@Test void a11_nullEntitySentinel_pipeToAndAsBytes_neverDelegateToItsWriteTo() throws Exception {
		// ResponseContent's NULL_ENTITY sentinel (used when the raw HttpResponse has no entity)
		// has its own writeTo(OutputStream) simplified to fail fast (it's never legitimately invoked -- see the
		// in-source comment). Forcing the sentinel into play via an HttpResponseInterceptor (which runs before
		// ResponseContent captures `entity`, same as a01 above) and exercising both read paths confirms neither
		// pipeTo(OutputStream) (-> ResponseContent#writeTo -> pipeTo -> asInputStream -> entity.getContent()) nor
		// asBytes() ever reaches NULL_ENTITY.writeTo(...); if they did, this test would fail with an
		// UnsupportedOperationException instead of observing empty content.
		HttpResponseInterceptor itcp = (response, context) -> response.setEntity(null);
		try (var client = RestClient.create().interceptors(itcp).build();
				var req = client.get(url());
				var res = req.run()) {
			assertArrayEquals(new byte[0], res.getContent().asBytes());
		}
		try (var client = RestClient.create().interceptors(itcp).build();
				var req = client.get(url());
				var res = req.run()) {
			var os = new ByteArrayOutputStream();
			res.getContent().pipeTo(os);
			assertArrayEquals(new byte[0], os.toByteArray());
		}
	}
}
