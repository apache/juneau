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
package org.apache.juneau.http.request;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.junit.jupiter.api.*;

class HttpRequestLineBean_Test extends TestBase {

	@Test void a01_of_defaultsToHttp11() {
		var r = HttpRequestLineBean.of("GET", "/users/123");
		assertEquals("GET", r.getMethod());
		assertEquals("/users/123", r.getUri());
		assertEquals(HttpProtocolVersion.HTTP_1_1, r.getProtocolVersion());
		assertEquals("GET /users/123 HTTP/1.1", r.toString());
	}

	@Test void a02_of_typedProtocolVersion() {
		var r = HttpRequestLineBean.of("POST", "/api/echo", HttpProtocolVersion.HTTP_2_0);
		assertEquals(HttpProtocolVersion.HTTP_2_0, r.getProtocolVersion());
		assertEquals("POST /api/echo HTTP/2.0", r.toString());
	}

	@Test void a03_of_parsedProtocolVersion() {
		var r = HttpRequestLineBean.of("GET", "/x", "HTTP/2");
		assertEquals(HttpProtocolVersion.of("HTTP", 2, 0), r.getProtocolVersion());
	}

	@Test void a04_nullMethodRejected() {
		assertThrows(IllegalArgumentException.class, () -> HttpRequestLineBean.of(null, "/x"));
	}

	@Test void a05_nullUriRejected() {
		assertThrows(IllegalArgumentException.class, () -> HttpRequestLineBean.of("GET", null));
	}

	@Test void a06_nullProtocolVersionRejected() {
		assertThrows(IllegalArgumentException.class, () -> HttpRequestLineBean.of("GET", "/x", (HttpProtocolVersion)null));
		assertThrows(IllegalArgumentException.class, () -> HttpRequestLineBean.of("GET", "/x", (String)null));
	}

	@Test void a07_equalsAndHashCode() {
		var a = HttpRequestLineBean.of("GET", "/x");
		var b = HttpRequestLineBean.of("GET", "/x");
		var c = HttpRequestLineBean.of("POST", "/x");
		var d = HttpRequestLineBean.of("GET", "/y");
		var e = HttpRequestLineBean.of("GET", "/x", HttpProtocolVersion.HTTP_2_0);

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, c);
		assertNotEquals(a, d);
		assertNotEquals(a, e);
		assertNotEquals(a, "not a request line");
		assertNotEquals(a, null);
	}
}
