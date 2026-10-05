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
package org.apache.juneau.test.assertions;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.opentest4j.*;

class InlineScriptSafety_Test extends TestBase {

	private static final String CLEAN = "inlinescript/clean/ok.js";
	private static final String DIRTY = "inlinescript/dirty/bad.js";

	@Test void a01_findCloseTags_everyCaseVariantWithItsLine() {
		assertList(InlineScriptSafety.findCloseTags("x.js", "ok\n * a></script>\nfine\n</SCRIPT >\n"), "x.js:2", "x.js:4");
	}

	@Test void a02_findCloseTags_ignoresOpeningTagsAndCleanText() {
		assertList(InlineScriptSafety.findCloseTags("x.js", "<script src=\"a.js\">\nvar s = 'script';\n"));
	}

	@Test void b01_listResources_filtersBySuffix() throws Exception {
		assertList(InlineScriptSafety.listResources(getClass(), DIRTY, ".js").stream().map(p -> p.getFileName().toString()).toList(), "bad.js", "zz.js");
		assertList(InlineScriptSafety.listResources(getClass(), DIRTY, ".css"));
	}

	@Test void b02_listResources_missingResource() {
		var type = getClass();
		assertThrows(java.io.FileNotFoundException.class, () -> InlineScriptSafety.listResources(type, "nope.js", ".js"));
	}

	@Test void c01_assertNoScriptCloseTag_cleanResource() throws Exception {
		InlineScriptSafety.assertNoScriptCloseTag(getClass(), CLEAN, ".js");
	}

	@Test void c02_assertNoScriptCloseTag_offendingResourceNamesFileAndLine() {
		var type = getClass();
		var e = assertThrows(AssertionFailedError.class, () -> InlineScriptSafety.assertNoScriptCloseTag(type, DIRTY, ".js"));
		assertEquals("Closing script tag found at 'bad.js:18'", e.getMessage());
	}

	@Test void c03_assertNoScriptCloseTag_noMatchingFiles() {
		var type = getClass();
		var e = assertThrows(AssertionFailedError.class, () -> InlineScriptSafety.assertNoScriptCloseTag(type, CLEAN, ".css"));
		assertTrue(e.getMessage().startsWith("No resources matching '.css'"));
	}
}
