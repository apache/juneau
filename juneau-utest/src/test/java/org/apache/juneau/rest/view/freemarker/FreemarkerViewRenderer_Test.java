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
package org.apache.juneau.rest.view.freemarker;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link FreemarkerViewRenderer}.
 *
 * <p>
 * Covers the diagnostic-message constant + default content-type. The full
 * {@link FreemarkerViewRenderer#process process(...)} dispatch path is exercised end-to-end by
 * the MockRest-flavored tests in this package (full Jetty / Spring Boot real-container coverage
 * is deferred to a follow-on TODO, the FreeMarker analog of TODO-97 for JSP, TODO-107 for
 * Thymeleaf, and TODO-108 for Mustache).
 *
 * @since 9.5.0
 */
class FreemarkerViewRenderer_Test extends TestBase {

	/* ---------------------------------------------------------------------------------------- *
	 * Section A: NO_ENGINE_DIAGNOSTIC text
	 * ---------------------------------------------------------------------------------------- */

	@Test void a01_diagnosticNamesFreemarkerCore() {
		assertTrue(FreemarkerViewRenderer.NO_ENGINE_DIAGNOSTIC.contains("org.freemarker:freemarker"),
			"Diagnostic must name the FreeMarker engine core dependency");
	}

	@Test void a02_diagnosticCallsOutSpringBootStarter() {
		assertTrue(FreemarkerViewRenderer.NO_ENGINE_DIAGNOSTIC.contains("spring-boot-starter-freemarker"),
			"Diagnostic must mention the Spring Boot starter alternative");
	}

	@Test void a03_diagnosticLinksToDocs() {
		assertTrue(FreemarkerViewRenderer.NO_ENGINE_DIAGNOSTIC.contains("FreemarkerViewSupport"),
			"Diagnostic must link to the FreeMarker topic page");
	}

	@Test void a04_defaultContentTypeIsHtmlUtf8() {
		assertEquals("text/html;charset=UTF-8", FreemarkerViewRenderer.DEFAULT_CONTENT_TYPE);
	}
}
