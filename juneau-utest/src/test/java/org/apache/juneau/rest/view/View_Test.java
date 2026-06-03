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
package org.apache.juneau.rest.view;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Contract tests for the engine-agnostic {@link View} interface.
 *
 * <p>
 * These tests pin the stable interface seam that per-engine bridge modules
 * ({@code juneau-rest-server-view-jsp}, future {@code -thymeleaf} / {@code -mustache} /
 * {@code -freemarker}) build against:
 *
 * <ul>
 * 	<li>{@link View#getTemplateName()} and {@link View#getAttributes()} are abstract &mdash;
 * 		bridge modules must supply them.
 * 	<li>{@link View#getResponseHeaders()} is a {@code default} method returning an empty map
 * 		&mdash; bridge modules may override but are not required to.
 * 	<li>The default {@code getResponseHeaders()} return value is immutable, matching the
 * 		{@link Map#of() Map.of()} contract.
 * </ul>
 *
 * @since 9.5.0
 */
@SuppressWarnings({"java:S5778" /* assertThrows lambdas with chained calls; intermediate invocations do not throw in practice */})
class View_Test extends TestBase {

	/** Minimal {@code View} impl that exercises the abstract members only. */
	private static final class A01_MinimalView implements View {
		private final String name;
		private final Map<String, Object> attrs;

		A01_MinimalView(String name, Map<String, Object> attrs) {
			this.name = name;
			this.attrs = attrs;
		}

		@Override public String getTemplateName() { return name; }
		@Override public Map<String, Object> getAttributes() { return attrs; }
	}

	@Test void a01_minimalImplCarriesTemplateAndAttributes() {
		var attrs = Map.<String, Object>of("k1", "v1", "k2", 42);
		var view = new A01_MinimalView("hello.jsp", attrs);

		assertEquals("hello.jsp", view.getTemplateName());
		assertEquals(attrs, view.getAttributes());
	}

	@Test void a02_defaultResponseHeadersIsEmpty() {
		var view = new A01_MinimalView("x.jsp", Map.of());
		assertNotNull(view.getResponseHeaders(), "default getResponseHeaders() must not return null");
		assertTrue(view.getResponseHeaders().isEmpty(),
			"default getResponseHeaders() must be empty out of the box");
	}

	@Test void a03_defaultResponseHeadersIsImmutable() {
		// Map.of() is immutable - mutation must fail.
		var view = new A01_MinimalView("x.jsp", Map.of());
		assertThrows(UnsupportedOperationException.class,
			() -> view.getResponseHeaders().put("Content-Type", "text/html"),
			"default getResponseHeaders() must be immutable");
	}

	@Test void a04_emptyAttributesIsLegal() {
		var view = new A01_MinimalView("x.jsp", Map.of());
		assertTrue(view.getAttributes().isEmpty(),
			"a View carrying no attributes must return an empty map (not null)");
	}

	/** Custom impl that overrides {@code getResponseHeaders()} to verify the seam works. */
	private static final class A05_HeadersView implements View {
		@Override public String getTemplateName() { return "h.jsp"; }
		@Override public Map<String, Object> getAttributes() { return Map.of(); }
		@Override public Map<String, String> getResponseHeaders() {
			return Map.of("Content-Type", "text/html; charset=UTF-8");
		}
	}

	@Test void a05_responseHeadersOverrideHonored() {
		var view = new A05_HeadersView();
		assertEquals("text/html; charset=UTF-8",
			view.getResponseHeaders().get("Content-Type"),
			"View impls that override getResponseHeaders() must surface the override");
	}
}
