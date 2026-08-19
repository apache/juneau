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
package org.apache.juneau.rest.server.views.markdown;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * SPI-resolution tests for {@link MarkdownRenderer#resolve()} and its testable {@code resolve(Iterable)} seam:
 * a registered provider wins, and the commonmark-java default is the fallback when none is registered.
 */
class MarkdownRenderer_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// a) The SPI is a simple, swappable functional contract
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_interfaceIsTriviallyImplementable() {
		MarkdownRenderer custom = md -> "<custom>" + md + "</custom>";
		assertEquals("<custom>x</custom>", custom.toHtml("x"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b) resolve(Iterable) - both branches without touching the JVM-wide ServiceLoader registry
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_registeredProviderWins() {
		MarkdownRenderer provider = md -> "SENTINEL";
		var resolved = MarkdownRenderer.resolve(List.of(provider));
		assertSame(provider, resolved);
		assertEquals("SENTINEL", resolved.toHtml("anything"));
	}

	@Test void b02_firstProviderWinsWhenSeveralRegistered() {
		MarkdownRenderer first = md -> "FIRST";
		MarkdownRenderer second = md -> "SECOND";
		assertEquals("FIRST", MarkdownRenderer.resolve(List.of(first, second)).toHtml("x"));
	}

	@Test void b03_fallsBackToCommonmarkDefaultWhenNoProvider() {
		var resolved = MarkdownRenderer.resolve(List.<MarkdownRenderer>of());
		assertInstanceOf(CommonmarkMarkdownRenderer.class, resolved);
		assertEquals("<p>hi</p>\n", resolved.toHtml("hi"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c) resolve() - the production ServiceLoader path
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_resolve_returnsWorkingRenderer() {
		// No provider is registered on the test classpath, so this exercises the commonmark-java fallback end-to-end.
		var resolved = MarkdownRenderer.resolve();
		assertNotNull(resolved);
		assertInstanceOf(CommonmarkMarkdownRenderer.class, resolved);
		var html = resolved.toHtml("| A |\n| --- |\n| 1 |");
		assertTrue(html.contains("<table>"), () -> "expected GFM table from the default, got:\n" + html);
	}
}
