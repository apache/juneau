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
package org.apache.juneau.marshall.html;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.svl.*;
import org.junit.jupiter.api.*;

/**
 * Tests {@link HtmlWidgetVar} resolution of <js>"$W{...}"</js> variables.
 *
 * <p>
 * Covers the defensive per-widget degradation contract: a widget whose {@link HtmlWidget#getHtml(VarResolverSession)}
 * throws must not propagate out of var resolution (which would abort the whole enclosing string and blank the page).
 * Instead it degrades to an empty fragment while surrounding text and unregistered-widget placeholders are unaffected.
 */
class HtmlWidgetVar_Test extends TestBase {

	private static VarResolverSession session(HtmlWidget...widgets) {
		var vr = VarResolver.create().vars(HtmlWidgetVar.class).build();
		var s = vr.createSession();
		s.bean(HtmlWidgetMap.class, new HtmlWidgetMap().append(widgets));
		return s;
	}

	private static HtmlWidget widget(String name, String html) {
		return new HtmlWidget() {
			@Override public String getName() { return name; }
			@Override public String getHtml(VarResolverSession session) { return html; }
			@Override public String getScript(VarResolverSession session) { return null; }
			@Override public String getStyle(VarResolverSession session) { return null; }
		};
	}

	private static HtmlWidget throwingWidget(String name) {
		return new HtmlWidget() {
			@Override public String getName() { return name; }
			@Override public String getHtml(VarResolverSession session) { throw new RuntimeException("boom"); }
			@Override public String getScript(VarResolverSession session) { return null; }
			@Override public String getStyle(VarResolverSession session) { return null; }
		};
	}

	@Test void a01_resolvesRegisteredWidget() {
		var s = session(widget("greeting", "hello"));
		assertEquals("before hello after", s.resolve("before $W{greeting} after"));
	}

	@Test void a02_unregisteredWidgetReturnsPlaceholder() {
		var s = session(widget("greeting", "hello"));
		assertEquals("before unknown-widget-missing after", s.resolve("before $W{missing} after"));
	}

	@Test void a03_throwingWidgetDegradesToEmptyAndDoesNotPropagate() {
		var s = session(throwingWidget("boom"));
		// The failing widget must degrade to empty rather than throw, and surrounding text must still render.
		assertDoesNotThrow(() -> s.resolve("before $W{boom} after"));
		assertEquals("before  after", s.resolve("before $W{boom} after"));
	}

	@Test void a04_throwingWidgetDoesNotAffectSiblingWidgets() {
		var s = session(throwingWidget("boom"), widget("ok", "OK"));
		assertEquals("[] [OK]", s.resolve("[$W{boom}] [$W{ok}]"));
	}
}
