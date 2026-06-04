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

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link JspViewRenderer}.
 *
 * <p>
 * Covers the path-joining helper and the diagnostic-message constant. The
 * {@link JspViewRenderer#process process(...)} dispatch path is exercised end-to-end by the
 * Jetty- / Spring-Boot-flavored integration tests in this package.
 *
 * @since 10.0.0
 */
class JspViewRenderer_Test extends TestBase {

	/* ---------------------------------------------------------------------------------------- *
	 * Section A: joinPath
	 * ---------------------------------------------------------------------------------------- */

	@Test void a01_joinTrailingSlashLeadingSlash() {
		// Both sides have a slash -> the leading slash on template is dropped.
		assertEquals("/WEB-INF/views/hello.jsp",
			JspViewRenderer.joinPath("/WEB-INF/views/", "/hello.jsp"));
	}

	@Test void a02_joinNoTrailingNoLeading() {
		// Neither side has a slash -> insert one.
		assertEquals("/WEB-INF/views/hello.jsp",
			JspViewRenderer.joinPath("/WEB-INF/views", "hello.jsp"));
	}

	@Test void a03_joinTrailingOnly() {
		assertEquals("/WEB-INF/views/hello.jsp",
			JspViewRenderer.joinPath("/WEB-INF/views/", "hello.jsp"));
	}

	@Test void a04_joinLeadingOnly() {
		assertEquals("/WEB-INF/views/hello.jsp",
			JspViewRenderer.joinPath("/WEB-INF/views", "/hello.jsp"));
	}

	@Test void a05_joinHandlesRootBase() {
		assertEquals("/hello.jsp", JspViewRenderer.joinPath("/", "hello.jsp"));
		assertEquals("/hello.jsp", JspViewRenderer.joinPath("/", "/hello.jsp"));
	}

	@Test void a06_joinHandlesNullBase() {
		assertEquals("/hello.jsp", JspViewRenderer.joinPath(null, "/hello.jsp"));
		assertEquals("/hello.jsp", JspViewRenderer.joinPath(null, "hello.jsp"));
	}

	@Test void a07_joinHandlesNullTemplate() {
		assertEquals("/WEB-INF/views/",
			JspViewRenderer.joinPath("/WEB-INF/views/", null));
	}

	@Test void a08_joinHandlesEmptyTemplate() {
		// Neither side has a slash, template is empty -> "/views" + "/" + "" = "/views/"
		assertEquals("/views/", JspViewRenderer.joinPath("/views", ""));
	}

	/* ---------------------------------------------------------------------------------------- *
	 * Section B: NO_ENGINE_DIAGNOSTIC text
	 * ---------------------------------------------------------------------------------------- */

	@Test void b01_diagnosticNamesJettyEngine() {
		assertTrue(JspViewRenderer.NO_ENGINE_DIAGNOSTIC.contains("jetty-ee11-apache-jsp"),
			"Diagnostic must name the Jetty 12 EE11 engine dependency");
	}

	@Test void b02_diagnosticNamesTomcatEngine() {
		assertTrue(JspViewRenderer.NO_ENGINE_DIAGNOSTIC.contains("tomcat-embed-jasper"),
			"Diagnostic must name the embedded Tomcat engine dependency");
	}

	@Test void b03_diagnosticLinksToDocs() {
		assertTrue(JspViewRenderer.NO_ENGINE_DIAGNOSTIC.contains("JspViewSupport"),
			"Diagnostic must link to the JSP topic page");
	}
}
