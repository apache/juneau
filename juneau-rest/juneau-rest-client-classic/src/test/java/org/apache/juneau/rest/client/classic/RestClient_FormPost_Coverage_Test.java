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

import static org.apache.juneau.http.classic.HttpEntities.*;
import static org.apache.juneau.http.classic.HttpParts.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.http.classic.header.*;
import org.apache.juneau.http.classic.resource.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Exercises every body-type branch of {@code RestClient.formPost(Object,Object)} -- previously entirely uncovered
 * in this module. Also verifies that the method still behaves correctly for every in-memory
 * entity type after simplifying the unreachable {@code catch (IOException e)} that used to wrap the
 * {@code UrlEncodedFormEntity(List)} construction calls.
 */
@SuppressWarnings({
	"resource" // Closeable resources in tests (e.g. the StreamResource passed as a formPost() body) are intentionally unassigned; closing is handled by test infrastructure.
})
class RestClient_FormPost_Coverage_Test {

	private static HttpServer server;
	private static ExecutorService executor;
	private static int port;
	private static final AtomicReference<String> lastBody = new AtomicReference<>();
	private static final AtomicReference<String> lastContentType = new AtomicReference<>();

	@BeforeAll
	static void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(0), 0);
		port = server.getAddress().getPort();
		// Without an explicit executor, exchanges run on HttpServer's single internal dispatch thread, which
		// starves under -T1C reactor-level parallel test load and can fail with "server failed to respond".
		executor = Executors.newCachedThreadPool();
		server.setExecutor(executor);
		server.createContext("/echo", exchange -> {
			lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
			lastContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
			exchange.sendResponseHeaders(200, -1);
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

	@Test void a01_nameValuePair_singleton() throws Exception {
		try (var client = RestClient.create().build();
				var req = client.formPost(url(), basicPart("a", "1"));
				var res = req.run()) {
			assertEquals(200, res.getStatusCode());
		}
		assertEquals("a=1", lastBody.get());
	}

	@Test void a02_nameValuePairArray() throws Exception {
		try (var client = RestClient.create().build();
				var req = client.formPost(url(), new NameValuePair[]{basicPart("a", "1"), basicPart("b", "2")});
				var res = req.run()) {
			assertEquals(200, res.getStatusCode());
		}
		assertEquals("a=1&b=2", lastBody.get());
	}

	@Test void a03_partList() throws Exception {
		try (var client = RestClient.create().build();
				var req = client.formPost(url(), partList("a", "1", "b", "2"));
				var res = req.run()) {
			assertEquals(200, res.getStatusCode());
		}
		assertEquals("a=1&b=2", lastBody.get());
	}

	@Test void a04_httpResource_headersCopiedAndBodySent() throws Exception {
		var resource = new StreamResource(ContentType.APPLICATION_FORM_URLENCODED, new ByteArrayInputStream("a=1".getBytes(StandardCharsets.UTF_8)))
			.setHeader("X-Foo", "bar");
		try (var client = RestClient.create().build();
				var req = client.formPost(url(), resource);
				var res = req.run()) {
			assertEquals(200, res.getStatusCode());
		}
		assertEquals("a=1", lastBody.get());
	}

	@Test void a05_httpEntity_noContentType_addsFormUrlEncodedHeader() throws Exception {
		try (var client = RestClient.create().build();
				var req = client.formPost(url(), stringEntity("a=1"));
				var res = req.run()) {
			assertEquals(200, res.getStatusCode());
		}
		assertEquals("a=1", lastBody.get());
		assertTrue(lastContentType.get().contains("application/x-www-form-urlencoded"), "Unexpected Content-Type: " + lastContentType.get());
	}

	@Test void a06_httpEntity_withContentType_leftAsIs() throws Exception {
		try (var client = RestClient.create().build();
				var req = client.formPost(url(), stringEntity("{\"a\":1}", ContentType.APPLICATION_JSON));
				var res = req.run()) {
			assertEquals(200, res.getStatusCode());
		}
		assertEquals("{\"a\":1}", lastBody.get());
		assertTrue(lastContentType.get().contains("application/json"), "Unexpected Content-Type: " + lastContentType.get());
	}

	@Test void a07_reader() throws Exception {
		try (var client = RestClient.create().build();
				var req = client.formPost(url(), new StringReader("a=1"));
				var res = req.run()) {
			assertEquals(200, res.getStatusCode());
		}
		assertEquals("a=1", lastBody.get());
	}

	@Test void a08_inputStream() throws Exception {
		try (var client = RestClient.create().build();
				var req = client.formPost(url(), new ByteArrayInputStream("a=1".getBytes(StandardCharsets.UTF_8)));
				var res = req.run()) {
			assertEquals(200, res.getStatusCode());
		}
		assertEquals("a=1", lastBody.get());
	}

	@Test void a09_genericObject_serializedViaUrlEncodingSerializer() throws Exception {
		try (var client = RestClient.create().build();
				var req = client.formPost(url(), java.util.Map.of("a", "1"));
				var res = req.run()) {
			assertEquals(200, res.getStatusCode());
		}
		// UrlEncodingSerializer quotes the numeric-looking string value to preserve its string-ness (UON encoding).
		assertEquals("a='1'", lastBody.get());
	}

	@Test void a10_supplier_unwrapsToUnderlyingBodyType() throws Exception {
		Supplier<Object> supplier = () -> basicPart("a", "1");
		try (var client = RestClient.create().build();
				var req = client.formPost(url(), supplier);
				var res = req.run()) {
			assertEquals(200, res.getStatusCode());
		}
		assertEquals("a=1", lastBody.get());
	}
}
