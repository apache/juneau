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

import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Exercises the {@link ResponseHeader} convenience conversion methods that aren't already reached by
 * other end-to-end client tests.
 */
class ResponseHeader_Test {

	private static HttpServer server;
	private static int port;

	@BeforeAll
	static void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(0), 0);
		port = server.getAddress().getPort();
		server.createContext("/headers", exchange -> {
			var h = exchange.getResponseHeaders();
			h.add("X-Bool", "true");
			h.add("X-Csv", "a,b,c");
			h.add("X-Date", "Sun, 06 Nov 1994 08:49:37 GMT");
			h.add("X-Etag", "\"abc123\"");
			h.add("X-Int", "42");
			exchange.sendResponseHeaders(200, -1);
			exchange.close();
		});
		server.start();
	}

	@AfterAll
	static void stopServer() {
		if (server != null)
			server.stop(0);
	}

	@SuppressWarnings("resource") // Client/response instances are short-lived test fixtures.
	private static RestResponse response() throws Exception {
		var client = RestClient.create().rootUrl("http://localhost:" + port).build();
		return client.get("/headers").run();
	}

	@Test void a01_asBoolean() throws Exception {
		try (var response = response()) {
			assertEquals(true, response.getHeader("X-Bool").asBoolean().orElse(null));
		}
	}

	@Test void a02_asCsvArray() throws Exception {
		try (var response = response()) {
			assertArrayEquals(new String[]{"a","b","c"}, response.getHeader("X-Csv").asCsvArray().orElse(null));
		}
	}

	@Test void a03_asDate() throws Exception {
		try (var response = response()) {
			var d = response.getHeader("X-Date").asDate();
			assertTrue(d.isPresent());
			assertEquals(1994, d.get().getYear());
		}
	}

	@Test void a04_asEntityTagHeader() throws Exception {
		try (var response = response()) {
			assertEquals("\"abc123\"", response.getHeader("X-Etag").asEntityTagHeader().getValue());
		}
	}

	@Test void a05_assertInteger() throws Exception {
		try (var response = response()) {
			response.getHeader("X-Int").assertInteger().is(42);
		}
	}

	@Test void a06_assertLong() throws Exception {
		try (var response = response()) {
			response.getHeader("X-Int").assertLong().is(42L);
		}
	}

	@Test void a07_assertString() throws Exception {
		try (var response = response()) {
			response.getHeader("X-Csv").assertString().is("a,b,c");
		}
	}

	@Test void a08_assertZonedDateTime() throws Exception {
		try (var response = response()) {
			response.getHeader("X-Date").assertZonedDateTime().isNotNull();
		}
	}
}
