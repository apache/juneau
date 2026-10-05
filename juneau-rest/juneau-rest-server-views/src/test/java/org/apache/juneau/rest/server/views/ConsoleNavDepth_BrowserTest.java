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

import static org.apache.juneau.rest.server.views.ConsoleBrowserFixture.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * The shell renders nav trees of any depth: one children row for each selected ancestor that has children, and
 * {@code aria-current} on every link of the active path (R15).
 */
@EnabledIfSystemProperty(named=ConsoleBrowserFixture.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class ConsoleNavDepth_BrowserTest extends TestBase {

	private static Map<String,Map<String,Object>> report;

	// Level k holds "lk" (with level k+1 as children, up to depth) and a leaf sibling "sk".
	private static String level(int k, int depth, String parent) {
		var href = parent + "/l" + k;
		var children = k < depth ? ",\"children\":[" + level(k + 1, depth, href) + "]" : "";
		return "{\"id\":\"l" + k + "\",\"label\":\"L" + k + "\",\"href\":\"" + href + "\"" + children + "},"
			+ "{\"id\":\"s" + k + "\",\"label\":\"S" + k + "\",\"href\":\"" + parent + "/s" + k + "\"}";
	}

	private static String contract(int depth, int activeLength) {
		var active = new StringJoiner(",");
		for (var k = 1; k <= activeLength; k++)
			active.add("\"l" + k + "\"");
		return "{\"version\":\"1\",\"title\":\"Depth\",\"nav\":[" + level(1, depth, "") + "],\"activeNav\":[" + active + "],\"cards\":[]}";
	}

	@BeforeAll
	static void probe() throws Exception {
		var f = ConsoleBrowserFixture.create("nav-depth");
		for (var d = 1; d <= 4; d++)
			f.page("depth-" + d, ORIGIN + "/page", contractPage(contract(d, d), ""));
		f.page("depth-4-active-2", ORIGIN + "/page", contractPage(contract(4, 2), ""));
		report = f.run();
	}

	private static List<String> path(int n) {
		var l = new ArrayList<String>();
		for (var k = 1; k <= n; k++)
			l.add("l" + k);
		return l;
	}

	@Test void a01_depths1to4_fullPath() {
		for (var d = 1; d <= 4; d++) {
			var dd = d;
			var r = report.get("depth-" + d);
			assertNoShellErrors(r);
			assertList(() -> "depth " + dd, r.get("current"), path(d).toArray());
			assertEquals(d - 1, ((Number)r.get("childRows")).intValue(), "depth " + d);
			assertEquals("Depth", r.get("navLabel"), "depth " + d + " nav aria-label (contract title; no header.title)");
			assertEquals("contract", mounted(r).get("activeNavSource"), "depth " + d);
		}
	}

	@Test void a02_partialPath_rendersRowsOnlyUnderSelectedAncestors() {
		var r = report.get("depth-4-active-2");
		assertNoShellErrors(r);
		assertList(r.get("current"), path(2).toArray());
		assertEquals(2, ((Number)r.get("childRows")).intValue());
	}
}
