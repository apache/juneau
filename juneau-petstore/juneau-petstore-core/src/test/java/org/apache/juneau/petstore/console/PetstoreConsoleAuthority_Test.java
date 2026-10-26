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
package org.apache.juneau.petstore.console;

import static org.apache.juneau.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.net.http.HttpRequest.*;
import java.net.http.HttpResponse.*;
import java.time.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.petstore.console.browser.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;

/**
 * The {@value PetstoreConsoleBoundary#AUTHORITY_PROPERTY} setting on a real server: set before the first console
 * request, it replaces {@code localhost:<port>} as the one {@code Host} (and {@code Origin}) the console accepts.
 */
@SuppressWarnings({
	"java:S2925", // start() polls a starting server; without a back-off a refused connection would busy-spin.
	"java:S8692" // start() polls a real HTTP server against a wall-clock deadline.
})
class PetstoreConsoleAuthority_Test extends TestBase {

	@RegisterExtension
	static MicroserviceTestFixture server = PetstoreTestServer.fixture();

	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
	private static final Pattern TOKEN = Pattern.compile("<body[^>]*\\sdata-juneau-csrf=\"([^\"]+)\"");

	private static String base;

	@BeforeAll
	static void start() throws Exception {
		// The server has bound but served no console request yet, so the boundary has not fixed its authority.
		base = "http://127.0.0.1:" + server.getPort();
		System.setProperty(PetstoreConsoleBoundary.AUTHORITY_PROPERTY, "127.0.0.1:" + server.getPort());
		var deadline = Instant.now().plusSeconds(30);
		for (var r = page(base); r == null || r.statusCode() != 200; r = page(base)) {
			if (Instant.now().isAfter(deadline))
				throw new IllegalStateException("console did not answer 200 at " + base + ": "
					+ (r == null ? "no response" : r.statusCode() + " " + r.headers().firstValue("X-Request-Boundary").orElse("") + " " + r.body()));
			Thread.sleep(250);
		}
	}

	@AfterAll
	static void stop() {
		System.clearProperty(PetstoreConsoleBoundary.AUTHORITY_PROPERTY);
	}

	private static HttpResponse<String> page(String root) throws Exception {
		try {
			return HTTP.send(HttpRequest.newBuilder(URI.create(root + "/console/store")).timeout(Duration.ofSeconds(20)).header("Accept", "text/html").GET().build(), BodyHandlers.ofString());
		} catch (IOException e) {  // still starting
			return null;
		}
	}

	private static long availablePet() throws Exception {
		var list = new JsonList(HTTP.send(HttpRequest.newBuilder(URI.create(base + "/petstore/pets")).header("Accept", "application/json").GET().build(), BodyHandlers.ofString()).body());
		for (var o : list)
			if ("AVAILABLE".equals(((JsonMap)o).getString("status")))
				return ((JsonMap)o).getLong("id");
		throw new IllegalStateException("No seeded AVAILABLE pet");
	}

	@Test void a01_theConfiguredAuthorityIsServed() throws Exception {
		var r = page(base);
		assertEquals(200, r.statusCode());
		assertTrue(TOKEN.matcher(r.body()).find(), "no data-juneau-csrf on <body>");
	}

	@Test void a02_theDefaultAuthorityIsNowRefused() throws Exception {
		var r = page("http://localhost:" + server.getPort());
		assertEquals(421, r.statusCode(), r.body());
		assertString("HOST_MISMATCH", r.headers().firstValue("X-Request-Boundary").orElse(null));
	}

	@Test void a03_writesExpectTheConfiguredOrigin() throws Exception {
		var m = TOKEN.matcher(page(base).body());
		assertTrue(m.find());
		var id = availablePet();
		var w = HttpRequest.newBuilder(URI.create(base + "/console/pets/" + id + "/sell")).timeout(Duration.ofSeconds(20))
			.header("Content-Type", "application/json").header("Accept", "application/json").header("X-Csrf-Token", m.group(1));
		var wrong = HTTP.send(w.copy().header("Origin", "http://localhost:" + server.getPort()).POST(BodyPublishers.ofString("{}")).build(), BodyHandlers.ofString());
		assertEquals(403, wrong.statusCode(), wrong.body());
		assertString("ORIGIN_MISMATCH", wrong.headers().firstValue("X-Request-Boundary").orElse(null));
		var right = HTTP.send(w.copy().header("Origin", base).POST(BodyPublishers.ofString("{}")).build(), BodyHandlers.ofString());
		assertEquals(200, right.statusCode(), right.body());
	}
}
