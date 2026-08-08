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
package org.apache.juneau.rest.client.mcp.auth;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.time.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link LoopbackRedirectReceiver}.
 *
 * @since 10.0.0
 */
class LoopbackRedirectReceiver_Test extends TestBase {

	@Test void a01_redirectUriIsLoopback() throws IOException {
		try (var r = LoopbackRedirectReceiver.open()) {
			var uri = r.redirectUri();
			assertEquals("127.0.0.1", uri.getHost());
			assertEquals("/callback", uri.getPath());
			assertEquals(r.port(), uri.getPort());
		}
	}

	@Test void a02_awaitCallbackReturnsFullUri() throws Exception {
		try (var r = LoopbackRedirectReceiver.open()) {
			var target = URI.create(r.redirectUri() + "?code=abc&state=s1");
			var t = new Thread(() -> get(target));
			t.start();
			var got = r.awaitCallback(Duration.ofSeconds(5));
			t.join(2000);
			assertTrue(got.toString().contains("code=abc"), got::toString);
			assertTrue(got.toString().contains("state=s1"), got::toString);
		}
	}

	@Test void b01_timeoutThrows() throws IOException {
		try (var r = LoopbackRedirectReceiver.open()) {
			var timeout = Duration.ofMillis(50);
			assertThrows(McpAuthException.class, () -> r.awaitCallback(timeout));
		}
	}

	// H2: the fixed-port (bind-first) constructor binds exactly the requested port so it matches a forPort registration.
	@Test void c01_fixedPortBindsRequestedPort() throws IOException {
		var port = freePort();
		try (var r = new LoopbackRedirectReceiver("/callback", port, "<html></html>")) {
			assertEquals(port, r.port());
			assertEquals(port, r.redirectUri().getPort());
			assertEquals("127.0.0.1", r.redirectUri().getHost());
		}
	}

	// H2: port 0 keeps the ephemeral (port-agnostic) default behavior on the fixed-port constructor.
	@Test void c02_fixedPortZeroIsEphemeral() throws IOException {
		try (var r = new LoopbackRedirectReceiver("/callback", 0, "<html></html>")) {
			assertTrue(r.port() > 0);
		}
	}

	private static int freePort() throws IOException {
		try (var s = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
			return s.getLocalPort();
		}
	}

	private static void get(URI uri) {
		try {
			var c = (HttpURLConnection) uri.toURL().openConnection();
			c.setRequestMethod("GET");
			c.getResponseCode();
			c.getInputStream().readAllBytes();
			c.disconnect();
		} catch (IOException e) { // HTT: loopback GET failure not expected in test
			throw new RuntimeException(e);
		}
	}
}
