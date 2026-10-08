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

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.views.RunEvent.*;
import org.junit.jupiter.api.*;

class RunViewPage_Test extends TestBase {

	private static RunEvent ev(long seq) {
		return RunEvent.step("a", "T").withSeq(seq);
	}

	@Test void a01_contractMapShape() {
		var p = RunViewPage.of(List.of(RunEvent.step("a", "T").withSeq(1)), "k3x9ab.1", false, false).validate();
		assertEquals(List.of("contractVersion", "events", "next", "more", "terminal"), new ArrayList<>(p.toContractMap().keySet()));
		assertEquals("1", p.toContractMap().get("contractVersion"));
		assertEquals("k3x9ab.1", p.next());
		assertEquals(1, p.events().size());
		assertFalse(p.more());
		assertFalse(p.terminal());
	}

	@Test void b01_seqMustAscend() {
		assertThrowsWithMessage(IllegalArgumentException.class, "ascending", () -> RunViewPage.of(List.of(ev(2), ev(2)), "a.2", false, false).validate());
		assertThrowsWithMessage(IllegalArgumentException.class, "ascending", () -> RunViewPage.of(List.of(ev(3), ev(2)), "a.2", false, false).validate());
	}

	@Test void b02_unnumberedEventRejected() {
		assertThrowsWithMessage(IllegalArgumentException.class, "seq", () -> RunViewPage.of(List.of(RunEvent.step("a", "T")), "a.1", false, false).validate());
	}

	@Test void b03_tokenGrammar() {
		assertThrowsWithMessage(IllegalArgumentException.class, "next", () -> RunViewPage.of(List.of(), "a b", false, false).validate());
		assertThrowsWithMessage(IllegalArgumentException.class, "next", () -> RunViewPage.of(List.of(), null, false, false).validate());
	}

	@Test void b04_eventCap() {
		var l = new ArrayList<RunEvent>();
		for (var i = 1; i <= RunViewPage.MAX_PAGE_EVENTS + 1; i++)
			l.add(ev(i));
		assertThrowsWithMessage(IllegalArgumentException.class, "10000", () -> RunViewPage.of(l, "a.1", false, false).validate());
	}

	@Test void b05_charCap() {
		var ctl = "\u0001";
		var l = new ArrayList<RunEvent>();
		for (var i = 1; i <= 150; i++)
			l.add(RunEvent.test("a", "jest", "s", "n", TestStatus.FAIL).withMsg(ctl.repeat(2000)).withTrace(ctl.repeat(8000)).withSeq(i));
		assertThrowsWithMessage(IllegalArgumentException.class, "chars", () -> RunViewPage.of(l, "a.1", false, false).validate());
	}

	@Test void b06_moreRequiresEvents() {
		assertThrowsWithMessage(IllegalArgumentException.class, "more", () -> RunViewPage.of(List.of(), "a.0", true, false).validate());
	}

	@Test void c01_emptyPageStillHasNext() {
		var p = RunViewPage.of(List.of(), "a.0", false, true).validate();
		assertEquals("a.0", p.toContractMap().get("next"));
		assertEquals(true, p.toContractMap().get("terminal"));
	}

	@Test void c02_defensiveCopy() {
		var l = new ArrayList<>(List.of(ev(1)));
		var p = RunViewPage.of(l, "a.1", false, false);
		l.clear();
		assertEquals(1, p.events().size());
	}
}
