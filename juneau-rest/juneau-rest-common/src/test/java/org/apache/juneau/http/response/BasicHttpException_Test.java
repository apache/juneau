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

import static org.apache.juneau.BasicTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.header.*;
import org.junit.jupiter.api.*;

/**
 * Read-accessor, root-cause, and equality coverage for {@link BasicHttpException}.
 *
 * <p>
 * Complements {@code BasicHttpException_FluentSetters_Test} (which covers the mutator surface); this class targets
 * the constructor overloads, {@code getRootCause()}, {@code getFullStackMessage(boolean)}, and {@code equals()}/
 * {@code hashCode()} — none of which had prior coverage.
 */
class BasicHttpException_Test extends TestBase {

	@Test void a01_constructor_causeOnly_nullCause() {
		var e = new BasicHttpException(500, "Internal Server Error", (Throwable)null);
		assertNull(e.getCause());
		assertEquals("Internal Server Error", e.getMessage());
	}

	@Test void a02_constructor_causeOnly_withCause() {
		var cause = new RuntimeException("boom");
		var e = new BasicHttpException(500, "x", cause);
		assertSame(cause, e.getCause());
		assertEquals("boom", e.getMessage());
	}

	@Test void a03_constructor_msgWithArgs() {
		var e = new BasicHttpException(500, "x", "value=%s", "abc");
		assertEquals("value=abc", e.getMessage());
	}

	@Test void a03b_constructor_msgWithExplicitNullArgs() {
		// Covers the args==null branch of the internal formatMessage() null-check, only reachable by explicitly
		// passing a null array (varargs otherwise never produces a null array through the public API).
		var e = new BasicHttpException(500, "x", "literal", (Object[])null);
		assertEquals("literal", e.getMessage());
	}

	@Test void a04_setStatusLine() {
		var e = new BasicHttpException(500, "x");
		var line = HttpStatusLineBean.of(200, "OK");
		e.setStatusLine(line);
		assertEquals(200, e.getStatusCode());
		assertEquals("OK", e.getStatusLine().getReasonPhrase());
	}

	@Test void a05_setStatusLine_nullRejected() {
		var e = new BasicHttpException(500, "x");
		assertThrowsWithMessage(IllegalArgumentException.class, "value", () -> e.setStatusLine(null));
	}

	@Test void a06_setHeaders_varargs() {
		var e = new BasicHttpException(500, "x")
			.addHeader("A", "old")
			.setHeaders(HttpHeaderBean.of("B", "1"), HttpHeaderBean.of("C", "2"));
		assertEquals(2, e.getHeaders().size());
	}

	@Test void a07_setHeaders_varargs_null() {
		var e = new BasicHttpException(500, "x").addHeader("A", "1").setHeaders((HttpHeader[])null);
		assertTrue(e.getHeaders().isEmpty());
	}

	@Test void a08_setHeaders_list_null() {
		var e = new BasicHttpException(500, "x").addHeader("A", "1").setHeaders((List<HttpHeader>)null);
		assertTrue(e.getHeaders().isEmpty());
	}

	@Test void a09_addHeader_headerInstance() {
		var e = new BasicHttpException(500, "x").addHeader(HttpHeaderBean.of("A", "1"));
		assertEquals("1", e.getHeaders().get(0).getValue());
	}

	@Test void a10_setContent_string_null() {
		var e = new BasicHttpException(500, "x").setContent((String)null);
		assertNull(e.getBody());
	}

	@Test void a11_setContent_httpBody() {
		var body = StringBody.of("hello");
		var e = new BasicHttpException(500, "x").setContent(body);
		assertSame(body, e.getBody());
	}

	@Test void a12_unmodifiable_idempotent() {
		var u = new BasicHttpException(500, "x").unmodifiable();
		assertSame(u, u.unmodifiable());
	}

	@Test void b01_getRootCause_skipsBasicHttpExceptionAndInvocationTargetException() {
		var root = new RuntimeException("root");
		var ite = new InvocationTargetException(root);
		var outer = new BasicHttpException(500, "x", ite);
		assertSame(root, outer.getRootCause());
	}

	@Test void b02_getRootCause_noCause_returnsNull() {
		var e = new BasicHttpException(500, "x");
		assertNull(e.getRootCause());
	}

	@Test void b03_getFullStackMessage_noScrub() {
		var cause = new RuntimeException("inner<>&");
		var e = new BasicHttpException(500, "x", cause, "outer<>&");
		var msg = e.getFullStackMessage(false);
		assertTrue(msg.contains("outer<>&"));
		assertTrue(msg.contains("inner<>&"));
	}

	@Test void b04_getFullStackMessage_scrub() {
		var cause = new RuntimeException("inner<>&");
		var e = new BasicHttpException(500, "x", cause, "outer<>&");
		var msg = e.getFullStackMessage(true);
		assertFalse(msg.contains("<"));
		assertFalse(msg.contains(">"));
		assertFalse(msg.contains("&"));
	}

	@Test void b05_getFullStackMessage_causeWithNullMessage() {
		var cause = new RuntimeException();
		var e = new BasicHttpException(500, "x", cause);
		var msg = e.getFullStackMessage(false);
		assertTrue(msg.contains("Caused by"));
	}

	@Test void b06_getFullStackMessage_nullMessage_skipsAppend() {
		// getMessage() returns null only when there's no explicit message, no cause, and no reason phrase.
		var e = new BasicHttpException(500, null);
		assertNull(e.getMessage());
		assertEquals("", e.getFullStackMessage(false));
	}

	@Test void c01_equals_differentStatusLine() {
		var a = new BasicHttpException(500, "x");
		var b = new BasicHttpException(404, "x");
		assertNotEquals(a, b);
	}

	@Test void c02_equals_differentHeaders() {
		var a = new BasicHttpException(500, "x").addHeader("A", "1");
		var b = new BasicHttpException(500, "x").addHeader("A", "2");
		assertNotEquals(a, b);
	}

	@Test void c03_equals_differentMessage() {
		var a = new BasicHttpException(500, "x", "m1");
		var b = new BasicHttpException(500, "x", "m2");
		assertNotEquals(a, b);
	}

	@Test void c04_equals_sameContent() {
		var a = new BasicHttpException(500, "x", "m1").addHeader("A", "1");
		var b = new BasicHttpException(500, "x", "m1").addHeader("A", "1");
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test void c05_equals_notAnException() {
		// Call a.equals(...) directly (not assertNotEquals(x, a), which would invoke x.equals(a) instead) so the
		// instanceof-false branch of BasicHttpException.equals() itself is actually exercised.
		var a = new BasicHttpException(500, "x");
		assertFalse(a.equals((Object)"not an exception"));
		assertFalse(a.equals(null));
	}
}
