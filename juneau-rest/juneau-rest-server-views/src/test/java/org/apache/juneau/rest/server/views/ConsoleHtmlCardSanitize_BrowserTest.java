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
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * An {@code html} card with {@code src} and {@code sanitize:true} paints the fetched fragment through the views
 * attribute-allowlist sanitizer: script, {@code on*} handlers, unsafe URLs and other unlisted attributes are dropped
 * while benign markup survives.  Without the flag the fragment is inserted as served.
 */
@EnabledIfSystemProperty(named=ConsoleBrowserFixture.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class ConsoleHtmlCardSanitize_BrowserTest extends TestBase {

	private static final String HOSTILE = """
		<script>window.__fired = 1;</script>\
		<img src="x" alt="pic" onerror="window.__fired = 1" onclick="window.__fired = 1" style="width:1px" class="c" id="evil">\
		<a href="javascript:window.__fired=1" onclick="window.__fired = 1">js-link</a>\
		<a href="data:text/html,x">data-link</a>\
		<a href="/ok" target="_blank" onmouseover="window.__fired = 1" title="t">ok-link</a>\
		<iframe src="/frame"></iframe>\
		<b>survivor</b>\
		<table><tr><td colspan="2" onclick="window.__fired = 1">cell</td></tr></table>
		""";

	private static final String BENIGN = "<p>Hello <b>world</b></p><table><tr><td>1</td><td>2</td></tr></table><a href=\"/ok\">go</a>";

	// A stray end tag must not close the sanitizer's wrapper and swallow what follows it.
	private static final String STRAY = "<p>one</p></div></body></html><p>two</p><b>after</b>";

	private static final String CONTRACT_TRUE = card("\"sanitize\":true");
	private static final String CONTRACT_ABSENT = card("\"bare\":false");
	private static final String CONTRACT_FALSE = card("\"sanitize\":false");

	private static String card(String extra) {
		return "{\"contractVersion\":\"1\",\"title\":\"S\",\"cards\":[{\"id\":\"h\",\"type\":\"html\",\"title\":\"Frag\",\"src\":\"/frag\","
			+ extra + "}]}";
	}

	// console-shell.cjs reads window.__probe when the case is probed; a getter inspects the painted card at that moment.
	private static final String PROBE = """
		<script>
		Object.defineProperty(window, '__probe', { configurable: true, get: function () {
			var host = document.getElementById('h');
			var els = host ? Array.prototype.slice.call(host.querySelectorAll('*')).filter(function (e) { return !e.classList.contains('jc-card-title'); }) : [];
			return {
				fired: window.__fired || 0,
				tags: els.map(function (e) { return e.tagName; }),
				attrs: els.map(function (e) { return e.tagName + ':' + Array.prototype.map.call(e.attributes, function (a) { return a.name; }).sort().join(','); }),
				hrefs: els.filter(function (e) { return e.tagName === 'A'; }).map(function (e) { return e.getAttribute('href'); }),
				title: host && host.querySelector('.jc-card-title') ? host.querySelector('.jc-card-title').textContent : null
			};
		} });
		</script>
		""";

	private static Map<String,Map<String,Object>> report;
	private static Map<String,Map<String,Object>> benignReport;
	private static Map<String,Map<String,Object>> lateReport;
	private static Map<String,Map<String,Object>> strayReport;

	@BeforeAll
	static void load() throws Exception {
		var views = viewsPack();
		report = ConsoleBrowserFixture.create("html-sanitize")
			.asset("/frag", HOSTILE, "text/html")
			.page("on", ORIGIN + "/on", contractPage(CONTRACT_TRUE, "", PROBE, views)).waitFor("#h b")
			.page("absent", ORIGIN + "/absent", contractPage(CONTRACT_ABSENT, "", PROBE, views)).waitFor("#h b")
			.page("false", ORIGIN + "/false", contractPage(CONTRACT_FALSE, "", PROBE, views)).waitFor("#h b")
			.page("on-no-views", ORIGIN + "/on-no-views", contractPage(CONTRACT_TRUE, "", PROBE, ""),
				"fragment", "#h b").waitFor("#h b, .jc-console-error-item")
			.run();
		benignReport = ConsoleBrowserFixture.create("html-sanitize-benign")
			.asset("/frag", BENIGN, "text/html")
			.page("on", ORIGIN + "/on", contractPage(CONTRACT_TRUE, "", PROBE, views)).waitFor("#h table")
			.run();
		// The views toolkit arrives as a slow external script, so the parser is still blocked (readyState "loading") when
		// the fragment fetch resolves: the sanitizer is not there yet, but will be by DOMContentLoaded.
		var lateViews = views.replace("<script>\n", "").replace("\n</script>\n", "\n;\n");
		lateReport = ConsoleBrowserFixture.create("html-sanitize-late")
			.asset("/frag", BENIGN, "text/html")
			.delayedAsset("/late-views.js", lateViews, "text/javascript", 1500)
			.page("on", ORIGIN + "/on", contractPage(CONTRACT_TRUE, "", PROBE, "<script src=\"/late-views.js\"></script>"))
				.waitFor("#h table, .jc-console-error-item")
			.run();
		strayReport = ConsoleBrowserFixture.create("html-sanitize-stray")
			.asset("/frag", STRAY, "text/html")
			.page("on", ORIGIN + "/on", contractPage(CONTRACT_TRUE, "", PROBE, views)).waitFor("#h p")
			.run();
	}

	@Test void a01_sanitizeTrue_dropsScriptHandlersAndUnsafeUrls() {
		var r = report.get("on");
		assertNoShellErrors(r);
		var p = probe(r);
		assertEquals(0, ((Number)p.get("fired")).intValue(), () -> "nothing executed: " + p);
		assertFalse(((List<?>)p.get("tags")).contains("SCRIPT"), () -> "script dropped: " + p);
		assertFalse(((List<?>)p.get("tags")).contains("IFRAME"), () -> "iframe dropped: " + p);
		for (var a : (List<?>)p.get("attrs"))
			assertFalse(String.valueOf(a).matches(".*\\b(on\\w+|style|class|id|target)\\b.*"), () -> "unlisted attribute survived: " + p);
		assertEquals(List.of("/ok"), ((List<?>)p.get("hrefs")).stream().filter(Objects::nonNull).toList(), () -> "only the safe href survives: " + p);
	}

	@Test void a02_sanitizeTrue_keepsBenignMarkupAndTheCardTitle() {
		var r = report.get("on");
		var p = probe(r);
		var tags = (List<?>)p.get("tags");
		assertTrue(tags.contains("B") && tags.contains("TABLE") && tags.contains("TD"), () -> "benign markup survives: " + p);
		assertTrue(((List<?>)p.get("attrs")).contains("TD:colspan"), () -> "allowlisted colspan survives: " + p);
		assertTrue(((List<?>)p.get("attrs")).contains("A:href,title"), () -> "allowlisted href/title survive: " + p);
		assertEquals("Frag", p.get("title"), () -> "sanitized paint must not clear the card title: " + p);
		var b = probe(benignReport.get("on"));
		assertEquals(List.of("P", "B", "TABLE", "TBODY", "TR", "TD", "TD", "A"), b.get("tags"), () -> b.toString());
	}

	@Test void a03_flagAbsent_fragmentIsInsertedAsServed() {
		for (var name : List.of("absent", "false")) {
			var r = report.get(name);
			assertNoShellErrors(r);
			var p = probe(r);
			assertTrue(((List<?>)p.get("tags")).contains("SCRIPT"), () -> name + ": script element kept: " + p);
			assertTrue(((List<?>)p.get("tags")).contains("IFRAME"), () -> name + ": iframe kept: " + p);
			assertTrue(((List<?>)p.get("attrs")).stream().anyMatch(a -> String.valueOf(a).startsWith("IMG:") && String.valueOf(a).contains("onerror")),
				() -> name + ": onerror kept: " + p);
		}
	}

	@Test void a04_sanitizeTrue_withoutViews_failsClosedWithE_JS_8() {
		var r = report.get("on-no-views");
		assertNull(queries(r).get("fragment"), () -> "nothing painted unsanitized: " + r);
		assertFailure(r, "E-JS-8", "card 'h' (type 'html') handler threw: 'html card sanitize:true needs JuneauViews; load the views toolkit'");
	}

	@Test void a05_viewsArrivingAfterTheFragment_stillPaintsSanitized() {
		var r = lateReport.get("on");
		assertNoShellErrors(r);
		var p = probe(r);
		assertEquals(List.of("P", "B", "TABLE", "TBODY", "TR", "TD", "TD", "A"), p.get("tags"), () -> "painted once views loaded: " + p);
	}

	@Test void a06_strayEndTag_doesNotDropTheRestOfTheFragment() {
		var r = strayReport.get("on");
		assertNoShellErrors(r);
		var p = probe(r);
		assertEquals(List.of("P", "P", "B"), p.get("tags"), () -> "everything after the stray end tag survives: " + p);
	}
}
