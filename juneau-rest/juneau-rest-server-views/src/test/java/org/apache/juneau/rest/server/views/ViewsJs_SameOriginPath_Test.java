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

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.console.*;
import org.junit.jupiter.api.*;

/**
 * The JS half of the shared same-origin path corpus ({@code same-origin-path-corpus.json}): every shipped JS guard
 * against the rows that Java's {@code SameOriginPath_Corpus_Test} runs through {@code SafePathTemplate}.  Gated on
 * {@code node} being on {@code PATH}.
 */
class ViewsJs_SameOriginPath_Test extends TestBase {

	/** Must equal {@code SameOriginPath_Corpus_Test.CASE_COUNT}. */
	private static final int CASE_COUNT = 45;

	private static Map<?,?> cached;

	private static synchronized Map<?,?> report() {
		if (cached != null)
			return cached;
		var r = NodeHarness.report("same-origin-path.cjs", ViewsMixin.VIEWS_JS_RESOURCE, ConsoleChromeMixin.CONSOLE_JS_RESOURCE,
			ConsoleChromeMixin.CHARTS_JS_RESOURCE, ConsoleChromeMixin.JOBLOG_JS_RESOURCE, ViewsMixin.CONSOLE_OUTPUT_JS_RESOURCE,
			ViewsMixin.RUN_VIEW_JS_RESOURCE, ViewsMixin.CALENDAR_JS_RESOURCE, ConsoleChromeMixin.BADGES_JS_RESOURCE);
		assumeTrue(r != null, "node (or same-origin-path.cjs) not available - JS layer skipped");
		return cached = r;
	}

	@Test void a01_everyRowRan() {
		assertEquals(CASE_COUNT, ((Number)report().get("count")).intValue());
		assertEquals(13, ((List<?>)report().get("guards")).size());
	}

	@Test void a02_everyGuardMatchesTheCorpus() {
		assertEquals(List.of(), report().get("mismatches"));
	}

	@Test void a03_crossHostLabelsMatchNodeUrl() {
		assertEquals(List.of(), report().get("labelMismatch"));
		// Node must read a backslash and a leading space the way a browser does, or the labels prove nothing.
		assertEquals(true, report().get("nodeFoldsBackslash"));
		assertEquals(true, report().get("nodeStripsLeadingSpace"));
	}

	@Test void a04_joblogRejectsEveryCrossHostRow() {
		assertEquals(List.of(), report().get("joblogAccepted"));
	}

	@Test void a05_theSharedPrefixRuleIsOneIdenticalHelper() {
		assertEquals(true, report().get("helperIdentical"));
		assertEquals(List.of(), report().get("notUsingHelper"));
		var helper = (String)report().get("helperText");
		assertTrue(helper.contains("\\u0000-\\u001f"), helper);
	}

	@Test void a06_noGuardAcceptsACrossHostRowExceptTheNamedAllowlistCases() {
		// The allowlist guards (console/markdown href, image src) deliberately accept the absolute https URL rows c15 and a16;
		// nothing else may accept a row that leaves the origin.
		var allowed = List.of(
			Map.of("guard", "consoleIsSafeHref", "row", "c15-absolute-https"),
			Map.of("guard", "markdownIsSafeHref", "row", "c15-absolute-https"),
			Map.of("guard", "imageIsSafeSrc", "row", "c15-absolute-https"),
			Map.of("guard", "consoleIsSafeHref", "row", "a16-https-allowlist"),
			Map.of("guard", "markdownIsSafeHref", "row", "a16-https-allowlist"),
			Map.of("guard", "imageIsSafeSrc", "row", "a16-https-allowlist"));
		assertEquals(allowed, report().get("crossHostOverrides"));
	}

	@Test void a07_fetchUrlAcceptsOnlyThisOriginAndSameOriginPaths() {
		var rows = (List<?>)report().get("fetchUrl");
		assertEquals(14, rows.size());
		for (var o : rows) {
			var row = (Map<?,?>)o;
			assertEquals(row.get("expected"), row.get("got"), String.valueOf(row.get("name")));
		}
	}
}
