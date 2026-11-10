// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.console.views;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.util.*;

import org.junit.jupiter.api.*;

/** The {@code chart} region type and its built-in populator, run through the Node harness. */
class RegionsChart_Test {

	private static Map<?,?> report() {
		var r = RegionsHarness.reportWithHelpers("regions-chart.cjs");
		assumeTrue(r != null, "node not available or regions-chart.cjs not found - behavioral layer skipped");
		assertNull(r.get("crash"), () -> String.valueOf(r.get("crash")));
		return r;
	}

	@Test void a01_chartIsARegionType() {
		assertTrue(String.valueOf(report().get("types")).contains("chart"));
	}

	@Test void a02_fetchesItsOwnDataUrl_andMountsMappedRows() {
		var r = report();
		assertEquals(List.of("/pets/s-7/price-series"), r.get("fetches"));
		assertEquals(1, ((Number)r.get("mountCount")).intValue());
		assertEquals(true, r.get("sameEl"));
		assertEquals(List.of(Map.of("day", "Mon", "price", 1), Map.of("day", "Tue", "price", 2)), r.get("rows"));
		var card = (Map<?,?>)r.get("card");
		assertEquals("line", card.get("kind"));
		assertEquals(240, ((Number)card.get("height")).intValue());
		assertEquals("day", card.get("x"));
		assertEquals("price", ((Map<?,?>)((List<?>)card.get("series")).get(0)).get("field"));
		assertEquals("ok", r.get("state"));
		assertEquals(List.of(), r.get("errors"));
	}

	@Test void a03_emptyRecords_showEmptyText() {
		var r = report();
		assertEquals(0, ((Number)r.get("emptyMounts")).intValue());
		assertEquals("No prices", r.get("emptyText"));
		assertEquals("No data", r.get("emptyDefault"));
	}

	@Test void a04_chartingAbsent_showsStatusWithoutThrowing() {
		var r = report();
		assertEquals("charting is not loaded", r.get("absentText"));
	}
}
