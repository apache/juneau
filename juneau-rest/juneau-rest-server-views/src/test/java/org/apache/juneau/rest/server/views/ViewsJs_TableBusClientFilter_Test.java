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

import static org.apache.juneau.rest.server.views.ViewsJs_TableBus_Test.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * A CLIENT-mode {@code set-filter} drives the table's in-memory ribbon row filter (the one the ribbon installs on
 * {@code $.fn.dataTable.ext.search}) and then {@code draw()}s - never a DataTables column search for the option, and
 * never an ajax reload.  SERVER mode reloads, and the options reach the request through the ribbon's own query-param
 * and column-search derivation.
 */
class ViewsJs_TableBusClientFilter_Test extends TestBase {

	static Map<String,Object> report;

	@BeforeAll static void runHarness() {
		report = BusHarness.run("table-bus.cjs",
			ViewsMixin.BUS_JS_RESOURCE, ViewsMixin.RENDERS_JS_RESOURCE, ViewsMixin.VIEWS_JS_RESOURCE, ViewsMixin.RIBBON_JS_RESOURCE, ViewsMixin.SEARCH_JS_RESOURCE);
	}

	@BeforeEach void needsNode() {
		assumeTrue(report != null, "node not on PATH");
		assertNull(report.get("crash"), () -> "harness crashed: " + report.get("crash"));
		assertEquals(true, report.get("hasTableBus"), "NS.tableBus is not exported");
	}

	@Test void a01_bindDoesNotColumnSearchAnOption() {
		assertEquals("[]", report.get("bindSearches"));
	}

	@Test void b01_setFilterOptionRefreshesRowFilterAndDraws() {
		var m = map(report.get("clientNew"));
		assertEquals("[]", m.get("searches"), "an option is a row predicate, not a DataTables column search");
		assertEquals(1, m.get("rowFilterRefreshes"));
		assertEquals(1, m.get("draws"));
		assertEquals(0, m.get("reloads"), "CLIENT mode filters in memory; it must not refetch");
		assertEquals("new", m.get("activeAge"));
		assertEquals("{\"schemaVersion\":1,\"viewId\":\"changes\",\"search\":\"\",\"columns\":{},\"options\":{\"mine\":false,\"openOnly\":false,\"age\":\"new\"}}", m.get("filter"),
			"an option is reported under options, never under columns");
	}

	@Test void b02_valuelessGroupMemberStillRefreshesTheRowFilter() {
		var m = map(report.get("clientAll"));
		assertEquals(2, m.get("rowFilterRefreshes"));
		assertEquals("all", m.get("filterAge"));
	}

	@Test void c01_plainPageWithoutABusFiltersToo() {
		assertNull(report.get("plainBus"));
		var m = map(report.get("plain"));
		assertEquals(1, m.get("rowFilterRefreshes"));
		assertEquals("new", m.get("lastAge"));
		assertEquals(1, m.get("draws"));
		assertEquals(2, m.get("seen"), "the starting filter, then the patch");
	}

	@Test void d01_serverModeUsesRequestDerivationNotRowFilter() {
		var m = map(report.get("server"));
		assertEquals("[]", m.get("searches"));
		assertEquals(1, m.get("reloads"));
		assertEquals(0, m.get("draws"));
		assertEquals("{\"mine\":\"true\"}", m.get("params"));
		assertEquals("{\"3\":\"open\"}", m.get("columnSearches"));
		assertEquals(true, m.get("filterOpenOnly"));
	}
}
