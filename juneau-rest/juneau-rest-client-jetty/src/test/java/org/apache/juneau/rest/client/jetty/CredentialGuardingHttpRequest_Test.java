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
package org.apache.juneau.rest.client.jetty;

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;

import org.eclipse.jetty.client.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link CredentialGuardingHttpRequest}.
 */
@SuppressWarnings({
	"resource" // HttpClient fixtures are never started; nothing to close.
})
class CredentialGuardingHttpRequest_Test {

	private static final String KEY = "X-API-Key";

	private final HttpClient client = new HttpClient();

	private CredentialGuardingHttpRequest request() {
		var r = new CredentialGuardingHttpRequest(client, URI.create("http://a.example.com:8080/start"));
		r.headers(f -> f.add(KEY, "k-1").add("X-Other", "o-1"));
		return r;
	}

	@Test
	void a01_sameOriginCopy_keepsCredentialHeaders() {
		var copy = request().copy(URI.create("http://a.example.com:8080/next"));
		assertEquals("k-1", copy.getHeaders().get(KEY));
		assertEquals("o-1", copy.getHeaders().get("X-Other"));
	}

	@Test
	void a02_crossOriginCopy_stripsCredentialHeadersOnly() {
		var copy = request().copy(URI.create("http://b.example.com:8080/next"));
		assertNull(copy.getHeaders().get(KEY));
		assertEquals("o-1", copy.getHeaders().get("X-Other"));
	}

	@Test
	void a03_copyWithoutUri_isSameOriginAndKeepsCredentialHeaders() {
		var copy = request().copy(null);
		assertEquals("http://a.example.com:8080", copy.getURI().toString());
		assertEquals("k-1", copy.getHeaders().get(KEY));
	}

	@Test
	void a04_copyIsGuardedToo() {
		var copy = request().copy(URI.create("http://a.example.com:8080/next"));
		assertInstanceOf(CredentialGuardingHttpRequest.class, copy);
		assertNull(copy.copy(URI.create("https://a.example.com:8080/next")).getHeaders().get(KEY));
	}

	@Test
	void a05_defaultClientCopyRequest_delegatesToTheGuardedCopy() {
		// HttpClient.copyRequest is protected; Jetty's redirector calls it, and its default delegates to HttpRequest.copy.
		var copy = new HttpClient() {
			Request copyOf(Request r, URI uri) {
				return copyRequest(r, uri);
			}
		}.copyOf(request(), URI.create("http://b.example.com:8080/next"));
		assertNull(copy.getHeaders().get(KEY));
	}
}
