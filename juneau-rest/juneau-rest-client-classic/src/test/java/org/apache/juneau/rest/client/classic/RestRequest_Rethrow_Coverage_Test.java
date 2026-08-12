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

import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Exercises {@code RestRequest#rethrow(Class...)} and the <l>Thrown</l>-response-header exception-reconstruction
 * branches of {@code run()} -- previously uncovered since no other suite drives a server that emits a
 * <c>Thrown</c> header combined with a client-side {@code rethrow(...)} registration.
 */
@SuppressWarnings({
	"resource" // client() returns a RestClient whose ownership is transferred to the caller, who is responsible for closing it; Eclipse JDT's @Owning warning is by design.
})
class RestRequest_Rethrow_Coverage_Test {

	// Constructors (and classes) are public: RestRequest#run() locates rethrow candidates via
	// ClassInfo#getPublicConstructor(), which only sees public constructors.

	/** Has only a {@code (HttpResponse)} constructor. */
	public static class ExcWithResponseCtor extends RuntimeException {
		private static final long serialVersionUID = 1L;
		public ExcWithResponseCtor(org.apache.http.HttpResponse r) { super("response-ctor:" + r.getStatusLine().getStatusCode()); }
	}

	/** Has only a {@code (String)} constructor. */
	public static class ExcWithStringCtor extends RuntimeException {
		private static final long serialVersionUID = 1L;
		public ExcWithStringCtor(String msg) { super(msg); }
	}

	/** Has only a {@code (String, Throwable)} constructor. */
	public static class ExcWithStringThrowableCtor extends RuntimeException {
		private static final long serialVersionUID = 1L;
		public ExcWithStringThrowableCtor(String msg, Throwable cause) { super(msg, cause); }
	}

	/** Has only a no-arg constructor. */
	public static class ExcWithNoArgCtor extends RuntimeException {
		private static final long serialVersionUID = 1L;
		public ExcWithNoArgCtor() { super("no-arg"); }
	}

	/** Has no constructor matching any of the resolution attempts (only a non-matching (int) constructor). */
	public static class ExcWithNoUsableCtor extends RuntimeException {
		private static final long serialVersionUID = 1L;
		public ExcWithNoUsableCtor(int code) { super("code:" + code); }
	}

	/** A {@link Throwable} that is NOT an {@link Exception}, to trigger the non-Exception wrapping branch. */
	public static class NonExceptionThrowable extends Throwable {
		private static final long serialVersionUID = 1L;
		public NonExceptionThrowable() { super("non-exception"); }
	}

	private static HttpServer server;
	private static int port;

	@BeforeAll
	static void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(0), 0);
		port = server.getAddress().getPort();
		server.createContext("/thrown", exchange -> {
			var q = exchange.getRequestURI().getQuery();
			exchange.getResponseHeaders().add("Thrown", q);
			var body = "body".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		server.createContext("/emptyThrown", exchange -> {
			// Present but value-less Thrown header (asParts() present but empty list).
			exchange.getResponseHeaders().add("Thrown", "");
			var body = "body".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		server.createContext("/noBody", exchange -> {
			exchange.getResponseHeaders().add("Thrown", exchange.getRequestURI().getQuery());
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

	private static String url(String path) {
		return "http://localhost:" + port + path;
	}

	private static RestClient client() {
		return RestClient.create().build();
	}

	@Test void a01_rethrow_nullAndNonThrowableElements_areIgnored() throws Exception {
		try (var c = client()) {
			// Covers rethrow()'s per-element null-check and Throwable.class.isAssignableFrom() branches: a null
			// element, a non-Throwable class, and a valid Throwable class in the same varargs call.
			try (var req = c.get(url("/thrown?" + enc(ExcWithNoArgCtor.class.getName() + ";ignored")))
					.rethrow((Class<?>)null, String.class, ExcWithNoArgCtor.class)) {
				assertThrows(ExcWithNoArgCtor.class, req::run);
			}
		}
	}

	@Test void a02_rethrow_calledTwice_appendsToExistingList() throws Exception {
		try (var c = client()) {
			try (var req = c.get(url("/thrown?" + enc(ExcWithNoArgCtor.class.getName())))
					.rethrow(String.class)
					.rethrow(ExcWithNoArgCtor.class)) {
				assertThrows(ExcWithNoArgCtor.class, req::run);
			}
		}
	}

	@Test void b01_responseConstructor_preferredFirst() throws Exception {
		try (var c = client()) {
			try (var req = c.get(url("/thrown?" + enc(ExcWithResponseCtor.class.getName() + ";msg")))
					.rethrow(ExcWithResponseCtor.class)) {
				var e = assertThrows(ExcWithResponseCtor.class, req::run);
				assertTrue(e.getMessage().contains("response-ctor:200"), "Unexpected message: " + e.getMessage());
			}
		}
	}

	@Test void b02_stringConstructor_usesHeaderMessage() throws Exception {
		try (var c = client()) {
			try (var req = c.get(url("/thrown?" + enc(ExcWithStringCtor.class.getName() + ";custom-message")))
					.rethrow(ExcWithStringCtor.class)) {
				var e = assertThrows(ExcWithStringCtor.class, req::run);
				assertEquals("custom-message", e.getMessage());
			}
		}
	}

	@Test void b03_stringConstructor_noMessage_fallsBackToResponseBody() throws Exception {
		try (var c = client()) {
			// No ";message" segment -- Part.getMessage() is null, so the ternary falls back to
			// response.getContent().asString() instead of the (absent) header message.
			try (var req = c.get(url("/thrown?" + enc(ExcWithStringCtor.class.getName())))
					.rethrow(ExcWithStringCtor.class)) {
				var e = assertThrows(ExcWithStringCtor.class, req::run);
				assertEquals("body", e.getMessage());
			}
		}
	}

	@Test void b04_stringThrowableConstructor_usesHeaderMessage() throws Exception {
		try (var c = client()) {
			try (var req = c.get(url("/thrown?" + enc(ExcWithStringThrowableCtor.class.getName() + ";custom-message")))
					.rethrow(ExcWithStringThrowableCtor.class)) {
				var e = assertThrows(ExcWithStringThrowableCtor.class, req::run);
				assertEquals("custom-message", e.getMessage());
			}
		}
	}

	@Test void b05_stringThrowableConstructor_noMessage_fallsBackToResponseBody() throws Exception {
		try (var c = client()) {
			try (var req = c.get(url("/thrown?" + enc(ExcWithStringThrowableCtor.class.getName())))
					.rethrow(ExcWithStringThrowableCtor.class)) {
				var e = assertThrows(ExcWithStringThrowableCtor.class, req::run);
				assertEquals("body", e.getMessage());
			}
		}
	}

	@Test void b06_noArgConstructor_usedAsLastResort() throws Exception {
		try (var c = client()) {
			try (var req = c.get(url("/thrown?" + enc(ExcWithNoArgCtor.class.getName() + ";ignored")))
					.rethrow(ExcWithNoArgCtor.class)) {
				var e = assertThrows(ExcWithNoArgCtor.class, req::run);
				assertEquals("no-arg", e.getMessage());
			}
		}
	}

	@Test void b07_noUsableConstructor_thrownInstanceStaysNull_noRethrowOccurs() throws Exception {
		try (var c = client()) {
			// None of the four constructor resolution attempts match -- thrownInstance stays null, the
			// "if (nn(thrownInstance))" branch is skipped, and run() completes normally without rethrowing.
			try (var req = c.get(url("/thrown?" + enc(ExcWithNoUsableCtor.class.getName())))
					.rethrow(ExcWithNoUsableCtor.class)) {
				try (var res = req.run()) {
					assertEquals(200, res.getStatusCode());
				}
			}
		}
	}

	@Test void b08_nonExceptionThrowable_wrappedInRestCallException() throws Exception {
		try (var c = client()) {
			try (var req = c.get(url("/thrown?" + enc(NonExceptionThrowable.class.getName())))
					.rethrow(NonExceptionThrowable.class)) {
				var e = assertThrows(RestCallException.class, req::run);
				assertTrue(e.getMessage().contains("Server threw non-Exception"), "Unexpected message: " + e.getMessage());
			}
		}
	}

	@Test void c01_classNameNotInRethrowList_noRethrowOccurs() throws Exception {
		try (var c = client()) {
			// The Thrown header's className doesn't match any registered rethrow class, so the inner for-loop
			// never finds a match and run() completes normally.
			try (var req = c.get(url("/thrown?" + enc("some.other.Exception;msg")))
					.rethrow(ExcWithNoArgCtor.class)) {
				try (var res = req.run()) {
					assertEquals(200, res.getStatusCode());
				}
			}
		}
	}

	@Test void c02_emptyThrownHeader_partsAbsentOrEmpty_returnsResponseDirectly() throws Exception {
		try (var c = client()) {
			// Thrown header present but with an empty value -- asParts() is present but its list is empty, so
			// "!partsOpt.isPresent() || partsOpt.get().isEmpty()" short-circuits to returning the response as-is.
			try (var req = c.get(url("/emptyThrown")).rethrow(ExcWithNoArgCtor.class)) {
				try (var res = req.run()) {
					assertEquals(200, res.getStatusCode());
				}
			}
		}
	}

	@Test void d01_errorStatus_thrownHeaderDetailSuffix_appendedToMessage() throws Exception {
		try (var c = client()) {
			// No rethrow registered (so the Thrown header isn't consumed by reconstruction), but errorCodes still
			// treats the status as an error -- exercising getThrownDetailSuffix()'s present-and-populated branch.
			try (var req = c.get(url("/thrown?" + enc("some.pkg.Boom;kaboom"))).errorCodes(sc -> true)) {
				var e = assertThrows(RestCallException.class, req::run);
				assertTrue(e.getMessage().contains("Thrown: some.pkg.Boom: kaboom"), "Unexpected message: " + e.getMessage());
			}
		}
	}

	@Test void d02_errorStatus_noThrownHeader_noDetailSuffix() throws Exception {
		try (var c = client()) {
			// getThrownDetailSuffix()'s "!thrown.isPresent()" branch -- no Thrown header at all.
			try (var req = c.get(url("/noBody")).errorCodes(sc -> true)) {
				var e = assertThrows(RestCallException.class, req::run);
				assertFalse(e.getMessage().contains("Thrown:"), "Unexpected message: " + e.getMessage());
			}
		}
	}

	@Test void d03_errorStatus_emptyThrownHeader_partsEmpty_noDetailSuffix() throws Exception {
		try (var c = client()) {
			// getThrownDetailSuffix()'s "partsOpt.isPresent() && !isEmpty()" -- present but empty list.
			try (var req = c.get(url("/emptyThrown")).errorCodes(sc -> true)) {
				var e = assertThrows(RestCallException.class, req::run);
				assertFalse(e.getMessage().contains("Thrown:"), "Unexpected message: " + e.getMessage());
			}
		}
	}

	@Test void d04_errorStatus_thrownHeaderNoMessage_noDetailSuffix() throws Exception {
		try (var c = client()) {
			// className present but message blank -- "inb(className) && inb(message)" short-circuits false.
			try (var req = c.get(url("/thrown?" + enc("some.pkg.Boom"))).errorCodes(sc -> true)) {
				var e = assertThrows(RestCallException.class, req::run);
				assertFalse(e.getMessage().contains("Thrown:"), "Unexpected message: " + e.getMessage());
			}
		}
	}

	private static String enc(String s) {
		return URLEncoder.encode(s, StandardCharsets.UTF_8);
	}
}
