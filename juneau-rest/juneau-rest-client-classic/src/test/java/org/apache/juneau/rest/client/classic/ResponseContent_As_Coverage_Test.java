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

import org.apache.juneau.http.classic.resource.*;
import org.apache.juneau.marshall.json.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Exercises the {@code HttpResource}/{@code BasicResource} target-type redirect branch of
 * {@link ResponseContent#as(org.apache.juneau.marshall.httppart.ClassMeta)} -- retargets the requested type to
 * {@link StreamResource} before the generic {@code HttpResponse}-arg-constructor lookup, since neither
 * {@code HttpResource} nor {@code BasicResource} is directly instantiable.
 */
@SuppressWarnings({
	"resource" // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
})
class ResponseContent_As_Coverage_Test {

	private static HttpServer server;
	private static ExecutorService executor;
	private static int port;

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
			var body = "hello".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "text/plain");
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		server.createContext("/nullJson", exchange -> {
			exchange.getRequestBody().readAllBytes();
			var body = "null".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		server.createContext("/noContentType", exchange -> {
			exchange.getRequestBody().readAllBytes();
			// Deliberately omits any Content-Type header.
			var body = "irrelevant".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
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

	private static String nullJsonUrl() {
		return "http://localhost:" + port + "/nullJson";
	}

	private static String noContentTypeUrl() {
		return "http://localhost:" + port + "/noContentType";
	}

	/** Public no-arg constructor, so the "parser returned null but the type is constructible" fallback applies. */
	public static class Widget {
		public String name = "default";
	}

	/** An interface has no constructors at all (so neither the {@code HttpResponse}-arg lookup nor a single-String
	 * constructor "string mutater" applies), so {@code as()} falls all the way through to the final
	 * Content-Type-negotiation-failure branches. */
	public interface UnconstructibleTarget {}

	@Test void a01_as_httpResource_returnsStreamResource() throws Exception {
		try (var client = RestClient.create().build();
				var req = client.get(url());
				var res = req.run()) {
			var r = res.getContent().as(HttpResource.class);
			assertInstanceOf(StreamResource.class, r);
			assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), readAll(r));
		}
	}

	@Test void a02_as_basicResource_returnsStreamResource() throws Exception {
		try (var client = RestClient.create().build();
				var req = client.get(url());
				var res = req.run()) {
			var r = res.getContent().as(BasicResource.class);
			assertInstanceOf(StreamResource.class, r);
			assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), readAll(r));
		}
	}

	private static byte[] readAll(HttpResource r) throws IOException {
		try (var in = r.getContent()) {
			return in.readAllBytes();
		}
	}

	@Test void a03_as_noArgCtorFallback_whenParserReturnsNull() throws Exception {
		// The JSON parser legitimately reads the "null" literal as a Java null; since Widget has a public no-arg
		// constructor (and isn't String), as() falls back to constructing a default instance instead of returning null.
		try (var client = RestClient.create().json().build();
				var req = client.get(nullJsonUrl());
				var res = req.run()) {
			var w = res.getContent().as(Widget.class);
			assertNotNull(w);
			assertEquals("default", w.name);
		}
	}

	@SuppressWarnings({
		"unchecked" // generic varargs array (Class<? extends Parser>...) is safe here.
	})
	@Test void a04_as_noContentTypeHeader_clientHasParsers_throwsContentTypeNotSpecified() throws Exception {
		// getMatchingParser() falls back to the sole registered parser when there's exactly one, regardless of media
		// type -- registering two (neither matching "text/plain", the implicit default when no header is present)
		// forces it to return null despite hasParsers() being true, which is what this branch actually needs.
		try (var client = RestClient.create().parsers(JsonParser.class, org.apache.juneau.marshall.xml.XmlParser.class).build();
				var req = client.get(noContentTypeUrl());
				var res = req.run()) {
			var e = assertThrows(RestCallException.class, () -> res.getContent().as(UnconstructibleTarget.class));
			var causeMsg = e.getCause().getMessage();
			assertTrue(causeMsg.contains("Content-Type not specified"), "Unexpected cause message: " + causeMsg);
		}
	}

	@Test void a05_as_noContentTypeHeader_clientHasNoParsers_throwsUnsupportedMediaType() throws Exception {
		try (var client = RestClient.create().build();
				var req = client.get(noContentTypeUrl());
				var res = req.run()) {
			var e = assertThrows(RestCallException.class, () -> res.getContent().as(UnconstructibleTarget.class));
			var causeMsg = e.getCause().getMessage();
			assertTrue(causeMsg.contains("Unsupported media-type"), "Unexpected cause message: " + causeMsg);
		}
	}
}
