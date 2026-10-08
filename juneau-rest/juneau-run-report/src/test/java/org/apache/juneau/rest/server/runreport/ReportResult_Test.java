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
package org.apache.juneau.rest.server.runreport;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.runreport.RunEvent.*;
import org.junit.jupiter.api.*;

class ReportResult_Test extends TestBase {

	private static ReportTest test(String name) {
		return new ReportTest("s", name, TestStatus.PASS, 5L, null, null);
	}

	private static List<String> kinds(List<?> events) {
		return events.stream().map(e -> ((org.apache.juneau.rest.server.runreport.RunEvent)e).kind().name().toLowerCase(Locale.ROOT)).toList();
	}

	@Test void b01_toEventsOrder() {
		var r = new ReportResult("jest", List.of(test("t1")), List.of("w1"), false, 0);
		var ev = r.toEvents("tests");
		assertEquals(List.of("replace", "test", "note"), kinds(ev));
		assertEquals("tests", ev.get(0).toContractMap().get("step"));
		assertEquals("warn", ev.get(2).toContractMap().get("level"));
		assertEquals("tests", ev.get(2).toContractMap().get("step"));
	}

	@Test void b02_noTestsAndWarningsKeepsPlaceholders() {
		var ev = new ReportResult("jest", List.of(), List.of("w"), false, 0).toEvents("tests");
		assertEquals(1, ev.size());
		assertEquals("note", ev.get(0).toContractMap().get("ev"));
	}

	@Test void b03_noTestsNoWarningsStillReplaces() {
		assertEquals(List.of("replace"), kinds(new ReportResult("jest", List.of(), List.of(), false, 0).toEvents("tests")));
	}

	@Test void b04_testsAreBoundToStepAndFw() {
		var e = new ReportResult("jest", List.of(test("t1")), List.of(), false, 0).toEvents("tests").get(1).toContractMap();
		assertEquals("tests", e.get("step"));
		assertEquals("jest", e.get("fw"));
		assertEquals(5L, e.get("ms"));
	}

	@Test void b05_emptyNamesAndOverlongTextAreRepaired() {
		var t = new ReportTest("", null, TestStatus.FAIL, -1L, "m".repeat(5000), "t".repeat(20000));
		var e = new ReportResult("jest", List.of(t), List.of(), false, 0).toEvents("tests").get(1).toContractMap();
		assertEquals("(unnamed)", e.get("suite"));
		assertEquals("(unnamed)", e.get("name"));
		assertFalse(e.containsKey("ms"));
		assertEquals(2000, ((String)e.get("msg")).length());
		assertEquals(8000, ((String)e.get("trace")).length());
	}

	@Test void b06_extremeControlCharsShedTrace() {
		var ctl = "\u0001";
		var t = new ReportTest(ctl.repeat(512), ctl.repeat(512), TestStatus.FAIL, null, ctl.repeat(2000), ctl.repeat(8000));
		var ev = new ReportResult("jest", List.of(t), List.of(), false, 0).toEvents("tests");
		assertDoesNotThrow(() -> ev.get(1).validate());
		assertFalse(ev.get(1).toContractMap().containsKey("trace"));
	}

	@Test void b07_fwGrammar() {
		assertThrows(IllegalArgumentException.class, () -> new ReportResult("Jest", List.of(), List.of(), false, 0));
	}

	@Test void b08_limitsMustBePositive() {
		assertThrows(IllegalArgumentException.class, () -> new ReportLimits(0, 1, 1, 1, 1));
		assertEquals(100_000, ReportLimits.DEFAULT.maxTests());
	}
}
