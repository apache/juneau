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
import org.junit.jupiter.api.*;

/**
 * Behavioral coverage for the {@code juneau-bus.js} core (message bus addendum, spec §3, §6.3, §7).  The Node
 * harness {@code bus-core.cjs} loads the bus standalone into the DOM shim and reports one section per subject; every
 * assertion is here, so a failure names the subject in surefire.  Gated on {@code node} being on {@code PATH}.
 */
class ViewsJs_BusCore_Test extends TestBase {

	private static final String CORE_END = "// ---- stock sources: session handshake";

	private static Map<String,Object> report;

	@BeforeAll
	static void runHarness() {
		report = BusHarness.run("bus-core.cjs", ViewsMixin.BUS_JS_RESOURCE);
	}

	@SuppressWarnings("unchecked")
	private static Map<String,Object> section(String name) {
		assumeTrue(report != null, "node not on PATH");
		var s = (Map<String,Object>) report.get(name);
		assertNotNull(s, "harness section " + name + " missing; report=" + report);
		return s;
	}

	@SuppressWarnings("unchecked")
	private static Map<String,Object> map(Map<String,Object> m, String key) {
		return (Map<String,Object>) m.get(key);
	}

	private static List<?> list(Map<String,Object> m, String key) {
		return (List<?>) m.get(key);
	}

	private static int num(Map<String,Object> m, String key) {
		return ((Number) m.get(key)).intValue();
	}

	private static String owned(String topic, String owner, String who, String key) {
		return "'" + topic + "' is owned by '" + owner + "'; '" + who + "' may not publish or clear it (publish cmd:" + key + " instead)";
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Source shape (no node needed)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a00_sourceShape() throws Exception {
		var js = BusHarness.source(ViewsMixin.BUS_JS_RESOURCE);
		assertTrue(js.contains("juneau-bus.js"), "the header comment names the asset (Task 3's serving test greps for it)");
		var end = js.indexOf(CORE_END);
		assertTrue(end > 0, "the stock-sources section marker is present");
		var core = js.substring(0, end);
		assertFalse(core.contains("addEventListener("), "the core attaches no listeners");
		assertFalse(core.contains("document."), "the core, the bridge runtime and the codec never touch the DOM");
		assertTrue(core.contains("NS.bus = {"), "the core defines exactly JuneauViews.bus");
		assertFalse(js.toLowerCase().contains("slds"), "no SLDS anywhere");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Harness sections
	//-----------------------------------------------------------------------------------------------------------------

	@Test void t01_constantsAndApi() {
		var s = section("t01");
		assertEquals("1", s.get("contractVersion"));
		assertEquals(List.of("card", "selection", "filter", "redraw", "detail", "bulk", "cmd", "probe", "job", "badge", "bridge"), s.get("families"));
		assertEquals(true, s.get("familiesFrozen"));
		assertEquals(64, num(s, "drainCap"));
		assertEquals(256, num(s, "historyCap"));
		assertEquals(256, num(s, "cycleTraceCap"));
		assertEquals("juneau:framework", s.get("senderKey"));
		assertEquals(List.of(), s.get("missingFunctions"));
		assertEquals(List.of("backoffDelay", "canonicalJson", "parseTopic", "resolvePath", "topicMatches"), s.get("util"));
		assertEquals(List.of("clearTimeout", "now", "random", "setTimeout"), s.get("timers"));
		assertEquals(List.of("bus"), s.get("namespaceKeys"), "juneau-bus.js adds nothing but JuneauViews.bus");
		var e = map(s, "busError");
		assertEquals("BusError", e.get("name"));
		assertEquals("E-JS-40", e.get("code"));
		assertEquals("m", e.get("message"));
		assertEquals("t", e.get("topic"));
		assertEquals(true, e.get("hasStack"));
		assertEquals(true, s.get("reloadKeepsInstance"), "a second load keeps the first instance and its retained state");
	}

	@Test void t02_topicSyntax() {
		var s = section("t02");
		assertEquals(List.of(
			"selection:changes=ok", "cmd:tasks=ok", "bridge:live=ok", "job:j-1.2_3=ok", "ssc.focus=ok", "ssc.focus:triage=ok",
			"app.region-picked=ok", "focus=E-JS-40", "selection=E-JS-40", "ssc.focus.x=E-JS-40", "Ssc.focus=E-JS-40",
			"ssc.focus:=E-JS-40", "ssc.focus:a b=E-JS-40", "ssc.:x=E-JS-40", "selection:*=E-JS-40", "=E-JS-40"),
			s.get("results"));
		assertEquals("{\"family\":\"ssc.focus\",\"key\":\"triage\",\"framework\":false,\"kind\":\"custom\"}", s.get("parsedCustom"));
		assertEquals("{\"family\":\"redraw\",\"key\":\"runs\",\"framework\":true,\"kind\":\"event\"}", s.get("parsedEvent"));
		assertEquals("command", s.get("parsedCommand"));
		assertEquals("{\"family\":\"selection\",\"key\":\"*\",\"framework\":true,\"kind\":\"state\"}", s.get("pattern"));
		assertEquals("invalid topic 'focus': 'focus' is not a framework family; a custom topic is 'ns.name' (did you mean 'app.focus'?)",
			s.get("focusMessage"));
		assertEquals("E-JS-40", s.get("subscribeWildcard"));
		assertEquals("E-JS-40", s.get("publishWildcard"));
		assertEquals("E-JS-40", s.get("declareBad"));
		assertEquals("E-JS-40", s.get("getBad"));
	}

	@Test void t03_publishSubscribeMeta() {
		var s = section("t03");
		assertEquals(true, s.get("r1"));
		assertEquals(true, s.get("r2"), "a publish with no subscribers is still accepted");
		assertEquals(List.of("{\"p\":{\"n\":1},\"m\":{\"topic\":\"app.note\",\"from\":\"page\",\"seq\":1,\"ts\":1000,\"retained\":false},\"frozen\":true}"),
			s.get("got"), "exactly one delivery: the double unsubscribe is idempotent; ts comes from config.timers.now");
		assertEquals(List.of("changes|-|3", "tasks|-|4", "server|live|5", "page|-|6"), s.get("metas"),
			"from = owner id / opts.from / 'page'; meta.bridge only when from is 'server'");
	}

	@Test void t04_retainedReplay() {
		var s = section("t04");
		assertEquals(1, num(s, "lateSync"), "the retained value is delivered DURING subscribe()");
		assertEquals(List.of("c-17|true|1|changes", "c-18|false|3|changes"), s.get("late"));
		assertEquals(1, num(s, "optOut"), "{retained:false} skips the replay but gets live messages");
		assertEquals(List.of("c-17"), s.get("getIds"));
		assertEquals(true, s.get("getFrozen"));
		assertEquals(0, num(s, "lateEvent"), "event topics are never replayed");
		assertEquals(true, s.get("eventNotRetained"));
	}

	@Test void t05_distinctUntilChanged() {
		var s = section("t05");
		assertEquals(List.of(true, false, true, true, true), s.get("returns"));
		assertEquals(List.of(1, 2), list(s, "seen").stream().map(o -> ((Number) o).intValue()).toList());
		assertEquals(List.of(false, true, false, false, false), s.get("suppressed"));
	}

	@Test void t06_clear() {
		var s = section("t06");
		assertEquals(true, s.get("r1"));
		assertEquals(false, s.get("r2"), "clearing an empty topic delivers nothing");
		assertEquals(List.of("{\"a\":1}|false|true", "null|true|false", "{\"a\":1}|false|false"), s.get("seen"), "the republish after clear is live, not a replay");
		assertEquals(true, s.get("getAfter"));
		assertEquals(0, num(s, "late"));
		assertEquals(true, s.get("republish"), "after clear, the same payload is a change again");
	}

	@Test void t07_jsonOnly() {
		var s = section("t07");
		var r = map(s, "results");
		var expected = new LinkedHashMap<String,String>();
		expected.put("fn", "payload.f is a function");
		expected.put("undef", "payload.u is undefined");
		expected.put("dom", "payload.el is a DOM node");
		expected.put("cycle", "payload.self is a cycle");
		expected.put("nan", "payload.n is NaN");
		expected.put("inf", "payload[0] is Infinity");
		expected.put("symbol", "payload.s is a symbol");
		expected.put("top", "payload is undefined");
		expected.forEach((k, why) -> {
			@SuppressWarnings("unchecked")
			var e = (Map<String,Object>) r.get(k);
			assertNotNull(e, k + " must throw");
			assertEquals("E-JS-42", e.get("code"), k);
			assertEquals("payload for 'app.x' is not JSON-serializable: " + why, e.get("message"), k);
		});
		assertEquals(0, num(s, "rejectedDeliveries"), "a rejected publish delivers nothing");
		assertEquals(true, s.get("sharedNotCycle"), "a shared (acyclic) reference is fine");
		assertEquals(true, s.get("nullOk"));
	}

	@Test void t08_deepFreeze() {
		var s = section("t08");
		assertEquals(true, s.get("deepFrozen"));
		assertEquals(true, s.get("copied"));
		assertEquals(false, s.get("originalFrozen"), "the caller's object is never frozen");
		assertEquals("TypeError", s.get("mutationName"));
		assertEquals(2, num(s, "value"));
		assertEquals(true, s.get("sameInstanceForAll"));
	}

	@Test void t09_schemaVersion() {
		var s = section("t09");
		var p = "E-JS-43 framework topic 'cmd:tasks' needs schemaVersion 1, got ";
		assertEquals(p + "undefined", s.get("missing"));
		assertEquals(p + "2", s.get("wrong"));
		assertEquals(p + "\"1\"", s.get("text"));
		assertEquals(p + "a null payload", s.get("nullPayload"));
		assertEquals("ok", s.get("ok"));
		assertEquals(true, s.get("customUnchecked"), "custom topics are not checked (OQ-B9)");
	}

	@Test void t10_breadthFirst() {
		assertEquals(List.of("A:one", "A:after-publish", "B:one", "C:two"), section("t10").get("log"));
	}

	@Test void t11_drainCap() {
		var s = section("t11");
		assertEquals(true, s.get("first"));
		assertEquals(64, num(s, "deliveries"));
		assertEquals(63, num(s, "trueReturns"));
		assertEquals(1, num(s, "falseReturns"));
		assertEquals(false, s.get("lastReturn"), "the 65th publish returns false");
		assertEquals(1, num(s, "capErrors"), "E-JS-44 once per drain");
		assertEquals("bus drain cap (64) exceeded; likely cycle: b -> app.pong -> a -> app.ping -> b", s.get("message"));
		assertEquals("a", s.get("from"));
		assertEquals("a", s.get("paintOn"), "painted on the card that published the dropped message");
		assertEquals(1, num(s, "dropped"));
		assertEquals(1, num(s, "consoleLogged"));
		assertEquals(true, s.get("nextDrainWorks"), "the cap resets with the drain");
	}

	@Test void t12_subscriberIsolation() {
		var s = section("t12");
		assertEquals(true, s.get("returned"));
		assertEquals(true, s.get("reached"));
		assertEquals(List.of("subscriber for 'app.x' (owner 'page') threw: boom", "subscriber for 'app.x' (owner 'w') threw: bang"), s.get("messages"));
		assertEquals(Arrays.asList(null, "w"), s.get("paintOn"));
	}

	@Test void t13_ownerRule() {
		var s = section("t13");
		var r = map(s, "returns");
		assertEquals(true, r.get("ownerPublish"));
		assertEquals(false, r.get("otherPublish"));
		assertEquals(false, r.get("globalPublish"));
		assertEquals(false, r.get("otherClear"));
		assertEquals(false, r.get("globalClear"));
		assertEquals(true, r.get("otherCmd"), "cmd: is writable by anyone");
		assertEquals(true, r.get("globalCmd"));
		assertEquals(true, r.get("claimFree"));
		assertEquals(false, r.get("claimedByOther"));
		assertEquals(false, r.get("unclaimedGlobal"));
		assertEquals(true, s.get("stillOwnersValue"));
		var c = map(s, "claimConflict");
		assertEquals("E-JS-49", c.get("code"), "claim() throws");
		assertEquals(owned("selection:tbl", "tbl", "other", "tbl"), c.get("message"));
		assertEquals("TypeError", map(s, "claimCommand").get("name"), "command topics have no owner to claim");
		assertEquals(List.of(
			owned("selection:tbl", "tbl", "other", "tbl") + "|other",
			owned("selection:tbl", "tbl", "page", "tbl") + "|null",
			owned("selection:tbl", "tbl", "other", "tbl") + "|other",
			owned("selection:tbl", "tbl", "page", "tbl") + "|null",
			owned("filter:other", "other", "tbl", "other") + "|tbl",
			owned("selection:zz", "zz", "page", "zz") + "|null"),
			s.get("messages"), "publish/clear REPORT E-JS-49 (sinks + console) and return false");
	}

	@Test void t14_declare() {
		var s = section("t14");
		assertEquals("topic 'app.x' re-declared with retain=false (was true)", map(s, "conflict").get("message"));
		assertEquals("E-JS-50", map(s, "conflict").get("code"));
		assertEquals("topic 'selection:*' re-declared with retain=false (was true)", map(s, "framework").get("message"));
		assertNull(s.get("frameworkOk"), "declaring a framework family with its own retain is allowed");
		assertEquals("topic 'ssc.focus:triage' re-declared with retain=false (was true)", map(s, "patternConflict").get("message"));
		assertEquals(List.of(1), list(s, "patternRetained").stream().map(o -> ((Number) o).intValue()).toList(), "'family:*' declares every key");
		assertEquals("TypeError", s.get("badOpts"));
		assertEquals(List.of("page", "ssc-script"), s.get("declaredBy"));
		assertEquals(true, s.get("retain"));
	}

	@Test void t15_undeclaredIsUnretained() {
		var s = section("t15");
		assertEquals(1, list(s, "live").size());
		assertEquals(0, num(s, "late"));
		assertEquals("{\"topic\":\"app.u\",\"kind\":\"custom\",\"retain\":false,\"declared\":false,\"retained\":false,\"subscribers\":2,\"owner\":null,\"declaredBy\":[]}",
			s.get("entry"));
	}

	@Test void t16_selfEcho() {
		var s = section("t16");
		assertEquals("[2]", String.valueOf(s.get("own")).replace(" ", ""));
		assertEquals("[1,2]", String.valueOf(s.get("echo")).replace(" ", ""));
		assertEquals("[1,2]", String.valueOf(s.get("global")).replace(" ", ""));
	}

	@Test void t17_historyRing() {
		var s = section("t17");
		assertEquals(256, num(s, "length"));
		assertEquals(45, num(s, "firstSeq"));
		assertEquals(300, num(s, "lastSeq"));
		assertEquals(List.of("dropped", "from", "seq", "size", "subscribers", "suppressed", "topic", "ts"), s.get("keys"), "no payload in history");
		assertEquals(true, s.get("frozen"));
		assertEquals(9, num(s, "size"), "size = canonical JSON length of {\"i\":299}");
		assertEquals(true, s.get("copy"));
		assertEquals(true, s.get("lastSuppressed"));
		assertEquals(302, num(s, "lastSeqAfter"), "suppressed publishes still take a seq");
	}

	@Test void t18_trace() {
		var s = section("t18");
		assertEquals(false, s.get("prev"));
		assertEquals(true, s.get("now"));
		assertEquals(false, s.get("query"), "trace() with no argument only reads");
		var big = ("{\"s\":\"" + "x".repeat(500) + "\"}").substring(0, 120) + "…";
		assertEquals(List.of(
			"[bus] #1 app.r  from=page  subs=1 retained  {\"count\":3,\"rows\":\"<3 rows>\"}",
			"[bus]   #2 app.nested  from=tasks  subs=0  {\"op\":\"reload\"}",
			"[bus] #3 app.r  from=page  suppressed (unchanged)",
			"[bus] #4 app.big  from=page  subs=0  " + big),
			s.get("lines"), "one console.debug line per publish, indented per drain depth; nothing after trace(false)");
	}

	@Test void t19_traceFlags() {
		var s = section("t19");
		assertEquals(true, s.get("viaStorage"));
		assertEquals(true, s.get("viaUrl"));
		assertEquals(false, s.get("otherValue"));
		assertEquals(false, s.get("off"));
		assertEquals(false, s.get("blockedStorage"), "blocked storage is not an error");
	}

	@Test void t20_errorSinks() {
		var s = section("t20");
		assertEquals(List.of("E-JS-45|BusError|app.x"), s.get("got"), "unregister is idempotent and effective");
		assertNull(s.get("sinkThrow"), "a throwing sink never reaches the publisher");
		assertEquals(3, num(s, "consoleErrors"), "console.error logs every reported error, sinks or not");
		assertEquals(true, s.get("sinkLogged"));
	}

	@Test void t21_ownerDispose() {
		var s = section("t21");
		assertEquals(0, num(s, "heard"));
		assertEquals(List.of("{\"schemaVersion\":1,\"ids\":[\"a\"]}|false", "null|true"), s.get("watcher"));
		assertEquals(0, num(s, "late"), "a late subscriber never sees a disposed owner's state");
		assertEquals(true, s.get("reclaim"));
		assertEquals(true, s.get("filterReleased"));
		assertEquals(false, s.get("afterDispose"));
		assertEquals(true, s.get("warned"));
	}

	@Test void t23_util() {
		var s = section("t23");
		assertEquals("[\"c-17\",5,true,true,true,true]", String.valueOf(s.get("path")).replace(" ", ""));
		assertEquals("{\"a\":[{\"c\":2,\"d\":1}],\"b\":1}", s.get("canonical"));
		assertEquals(List.of(true, false, false, true, true), s.get("matches"));
	}

	@Test void t24_topics() {
		var s = section("t24");
		assertEquals(List.of(
			"{\"topic\":\"app.x\",\"kind\":\"custom\",\"retain\":false,\"declared\":true,\"retained\":false,\"subscribers\":0,\"owner\":null,\"declaredBy\":[\"page\"]}",
			"{\"topic\":\"job:*\",\"kind\":\"state\",\"retain\":true,\"declared\":true,\"retained\":false,\"subscribers\":0,\"owner\":null,\"declaredBy\":[\"bridge:live\"]}",
			"{\"topic\":\"selection:changes\",\"kind\":\"state\",\"retain\":true,\"declared\":true,\"retained\":true,\"subscribers\":1,\"owner\":\"changes\",\"declaredBy\":[]}",
			"{\"topic\":\"ssc.u\",\"kind\":\"custom\",\"retain\":false,\"declared\":false,\"retained\":false,\"subscribers\":0,\"owner\":null,\"declaredBy\":[]}"),
			s.get("topics"));
		assertEquals(true, s.get("frozen"));
	}

	@Test void t25_subscribeDuringDrain() {
		var s = section("t25");
		assertEquals(List.of("2|true|3"), s.get("afterDrain"), "one replay of the newest value; the queued v=1 and v=2 copies are skipped");
		assertEquals(List.of("2|true|3", "3|false|4"), s.get("all"), "later publishes still arrive");
	}

	@Test void t26_overflowRollsBackRetained() {
		var s = section("t26");
		assertEquals("undefined", s.get("freshGet"), "a purged first value leaves nothing retained");
		assertEquals(List.of(true), s.get("freshHist"), "the purged message is recorded as dropped");
		assertEquals(true, s.get("republish"), "the same value is not suppressed as unchanged after the rollback");
		assertEquals(5, num(s, "priorGet"), "a purged update restores the previous value");
		assertEquals(List.of(5), list(s, "late").stream().map(o -> ((Number) o).intValue()).toList());
		assertEquals(2, num(s, "capErrors"));
	}

	@Test void t27_clearSurvivesOverflow() {
		var s = section("t27");
		assertEquals(true, s.get("get"));
		assertEquals(0, num(s, "late"));
		assertEquals(true, s.get("reclaim"), "the disposed owner's claim is released");
	}

	@Test void t28_unsubscribeMidDrain() {
		assertEquals(List.of("A", "C", "A"), section("t28").get("log"), "B is gone before its turn; C unsubscribes itself after its first call");
	}

	@Test void t29_getDuringDrain() {
		assertEquals(2, num(section("t29"), "during"));
	}

	@Test void t30_ownerOnlyEvent() {
		var s = section("t30");
		var r = map(s, "returns");
		assertEquals(true, r.get("owner"));
		assertEquals(false, r.get("other"));
		assertEquals(false, r.get("global"));
		assertEquals(List.of(
			owned("redraw:tbl", "tbl", "other", "tbl") + "|other",
			owned("redraw:tbl", "tbl", "page", "tbl") + "|null"),
			s.get("errors"));
	}

	@Test void t31_traceCleared() {
		assertEquals(List.of(
			"[bus] #1 selection:c  from=c  subs=0 retained  {\"ids\":[\"a\"],\"schemaVersion\":1}",
			"[bus] #2 selection:c  from=c  subs=0 retained cleared  null"),
			section("t31").get("lines"));
	}

	@Test void t32_overflowRollbackAroundClears() {
		var s = section("t32");
		assertEquals(true, s.get("aGet"), "v1, clear, v2 purged: the clear wins and nothing stays stored");
		assertEquals(true, s.get("aRepublish"), "so republishing v2 is a change");
		assertEquals(true, s.get("bGet"), "a clear that is the overflowing message is not undone");
		assertEquals(true, s.get("bRepublish"));
	}
}
