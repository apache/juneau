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

import static org.apache.juneau.BasicTestUtils.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.views.RunEvent.*;
import org.junit.jupiter.api.*;

class RunEvent_Test extends TestBase {

	private static final RunEvent STEP_EVENT = RunEvent.step("tests", "mvn test").withN(4).withState(StepState.WAITING).withRawLine(12).withSeq(1);
	private static final RunEvent END_EVENT = RunEvent.end("tests", EndStatus.FAIL).withMs(1200).withExit(1).withSeq(2);
	private static final RunEvent SUITE_EVENT = RunEvent.suite("tests", "jest", "src/a.test.js", 3, 1, 0).withRawLine(5).withSeq(3);
	private static final RunEvent TEST_EVENT = RunEvent.test("tests", "jest", "src/a.test.js", "adds", TestStatus.FAIL)
		.withMs(7).withMsg("expected 1").withTrace("at a.js:1").withRawLine(9).withSeq(4);
	private static final RunEvent REPLACE_EVENT = RunEvent.replace("tests").withSeq(5);
	private static final RunEvent NOTE_EVENT = RunEvent.note(Level.WARN, "slow").withHref("https://example.org/x").withStep("tests").withSeq(6);
	private static final RunEvent DONE_EVENT = RunEvent.done(DoneStatus.CANCELLED).withSeq(7);

	@Test void a01_stepFactory() {
		var e = RunEvent.step("tests", "mvn test").withN(4).withRawLine(12);
		assertEquals(Map.of("ev", "step", "id", "tests", "title", "mvn test", "n", 4, "rawLine", 12), e.toContractMap());
	}

	@Test void a02_numberedCopyIncludesSeq() {
		var e = RunEvent.done(DoneStatus.OK).withSeq(7);
		assertEquals(Kind.DONE, e.kind());
		assertEquals(7L, e.seq());
		assertEquals(0L, RunEvent.done(DoneStatus.OK).seq());
		assertEquals(7L, e.toContractMap().get("seq"));
		assertFalse(RunEvent.done(DoneStatus.OK).toContractMap().containsKey("seq"));
	}

	@Test void a03_waitingStatus() {
		assertEquals("waiting", RunEvent.step("gate", "Vote").withState(StepState.WAITING).toContractMap().get("status"));
		assertEquals("running", RunEvent.step("gate", "Vote").withState(StepState.RUNNING).toContractMap().get("status"));
		assertFalse(RunEvent.step("gate", "Vote").toContractMap().containsKey("status"));
	}

	@Test void a04_immutable() {
		var a = RunEvent.step("a", "T");
		var b = a.withN(2);
		assertFalse(a.toContractMap().containsKey("n"));
		assertEquals(2, b.toContractMap().get("n"));
	}

	@Test void b01_stepIdGrammar() {
		assertThrowsWithMessage(IllegalArgumentException.class, "RunEvent step id", () -> RunEvent.step("_x", "t"));
		assertThrowsWithMessage(IllegalArgumentException.class, "RunEvent step title", () -> RunEvent.step("a", ""));
		assertThrowsWithMessage(IllegalArgumentException.class, "RunEvent step title", () -> RunEvent.step("a", "x".repeat(201)));
		assertThrowsWithMessage(IllegalArgumentException.class, "RunEvent end id", () -> RunEvent.end("a b", EndStatus.OK));
	}

	@Test void b02_ranges() {
		var s = RunEvent.step("a", "T");
		assertThrowsWithMessage(IllegalArgumentException.class, "n must be", () -> s.withN(0));
		assertThrowsWithMessage(IllegalArgumentException.class, "n must be", () -> s.withN(10000));
		assertThrowsWithMessage(IllegalArgumentException.class, "rawLine", () -> s.withRawLine(0));
		var e = RunEvent.end("a", EndStatus.OK);
		assertThrowsWithMessage(IllegalArgumentException.class, "ms must be", () -> e.withMs(-1));
		assertThrowsWithMessage(IllegalArgumentException.class, "ms must be", () -> e.withMs(RunEvent.MAX_SAFE_INT + 1));
		assertDoesNotThrow(() -> e.withMs(RunEvent.MAX_SAFE_INT));
		assertThrowsWithMessage(IllegalArgumentException.class, "counts.pass", () -> RunEvent.suite("a", "jest", "s", -1, 0, 0));
		assertThrowsWithMessage(IllegalArgumentException.class, "counts.skip", () -> RunEvent.suite("a", "jest", "s", 0, 0, 1_000_000_001));
		assertThrowsWithMessage(IllegalArgumentException.class, "seq", () -> s.withSeq(-1));
	}

	@Test void b03_clipsRejected() {
		assertThrowsWithMessage(IllegalArgumentException.class, "test name", () -> RunEvent.test("a", "jest", "s", "n".repeat(513), TestStatus.PASS));
		var t = RunEvent.test("a", "jest", "s", "n", TestStatus.PASS);
		assertThrowsWithMessage(IllegalArgumentException.class, "test msg", () -> t.withMsg("m".repeat(2001)));
		assertThrowsWithMessage(IllegalArgumentException.class, "test trace", () -> t.withTrace("m".repeat(8001)));
		assertThrowsWithMessage(IllegalArgumentException.class, "note text", () -> RunEvent.note(Level.INFO, "m".repeat(1001)));
		assertThrowsWithMessage(IllegalArgumentException.class, "note text", () -> RunEvent.note(Level.INFO, ""));
	}

	@Test void b04_fwGrammar() {
		assertThrowsWithMessage(IllegalArgumentException.class, "fw", () -> RunEvent.suite("a", "Jest", "s", 0, 0, 0));
		assertThrowsWithMessage(IllegalArgumentException.class, "fw", () -> RunEvent.test("a", "", "s", "n", TestStatus.PASS));
	}

	@Test void b05_noteHref() {
		var n = RunEvent.note(Level.INFO, "x");
		assertThrowsWithMessage(IllegalArgumentException.class, "note href", () -> n.withHref("javascript:alert(1)"));
		assertDoesNotThrow(() -> n.withHref("/runs/42"));
	}

	@Test void b06_eventSizeCap() {
		var ctl = "\u0001";
		var e = RunEvent.test("a", "jest", ctl.repeat(512), ctl.repeat(512), TestStatus.FAIL).withMsg(ctl.repeat(2000)).withTrace(ctl.repeat(8000));
		assertThrowsWithMessage(IllegalArgumentException.class, "serialized size", e::validate);
		assertSame(TEST_EVENT, TEST_EVENT.validate());
	}

	@Test void b07_memberNotOnKind() {
		assertThrowsWithMessage(IllegalArgumentException.class, "does not have", () -> RunEvent.done(DoneStatus.OK).withN(1));
	}

	@Test void c01_fromMapRoundTripsEveryKind() {
		for (var e : List.of(STEP_EVENT, END_EVENT, SUITE_EVENT, TEST_EVENT, REPLACE_EVENT, NOTE_EVENT, DONE_EVENT)) {
			assertEquals(e.toContractMap(), RunEvent.fromMap(e.toContractMap()).toContractMap());
			assertEquals(e, RunEvent.fromMap(e.toContractMap()));
		}
	}

	@Test void c02_fromMapIgnoresUnknownMembers() {
		var m = STEP_EVENT.toContractMap();
		m.put("x", 1);
		assertEquals(STEP_EVENT.toContractMap(), RunEvent.fromMap(m).toContractMap());
	}

	@Test void c03_fromMapUnknownEvThrows() {
		assertThrowsWithMessage(IllegalArgumentException.class, "RunEvent ev", () -> RunEvent.fromMap(Map.of("ev", "bogus")));
		assertThrowsWithMessage(IllegalArgumentException.class, "RunEvent ev", () -> RunEvent.fromMap(Map.of()));
	}

	@Test void c04_fromMapMissingRequiredThrows() {
		assertThrowsWithMessage(IllegalArgumentException.class, "step title", () -> RunEvent.fromMap(Map.of("ev", "step", "id", "a")));
		assertThrowsWithMessage(IllegalArgumentException.class, "suite counts", () -> RunEvent.fromMap(Map.of("ev", "suite", "step", "a", "fw", "jest", "suite", "s")));
	}

	@Test void c05_keyOrder() {
		assertEquals(List.of("ev", "seq", "id", "title"), new ArrayList<>(RunEvent.step("a", "T").withSeq(3).toContractMap().keySet()));
		assertEquals(List.of("ev", "seq", "step", "fw", "suite", "name", "status", "ms", "msg", "trace", "rawLine"),
			new ArrayList<>(TEST_EVENT.toContractMap().keySet()));
	}

	@Test void c06_fromMapNumbers() {
		assertEquals(5L, RunEvent.fromMap(Map.of("ev", "done", "status", "ok", "seq", 5.0)).seq());
		assertThrowsWithMessage(IllegalArgumentException.class, "seq must be an integer", () -> RunEvent.fromMap(Map.of("ev", "done", "status", "ok", "seq", 5.5)));
		assertThrowsWithMessage(IllegalArgumentException.class, "status", () -> RunEvent.fromMap(Map.of("ev", "done", "status", "bogus")));
	}

	@Test void c07_countsAreCopied() {
		var m = SUITE_EVENT.toContractMap();
		@SuppressWarnings("unchecked")
		var counts = (Map<String,Object>)m.get("counts");
		counts.put("pass", 99);
		assertBean(SUITE_EVENT.toContractMap(), "counts", "{pass=3,fail=1,skip=0}");
	}
}
