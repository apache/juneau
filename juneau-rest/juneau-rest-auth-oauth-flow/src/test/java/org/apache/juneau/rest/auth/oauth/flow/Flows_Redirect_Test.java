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
package org.apache.juneau.rest.auth.oauth.flow;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.auth.oauth.oidc.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Tests that token and discovery requests do not follow HTTP redirects.
 *
 * @since 10.0.0
 */
class Flows_Redirect_Test extends TestBase {

	private HttpServer server;
	private final AtomicInteger targetHits = new AtomicInteger();

	@BeforeEach
	void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		server.createContext("/target", ex -> {
			targetHits.incrementAndGet();
			var body = "{\"access_token\":\"at\",\"token_type\":\"Bearer\",\"expires_in\":3600}".getBytes();
			ex.getResponseHeaders().add("Content-Type", "application/json");
			ex.sendResponseHeaders(200, body.length);
			try (var os = ex.getResponseBody()) {
				os.write(body);
			}
		});
		server.createContext("/redirect", ex -> {
			ex.getResponseHeaders().add("Location", base() + "/target");
			ex.sendResponseHeaders(302, -1);
			ex.close();
		});
		server.createContext("/.well-known/oauth-authorization-server", ex -> {
			ex.getResponseHeaders().add("Location", base() + "/target");
			ex.sendResponseHeaders(302, -1);
			ex.close();
		});
		server.createContext("/.well-known/openid-configuration", ex -> {
			ex.getResponseHeaders().add("Location", base() + "/target");
			ex.sendResponseHeaders(302, -1);
			ex.close();
		});
		server.start();
	}

	@AfterEach
	void stopServer() {
		server.stop(0);
	}

	private String base() {
		return "http://127.0.0.1:" + server.getAddress().getPort();
	}

	@Test void a01_tokenRequestDoesNotFollowRedirect() {
		var flow = OAuthClientCredentialsFlow.create()
			.tokenEndpoint(URI.create(base() + "/redirect"))
			.clientId("id")
			.clientSecret("s")
			.build();
		assertThrows(OAuthFlowException.class, flow::acquire);
		assertEquals(0, targetHits.get());
	}

	@Test void a02_discoveryDoesNotFollowRedirect() {
		var client = OidcDiscoveryClient.create().issuer(URI.create(base())).build();
		assertThrows(Exception.class, client::discover);
		assertEquals(0, targetHits.get());
	}
}
