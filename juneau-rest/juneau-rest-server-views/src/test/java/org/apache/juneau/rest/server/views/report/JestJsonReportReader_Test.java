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
package org.apache.juneau.rest.server.views.report;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.apache.juneau.rest.server.views.report.ReportTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;

class JestJsonReportReader_Test extends TestBase {

	@Test void a01_mapping() {
		var r = new JestJsonReportReader().read(resource("jest-mixed.json"));
		assertEquals(List.of("pass", "skip", "fail"), statuses(r));
		assertBean(r.tests().get(2), "suite,msg", "b.test.js,expected 1");
		assertEquals("jest", r.fw());
		assertEquals(4L, r.tests().get(0).ms());
		assertNull(r.tests().get(1).ms());
	}

	@Test void a02_traceJoinsAllFailureMessages() {
		var t = new JestJsonReportReader().read(resource("jest-mixed.json")).tests().get(2);
		assertEquals("expected 1\nat x\nsecond\nat y", t.trace());
	}

	@Test void a03_nameIsFullNameElseAncestorsPlusTitle() {
		var r = new JestJsonReportReader().read(resource("jest-mixed.json"));
		assertEquals("math adds", r.tests().get(0).name());
		assertEquals("api get returns 200", r.tests().get(2).name());
	}

	@Test void a04_singleFileKeepsLastTwoSegments() {
		var r = new JestJsonReportReader().read(resource("jest-crashed.json"));
		assertEquals("src/c.test.js", r.tests().get(0).suite());
	}

	@Test void b01_crashedFileIsOneErrorTest() {
		var r = new JestJsonReportReader().read(resource("jest-crashed.json"));
		assertEquals(1, r.tests().size());
		assertBean(r.tests().get(0), "name,status,msg", "(suite failed to run),ERROR,Cannot find module './x'");
	}

	@Test void b02_truncatedIsZeroTestsWithWarning() {
		var r = new JestJsonReportReader().read(resource("jest-truncated.json"));
		assertTrue(r.tests().isEmpty());
		assertTrue(r.warnings().get(0).startsWith("report unreadable"), r.warnings().get(0));
		assertTrue(r.warnings().get(0).endsWith("(jest-truncated.json)"), r.warnings().get(0));
	}

	@Test void b03_oversizeSkipped() {
		var r = new JestJsonReportReader().read(resource("jest-mixed.json"), new ReportLimits(10, 100, 512, 2000, 8000));
		assertEquals(1, r.skippedFiles());
		assertTrue(r.tests().isEmpty());
	}

	@Test void b04_twelveThousandAssertions(@TempDir Path d) throws Exception {
		var sb = new StringBuilder("{\"testResults\":[{\"name\":\"/w/a.test.js\",\"status\":\"failed\",\"assertionResults\":[");
		for (var i = 0; i < 12_000; i++) {
			if (i > 0)
				sb.append(',');
			sb.append("{\"title\":\"t").append(i).append("\",\"status\":\"").append(i % 3 == 0 ? "failed" : i % 3 == 1 ? "passed" : "pending").append("\",\"duration\":1,\"failureMessages\":[]}");
		}
		var f = d.resolve("big.json");
		Files.writeString(f, sb.append("]}]}").toString());
		var r = new JestJsonReportReader().read(f);
		assertEquals(12_000, r.tests().size());
		assertEquals(4000, statuses(r).stream().filter("fail"::equals).count());
	}

	@Test void b05_notAnObject(@TempDir Path d) throws Exception {
		var f = d.resolve("x.json");
		for (var text : new String[] {"[]", "", "42"}) {
			Files.writeString(f, text);
			var r = new JestJsonReportReader().read(f);
			assertTrue(r.tests().isEmpty(), text);
			assertTrue(r.warnings().get(0).startsWith("report unreadable"), text + ": " + r.warnings());
		}
	}

	@Test void b06_maxTests(@TempDir Path d) throws Exception {
		var r = new JestJsonReportReader().read(resource("jest-mixed.json"), new ReportLimits(1 << 20, 2, 512, 2000, 8000));
		assertEquals(2, r.tests().size());
		assertTrue(r.truncated());
		assertEquals(List.of("report truncated at 2 tests"), r.warnings());
	}
}
