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

import org.apache.http.*;
import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Immutability-paradigm tests for the {@link BasicHttpResponse} hierarchy, using {@link Ok} as a representative leaf.
 *
 * <p>
 * Mirrors the pilot ({@code BasicStatusLine_Test} b01–b06): reflective mutator guard, idempotency, content
 * equality, snapshot independence, and snapshot-construction regression.
 */
class BasicHttpResponse_Test extends TestBase {

	@Test void b01_unmodifiable_returnsUnmodifiableLeafType() {
		var ok = new Ok();
		assertFalse(ok.isUnmodifiable());
		var u = ok.unmodifiable();
		assertTrue(u.isUnmodifiable());
		assertInstanceOf(Ok.Unmodifiable.class, u);
		assertInstanceOf(Ok.class, u, "Snapshot must preserve the concrete leaf type.");
	}

	@Test void b02_reflectiveGuard_everyPublicMutatorThrows() throws Exception {
		var u = new Ok().setContent("body").setHeader2("X-Test", "v").unmodifiable();
		var mutators = 0;
		for (var m : Ok.class.getMethods()) {
			var n = m.getName();
			if (Modifier.isStatic(m.getModifiers()) || ! (n.startsWith("set") || n.startsWith("add") || n.startsWith("remove")))
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
		var u = new Ok().unmodifiable();
		assertSame(u, u.unmodifiable());
	}

	@Test void b04_contentEquality_equalContentComparesEqual() {
		// Content equality — the slices satisfiable on their own.
		// Two independently-frozen snapshots of equal content compare equal.
		var u1 = new Ok().setHeader2("X-Test", "v").unmodifiable();
		var u2 = new Ok().setHeader2("X-Test", "v").unmodifiable();
		assertEquals(u1, u2);
		assertEquals(u1.hashCode(), u2.hashCode());
		// Two modifiable beans of equal content compare equal.
		var m1 = new Ok().setHeader2("X-Test", "v");
		var m2 = new Ok().setHeader2("X-Test", "v");
		assertEquals(m1, m2);
		assertEquals(m1.hashCode(), m2.hashCode());
		// Cross-boundary equality: now that HeaderList equality is content-only, a modifiable bean
		// compares equal to its own frozen snapshot.
		assertEquals(m1, m1.unmodifiable());
		assertEquals(m1.hashCode(), m1.unmodifiable().hashCode());
	}

	@Test void b05_snapshotIndependence_mutatingOriginalDoesNotAffectSnapshot() {
		var ok = new Ok().setHeader2("X-Test", "before");
		var u = ok.unmodifiable();
		ok.setHeader2("X-Test", "after").setStatusCode2(201);
		assertEquals("before", u.getFirstHeader("X-Test").getValue());
		assertEquals(200, u.getStatusLine().getStatusCode());
	}

	@Test void b06_snapshotConstruction_freezeDoesNotThrow() {
		// Regression: constructing the snapshot must not route the freeze through the throwing modify().
		var ok = new Ok().setContent("body").setHeader2("X-Test", "v").setReasonPhrase2("Custom");
		assertDoesNotThrow(ok::unmodifiable);
	}

	@Test void b07_instanceConstant_isUnmodifiableLeaf() {
		assertTrue(Ok.INSTANCE.isUnmodifiable());
		assertInstanceOf(Ok.class, Ok.INSTANCE);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Header, entity, status-line and equality behavior on the root type
	//-----------------------------------------------------------------------------------------------------------------

	private static final Header H1 = new org.apache.http.message.BasicHeader("X-A", "1");
	private static final Header H2 = new org.apache.http.message.BasicHeader("X-A", "2");

	@Test void c01_headerAccessors() {
		var x = new Ok();
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
		x.setHeaders(org.apache.juneau.http.classic.header.HeaderList.of(H2));
		assertEquals(1, x.getAllHeaders().length);
		x.setHeaders(java.util.List.of(H1));
		assertEquals("1", x.getFirstHeader("X-A").getValue());
		x.setHeaders2(H2);
		assertEquals("2", x.getFirstHeader("X-A").getValue());
		x.setHeader2(H1);
		assertEquals("1", x.getFirstHeader("X-A").getValue());
	}

	@Test void c02_statusLineMutators() throws Exception {
		var x = new Ok();
		x.setStatusCode(201);
		assertEquals(201, x.getStatusLine().getStatusCode());
		x.setReasonPhrase("Made");
		assertEquals("Made", x.getStatusLine().getReasonPhrase());
		x.setStatusLine(HttpVersion.HTTP_1_0, 202);
		assertEquals(HttpVersion.HTTP_1_0, x.getProtocolVersion());
		assertEquals(202, x.getStatusLine().getStatusCode());
		x.setStatusLine(HttpVersion.HTTP_1_1, 203, "Other");
		assertEquals("Other", x.getStatusLine().getReasonPhrase());
		x.setStatusLine(new org.apache.http.message.BasicStatusLine(HttpVersion.HTTP_1_1, 204, "None"));
		assertEquals(204, x.getStatusLine().getStatusCode());
		assertEquals("None", x.getStatusLine().getReasonPhrase());
		x.setLocale(java.util.Locale.FRANCE);
		assertEquals(java.util.Locale.FRANCE, x.getLocale());
		x.setParams(null);
		assertNull(x.getParams());
		x.setEntity(new org.apache.http.entity.StringEntity("e", java.nio.charset.StandardCharsets.UTF_8));
		assertNotNull(x.getEntity());
		x.setEntity(null);
		// Clearing the entity reverts to the lazily-built default entity, whose text is the reason phrase.
		assertEquals(x.getStatusLine().getReasonPhrase(), org.apache.http.util.EntityUtils.toString(x.getEntity()));
	}

	@Test void c03_fluentSetters() {
		var x = new Ok();
		assertSame(x, x.setContent(new org.apache.http.entity.StringEntity("e", java.nio.charset.StandardCharsets.UTF_8)));
		assertSame(x, x.setContent("s"));
		assertSame(x, x.setLocale2(java.util.Locale.US));
		assertSame(x, x.setProtocolVersion(HttpVersion.HTTP_1_0));
		assertSame(x, x.setReasonPhrase2("R"));
		assertSame(x, x.setReasonPhraseCatalog(org.apache.http.impl.EnglishReasonPhraseCatalog.INSTANCE));
		assertSame(x, x.setStatusCode2(201));
		assertSame(x, x.setStatusLine(org.apache.juneau.http.classic.BasicStatusLine.create(202, "Acc")));
		assertEquals(202, x.getStatusLine().getStatusCode());
		assertSame(x, x.setLocation("http://localhost/a"));
		assertEquals("http://localhost/a", x.getFirstHeader("Location").getValue());
		assertSame(x, x.setLocation(java.net.URI.create("http://localhost/b")));
		assertEquals("http://localhost/b", x.getFirstHeader("Location").getValue());
	}

	@Test void c04_getEntity_defaultsToReasonPhrase_andToString() throws Exception {
		var x = new Ok();
		assertEquals("OK", org.apache.http.util.EntityUtils.toString(x.getEntity()));
		assertSame(x.getEntity(), x.getEntity());
		assertTrue(x.toString().contains("200"), x::toString);
		var withoutContent = new Ok().setHeader2("X-A", "1");
		assertFalse(withoutContent.toString().contains("StringEntity"), withoutContent::toString);
		var withContent = new Ok().setContent("body");
		assertNotEquals(withoutContent.toString(), withContent.toString());
	}

	@Test void c05_responseConstructor_copiesHeadersContentAndStatus() throws Exception {
		var source = new Created().setHeader2("X-Test", "v").setContent("body");
		var x = new Created((HttpResponse)source);
		assertEquals(201, x.getStatusLine().getStatusCode());
		assertEquals("v", x.getFirstHeader("X-Test").getValue());
		assertEquals("body", org.apache.http.util.EntityUtils.toString(x.getEntity()));
		assertThrows(IllegalArgumentException.class, () -> new Created((HttpResponse)null));
	}

	@Test void c06_equalsAndHashCode_distinguishContent() {
		var x = new Ok().setHeader2("X-A", "1").setContent("body");
		assertEquals(x, x);
		assertNotEquals("not a response", x);
		assertNotEquals(x, new Ok().setHeader2("X-A", "2").setContent("body"));
		assertNotEquals(x, new Ok().setHeader2("X-A", "1").setContent("other"));
		assertNotEquals(x, new Created().setHeader2("X-A", "1").setContent("body"));
		var shared = new Ok().setHeader2("X-A", "1").setContent("body");
		var y = new Ok(shared);
		assertEquals(shared, y);
		assertEquals(shared.hashCode(), y.hashCode());
	}

	@Test void c07_unmodifiableRoot_rejectsHeaderAndStatusLineMutators() {
		var u = new Ok().unmodifiable();
		assertThrows(UnsupportedOperationException.class, () -> u.addHeader(H1));
		assertThrows(UnsupportedOperationException.class, () -> u.setParams(null));
		assertThrows(UnsupportedOperationException.class, () -> u.setLocation("http://localhost/x"));
		assertThrows(UnsupportedOperationException.class, () -> u.setStatusLine(HttpVersion.HTTP_1_1, 200));
		assertThrows(UnsupportedOperationException.class, () -> u.setStatusLine(HttpVersion.HTTP_1_1, 200, "OK"));
		var statusLine = new org.apache.http.message.BasicStatusLine(HttpVersion.HTTP_1_1, 200, "OK");
		assertThrows(UnsupportedOperationException.class, () -> u.setStatusLine(statusLine));
	}
}
