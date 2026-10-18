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
package org.apache.juneau.rest.server.view.freemarker.console;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.console.*;
import org.junit.jupiter.api.*;

class Section_Test extends TestBase {

	private static Map<?,?> map(Section s) {
		s.check("d");
		return Json5.to(Json.of(s.toMap()), Map.class);
	}

	private static String e77(Runnable r) {
		return assertThrows(IllegalArgumentException.class, r::run).getMessage();
	}

	@Test void a01_range_shape() {
		var s = RangeSection.quick("7d", "30d", "ytd").custom(true).defaultPreset("30d").label("Range").publishes(Topics.filter("range"));
		assertEquals(Json5.to("{id:'range',kind:'range',topic:'filter:range',label:'Range',presets:['7d','30d','ytd'],custom:true,defaultPreset:'30d'}", Map.class), map(s));
	}

	@Test void a02_range_defaults() {
		var s = RangeSection.quick("24h").publishes(Topics.filter("r"));
		assertEquals(Json5.to("{id:'r',kind:'range',topic:'filter:r',presets:['24h'],custom:false,defaultPreset:'24h'}", Map.class), map(s));
	}

	@Test void a03_range_errors() {
		assertEquals("E-77: dashboard 'd': range section 'r' needs at least one preset",
			e77(() -> RangeSection.quick().publishes(Topics.filter("r")).check("d")));
		assertEquals("E-77: dashboard 'd': range section 'r' preset '30x' is not Nd, Nh or ytd",
			e77(() -> RangeSection.quick("30x").publishes(Topics.filter("r")).check("d")));
		assertEquals("E-77: dashboard 'd': range section 'r' lists preset '7d' twice",
			e77(() -> RangeSection.quick("7d", "7d").publishes(Topics.filter("r")).check("d")));
		assertEquals("E-77: dashboard 'd': range section 'r' default preset '90d' is not one of its presets",
			e77(() -> RangeSection.quick("7d").defaultPreset("90d").publishes(Topics.filter("r")).check("d")));
	}

	@Test void a04_search_shape() {
		var s = SearchSection.create().placeholder("status:open").publishes(Topics.filter("search"));
		assertEquals(Json5.to("{id:'search',kind:'search',topic:'filter:search',placeholder:'status:open'}", Map.class), map(s));
	}

	@Test void a05_choice_shapeAndErrors() {
		var s = ChoiceSection.fromDim("orders", "reason").label("Reason").max(3).publishes(Topics.filter("reasons"));
		assertEquals(Json5.to("{id:'reasons',kind:'choice',topic:'filter:reasons',label:'Reason',source:'orders',dim:'reason',max:3}", Map.class), map(s));
		assertEquals(50, ((Number)map(ChoiceSection.fromDim("o", "r").publishes(Topics.filter("c"))).get("max")).intValue());
		assertEquals("E-77: dashboard 'd': choice section 'c' source 'a b' is not a valid topic key",
			e77(() -> ChoiceSection.fromDim("a b", "r").publishes(Topics.filter("c")).check("d")));
		assertEquals("E-77: dashboard 'd': choice section 'c' dim '' is not a valid topic key",
			e77(() -> ChoiceSection.fromDim("o", "").publishes(Topics.filter("c")).check("d")));
		assertEquals("E-77: dashboard 'd': choice section 'c' max must be at least 1",
			e77(() -> ChoiceSection.fromDim("o", "r").max(0).publishes(Topics.filter("c")).check("d")));
	}

	@Test void a06_publishes_errors() {
		assertEquals("E-77: dashboard 'd': a search section must call publishes(Topics.filter(...))",
			e77(() -> SearchSection.create().check("d")));
		assertEquals("E-77: dashboard 'd': section topic 'selection:x' is not a filter: topic",
			e77(() -> SearchSection.create().publishes(Topics.selection("x")).check("d")));
		assertEquals("E-77: dashboard 'd': section id 'a.b' is not a valid id",
			e77(() -> SearchSection.create().publishes(Topics.filter("a.b")).check("d")));
	}

	private static ChartDef share() {
		return ChartDef.donut("share").data(List.of(Map.of("reason", "late", "count", 4))).label("reason").value("count");
	}

	@Test void a07_card_shapeAndErrors() {
		var s = CardSection.of(share());
		assertEquals(Json5.to("{kind:'card',card:'share'}", Map.class), map(s));
		assertEquals("share", s.cards().get(0).id());
		assertEquals("E-77: dashboard 'd': card section 'share' publishes through its card; remove publishes(...)",
			e77(() -> CardSection.of(share()).publishes(Topics.filter("x")).check("d")));
		assertEquals("E-77: dashboard 'd': card 'share' is bare; a dashboard member needs a card host",
			e77(() -> CardSection.of(share().toCardSpec().bare(true)).check("d")));
		assertEquals("E-77: dashboard 'd': card 'note' has type 'html', which has a dedicated PageSpec method; add it with PageSpec.html(...) and use panel(\"note\")",
			e77(() -> CardSection.of(CardSpec.of("html", "note")).check("d")));
	}

	@Test void a08_custom_shapeAndErrors() {
		var s = Section.custom("app.goto", JsonMap.of("mode", "dsl")).label("Go to").publishes(Topics.filter("goto"));
		assertEquals(Json5.to("{id:'goto',kind:'custom',topic:'filter:goto',label:'Go to',name:'app.goto',args:{mode:'dsl'}}", Map.class), map(s));
		assertEquals("E-77: dashboard 'd': custom section name 'goto' is not a dotted extension name",
			e77(() -> Section.custom("goto", JsonMap.of()).publishes(Topics.filter("g")).check("d")));
		assertEquals("E-77: dashboard 'd': custom section name 'App.goto' is not a dotted extension name",
			e77(() -> Section.custom("App.goto", JsonMap.of()).publishes(Topics.filter("g")).check("d")));
	}

	@Test void a09_rail() {
		var r = Rail.right().width(320).sections(SearchSection.create().publishes(Topics.filter("s")));
		r.check("d");
		assertEquals(Json5.to("{side:'right',width:320,sections:[{id:'s',kind:'search',topic:'filter:s'}]}", Map.class),
			Json5.to(Json.of(r.toMap()), Map.class));
		assertEquals(280, ((Number)Rail.left().toMap().get("width")).intValue());
		assertEquals("E-77: dashboard 'd': rail width 199 is outside 200-480",
			e77(() -> Rail.left().width(199).check("d")));
		assertEquals("E-77: dashboard 'd': rail width 481 is outside 200-480",
			e77(() -> Rail.left().width(481).check("d")));
	}

	@Test void a10_boundaries() {
		Rail.left().width(200).check("d");
		Rail.left().width(480).check("d");
		ChoiceSection.fromDim("o", "r").max(1).publishes(Topics.filter("c")).check("d");
	}

	@Test void a11_datatablesOwnedCard() {
		assertEquals("E-77: dashboard 'd': card 't' has type 'datatables', which has a dedicated PageSpec method; add it with PageSpec.table(...) and use panel(\"t\")",
			e77(() -> CardSection.of(CardSpec.of("datatables", "t")).check("d")));
	}

	@Test void a12_percentInValueIsHarmless() {
		assertEquals("E-77: dashboard 'd': range section 'r' preset '5%d' is not Nd, Nh or ytd",
			e77(() -> RangeSection.quick("5%d").publishes(Topics.filter("r")).check("d")));
	}

	@Test void a13_nullInputs() {
		assertEquals("E-77: dashboard 'd': range section 'r' preset 'null' is not Nd, Nh or ytd",
			e77(() -> RangeSection.quick("7d", null).publishes(Topics.filter("r")).check("d")));
		var target = Rail.left();
		assertThrows(NullPointerException.class, () -> target.sections((Section)null));
		assertThrows(NullPointerException.class, () -> CardSection.of((CardSpec)null));
		assertThrows(NullPointerException.class, () -> CardSection.of((ChartDef)null));
	}

	@Test void a14_customArgsCopied() {
		var args = JsonMap.of("mode", "dsl");
		var s = Section.custom("app.goto", args).publishes(Topics.filter("goto"));
		args.put("mode", "changed");
		assertEquals("dsl", ((Map<?,?>)map(s).get("args")).get("mode"));
	}

	@Test void a15_choice_reservedDimNames() {
		for (var dim : List.of("__proto__", "constructor", "prototype"))
			assertEquals("E-77: dashboard 'd': choice section 'c' dim '" + dim + "' is a reserved name",
				e77(() -> ChoiceSection.fromDim("o", dim).publishes(Topics.filter("c")).check("d")));
	}
}
