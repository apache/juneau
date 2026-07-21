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
package org.apache.juneau.http.response;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.header.*;
import org.junit.jupiter.api.*;

class BasicHttpResponse_FluentSetters_Test extends TestBase {

	private Ok newOk() {
		return new Ok();
	}

	@Test void a01_setStatusCode_preservesRest() {
		var r = newOk().setStatusCode(204);
		assertEquals(204, r.getStatusCode());
		assertEquals("OK", r.getStatusLine().getReasonPhrase());
		assertEquals(HttpProtocolVersion.HTTP_1_1, r.getStatusLine().getProtocolVersion());
	}

	@Test void a02_setReasonPhrase_preservesRest() {
		var r = newOk().setReasonPhrase("Yo");
		assertEquals(200, r.getStatusCode());
		assertEquals("Yo", r.getStatusLine().getReasonPhrase());
	}

	@Test void a03_setProtocolVersion_preservesRest() {
		var r = newOk().setProtocolVersion(HttpProtocolVersion.HTTP_2_0);
		assertEquals(HttpProtocolVersion.HTTP_2_0, r.getStatusLine().getProtocolVersion());
	}

	@Test void a04_setStatusLine_replacesAll() {
		var r = newOk().setStatusLine(HttpStatusLineBean.of(HttpProtocolVersion.HTTP_2_0, 201, "Created"));
		assertEquals(201, r.getStatusCode());
	}

	@Test void a05_addHeader_appends() {
		var r = newOk().addHeader("X-Trace", "1").addHeader("X-Trace", "2");
		assertEquals(2, r.getHeaders().size());
	}

	@Test void a06_setHeader_replacesByName() {
		var r = newOk().addHeader("X-Trace", "1").setHeader("X-Trace", "2");
		assertEquals(1, r.getHeaders().size());
		assertEquals("2", r.getHeaders().get(0).getValue());
	}

	@Test void a07_setHeaders_listReplacesAll() {
		var r = newOk().addHeader("A", "1")
			.setHeaders(List.of(HttpHeaderBean.of("B", "2"), HttpHeaderBean.of("C", "3")));
		assertEquals(2, r.getHeaders().size());
		assertEquals("B", r.getHeaders().get(0).getName());
	}

	@Test void a08_setHeaders_varargsReplacesAll() {
		var r = newOk().addHeader("A", "1")
			.setHeaders(HttpHeaderBean.of("B", "2"));
		assertEquals(1, r.getHeaders().size());
		assertEquals("B", r.getHeaders().get(0).getName());
	}

	@Test void a09_setBody_mutates() {
		var r = newOk().setBody(StringBody.of("hi"));
		assertEquals("hi", r.getBody().toString());
	}

	@Test void a10_setContent_string() {
		var r = newOk().setContent("hi");
		assertEquals("hi", r.getBody().toString());
	}

	@Test void a11_setContent_null_clearsBody() {
		var r = newOk().setBody(StringBody.of("hi")).setContent((String)null);
		assertNull(r.getBody());
	}

	@Test void a12_setLocale_mutates() {
		var r = newOk().setLocale(Locale.GERMAN);
		assertEquals(Locale.GERMAN, r.getLocale());
	}

	@Test void a13_unmodifiable_locksMutations() {
		var r = newOk().addHeader("X-Trace", "1").unmodifiable();
		assertTrue(r.isUnmodifiable());
		assertThrows(UnsupportedOperationException.class, () -> r.addHeader("X-More", "v"));
		assertThrows(UnsupportedOperationException.class, () -> r.setHeader("X-Trace", "v"));
		assertThrows(UnsupportedOperationException.class, () -> r.setStatusCode(404));
		assertThrows(UnsupportedOperationException.class, () -> r.setContent("x"));
		assertThrows(UnsupportedOperationException.class, () -> r.setLocale(Locale.ENGLISH));
	}

	@Test void a14_unmodifiable_doesNotBlockReread() {
		var r = newOk().addHeader("X-Trace", "1").unmodifiable();
		assertEquals(1, r.getHeaders().size());
		assertEquals(200, r.getStatusCode());
	}

	@Test void a15_getHeaders_isUnmodifiableView() {
		var r = newOk().addHeader("X-Trace", "1");
		var view = r.getHeaders();
		assertThrows(UnsupportedOperationException.class, view::clear);
	}
}
