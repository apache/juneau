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
package org.apache.juneau.rest.client.mcp;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.*;

import org.apache.juneau.rest.client.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Unit tests for {@link McpAuthInterceptor}.
 */
@SuppressWarnings("resource") // mock transports are in-memory no-op closeables; test bodies close the RestClient/RestResponse that matters via try-with-resources.
class McpAuthInterceptor_Test {

	@Test
	void a01_onInit_setsAuthorizationHeader_fromTokenSupplier() throws Exception {
		var seen = new AtomicReference<String>();
		HttpTransport transport = tReq -> {
			var h = tReq.getFirstHeader("Authorization");
			seen.set(h == null ? null : h.value());
			return TransportResponse.builder().statusCode(204).build();
		};
		var interceptor = new McpAuthInterceptor(() -> "abc123");
		try (var client = RestClient.builder().transport(transport).interceptors(interceptor).build()) {
			try (var res = client.get("http://x/ping").run()) {
				assertEquals(204, res.getStatusCode());
			}
		}
		assertEquals("Bearer abc123", seen.get());
	}

	@Test
	void a02_ofStaticBearer_alwaysSendsSameToken() throws Exception {
		var seen = new AtomicReference<String>();
		HttpTransport transport = tReq -> {
			var h = tReq.getFirstHeader("Authorization");
			seen.set(h == null ? null : h.value());
			return TransportResponse.builder().statusCode(204).build();
		};
		var interceptor = McpAuthInterceptor.ofStaticBearer("static-token");
		try (var client = RestClient.builder().transport(transport).interceptors(interceptor).build()) {
			try (var res = client.get("http://x/ping").run()) {
				assertEquals(204, res.getStatusCode());
			}
			try (var res = client.get("http://x/ping").run()) {
				assertEquals(204, res.getStatusCode());
			}
		}
		assertEquals("Bearer static-token", seen.get());
	}

	/** A blank (null, empty, or whitespace-only) token is treated like a null token, not sent as a credential-less header. */
	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {"", "   "})
	void a03_onInit_blankToken_omitsAuthorizationHeader(String token) throws Exception {
		var sawHeader = new AtomicBoolean(true);
		HttpTransport transport = tReq -> {
			sawHeader.set(tReq.getFirstHeader("Authorization") != null);
			return TransportResponse.builder().statusCode(204).build();
		};
		var interceptor = new McpAuthInterceptor(() -> token);
		try (var client = RestClient.builder().transport(transport).interceptors(interceptor).build()) {
			try (var res = client.get("http://x/ping").run()) {
				assertEquals(204, res.getStatusCode());
			}
		}
		assertFalse(sawHeader.get());
	}

	@Test
	void b01_constructor_nullTokenSupplier_throwsIllegalArgumentException() {
		assertThrows(IllegalArgumentException.class, () -> new McpAuthInterceptor(null));
	}

	@Test
	void b02_ofStaticBearer_nullToken_throwsIllegalArgumentException() {
		assertThrows(IllegalArgumentException.class, () -> McpAuthInterceptor.ofStaticBearer(null));
	}
}
