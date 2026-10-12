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
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.console.*;
import org.apache.juneau.rest.server.views.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

class ChartDef_Test extends TestBase {

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

	@Test void a04_customKind_set() {
		var m = map(ChartDef.of("heatmap", "h").src("/h").set("cells", "count"));
		assertEquals("heatmap", m.get("kind"));
		assertEquals("count", m.get("cells"));
	}

	@Test void a05_visibleWhen_carried() {
		var spec = ChartDef.line("t").src("/s").x("d").series("v", "V").visibleWhen(VisibilityRule.when("role").present()).toCardSpec();
		assertNotNull(spec.toMap().get("visibleWhen"));
	}

	@Test void a06_errors() {
		assertEquals("ChartDef 't' requires exactly one of src(...) or data(...).",
			assertThrows(IllegalArgumentException.class, () -> ChartDef.line("t").x("d").series("v", "V").toCardSpec()).getMessage());
		assertEquals("ChartDef 't' requires exactly one of src(...) or data(...).",
			assertThrows(IllegalArgumentException.class, () -> ChartDef.donut("t").src("/s").data(List.of()).toCardSpec()).getMessage());
		assertEquals("ChartDef 't' kind 'line' requires x(...) and at least one series(...).",
			assertThrows(IllegalArgumentException.class, () -> ChartDef.line("t").src("/s").x("d").toCardSpec()).getMessage());
		assertEquals("ChartDef 't' series tone '9' must be 1-8.",
			assertThrows(IllegalArgumentException.class, () -> ChartDef.line("t").series("v", "V", 9)).getMessage());
		assertEquals("ChartDef 't' series tone 'pink' must be 'danger', 'ok' or 'warning'.",
			assertThrows(IllegalArgumentException.class, () -> ChartDef.line("t").series("v", "V", "pink")).getMessage());
		assertEquals("ChartDef 't' x type 'date' must be 'category', 'number' or 'time'.",
			assertThrows(IllegalArgumentException.class, () -> ChartDef.line("t").x("d", "date", null)).getMessage());
		assertEquals("ChartDef 't' legend 'top' must be 'bottom', 'right' or 'none'.",
			assertThrows(IllegalArgumentException.class, () -> ChartDef.line("t").legend("top")).getMessage());
		assertEquals("ChartDef 't' height '100' is below the minimum '120'.",
			assertThrows(IllegalArgumentException.class, () -> ChartDef.line("t").height(100)).getMessage());
		assertEquals("ChartDef 't' refreshMs '1000' is below the minimum '5000'.",
			assertThrows(IllegalArgumentException.class, () -> ChartDef.line("t").refreshMs(1000)).getMessage());
		assertEquals("ChartDef 't' key 'series' has a dedicated method.",
			assertThrows(IllegalArgumentException.class, () -> ChartDef.line("t").set("series", List.of())).getMessage());
		assertEquals("ChartDef 't' key 'src' has a dedicated method.",
			assertThrows(IllegalArgumentException.class, () -> ChartDef.line("t").set("src", "/x")).getMessage());
		assertEquals("ChartDef requires a non-blank kind.",
			assertThrows(IllegalArgumentException.class, () -> ChartDef.of(" ", "t")).getMessage());
		assertEquals("ChartDef 't' onClick href must be a same-origin path; got 'https://evil.test/x'.",
			assertThrows(IllegalArgumentException.class, () -> ChartDef.line("t").onClickHref("https://evil.test/x")).getMessage());
		assertEquals("ChartDef 't' onClick href must be a same-origin path; got '//evil.test/x'.",
			assertThrows(IllegalArgumentException.class, () -> ChartDef.line("t").onClickHref("//evil.test/x")).getMessage());
		assertEquals("ChartDef 't' refreshMs(...) needs src(...); inline data never refreshes.",
			assertThrows(IllegalArgumentException.class, () -> ChartDef.donut("t").data(List.of()).refreshMs(6000).toCardSpec()).getMessage());
		assertEquals("ChartDef 't' data rows must not be null.",
			assertThrows(IllegalArgumentException.class, () -> ChartDef.donut("t").data(null)).getMessage());
		assertEquals("ChartDef 't' onClick href must be a same-origin path; got 'null'.",
			assertThrows(IllegalArgumentException.class, () -> ChartDef.line("t").onClickHref(null)).getMessage());
		assertEquals("ChartDef 't' kind 'donut' requires label(...) and value(...).",
			assertThrows(IllegalArgumentException.class, () -> ChartDef.donut("t").src("/s").label("s").toCardSpec()).getMessage());
		assertEquals("ChartDef 't' kind 'donut' requires label(...) and value(...).",
			assertThrows(IllegalArgumentException.class, () -> ChartDef.donut("t").src("/s").value("n").toCardSpec()).getMessage());
		assertEquals("ChartDef 't' kind 'donut' requires label(...) and value(...).",
			assertThrows(IllegalArgumentException.class, () -> ChartDef.donut("t").src("/s").label(" ").value("n").toCardSpec()).getMessage());
		assertEquals("ChartDef 't' series requires a non-blank field.",
			assertThrows(IllegalArgumentException.class, () -> ChartDef.line("t").series(null, "V")).getMessage());
		assertEquals("ChartDef 't' series requires a non-blank field.",
			assertThrows(IllegalArgumentException.class, () -> ChartDef.bar("t").series(" ", "V", 2)).getMessage());
		assertEquals("ChartDef 't' series requires a non-blank field.",
			assertThrows(IllegalArgumentException.class, () -> ChartDef.bar("t").series("", "V", "ok")).getMessage());
	}

	@ParameterizedTest
	@ValueSource(strings = {"Heat", "heat map", "1x", "-x", "x_y", "abcdefghijklmnopqrstuvwxyz0123456"})
	void a06a_kindPattern_rejected(String kind) {
		assertEquals("ChartDef 't' kind must match '^[a-z][a-z0-9-]{0,31}$'; got '" + kind + "'.",
			assertThrows(IllegalArgumentException.class, () -> ChartDef.of(kind, "t")).getMessage());
	}

	@ParameterizedTest
	@ValueSource(strings = {"heatmap", "my-kind-2", "abcdefghijklmnopqrstuvwxyz012345"})
	void a06d_kindPattern_passes(String kind) {
		assertEquals(kind, map(ChartDef.of(kind, "t").src("/s")).get("kind"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"publishes", "subscribes", "template", "bare", "class"})
	void a06b_cardLevelKeys_pointAtToCardSpec(String key) {
		assertEquals("ChartDef 't' key '" + key + "' is card-level; set it on toCardSpec().",
			assertThrows(IllegalArgumentException.class, () -> ChartDef.line("t").set(key, "x")).getMessage());
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
}
