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

import org.apache.juneau.rest.client.*;
import org.eclipse.jetty.client.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link CredentialGuardingHttpClient}, including the defensive fallbacks that fail closed (strip
 * credentials) when the origins of a redirect cannot be compared.
 */
@SuppressWarnings({
	"resource" // HttpClient fixtures are never started; nothing to close.
})
class CredentialGuardingHttpClient_Test {

	private static final String KEY = "X-API-Key";

	private final CredentialGuardingHttpClient client = new CredentialGuardingHttpClient();

	private Request oldRequest() {
		return client.newRequest("http://a.example.com:8080/start").headers(f -> f.add(KEY, "k-1").add("X-Other", "o-1"));
	}

	@Test
	void a01_sameOrigin_keepsCredentialHeaders() {
		var copy = client.copyRequest(oldRequest(), URI.create("http://a.example.com:8080/next"));
		assertEquals("k-1", copy.getHeaders().get(KEY));
		assertEquals("o-1", copy.getHeaders().get("X-Other"));
	}

	@Test
	void a02_differentOrigin_stripsCredentialHeadersOnly() {
		var copy = client.copyRequest(oldRequest(), URI.create("http://b.example.com:8080/next"));
		assertNull(copy.getHeaders().get(KEY));
		assertEquals("o-1", copy.getHeaders().get("X-Other"));
	}

	@Test
	void a03_unparseableOriginalScheme_failsClosedAndStrips() {
		// A scheme containing a space cannot be turned back into a URI, so the origin comparison is impossible.
		var old = oldRequest().scheme("ht tp");
		var copy = client.copyRequest(old, URI.create("http://a.example.com:8080/next"));
		assertNull(copy.getHeaders().get(KEY));
		assertEquals("o-1", copy.getHeaders().get("X-Other"));
	}

	@Test
	void a04_underscoreHostRedirectTarget_rejectedByJettyBeforeTheOriginCheck() {
		// java.net.URI parses an underscore host name with a null host, and Jetty refuses to build a request for a null
		// host, so a redirect to such a host fails in Jetty itself and never reaches this client's origin comparison.
		var ex = assertThrows(IllegalArgumentException.class, () -> client.copyRequest(oldRequest(), URI.create("http://my_service:8080/next")));
		assertTrue(ex.getMessage().contains("Invalid URI host"), ex.getMessage());
	}
}
