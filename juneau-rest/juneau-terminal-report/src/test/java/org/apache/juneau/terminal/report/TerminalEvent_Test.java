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
package org.apache.juneau.terminal.report;

import static org.apache.juneau.BasicTestUtils.*;
import static org.apache.juneau.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.terminal.report.TerminalEvent.*;
import org.junit.jupiter.api.*;

class TerminalEvent_Test extends TestBase {

	private static final TerminalEvent STEP_EVENT = TerminalEvent.step("tests", "mvn test").withN(4).withState(StepState.WAITING).withRawOffset(120).withSeq(1);
	private static final TerminalEvent END_EVENT = TerminalEvent.end("tests", EndStatus.FAIL).withMs(1200).withExit(1).withSeq(2);
	private static final TerminalEvent SUITE_EVENT = TerminalEvent.suite("tests", "jest", "src/a.test.js", 3, 1, 0).withRawOffset(50).withSeq(3);
	private static final TerminalEvent TEST_EVENT = TerminalEvent.test("tests", "jest", "src/a.test.js", "adds", TestStatus.FAIL)
		.withMs(7).withMsg("expected 1").withTrace("at a.js:1").withRawOffset(90).withSeq(4);
	private static final TerminalEvent REPLACE_EVENT = TerminalEvent.replace("tests").withSeq(5);
	private static final TerminalEvent NOTE_EVENT = TerminalEvent.note(Level.WARN, "slow").withHref("https://example.org/x").withStep("tests").withSeq(6);
	private static final TerminalEvent DONE_EVENT = TerminalEvent.done(DoneStatus.CANCELLED).withSeq(7);

	@Test void a01_stepFactory() {
		var e = TerminalEvent.step("tests", "mvn test").withN(4).withRawOffset(12);
		assertEquals(Map.of("ev", "step", "id", "tests", "title", "mvn test", "n", 4, "rawOffset", 12L), e.toContractMap());
	}

	@Test void a02_numberedCopyIncludesSeq() {
		var e = TerminalEvent.done(DoneStatus.OK).withSeq(7);
		assertEquals(Kind.DONE, e.kind());
		assertEquals(7L, e.seq());
		assertEquals(0L, TerminalEvent.done(DoneStatus.OK).seq());
		assertEquals(7L, e.toContractMap().get("seq"));
		assertFalse(TerminalEvent.done(DoneStatus.OK).toContractMap().containsKey("seq"));
	}

	@Test void a03_waitingStatus() {
		assertEquals("waiting", TerminalEvent.step("gate", "Vote").withState(StepState.WAITING).toContractMap().get("status"));
		assertEquals("running", TerminalEvent.step("gate", "Vote").withState(StepState.RUNNING).toContractMap().get("status"));
		assertFalse(TerminalEvent.step("gate", "Vote").toContractMap().containsKey("status"));
	}

	@Test void a04_immutable() {
		var a = TerminalEvent.step("a", "T");
		var b = a.withN(2);
		assertFalse(a.toContractMap().containsKey("n"));
		assertEquals(2, b.toContractMap().get("n"));
	}

	@Test void b01_stepIdGrammar() {
		assertThrowsWithMessage(IllegalArgumentException.class, "TerminalEvent step id", () -> TerminalEvent.step("_x", "t"));
		assertThrowsWithMessage(IllegalArgumentException.class, "TerminalEvent step title", () -> TerminalEvent.step("a", ""));
		assertThrowsWithMessage(IllegalArgumentException.class, "TerminalEvent step title", () -> TerminalEvent.step("a", "x".repeat(201)));
		assertThrowsWithMessage(IllegalArgumentException.class, "TerminalEvent end id", () -> TerminalEvent.end("a b", EndStatus.OK));
	}

	@Test void b02_ranges() {
		var s = TerminalEvent.step("a", "T");
		assertThrowsWithMessage(IllegalArgumentException.class, "n must be", () -> s.withN(0));
		assertThrowsWithMessage(IllegalArgumentException.class, "n must be", () -> s.withN(10000));
		assertThrowsWithMessage(IllegalArgumentException.class, "rawOffset", () -> s.withRawOffset(-1));
		var e = TerminalEvent.end("a", EndStatus.OK);
		assertThrowsWithMessage(IllegalArgumentException.class, "ms must be", () -> e.withMs(-1));
		assertThrowsWithMessage(IllegalArgumentException.class, "ms must be", () -> e.withMs(TerminalEvent.MAX_SAFE_INT + 1));
		assertDoesNotThrow(() -> e.withMs(TerminalEvent.MAX_SAFE_INT));
		assertThrowsWithMessage(IllegalArgumentException.class, "counts.pass", () -> TerminalEvent.suite("a", "jest", "s", -1, 0, 0));
		assertThrowsWithMessage(IllegalArgumentException.class, "counts.skip", () -> TerminalEvent.suite("a", "jest", "s", 0, 0, 1_000_000_001));
		assertThrowsWithMessage(IllegalArgumentException.class, "seq", () -> s.withSeq(-1));
	}

	@Test void b03_clipsRejected() {
		assertThrowsWithMessage(IllegalArgumentException.class, "test name", () -> TerminalEvent.test("a", "jest", "s", "n".repeat(513), TestStatus.PASS));
		var t = TerminalEvent.test("a", "jest", "s", "n", TestStatus.PASS);
		assertThrowsWithMessage(IllegalArgumentException.class, "test msg", () -> t.withMsg("m".repeat(2001)));
		assertThrowsWithMessage(IllegalArgumentException.class, "test trace", () -> t.withTrace("m".repeat(8001)));
		assertThrowsWithMessage(IllegalArgumentException.class, "note text", () -> TerminalEvent.note(Level.INFO, "m".repeat(1001)));
		assertThrowsWithMessage(IllegalArgumentException.class, "note text", () -> TerminalEvent.note(Level.INFO, ""));
	}

	@Test void b04_fwGrammar() {
		assertThrowsWithMessage(IllegalArgumentException.class, "fw", () -> TerminalEvent.suite("a", "Jest", "s", 0, 0, 0));
		assertThrowsWithMessage(IllegalArgumentException.class, "fw", () -> TerminalEvent.test("a", "", "s", "n", TestStatus.PASS));
	}

	@Test void b05_noteHref() {
		var n = TerminalEvent.note(Level.INFO, "x");
		assertThrowsWithMessage(IllegalArgumentException.class, "note href", () -> n.withHref("javascript:alert(1)"));
		assertDoesNotThrow(() -> n.withHref("/runs/42"));
	}

	@Test void b06_eventSizeCap() {
		var ctl = "\u0001";
		var e = TerminalEvent.test("a", "jest", ctl.repeat(512), ctl.repeat(512), TestStatus.FAIL).withMsg(ctl.repeat(2000)).withTrace(ctl.repeat(8000));
		assertThrowsWithMessage(IllegalArgumentException.class, "serialized size", e::validate);
		assertSame(TEST_EVENT, TEST_EVENT.validate());
	}

	@Test void b07_memberNotOnKind() {
		assertThrowsWithMessage(IllegalArgumentException.class, "does not have", () -> TerminalEvent.done(DoneStatus.OK).withN(1));
	}

	@Test void b08_rawOffset() {
		var s = TerminalEvent.step("a", "T").withRawOffset(0);
		assertEquals(0L, s.toContractMap().get("rawOffset"));
		assertEquals(TerminalEvent.MAX_SAFE_INT, TerminalEvent.suite("a", "jest", "s", 1, 0, 0).withRawOffset(TerminalEvent.MAX_SAFE_INT).toContractMap().get("rawOffset"));
		assertEquals(7L, TerminalEvent.test("a", "jest", "s", "n", TestStatus.PASS).withRawOffset(7).toContractMap().get("rawOffset"));
		assertThrowsWithMessage(IllegalArgumentException.class, "rawOffset must be 0-", () -> s.withRawOffset(-1));
		assertThrowsWithMessage(IllegalArgumentException.class, "rawOffset must be 0-", () -> s.withRawOffset(TerminalEvent.MAX_SAFE_INT + 1));
		assertThrowsWithMessage(IllegalArgumentException.class, "does not have", () -> TerminalEvent.end("a", EndStatus.OK).withRawOffset(1));
	}

	@Test void c01_fromMapRoundTripsEveryKind() {
		for (var e : List.of(STEP_EVENT, END_EVENT, SUITE_EVENT, TEST_EVENT, REPLACE_EVENT, NOTE_EVENT, DONE_EVENT)) {
			assertEquals(e.toContractMap(), TerminalEvent.fromMap(e.toContractMap()).toContractMap());
			assertEquals(e, TerminalEvent.fromMap(e.toContractMap()));
		}
	}

	@Test void c02_fromMapIgnoresUnknownMembers() {
		var m = STEP_EVENT.toContractMap();
		m.put("x", 1);
		assertEquals(STEP_EVENT.toContractMap(), TerminalEvent.fromMap(m).toContractMap());
	}

	@Test void c03_fromMapUnknownEvThrows() {
		assertThrowsWithMessage(IllegalArgumentException.class, "TerminalEvent ev", () -> TerminalEvent.fromMap(Map.of("ev", "bogus")));
		assertThrowsWithMessage(IllegalArgumentException.class, "TerminalEvent ev", () -> TerminalEvent.fromMap(Map.of()));
	}

	@Test void c04_fromMapMissingRequiredThrows() {
		assertThrowsWithMessage(IllegalArgumentException.class, "step title", () -> TerminalEvent.fromMap(Map.of("ev", "step", "id", "a")));
		assertThrowsWithMessage(IllegalArgumentException.class, "suite counts", () -> TerminalEvent.fromMap(Map.of("ev", "suite", "step", "a", "fw", "jest", "suite", "s")));
	}

	@Test void c05_keyOrder() {
		assertEquals(List.of("ev", "seq", "id", "title"), new ArrayList<>(TerminalEvent.step("a", "T").withSeq(3).toContractMap().keySet()));
		assertEquals(List.of("ev", "seq", "step", "fw", "suite", "name", "status", "ms", "msg", "trace", "rawOffset"),
			new ArrayList<>(TEST_EVENT.toContractMap().keySet()));
	}

	@Test void c06_fromMapNumbers() {
		assertEquals(5L, TerminalEvent.fromMap(Map.of("ev", "done", "status", "ok", "seq", 5.0)).seq());
		assertThrowsWithMessage(IllegalArgumentException.class, "seq must be an integer", () -> TerminalEvent.fromMap(Map.of("ev", "done", "status", "ok", "seq", 5.5)));
		assertThrowsWithMessage(IllegalArgumentException.class, "status", () -> TerminalEvent.fromMap(Map.of("ev", "done", "status", "bogus")));
	}

	@Test void c07_countsAreCopied() {
		var m = SUITE_EVENT.toContractMap();
		@SuppressWarnings({
			"unchecked" // Casts a parsed JSON value to its known Map/List shape.
		})
		var counts = (Map<String,Object>)m.get("counts");
		counts.put("pass", 99);
		assertBean(SUITE_EVENT.toContractMap(), "counts", "{pass=3,fail=1,skip=0}");
	}

	@Test void c08_rawOffsetRoundTripsAndOrders() {
		var step = TerminalEvent.step("a", "T").withRawOffset(120);
		var suite = TerminalEvent.suite("a", "jest", "s", 1, 0, 0).withRawOffset(5);
		var test = TerminalEvent.test("a", "jest", "s", "n", TestStatus.FAIL).withRawOffset(9);
		for (var e : List.of(step, suite, test))
			assertEquals(e, TerminalEvent.fromMap(e.toContractMap()));
		assertEquals(List.of("ev", "id", "title", "rawOffset"), new ArrayList<>(step.toContractMap().keySet()));
		assertEquals(List.of("ev", "step", "fw", "suite", "counts", "rawOffset"), new ArrayList<>(suite.toContractMap().keySet()));
		assertEquals(List.of("ev", "step", "fw", "suite", "name", "status", "rawOffset"), new ArrayList<>(test.toContractMap().keySet()));
		assertEquals(120L, TerminalEvent.fromMap(Map.of("ev", "step", "id", "a", "title", "T", "rawOffset", 120.0)).toContractMap().get("rawOffset"));
		assertThrowsWithMessage(IllegalArgumentException.class, "rawOffset", () -> TerminalEvent.fromMap(Map.of("ev", "step", "id", "a", "title", "T", "rawOffset", 1.5)));
	}
}
