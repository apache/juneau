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

import static org.apache.juneau.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.console.*;
import org.apache.juneau.console.views.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

class ChartDef_Test extends TestBase {

	private static Object j(String json5) { return Json5.to(json5, Object.class); }

	private static Map<?,?> map(ChartDef d) {
		return Json5.to(Json.of(d.toCardSpec().toMap()), Map.class);
	}

	@Test void a01_line_fullShape() {
		var d = ChartDef.line("orders-trend").title("Orders").src("/console/store/stats/daily")
			.x("day", "category", "Day").y("Orders", 0, "integer")
			.series("placed", "Placed").series("delivered", "Delivered", "ok")
			.legend("bottom").height(260).refreshMs(60_000).onClickHref("/console/store/orders?day={day}");
		assertEquals(Json5.to("{id:'orders-trend',type:'chart',title:'Orders',src:'/console/store/stats/daily',kind:'line',"
			+ "x:{field:'day',type:'category',label:'Day'},y:{label:'Orders',min:0,format:'integer'},legend:'bottom',height:260,"
			+ "refreshMs:60000,onClick:{href:'/console/store/orders?day={day}'},"
			+ "series:[{field:'placed',label:'Placed'},{field:'delivered',label:'Delivered',tone:'ok'}]}", Map.class), map(d));
	}

	@Test void a02_bar_stackedHorizontal_numericTone() {
		var m = map(ChartDef.bar("b").src("/s").x("k").series("v", "V", 3).stacked(true).horizontal());
		assertEquals(true, m.get("stacked"));
		assertEquals("horizontal", m.get("orientation"));
		assertEquals(Map.of("field", "k"), m.get("x"));
		assertEquals(List.of(Map.of("field", "v", "label", "V", "tone", 3)), m.get("series"));
	}

	@Test void a03_donut_inlineData_center() {
		var rows = List.<Map<String,Object>>of(Map.of("status", "placed", "count", 3));
		var m = map(ChartDef.donut("mix").label("status").value("count").center("total").data(rows).emptyText("Nothing yet."));
		assertEquals(Json5.to("{id:'mix',type:'chart',kind:'donut',label:'status',value:'count',center:'total',"
			+ "data:[{status:'placed',count:3}],emptyText:'Nothing yet.'}", Map.class), m);
	}

	@Test void a04_heatmap_helperForm() {
		var m = map(ChartDef.heatmap("h").src("/h").x("fi").yField("reason").value("count").scale(0, 100));
		assertEquals(j("{id:'h',type:'chart',src:'/h',helper:{kind:'heatmap',x:{field:'fi'},y:{field:'reason'},value:'count',scale:{min:0,max:100}}}"), m);
	}

	@Test void a05_visibleWhen_carried() {
		var spec = ChartDef.line("t").src("/s").x("d").series("v", "V").visibleWhen(VisibilityRule.when("role").present()).toCardSpec();
		assertNotNull(spec.toMap().get("visibleWhen"));
	}

	@Test void a06_errors() {
		var target = ChartDef.line("t").x("d").series("v", "V");
		assertEquals("ChartDef 't' requires exactly one of src(...), data(...) or source(...).",
			assertThrows(IllegalArgumentException.class, target::toCardSpec).getMessage());
		var target2 = ChartDef.donut("t").src("/s").data(List.of());
		assertEquals("ChartDef 't' requires exactly one of src(...), data(...) or source(...).",
			assertThrows(IllegalArgumentException.class, target2::toCardSpec).getMessage());
		var target3 = ChartDef.line("t").src("/s").x("d");
		assertEquals("ChartDef 't' kind 'line' requires x(...) and at least one series(...).",
			assertThrows(IllegalArgumentException.class, target3::toCardSpec).getMessage());
		var target4 = ChartDef.line("t");
		assertEquals("ChartDef 't' series tone '9' must be 1-8.",
			assertThrows(IllegalArgumentException.class, () -> target4.series("v", "V", 9)).getMessage());
		var target5 = ChartDef.line("t");
		assertEquals("ChartDef 't' series tone 'pink' must be 'danger', 'ok' or 'warning'.",
			assertThrows(IllegalArgumentException.class, () -> target5.series("v", "V", "pink")).getMessage());
		var target6 = ChartDef.line("t");
		assertEquals("ChartDef 't' x type 'date' must be 'category', 'number' or 'time'.",
			assertThrows(IllegalArgumentException.class, () -> target6.x("d", "date", null)).getMessage());
	}

	@Test void a06d_errors_optionLimits() {
		var target7 = ChartDef.line("t");
		assertEquals("ChartDef 't' legend 'top' must be 'bottom', 'right' or 'none'.",
			assertThrows(IllegalArgumentException.class, () -> target7.legend("top")).getMessage());
		var target8 = ChartDef.line("t");
		assertEquals("ChartDef 't' height '100' is below the minimum '120'.",
			assertThrows(IllegalArgumentException.class, () -> target8.height(100)).getMessage());
		var target9 = ChartDef.line("t");
		assertEquals("ChartDef 't' refreshMs '1000' is below the minimum '5000'.",
			assertThrows(IllegalArgumentException.class, () -> target9.refreshMs(1000)).getMessage());
		var target10 = ChartDef.line("t");
		var arg4 = List.of();
		assertEquals("ChartDef 't' key 'series' has a dedicated method.",
			assertThrows(IllegalArgumentException.class, () -> target10.set("series", arg4)).getMessage());
		var target11 = ChartDef.line("t");
		assertEquals("ChartDef 't' key 'src' has a dedicated method.",
			assertThrows(IllegalArgumentException.class, () -> target11.set("src", "/x")).getMessage());
	}

	@Test void a06e_errors_hrefDataAndRequiredFields() {
		var target12 = ChartDef.line("t");
		var target13 = ChartDef.line("t");
		var target14 = ChartDef.donut("t").data(List.of()).refreshMs(6000);
		assertEquals("ChartDef 't' onClick href must be a same-origin path; got 'https://evil.test/x'.",
			assertThrows(IllegalArgumentException.class, () -> target12.onClickHref("https://evil.test/x")).getMessage());
		assertEquals("ChartDef 't' onClick href must be a same-origin path; got '//evil.test/x'.",
			assertThrows(IllegalArgumentException.class, () -> target13.onClickHref("//evil.test/x")).getMessage());
		assertEquals("ChartDef 't' refreshMs(...) needs src(...); inline data never refreshes.",
			assertThrows(IllegalArgumentException.class, target14::toCardSpec).getMessage());
		var target15 = ChartDef.donut("t");
		assertEquals("ChartDef 't' data rows must not be null.",
			assertThrows(IllegalArgumentException.class, () -> target15.data(null)).getMessage());
		var target16 = ChartDef.line("t");
		assertEquals("ChartDef 't' onClick href must be a same-origin path; got 'null'.",
			assertThrows(IllegalArgumentException.class, () -> target16.onClickHref(null)).getMessage());
		var target17 = ChartDef.donut("t").src("/s").label("s");
		assertEquals("ChartDef 't' kind 'donut' requires label(...) and value(...).",
			assertThrows(IllegalArgumentException.class, target17::toCardSpec).getMessage());
		var target18 = ChartDef.donut("t").src("/s").value("n");
		assertEquals("ChartDef 't' kind 'donut' requires label(...) and value(...).",
			assertThrows(IllegalArgumentException.class, target18::toCardSpec).getMessage());
		var target19 = ChartDef.donut("t").src("/s").label(" ").value("n");
		assertEquals("ChartDef 't' kind 'donut' requires label(...) and value(...).",
			assertThrows(IllegalArgumentException.class, target19::toCardSpec).getMessage());
		var target20 = ChartDef.line("t");
		assertEquals("ChartDef 't' series requires a non-blank field.",
			assertThrows(IllegalArgumentException.class, () -> target20.series(null, "V")).getMessage());
		var target21 = ChartDef.bar("t");
		assertEquals("ChartDef 't' series requires a non-blank field.",
			assertThrows(IllegalArgumentException.class, () -> target21.series(" ", "V", 2)).getMessage());
		var target22 = ChartDef.bar("t");
		assertEquals("ChartDef 't' series requires a non-blank field.",
			assertThrows(IllegalArgumentException.class, () -> target22.series("", "V", "ok")).getMessage());
	}

	@ParameterizedTest
	@ValueSource(strings = {"publishes", "subscribes", "template", "bare", "class"})
	void a06b_cardLevelKeys_pointAtToCardSpec(String key) {
		var target23 = ChartDef.line("t");
		assertEquals("ChartDef 't' key '" + key + "' is card-level; set it on toCardSpec().",
			assertThrows(IllegalArgumentException.class, () -> target23.set(key, "x")).getMessage());
	}

	@Test void a06c_wiringGoesOnTheCardSpec() {
		var spec = ChartDef.bar("g").src("/s?q={q}").x("k").series("v", "V").toCardSpec()
			.subscribes(Subscription.to(Topics.filter("changes")).as("params").map("q", "search"));
		assertNotNull(spec.toMap().get("subscribes"));
	}

	@Test void a07_cardSpec_bodyMap_isUnmodifiableCopy() {
		var spec = ChartDef.line("t").src("/s").x("d").series("v", "V").toCardSpec();
		var body = spec.bodyMap();
		assertEquals("line", body.get("kind"));
		assertThrows(UnsupportedOperationException.class, () -> body.put("kind", "bar"));
	}

	@Test void b01_legacyForm_byteIdentical() {
		// The legacy output of this exact call chain (a01) must not change by one key or order.
		var before = "{id:'orders-trend',type:'chart',title:'Orders',src:'/console/store/stats/daily',kind:'line',"
			+ "x:{field:'day'},series:[{field:'placed',label:'Placed'},{field:'delivered',label:'Delivered'}]}";
		var built = map(ChartDef.line("orders-trend").title("Orders").src("/console/store/stats/daily")
			.x("day").series("placed", "Placed").series("delivered", "Delivered"));
		assertEquals(j(before), built);
		assertEquals(new ArrayList<>(((Map<?,?>)j(before)).keySet()), new ArrayList<>(built.keySet()));
	}

	@Test void b02_scatter_pie_raw() {
		assertEquals(j("{id:'s',type:'chart',src:'/s',helper:{kind:'scatter',x:{field:'a',type:'number'},series:[{field:'b',label:'B'}],symbolSize:{'$fn':'symbolSizeBy',args:{field:'c'}}}}"),
			map(ChartDef.scatter("s").src("/s").x("a", "number", null).series("b", "B").symbolSizeBy("c")));
		assertEquals(j("{id:'p',type:'chart',data:[{k:'a',v:1}],helper:{kind:'pie',label:'k',value:'v'}}"),
			map(ChartDef.pie("p").data(List.of(Map.of("k", "a", "v", 1))).label("k").value("v")));
		assertEquals(j("{id:'r',type:'chart',src:'/r',option:{series:[{type:'gauge'}]}}"),
			map(ChartDef.raw("r").src("/r").option("{series:[{type:'gauge'}]}")));
	}

	@Test void b03_raw_requiresOption() {
		var target24 = ChartDef.raw("r").src("/r");
		var e = assertThrows(IllegalArgumentException.class, target24::toCardSpec);
		assertString("ChartDef 'r' raw(...) requires option(...).", e.getMessage());
	}

	@Test void b04_option_json5ParseError_failsToCardSpec() {
		var d = ChartDef.line("t").src("/s").x("d").series("v", "V").option("{series: [");
		var e = assertThrows(IllegalArgumentException.class, d::toCardSpec);
		assertContains("ChartDef 't' option is not valid JSON5", e.getMessage());
	}

	@Test void b05_helperPlusOption() {
		var m = map(ChartDef.line("t").src("/s").x("d").series("v", "V").option(JsonMap.of("grid", JsonMap.of("top", 8))));
		assertEquals("line", m.get("kind"));
		assertEquals(j("{grid:{top:8}}"), m.get("option"));
	}

	@Test void b06_decorate_order_andTopicPublished() {
		var m = map(ChartDef.line("t").src("/s").x("d").series("v", "V")
			.decorate(Markers.now("Today")).decorate("app.grid", JsonMap.of("unit", "week"))
			.decorate(Markers.values(List.of()).onClick("app.release")));
		assertEquals(j("[{use:'markers',now:{label:'Today'}},{use:'app.grid',unit:'week'},{use:'markers',values:[],topic:'app.release'}]"), m.get("decorators"));
		assertEquals(j("[{topic:'app.release',retain:false}]"), m.get("publishes"));
	}

	@Test void b07_decorate_badName_isE74() {
		var target25 = ChartDef.line("t");
		var arg = JsonMap.of();
		var e = assertThrows(IllegalArgumentException.class, () -> target25.decorate("Bad", arg));
		assertString("E-74: 'Bad' is not a valid decorator name", e.getMessage());
		var target26 = ChartDef.line("t");
		var arg2 = JsonMap.of("use", "x");
		assertThrows(IllegalArgumentException.class, () -> target26.decorate("ok", arg2));
	}

	@Test void b08_patch_mergesInOrder_byKeyAndById() {
		var m = map(ChartDef.line("t").src("/s").x("d").series("v", "V")
			.patch("{yAxis:{type:'log'},series:[{id:'v',smooth:true}]}")
			.patch(JsonMap.of("yAxis", JsonMap.of("min", 1), "series", List.of(Map.of("id", "v", "symbol", "none"), Map.of("id", "w")))));
		assertEquals(j("{yAxis:{type:'log',min:1},series:[{id:'v',smooth:true,symbol:'none'},{id:'w'}]}"), m.get("patch"));
	}

	@Test void b09_patch_arrayWithoutIds_replaces() {
		var m = map(ChartDef.line("t").src("/s").x("d").series("v", "V").patch("{color:['#111']}").patch("{color:['#222','#333']}"));
		assertEquals(j("{color:['#222','#333']}"), m.get("patch"));
	}

	@Test void b10_on_addsPublish_andChecksEvent() {
		var m = map(ChartDef.line("t").src("/s").x("d").series("v", "V").on("click", "app.pick"));
		assertEquals(j("{click:'app.pick'}"), m.get("on"));
		assertEquals(j("[{topic:'app.pick',retain:false}]"), m.get("publishes"));
		var target27 = ChartDef.line("t");
		var e = assertThrows(IllegalArgumentException.class, () -> target27.on("Click", "app.pick"));
		assertString("E-74: 'Click' is not a valid event name", e.getMessage());
	}

	@Test void b11_publishZoomLegend() {
		var m = map(ChartDef.line("t").src("/s").x("d").series("v", "V").publishZoom().publishLegend());
		assertEquals(j("[{topic:'zoom:t',retain:true},{topic:'legend:t',retain:true}]"), m.get("publishes"));
	}

	@Test void b12_source_subscribes_andExactlyOne() {
		var m = map(ChartDef.line("t").source(Topics.data("x")).x("d").series("v", "V"));
		assertEquals("data:x", m.get("source"));
		assertEquals(j("[{topic:'data:x',as:'data'}]"), m.get("subscribes"));
		var target28 = ChartDef.line("t").src("/s").source("data:x").x("d").series("v", "V");
		var e = assertThrows(IllegalArgumentException.class, target28::toCardSpec);
		assertString("ChartDef 't' requires exactly one of src(...), data(...) or source(...).", e.getMessage());
		var target29 = ChartDef.line("t");
		assertThrows(IllegalArgumentException.class, () -> target29.source("filter:x"));
	}

	@Test void b13_group_renderer_theme() {
		var m = map(ChartDef.line("t").src("/s").x("d").series("v", "V").group("g").renderer("svg").theme("juneau-open"));
		assertEquals("g", m.get("group"));
		assertEquals("svg", m.get("renderer"));
		assertEquals("juneau-open", m.get("theme"));
		var target30 = ChartDef.line("t");
		assertThrows(IllegalArgumentException.class, () -> target30.renderer("webgl"));
	}

	@Test void b14_largeData_partialFrames_a11y() {
		var m = map(ChartDef.line("t").source("data:x").x("d").series("v", "V").largeData(5000, true).partialFrames().a11y("Requests by day", false));
		assertEquals(j("{threshold:5000,progressive:5000}"), m.get("largeData"));
		assertEquals(true, m.get("partialFrames"));
		assertEquals(j("{summary:'Requests by day',table:false}"), m.get("a11y"));
		assertEquals(j("{threshold:200}"), map(ChartDef.line("t").src("/s").x("d").series("v", "V").largeData(200)).get("largeData"));
	}

	@Test void b15_partialFrames_needsSource() {
		var target31 = ChartDef.line("t").src("/s").x("d").series("v", "V").partialFrames();
		var e = assertThrows(IllegalArgumentException.class, target31::toCardSpec);
		assertString("ChartDef 't' partialFrames() needs source(...).", e.getMessage());
	}

	@Test void b16_kindOnlyMethods_rejectedOnOtherKinds() {
		var target32 = ChartDef.line("t");
		assertThrows(IllegalArgumentException.class, () -> target32.yField("r"));
		var target33 = ChartDef.bar("t");
		assertThrows(IllegalArgumentException.class, () -> target33.symbolSizeBy("r"));
		var target34 = ChartDef.line("t");
		assertThrows(IllegalArgumentException.class, () -> target34.scale(0, 1));
	}

	@Test void b17_heatmap_requiresXyValue() {
		var target35 = ChartDef.heatmap("h").src("/h").x("a");
		var e = assertThrows(IllegalArgumentException.class, target35::toCardSpec);
		assertString("ChartDef 'h' kind 'heatmap' requires x(...), yField(...) and value(...).", e.getMessage());
	}

	@Test void b18_newKeysAreDedicated() {
		var target36 = ChartDef.line("t");
		for (var k : List.of("source", "helper", "option", "patch", "decorators", "on", "group", "renderer", "theme", "largeData", "partialFrames", "a11y", "maps"))
			assertThrows(IllegalArgumentException.class, () -> target36.set(k, 1), k);
	}

	@Test void b19_maps_sameOrigin() {
		assertEquals(j("{world:'/geo/world.json'}"), map(ChartDef.raw("r").src("/r").option("{}").map("world", "/geo/world.json")).get("maps"));
		var target37 = ChartDef.raw("r");
		assertThrows(IllegalArgumentException.class, () -> target37.map("world", "https://x/w.json"));
	}

	@Test void b20_ofIsGone() {
		assertTrue(Arrays.stream(ChartDef.class.getMethods()).noneMatch(x -> x.getName().equals("of")));
	}

	@Test void b21_largeData_thresholdBelowOne_rejected() {
		var target38 = ChartDef.line("t");
		var e = assertThrows(IllegalArgumentException.class, () -> target38.largeData(0));
		assertString("ChartDef 't' largeData threshold '0' must be at least 1.", e.getMessage());
	}

	@Test void b22_group_blank_rejected() {
		var target39 = ChartDef.line("t");
		var e = assertThrows(IllegalArgumentException.class, () -> target39.group(" "));
		assertString("ChartDef 't' group must be non-blank.", e.getMessage());
	}

	@Test void b23_patch_json5ParseError_failsToCardSpec() {
		var d = ChartDef.line("t").src("/s").x("d").series("v", "V").patch("{a: [");
		var e = assertThrows(IllegalArgumentException.class, d::toCardSpec);
		assertContains("ChartDef 't' patch is not valid JSON5", e.getMessage());
	}

	@Test void b24_decorate_customDecoratorWithBadName_isE74() {
		Decorator bad = () -> JsonMap.of("use", "Bad");
		var target40 = ChartDef.line("t");
		var e = assertThrows(IllegalArgumentException.class, () -> target40.decorate(bad));
		assertString("E-74: 'Bad' is not a valid decorator name", e.getMessage());
		Decorator none = () -> JsonMap.of("x", 1);
		var target41 = ChartDef.line("t");
		var e2 = assertThrows(IllegalArgumentException.class, () -> target41.decorate(none));
		assertString("E-74: 'null' is not a valid decorator name", e2.getMessage());
	}

	@Test void b25_decorateWithUse_inArgs_rejected() {
		var target42 = ChartDef.line("t");
		var arg3 = JsonMap.of("use", "y");
		var e = assertThrows(IllegalArgumentException.class, () -> target42.decorate("app.x", arg3));
		assertString("ChartDef 't' decorator 'app.x' args must not set 'use'.", e.getMessage());
	}

	@Test void b28_on_badTopic_doesNotStoreTheEvent() {
		var c = ChartDef.line("t").src("/s").x("d").series("v", "V");
		assertThrows(IllegalArgumentException.class, () -> c.on("click", "Bad Topic!"));
		assertNull(c.toCardSpec().toMap().get("on"));
	}

	@Test void b26_raw_withHelperKeys_rejected() {
		var target43 = ChartDef.raw("r").src("/s").option("{}").x("d");
		var e = assertThrows(IllegalArgumentException.class, target43::toCardSpec);
		assertString("ChartDef 'r' raw(...) takes no helper keys (x, y, series, ...); put them in option(...).", e.getMessage());
		var target44 = ChartDef.raw("r").src("/s").option("{}").series("v", "V");
		assertThrows(IllegalArgumentException.class, target44::toCardSpec);
		assertNotNull(ChartDef.raw("r").src("/s").option("{}").height(200).toCardSpec());
	}

	@Test void b27_publishes_dedupedByTopic_retainUpgrades() {
		var m = map(ChartDef.line("t").src("/s").x("d").series("v", "V")
			.on("click", "app.pick").on("dblclick", "app.pick").publishZoom().publishZoom());
		assertEquals(j("[{topic:'app.pick',retain:false},{topic:'zoom:t',retain:true}]"), m.get("publishes"));
		var viaDecorator = map(ChartDef.line("t").src("/s").x("d").series("v", "V").on("click", "app.pick").decorate(Markers.values(List.of()).onClick("app.pick")));
		assertEquals(j("[{topic:'app.pick',retain:false}]"), viaDecorator.get("publishes"));
		var upgraded = map(ChartDef.line("t").src("/s").x("d").series("v", "V").on("click", "zoom:t").publishZoom());
		assertEquals(j("[{topic:'zoom:t',retain:true}]"), upgraded.get("publishes"));
	}
}
