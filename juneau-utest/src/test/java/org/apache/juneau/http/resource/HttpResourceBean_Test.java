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
package org.apache.juneau.http.resource;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.header.*;
import org.junit.jupiter.api.*;

class HttpResourceBean_Test extends TestBase {

	@Test void a01_of_body_emptyHeaders() {
		var r = HttpResourceBean.of(StringBody.of("body"));
		assertEquals("body", r.toString());
		assertTrue(r.getHeaders().isEmpty());
	}

	@Test void a02_implementsHttpResourceInterface() {
		HttpResource r = HttpResourceBean.of(StringBody.of("x"));
		assertNotNull(r);
		assertTrue(r.getHeaders().isEmpty());
	}

	@Test void a03_withHeader_typed_addsHeader() {
		var r = HttpResourceBean.of(StringBody.of("body"))
			.withHeader(HttpHeaderBean.of("Cache-Control", "no-cache"));
		assertEquals(1, r.getHeaders().size());
		assertEquals("Cache-Control", r.getHeaders().get(0).getName());
		assertEquals("no-cache", r.getHeaders().get(0).getValue());
	}

	@Test void a04_withHeader_nameValue_addsHeader() {
		var r = HttpResourceBean.of(StringBody.of("body"))
			.withHeader("Cache-Control", "no-cache");
		assertEquals("no-cache", r.getFirstHeader("Cache-Control").getValue());
	}

	@Test void a05_withHeader_returnsNewInstance() {
		var r1 = HttpResourceBean.of(StringBody.of("body"));
		var r2 = r1.withHeader("X-Trace", "1");
		assertNotSame(r1, r2);
		assertTrue(r1.getHeaders().isEmpty());
		assertEquals(1, r2.getHeaders().size());
	}

	@Test void a06_withHeaders_array() {
		var r = HttpResourceBean.of(StringBody.of("x"))
			.withHeaders(HttpHeaderBean.of("A", "1"), HttpHeaderBean.of("B", "2"));
		assertEquals(2, r.getHeaders().size());
	}

	@Test void a07_withHeaders_array_ignoresNulls() {
		var r = HttpResourceBean.of(StringBody.of("x"))
			.withHeaders((HttpHeader)null, HttpHeaderBean.of("A", "1"), (HttpHeader)null);
		assertEquals(1, r.getHeaders().size());
		assertEquals("A", r.getHeaders().get(0).getName());
	}

	@Test void a08_withHeaders_list() {
		var r = HttpResourceBean.of(StringBody.of("x"))
			.withHeaders(List.of(HttpHeaderBean.of("A", "1"), HttpHeaderBean.of("B", "2")));
		assertEquals(2, r.getHeaders().size());
	}

	@Test void a09_withHeaders_emptyArrayIsNoop() {
		var r1 = HttpResourceBean.of(StringBody.of("x"));
		var r2 = r1.withHeaders();
		assertSame(r1, r2);
	}

	@Test void a10_contentType_fromHeader() {
		var r = HttpResourceBean.of(StringBody.of("x"))
			.withHeader("Content-Type", "application/pdf");
		assertEquals("application/pdf", r.getContentType());
	}

	@Test void a11_contentType_fallsBackToBody() {
		var r = HttpResourceBean.of(StringBody.of("x"));
		assertEquals(StringBody.of("x").getContentType(), r.getContentType());
	}

	@Test void a12_nullBody_rejected() {
		assertThrows(IllegalArgumentException.class, () -> HttpResourceBean.of(null));
	}

	@Test void a13_nullHeader_rejected() {
		var r = HttpResourceBean.of(StringBody.of("x"));
		assertThrows(IllegalArgumentException.class, () -> r.withHeader(null));
	}
}
