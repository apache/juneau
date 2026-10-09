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
 * Ribbon controls over the bus in real Chromium (spec 5.5): a target-card refresh sends cmd:tasks, an option toggle sets the filter and
 * reaches a {@code filter} subscriber, a column-scoped option reaches the server request, and a {@code publish} item feeds another card's
 * {@code filter} role.
 */
@EnabledIfSystemProperty(named=ConsoleBusBrowserSupport.GATE, matches="true", disabledReason=ConsoleBusBrowserSupport.DISABLED)
class ConsoleBusRibbon_BrowserTest extends TestBase {

	private static Map<String,Object> r;

	@BeforeAll
	static void probe() throws Exception {
		r = run("ribbon", "master-detail.json", true, "{declare:[{topic:'app.region-picked',retain:true}]}", "");
	}

	@Test void a01_noJsFailures() {
		assertEquals(List.of(), list(r.get("jsFailures")), () -> r.toString());
		assertEquals(List.of(), list(r.get("consoleErrors")), () -> r.toString());
	}

	@Test void a02_theRibbonShowsEveryDeclaredControl() {
		var controls = list(r.get("controls"));
		for (var label : List.of("Mine only", "Open only", "Focus east", "Refresh", "Refresh tasks"))
			assertTrue(controls.contains(label), () -> label + " missing from " + controls);
	}

	@Test void a03_refreshTasksSendsACommandAndRefetchesTheTargetOnly() {
		assertEquals(Boolean.TRUE, r.get("refreshTasksClicked"));
		var t = map(r.get("refreshTasks"));
		assertEquals(Boolean.TRUE, t.get("cmdTasksInHistory"));
		assertEquals(num(t.get("tasksBefore")) + 1, num(t.get("tasksAfter")));
	}

	@Test void a04_optionToggleSetsTheFilterAndReachesTheFilterSubscriber() {
		assertEquals(Boolean.TRUE, r.get("mineClicked"));
		var m = map(r.get("mine"));
		assertEquals(Boolean.TRUE, map(map(m.get("filter")).get("options")).get("mine"));
		assertEquals("true", m.get("pressed"));
		assertEquals(Boolean.TRUE, m.get("cmdChangesInHistory"));
		assertNotNull(m.get("byOwnerLast"), "the recorder card's filter role must have run");
		assertEquals(Boolean.TRUE, map(map(m.get("byOwnerLast")).get("options")).get("mine"));
	}

	@Test void a05_columnScopedOptionAndPublishReachTheServerRequests() {
		assertEquals("open", map(r.get("columnScoped")).get("stateSearch"));
		var f = map(r.get("focusEast"));
		assertEquals(Map.of("region", "east"), f.get("retained"));
		assertEquals("east", f.get("regionSearch"));
	}
}
