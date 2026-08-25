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
package org.apache.juneau.rest.server.auth.saml;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Tests that {@link SamlMetadataResolvers#url(String)} bounds the remote metadata fetch to a byte cap, both
 * when the server declares an over-cap {@code Content-Length} up front and when it streams past the cap with
 * no declared length at all.
 *
 * @since 10.0.0
 */
class SamlMetadataResolvers_BoundedFetch_Test extends TestBase {

	private static final String MINIMAL_METADATA =
		"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
		+ "<EntityDescriptor xmlns=\"urn:oasis:names:tc:SAML:2.0:metadata\" entityID=\"https://idp.example.com/idp\">"
		+ "<IDPSSODescriptor protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">"
		+ "<SingleSignOnService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect\" Location=\"https://idp.example.com/sso\"/>"
		+ "</IDPSSODescriptor></EntityDescriptor>";

	@FunctionalInterface
	private interface UrlAction {
		void run(String url) throws Exception;
	}

	private static void withServer(HttpHandler handler, UrlAction action) throws Exception {
		var server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		server.createContext("/metadata", handler);
		server.start();
		try {
			var url = "http://127.0.0.1:" + server.getAddress().getPort() + "/metadata";
			action.run(url);
		} finally {
			server.stop(0);
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// A: declared Content-Length over the cap — rejected before streaming the body.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void a01_declaredContentLengthOverDefaultCap_rejected() {
		// 2 MiB of filler with a matching (accurate) Content-Length header, against the 1 MiB default cap.
		var body = new byte[2 * 1024 * 1024];
		Arrays.fill(body, (byte) 'A');
		var ex = assertThrows(IOException.class, () -> withServer(exchange -> {
			exchange.sendResponseHeaders(200, body.length);
			try (var os = exchange.getResponseBody()) {
				os.write(body);
			}
		}, SamlMetadataResolvers::url));
		// Rejection happens up-front on the declared Content-Length, not after a failed XML parse of the filler.
		assertTrue(ex.getMessage().contains("cap"), "Expected a cap-rejection message, got: " + ex.getMessage());
	}

	@Test void a02_declaredContentLengthOverCustomCap_rejected() {
		var body = new byte[4000];
		Arrays.fill(body, (byte) 'A');
		var ex = assertThrows(IOException.class, () -> withServer(exchange -> {
			exchange.sendResponseHeaders(200, body.length);
			try (var os = exchange.getResponseBody()) {
				os.write(body);
			}
		}, url -> SamlMetadataResolvers.url(url, null, 1024)));
		assertTrue(ex.getMessage().contains("cap"), "Expected a cap-rejection message, got: " + ex.getMessage());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: no declared Content-Length (chunked) — streaming aborts once the actual byte count exceeds the cap.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void b01_chunkedStreamOverDefaultCap_rejected() {
		var chunk = new byte[64 * 1024];
		Arrays.fill(chunk, (byte) 'A');
		var ex = assertThrows(IOException.class, () -> withServer(exchange -> {
			exchange.sendResponseHeaders(200, 0); // 0 → chunked transfer, no Content-Length header
			try (var os = exchange.getResponseBody()) {
				for (var i = 0; i < 32; i++) // 32 * 64 KiB = 2 MiB, well past the 1 MiB default cap
					os.write(chunk);
			}
		}, SamlMetadataResolvers::url));
		assertTrue(ex.getMessage().contains("cap"), "Expected a cap-rejection message, got: " + ex.getMessage());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: typical small metadata document — still resolves under the cap (regression guard).
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c01_smallValidMetadata_stillResolves() {
		var bytes = MINIMAL_METADATA.getBytes(StandardCharsets.UTF_8);
		assertDoesNotThrow(() -> withServer(exchange -> {
			exchange.sendResponseHeaders(200, bytes.length);
			try (var os = exchange.getResponseBody()) {
				os.write(bytes);
			}
		}, url -> assertNotNull(SamlMetadataResolvers.url(url))));
	}
}
