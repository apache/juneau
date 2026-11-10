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
package org.apache.juneau.console.freemarker;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.console.*;
import org.junit.jupiter.api.*;

class SwitcherDef_Test extends TestBase {

	private static CardSpec chart(String id) {
		return ChartDef.line(id).src("/x").x("d").series("n", "N").toCardSpec();
	}

	private static String msg(Runnable r) {
		return assertThrows(IllegalArgumentException.class, r::run).getMessage();
	}

	@Test void a01_singleSectionSerializesAsCard() {
		var m = SwitcherDef.create("sw").title("Revenue")
			.view("chart", "Chart", v -> v.section(chart("c1")))
			.initialView("chart").toCardSpec().toMap();
		assertEquals("sw", m.get("id"));
		assertEquals("switcher", m.get("type"));
		assertEquals("Revenue", m.get("title"));
		assertEquals("chart", m.get("initialView"));
		var views = (List<?>)m.get("views");
		assertEquals(1, views.size());
		var v0 = (Map<?,?>)views.get(0);
		assertEquals("chart", v0.get("id"));
		assertEquals("Chart", v0.get("label"));
		assertEquals("c1", ((Map<?,?>)v0.get("card")).get("id"));
		assertFalse(v0.containsKey("sections"));
	}

	@Test void a02_twoSectionsSerializeAsSections() {
		var m = SwitcherDef.create("sw")
			.view("v", "V", v -> v.section("a", "A", chart("c1")).section("b", "B", chart("c2")).initialSection("b"))
			.toCardSpec().toMap();
		var v0 = (Map<?,?>)((List<?>)m.get("views")).get(0);
		assertFalse(v0.containsKey("card"));
		assertEquals("b", v0.get("initialSection"));
		var secs = (List<?>)v0.get("sections");
		assertEquals(2, secs.size());
		assertEquals("a", ((Map<?,?>)secs.get(0)).get("id"));
		assertEquals("A", ((Map<?,?>)secs.get(0)).get("label"));
		assertEquals("c2", ((Map<?,?>)((Map<?,?>)secs.get(1)).get("card")).get("id"));
	}

	@Test void a03_noViews() {
		assertEquals("SwitcherDef 'sw' requires at least one view.", msg(() -> SwitcherDef.create("sw").toCardSpec()));
	}

	@Test void a04_viewWithoutSections() {
		assertEquals("SwitcherDef 'sw' view 'v' requires a card or at least one section.",
			msg(() -> SwitcherDef.create("sw").view("v", "V", v -> {}).toCardSpec()));
	}

	@Test void a05_duplicateViewId() {
		assertEquals("SwitcherDef 'sw' view id 'v' is declared more than once.",
			msg(() -> SwitcherDef.create("sw").view("v", "V", v -> v.section(chart("c1"))).view("v", "W", v -> v.section(chart("c2"))).toCardSpec()));
	}

	@Test void a06_duplicateSectionId() {
		assertEquals("SwitcherDef 'sw' view 'v' section id 'a' is declared more than once.",
			msg(() -> SwitcherDef.create("sw").view("v", "V", v -> v.section("a", "A", chart("c1")).section("a", "B", chart("c2"))).toCardSpec()));
	}

	@Test void a07_badIdGrammar() {
		for (var bad : List.of("a.b", "a:b", "a~b", "1a", "", "a b")) {
			assertTrue(msg(() -> SwitcherDef.create("sw").view(bad, "V", v -> v.section(chart("c1"))).toCardSpec())
				.startsWith("SwitcherDef 'sw' view id '" + bad + "' must match "), bad);
			assertTrue(msg(() -> SwitcherDef.create("sw").view("v", "V", v -> v.section(bad, "A", chart("c1")).section("z", "Z", chart("c2"))).toCardSpec())
				.startsWith("SwitcherDef 'sw' view 'v' section id '" + bad + "' must match "), bad);
		}
	}

	@Test void a08_nestedSwitcherRefused() {
		var inner = SwitcherDef.create("in").view("x", "X", v -> v.section(chart("c1"))).toCardSpec();
		assertEquals("SwitcherDef 'sw' view 'v' cannot hold a switcher card ('in').",
			msg(() -> SwitcherDef.create("sw").view("v", "V", v -> v.section(inner)).toCardSpec()));
	}

	@Test void a09_unknownInitials() {
		assertEquals("SwitcherDef 'sw' initialView 'zz' names no view.",
			msg(() -> SwitcherDef.create("sw").view("v", "V", v -> v.section(chart("c1"))).initialView("zz").toCardSpec()));
		assertEquals("SwitcherDef 'sw' view 'v' initialSection 'zz' names no section.",
			msg(() -> SwitcherDef.create("sw").view("v", "V", v -> v.section("a", "A", chart("c1")).section("b", "B", chart("c2")).initialSection("zz")).toCardSpec()));
	}

	@Test void a10_mixedFormsRefusedByValidate() {
		var view = new LinkedHashMap<String,Object>();
		view.put("id", "v");
		view.put("label", "V");
		view.put("card", chart("c1").toMap());
		view.put("sections", List.of(Map.of("id", "a", "label", "A", "card", chart("c2").toMap())));
		var body = new LinkedHashMap<String,Object>();
		body.put("views", List.of(view));
		assertEquals("SwitcherDef 'sw' view 'v' takes either a card or sections, not both.", msg(() -> SwitcherDef.validate("sw", body)));
	}

	@Test void a11_duplicateNestedCardId() {
		assertEquals("SwitcherDef 'sw' card id 'c1' is declared more than once.",
			msg(() -> SwitcherDef.create("sw").view("v", "V", v -> v.section(chart("c1"))).view("w", "W", v -> v.section(chart("c1"))).toCardSpec()));
	}

	@Test void a12_modelOnViewAndSection() {
		var m = SwitcherDef.create("sw")
			.view("v", "V", v -> v.on("activated", "orders.shown").section("a", "A", chart("c1"), s -> s.on("activated", "orders.sec")).section("b", "B", chart("c2")))
			.toCardSpec().toMap();
		var v0 = (Map<?,?>)((List<?>)m.get("views")).get(0);
		assertEquals("orders.shown", ((Map<?,?>)((Map<?,?>)v0.get("model")).get("on")).get("activated"));
		var s0 = (Map<?,?>)((List<?>)v0.get("sections")).get(0);
		assertEquals("orders.sec", ((Map<?,?>)((Map<?,?>)s0.get("model")).get("on")).get("activated"));
	}

	@Test void a13_singleSectionWithModelKeepsSectionsForm() {
		var m = SwitcherDef.create("sw")
			.view("v", "V", v -> v.section("main", "Main", chart("c1"), s -> s.on("activated", "orders.sec")))
			.toCardSpec().toMap();
		assertTrue(((Map<?,?>)((List<?>)m.get("views")).get(0)).containsKey("sections"));
	}

	@Test void a14_wiringKeptVerbatimInNestedMap() {
		var c = ChartDef.line("c").src("/x").x("d").series("n", "N").on("click", "c.click").toCardSpec();
		var m = SwitcherDef.create("sw").view("v", "V", v -> v.section(c)).toCardSpec().toMap();
		var card = (Map<?,?>)((Map<?,?>)((List<?>)m.get("views")).get(0)).get("card");
		assertEquals(c.toMap().get("publishes"), card.get("publishes"));
	}
}
