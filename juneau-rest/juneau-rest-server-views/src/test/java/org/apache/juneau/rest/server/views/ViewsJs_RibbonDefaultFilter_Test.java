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

import static org.apache.juneau.rest.server.views.ViewsJs_RibbonBus_Test.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * An {@code optionGroup} starts on its explicit {@code default} only: with no {@code default} nothing is selected, a
 * restored member wins, and the default reaches the first {@code filter:<id>} payload and paints the member pressed.
 * Runs {@code ribbon-bus.cjs} through {@link BusHarness}.
 */
class ViewsJs_RibbonDefaultFilter_Test extends TestBase {

	static Map<String,Object> report;

	@BeforeAll static void runHarness() {
		report = BusHarness.run("ribbon-bus.cjs",
			ViewsMixin.BUS_JS_RESOURCE, ViewsMixin.RENDERS_JS_RESOURCE, ViewsMixin.VIEWS_JS_RESOURCE, ViewsMixin.RIBBON_JS_RESOURCE);
	}

	@BeforeEach void needsNode() {
		assumeTrue(report != null, "node not on PATH");
		assertNull(report.get("crash"), () -> "harness crashed: " + report.get("crash"));
	}

	@Test void a01_explicitDefaultApplies() {
		assertEquals("{\"age\":\"new\"}", report.get("defaultNothingStored"));
	}

	@Test void a02_persistedMemberWins() {
		assertEquals("{\"age\":\"all\"}", report.get("defaultStoredMember"));
	}

	@Test void a03_noDefaultMeansNoSelection() {
		assertEquals("{}", report.get("noDefaultNothingStored"));
	}

	@Test void b01_defaultReachesTheFirstFilterPayloadAndPaintsPressed() {
		var m = map(report.get("initial"));
		assertEquals("{\"age\":\"new\"}", m.get("activeState"));
		assertEquals("false", m.get("open"));
		assertEquals("true", m.get("new"));
		assertEquals("false", m.get("all"));
		assertEquals("new", m.get("filterAge"));
	}

	@Test void c01_deselectPersistsEmptyAndSurvivesReload() {
		var m = map(report.get("deselect"));
		assertEquals("all", m.get("selected"));
		assertEquals("", m.get("stored"));
		assertEquals("null", m.get("live"));
		assertEquals("null", m.get("reloaded"));
		assertEquals(true, m.get("reloadedHasKey"));
	}

	@Test void c02_storedEmptyBeatsDefault() {
		var m = map(report.get("storedEmpty"));
		assertEquals(true, m.get("hasKey"));
		assertEquals("null", m.get("value"));
	}

	@Test void d01_storedNullStringFallsThroughToDefault() {
		assertEquals("{\"age\":\"new\"}", report.get("storedNullString"));
	}

	@Test void d02_storedStaleMemberFallsThroughToDefault() {
		assertEquals("{\"age\":\"new\"}", report.get("storedStaleMember"));
	}

	@Test void d03_storedNullStringWithoutDefaultMeansNoSelection() {
		assertEquals("{}", report.get("storedNullNoDefault"));
	}
}
