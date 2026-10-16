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

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class RunViewChecks_Test extends TestBase {

	@Test void a01_lineHrefAcceptsSameOriginPathsAndFragments() {
		for (var s : new String[]{ "#step-3", "/run/42", "/x/y?z=1", "chart.png", "java\tscript" })
			assertTrue(RunViewChecks.isSafeLineHref(s), s);
	}

	@Test void a02_lineHrefRefusesAnyBackslash() {
		for (var s : new String[]{ "/a\\b", "/foo?q=C:\\x", "\\\\e.x\\a", "/\\e.x" })
			assertFalse(RunViewChecks.isSafeLineHref(s), s);
	}

	@Test void a03_noteHrefAndRawHrefTemplateRefuseAnyBackslash() {
		assertFalse(RunViewChecks.isNoteHref("/a\\b"));
		assertFalse(RunViewChecks.isRawHrefTemplate("/a\\b?o={offset}"));
		assertTrue(RunViewChecks.isRawHrefTemplate("/a/b?o={offset}"));
		assertFalse(RunViewChecks.isRawHrefTemplate("/a/b?l={line}"));
	}

	@Test void a04_clipKeepsSixtyFourCharactersAndAnEllipsis() {
		assertEquals("null", RunViewChecks.clip(null));
		assertEquals("short", RunViewChecks.clip("short"));
		assertEquals("a".repeat(64), RunViewChecks.clip("a".repeat(64)));
		assertEquals("a".repeat(64) + "\u2026", RunViewChecks.clip("a".repeat(65)));
	}

	@Test void a05_minRefreshIsOneSecond() {
		assertEquals(1000L, RunViewChecks.MIN_REFRESH_MS);
	}
}
