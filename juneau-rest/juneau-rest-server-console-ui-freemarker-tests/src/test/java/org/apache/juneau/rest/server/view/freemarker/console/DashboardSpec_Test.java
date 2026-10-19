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

class DashboardSpec_Test extends TestBase {

	private static final List<Map<String,Object>> ROWS = List.of(Map.of("d", "2026-10-01", "v", 3));

	private static ChartDef daily() {
		return ChartDef.line("daily").data(ROWS).x("d").series("v", "V").publishZoom();
	}

	private static ChartDef byReason() {
		return ChartDef.bar("by-reason").data(ROWS).x("d").series("v", "V");
	}

	private static DashboardSpec full() {
		return DashboardSpec.create("noqc").title("No-QC")
			.rail(Rail.left().width(300).sections(
				RangeSection.quick("7d", "30d").defaultPreset("30d").publishes(Topics.filter("range")),
				ChoiceSection.fromDim("orders", "reason").publishes(Topics.filter("reasons")),
				CardSection.of(ChartDef.donut("share").data(ROWS).label("d").value("v"))))
			.grid(2)
			.panel(daily())
			.panel(byReason())
			.panel("notes", 2)
			.status(Topics.data("orders"))
			.persist(PersistSpec.page("app.noqc").version(2).topics(Topics.filter("range"), Topics.filter("reasons"), Topics.zoom("daily")).shareLink());
	}

	private static Map<String,CardSpec> page(String...htmlIds) {
		var m = new LinkedHashMap<String,CardSpec>();
		for (var id : htmlIds)
			m.put(id, CardSpec.of("html", id));
		return m;
	}

	private static String e77(Runnable r) {
		return assertThrows(IllegalArgumentException.class, r::run).getMessage();
	}

	@Test void a01_shape() {
		var json = full().toJson(page("notes"));
		assertEquals(Json5.to("{id:'noqc',title:'No-QC',grid:2,"
			+ "panels:[{card:'daily',span:1},{card:'by-reason',span:1},{card:'notes',span:2}],"
			+ "rail:{side:'left',width:300,sections:["
			+ "{id:'range',kind:'range',topic:'filter:range',presets:['7d','30d'],custom:false,defaultPreset:'30d'},"
			+ "{id:'reasons',kind:'choice',topic:'filter:reasons',source:'orders',dim:'reason',max:50},"
			+ "{kind:'card',card:'share'}]},"
			+ "status:['data:orders'],"
			+ "persist:{ns:'app.noqc',version:2,topics:['filter:range','filter:reasons','zoom:daily'],shareLink:true,decoders:[]}}", Map.class),
			Json5.to(Json.of(json), Map.class));
	}

	@Test void a02_sectionTopics_ownedBySection() {
		// filter: is a framework family (E-45 for a TopicDecl), so ownership is the section JSON's topic,
		// which the wiring validator reads.
		var json = full().toJson(page("notes"));
		var sections = (List<?>)((Map<?,?>)json.get("rail")).get("sections");
		assertEquals("filter:range", ((Map<?,?>)sections.get(0)).get("topic"));
		assertEquals("range", ((Map<?,?>)sections.get(0)).get("id"));
	}

	@Test void a03_ownedCards() {
		var ids = full().ownedCards().stream().map(CardSpec::id).toList();
		assertEquals(List.of("daily", "by-reason", "share"), ids);
	}

	@Test void a04_minimal_defaults() {
		var json = DashboardSpec.create("d").panel(byReason()).toJson();
		assertEquals(Json5.to("{id:'d',grid:2,panels:[{card:'by-reason',span:1}]}", Map.class), Json5.to(Json.of(json), Map.class));
	}

	@Test void a05_gridAndSpanErrors() {
		assertEquals("E-77: dashboard 'd': grid 5 is outside 1-4", e77(() -> DashboardSpec.create("d").grid(5).toJson()));
		assertEquals("E-77: dashboard 'd': grid 0 is outside 1-4", e77(() -> DashboardSpec.create("d").grid(0).toJson()));
		assertEquals("E-77: dashboard 'd': panel 'by-reason' span 3 is outside 1-2",
			e77(() -> DashboardSpec.create("d").panel(byReason(), 3).toJson()));
		assertEquals("E-77: dashboard 'd': panel 'by-reason' span 0 is outside 1-2",
			e77(() -> DashboardSpec.create("d").panel(byReason(), 0).toJson()));
	}

	@Test void a06_idErrors() {
		assertEquals("E-77: dashboard '1d': dashboard id '1d' is not a valid id", e77(() -> DashboardSpec.create("1d").toJson()));
		assertEquals("E-77: dashboard 'd': card 'by-reason' is in the dashboard twice",
			e77(() -> DashboardSpec.create("d").panel(byReason()).panel(byReason()).toJson()));
		assertEquals("E-77: dashboard 'd': section id 'r' is used twice",
			e77(() -> DashboardSpec.create("d").rail(Rail.left().sections(
				SearchSection.create().publishes(Topics.filter("r")), SearchSection.create().publishes(Topics.filter("r")))).toJson()));
		assertEquals("E-77: dashboard 'd': section id 'by-reason' duplicates card 'by-reason'",
			e77(() -> DashboardSpec.create("d").panel(byReason()).rail(Rail.left().sections(
				SearchSection.create().publishes(Topics.filter("by-reason")))).toJson()));
		assertEquals("E-77: dashboard 'd': section id 'notes' duplicates card 'notes'",
			e77(() -> DashboardSpec.create("d").rail(Rail.left().sections(
				SearchSection.create().publishes(Topics.filter("notes")))).toJson(page("notes"))));
	}

	@Test void a07_panelErrors() {
		assertEquals("E-77: dashboard 'd': panel 'notes' names no card on this page",
			e77(() -> DashboardSpec.create("d").panel("notes").toJson()));
		assertEquals("E-77: dashboard 'd': card 'h' has type 'html', which has a dedicated PageSpec method; add it with PageSpec.html(...) and use panel(\"h\")",
			e77(() -> DashboardSpec.create("d").panel(CardSpec.of("html", "h")).toJson()));
		assertEquals("E-77: dashboard 'd': card 'notes' is bare; a dashboard member needs a card host",
			e77(() -> DashboardSpec.create("d").panel("notes").toJson(Map.of("notes", CardSpec.of("html", "notes").bare(true)))));
	}

	@Test void a08_statusErrors() {
		assertEquals("E-77: dashboard 'd': status topic 'filter:x' is not a data: topic",
			e77(() -> DashboardSpec.create("d").status(Topics.filter("x")).toJson()));
	}

	@Test void a09_persistMembership() {
		assertEquals("E-77: dashboard 'd': persist topic 'filter:nope' names no section of this dashboard",
			e77(() -> DashboardSpec.create("d").persist(PersistSpec.page("a").topics(Topics.filter("nope"))).toJson()));
		assertEquals("E-77: dashboard 'd': persist topic 'zoom:by-reason' is not published by a card of this dashboard",
			e77(() -> DashboardSpec.create("d").panel(byReason()).persist(PersistSpec.page("a").topics(Topics.zoom("by-reason"))).toJson()));
		assertEquals("E-77: dashboard 'd': persist topic 'legend:daily' is not published by a card of this dashboard",
			e77(() -> DashboardSpec.create("d").panel(daily()).persist(PersistSpec.page("a").topics("legend:daily")).toJson()));
	}

	@Test void a10_sectionAndPersistChecksRun() {
		assertEquals("E-77: dashboard 'd': rail width 100 is outside 200-480",
			e77(() -> DashboardSpec.create("d").rail(Rail.left().width(100)).toJson()));
		assertEquals("E-77: dashboard 'd': persist version 0 must be at least 1",
			e77(() -> DashboardSpec.create("d").rail(Rail.left().sections(SearchSection.create().publishes(Topics.filter("s"))))
				.persist(PersistSpec.page("a").version(0).topics(Topics.filter("s"))).toJson()));
		assertEquals("E-77: dashboard 'd': custom section name 'x' is not a dotted extension name",
			e77(() -> DashboardSpec.create("d").rail(Rail.left().sections(Section.custom("x", JsonMap.of()).publishes(Topics.filter("s")))).toJson()));
	}

	@Test void a11_gridAndSpanBoundaries() {
		assertEquals(4, DashboardSpec.create("d").grid(4).panel(byReason(), 4).toJson().get("grid"));
		assertEquals(1, DashboardSpec.create("d").grid(1).panel(byReason(), 1).toJson().get("grid"));
	}

	@Test void a12_datatablesPanel() {
		assertEquals("E-77: dashboard 'd': card 't' has type 'datatables', which has a dedicated PageSpec method; add it with PageSpec.table(...) and use panel(\"t\")",
			e77(() -> DashboardSpec.create("d").panel(CardSpec.of("datatables", "t")).toJson()));
	}

	@Test void a13_persistOwnedTopics() {
		var legend = ChartDef.donut("share").data(ROWS).label("d").value("v").publishLegend();
		assertDoesNotThrow(() -> DashboardSpec.create("d").panel(legend).persist(PersistSpec.page("a").topics(Topics.legend("share"))).toJson());
		var zoomCard = ChartDef.line("z").data(ROWS).x("d").series("v", "V").publishZoom();
		assertDoesNotThrow(() -> DashboardSpec.create("d").rail(Rail.left().sections(CardSection.of(zoomCard)))
			.persist(PersistSpec.page("a").topics(Topics.zoom("z"))).toJson());
	}

	@Test void a14_badStatusKey() {
		assertEquals("E-77: dashboard 'd': status topic 'data:a b' is not a data: topic",
			e77(() -> DashboardSpec.create("d").status("data:a b").toJson()));
	}

	@Test void a15_cardSectionCollidesWithPanel() {
		assertEquals("E-77: dashboard 'd': card 'by-reason' is in the dashboard twice",
			e77(() -> DashboardSpec.create("d").panel(byReason()).rail(Rail.left().sections(CardSection.of(byReason()))).toJson()));
	}

	@Test void a16_nullPanelId() {
		assertEquals("E-77: dashboard 'd': panel 'null' names no card on this page",
			e77(() -> DashboardSpec.create("d").panel((String)null).toJson()));
		var target = DashboardSpec.create("d");
		assertThrows(NullPointerException.class, () -> target.panel((ChartDef)null));
	}

	@Test void a17_duplicateStatusTopic() {
		assertEquals("E-77: dashboard 'd': status lists topic 'data:orders' twice",
			e77(() -> DashboardSpec.create("d").status(Topics.data("orders"), Topics.data("orders")).toJson()));
	}
}
