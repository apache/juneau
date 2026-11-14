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
package org.apache.juneau.rest.client;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;

import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Verifies the default limits of {@code JavaHttpTransport}: bounded default timeouts and a response body size cap.
 */
@SuppressWarnings("resource") // Transport/response instances are short-lived test fixtures.
class JavaHttpTransport_Limits_Test {

	private static HttpServer server;
	private static int port;

	@BeforeAll
	static void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		port = server.getAddress().getPort();
		server.createContext("/body", exchange -> {
			var body = new byte[1000];
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		server.start();
	}

	@AfterAll
	static void stopServer() {
		server.stop(0);
	}

	private static TransportRequest request() {
		return TransportRequest.builder().method("GET").uri("http://127.0.0.1:" + port + "/body").build();
	}

	@Test void a01_defaultsAreBounded() {
		assertTrue(JavaHttpTransport.DEFAULT_CONNECT_TIMEOUT.toSeconds() > 0);
		assertTrue(JavaHttpTransport.DEFAULT_REQUEST_TIMEOUT.toSeconds() > 0);
		assertTrue(JavaHttpTransport.DEFAULT_MAX_RESPONSE_BYTES > 0);
	}

	@Test void a02_bodyWithinCap_isRead() throws Exception {
		try (var transport = JavaHttpTransport.builder().maxResponseBytes(1000).build(); var response = transport.execute(request())) {
			assertEquals(1000, response.getBody().readAllBytes().length);
		}
	}

	@Test void a03_bodyOverCap_isRefused() throws Exception {
		try (var transport = JavaHttpTransport.builder().maxResponseBytes(999).build(); var response = transport.execute(request())) {
			var in = response.getBody();
			assertThrows(IOException.class, in::readAllBytes);
		}
	}

	@Test void a04_nonPositiveCap_isUnlimited() throws Exception {
		try (var transport = JavaHttpTransport.builder().maxResponseBytes(0).build(); var response = transport.execute(request())) {
			assertEquals(1000, response.getBody().readAllBytes().length);
		}
	}
}
