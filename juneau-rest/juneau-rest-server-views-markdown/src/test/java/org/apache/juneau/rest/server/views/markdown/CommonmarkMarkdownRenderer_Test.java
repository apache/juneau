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

import static org.apache.juneau.BasicTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Behavioural tests for {@link CommonmarkMarkdownRenderer} - the shipped commonmark-java default with the GFM
 * tables extension enabled.
 */
class CommonmarkMarkdownRenderer_Test extends TestBase {

	private static final CommonmarkMarkdownRenderer RENDERER = new CommonmarkMarkdownRenderer();

	//-----------------------------------------------------------------------------------------------------------------
	// a) Core CommonMark rendering
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_paragraph() {
		assertEquals("<p>Hello world.</p>\n", RENDERER.toHtml("Hello world."));
	}

	@Test void a02_heading() {
		assertEquals("<h1>Runbook</h1>\n", RENDERER.toHtml("# Runbook"));
	}

	@Test void a03_unorderedList() {
		assertEquals("<ul>\n<li>one</li>\n<li>two</li>\n</ul>\n", RENDERER.toHtml("- one\n- two"));
	}

	@Test void a04_fencedCodeBlock() {
		var html = RENDERER.toHtml("```\ncode();\n```");
		assertTrue(html.contains("<pre><code>"), () -> "expected a code block, got:\n" + html);
		assertTrue(html.contains("code();"), () -> "expected code content, got:\n" + html);
	}

	@Test void a05_inlineEmphasisAndLink() {
		assertEquals("<p><em>hi</em> <a href=\"https://x\">y</a></p>\n", RENDERER.toHtml("*hi* [y](https://x)"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b) GFM tables extension - the reason this ships enabled by default
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_pipeTable_rendersAsTable_notLiteralPipes() {
		var md = "| A | B |\n| --- | --- |\n| 1 | 2 |";
		var html = RENDERER.toHtml(md);
		assertTrue(html.contains("<table>"), () -> "GFM tables extension not enabled - got:\n" + html);
		assertTrue(html.contains("<th>A</th>"), () -> "expected header cell, got:\n" + html);
		assertTrue(html.contains("<td>1</td>"), () -> "expected body cell, got:\n" + html);
		assertFalse(html.contains("| A | B |"), () -> "table rendered as literal pipes, got:\n" + html);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c) Edge cases
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_emptyString_rendersEmpty() {
		assertEquals("", RENDERER.toHtml(""));
	}

	@Test void c02_null_throws() {
		assertThrowsWithMessage(IllegalArgumentException.class, "Markdown source must not be null.", () -> RENDERER.toHtml(null));
	}

	@Test void c03_rendererIsReusableAcrossCalls() {
		assertEquals("<p>one</p>\n", RENDERER.toHtml("one"));
		assertEquals("<p>two</p>\n", RENDERER.toHtml("two"));
	}
}
