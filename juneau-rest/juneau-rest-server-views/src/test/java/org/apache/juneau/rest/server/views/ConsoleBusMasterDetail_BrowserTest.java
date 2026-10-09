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

import static org.apache.juneau.rest.server.views.ConsoleBusBrowserSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * Master/detail over the bus in real Chromium (spec 5.2): the {@code tasks} card follows {@code selection:changes} as a {@code params} subscription,
 * shows its {@code emptyText} while nothing is selected, refetches with the encoded id on each pick, and clears again on deselect.
 */
@EnabledIfSystemProperty(named=ConsoleBusBrowserSupport.GATE, matches="true", disabledReason=ConsoleBusBrowserSupport.DISABLED)
class ConsoleBusMasterDetail_BrowserTest extends TestBase {

	private static Map<String,Object> r;

	@BeforeAll
	static void probe() throws Exception {
		r = run("master-detail", "master-detail.json", true, "{declare:[{topic:'app.region-picked',retain:true}]}", "");
	}

	@Test void a01_noJsFailures() {
		assertEquals(List.of(), list(r.get("jsFailures")), () -> r.toString());
		assertEquals(List.of(), list(r.get("consoleErrors")), () -> r.toString());
	}

	@Test void a02_awaitingParamsShowsEmptyTextAndFetchesNothing() {
		var initial = map(r.get("initial"));
		assertEquals("Select a change to see its tasks.", initial.get("tasksEmpty"));
		assertEquals(0, num(initial.get("tasksFetches")));
		assertEquals(0, num(map(initial.get("selection")).get("count")));
		assertEquals(List.of("mounted", "pending", "mounted"), list(r.get("cardStates")));
	}

	@Test void a03_pickingARowFetchesTheDetailForThatId() {
		assertEquals(Boolean.TRUE, r.get("selectClicked"));
		var sel = map(r.get("selected"));
		assertEquals(List.of("/api/changes/c-17/tasks"), list(sel.get("tasksPaths")));
		assertEquals(List.of("c-17"), list(map(sel.get("selection")).get("ids")));
		assertEquals(Boolean.TRUE, sel.get("tasksHasRow"));
	}

	@Test void a04_deselectingClearsTheDetailBackToEmptyText() {
		var c = map(r.get("cleared"));
		assertEquals("Select a change to see its tasks.", c.get("tasksEmpty"));
		assertEquals(1, num(c.get("tasksFetches")), "clearing must not fetch");
		assertEquals(0, num(map(c.get("selection")).get("count")));
	}

	@Test void a05_theIdIsUrlEncodedInThePath() {
		assertEquals("/api/changes/c%2F9%20x/tasks", map(r.get("encoded")).get("lastTasksPath"));
	}
}
