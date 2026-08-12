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
package org.apache.juneau.rest.client.classic.assertion;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.charset.*;

import org.apache.juneau.http.classic.response.*;
import org.apache.juneau.rest.client.classic.*;
import org.apache.juneau.test.assertions.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Exercises the {@link FluentResponseBodyAssertion} transform/configuration methods and its
 * {@code RestCallException} wrapping branches against a real local server -- a real
 * {@link org.apache.http.HttpEntity} stream is needed to reach the {@code IOException} paths (a
 * Content-Length that overstates the bytes actually written triggers a connection-closed IOException when
 * the client tries to read out to the declared length). The wrapped exception is a {@link BadRequest}, not an
 * {@link AssertionError}, because the class's constructor configures {@code BadRequest} as its default
 * {@code throwable} (see {@link org.apache.juneau.test.assertions.Assertion#setThrowable(Class)}).
 */
@SuppressWarnings({
	"resource" // okResponse()/brokenResponse() return a RestResponse whose ownership is transferred to the caller, who is responsible for closing it; the underlying client is a short-lived test fixture and Eclipse JDT's @Owning warning is by design.
})
class FluentResponseBodyAssertion_Test {

	private static HttpServer server;
	private static int port;

	@BeforeAll
	static void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(0), 0);
		port = server.getAddress().getPort();
		server.createContext("/ok", exchange -> {
			var body = "hello".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "text/plain");
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		server.createContext("/broken", exchange -> {
			// Declares far more bytes than are actually written, then closes the connection early so the
			// client's read of the entity content fails with a connection-closed IOException mid-stream.
			exchange.getResponseHeaders().add("Content-Type", "text/plain");
			exchange.sendResponseHeaders(200, 10_000);
			exchange.getResponseBody().write("short".getBytes(StandardCharsets.UTF_8));
			exchange.close();
		});
		server.start();
	}

	@AfterAll
	static void stopServer() {
		if (server != null)
			server.stop(0);
	}

	private static RestResponse okResponse() throws Exception {
		var client = RestClient.create().rootUrl("http://localhost:" + port).build();
		return client.get("/ok").run();
	}

	private static RestResponse brokenResponse() throws Exception {
		var client = RestClient.create().rootUrl("http://localhost:" + port).build();
		return client.get("/broken").run();
	}

	@Test void a01_as_typeVarargs() throws Exception {
		try (var response = okResponse()) {
			new FluentResponseBodyAssertion<>(response.getContent(), null).as((Type)String.class).is("hello");
		}
	}

	@Test void a02_isNotEmpty() throws Exception {
		try (var response = okResponse()) {
			new FluentResponseBodyAssertion<>(response.getContent(), null).isNotEmpty();
		}
	}

	@Test void a03_configMethods_returnThis() throws Exception {
		try (var response = okResponse()) {
			var a = new FluentResponseBodyAssertion<>(response.getContent(), null);
			assertSame(a, a.setOut(System.out));
			assertSame(a, a.setSilent());
			assertSame(a, a.setStdOut());
		}
	}

	@Test void b01_asBytes_ioFailure_wrapsInConfiguredThrowable() throws Exception {
		try (var response = brokenResponse()) {
			var a = new FluentResponseBodyAssertion<>(response.getContent(), null);
			// Default throwable is BadRequest (set by the constructor), not AssertionError.
			assertThrows(BadRequest.class, a::asBytes);
		}
	}

	@Test void b02_asType_ioFailure_wrapsInConfiguredThrowable() throws Exception {
		try (var response = brokenResponse()) {
			var a = new FluentResponseBodyAssertion<>(response.getContent(), null);
			assertThrows(BadRequest.class, () -> a.as((Type)String.class));
		}
	}

	@Test void b03_asString_ioFailure_wrapsInConfiguredThrowable() throws Exception {
		try (var response = brokenResponse()) {
			var a = new FluentResponseBodyAssertion<>(response.getContent(), null);
			assertThrows(BadRequest.class, a::asString);
		}
	}

	// c01-c03: Assertion.error(cause, msg) throws its configured throwable internally when one is set (as it
	// always is by default here), so the explicit "throw error(...)" statement in valueAsBytes()/valueAsType()/
	// valueAsString() is normally unreachable. Clearing the throwable (setThrowable(null)) makes error() return
	// normally instead, so that outer throw actually fires -- the only way to reach it.

	@Test void c01_asBytes_ioFailure_noConfiguredThrowable_throwsBasicAssertionError() throws Exception {
		try (var response = brokenResponse()) {
			var a = new FluentResponseBodyAssertion<>(response.getContent(), null).setThrowable(null);
			assertThrows(BasicAssertionError.class, a::asBytes);
		}
	}

	@Test void c02_asType_ioFailure_noConfiguredThrowable_throwsBasicAssertionError() throws Exception {
		try (var response = brokenResponse()) {
			var a = new FluentResponseBodyAssertion<>(response.getContent(), null).setThrowable(null);
			assertThrows(BasicAssertionError.class, () -> a.as((Type)String.class));
		}
	}

	@Test void c03_asString_ioFailure_noConfiguredThrowable_throwsBasicAssertionError() throws Exception {
		try (var response = brokenResponse()) {
			var a = new FluentResponseBodyAssertion<>(response.getContent(), null).setThrowable(null);
			assertThrows(BasicAssertionError.class, a::asString);
		}
	}
}
