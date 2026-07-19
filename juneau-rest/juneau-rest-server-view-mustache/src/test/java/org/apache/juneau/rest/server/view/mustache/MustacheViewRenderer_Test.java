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
package org.apache.juneau.rest.server.view.mustache;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link MustacheViewRenderer}.
 */
class MustacheViewRenderer_Test extends TestBase {

	/* ---------------------------------------------------------------------------------------- *
	 * Section A: NO_ENGINE_DIAGNOSTIC text
	 * ---------------------------------------------------------------------------------------- */

	@Test void a01_diagnosticNamesMustacheJavaCompiler() {
		assertTrue(MustacheViewRenderer.NO_ENGINE_DIAGNOSTIC.contains("com.github.spullara.mustache.java:compiler"),
			"Diagnostic must name the mustache.java compiler dependency");
	}

	@Test void a02_diagnosticCallsOutJmustacheGotcha() {
		assertTrue(MustacheViewRenderer.NO_ENGINE_DIAGNOSTIC.contains("jmustache"),
			"Diagnostic must call out the Spring Boot starter / jmustache gotcha");
	}

	@Test void a03_diagnosticLinksToDocs() {
		assertTrue(MustacheViewRenderer.NO_ENGINE_DIAGNOSTIC.contains("MustacheViewSupport"),
			"Diagnostic must link to the Mustache topic page");
	}

	@Test void a04_defaultContentTypeIsHtmlUtf8() {
		assertEquals("text/html;charset=UTF-8", MustacheViewRenderer.DEFAULT_CONTENT_TYPE);
	}

	/* ---------------------------------------------------------------------------------------- *
	 * Section B: Typed-View template-name traversal gate
	 * ---------------------------------------------------------------------------------------- */

	@Test void b01_gateAllowsPlainName() {
		assertEquals("hello", MustacheViewRenderer.gateTemplateName("/", "hello"));
	}

	@Test void b02_gateAllowsSubdirName() {
		assertEquals("admin/dashboard", MustacheViewRenderer.gateTemplateName("/", "admin/dashboard"));
	}

	@Test void b03_gateAllowsNameUnderBasePath() {
		assertEquals("hello.mustache", MustacheViewRenderer.gateTemplateName("/templates/", "hello.mustache"));
	}

	@Test void b04_gateRejectsTraversal() {
		assertThrows(IllegalArgumentException.class, () -> MustacheViewRenderer.gateTemplateName("/", "../secret"));
	}

	@Test void b05_gateRejectsNestedTraversal() {
		assertThrows(IllegalArgumentException.class, () -> MustacheViewRenderer.gateTemplateName("/templates/", "a/b/../../../secret"));
	}
}
