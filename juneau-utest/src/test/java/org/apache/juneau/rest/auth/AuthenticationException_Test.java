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
package org.apache.juneau.rest.auth;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link AuthenticationException} &mdash; status code 401, fluent
 * {@link AuthenticationException#wwwAuthenticate(String)} setter, and inheritance from
 * {@link org.apache.juneau.http.response.Unauthorized}.
 *
 * @since 9.5.0
 */
class AuthenticationException_Test extends TestBase {

	@Test void a01_statusCodeIs401() {
		var ex = new AuthenticationException();
		Assertions.assertEquals(401, ex.getStatusCode());
		Assertions.assertEquals("Unauthorized", ex.getStatusLine().getReasonPhrase());
	}

	@Test void a02_constructorWithMessage() {
		var ex = new AuthenticationException("Token {0}", "missing");
		Assertions.assertEquals("Token missing", ex.getMessage());
	}

	@Test void a03_constructorWithCause() {
		var cause = new IllegalStateException("nope");
		var ex = new AuthenticationException(cause);
		Assertions.assertSame(cause, ex.getCause());
	}

	@Test void a04_constructorWithCauseAndMessage() {
		var cause = new IllegalStateException("inner");
		var ex = new AuthenticationException(cause, "wrapped {0}", "value");
		Assertions.assertSame(cause, ex.getCause());
		Assertions.assertEquals("wrapped value", ex.getMessage());
	}

	@Test void b01_wwwAuthenticateFluentSetterChains() {
		var ex = new AuthenticationException("denied")
			.wwwAuthenticate("Bearer realm=\"api\"");
		var header = ex.getHeaders().stream()
			.filter(h -> "WWW-Authenticate".equalsIgnoreCase(h.getName()))
			.findFirst()
			.orElseThrow();
		Assertions.assertEquals("Bearer realm=\"api\"", header.getValue());
	}

	@Test void b02_wwwAuthenticateReplacesPreviousHeader() {
		var ex = new AuthenticationException()
			.wwwAuthenticate("Bearer realm=\"one\"")
			.wwwAuthenticate("Bearer realm=\"two\"");
		var matches = ex.getHeaders().stream()
			.filter(h -> "WWW-Authenticate".equalsIgnoreCase(h.getName()))
			.toList();
		Assertions.assertEquals(1, matches.size(), "expected one WWW-Authenticate header");
		Assertions.assertEquals("Bearer realm=\"two\"", matches.get(0).getValue());
	}
}
