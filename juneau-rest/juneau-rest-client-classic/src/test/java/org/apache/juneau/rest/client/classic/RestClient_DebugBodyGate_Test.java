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
import java.util.*;
import java.util.logging.*;

import org.apache.juneau.http.*;
import org.apache.juneau.marshall.DetailLevel;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Verifies that the classic client's FULL request logging writes body text only when the body-dump gate is set.
 */
class RestClient_DebugBodyGate_Test {

	private static HttpServer server;
	private static String url;

	@BeforeAll
	static void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/echo", exchange -> {
			try (var in = exchange.getRequestBody()) { in.readAllBytes(); }
			var body = "response-body-text".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "text/plain");
			exchange.sendResponseHeaders(200, body.length);
			try (var os = exchange.getResponseBody()) { os.write(body); }
			exchange.close();
		});
		server.start();
		url = "http://127.0.0.1:" + server.getAddress().getPort();
	}

	@AfterAll
	static void stopServer() {
		if (server != null)
			server.stop(0);
	}

	@AfterEach
	void reset() {
		RestDebugBodyGate.setAllowDumpBodiesForTest(null);
	}

	private static String logged() throws Exception {
		var messages = new ArrayList<String>();
		var logger = Logger.getLogger(RestClient_DebugBodyGate_Test.class.getName() + System.nanoTime());
		logger.setUseParentHandlers(false);
		logger.setLevel(Level.ALL);
		logger.addHandler(new Handler() {
			@Override public void publish(LogRecord r) { messages.add(r.getMessage()); }
			@Override public void flush() { /* Nothing buffered */ }
			@Override public void close() { /* Nothing to release */ }
		});
		try (var c = RestClient.create().rootUrl(url).allowPrivateUrls(true).logger(logger).logRequests(DetailLevel.FULL, Level.INFO, null).build()) {
			c.post("/echo", "request-body-text").run().getContent().asString();
		}
		return String.join("\n", messages);
	}

	@Test
	void a01_gateUnset_noBodyText() throws Exception {
		RestDebugBodyGate.setAllowDumpBodiesForTest(Boolean.FALSE);
		var msg = logged();
		assertTrue(msg.contains("---response content---"), msg);
		assertFalse(msg.contains("response-body-text"), msg);
		assertFalse(msg.contains("request-body-text"), msg);
		assertTrue(msg.contains("body suppressed"), msg);
	}

	@Test
	void a02_gateSet_bodyTextPresent() throws Exception {
		RestDebugBodyGate.setAllowDumpBodiesForTest(Boolean.TRUE);
		var msg = logged();
		assertTrue(msg.contains("response-body-text"), msg);
		assertTrue(msg.contains("request-body-text"), msg);
	}
}
