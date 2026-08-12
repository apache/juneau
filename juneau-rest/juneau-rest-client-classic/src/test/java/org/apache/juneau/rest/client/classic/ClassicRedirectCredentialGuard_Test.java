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

import org.apache.http.*;
import org.apache.http.message.*;
import org.apache.http.protocol.*;
import org.junit.jupiter.api.*;

/**
 * Direct unit tests for {@link ClassicRedirectCredentialGuard}'s edge cases that end-to-end redirect tests
 * ({@link RestClient_RedirectCredentials_Test}) never reach &mdash; a real HttpClient exchange always
 * supplies a resolvable {@link HttpHost} target, so a missing/unparsable target host is only reachable
 * by driving {@link ClassicRedirectCredentialGuard#process(HttpRequest, HttpContext)} directly against a
 * hand-built {@link HttpContext}. In the same package as the guard since it (and its members) are
 * package-private.
 */
class ClassicRedirectCredentialGuard_Test {

	private final ClassicRedirectCredentialGuard guard = new ClassicRedirectCredentialGuard();

	@Test
	void a01_noTargetHostInContext_isNoOp() {
		var context = new BasicHttpContext();
		var request = new BasicHttpRequest("GET", "/");
		request.addHeader("Authorization", "Bearer secret");
		assertDoesNotThrow(() -> guard.process(request, context));
		assertNotNull(request.getFirstHeader("Authorization"), "with no target host to compare against, credentials must be left untouched");
	}

	@Test
	void a02_targetHostToURIThrows_originUnresolvable_isNoOp() {
		// "|" is not a legal URI character but passes HttpHost's blanks-only hostname validation, so
		// HttpHost.toURI() produces a string that URI.create(...) rejects with IllegalArgumentException —
		// exactly the case originOf(...) guards against.
		var context = new BasicHttpContext();
		HttpCoreContext.adapt(context).setTargetHost(new HttpHost("exa|mple.com", 80, "http"));
		var request = new BasicHttpRequest("GET", "/");
		request.addHeader("Authorization", "Bearer secret");
		assertDoesNotThrow(() -> guard.process(request, context));
		assertNotNull(request.getFirstHeader("Authorization"), "an unresolvable origin must not strip credentials");
	}

	@Test
	void a03_firstRequestInExchange_recordsOriginWithoutStrippingCredentials() {
		var context = new BasicHttpContext();
		HttpCoreContext.adapt(context).setTargetHost(new HttpHost("example.com", 80, "http"));
		var request = new BasicHttpRequest("GET", "/");
		request.addHeader("Authorization", "Bearer secret");
		guard.process(request, context);
		assertNotNull(request.getFirstHeader("Authorization"), "the first request of an exchange only records the origin; nothing to compare against yet");
	}
}
