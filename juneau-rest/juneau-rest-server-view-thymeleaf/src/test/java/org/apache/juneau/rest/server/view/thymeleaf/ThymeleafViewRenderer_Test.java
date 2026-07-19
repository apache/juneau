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
package org.apache.juneau.rest.server.view.thymeleaf;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link ThymeleafViewRenderer}.
 *
 * @since 10.0.0
 */
class ThymeleafViewRenderer_Test extends TestBase {

	/* ---------------------------------------------------------------------------------------- *
	 * Section A: NO_ENGINE_DIAGNOSTIC text
	 * ---------------------------------------------------------------------------------------- */

	@Test void a01_diagnosticNamesSpringBootStarter() {
		assertTrue(ThymeleafViewRenderer.NO_ENGINE_DIAGNOSTIC.contains("spring-boot-starter-thymeleaf"),
			"Diagnostic must name the Spring Boot starter dependency");
	}

	@Test void a02_diagnosticNamesThymeleafCore() {
		assertTrue(ThymeleafViewRenderer.NO_ENGINE_DIAGNOSTIC.contains("org.thymeleaf:thymeleaf"),
			"Diagnostic must name the Thymeleaf core dependency");
	}

	@Test void a03_diagnosticLinksToDocs() {
		assertTrue(ThymeleafViewRenderer.NO_ENGINE_DIAGNOSTIC.contains("ThymeleafViewSupport"),
			"Diagnostic must link to the Thymeleaf topic page");
	}

	@Test void a04_defaultContentTypeIsHtmlUtf8() {
		assertEquals("text/html;charset=UTF-8", ThymeleafViewRenderer.DEFAULT_CONTENT_TYPE);
	}

	/* ---------------------------------------------------------------------------------------- *
	 * Section B: newContext(...) attribute population
	 * ---------------------------------------------------------------------------------------- */

	@Test void b01_newContextCopiesAllAttributes() {
		var view = ThymeleafView.of("hello")
			.attr("name", "Bob")
			.attr("age", 42)
			.attr("active", Boolean.TRUE);

		var req = mock(RestRequest.class);
		when(req.getLocale()).thenReturn(Locale.US);

		var ctx = ThymeleafViewRenderer.newContext(req, view);

		assertEquals(Locale.US, ctx.getLocale());
		assertEquals("Bob", ctx.getVariable("name"));
		assertEquals(42, ctx.getVariable("age"));
		assertEquals(Boolean.TRUE, ctx.getVariable("active"));
	}

	@Test void b02_newContextUsesRequestLocale() {
		var view = ThymeleafView.of("hello");

		var req = mock(RestRequest.class);
		when(req.getLocale()).thenReturn(Locale.GERMAN);

		var ctx = ThymeleafViewRenderer.newContext(req, view);
		assertEquals(Locale.GERMAN, ctx.getLocale());
	}

	@Test void b03_newContextEmptyViewProducesEmptyContext() {
		var view = ThymeleafView.of("hello");

		var req = mock(RestRequest.class);
		when(req.getLocale()).thenReturn(Locale.US);

		var ctx = ThymeleafViewRenderer.newContext(req, view);
		assertTrue(ctx.getVariableNames().isEmpty(),
			"newContext on an attribute-less view must produce a variable-less Context");
	}

	/* ---------------------------------------------------------------------------------------- *
	 * Section C: Typed-View template-name traversal gate
	 * ---------------------------------------------------------------------------------------- */

	@Test void c01_gateAllowsPlainName() {
		assertEquals("hello", ThymeleafViewRenderer.gateTemplateName("/", "hello"));
	}

	@Test void c02_gateAllowsSubdirName() {
		assertEquals("admin/dashboard", ThymeleafViewRenderer.gateTemplateName("/", "admin/dashboard"));
	}

	@Test void c03_gateAllowsNameUnderBasePath() {
		assertEquals("hello", ThymeleafViewRenderer.gateTemplateName("/templates/", "hello"));
	}

	@Test void c04_gateRejectsTraversal() {
		assertThrows(IllegalArgumentException.class, () -> ThymeleafViewRenderer.gateTemplateName("/", "../secret"));
	}

	@Test void c05_gateRejectsNestedTraversal() {
		assertThrows(IllegalArgumentException.class, () -> ThymeleafViewRenderer.gateTemplateName("/templates/", "a/b/../../../secret"));
	}
}
