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
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * One minimal "it boots in a browser" check for the richest parity case (case 19,
 * {@code pagespec-corpus/19-visible-when-passthrough.json}: a table with a {@code visibleWhen} row action and detail region):
 * no console error or page error, and the row action's label is on the opened menu. The contract itself is owned by
 * {@code PageSpec_Parity_Test}.
 */
@EnabledIfSystemProperty(named=ConsoleBrowserFixture.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class PageSpecParity_BrowserTest extends TestBase {

	private static final String ROWS = """
		[{"id":"1","state":"open"}]
		""";

	private static Map<String,Map<String,Object>> report;

	@BeforeAll
	static void probe() throws Exception {
		report = ConsoleBrowserFixture.create("pagespec-parity")
			.asset("/rest/t/data", ROWS, "application/json")
			.page("parity", ORIGIN + "/page", corpusPage("19-visible-when-passthrough", "", viewsPack()),
				"label", "body .juneau-view-action-menu .juneau-view-action-item")
			.waitFor("#t-body tbody .juneau-view-action-trigger")
			.click("#t-body tbody .juneau-view-action-trigger")
			.run();
	}

	@Test void a01_bootsCleanly() {
		assertNoShellErrors(report.get("parity"));
	}

	@Test void a02_rowActionLabelIsOnTheRow() {
		assertEquals("Close", queries(report.get("parity")).get("label"), () -> report.get("parity").toString());
	}
}
