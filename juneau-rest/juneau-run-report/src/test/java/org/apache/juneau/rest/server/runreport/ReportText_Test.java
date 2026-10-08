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

import static org.apache.juneau.BasicTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class ReportText_Test extends TestBase {

	@Test void a01_stripAnsi() {
		assertEquals("FAIL x", ReportText.stripAnsi("\u001b[31mFAIL\u001b[0m x"));
		assertEquals("", ReportText.stripAnsi(null));
	}

	@Test void a02_clipAddsEllipsis() {
		assertEquals("abcd…", ReportText.clip("abcdefgh", 5));
		assertEquals("abc", ReportText.clip("abc", 5));
		assertEquals("", ReportText.clip(null, 5));
	}

	@Test void a04_commonDirStrip() {
		assertEquals(List.of("a.test.js", "b/c.test.js"), ReportText.stripCommonDir(List.of("/ci/w/src/a.test.js", "/ci/w/src/b/c.test.js")));
		assertEquals(List.of("a.test.js", "b.test.js"), ReportText.stripCommonDir(List.of("/ci/work/repo/src/a.test.js", "/ci/work/repo/src/b.test.js")));
	}

	@Test void a05_singlePathKeepsLastTwoSegments() {
		assertEquals(List.of("src/a.test.js"), ReportText.stripCommonDir(List.of("/ci/w/src/a.test.js")));
		assertEquals(List.of("a.js"), ReportText.stripCommonDir(List.of("a.js")));
	}

	@Test void a06_fileNameNeverAbsolute() {
		assertEquals("TEST-a.xml", ReportText.fileName(Path.of("/tmp/x/TEST-a.xml")));
		assertEquals("", ReportText.fileName(null));
	}

	@Test void a07_backslashesAndDivergentRoots() {
		assertEquals(List.of("src/a.js", "lib/b.js"), ReportText.stripCommonDir(List.of("C:\\w\\src\\a.js", "C:\\w\\lib\\b.js")));
	}

	@Test void a08_noCommonDirAndEmpty() {
		assertEquals(List.of("a.js", "b.js"), ReportText.stripCommonDir(List.of("a.js", "b.js")));
		assertEquals(List.of(), ReportText.stripCommonDir(List.of()));
	}
}
