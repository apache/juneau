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
package org.apache.juneau.rest.view.jsp;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.view.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link JspView}.
 *
 * <p>
 * Covers the immutable-value-class contract: {@link JspView#of(String) of(...)} validation, fluent
 * {@link JspView#attr(String, Object) attr(...)} / {@link JspView#attrs(Map) attrs(...)} /
 * {@link JspView#header(String, String) header(...)} chaining, and the {@link View}-interface
 * surface ({@code getTemplateName} / {@code getAttributes} / {@code getResponseHeaders}).
 *
 * @since 9.5.0
 */
class JspView_Test extends TestBase {

	/* ---------------------------------------------------------------------------------------- *
	 * Section A: factory + invariants
	 * ---------------------------------------------------------------------------------------- */

	@Test void a01_ofProducesViewWithTemplateAndEmptyMaps() {
		var v = JspView.of("hello.jsp");
		assertEquals("hello.jsp", v.getTemplateName());
		assertTrue(v.getAttributes().isEmpty(), "attributes must be empty");
		assertTrue(v.getResponseHeaders().isEmpty(), "responseHeaders must be empty");
	}

	@Test void a02_ofRejectsNullTemplate() {
		assertThrows(IllegalArgumentException.class, () -> JspView.of(null));
	}

	@Test void a03_ofRejectsBlankTemplate() {
		assertThrows(IllegalArgumentException.class, () -> JspView.of(""));
		assertThrows(IllegalArgumentException.class, () -> JspView.of("   "));
	}

	@Test void a04_isViewInstance() {
		// Verifies the type relationship that ResponseProcessor pattern-matches on.
		View v = JspView.of("x.jsp");
		assertNotNull(v);
	}

	/* ---------------------------------------------------------------------------------------- *
	 * Section B: attr(...)
	 * ---------------------------------------------------------------------------------------- */

	@Test void b01_attrAddsBindingOnNewInstance() {
		var v0 = JspView.of("hello.jsp");
		var v1 = v0.attr("name", "Bob");

		assertNotSame(v0, v1, "attr() must return a new instance");
		assertTrue(v0.getAttributes().isEmpty(), "original must remain unchanged");
		assertEquals("Bob", v1.getAttributes().get("name"));
	}

	@Test void b02_attrChainsMultipleBindings() {
		var v = JspView.of("hello.jsp")
			.attr("name", "Bob")
			.attr("age", 42)
			.attr("active", Boolean.TRUE);

		assertEquals("Bob", v.getAttributes().get("name"));
		assertEquals(42, v.getAttributes().get("age"));
		assertEquals(Boolean.TRUE, v.getAttributes().get("active"));
		assertEquals(3, v.getAttributes().size());
	}

	@Test void b03_attrReplacesExistingBinding() {
		var v = JspView.of("hello.jsp")
			.attr("name", "Bob")
			.attr("name", "Alice");

		assertEquals("Alice", v.getAttributes().get("name"));
		assertEquals(1, v.getAttributes().size());
	}

	@Test void b04_attrRejectsNullValue() {
		// Servlet spec: setAttribute(name, null) removes the binding. JspView mirrors that at
		// build-time so the renderer never has to short-circuit a request-attribute write at
		// dispatch time.
		assertThrows(IllegalArgumentException.class,
			() -> JspView.of("hello.jsp").attr("missing", null));
	}

	@Test void b05_attrRejectsNullKey() {
		assertThrows(IllegalArgumentException.class,
			() -> JspView.of("hello.jsp").attr(null, "v"));
	}

	@Test void b06_attrRejectsBlankKey() {
		assertThrows(IllegalArgumentException.class,
			() -> JspView.of("hello.jsp").attr("", "v"));
		assertThrows(IllegalArgumentException.class,
			() -> JspView.of("hello.jsp").attr("   ", "v"));
	}

	@Test void b07_attributesMapIsImmutable() {
		var v = JspView.of("hello.jsp").attr("name", "Bob");
		assertThrows(UnsupportedOperationException.class,
			() -> v.getAttributes().put("name", "Alice"));
	}

	/* ---------------------------------------------------------------------------------------- *
	 * Section C: attrs(...)
	 * ---------------------------------------------------------------------------------------- */

	@Test void c01_attrsMergesMap() {
		var v = JspView.of("hello.jsp")
			.attrs(Map.of("a", 1, "b", 2));
		assertEquals(1, v.getAttributes().get("a"));
		assertEquals(2, v.getAttributes().get("b"));
	}

	@Test void c02_attrsNullReturnsSameInstance() {
		var v0 = JspView.of("hello.jsp");
		var v1 = v0.attrs(null);
		assertSame(v0, v1, "attrs(null) is a no-op");
	}

	@Test void c03_attrsEmptyReturnsSameInstance() {
		var v0 = JspView.of("hello.jsp");
		var v1 = v0.attrs(Map.of());
		assertSame(v0, v1, "attrs(emptyMap) is a no-op");
	}

	@Test void c04_attrsRejectsBlankKey() {
		var m = new LinkedHashMap<String, Object>();
		m.put("ok", 1);
		m.put("", 2);
		assertThrows(IllegalArgumentException.class,
			() -> JspView.of("hello.jsp").attrs(m));
	}

	@Test void c05_attrsOverridesExistingBindings() {
		var v = JspView.of("hello.jsp")
			.attr("a", 1)
			.attrs(Map.of("a", 99, "b", 2));
		assertEquals(99, v.getAttributes().get("a"));
		assertEquals(2, v.getAttributes().get("b"));
	}

	/* ---------------------------------------------------------------------------------------- *
	 * Section D: header(...)
	 * ---------------------------------------------------------------------------------------- */

	@Test void d01_headerAddsResponseHeader() {
		var v = JspView.of("hello.jsp")
			.header("Content-Type", "text/html; charset=UTF-8");
		assertEquals("text/html; charset=UTF-8", v.getResponseHeaders().get("Content-Type"));
	}

	@Test void d02_headerChainsMultipleHeaders() {
		var v = JspView.of("hello.jsp")
			.header("Content-Type", "text/html; charset=UTF-8")
			.header("Cache-Control", "no-store");
		assertEquals(2, v.getResponseHeaders().size());
	}

	@Test void d03_headerRejectsBlankName() {
		assertThrows(IllegalArgumentException.class,
			() -> JspView.of("hello.jsp").header("", "v"));
		assertThrows(IllegalArgumentException.class,
			() -> JspView.of("hello.jsp").header(null, "v"));
	}

	@Test void d04_headerRejectsNullValue() {
		// null header value is rejected (unlike attribute values).
		assertThrows(IllegalArgumentException.class,
			() -> JspView.of("hello.jsp").header("X", null));
	}

	@Test void d05_headerReturnsNewInstance() {
		var v0 = JspView.of("hello.jsp");
		var v1 = v0.header("X", "1");
		assertNotSame(v0, v1);
		assertTrue(v0.getResponseHeaders().isEmpty());
	}

	@Test void d06_responseHeadersMapIsImmutable() {
		var v = JspView.of("hello.jsp").header("X", "1");
		assertThrows(UnsupportedOperationException.class,
			() -> v.getResponseHeaders().put("Y", "2"));
	}

	/* ---------------------------------------------------------------------------------------- *
	 * Section E: toString + misc
	 * ---------------------------------------------------------------------------------------- */

	@Test void e01_toStringIncludesTemplateName() {
		var v = JspView.of("hello.jsp").attr("k", "v").header("H", "1");
		var s = v.toString();
		assertTrue(s.contains("hello.jsp"), "toString must include template name: " + s);
		assertTrue(s.contains("k"), "toString must include attribute keys: " + s);
		assertTrue(s.contains("H"), "toString must include header keys: " + s);
	}
}
