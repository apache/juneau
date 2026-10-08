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

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

/** Unit tests for the {@link VisibilityRule} builder, its {@code test(rules, map)} evaluator, and its wire shape. */
class VisibilityRule_Test extends TestBase {

	@Test void a01_eqSerializesFieldOpValue() {
		var r = VisibilityRule.when("status").eq("open");
		assertEquals("status", r.field);
		assertEquals("eq", r.op);
		assertEquals("open", r.value);
	}

	@Test void a02_presentAndAbsentOmitValue() {
		assertNull(VisibilityRule.when("name").present().value);
		assertNull(VisibilityRule.when("name").absent().value);
	}

	@Test void a03_inWrapsVarargsAsAList() {
		assertEquals(List.of("open", "acked"), VisibilityRule.when("status").in("open", "acked").value);
	}

	@Test void a04_whenRejectsBlankField() {
		assertThrows(IllegalArgumentException.class, () -> VisibilityRule.when(" "));
		assertThrows(IllegalArgumentException.class, () -> VisibilityRule.when(null));
	}

	@Test void a05_eqRejectsNullValue() {
		assertThrows(IllegalArgumentException.class, () -> VisibilityRule.when("status").eq(null));
		assertThrows(IllegalArgumentException.class, () -> VisibilityRule.when("status").contains(null));
	}

	@Test void a06_testMatchesAndRule() {
		var rules = List.of(VisibilityRule.when("status").eq("open"), VisibilityRule.when("priority").in("p1", "p2"));
		assertTrue(VisibilityRule.test(rules, Map.of("status", "open", "priority", "p1")));
		assertFalse(VisibilityRule.test(rules, Map.of("status", "open", "priority", "p3")));
	}

	@Test void a07_testFailsClosedOnMissingField() {
		assertFalse(VisibilityRule.test(List.of(VisibilityRule.when("status").ne("open")), Map.of()));
	}

	@Test void a08_absentMatchesMissingAndNull() {
		var rules = List.of(VisibilityRule.when("name").absent());
		assertTrue(VisibilityRule.test(rules, Map.of()));
		var m = new HashMap<String,Object>();
		m.put("name", null);
		assertTrue(VisibilityRule.test(rules, m));
		assertFalse(VisibilityRule.test(rules, Map.of("name", "a")));
	}

	@Test void a09_dottedPathResolvesNestedFacts() {
		var rules = List.of(VisibilityRule.when("facts.viewer.roles").contains("admin"));
		assertTrue(VisibilityRule.test(rules, Map.of("facts", Map.of("viewer", Map.of("roles", List.of("admin"))))));
	}

	@Test void a10_emptyRuleListAlwaysMatches() {
		assertTrue(VisibilityRule.test(List.of(), Map.of()));
		assertTrue(VisibilityRule.test(null, Map.of()));
	}

	@Test void a11_unknownOpThrows() {
		var r = VisibilityRule.when("status").eq("open");
		r.op = "bogus";
		var ex = assertThrows(IllegalArgumentException.class, () -> VisibilityRule.test(List.of(r), Map.of("status", "open")));
		assertTrue(ex.getMessage().contains("'bogus'"), ex.getMessage());
	}

	@Test void a12_toMapOmitsValueWhenAbsent() {
		assertEquals(Map.of("field", "n", "op", "present"), VisibilityRule.when("n").present().toMap());
		assertEquals(Map.of("field", "s", "op", "in", "value", List.of("a")), VisibilityRule.when("s").in("a").toMap());
	}

	@Test void b01_rowActionSerializesVisibleWhen() {
		var a = RowAction.create("escalate").visibleWhen(VisibilityRule.when("status").eq("open"));
		var json = Json.of(a);
		assertTrue(json.contains("visibleWhen"), json);
		assertTrue(json.contains("\"op\":\"eq\""), json);
	}

	@Test void b02_rowActionOmitsVisibleWhenWhenUnset() {
		assertFalse(Json.of(RowAction.create("x")).contains("visibleWhen"));
	}

	@Test void b03_regionAndFieldCarryVisibleWhenInContract() {
		var rule = VisibilityRule.when("facts.admin").eq(true);
		var region = RegionDef.create("r").visibleWhen(rule)
			.fields(RegionDef.Field.of("a").visibleWhen(rule), RegionDef.Field.of("b"));
		var m = region.toContractMap();
		assertEquals(List.of(rule.toMap()), m.get("visibleWhen"));
		var fields = (List<?>)m.get("fields");
		assertEquals(List.of(rule.toMap()), ((Map<?,?>)fields.get(0)).get("visibleWhen"));
		assertFalse(((Map<?,?>)fields.get(1)).containsKey("visibleWhen"));
	}

	@Test void b04_enabledWhenRejectsCollectionOps() {
		assertThrows(IllegalArgumentException.class,
			() -> org.apache.juneau.rest.server.views.RowActionEnabledRule.of("s", org.apache.juneau.rest.server.widgets.Op.IN, "x", "r"));
	}
}
