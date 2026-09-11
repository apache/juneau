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
import org.junit.jupiter.api.*;

/**
 * Always-on coverage for {@code JuneauViews.regions.mount}: HTML-slot hookup, loud failure on missing id or
 * unregistered populator name, bus targeting by slot id, and author-marked page-nav {@code aria-current}.
 *
 * <p>
 * The behavioral half runs the real runtime source under a DOM shim (see {@code src/test/js/regions-mount.cjs}).
 */
class Regions_Mount_Test extends TestBase {

	private static Map<?,?> report() {
		var r = RegionsHarness.report("regions-mount.cjs");
		assumeTrue(r != null, "node not available or regions-mount.cjs not found - behavioral layer skipped");
		return r;
	}

	private static void assertAllTrue(Map<?,?> r, String... keys) {
		for (var k : keys)
			assertEquals(true, r.get(k), () -> k + " -> " + r.get(k) + " in " + r);
	}

	@Test void a01_sourceShape_mountLivesOnRegionsNotPages() throws Exception {
		var body = RegionsHarness.regionsJs();
		assertTrue(body.contains("function mount(hookup)"), body);
		assertTrue(body.contains("mount: mount"), body);
		assertFalse(body.contains("NS.pages.mount"), "mount must not publish under NS.pages");
		assertTrue(body.contains("no populator is registered under the name"), body);
		assertTrue(body.contains("no element with id"), body);
		assertTrue(body.contains("the string 'juneau-table' is not a populator"), body);
		assertTrue(body.contains("{ table: url }"), body);
	}

	@Test void a02_sourceShape_mountDoesNotFallThroughToDefaultPopulate() throws Exception {
		var body = RegionsHarness.regionsJs();
		var mountStart = body.indexOf("function mount(hookup)");
		assertTrue(mountStart >= 0, body);
		var mountEnd = body.indexOf("NS.regions = {", mountStart);
		assertTrue(mountEnd > mountStart, body);
		var mountFn = body.substring(mountStart, mountEnd);
		assertFalse(mountFn.contains("defaultPopulate("), mountFn);
		assertTrue(mountFn.contains("typeof resolve(name) !== \"function\""), mountFn);
	}

	@Test void b01_mountStampsAttrsAndPopulatesBodies() {
		var r = report();
		assertAllTrue(r, "t1_mountIsOnRegions", "t1_notOnPages", "t1_probesKeyIsSlotId", "t1_detailsKeyIsSlotId",
			"t1_containerEmptyAtPopulate", "t1_probesPainted", "t1_detailsPainted", "t1_noErrors",
			"t1_thenable", "t1_regionEnrolmentWasSync");
		assertEquals(2, ((Number)r.get("t1_handleCount")).intValue(), r::toString);
		assertEquals("probes", r.get("t1_probesAttr"));
		assertEquals("ssc-probes", r.get("t1_probesPopulate"));
		assertEquals("details", r.get("t1_detailsAttr"));
		assertEquals("ssc-probe-details", r.get("t1_detailsPopulate"));
	}

	@Test void b02_missingIdFailsLoudAndEnrolsNothing() {
		var r = report();
		assertAllTrue(r, "t2_threw", "t2_namesMissingId", "t2_consoleError", "t2_probesNotStamped",
			"t2_probesNotEnrolled", "t2_defaultDidNotRun");
	}

	@Test void b03_unregisteredNameFailsLoudAndDoesNotRunDefaultPopulate() {
		var r = report();
		assertAllTrue(r, "t3_threw", "t3_namesBadPopulator", "t3_consoleError", "t3_probesNotStamped",
			"t3_detailsNotStamped", "t3_defaultDidNotRun");
	}

	@Test void b04_blankPopulatorNameFailsLoud() {
		var r = report();
		assertAllTrue(r, "t4_threw", "t4_consoleError", "t4_defaultDidNotRun");
	}

	@Test void b05_busTargetingUsesSlotIdAfterMount() {
		var r = report();
		assertAllTrue(r, "t5_detailsGotTargeted", "t5_probesDidNotGetOwn", "t5_broadcastStillWorks");
	}

	@Test void b06_pageNavIsAuthorMarkedAriaCurrentWithoutPills() {
		var r = report();
		assertAllTrue(r, "t6_pageNavNotExported", "t6_authorAriaPreserved", "t6_noSelectedClass", "t6_noPillClass");
	}

	@Test void b07_juneauTableStringThrowsNewMessageAndEnrolsNothing() {
		var r = report();
		assertAllTrue(r, "t7_threw", "t7_pointsAtTableUrl", "t7_notUnregisteredName", "t7_consoleError",
			"t7_notStamped", "t7_defaultDidNotRun");
	}
}
