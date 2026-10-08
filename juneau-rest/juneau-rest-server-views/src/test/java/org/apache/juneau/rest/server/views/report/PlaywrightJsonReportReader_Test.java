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

class PlaywrightJsonReportReader_Test extends TestBase {

	@Test void a01_mapping() {
		var r = new PlaywrightJsonReportReader().read(resource("playwright-mixed.json"));
		assertEquals(List.of("pass", "fail", "pass", "skip", "fail"), statuses(r));
		assertBean(r.tests().get(1), "suite,name", "a.spec.ts,Login › breaks [chromium]");
		assertEquals("Login › works", r.tests().get(0).name());
		assertEquals("playwright", r.fw());
	}

	@Test void a02_msgAnsiStrippedFirstLine() {
		var t = new PlaywrightJsonReportReader().read(resource("playwright-mixed.json")).tests().get(1);
		assertEquals("expected visible", t.msg());
		assertEquals("expected visible\nCall log: x", t.trace());
	}

	@Test void a03_durationIsSumOfResults() {
		assertEquals(80L, new PlaywrightJsonReportReader().read(resource("playwright-mixed.json")).tests().get(2).ms());
	}

	@Test void a04_statusFallsBackToLastResult() {
		var t = new PlaywrightJsonReportReader().read(resource("playwright-mixed.json")).tests().get(4);
		assertBean(t, "suite,name,status,msg", "b.spec.ts,hangs,FAIL,Test timeout of 30000ms exceeded.");
	}

	@Test void a05_flakyPassHasNoMessage() {
		assertNull(new PlaywrightJsonReportReader().read(resource("playwright-mixed.json")).tests().get(2).msg());
	}

	@Test void b01_truncated() {
		var r = new PlaywrightJsonReportReader().read(resource("playwright-truncated.json"));
		assertTrue(r.tests().isEmpty());
		assertTrue(r.warnings().get(0).startsWith("report unreadable"), r.warnings().get(0));
	}

	@Test void b02_twelveThousandTests(@TempDir Path d) throws Exception {
		var sb = new StringBuilder("{\"suites\":[{\"title\":\"a.spec.ts\",\"file\":\"a.spec.ts\",\"specs\":[");
		for (var i = 0; i < 12_000; i++) {
			if (i > 0)
				sb.append(',');
			sb.append("{\"title\":\"s").append(i).append("\",\"tests\":[{\"status\":\"expected\",\"results\":[{\"status\":\"passed\",\"duration\":1}]}]}");
		}
		var f = d.resolve("big.json");
		Files.writeString(f, sb.append("]}]}").toString());
		assertEquals(12_000, new PlaywrightJsonReportReader().read(f).tests().size());
	}

	@Test void b03_maxTests() {
		var r = new PlaywrightJsonReportReader().read(resource("playwright-mixed.json"), new ReportLimits(1 << 20, 2, 512, 2000, 8000));
		assertEquals(2, r.tests().size());
		assertTrue(r.truncated());
	}
}
