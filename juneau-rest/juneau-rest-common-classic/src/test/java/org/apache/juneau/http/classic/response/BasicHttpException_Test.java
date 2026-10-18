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
package org.apache.juneau.http.classic.response;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.http.*;
import org.junit.jupiter.api.*;

/**
 * Immutability-paradigm tests for the {@link BasicHttpException} hierarchy, using {@link NotFound} as a representative
 * leaf.
 *
 * <p>
 * Mirrors the pilot ({@code BasicHttpResponse_Test} b01–b07): reflective mutator guard, idempotency, content
 * equality, snapshot independence, and snapshot-construction regression.  This is the {@link Throwable} branch — the
 * root is non-generic (JLS &sect;8.1.2), the funnel returns the base type, and each leaf retains its covariant setter
 * overrides.
 */
class BasicHttpException_Test extends TestBase {

	@Test void b01_unmodifiable_returnsUnmodifiableLeafType() {
		var x = new NotFound();
		assertFalse(x.isUnmodifiable());
		var u = x.unmodifiable();
		assertTrue(u.isUnmodifiable());
		assertInstanceOf(NotFound.Unmodifiable.class, u);
		assertInstanceOf(NotFound.class, u, "Snapshot must preserve the concrete leaf type.");
	}

	@Test void b02_reflectiveGuard_everyPublicMutatorThrows() throws Exception {
		var u = new NotFound().setContent("body").setHeader2("X-Test", "v").unmodifiable();
		var mutators = 0;
		for (var m : NotFound.class.getMethods()) {
			var n = m.getName();
			// Only funnel-routed mutators declared in our hierarchy — skip JDK-declared set/add/remove such as
			// Throwable.setStackTrace(...) and Throwable.addSuppressed(...), which are not routed through modify().
			if (Modifier.isStatic(m.getModifiers()) || m.getDeclaringClass().getName().startsWith("java.") || ! (n.startsWith("set") || n.startsWith("add") || n.startsWith("remove")))
				continue;
			mutators++;
			var types = m.getParameterTypes();
			var args = new Object[types.length];
			for (var i = 0; i < types.length; i++)
				args[i] = types[i] == int.class ? Integer.valueOf(0) : null;
			var ex = assertThrows(InvocationTargetException.class, () -> m.invoke(u, args), m::getName);
			assertInstanceOf(UnsupportedOperationException.class, ex.getCause(), m::getName);
		}
		assertTrue(mutators >= 10, "Expected at least 10 public mutators, found " + mutators);
	}

	@Test void b03_idempotency_alreadyUnmodifiableReturnsThis() {
		var u = new NotFound().unmodifiable();
		assertSame(u, u.unmodifiable());
	}

	@Test void b04_contentEquality_equalContentComparesEqual() {
		// Content equality — the slices satisfiable on their own.
		// Two independently-frozen snapshots of equal content compare equal.
		var u1 = new NotFound().setHeader2("X-Test", "v").unmodifiable();
		var u2 = new NotFound().setHeader2("X-Test", "v").unmodifiable();
		assertEquals(u1, u2);
		assertEquals(u1.hashCode(), u2.hashCode());
		// Two modifiable beans of equal content compare equal.
		var m1 = new NotFound().setHeader2("X-Test", "v");
		var m2 = new NotFound().setHeader2("X-Test", "v");
		assertEquals(m1, m2);
		assertEquals(m1.hashCode(), m2.hashCode());
		// Cross-boundary equality: now that HeaderList equality is content-only, a modifiable bean
		// compares equal to its own frozen snapshot (over statusLine + headers + getMessage(), excluding the
		// value-less content wrapper).
		assertEquals(m1, m1.unmodifiable());
		assertEquals(m1.hashCode(), m1.unmodifiable().hashCode());
	}

	@Test void b05_snapshotIndependence_mutatingOriginalDoesNotAffectSnapshot() {
		var x = new NotFound().setHeader2("X-Test", "before");
		var u = x.unmodifiable();
		x.setHeader2("X-Test", "after").setStatusCode2(201);
		assertEquals("before", u.getFirstHeader("X-Test").getValue());
		assertEquals(404, u.getStatusLine().getStatusCode());
	}

	@Test void b06_snapshotConstruction_freezeDoesNotThrow() {
		// Regression: constructing the snapshot must not route the freeze through the throwing modify().
		var x = new NotFound().setContent("body").setHeader2("X-Test", "v").setReasonPhrase2("Custom");
		assertDoesNotThrow(x::unmodifiable);
	}

	@Test void b07_instanceConstant_isUnmodifiableLeaf() {
		assertTrue(NotFound.INSTANCE.isUnmodifiable());
		assertInstanceOf(NotFound.class, NotFound.INSTANCE);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Message, header, status-line and constructor behavior on the root type
	//-----------------------------------------------------------------------------------------------------------------

	private static final Header H1 = new org.apache.http.message.BasicHeader("X-A", "1");
	private static final Header H2 = new org.apache.http.message.BasicHeader("X-A", "2");

	@Test void c01_constructors() {
		assertEquals(0, new BasicHttpException().getStatusLine().getStatusCode());
		assertEquals(418, new BasicHttpException(418).getStatusLine().getStatusCode());

		var a = new BasicHttpException(400, "bad %s", "thing");
		assertEquals("bad thing", a.getMessage());
		assertEquals(400, a.getStatusLine().getStatusCode());

		var cause = new IllegalStateException("inner");
		var b = new BasicHttpException(500, cause);
		assertSame(cause, b.getCause());
		assertEquals("java.lang.IllegalStateException: inner", b.getMessage());

		var c = new BasicHttpException(500, cause, "outer %s", "msg");
		assertEquals("outer msg", c.getMessage());
		assertEquals("outer msg", assertDoesNotThrow(() -> org.apache.http.util.EntityUtils.toString(c.getEntity())));
	}

	@Test void c02_responseConstructor_copiesHeadersEntityStatusAndThrownMessage() throws Exception {
		var source = new BasicHttpException(404).setHeader2("X-Test", "v").setHeader2("Thrown", "java.lang.RuntimeException;original");
		source.setContent("body");
		// Cast selects the HttpResponse constructor rather than the protected copy constructor.
		var x = new BasicHttpException((HttpResponse)source);
		assertEquals(404, x.getStatusLine().getStatusCode());
		assertEquals("v", x.getFirstHeader("X-Test").getValue());
		assertEquals("original", x.getMessage());
		assertEquals("body", org.apache.http.util.EntityUtils.toString(x.getEntity()));
		assertThrows(IllegalArgumentException.class, () -> new BasicHttpException((org.apache.http.HttpResponse)null));
	}

	@Test void c03_responseConstructor_emptyOrMissingThrownKeepsDefaultMessage() {
		var noThrown = new BasicHttpException((HttpResponse)new BasicHttpException(404));
		assertEquals(404, noThrown.getStatusLine().getStatusCode());
		assertEquals("Not Found", noThrown.getMessage());
		var emptyThrown = new BasicHttpException((HttpResponse)new BasicHttpException(404).setHeader2("Thrown", ""));
		assertEquals(404, emptyThrown.getStatusLine().getStatusCode());
		assertEquals("Not Found", emptyThrown.getMessage());
	}

	@Test void c04_headerAccessors() {
		var x = new BasicHttpException(200);
		x.addHeader(H1);
		x.addHeader("X-A", "2");
		x.addHeader("X-B", "b");
		assertTrue(x.containsHeader("x-a"));
		assertFalse(x.containsHeader("X-Z"));
		assertEquals(3, x.getAllHeaders().length);
		assertEquals(2, x.getHeaders("X-A").length);
		assertEquals("1", x.getFirstHeader("X-A").getValue());
		assertEquals("2", x.getLastHeader("X-A").getValue());
		assertNull(x.getFirstHeader("X-Z"));
		assertNull(x.getLastHeader("X-Z"));
		assertSame(x.getHeaders(), x.getHeaders());
		assertTrue(x.headerIterator().hasNext());
		assertTrue(x.headerIterator("X-B").hasNext());
		x.removeHeader(H1);
		assertEquals(1, x.getHeaders("X-A").length);
		x.removeHeaders("X-A");
		assertFalse(x.containsHeader("X-A"));
		x.setHeader(H1);
		assertEquals("1", x.getFirstHeader("X-A").getValue());
		x.setHeader("X-A", "3");
		assertEquals("3", x.getFirstHeader("X-A").getValue());
		x.setHeaders(new Header[]{H1, H2});
		assertEquals(2, x.getAllHeaders().length);
		assertEquals(0, x.getHeaders("X-B").length);
		x.setHeaders(org.apache.juneau.http.classic.header.HeaderList.of(H2));
		assertEquals(1, x.getAllHeaders().length);
		x.setHeaders(java.util.List.of(H1));
		assertEquals("1", x.getFirstHeader("X-A").getValue());
		x.setHeaders2(H2);
		assertEquals("2", x.getFirstHeader("X-A").getValue());
	}

	@Test void c05_statusLineMutators() throws Exception {
		var x = new BasicHttpException(200);
		x.setStatusCode(201);
		assertEquals(201, x.getStatusLine().getStatusCode());
		x.setReasonPhrase("Made");
		assertEquals("Made", x.getStatusLine().getReasonPhrase());
		x.setStatusLine(org.apache.http.HttpVersion.HTTP_1_0, 202);
		assertEquals(org.apache.http.HttpVersion.HTTP_1_0, x.getProtocolVersion());
		assertEquals(202, x.getStatusLine().getStatusCode());
		x.setStatusLine(org.apache.http.HttpVersion.HTTP_1_1, 203, "Other");
		assertEquals("Other", x.getStatusLine().getReasonPhrase());
		x.setStatusLine(new org.apache.http.message.BasicStatusLine(org.apache.http.HttpVersion.HTTP_1_1, 204, "None"));
		assertEquals(204, x.getStatusLine().getStatusCode());
		assertEquals("None", x.getStatusLine().getReasonPhrase());
		x.setLocale(java.util.Locale.FRANCE);
		assertEquals(java.util.Locale.FRANCE, x.getLocale());
		x.setParams(null);
		assertNull(x.getParams());
		x.setEntity(new org.apache.http.entity.StringEntity("e", java.nio.charset.StandardCharsets.UTF_8));
		assertNotNull(x.getEntity());
		x.setEntity(null);
		// Clearing the entity reverts to the lazily-built default entity, whose text is the message.
		assertEquals(x.getMessage(), org.apache.http.util.EntityUtils.toString(x.getEntity()));
		x.setMessage("hello %s", "world");
		assertEquals("hello world", x.getMessage());
		assertEquals("hello world", x.toString());
	}

	@Test void c06_getMessage_fallbacks() {
		assertEquals("java.lang.IllegalStateException: inner", new BasicHttpException(500, new IllegalStateException("inner")).getMessage());
		var x = new BasicHttpException(404).setReasonPhrase2("Nope");
		assertEquals("Nope", x.getMessage());
		var causeMessage = new BasicHttpException(404, new RuntimeException("from cause"), null);
		assertEquals("from cause", causeMessage.getMessage());
		var noMessageCause = new BasicHttpException(404, new RuntimeException(), null).setReasonPhrase2("Nope");
		assertEquals("Nope", noMessageCause.getMessage());
	}

	@Test void c07_getFullStackMessage_andRootCause() {
		var root = new IllegalStateException("<root> & co");
		var mid = new RuntimeException(null, root);
		var x = new BasicHttpException(500, mid, "top <msg>");
		var raw = x.getFullStackMessage(false);
		assertTrue(raw.startsWith("top <msg>"), raw);
		assertTrue(raw.contains("Caused by (RuntimeException)\nCaused by (IllegalStateException): <root> & co"), raw);
		var scrubbed = x.getFullStackMessage(true);
		assertTrue(scrubbed.startsWith("top  msg "), scrubbed);
		assertTrue(scrubbed.contains("Caused by (IllegalStateException):  root    co"), scrubbed);
		assertSame(root, new BasicHttpException(500, new java.lang.reflect.InvocationTargetException(root)).getRootCause());
		assertNull(new BasicHttpException(500).getRootCause());
	}

	@Test void c08_assertStatusCode_andFluentSetters() {
		var x = new BasicHttpException(404);
		x.assertStatusCode(new BasicHttpException(404));
		var other = new BasicHttpException(200);
		assertThrows(IllegalArgumentException.class, () -> x.assertStatusCode(other));
		assertThrows(IllegalArgumentException.class, () -> x.assertStatusCode(null));

		var y = new BasicHttpException(200);
		assertSame(y, y.setContent(new org.apache.http.entity.StringEntity("e", java.nio.charset.StandardCharsets.UTF_8)));
		assertSame(y, y.setContent("s"));
		assertSame(y, y.setLocale2(java.util.Locale.US));
		assertSame(y, y.setProtocolVersion(org.apache.http.HttpVersion.HTTP_1_0));
		assertSame(y, y.setReasonPhrase2("R"));
		assertSame(y, y.setReasonPhraseCatalog(org.apache.http.impl.EnglishReasonPhraseCatalog.INSTANCE));
		assertSame(y, y.setStatusCode2(201));
		assertSame(y, y.setStatusLine(org.apache.juneau.http.classic.BasicStatusLine.create(202, "Acc")));
		assertEquals(202, y.getStatusLine().getStatusCode());
		assertSame(y, y.setMessage("m"));
	}

	@Test void c09_equalsAndHashCode_distinguishContent() {
		var x = new BasicHttpException(404).setHeader2("X-A", "1");
		assertEquals(x, x);
		assertNotEquals("not an exception", x);
		assertNotEquals(x, new BasicHttpException(404).setHeader2("X-A", "2"));
		assertNotEquals(x, new BasicHttpException(500).setHeader2("X-A", "1"));
		assertNotEquals(x, new BasicHttpException(404, "other").setHeader2("X-A", "1"));
		assertEquals(x, new BasicHttpException(404).setHeader2("X-A", "1"));
	}

	@Test void c10_unmodifiableRoot_rejectsEveryMutator() {
		var u = new BasicHttpException(404).setHeader2("X-A", "1").unmodifiable();
		assertSame(u, u.unmodifiable());
		assertThrows(UnsupportedOperationException.class, () -> u.addHeader(H1));
		assertThrows(UnsupportedOperationException.class, () -> u.setParams(null));
		assertThrows(UnsupportedOperationException.class, () -> u.setStatusLine(org.apache.http.HttpVersion.HTTP_1_1, 200));
		assertThrows(UnsupportedOperationException.class, () -> u.setStatusLine(org.apache.http.HttpVersion.HTTP_1_1, 200, "OK"));
		var statusLine = new org.apache.http.message.BasicStatusLine(org.apache.http.HttpVersion.HTTP_1_1, 200, "OK");
		assertThrows(UnsupportedOperationException.class, () -> u.setStatusLine(statusLine));
		assertEquals("1", u.getFirstHeader("X-A").getValue());
	}
}
