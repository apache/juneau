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
package org.apache.juneau.ng.http.response;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class HttpStatusLineBean_Test extends TestBase {

	@Test void a01_of_defaultsToHttp11() {
		var s = HttpStatusLineBean.of(200, "OK");
		assertEquals(200, s.getStatusCode());
		assertEquals("OK", s.getReasonPhrase());
		assertEquals("HTTP/1.1", s.getProtocolVersion());
		assertEquals("HTTP/1.1 200 OK", s.toString());
	}

	@Test void a02_of_customProtocolVersion() {
		var s = HttpStatusLineBean.of("HTTP/2", 418, "I'm a teapot");
		assertEquals(418, s.getStatusCode());
		assertEquals("I'm a teapot", s.getReasonPhrase());
		assertEquals("HTTP/2", s.getProtocolVersion());
		assertEquals("HTTP/2 418 I'm a teapot", s.toString());
	}

	@Test void a03_toString_omitsNullReasonPhrase() {
		var s = HttpStatusLineBean.of(204, null);
		assertNull(s.getReasonPhrase());
		assertEquals("HTTP/1.1 204", s.toString());
	}

	@Test void a04_nullProtocolVersionRejected() {
		assertThrows(IllegalArgumentException.class, () -> HttpStatusLineBean.of(null, 200, "OK"));
	}

	@Test void a05_equalsAndHashCode() {
		var a = HttpStatusLineBean.of(200, "OK");
		var b = HttpStatusLineBean.of(200, "OK");
		var c = HttpStatusLineBean.of(404, "OK");
		var d = HttpStatusLineBean.of(200, "Found");
		var e = HttpStatusLineBean.of("HTTP/2", 200, "OK");

		assertEquals(a, a);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, c);
		assertNotEquals(a, d);
		assertNotEquals(a, e);
		assertNotEquals(a, "not a status line");
		assertNotEquals(a, null);
	}

	@Test void a06_nullReasonPhrase_equalsAndHash() {
		var a = HttpStatusLineBean.of(204, null);
		var b = HttpStatusLineBean.of(204, null);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, HttpStatusLineBean.of(204, "No Content"));
	}
}
