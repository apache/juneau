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

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.apache.juneau.rest.server.runreport.ReportTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;

class SurefireReportReader_Test extends TestBase {

	@Test void a01_directoryOfReports() {
		var r = new SurefireReportReader().read(resource("surefire-mixed"));
		assertEquals(List.of("pass", "fail", "error", "skip", "pass"), statuses(r));
		assertBean(r.tests().get(1), "suite,name,msg", "com.example.BetaTest,failsOnPurpose,expected 1 but was 2");
		assertEquals(12L, r.tests().get(0).ms());
		assertTrue(r.tests().get(1).trace().startsWith("java.lang.AssertionError"));
		assertTrue(r.warnings().isEmpty());
		assertFalse(r.truncated());
	}

	@Test void a02_suiteFallsBackToTestsuiteName(@TempDir Path d) throws Exception {
		Files.writeString(d.resolve("TEST-x.xml"), "<testsuite name=\"My Suite\"><testcase name=\"n\"/></testsuite>");
		assertBean(new SurefireReportReader().read(d).tests().get(0), "suite,name,status", "My Suite,n,PASS");
	}

	@Test void a03_noReportsWarns(@TempDir Path d) {
		var r = new SurefireReportReader().read(d);
		assertEquals(List.of("no TEST-*.xml files in " + d.getFileName()), r.warnings());
		assertTrue(r.tests().isEmpty());
	}

	@Test void a04_singleFileAndFw() {
		var r = new SurefireReportReader().read(resource("surefire-ok/TEST-com.example.AlphaTest.xml"));
		assertEquals(1, r.tests().size());
		assertEquals("surefire", r.fw());
	}

	@Test void b01_truncatedKeepsClosedTestcases() {
		var r = new JUnitXmlReportReader().read(resource("junit-truncated.xml"));
		assertEquals(2, r.tests().size());
		assertTrue(r.truncated());
		assertTrue(r.warnings().get(0).startsWith("report truncated: unexpected end of file after 2 tests (junit-truncated.xml)"), r.warnings().get(0));
	}

	@Test void b02_dtdIsNotExpanded() {
		var r = new JUnitXmlReportReader().read(resource("junit-dtd.xml"));
		assertTrue(r.tests().stream().noneMatch(t -> String.valueOf(t.msg()).contains("root:")));
		assertFalse(r.warnings().isEmpty() && r.tests().isEmpty());
	}

	@Test void b03_oversizeFileSkipped() {
		var lim = new ReportLimits(10, 100, 512, 2000, 8000);
		var r = new JUnitXmlReportReader().read(resource("pytest.xml"), lim);
		assertEquals(1, r.skippedFiles());
		assertTrue(r.tests().isEmpty());
		assertTrue(r.warnings().get(0).contains("pytest.xml"));
		assertFalse(r.warnings().get(0).contains("/"));
	}

	@Test void b04_maxTestsStops(@TempDir Path d) throws Exception {
		var f = d.resolve("big.xml");
		Files.writeString(f, cases(500));
		var r = new JUnitXmlReportReader().read(f, new ReportLimits(1 << 25, 100, 512, 2000, 8000));
		assertEquals(100, r.tests().size());
		assertTrue(r.truncated());
		assertEquals(List.of("report truncated at 100 tests"), r.warnings());
	}

	@Test void b05_hugeTraceIsClipped(@TempDir Path d) throws Exception {
		var f = d.resolve("t.xml");
		Files.writeString(f, "<testsuite name=\"s\"><testcase name=\"n\"><failure>" + "x".repeat(5_000_000) + "</failure></testcase><testcase name=\"m\"/></testsuite>");
		var r = new JUnitXmlReportReader().read(f, new ReportLimits(1 << 26, 100, 512, 2000, 8000));
		assertEquals(2, r.tests().size());
		assertTrue(r.tests().get(0).trace().length() <= 8000);
		assertTrue(r.tests().get(0).msg().length() <= 2000);
	}

	@Test void b06_twelveThousandTestcases(@TempDir Path d) throws Exception {
		var f = d.resolve("many.xml");
		Files.writeString(f, cases(12_000));
		assertEquals(12_000, new JUnitXmlReportReader().read(f).tests().size());
	}

	@Test void b07_missingFile(@TempDir Path d) {
		var r = new JUnitXmlReportReader().read(d.resolve("nope.xml"));
		assertTrue(r.tests().isEmpty());
		assertEquals(1, r.warnings().size());
		assertTrue(r.warnings().get(0).contains("nope.xml"));
	}

	@Test void b08_flakyChildrenAreIgnored() {
		var r = new SurefireReportReader().read(resource("surefire-mixed"));
		assertBean(r.tests().get(4), "name,status,msg", "flaky,PASS,<null>");
	}

	@Test void b09_notXml(@TempDir Path d) throws Exception {
		var f = d.resolve("x.xml");
		Files.writeString(f, "this is not xml");
		var r = new JUnitXmlReportReader().read(f);
		assertTrue(r.tests().isEmpty());
		assertTrue(r.warnings().get(0).startsWith("report unreadable"), r.warnings().get(0));
	}

	private static String cases(int n) {
		var sb = new StringBuilder("<testsuite name=\"s\">\n");
		for (var i = 0; i < n; i++)
			sb.append("<testcase classname=\"c\" name=\"t").append(i).append("\" time=\"0.001\"/>\n");
		return sb.append("</testsuite>").toString();
	}
}
