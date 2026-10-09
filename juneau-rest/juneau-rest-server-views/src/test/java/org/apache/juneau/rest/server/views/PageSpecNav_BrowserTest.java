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
 * {@code PageSpec.navUnder(...)}'s appended depth-3/4 nodes render in the live nav tree, and the server-computed
 * {@code activeNav} path is what the browser highlights (parity case 13, {@code pagespec-corpus/13-navunder-depth3.json}).
 */
@EnabledIfSystemProperty(named=ConsoleBrowserFixture.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class PageSpecNav_BrowserTest extends TestBase {

	private static Map<String,Map<String,Object>> report;

	@BeforeAll
	static void probe() throws Exception {
		report = ConsoleBrowserFixture.create("pagespec-nav")
			.page("nav", ORIGIN + "/page", corpusPage("13-navunder-depth3", "", ""),
				"leaf", "nav.juneau-page-nav a[href=\"/fleet/instances/i-1/logs\"]",
				"depth3", "nav.juneau-page-nav a[href=\"/fleet/instances/i-1\"]")
			.run();
	}

	@Test void a01_mountsWithoutErrors() {
		assertNoShellErrors(report.get("nav"));
	}

	@Test void a02_appendedNodesRender() {
		var q = queries(report.get("nav"));
		assertEquals("Logs", q.get("leaf"));
		assertNotNull(q.get("depth3"));
	}

	@Test void a03_serverActiveNavIsTheHighlightedPath() {
		var r = report.get("nav");
		assertEquals("contract", mounted(r).get("activeNavSource"));
		assertList(r.get("current"), "fleet", "instances", "i-1", "logs");
		assertEquals(3, ((Number)r.get("childRows")).intValue());
	}
}
