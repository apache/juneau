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
package org.apache.juneau.rest.server.view.freemarker.console;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

import freemarker.template.*;

/**
 * {@link PageCapture}'s merge-aware setters: single-value conflict fields and union fields.
 *
 * @since 10.0.0
 */
class PageCapture_Merge_Test extends TestBase {

	@Test void a01_mergeTab_blankInputIsNoOp() throws TemplateModelException {
		var cap = new PageCapture();
		cap.mergeTab("");
		assertEquals("", cap.tab());
	}

	@Test void a02_mergeTab_firstWriterWins() throws TemplateModelException {
		var cap = new PageCapture();
		cap.mergeTab("slo");
		assertEquals("slo", cap.tab());
	}

	@Test void a03_mergeTab_sameValueTwiceRejected() throws TemplateModelException {
		var cap = new PageCapture();
		cap.mergeTab("slo");
		var ex = assertThrows(TemplateModelException.class, () -> cap.mergeTab("slo"));
		assertEquals("PageSpec sets tab='slo' and <@page tab='slo'> also sets it; set it in one place.", ex.getMessage());
	}

	@Test void a04_mergeTab_conflictRejected() throws TemplateModelException {
		var cap = new PageCapture();
		cap.mergeTab("slo");
		var ex = assertThrows(TemplateModelException.class, () -> cap.mergeTab("activity"));
		assertEquals("PageSpec sets tab='slo' and <@page tab='activity'> also sets it; set it in one place.", ex.getMessage());
	}

	@Test void a05_mergeTheme_conflictRejected() throws TemplateModelException {
		var cap = new PageCapture();
		cap.theme("light-red");  // As a page spec seeds it.
		var ex = assertThrows(TemplateModelException.class, () -> cap.mergeTheme("open"));
		assertEquals("PageSpec theme 'light-red' conflicts with <@theme name='open'>; remove name= from <@theme> to inherit the page theme.", ex.getMessage());
	}

	@Test void a05b_mergeTheme_twoDirectivesDiffer_rejectedWithOwnMessage() throws TemplateModelException {
		var cap = new PageCapture();
		cap.mergeTheme("light-red");
		var ex = assertThrows(TemplateModelException.class, () -> cap.mergeTheme("open"));
		assertEquals("<@theme name='open'> conflicts with an earlier <@theme name='light-red'> on this page; use one.", ex.getMessage());
	}

	@Test void a06_mergeTheme_sameValueTwiceIsNoOp() throws TemplateModelException {
		var cap = new PageCapture();
		cap.mergeTheme("light-red");
		cap.mergeTheme("light-red");
		assertDoesNotThrow(() -> cap.mergeTheme("light-red"));
	}

	@Test void a07_mergeBodyAttrs_unionsDisjointNames() throws TemplateModelException {
		var cap = new PageCapture();
		cap.mergeBodyAttrs("data-foo=\"1\"");
		cap.mergeBodyAttrs("data-bar=\"2\"");
		assertEquals("data-foo=\"1\" data-bar=\"2\"", cap.bodyAttrs);
	}

	@Test void a07b_mergeBodyAttrs_sameNameRejected() throws TemplateModelException {
		var cap = new PageCapture();
		cap.mergeBodyAttrs("data-foo=\"1\"");
		var ex = assertThrows(TemplateModelException.class, () -> cap.mergeBodyAttrs("data-foo=\"2\""));
		assertEquals("PageSpec body attribute 'data-foo' is also set by <@body>; set it in one place.", ex.getMessage());
	}

	@Test void a08_mergeToolkit_unionPreservesOrderAndDedupes() {
		var cap = new PageCapture();
		cap.toolkit(List.of("views"));
		cap.mergeToolkit(List.of("views", "cmd-k"));
		assertEquals(List.of("views", "cmd-k"), cap.toolkits());
	}

	@Test void a09_mergeCss_appendsOnlyNewEntries() {
		var cap = new PageCapture();
		cap.css(List.of("/css/a.css"));
		cap.mergeCss(List.of("/css/a.css", "/css/b.css"));
		assertEquals(List.of("/css/a.css", "/css/b.css"), cap.cssHrefs());
	}

	@Test void a10_mergeInit_appendsOnlyNewEntries() {
		var cap = new PageCapture();
		cap.init(List.of("/js/a.js"));
		cap.mergeInit(List.of("/js/b.js"));
		assertEquals(List.of("/js/a.js", "/js/b.js"), cap.initScripts());
	}

	@Test void a11_mergeTheme_blankIsNoOp() throws TemplateModelException {
		var cap = new PageCapture();
		cap.mergeTheme("light-red");
		cap.mergeTheme("");
		cap.mergeTheme(null);
		assertDoesNotThrow(() -> cap.mergeTheme("light-red"));
	}

	@Test void a12_mergeBodyAttrs_blankIsNoOp() throws TemplateModelException {
		var cap = new PageCapture();
		cap.mergeBodyAttrs("");
		cap.mergeBodyAttrs("data-foo=\"1\"");
		cap.mergeBodyAttrs(" ");
		assertEquals("data-foo=\"1\"", cap.bodyAttrs);
	}

	@Test void a13_mergeBodyAttrs_sameNameSameValue_stillRejected() throws TemplateModelException {
		var cap = new PageCapture();
		cap.mergeBodyAttrs("data-foo=\"1\"");
		assertThrows(TemplateModelException.class, () -> cap.mergeBodyAttrs("data-foo=\"1\""));
	}

	@Test void a14_mergeToolkit_sameListTwiceIsIdempotent() {
		var cap = new PageCapture();
		cap.mergeToolkit(List.of("views", "cmd-k"));
		cap.mergeToolkit(List.of("views", "cmd-k"));
		assertEquals(List.of("views", "cmd-k"), cap.toolkits());
	}

	@Test void a15_mergeCss_intoEmptyList() {
		var cap = new PageCapture();
		cap.mergeCss(List.of("/css/a.css"));
		assertEquals(List.of("/css/a.css"), cap.cssHrefs());
	}

	@Test void a16_mergeInit_intoEmptyList() {
		var cap = new PageCapture();
		cap.mergeInit(List.of("/js/a.js", "/js/a.js"));
		assertEquals(List.of("/js/a.js"), cap.initScripts());
	}
}
