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
 * The console shell on the page bus (message bus addendum, spec §4.1, §4.5, §5.2-§5.4, §6.3): {@code card:<id>}
 * lifecycle and ownership, implicit claims, {@code cmd:<id>} ops, declared roles and {@code params}, buffered
 * {@code ctx.publish}/{@code ctx.subscribe}, the error painter, and E-JS-46.  Drives {@code console-shell-bus.cjs}
 * against the real {@code juneau-bus.js} and {@code juneau-console.js}; gated on {@code node} being on {@code PATH}.
 *
 * <p>
 * Lives in this module, not console-ui, for the same reason as {@code ConsoleCards_Shell_Test}: every Node harness
 * that runs the shell lives here, and {@link BusHarness} is package-private.
 */
class ConsoleBus_Shell_Test extends TestBase {

	private static final String CONSOLE_JS = "/org/apache/juneau/console/juneau-console.js";

	private static Map<String,Object> report;

	@BeforeAll
	static void runHarness() {
		report = BusHarness.run("console-shell-bus.cjs", ViewsMixin.BUS_JS_RESOURCE, CONSOLE_JS);
	}

	@SuppressWarnings("unchecked")
	private static Map<String,Object> scenario(String name) {
		assumeTrue(report != null, "node not on PATH");
		assertNull(report.get("harnessError"), () -> String.valueOf(report.get("harnessError")));
		var s = (Map<String,Object>) report.get(name);
		assertNotNull(s, name);
		return s;
	}

	@SuppressWarnings("unchecked")
	private static Map<String,Object> map(Map<String,Object> m, String key) {
		return (Map<String,Object>) m.get(key);
	}

	@Test void a00_sourceShape() throws Exception {
		var src = BusHarness.source(CONSOLE_JS);
		var validate = src.indexOf("paintWiringProblems(doc, bus.wiring.validate(contract, cardTypeTable()));");
		var anchor = src.indexOf("// ---- bus: bridges attach here (Task 13) ----");
		var wire = src.indexOf("wireCards(doc, bus, contract);");
		assertTrue(validate > 0 && validate < anchor && anchor < wire, "validate, then the Task 13 anchor, then wireCards");
		assertEquals(anchor, src.lastIndexOf("// ---- bus: bridges attach here (Task 13) ----"), "exactly one anchor");
		assertFalse(src.contains("extras.bus"), "handlers declare roles/ops/implicit directly");
	}

	@Test void a01_noBusOnAWiredPageIsE46() {
		var s = scenario("s1");
		assertNull(s.get("threw"), "mount itself succeeds; the check runs at DOMContentLoaded");
		assertEquals("E-JS-46", map(s, "readyThrew").get("code"));
		assertEquals("page wires topics but juneau-bus.js is not loaded", map(s, "readyThrew").get("message"));
		assertEquals(List.of("E-JS-46"), s.get("banner"));
	}

	@Test void a02_noBusAndNoWiringMountsButCtxPublishIsE46() {
		var s = scenario("s1b");
		assertNull(s.get("threw"));
		assertNull(s.get("readyThrew"));
		assertEquals("E-JS-46", s.get("late"));
	}

	@Test void a03_cardLifecycle() {
		var s = scenario("s2");
		assertEquals(Map.of("schemaVersion", 1, "id", "k", "type", "rec", "state", "mounted"), s.get("mounted"));
		assertEquals(Map.of("schemaVersion", 1, "id", "bad", "type", "boom", "state", "failed",
			"error", Map.of("code", "E-JS-8", "message", "kaput")), s.get("failed"));
		assertEquals("k", s.get("owner"));
		assertEquals(List.of("destroyed", "cleared"), s.get("afterDestroy"), "destroyed, then dispose() clears card:k");
		assertNull(s.get("retainedAfter"));
		assertEquals(List.of("render:k", "destroy:k"), s.get("log"));
	}

	@Test void a04_implicitTopicsAreClaimed() {
		var s = scenario("s3");
		assertEquals("t", s.get("selectionOwner"));
		assertEquals("t", s.get("redrawOwner"));
		assertEquals(false, s.get("accepted"));
		assertEquals(true, s.get("refused"), "E-JS-49 is logged");
		assertEquals(List.of(), s.get("banner"), "a page-level E-JS-49 has no card to paint on");
	}

	@Test void a05_cmdOps() {
		var s = scenario("s4");
		assertEquals(List.of("render:g", "refresh:g", "op:reload:g:true"), s.get("log"));
		assertEquals(List.of("E-JS-48"), s.get("inline"));
		assertEquals("card 'g' (type 'grid') has no op 'explode'", s.get("inlineText"));
		assertEquals(List.of(), s.get("banner"));
	}

	@Test void a06_customRoleMapAndWhenEmpty() {
		var s = scenario("s5");
		assertEquals(List.of("render:m1", "render:m2", "render:m3", "hl:m1:{\"hot\":3}", "hl:m2:{\"hot\":3}"), s.get("atMount"),
			"custom roles replay the retained value; refresh does not");
		assertEquals(List.of(
			"hl:m1:{\"hot\":7}", "hl:m2:{\"hot\":7}", "refresh:m3",
			"hl:m1:null",          // {other:1}: the mapped path is missing; whenEmpty clear calls with null, keep skips
			"hl:m1:null",          // the clear
			"refresh:m3"           // C2's refreshCard coalesces the two refreshes that arrived while one ran
		), s.get("after"));
	}

	@Test void a07_params() {
		var s = scenario("s6");
		assertEquals(Map.of("text", "Select a change.", "role", "status", "defaultText", "Nothing selected.",
			"card", "pending", "title", "Tasks"), s.get("awaiting"));
		assertEquals(Map.of("log", List.of("render:/api/changes/c%2017/tasks"), "body", "body:tasks", "empty", true,
			"card", "mounted"), s.get("first"));
		assertEquals(List.of("refresh:tasks"), s.get("same"), "same URL: no re-render; cmd refresh still refreshes");
		assertEquals(List.of("destroy:tasks", "render:/api/changes/c%2018/tasks"), s.get("changed"));
		assertEquals(List.of("destroy:tasks"), s.get("cleared"));
		assertEquals("Select a change.", s.get("clearedText"));
		assertEquals("pending", s.get("cardAfterClear"));
		assertEquals(List.of("E-JS-51", "E-JS-51", "E-JS-51"), s.get("plainInline"), "x <- ids is an array, once per message");
	}

	@Test void a08_ctxCallsBeforeReadyAreBuffered() {
		var s = scenario("s7");
		assertEquals(List.of("p"), s.get("ping"), "published once, as the card's owner");
		assertEquals(Map.of("n", 1), s.get("value"));
		assertEquals(List.of(Map.of("n", 2)), s.get("heard"));
		assertEquals(List.of(), s.get("banner"));
	}

	@Test void a09_painterRouting() {
		var s = scenario("s8");
		assertEquals(List.of("E-JS-52", "E-JS-53", "E-JS-58"), s.get("banner"),
			"validate's E-JS-52, a banner code, and detail.banner");
		assertEquals(List.of("E-JS-44"), s.get("inlineV"), "E-JS-45 stays console-only");
		assertEquals(List.of("E-JS-47"), s.get("inlineH"));
		assertEquals(List.of("E-JS-52", "E-JS-53", "E-JS-56", "E-JS-57"), s.get("exported"));
		assertEquals(true, s.get("frozen"));
	}

	@Test void a11_shellLoadedBeforeBusIsBuffered() {
		var s = scenario("s10");
		assertNull(s.get("threw"), "mounting before juneau-bus.js exists must not touch the bus");
		assertNull(s.get("readyThrew"));
		assertEquals(true, s.get("busLoaded"));
		assertEquals(List.of("p"), s.get("ping"), "buffered publish ran once, as the card's owner");
		assertEquals(Map.of("n", 1), s.get("value"));
		assertEquals(List.of(Map.of("n", 2)), s.get("heard"));
		assertEquals("mounted", s.get("card"));
		assertEquals(List.of(), s.get("banner"));
		assertEquals(List.of(), s.get("errors"));
	}

	@Test void a10_undeclaredScriptTopicIsE41() {
		var s = scenario("s9");
		assertEquals(List.of("E-JS-41"), s.get("inline"));
		assertEquals("card 'w' subscribes to 'app.region-picked', which is declared publisher=script, but no page script declared it by DOMContentLoaded",
			s.get("text"));
		assertEquals(List.of(), s.get("banner"));
	}

	@Test void a12_destroyedBeforeReadyNothingBufferedRuns() {
		var s = scenario("s11");
		assertEquals(0, s.get("ping"), "the dead card's buffered publish must not run");
		assertEquals(0, s.get("heard"), "nor its buffered subscribe");
		assertEquals(List.of(), s.get("banner"));
	}

	@Test void a13_afterDestroyNothingOfTheCardIsLive() {
		var s = scenario("s12");
		assertEquals(List.of(), s.get("after"), "cmd:<id> and the role topic go quiet");
		assertEquals(0, s.get("ctxHeard"), "a ctx.subscribe topic goes quiet");
	}

	@Test void a14_asyncRenderAfterDestroyDoesNotLeak() {
		var s = scenario("s13");
		assertEquals(0, s.get("ping"));
		assertEquals(0, s.get("heard"));
		assertEquals(List.of(0), s.get("pongSubs"), "no live subscription is left behind");
	}

	@Test void a15_throwingImplicitDoesNotAbortWiring() {
		var s = scenario("s14");
		assertNull(s.get("readyThrew"));
		assertEquals("mounted", s.get("okCard"));
		assertEquals("ok", s.get("selectionOwner"));
		assertEquals(1, s.get("errors"), "reported once, by wiring");
	}

	@Test void a16_prototypeMemberIsNotARole() {
		var s = scenario("s15");
		assertEquals(List.of(0), s.get("subscribers"));
	}

	@Test void a17_throwingPhasePublishStillDispatchesTheDomEvent() {
		var s = scenario("s16");
		assertNull(s.get("threw"));
		assertEquals(List.of("mounted:k"), s.get("events"));
	}

	@Test void a18_staleRenderAndAwaitingRefresh() {
		var s = scenario("s17");
		assertEquals("pending", s.get("phaseAwaiting"));
		assertEquals(0, s.get("staleEvents"), "a stale render must not announce card-mounted");
		assertEquals("pending", s.get("phaseAfter"));
		assertEquals(List.of(), s.get("refreshLog"), "refresh while awaiting params is a no-op");
		assertNull(s.get("threw"));
	}
}
