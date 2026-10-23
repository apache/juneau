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

import static org.apache.juneau.rest.server.views.ConsoleBrowserFixture.*;
import static org.apache.juneau.test.bct.BctAssertions.assertSize;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.console.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * The header color-scheme control: it renders only when the page opts in, sits between the header actions and the user
 * menu, stores the visitor's choice, flips the root attribute with exactly one change event, supports the radio-group
 * keyboard model, and follows OS and cross-tab changes only where the precedence allows.
 */
@SuppressWarnings({
	"unchecked" // The probe report is parsed into raw Maps and Lists.
})
@EnabledIfSystemProperty(named=ConsoleBrowserFixture.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class ConsoleScheme_BrowserTest extends TestBase {

	private static final String KEY = "juneau-console:scheme";
	private static final String TOGGLE = "data-jc-scheme-toggle";

	private static final String SIMPLE = """
		{"contractVersion":"1","title":"S","header":{"title":"T","links":[{"id":"l","label":"Docs","href":"/docs"}]},"cards":[]}
		""";

	private static final String FULL = """
		{"contractVersion":"1","title":"S","header":{"title":"T","links":[{"id":"l","label":"Docs","href":"/docs"}],
		 "userMenu":{"label":"Pat","initials":"P","items":[{"id":"o","label":"Out","href":"/out"}]},
		 "slots":{"actions":"header.actions"}},"cards":[]}
		""";

	private static final String FULL_TEMPLATES = """
		<template data-slot="header.actions"><button type="button" class="probe-action">A</button></template>
		""";

	private static final String TOGGLE_ONLY = """
		{"contractVersion":"1","title":"S","header":{"title":"T"},"cards":[]}
		""";

	private static final String NO_HEADER = """
		{"contractVersion":"1","title":"S","cards":[]}
		""";

	private static final String REPLACE = """
		{"contractVersion":"1","title":"S","header":{"slots":{"replace":"header.replace"}},"cards":[]}
		""";

	private static final String REPLACE_TEMPLATES = """
		<template data-slot="header.replace"><header class="my-header">WHOLE</header></template>
		""";

	/** Records every change event from before the shell runs, so events fired while the page loads are seen too. */
	private static final String EVENTS = """
		<script>
		window.__events = [];
		document.addEventListener('juneau:theme-changed', function (e) { window.__events.push(e.detail.scheme); });
		</script>
		""";

	/** Publishes the page state lazily, so the probe reads it after any clicks and steps. */
	private static final String PROBE = """
		<script>
		window.__probe = { toJSON: function () {
			var h = document.documentElement, t = document.querySelector('.jc-scheme-toggle'), out = {};
			out.scheme = h.getAttribute('data-jc-scheme');
			out.events = window.__events.slice();
			try { out.stored = localStorage.getItem('juneau-console:scheme'); } catch (x) { out.stored = 'ERR'; }
			out.apiOk = window.__apiOk === true;
			out.hasToggle = !!t;
			if (t) {
				out.role = t.getAttribute('role');
				out.label = t.getAttribute('aria-label');
				out.buttons = Array.prototype.map.call(t.querySelectorAll('button[role=radio]'), function (b) {
					return { scheme: b.getAttribute('data-scheme'), checked: b.getAttribute('aria-checked'), tab: b.getAttribute('tabindex'), title: b.getAttribute('title') };
				});
				var f = document.activeElement;
				out.focused = f ? f.getAttribute('data-scheme') : null;
				var on = t.querySelector('[aria-checked=true]');
				if (on) {
					var cs = getComputedStyle(on), p = document.createElement('i');
					document.body.appendChild(p);
					var tok = function (prop, name) { p.style[prop] = 'var(' + name + ')'; return getComputedStyle(p)[prop]; };
					out.style = { bg: cs.backgroundColor, wash: tok('backgroundColor', '--jc-accent-wash'), color: cs.color, text: tok('color', '--jc-text'),
						border: cs.borderBottomColor, accent: tok('color', '--jc-accent') };
					p.remove();
				}
			}
			var ha = document.querySelector('.jc-header-actions');
			out.actions = ha ? Array.prototype.map.call(ha.children, function (c) { return c.className.split(' ')[0] || c.tagName.toLowerCase(); }) : null;
			return out;
		} };
		</script>
		""";

	private static final String BADGE = """
		<script>
		var ha = document.querySelector('.jc-header-actions');
		if (ha) { var b = document.createElement('div'); b.className = 'jc-header-badges'; ha.appendChild(b); }
		</script>
		""";

	private static Map<String,Map<String,Object>> report;

	private static String page(String json, String templates, String attrs, String after) {
		return contractPage(json, templates, EVENTS, after + PROBE, attrs, true);
	}

	/** A toggle page whose head carries no scheme script, as when a CSP blocks it. */
	private static String pageWithoutHeadScript(String json, String attrs) {
		var withScript = page(json, attrs);
		var without = withScript.replace("<script>" + ConsoleChromeMixin.SCHEME_SCRIPT + "</script>", "");
		assertNotEquals(withScript, without, "the fixture no longer emits the head script");
		return without;
	}

	private static String page(String json, String attrs) {
		return page(json, "", attrs, "");
	}

	@BeforeAll
	static void runPages() throws Exception {
		report = ConsoleBrowserFixture.create("scheme")
			.page("plain", ORIGIN + "/plain", page(SIMPLE, ""))
			.page("render", ORIGIN + "/render", page(SIMPLE, TOGGLE))
			.page("order", ORIGIN + "/order", page(FULL, FULL_TEMPLATES, TOGGLE, BADGE))
			.page("only", ORIGIN + "/only", page(TOGGLE_ONLY, TOGGLE))
			.page("clickDark", ORIGIN + "/clickDark", page(SIMPLE, TOGGLE)).click("[data-scheme=dark]")
			.page("clickDarkTwice", ORIGIN + "/clickDarkTwice", page(SIMPLE, TOGGLE)).click("[data-scheme=dark]").click("[data-scheme=dark]")
			.page("autoClears", ORIGIN + "/autoClears", page(SIMPLE, TOGGLE)).storage(Map.of(KEY, "dark")).colorScheme("light").click("[data-scheme=auto]")
			.page("pinned", ORIGIN + "/pinned", page(SIMPLE, "data-jc-scheme=\"dark\" data-jc-scheme-pin=\"dark\" " + TOGGLE))
			.page("keyRight", ORIGIN + "/keyRight", page(SIMPLE, TOGGLE)).click("[data-scheme=auto]").press("ArrowRight")
			.page("keyEnd", ORIGIN + "/keyEnd", page(SIMPLE, TOGGLE)).click("[data-scheme=auto]").press("End")
			.page("keyHome", ORIGIN + "/keyHome", page(SIMPLE, TOGGLE)).click("[data-scheme=dark]").press("Home")
			.page("osAuto", ORIGIN + "/osAuto", page(SIMPLE, "")).colorScheme("light").emulate("dark")
			.page("osPinned", ORIGIN + "/osPinned", page(SIMPLE, "data-jc-scheme=\"light\" data-jc-scheme-pin=\"light\"")).colorScheme("light").emulate("dark")
			.page("osStored", ORIGIN + "/osStored", page(SIMPLE, TOGGLE)).storage(Map.of(KEY, "light")).colorScheme("light").emulate("dark")
			.page("storageToggle", ORIGIN + "/storageToggle", page(SIMPLE, TOGGLE)).colorScheme("light")
				.eval("window.dispatchEvent(new StorageEvent('storage', {key: '" + KEY + "', newValue: 'dark'}))")
			.page("storagePlain", ORIGIN + "/storagePlain", page(SIMPLE, "")).colorScheme("light")
				.eval("window.dispatchEvent(new StorageEvent('storage', {key: '" + KEY + "', newValue: 'dark'}))")
			.page("noHead", ORIGIN + "/noHead", pageWithoutHeadScript(SIMPLE, TOGGLE)).storage(Map.of(KEY, "dark")).colorScheme("light")
			.page("noHeader", ORIGIN + "/noHeader", pageWithoutHeadScript(NO_HEADER, "data-jc-scheme=\"light\" " + TOGGLE)).storage(Map.of(KEY, "dark")).colorScheme("light")
			.page("api", ORIGIN + "/api", page(REPLACE, REPLACE_TEMPLATES, TOGGLE,
				"<script>var c = JuneauConsole.schemeControl(document); document.body.appendChild(c); window.__apiOk = c.matches('.jc-scheme-toggle[role=radiogroup]');</script>"))
				.click("[data-scheme=dark]")
			.run();
	}

	private static Map<String,Object> p(String name) {
		var r = report.get(name);
		assertNoShellErrors(r);
		return probe(r);
	}

	private static List<Map<String,Object>> buttons(Map<String,Object> pr) {
		return (List<Map<String,Object>>)pr.get("buttons");
	}

	private static String checked(Map<String,Object> pr) {
		var on = buttons(pr).stream().filter(b -> "true".equals(b.get("checked"))).toList();
		assertSize(() -> "exactly one checked: " + pr, 1, on);
		assertEquals("0", on.get(0).get("tab"));
		assertEquals(1, buttons(pr).stream().filter(b -> "0".equals(b.get("tab"))).count());
		return (String)on.get(0).get("scheme");
	}

	@Test void a01_noToggleWithoutAttr() {
		assertEquals(false, p("plain").get("hasToggle"));
		assertEquals(List.of(), p("plain").get("events"), "nothing is announced at load");
	}

	@Test void a02_toggleRenders() {
		var pr = p("render");
		assertEquals(true, pr.get("hasToggle"));
		assertEquals("radiogroup", pr.get("role"));
		assertEquals("Color scheme", pr.get("label"));
		assertEquals(List.of("auto", "light", "dark"), buttons(pr).stream().map(b -> b.get("scheme")).toList());
		assertEquals("auto", checked(pr));
		assertEquals(List.of(), pr.get("events"), "nothing is announced at load");
		assertEquals(List.of(), p("order").get("events"));
		assertEquals(List.of(), p("only").get("events"));
	}

	@Test void a03_order() {
		assertEquals(List.of("jc-header-link", "probe-action", "jc-scheme-toggle", "jc-user-menu", "jc-header-badges"), p("order").get("actions"));
	}

	@Test void a04_toggleOnlyCreatesActions() {
		var pr = p("only");
		assertEquals(List.of("jc-scheme-toggle"), pr.get("actions"));
	}

	@Test void a05_clickDark() {
		var pr = p("clickDark");
		assertEquals("dark", pr.get("stored"));
		assertEquals("dark", pr.get("scheme"));
		assertEquals(List.of("dark"), pr.get("events"));
		assertEquals("dark", checked(pr));
		assertEquals(List.of("dark"), p("clickDarkTwice").get("events"), "a repeat click is not a change");
	}

	@Test void a06_autoClears() {
		var pr = p("autoClears");
		assertNull(pr.get("stored"));
		assertEquals("light", pr.get("scheme"));
		assertEquals(List.of("light"), pr.get("events"));
		assertEquals("auto", checked(pr));
	}

	@Test void a07_pinnedAutoTitle() {
		var pr = p("pinned");
		var auto = buttons(pr).get(0);
		assertEquals("Follow app default", auto.get("title"));
		assertEquals("dark", pr.get("scheme"));
		assertEquals(List.of(), pr.get("events"), "a pinned page already agrees with its head script");
		assertEquals("Follow system", buttons(p("render")).get(0).get("title"));
	}

	@Test void a08_keyboard() {
		var right = p("keyRight");
		assertEquals("light", right.get("focused"));
		assertEquals("light", checked(right));
		var end = p("keyEnd");
		assertEquals("dark", end.get("focused"));
		assertEquals("dark", checked(end));
		var home = p("keyHome");
		assertEquals("auto", checked(home));
		assertNull(home.get("stored"));
	}

	@Test void a09_osChangeInAuto() {
		var auto = p("osAuto");
		assertEquals("dark", auto.get("scheme"));
		assertEquals(List.of("dark"), auto.get("events"));
		assertEquals(List.of(), p("osPinned").get("events"));
		assertEquals("light", p("osPinned").get("scheme"));
		assertEquals(List.of(), p("osStored").get("events"));
		assertEquals("light", p("osStored").get("scheme"));
	}

	@Test void a10_storageEventFollowed() {
		var on = p("storageToggle");
		assertEquals("dark", on.get("scheme"));
		assertEquals(List.of("dark"), on.get("events"));
		assertEquals("light", p("storagePlain").get("scheme"));
		assertEquals(List.of(), p("storagePlain").get("events"));
	}

	@Test void a11_schemeControlApi() {
		var pr = p("api");
		assertEquals(true, pr.get("hasToggle"));
		assertEquals("dark", pr.get("scheme"), "the API control is wired: clicking Dark applies it");
		assertEquals("dark", pr.get("stored"));
		assertEquals(true, pr.get("apiOk"));
	}

	@Test void a12_checkedStyle() {
		var s = (Map<String,Object>)p("render").get("style");
		assertEquals(s.get("wash"), s.get("bg"));
		assertEquals(s.get("text"), s.get("color"));
		assertEquals(s.get("accent"), s.get("border"));
	}

	@Test void a13_scriptlessPageCatchesUp() {
		var pr = p("noHead");
		assertEquals("dark", pr.get("scheme"), "without the head script the control applies the stored choice");
		assertEquals(List.of("dark"), pr.get("events"));
		assertEquals("dark", checked(pr));
	}

	@Test void a14_headerlessPageCatchesUp() {
		var pr = p("noHeader");
		assertEquals(false, pr.get("hasToggle"));
		assertEquals("dark", pr.get("scheme"), "the shell applies the stored choice even when it renders no control");
		assertEquals(List.of("dark"), pr.get("events"));
	}
}
