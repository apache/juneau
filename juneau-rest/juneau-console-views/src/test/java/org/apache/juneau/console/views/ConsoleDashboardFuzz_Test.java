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
package org.apache.juneau.console.views;

import static org.apache.juneau.bct.BctAssertions.assertSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.console.*;
import org.junit.jupiter.api.*;

/**
 * The seeded share-link fuzz over {@code juneau-dashboard.js} without a DOM, plus the hand-written edge cases for the
 * banned keys, depth and cycle rejection, and the payload and fragment size caps.  Runs when node is on PATH.
 */
class ConsoleDashboardFuzz_Test extends TestBase {

	private static Map<?,?> part(String key) {
		var r = NodeHarness.report("dashboard-fuzz.cjs", ConsoleChromeMixin.DASHBOARD_JS_RESOURCE);
		assumeTrue(r != null, "node (or dashboard-fuzz.cjs) not available - JS layer skipped");
		var s = (Map<?,?>)r.get(key);
		assertNotNull(s, () -> "section '" + key + "' missing from the harness output");
		assertNull(s.get("crash"), () -> "section '" + key + "' crashed: " + s.get("crash"));
		return s;
	}

	private static final String NOT_PLAIN = "E-JS-104: share link rejected: decoder 'app.d' returned a value that is not plain JSON; using saved state or defaults";

	@Test void a01_fuzz_shareLinkCodecAndRestore() {
		var r = part("fuzz");
		assertEquals(List.of(), r.get("failures"), "seed 0x5eed0694; replay the case number shown");
		assertEquals(2400, ((Number)r.get("cases")).intValue());
		assertEquals(List.of("decoder", "link", "null", "saved"), r.get("sources"));
		var per = (Map<?,?>)r.get("perCategory");
		assertSize(12, per);
		for (var v : per.values())
			assertEquals(200, ((Number)v).intValue());
		assertEquals(true, r.get("polluted"));
	}

	@Test void a02_bannedKeys_rejectedEverywhere() {
		var e = part("edges");
		assertEquals(List.of("the link carries the key '__proto__'", "the link carries the key 'constructor'", "the link carries the key 'prototype'"), e.get("linkBanned"));
		for (var o : (List<?>)e.get("savedBanned")) {
			assertNull(((Map<?,?>)o).get("source"));
			assertEquals(true, ((Map<?,?>)o).get("deleteSaved"));
		}
		var dec = (List<?>)e.get("decoderBanned");
		assertSize(3, dec);
		for (var o : dec) {
			assertNull(((Map<?,?>)o).get("source"));
			assertEquals(List.of(NOT_PLAIN), ((Map<?,?>)o).get("warns"));
		}
		assertEquals(true, e.get("polluted"));
	}

	@Test void a03_depthAndCycles_rejected() {
		var e = part("edges");
		for (var k : List.of("cycle", "cycleArray", "deepDecoder")) {
			var m = (Map<?,?>)e.get(k);
			assertNull(m.get("source"), k);
			assertEquals(List.of(NOT_PLAIN), m.get("warns"), k);
		}
		assertEquals("decoder:app.d", ((Map<?,?>)e.get("shallowDecoder")).get("source"));
		assertEquals(List.of(), ((Map<?,?>)e.get("shallowDecoder")).get("warns"));
		assertNull(((Map<?,?>)e.get("deepSaved")).get("source"));
		assertEquals(true, ((Map<?,?>)e.get("deepSaved")).get("deleteSaved"));
		assertEquals("the link is nested more than 64 deep", e.get("deepLink"));
		assertEquals(List.of("t"), e.get("shallowLink"));
	}

	@Test void a04_payloadCap_countsBytesNotCharacters() {
		var c = (Map<?,?>)part("edges").get("cap");
		for (var k : List.of("atBytes", "threeBytes", "fourBytes"))
			assertEquals(4096, ((Number)c.get(k)).intValue(), k);
		assertEquals(4097, ((Number)c.get("overBytes")).intValue());
		// Fewer than 4096 characters but more than 4096 bytes.
		assertTrue(((Number)c.get("charsLength")).intValue() < 4096);
		assertTrue(((Number)c.get("charsBytes")).intValue() > 4096);
		assertEquals(List.of("filter:q"), c.get("at"));
		assertEquals(List.of("filter:q"), c.get("three"));
		assertEquals(List.of("filter:q"), c.get("four"));
		assertEquals(List.of("filter:r"), c.get("over"), "the over-cap payload is dropped, the other topic kept");
		assertEquals(List.of("filter:r"), c.get("chars"));
		assertEquals(true, c.get("atRoundTrip"));
		assertEquals(true, c.get("overCheck"));
		assertEquals(List.of("filter:q", "filter:r"), c.get("twoSmall"));
	}

	@Test void a05_fragmentCap_nullOverEightKiB() {
		var e = part("edges");
		assertEquals(true, ((Map<?,?>)e.get("cap")).get("tooBigIsNull"));
		assertEquals("the link is not base64url", ((Map<?,?>)e.get("fragmentCap")).get("at"));
		assertEquals("the link is over 8192 characters", ((Map<?,?>)e.get("fragmentCap")).get("over"));
	}
}
