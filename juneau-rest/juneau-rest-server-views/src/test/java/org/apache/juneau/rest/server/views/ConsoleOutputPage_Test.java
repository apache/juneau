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
import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.views.ConsoleOutputLine.*;
import org.junit.jupiter.api.*;

class ConsoleOutputPage_Test extends TestBase {

	private static final Instant NOW = Instant.parse("2026-10-07T13:01:23.456Z");

	private static void assertJson(String expected, Object actual) {
		assertEquals(Json.to(expected.replace('\'', '"'), Map.class), Json.to(Json.of(actual), Map.class));
	}

	private static ConsoleOutputLine line(long n) {
		return ConsoleOutputLine.info("l" + n).n(n);
	}

	@Test void a01_forwardContractMap() {
		var p = ConsoleOutputPage.forward().lines(List.of(line(41), line(42))).next("42").before("40")
			.status("RUNNING", Style.ACCENT, false, Instant.parse("2026-10-07T13:00:00Z"), null).now(NOW);
		assertJson("{'contractVersion':'1','lines':[{'n':41,'level':'INFO','text':'l41'},{'n':42,'level':'INFO','text':'l42'}],"
			+ "'next':'42','more':false,'hasEarlier':true,'before':'40','state':'RUNNING','stateStyle':'accent','terminal':false,"
			+ "'startedAt':'2026-10-07T13:00:00.000Z','now':'2026-10-07T13:01:23.456Z'}", p.validate().toContractMap());
	}

	@Test void a02_earlierOmitsNext() {
		var p = ConsoleOutputPage.earlier().lines(List.of(line(1))).now(NOW);
		assertJson("{'contractVersion':'1','lines':[{'n':1,'level':'INFO','text':'l1'}],'more':false,'hasEarlier':false,"
			+ "'terminal':false,'now':'2026-10-07T13:01:23.456Z'}", p.validate().toContractMap());
	}

	@Test void a03_terminalWithDuration() {
		var p = ConsoleOutputPage.forward().next("0").status("DONE", Style.SUCCESS, true, null, 83_000L).now(NOW);
		assertJson("{'contractVersion':'1','lines':[],'next':'0','more':false,'hasEarlier':false,'state':'DONE',"
			+ "'stateStyle':'success','terminal':true,'durationMs':83000,'now':'2026-10-07T13:01:23.456Z'}", p.validate().toContractMap());
	}

	@Test void a04_nowDefaultsToClock() {
		var p = ConsoleOutputPage.forward().next("0").validate();
		assertNotNull(p.now);
	}

	@Test void b01_strictlyIncreasing() {
		assertThrowsWithMessage(IllegalArgumentException.class, "strictly increasing",
			() -> ConsoleOutputPage.forward().next("2").lines(List.of(line(2), line(2))).validate());
		assertThrowsWithMessage(IllegalArgumentException.class, "strictly increasing",
			() -> ConsoleOutputPage.forward().next("2").lines(List.of(line(3), line(2))).validate());
		assertDoesNotThrow(() -> ConsoleOutputPage.forward().next("9").lines(List.of(line(2), line(9))).validate());
	}

	@Test void b02_lineNumbersRequired() {
		assertThrowsWithMessage(IllegalArgumentException.class, "lines[0].n is required",
			() -> ConsoleOutputPage.forward().next("1").lines(List.of(ConsoleOutputLine.info("x"))).validate());
		var withNull = new ArrayList<ConsoleOutputLine>();
		withNull.add(null);
		assertThrowsWithMessage(IllegalArgumentException.class, "lines[0] is null",
			() -> ConsoleOutputPage.forward().next("1").lines(withNull).validate());
	}

	@Test void b03_nextRules() {
		assertThrowsWithMessage(IllegalArgumentException.class, "forward page needs next",
			() -> ConsoleOutputPage.forward().validate());
		assertThrowsWithMessage(IllegalArgumentException.class, "earlier page must not carry next",
			() -> ConsoleOutputPage.earlier().next("1").validate());
		assertThrowsWithMessage(IllegalArgumentException.class, "next is not a valid token",
			() -> ConsoleOutputPage.forward().next("a b").validate());
	}

	@Test void b04_moreRules() {
		assertThrowsWithMessage(IllegalArgumentException.class, "more must be false when lines is empty",
			() -> ConsoleOutputPage.forward().next("0").more(true).validate());
		assertThrowsWithMessage(IllegalArgumentException.class, "more must be false on a terminal page",
			() -> ConsoleOutputPage.forward().next("1").lines(List.of(line(1))).more(true).status("DONE", null, true, null, null).validate());
		assertThrowsWithMessage(IllegalArgumentException.class, "more must be false on an earlier page",
			() -> ConsoleOutputPage.earlier().lines(List.of(line(1))).more(true).validate());
	}

	@Test void b05_beforeRules() {
		var p = ConsoleOutputPage.forward().next("0");
		p.hasEarlier = true;
		assertThrowsWithMessage(IllegalArgumentException.class, "before must be present exactly when hasEarlier", p::validate);
		var q = ConsoleOutputPage.forward().next("0");
		q.before = "3";
		assertThrowsWithMessage(IllegalArgumentException.class, "before must be present exactly when hasEarlier", q::validate);
		assertThrowsWithMessage(IllegalArgumentException.class, "before is not a valid token",
			() -> ConsoleOutputPage.forward().next("0").before("a/b").validate());
	}

	@Test void b06_durationOnlyWhenTerminal() {
		assertThrowsWithMessage(IllegalArgumentException.class, "durationMs is only allowed on a terminal page",
			() -> ConsoleOutputPage.forward().next("0").status("RUNNING", null, false, null, 5L).validate());
		assertThrowsWithMessage(IllegalArgumentException.class, "durationMs must be >= 0",
			() -> ConsoleOutputPage.forward().next("0").status("DONE", null, true, null, -1L).validate());
	}

	@Test void b07_linesAreValidated() {
		assertThrowsWithMessage(IllegalArgumentException.class, "ui.color",
			() -> ConsoleOutputPage.forward().next("1").lines(List.of(line(1).color("red"))).validate());
	}

	@Test void c01_visibleChars() {
		assertEquals(3, ConsoleOutputPage.visibleChars(ConsoleOutputLine.info("abc")));
		assertEquals(3, ConsoleOutputPage.visibleChars(ConsoleOutputLine.frags(Frag.text("ab"), Frag.block(), Frag.text("c"))));
		assertEquals(0, ConsoleOutputPage.visibleChars(ConsoleOutputLine.info("")));
	}

	@Test void c02_constants() {
		assertEquals("1", ConsoleOutputPage.CONTRACT_VERSION);
		assertEquals(8_388_608, ConsoleOutputPage.MAX_PAGE_CHARS);
	}
}
