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
import java.util.concurrent.atomic.*;

import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Tests that {@code {name}} path-variable values are encoded into a single path segment and that dot segments are refused.
 */
@Timeout(60)
class RestRequest_PathValues_Test {

	private static HttpServer server;
	private static int port;
	private static final AtomicReference<String> rawPath = new AtomicReference<>();

	@BeforeAll
	static void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		port = server.getAddress().getPort();
		server.createContext("/", exchange -> {
			rawPath.set(exchange.getRequestURI().getRawPath() + (exchange.getRequestURI().getRawQuery() == null ? "" : "?" + exchange.getRequestURI().getRawQuery()));
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

	private String call(String template, String name, String value) throws Exception {
		rawPath.set(null);
		try (var c = RestClient.create().build(); var r = c.get("http://127.0.0.1:" + port + template).pathData(name, value).run()) {
			r.getStatusLine();
		}
		return rawPath.get();
	}

	@Test void a01_specialCharactersAreEncodedInOneSegment() throws Exception {
		assertEquals("/x/a%20b%3Fc%23d/y", call("/x/{id}/y", "id", "a b?c#d"));
	}

	@Test void a02_percentIsEncoded() throws Exception {
		assertEquals("/x/a%2562", call("/x/{id}", "id", "a%62"));
	}

	@Test void a03_slashInSingleSegmentValueIsRefused() {
		assertThrows(Exception.class, () -> call("/x/{id}", "id", "a/b"));
	}

	@Test void a04_dotSegmentsAreRefused() {
		assertThrows(Exception.class, () -> call("/x/{id}", "id", ".."));
		assertThrows(Exception.class, () -> call("/x/*", "/*", "a/../b"));
	}

	@Test void a05_remainderKeepsSlashes() throws Exception {
		assertEquals("/x/a/b%20c", call("/x/*", "/*", "a/b c"));
	}

	@Test void a06_backslashIsEncoded() throws Exception {
		assertEquals("/x/a%5Cb", call("/x/{id}", "id", "a\\b"));
	}
}
