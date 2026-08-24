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
package org.apache.juneau.rest.server.views;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.widgets.*;
import org.junit.jupiter.api.*;

/**
 * Pins the card-layout CSS contract in {@code juneau-views.css}: the {@code --jc-card-*} design tokens, the
 * responsive auto-fill grid rule (whose {@code minmax()} floor reads {@code --jc-card-min}, overridden inline
 * per-grid by the emitter from {@link CardGrid#minCardPx}), the color-free error-status rule, and &mdash; the
 * anti-drift part &mdash; that every {@code juneau-view-card*} class the {@link CardGridTable} emitter stamps has a
 * matching CSS rule.
 */
class CardGridCss_Test extends TestBase {

	private static String css() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.VIEWS_CSS_RESOURCE)) {
			assertNotNull(in, () -> "missing classpath resource: " + ViewsMixin.VIEWS_CSS_RESOURCE);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	@Test void a01_designTokensPresent() throws Exception {
		var c = css();
		for (var token : new String[]{
			"--jc-card-bg", "--jc-card-border", "--jc-card-radius", "--jc-card-gap", "--jc-card-min", "--jc-card-shadow"
		})
			assertTrue(c.contains(token + ":"), () -> "missing card design token: " + token);
	}

	@Test void a02_noStatusErrorColorToken() throws Exception {
		// The error status is distinguished by a heavier border + bold weight, not a color token, so it reads in any theme.
		assertFalse(css().contains("--jc-card-status-error:"), "there must be no --jc-card-status-error color token");
	}

	@Test void a03_responsiveAutoFillGrid() throws Exception {
		var c = css();
		assertTrue(c.contains(".juneau-view-card-grid"), c);
		assertTrue(c.replaceAll("\\s+", " ").contains("grid-template-columns: repeat(auto-fill, minmax(var(--jc-card-min"),
			"grid must be a responsive auto-fill minmax() reading --jc-card-min");
	}

	@Test void a04_errorStatusRuleIsColorFree() throws Exception {
		var c = css();
		assertTrue(c.contains("[data-juneau-card-status][data-state=\"error\"]"),
			"an error-state rule keyed off the emitter's status attribute must exist");
	}

	@Test void a05_everyEmittedClassHasACssRule() throws Exception {
		// Anti-drift: render a representative grid, harvest its juneau-view-card* classes, and assert each has a rule.
		var grid = CardGrid.create("g1").title("Dashboard").minCardPx(320).cards(
			Card.create("c1", "Static").body(CardFieldList.create().columns(2).fields(
				CardField.of("name", "Name", "Widget A"))),
			Card.create("c2", "Live").body(CardFieldList.create().fields(
				CardField.of("k", "Label", "v")).refresh("/data/summary").pollIntervalMs(10_000)));
		var html = Html.of(CardGridTable.of(grid));
		var css = css();
		for (var cls : new String[]{
			"juneau-view-card-grid", "juneau-view-card-grid-title", "juneau-view-card", "juneau-view-card-header",
			"juneau-view-card-title", "juneau-view-card-status", "juneau-view-card-actions", "juneau-view-card-refresh",
			"juneau-view-card-banner", "juneau-view-card-body", "juneau-view-card-fields", "juneau-view-card-field"
		}) {
			assertTrue(html.contains("\"" + cls + "\"") || html.contains(cls),
				() -> "expected emitter to stamp class: " + cls);
			assertTrue(css.contains("." + cls), () -> "emitted class has no CSS rule (drift): " + cls);
		}
	}
}
