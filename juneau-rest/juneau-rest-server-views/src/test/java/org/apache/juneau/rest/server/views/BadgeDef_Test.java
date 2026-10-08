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

import java.util.*;

import org.junit.jupiter.api.*;

class BadgeDef_Test {

	@Test void a01_minimal() {
		var b = BadgeDef.create("pending").src("/rest/changes/pending");
		b.validate();
		var m = b.toContractMap();
		assertEquals("pending", m.get("id"));
		assertEquals("/rest/changes/pending", m.get("src"));
		assertFalse(m.containsKey("placement"));
		assertFalse(m.containsKey("tone"));
	}

	@Test void a02_fullExample_matchesJavadoc() {
		var b = BadgeDef.create("pending")
			.src("/rest/changes/pending")
			.scope("beanType", BadgeDef.ScopeBy.NAV, Map.of("suspensions", List.of("Suspension")))
			.href("/ui/changes")
			.stateFilter("beanType")
			.refreshes("rules-table")
			.refreshMs(60_000);
		b.validate();
		var m = b.toContractMap();
		assertEquals("/ui/changes", m.get("href"));
		assertEquals(60_000L, m.get("refreshMs"));
		assertEquals(List.of("rules-table"), m.get("refreshes"));
		@SuppressWarnings("unchecked")
		var scope = (Map<String,Object>) m.get("scope");
		assertEquals("beanType", scope.get("param"));
		assertEquals("nav", scope.get("by"));
		@SuppressWarnings("unchecked")
		var stateFilter = (Map<String,Object>) m.get("stateFilter");
		assertEquals("beanType", stateFilter.get("column"));
	}

	@Test void a03_idRequired() {
		var e = assertThrows(IllegalArgumentException.class, () -> BadgeDef.create(null));
		assertTrue(e.getMessage().contains("id must not be null or blank"), e.getMessage());
		var e2 = assertThrows(IllegalArgumentException.class, () -> BadgeDef.create(" "));
		assertTrue(e2.getMessage().contains("id must not be null or blank"), e2.getMessage());
	}

	@Test void a04_e61_srcRequired() {
		var e = assertThrows(IllegalArgumentException.class, () -> BadgeDef.create("pending").validate());
		assertEquals("<@badge id='pending'> requires src=.", e.getMessage());
	}

	@Test void a05_e62_refreshMsFloor() {
		var b = BadgeDef.create("pending").src("/x").refreshMs(1000);
		var e = assertThrows(IllegalArgumentException.class, b::validate);
		assertEquals("'pending' refreshMs='1000' is below the minimum '5000'.", e.getMessage());
	}

	@Test void a06_refreshMsAtFloorIsFine() {
		BadgeDef.create("pending").src("/x").refreshMs(5000).validate();
	}

	@Test void a07_e66_toolbarRequiresTable() {
		var b = BadgeDef.create("pending").src("/x").placement(BadgeDef.Placement.TOOLBAR);
		var e = assertThrows(IllegalArgumentException.class, b::validate);
		assertEquals("<@badge id='pending'> placement='toolbar' requires table=.", e.getMessage());
	}

	@Test void a08_toolbarWithTableIsFine() {
		BadgeDef.create("pending").src("/x").placement(BadgeDef.Placement.TOOLBAR).table("rules-table").validate();
	}

	@Test void a09_r7_scopeByViewRequiresTable_uncoded() {
		var b = BadgeDef.create("pending").src("/x").scope("beanType", BadgeDef.ScopeBy.VIEW, null);
		var e = assertThrows(IllegalArgumentException.class, b::validate);
		assertTrue(e.getMessage().contains("scope.by='view' requires table"), e.getMessage());
	}

	@Test void a10_scopeByViewWithTableIsFine() {
		BadgeDef.create("pending").src("/x").table("rules-table")
			.scope("beanType", BadgeDef.ScopeBy.VIEW, null).validate();
	}

	@Test void a11_labelToneTooltipMax_roundTrip() {
		var m = BadgeDef.create("pending").src("/x")
			.label(Map.of("one", "{total} item", "other", "{total} items"))
			.tone(BadgeDef.Tone.DANGER)
			.tooltipMax(5)
			.toContractMap();
		@SuppressWarnings("unchecked")
		var label = (Map<String,Object>) m.get("label");
		assertEquals("{total} item", label.get("one"));
		assertEquals("danger", m.get("tone"));
		assertEquals(5, m.get("tooltipMax"));
	}

	@Test void a12_visibleWhen_roundTrip() {
		var m = BadgeDef.create("pending").src("/x")
			.visibleWhen(VisibilityRule.when("facts.viewer.roles").contains("oncall"))
			.toContractMap();
		@SuppressWarnings("unchecked")
		var vw = (List<Map<String,Object>>) m.get("visibleWhen");
		assertEquals(1, vw.size());
		assertEquals("facts.viewer.roles", vw.get(0).get("field"));
		assertEquals("contains", vw.get(0).get("op"));
		assertEquals("oncall", vw.get(0).get("value"));
	}
}
