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

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

class ConsoleOutputChecks_Test extends TestBase {

	@SuppressWarnings("unchecked")
	static Map<String,Map<String,List<String>>> vectors() throws IOException {
		try (var in = ConsoleOutputChecks_Test.class.getResourceAsStream("console-output-vectors.json")) {
			assertNotNull(in, "console-output-vectors.json missing from the test classpath");
			return Json.to(new String(in.readAllBytes(), StandardCharsets.UTF_8), Map.class);
		}
	}

	private static void check(String group, Predicate<String> p) throws IOException {
		var g = vectors().get(group);
		for (var s : g.get("accept"))
			assertTrue(p.test(s), () -> group + " should accept: " + s);
		for (var s : g.get("reject"))
			assertFalse(p.test(s), () -> group + " should reject: " + s);
	}

	@Test void a01_color() throws Exception { check("color", ConsoleOutputChecks::isSafeColor); }
	@Test void a02_href() throws Exception { check("href", ConsoleOutputChecks::isSafeLineHref); }
	@Test void a03_imageSrc() throws Exception { check("imageSrc", ConsoleOutputChecks::isSafeLineImageSrc); }
	@Test void a04_icon() throws Exception { check("icon", ConsoleOutputChecks::isIconName); }
	@Test void a05_token() throws Exception { check("token", ConsoleOutputChecks::isToken); }

	@Test void a06_tokenLength() {
		assertTrue(ConsoleOutputChecks.isToken("a".repeat(128)));
		assertFalse(ConsoleOutputChecks.isToken("a".repeat(129)));
	}

	@Test void a07_nulls() {
		assertFalse(ConsoleOutputChecks.isSafeColor(null));
		assertFalse(ConsoleOutputChecks.isSafeLineHref(null));
		assertFalse(ConsoleOutputChecks.isSafeLineImageSrc(null));
		assertFalse(ConsoleOutputChecks.isIconName(null));
		assertFalse(ConsoleOutputChecks.isToken(null));
		assertFalse(ConsoleOutputChecks.isLogId(null));
		assertFalse(ConsoleOutputChecks.isAnchorPrefix(null));
	}

	@Test void a08_logId() {
		assertTrue(ConsoleOutputChecks.isLogId("7f3a9c"));
		assertTrue(ConsoleOutputChecks.isLogId("a_b-C"));
		assertFalse(ConsoleOutputChecks.isLogId("a.b"));
		assertFalse(ConsoleOutputChecks.isLogId("a/b"));
		assertFalse(ConsoleOutputChecks.isLogId(""));
		assertFalse(ConsoleOutputChecks.isLogId("a".repeat(129)));
	}

	@Test void a09_anchorPrefix() {
		assertTrue(ConsoleOutputChecks.isAnchorPrefix("L"));
		assertTrue(ConsoleOutputChecks.isAnchorPrefix("raw-L"));
		assertTrue(ConsoleOutputChecks.isAnchorPrefix("a" + "b".repeat(31)));
		assertFalse(ConsoleOutputChecks.isAnchorPrefix("a" + "b".repeat(32)));
		assertFalse(ConsoleOutputChecks.isAnchorPrefix("9L"));
		assertFalse(ConsoleOutputChecks.isAnchorPrefix("L.x"));
	}

	@Test void a10_clip() {
		assertEquals("null", ConsoleOutputChecks.clip(null));
		assertEquals("abc", ConsoleOutputChecks.clip("abc"));
		assertEquals("a".repeat(64) + "…", ConsoleOutputChecks.clip("a".repeat(65)));
	}

	@Test void a11_tabCrLfAreStrippedBeforeChecking() {
		// A CR/LF/TAB inside a value is dropped before the grammar runs, so these accept their stripped form.
		assertTrue(ConsoleOutputChecks.isSafeLineHref("#a\nb"));
		assertTrue(ConsoleOutputChecks.isSafeLineImageSrc("/r\n/c.png"));
		assertFalse(ConsoleOutputChecks.isSafeLineHref("java\nscript:alert(1)"));
	}

	@Test void a12_hrefGrammarMatchesDetailEndpoint() {
		// Non-fragment hrefs and image sources use the same rule as RegionDef.isSafeDetailEndpoint.
		for (var s : List.of("chart.png", "jobs/7/lines", "/x/y", "./x", "//e.x/a", "../x", "http://e.x/", "a:b")) {
			assertEquals(RegionDef.isSafeDetailEndpoint(s), ConsoleOutputChecks.isSafeLineHref(s), s);
			assertEquals(RegionDef.isSafeDetailEndpoint(s), ConsoleOutputChecks.isSafeLineImageSrc(s), s);
		}
	}
}
