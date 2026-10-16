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

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.console.*;
import org.junit.jupiter.api.*;

/**
 * The dashboard runtime {@code juneau-dashboard.js} without a DOM: its registries and queue, the share-link codec and
 * validator, restore order, quick ranges, the status text and a seeded share-link fuzz.  Runs when node is on PATH.
 */
class ConsoleDashboard_Test extends TestBase {

	private static Map<?,?> report() {
		var r = NodeHarness.report("dashboard.cjs", ConsoleChromeMixin.DASHBOARD_JS_RESOURCE);
		assumeTrue(r != null, "node (or dashboard.cjs) not available - JS layer skipped");
		return r;
	}

	private static Map<?,?> part(String key) {
		var s = (Map<?,?>)report().get(key);
		assertNotNull(s, () -> "section '" + key + "' missing from the harness output");
		assertNull(s.get("crash"), () -> "section '" + key + "' crashed: " + s.get("crash"));
		return s;
	}

	private static final String E97 = "E-JS-97: JuneauDashboard.";

	@Test void a01_registry_namesAndReasons() {
		var r = part("registry");
		assertEquals(E97 + "section.register: name 'note' is bare", r.get("bare"));
		assertEquals(E97 + "section.register: name 'App.note' is not a valid name", r.get("invalid"));
		assertEquals(E97 + "decoder.register: name '7' is not a valid name", r.get("nonString"));
		assertEquals(E97 + "section.register: name 'app.empty' has no mount function", r.get("noMount"));
		assertEquals(E97 + "decoder.register: name 'app.dec' is not a function", r.get("notFn"));
		assertNull(r.get("ok"));
		assertEquals(E97 + "decoder.register: name 'app.dec' is already registered", r.get("dup"));
		assertEquals(List.of("app.dec"), r.get("decoders"));
		assertEquals(true, r.get("frozen"));
		assertEquals("1", r.get("version"));
	}

	@Test void a02_queue_drainedThenLive() {
		var r = part("registry");
		assertEquals(List.of("app.queued"), r.get("queued"));
		assertEquals(List.of(
			"[juneau-dashboard] " + E97 + "decoder.register: name 'bare' is bare",
			"[juneau-dashboard] JuneauDashboard: unknown registry 'nope'",
			"[juneau-dashboard] JuneauConsoleDashboard entries are [registry, name, impl] triples"
		), r.get("queueErrors"));
		assertNull(r.get("pushed"));
		assertEquals("JuneauConsoleDashboard entries are [registry, name, impl] triples", r.get("pushJunk"));
		assertEquals(List.of("app.pushed", "app.queued"), r.get("sections"));
	}

	@Test void a03_extensionName_matchesJava() {
		assertEquals(BusWiringValidator.EXTENSION_NAME.pattern(), part("registry").get("nameRe"));
	}

	@Test void a04_railWidth_clamped() {
		assertEquals(List.of(200, 480, 300, 280, 250), part("registry").get("clamp"));
	}

	private static final String E104 = "E-JS-104: share link rejected: ";
	private static final String USING = "; using saved state or defaults";

	@Test void a05_codec_roundTripAndShape() {
		var r = part("codec");
		assertEquals("#s=", r.get("prefix"));
		assertEquals(true, r.get("noPadding"));
		// Only listed topics are encoded, in persist order.
		assertEquals("{\"filter:range\":{\"schemaVersion\":1,\"viewId\":\"d\",\"options\":{\"range\":\"30d\"},"
			+ "\"start\":\"2026-09-11T00:00:00.000Z\",\"end\":\"2026-10-11T00:00:00.000Z\"},"
			+ "\"zoom:daily\":{\"schemaVersion\":1,\"start\":10,\"end\":90,\"startValue\":null,\"endValue\":null}}", r.get("roundTrip"));
		assertEquals(true, r.get("nullProto"));
		assertEquals("café ✓", r.get("unicode"));
	}

	@Test void a06_codec_wholeLinkErrors() {
		assertEquals(List.of(
			"the link is over 8192 characters",
			"the link is not base64url",
			"the link is not UTF-8",
			"the link is not JSON",
			"the link carries the key '__proto__'",
			"the link is nested more than 64 deep",
			"the link is not a dashboard link",
			"the link is for namespace 'other.dash', not 'app.dash'",
			"the link is version 1, not 2",
			"the link is for namespace object, not 'app.dash'"
		), part("codec").get("errors"));
	}

	@Test void a07_checkPayload() {
		var r = part("validate");
		assertEquals("{\"schemaVersion\":1,\"viewId\":\"d\",\"search\":\"x\"}", r.get("filterOk"));
		for (var k : List.of("noViewId", "badVersion", "badSearch", "badColumns", "oversize", "zoomBad", "legendBad", "otherFamily", "array", "noColon"))
			assertNull(r.get(k), k);
		assertEquals("{\"schemaVersion\":1,\"start\":0,\"end\":100,\"startValue\":\"Mon\",\"endValue\":1700000000000}", r.get("zoomOk"));
		assertEquals("{\"schemaVersion\":1,\"selected\":{\"a\":true,\"b\":false}}", r.get("legendOk"));
	}

	private static Map<?,?> restore(String key) {
		return (Map<?,?>)part("restore").get(key);
	}

	private static String q(String search) {
		return "{\"filter:q\":{\"schemaVersion\":1,\"viewId\":\"d\",\"search\":\"" + search + "\"}}";
	}

	@Test void a08_restore_order() {
		var d = restore("decoderWins");
		assertEquals("decoder:app.wins", d.get("source"));
		assertEquals(q("decoder"), d.get("topics"));   // filter:unknown is not listed, so it is ignored
		assertEquals(true, d.get("stripFragment"));     // a #s= fragment is removed even when a decoder won
		assertEquals(List.of(E104 + "decoder 'app.missing' is not registered" + USING, E104 + "decoder 'app.throws' threw: boom" + USING), d.get("warns"));

		var p = restore("protoDecoder");
		assertEquals("link", p.get("source"));
		assertEquals(q("link"), p.get("topics"));
		assertEquals(List.of(E104 + "decoder 'app.proto' returned a value that is not plain JSON" + USING), p.get("warns"));

		assertEquals("link", restore("linkWins").get("source"));
		assertEquals(List.of(), restore("linkWins").get("warns"));

		var off = restore("linkOff");
		assertEquals("saved", off.get("source"));
		assertEquals(q("saved"), off.get("topics"));
		assertEquals(true, off.get("stripFragment"));

		var bad = restore("badLink");
		assertEquals("saved", bad.get("source"));
		assertEquals(List.of(E104 + "the link is not base64url" + USING), bad.get("warns"));
	}

	@Test void a09_restore_savedState() {
		var s = restore("savedOnly");
		assertEquals("saved", s.get("source"));
		assertEquals(false, s.get("stripFragment"));
		assertEquals(false, s.get("deleteSaved"));
		for (var k : List.of("oldVersion", "badSaved")) {
			assertNull(restore(k).get("source"), k);
			assertNull(restore(k).get("topics"), k);
			assertEquals(true, restore(k).get("deleteSaved"), k);
		}
		var n = restore("none");
		assertNull(n.get("source"));
		assertEquals(false, n.get("stripFragment"));
		assertEquals(false, n.get("deleteSaved"));
	}

	@Test void a10_restore_runsOnce() {
		var r = part("timing");
		assertNull(r.get("before"));
		assertNull(r.get("initial"));
		assertEquals(E97 + "decoder.register: name 'app.late' was registered after restore ran", r.get("late"));
		assertNull(r.get("lateSection"));
	}

	private static Map<String,Object> filter(Object...kv) {
		var m = new LinkedHashMap<String,Object>();
		m.put("schemaVersion", 1);
		m.put("viewId", "d");
		for (var i = 0; i < kv.length; i += 2)
			m.put((String)kv[i], kv[i + 1]);
		return m;
	}

	@Test void a11_range_presetsAndCustom() {
		var r = part("range");
		assertEquals(List.of("7d", "30d", "YTD"), r.get("labels"));
		assertEquals(List.of("datetime-local", "datetime-local"), r.get("inputTypes"));
		assertEquals(List.of("false", "true", "false"), r.get("pressedInitial"));
		assertEquals("30d", r.get("firstRange"));
		assertEquals(30, ((Number)r.get("firstDays")).intValue());
		assertEquals(true, r.get("firstFloored"));
		assertEquals(List.of("schemaVersion", "viewId", "options", "start", "end"), r.get("firstKeys"));
		assertEquals("7d", r.get("clicked"));
		assertEquals(168, ((Number)r.get("clickedHours")).intValue());
		assertEquals(Arrays.asList(null, "true"), r.get("invalid"));
		assertEquals(2, ((Number)r.get("countAfterInvalid")).intValue());
		assertEquals(filter("options", Map.of("range", "custom"), "start", "2026-10-01T00:00:00.000Z", "end", "2026-10-08T12:30:00.000Z"), r.get("custom"));
		assertEquals(List.of("false", "false", "false"), r.get("pressedAfterCustom"));
		assertEquals(filter("options", Map.of("range", "custom"), "start", "2026-01-02T03:04:00.000Z", "end", "2026-01-05T00:00:00.000Z"), r.get("restored"));
		assertEquals(List.of("2026-01-02T03:04", "2026-01-05T00:00"), r.get("restoredInputs"));
		assertEquals(List.of(true, false, false, false), r.get("validate"));
	}

	@Test void a12_quickRange_utcAndParse() {
		var r = part("range");
		assertEquals(Arrays.asList(
			Map.of("start", "2026-10-04T13:40:00.000Z", "end", "2026-10-11T13:40:00.000Z"),
			Map.of("start", "2026-10-11T01:40:00.000Z", "end", "2026-10-11T13:40:00.000Z"),
			Map.of("start", "2026-01-01T00:00:00.000Z", "end", "2026-10-11T13:40:00.000Z"),
			null
		), r.get("quick"));
		// NaN serializes as null.
		assertEquals(Arrays.asList(1790812800000L, 1790812830000L, null, null), ((List<?>)r.get("parse")).stream().map(x -> x == null ? null : ((Number)x).longValue()).toList());
	}

	@Test void a13_search_pauseAndEnter() {
		var r = part("search");
		assertEquals("abc", r.get("initialValue"));
		assertEquals("search", r.get("type"));
		assertEquals("Find", r.get("placeholder"));
		assertEquals("Search", r.get("label"));
		assertEquals(List.of("abc", "abcd", "abcde"), r.get("pubs"));
		assertEquals(1, ((Number)r.get("beforePause")).intValue());
		assertEquals(List.of(250), r.get("pause"));
		assertEquals(1, ((Number)r.get("liveBeforeEnter")).intValue());
		assertEquals(0, ((Number)r.get("liveAfterEnter")).intValue());   // Enter itself cancelled the pending pause
	}

	private static Map<String,Object> row(String value, String count, boolean checked, boolean missing) {
		var m = new LinkedHashMap<String,Object>();
		m.put("value", value);
		m.put("count", count);
		m.put("checked", checked);
		m.put("missing", missing);
		return m;
	}

	@Test void a14_choice_dictCountsCapAndAllSelected() {
		var r = part("choice");
		assertEquals(filter("columns", Map.of("reason", List.of("late", "gone")), "options", Map.of("deselected", List.of())), r.get("restoredFirst"));
		assertEquals(List.of(row("late", "4", true, false), row("lost", "3", false, false), row("damaged", "2", false, false), row("gone", null, true, true)), r.get("shown"));
		assertEquals(List.of("Show all (4)", "Select all"), r.get("more"));
		assertEquals(5, ((Number)r.get("all")).intValue());
		assertEquals(List.of("Show fewer", "Select all"), r.get("less"));
		assertEquals(filter(), r.get("first"));
		assertEquals(filter("columns", Map.of("reason", List.of("lost", "damaged", "wrong-item")), "options", Map.of("deselected", List.of("late"))), r.get("unchecked"));
		assertEquals(filter(), r.get("rechecked"));
		assertEquals(true, r.get("ignored"));
		assertEquals(List.of(row("lost", "3", true, false), row("damaged", "2", true, false), row("wrong-item", "1", true, false), row("late", null, false, true)), r.get("gone"));
		// Ticking the last departed value makes everything selected again, so the columns are left out.
		assertEquals(filter(), r.get("retick"));
		assertEquals(List.of("'reason' is not a dictionary column"), r.get("notDict"));
		assertEquals(List.of(true, false, false, true), r.get("validate"));
	}

	@Test void a15_custom_unknownAndCtx() {
		var r = part("custom");
		var msg = "E-JS-105: dashboard 'd' uses unknown rail section 'app.nope'; registered: 'app.note'";
		assertNull(r.get("unknown"));
		assertEquals(msg, r.get("box"));
		assertEquals("alert", r.get("role"));
		assertEquals("jc-card-error", r.get("cls"));
		assertEquals(List.of("[juneau-dashboard] " + msg), r.get("log"));
		var seen = new LinkedHashMap<String,Object>();
		seen.put("id", "n");
		seen.put("topic", "filter:n");
		seen.put("viewId", "d");
		seen.put("args", Map.of("a", 1));
		seen.put("initial", null);
		seen.put("frozen", true);
		assertEquals(seen, r.get("seen"));
		assertEquals(List.of(Map.of("from", "n", "topic", "filter:n", "payload", filter("search", "n"))), r.get("published"));
	}

	private static final String NOT_CACHED = "The browser would not store this data, so it is held in memory and reloads with the page.";

	private static Map<String,Object> st(String text, String tone, String title) {
		var m = new LinkedHashMap<String,Object>();
		m.put("text", text);
		m.put("tone", tone);
		m.put("title", title);
		return m;
	}

	@Test void a16_statusText_formats() {
		var r = part("status");
		assertEquals(List.of(
			st("Loading 63% (412k rows)", "normal", null),
			st("Loading (412k rows)", "normal", null),
			st("412,118 rows · loaded 2 min ago", "normal", null),
			st("412,118 rows · loaded 2 min ago · refresh failed", "warning", null),
			st("2,000,000 rows · loaded just now · truncated at 2M", "warning", null),
			st("12 rows · loaded 3 h ago · aggregated · not cached", "normal", NOT_CACHED),
			st("Load failed: E-JS-62 · HTTP 500", "error", null),
			st("Waiting for data", "normal", null),
			st("5 rows", "normal", null)
		), r.get("texts"));
		assertEquals(List.of("999", "1k", "2k", "412k", "2M", "1.5M"), r.get("compact"));
		assertEquals(List.of("just now", "just now", "just now", "1 min ago", "59 min ago", "1 h ago", "1 d ago"), r.get("ago"));
		assertEquals(Map.of("a1", "a", "a2", "a", "wait1", List.of(4000), "a3", "c", "wait2", List.of(4900)), r.get("announcer"));
	}

	private static Map<String,Object> cmd(String source, boolean clearCache) {
		var p = new LinkedHashMap<String,Object>();
		p.put("schemaVersion", 1);
		p.put("op", "refresh");
		p.put("options", Map.of("clearCache", clearCache));
		return Map.of("topic", "cmd:" + source, "payload", p);
	}

	@Test void a17_header_statusRefreshSaveCopy() {
		var r = part("header");
		var ready = "orders: 412,118 rows · loaded 2 min ago";
		var failed = "totals: Load failed: E-JS-62 · HTTP 500";
		assertEquals(List.of(List.of("orders: Waiting for data", "normal"), List.of("totals: Waiting for data", "normal")), r.get("initialLines"));
		assertEquals(List.of(30000), r.get("firstPauses"));
		assertEquals(ready + "; totals: Waiting for data", r.get("live1"));
		assertEquals(ready + "; totals: Waiting for data", r.get("live2"));
		assertEquals(true, r.get("waitOk"));
		assertEquals(ready + "; " + failed, r.get("live3"));
		assertEquals(List.of(List.of(ready, "normal"), List.of(failed, "error")), r.get("afterLines"));
		assertEquals(List.of(cmd("orders", false), cmd("totals", false), cmd("orders", true), cmd("totals", true)), r.get("published"));
		assertEquals(List.of("true", false, true), r.get("opened"));
		assertEquals(List.of("false", true), r.get("closed"));
		assertEquals(List.of("false", true, true), r.get("escaped"));
		assertEquals(1, ((Number)r.get("savePending")).intValue());
		var q = filter("search", "xy");
		assertEquals(List.of(
			List.of("state", Map.of("v", 1, "topics", Map.of("filter:q", q))),
			List.of("state", Map.of("v", 1, "topics", Map.of()))
		), r.get("sets"));
		assertEquals("Copied", r.get("copiedText"));
		assertEquals(true, r.get("urlStart"));
		assertEquals(Map.of("t", Map.of("filter:q", q)), r.get("decoded"));
		assertEquals(Map.of("readOnly", true, "sameUrl", true, "label", "Share link", "selected", true, "focused", true, "hint", "Press Ctrl+C / Cmd+C"), r.get("field"));
		var tooLong = new LinkedHashMap<String,Object>();
		tooLong.put("buttons", List.of("Link too long"));
		tooLong.put("warns", List.of("[juneau-dashboard] the share link is over 8192 characters; it was not copied"));
		tooLong.put("statusLines", 0);
		tooLong.put("clipped", null);
		tooLong.put("header", List.of("jc-dash-actions"));
		assertEquals(tooLong, r.get("tooLong"));
	}

	@Test void a18_encodeLink_dropsUnserializableTopic() {
		var r = part("encodeGuard");
		assertEquals(Map.of("filter:q", filter("search", "ok")), r.get("t"));
	}

	@Test void a19_rail_focusErrorsMissingAndDates() {
		var r = part("railA11y");
		assertEquals(Map.of("count", 1, "same", true, "value", "late", "active", true, "rebuilt", true), r.get("afterToggle"));
		assertEquals(Map.of("count", 1, "text", "Show fewer", "active", true), r.get("afterMore"));
		assertEquals(Map.of("count", 1, "same", true, "rebuilt", true, "active", true, "value", "lost"), r.get("afterVersion"));
		assertEquals(List.of("gone| (not in data)"), r.get("missing"));
		assertEquals(Map.of("text", "Enter a valid start and end.", "role", "alert", "described", List.of(true, false)), r.get("unparsable"));
		assertEquals(Map.of("text", "Start must be before end.", "role", "alert", "described", List.of(false, true)), r.get("order"));
		assertEquals(Map.of("text", "", "role", "alert", "described", List.of(false, false)), r.get("ok"));
		assertEquals("Start must be before end.", r.get("shown"));
		assertEquals(Map.of("text", "", "role", "alert", "described", List.of(false, false)), r.get("afterEdit"));
		assertEquals(Map.of("text", "", "role", "alert", "described", List.of(false, false)), r.get("afterPreset"));
		// Impossible dates are NaN (null in JSON); real ones, including 29 Feb of a leap year, parse.
		var parsed = ((List<?>)r.get("parse")).stream().map(x -> x == null ? null : ((Number)x).longValue()).toList();
		assertEquals(Arrays.asList(null, null, null, 1772323140000L, 1835395200000L, null, null, 1790812859123L), parsed);
	}

	@Test void a20_header_pagehideFlushAnnouncedCopyAndMenuDismiss() {
		var r = part("headerA11y");
		assertEquals(Map.of("pendingBefore", 1, "pendingAfter", 0, "sets", 1, "afterIdle", 1,
			"visibility", Map.of("whileVisible", 1, "afterHidden", 2)), r.get("flushed"));
		assertEquals(Map.of("live1", "Copied", "liveCleared", "", "live2", "Copied", "reverts", 1, "text", "Copy link"), r.get("copy"));
		assertEquals(Map.of(
			"open", List.of("true", false),
			"insidePress", List.of("true", false),
			"insideFocus", List.of("true", false),
			"outsidePress", List.of("false", true),
			"focusAway", List.of("false", true),
			"reopened", List.of("true", false),
			"idleFocusout", List.of("true", false)
		), r.get("menu"));
	}

	@Test void a21_choice_deselectedSurvivesReloadAndSelectAll() {
		var r = part("choice2");
		assertEquals(filter("columns", Map.of("reason", List.of("lost", "wrong-item")), "options", Map.of("deselected", List.of("damaged", "late"))), r.get("first"));
		// After a reload the departed unticked values are listed again, greyed and unticked, with a way back.
		assertEquals(List.of(row("lost", null, true, false), row("wrong-item", null, true, false), row("damaged", null, false, true), row("late", null, false, true)),
			((List<?>)r.get("restored")).stream().map(x -> rowOnly((Map<?,?>)x)).toList());
		assertEquals(List.of("Select all"), r.get("restoredButtons"));
		assertEquals(filter("columns", Map.of("reason", List.of("lost", "wrong-item", "damaged")), "options", Map.of("deselected", List.of("late"))), r.get("afterDamaged"));
		// Ticking the last one restores "everything": no columns.
		assertEquals(filter(), r.get("afterLate"));
		assertEquals(List.of(), r.get("afterLateButtons"));
		// Select all clears the filter without columns, and the button goes away.
		assertEquals(List.of("Show all (3)", "Select all"), r.get("before2"));
		var after = (Map<?,?>)r.get("after2");
		assertEquals(filter(), after.get("published"));
		assertEquals(List.of("Show all (3)"), after.get("buttons"));
		assertEquals(List.of(true, true), ((List<?>)after.get("rows")).stream().map(x -> ((Map<?,?>)x).get("checked")).toList());
		assertEquals(List.of(true, false, false, true, false), r.get("validate"));
	}

	@Test void a22_choice_departedValuesAreCapped() {
		var r = part("choice2");
		// Ticked values that left the frame are always listed; unticked ones stop at max (2) until Show all.
		assertEquals(List.of("lost", "damaged", "k1", "k2", "k3", "g0", "g1"), r.get("capped"));
		assertEquals(List.of("Show all (9)", "Select all"), r.get("cappedButtons"));
		assertEquals(List.of("lost", "damaged", "wrong-item", "k1", "k2", "k3", "g0", "g1", "g2", "g3", "g4", "g5"), r.get("lifted"));
	}

	@Test void a23_restore_decoderWithThrowingGetterFallsBack() {
		var r = part("restoreGetter");
		assertEquals("saved", r.get("source"));
		assertEquals("{\"filter:q\":{\"schemaVersion\":1,\"viewId\":\"d\",\"search\":\"s\"}}", r.get("topics"));
		assertEquals(List.of(E104 + "decoder 'app.getter' returned a value that is not plain JSON" + USING), r.get("warns"));
	}

	@Test void a24_range_restoredCustomIsPublishedAsIso() {
		assertEquals(filter("options", Map.of("range", "custom"), "start", "2026-01-01T00:00:00.000Z", "end", "2026-02-01T00:00:00.000Z"), part("range").get("loose"));
	}

	@Test void a25_choice_payloadStaysUnderCapAndShowAllOnlyWhenNeeded() {
		var r = part("choice2");
		var shrunk = (Map<?,?>)r.get("capShrunk");
		assertTrue(((Number)shrunk.get("bytes")).intValue() <= 4096);
		assertEquals(10, shrunk.get("columns"));
		assertTrue(((Number)shrunk.get("gone")).intValue() > 0 && ((Number)shrunk.get("gone")).intValue() < 150);
		assertEquals(true, shrunk.get("prefix"));
		var dropped = (Map<?,?>)r.get("capDropped");
		assertTrue(((Number)dropped.get("bytes")).intValue() <= 4096);
		assertEquals(98, dropped.get("columns"));
		assertNull(dropped.get("gone"));
		assertEquals(5, ((Map<?,?>)r.get("capSmall")).get("gone"));
		assertEquals(Map.of("rows", 6, "buttons", List.of("Select all")), r.get("noShowAll"));
	}

	@Test void a26_choice_selectAllMovesFocusToFirstBox() {
		var r = (Map<?,?>)part("railA11y").get("afterSelectAll");
		assertEquals(Map.of("count", 1, "first", true, "active", true, "buttons", List.of("Show fewer")), r);
	}

	private static Map<String,Object> rowOnly(Map<?,?> m) {
		return row((String)m.get("value"), null, (Boolean)m.get("checked"), (Boolean)m.get("missing"));
	}
}
