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
import org.apache.juneau.http.header.*;
import org.junit.jupiter.api.*;

class BasicHttpException_FluentSetters_Test extends TestBase {

	@Test void a01_setStatusCode_preservesRest() {
		var e = new BasicHttpException(500, "Internal Server Error").setStatusCode(503);
		assertEquals(503, e.getStatusCode());
		assertEquals("Internal Server Error", e.getStatusLine().getReasonPhrase());
	}

	@Test void a02_setHeaders_andHeader() {
		var e = new BasicHttpException(500, "x")
			.addHeader("A", "1")
			.setHeader("A", "2");
		assertEquals(1, e.getHeaders().size());
		assertEquals("2", e.getHeaders().get(0).getValue());
	}

	@Test void a03_setHeaders_list() {
		var e = new BasicHttpException(500, "x")
			.setHeaders(List.of(HttpHeaderBean.of("A", "1"), HttpHeaderBean.of("B", "2")));
		assertEquals(2, e.getHeaders().size());
	}

	@Test void a04_setProtocolVersion() {
		var e = new BasicHttpException(500, "x").setProtocolVersion(HttpProtocolVersion.HTTP_2_0);
		assertEquals(HttpProtocolVersion.HTTP_2_0, e.getStatusLine().getProtocolVersion());
	}

	@Test void a05_setLocale() {
		var e = new BasicHttpException(500, "x").setLocale(Locale.GERMAN);
		assertEquals(Locale.GERMAN, e.getLocale());
	}

	@Test void a06_setContent_string() {
		var e = new BasicHttpException(500, "x").setContent("body");
		assertEquals("body", e.getBody().toString());
	}

	@Test void a07_setUnmodifiable() {
		var e = new BasicHttpException(500, "x").setUnmodifiable();
		assertTrue(e.isUnmodifiable());
		assertThrows(IllegalStateException.class, () -> e.setStatusCode(503));
		assertThrows(IllegalStateException.class, () -> e.addHeader("A", "1"));
	}

	@Test void a08_throwsAsRuntimeException() {
		var e = new BasicHttpException(500, "x", "msg");
		assertEquals("msg", e.getMessage());
		assertThrows(BasicHttpException.class, () -> { throw e; });
	}

	@Test void a09_subclass_inheritsSetters() {
		// Verifies that subclasses (Ok, InternalServerError, etc.) inherit the fluent setters.
		var e = new org.apache.juneau.http.response.InternalServerError("ouch")
			.setHeader("X-Trace", "abc");
		assertEquals(500, e.getStatusCode());
		assertEquals("abc", e.getHeaders().get(0).getValue());
	}
}
