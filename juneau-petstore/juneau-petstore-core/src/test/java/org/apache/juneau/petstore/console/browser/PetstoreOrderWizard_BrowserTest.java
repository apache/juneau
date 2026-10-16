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
package org.apache.juneau.petstore.console.browser;

import static org.apache.juneau.petstore.console.browser.PetstoreBrowser.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.microservice.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.junit.jupiter.api.extension.*;

/**
 * The Orders "+ New order" wizard in the real browser: the ribbon item opens the compact first step, its Review
 * button stacks the second step with the first step's inputs, and that step's Confirm places the order (it posts to
 * the endpoint and method its child action declares), closes both steps, shows the success banner under the
 * ribbon and redraws the table.
 */
@EnabledIfSystemProperty(named=PetstoreBrowser.GATE, matches="true", disabledReason=PetstoreBrowser.DISABLED)
class PetstoreOrderWizard_BrowserTest extends TestBase {

	@RegisterExtension
	static MicroserviceTestFixture server = PetstoreTestServer.fixture();

	private static final String TOP = "(() => { const d = document.querySelectorAll('[role=dialog]'); return d[d.length - 1]; })()";

	private static final String NEW_ORDER = "[...document.querySelectorAll('.juneau-view-ribbon-btn')].find(b => b.getAttribute('aria-label') === '+ New order')";

	private static Map<String,Object> wait(String js) {
		return Map.of("waitFor", js, "timeoutMs", 5000);
	}

	private static Map<String,Object> eval(String js) {
		return Map.of("evaluate", js);
	}

	private static final String FIELD = "(n => document.querySelector('[data-juneau-form-field=' + n + ']'))";

	private static final String ROWS = "document.querySelectorAll('table tbody tr').length";

	/** The table's total row count, read from its "1-25 of 2,001" summary. */
	private static final String TOTAL = "Number(/of (\\d+(?:,\\d{3})*)/.exec(document.body.textContent)[1].replace(/,/g, ''))";

	private static Map<String,Map<String,Object>> report;

	/** One full pass through the wizard at the given viewport, shipping on the given date. */
	private static Map<String,Object> wizard(String name, List<Integer> viewport, String shipDate, String shown) {
		return Map.of("name", name, "path", "/console/orders", "capture", "/console/orders", "viewport", viewport,
			"actions", List.of(
				wait(NEW_ORDER + " && " + ROWS + " > 0"),
				eval("window.__totalBefore = " + TOTAL),
				eval(NEW_ORDER + ".click()"),
				wait("document.querySelector('[data-juneau-form-field=review]')"),
				eval("window.__step1 = " + TOP + ".querySelector('h2, [class*=title]').textContent"),
				// Choose the second available pet and a ship date, as a user would.
				eval("(() => { const f = " + FIELD + "('petId'); f.selectedIndex = 1; window.__petId = f.value; window.__petName = f.options[1].textContent;"
					+ " f.dispatchEvent(new Event('input', {bubbles: true})); f.dispatchEvent(new Event('change', {bubbles: true}));"
					+ " const d = " + FIELD + "('shipDate'); d.value = '" + shipDate + "';"
					+ " d.dispatchEvent(new Event('input', {bubbles: true})); d.dispatchEvent(new Event('change', {bubbles: true})); })()"),
				eval(FIELD + "('review').click()"),
				wait("[...document.querySelectorAll('[role=dialog]')].length > 1 && " + TOP + ".querySelector('.juneau-view-dialog-confirm')"),
				eval("window.__step2 = " + TOP + ".textContent"),
				eval("window.__dialogs = document.querySelectorAll('[role=dialog]').length"),
				eval("window.__compact = document.querySelectorAll('.petstore-dialog-compact').length"),
				// The stacked step sits inside the viewport, not spilling off a narrow screen.
				eval("window.__rect = JSON.stringify(" + TOP + ".getBoundingClientRect()) + ' vs ' + window.innerWidth + 'x' + window.innerHeight"),
				eval("window.__offset = " + TOP + ".getBoundingClientRect().left - document.querySelectorAll('[role=dialog]')[0].getBoundingClientRect().left"),
				eval("window.__fits = (r => r.left >= 0 && r.right <= window.innerWidth + 1 && r.top >= 0 && r.bottom <= window.innerHeight + 1)(" + TOP + ".getBoundingClientRect())"),
				eval("window.__fontPx = parseFloat(getComputedStyle(document.querySelectorAll('[role=dialog]')[0]).fontSize)"),
				eval("window.__innerWidth = window.innerWidth"),
				eval("window.__firstRect = JSON.stringify(document.querySelectorAll('[role=dialog]')[0].getBoundingClientRect())"),
				eval("window.__firstFits = (r => r.left >= 0 && r.right <= window.innerWidth + 1 && r.top >= 0 && r.bottom <= window.innerHeight + 1)(document.querySelectorAll('[role=dialog]')[0].getBoundingClientRect())"),
				eval("window.__firstWidth = document.querySelectorAll('[role=dialog]')[0].getBoundingClientRect().width"),
				eval(TOP + ".querySelector('.juneau-view-dialog-confirm').click()"),
				wait("document.querySelector('.juneau-view-ribbon-action-outcome[data-state=success]') && " + TOTAL + " === window.__totalBefore + 1"),
				// Sort by id, newest first, so the placed order is the first row.  With the View Settings gear the sort controls are Juneau's .juneau-view-col-sort-icon; the Id column is first.
				eval("window.__orderId = /Placed order '(\\d+)'/.exec(document.body.textContent)[1]"),
				eval("document.querySelector('th .juneau-view-col-sort-icon').click()"),
				wait("document.querySelector('table tbody tr td')?.textContent.trim() === window.__orderId")),
			"queries", Map.ofEntries(
				Map.entry("step1", "window.__step1"), Map.entry("step2", "window.__step2"), Map.entry("compact", "window.__compact"),
				Map.entry("dialogs", "window.__dialogs"), Map.entry("fits", "window.__fits"), Map.entry("firstFits", "window.__firstFits"), Map.entry("firstWidth", "window.__firstWidth"), Map.entry("fontPx", "window.__fontPx"), Map.entry("innerWidth", "window.__innerWidth"), Map.entry("firstRect", "window.__firstRect"), Map.entry("rect", "window.__rect"), Map.entry("offset", "window.__offset"), Map.entry("petId", "window.__petId"),
				Map.entry("petName", "window.__petName"), Map.entry("totalBefore", "window.__totalBefore"), Map.entry("totalAfter", TOTAL),
				Map.entry("firstRow", "document.querySelector('table tbody tr').textContent"), Map.entry("orderId", "window.__orderId"),
				Map.entry("backdrops", "document.querySelectorAll('.juneau-view-dialog-backdrop').length"),
				Map.entry("banner", "document.querySelector('.juneau-view-ribbon-action-outcome[data-state=success]')?.textContent ?? null"),
				Map.entry("all", "document.body.textContent")));
	}

	@BeforeAll
	static void probe() throws Exception {
		var base = PetstoreTestServer.awaitReady(server);
		report = run("order-wizard", base, List.of(
			wizard("wide", List.of(1280, 900), "2031-03-14", "Mar 14, 2031"),
			wizard("narrow", List.of(420, 800), "2032-04-15", "Apr 15, 2032")));
	}

	/** The order this places is left in the throwaway test server's store; nothing else reads it. */
	@SuppressWarnings("unchecked") // requests is a JSON array of {url, method, postData}.
	private static void verify(String name, String shipDate, String shown) {
		var c = assertClean(report, name);
		assertContains("New order", String.valueOf(query(c, "step1")));
		// The second step reads back the first step's inputs: the chosen pet and the ship date.
		var step2 = String.valueOf(query(c, "step2"));
		assertContains("Review order", step2);
		assertContains(String.valueOf(query(c, "petName")), step2);
		assertContains(shipDate, step2);
		assertEquals(2, ((Number)query(c, "dialogs")).intValue(), "both steps are stacked");
		assertTrue((Integer)query(c, "compact") > 0);
		assertTrue(((Number)query(c, "offset")).doubleValue() > 0, "the second step is offset from the first, so the two read as a stack");
		assertEquals(Boolean.TRUE, query(c, "fits"), () -> "the stacked step must fit the viewport: " + query(c, "rect"));
		assertEquals(Boolean.TRUE, query(c, "firstFits"), () -> "the compact first step must fit the viewport: " + query(c, "firstRect"));
		var em = ((Number)query(c, "fontPx")).doubleValue();
		var width = ((Number)query(c, "firstWidth")).doubleValue();
		var offset = ((Number)query(c, "offset")).doubleValue();
		assertEquals(1.5 * em, offset, 1.0, "the stack offset is one 1.5em shift");
		if ("wide".equals(name))
			assertTrue(width >= 28 * em - 1, "wide geometry keeps the 28em minimum width: " + width);
		else
			assertEquals(((Number)query(c, "innerWidth")).doubleValue() - 3 * em, width, 2.0, "narrow: the compact step fills the viewport less the 3em slack");
		var posted = ((List<Map<String,Object>>)c.get("requests")).stream()
			.filter(r -> "POST".equals(r.get("method")) && String.valueOf(r.get("url")).endsWith("/console/orders")).toList();
		assertEquals(1, posted.size());
		// A successful submit redraws the table, which re-queries it after the order was posted.
		var urls = ((List<Map<String,Object>>)c.get("requests")).stream().filter(r -> "POST".equals(r.get("method"))).map(r -> String.valueOf(r.get("url"))).toList();
		assertTrue(urls.lastIndexOf(posted.get(0).get("url").toString() + "/query") > urls.indexOf(posted.get(0).get("url").toString()));
		var body = new org.apache.juneau.marshall.collections.JsonMap(String.valueOf(posted.get(0).get("postData")));
		assertEquals(String.valueOf(query(c, "petId")), body.getMap("fields").getString("petId"));
		assertEquals(shipDate, body.getMap("fields").getString("shipDate"));
		// The committed write closes the whole stack - the first step too, not just the one that submitted - and
		// its outcome is the banner under the ribbon.
		assertEquals(0, ((Number)query(c, "backdrops")).intValue());
		assertContains("Placed order", String.valueOf(query(c, "banner")));
		// The new order is in the redrawn table: the total grew by one and the newest row is that order, still placed
		// and carrying the chosen pet and ship date.
		assertEquals(((Number)query(c, "totalBefore")).intValue() + 1, ((Number)query(c, "totalAfter")).intValue());
		var row = String.valueOf(query(c, "firstRow"));
		assertTrue(row.startsWith(String.valueOf(query(c, "orderId")) + query(c, "petId")), row);
		// A date-only value renders as the calendar day it names, in any browser time zone.
		assertTrue(row.matches(".*" + shown + ".*"), row);
		assertTrue(row.endsWith("PLACED"), row);
	}

	@Test void a01_wideViewport() {
		verify("wide", "2031-03-14", "Mar 14, 2031");
	}

	@Test void a02_narrowViewport() {
		verify("narrow", "2032-04-15", "Apr 15, 2032");
	}
}
