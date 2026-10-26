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
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

class ChartDef_Treemap_Test extends TestBase {

	private static Object j(String json5) { return Json5.to(json5, Object.class); }

	private static Map<?,?> map(ChartDef d) {
		return Json5.to(Json.of(d.toCardSpec().toMap()), Map.class);
	}

	private static String fail(Runnable r) {
		return assertThrows(IllegalArgumentException.class, r::run).getMessage();
	}

	private static ChartDef fleet() {
		return ChartDef.treemap("fleet").src("/api/hosts").groupBy("region", "cluster").label("host");
	}

	@Test void a01_treemap_emitsHelperUnderHelperKey() {
		var d = fleet()
			.colorBy("health", ChartDef.legendItem("ok", "Healthy", "ok"), ChartDef.legendItem("warn", "Degraded", "warning"),
				ChartDef.legendItem("down", "Down", "danger"))
			.tileFields("host", "health", "cpu");
		assertEquals(j("{id:'fleet',type:'chart',src:'/api/hosts',helper:{kind:'treemap',groupBy:['region','cluster'],label:'host',"
			+ "color:{field:'health',legend:[{value:'ok',label:'Healthy',tone:'ok'},{value:'warn',label:'Degraded',tone:'warning'},"
			+ "{value:'down',label:'Down',tone:'danger'}]},tileFields:['host','health','cpu']}}"), map(d));
	}

	@Test void a02_treemap_isAKind() {
		assertTrue(ChartDef.KINDS.contains("treemap"));
		assertFalse(ChartDef.LEGACY_KINDS.contains("treemap"));
	}

	@Test void a03_groupBy_emptyOrBlank_rejected() {
		assertEquals("ChartDef 'fleet' groupBy requires at least one non-blank field.", fail(() -> ChartDef.treemap("fleet").groupBy()));
		assertEquals("ChartDef 'fleet' groupBy requires at least one non-blank field.", fail(() -> ChartDef.treemap("fleet").groupBy("a", " ")));
	}

	@Test void a04_colorBy_unknownTone_rejected() {
		assertEquals("ChartDef legendItem tone 'success' must be 'ok', 'warning', 'danger', '--jc-chart-1'..'--jc-chart-8' or null.",
			fail(() -> ChartDef.legendItem("x", "X", "success")));
		assertEquals("ChartDef legendItem tone '--jc-chart-9' must be 'ok', 'warning', 'danger', '--jc-chart-1'..'--jc-chart-8' or null.",
			fail(() -> ChartDef.legendItem("x", "X", "--jc-chart-9")));
	}

	@Test void a05_legendItem_nullTone_isNeutral() {
		var m = ChartDef.legendItem("x", "X", null);
		assertEquals(Map.of("value", "x", "label", "X"), m);
		assertFalse(m.containsKey("tone"));
		assertEquals("--jc-chart-3", ChartDef.legendItem("x", "X", "--jc-chart-3").get("tone"));
		assertEquals("x", ChartDef.legendItem("x", null, null).get("label"));
		assertEquals("ChartDef legendItem requires a non-blank value.", fail(() -> ChartDef.legendItem(" ", "X", null)));
	}

	@Test void a06_size_optional_andNamed() {
		var none = (Map<?,?>)map(fleet()).get("helper");
		assertFalse(none.containsKey("size"));
		var sized = (Map<?,?>)map(fleet().size("cpu")).get("helper");
		assertEquals("cpu", sized.get("size"));
		assertEquals("ChartDef 'fleet' size requires a non-blank field.", fail(() -> fleet().size(" ")));
	}

	@Test void a07_label_required() {
		assertEquals("ChartDef 'fleet' kind 'treemap' requires label(field).",
			fail(() -> ChartDef.treemap("fleet").src("/s").groupBy("region").toCardSpec()));
		assertEquals("ChartDef 'fleet' kind 'treemap' requires groupBy(...).",
			fail(() -> ChartDef.treemap("fleet").src("/s").label("host").toCardSpec()));
	}

	@Test void a08_tileFields_invalidName_rejected() {
		assertEquals("ChartDef 'fleet' tileFields entry 'a b' must match [A-Za-z0-9_.-]+.", fail(() -> ChartDef.treemap("fleet").tileFields("a b")));
		assertEquals("ChartDef 'fleet' tileFields entry 'a{b}' must match [A-Za-z0-9_.-]+.", fail(() -> ChartDef.treemap("fleet").tileFields("ok", "a{b}")));
	}

	@Test void a09_set_treemapKeys_rejected() {
		for (var k : List.of("groupBy", "size", "color", "tileFields"))
			assertEquals("ChartDef 'fleet' key '" + k + "' has a dedicated method.", fail(() -> fleet().set(k, "x")));
	}

	@Test void a10_kinds_matchBothSchemaKindEnums() throws Exception {
		try (var in = ChartDef.class.getResourceAsStream("/org/apache/juneau/console/juneau-page.schema.json")) {
			assertNotNull(in, "page schema on the classpath");
			var schema = Json5.to(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8), Map.class);
			var chart = (Map<?,?>)((Map<?,?>)schema.get("$defs")).get("chartCard");
			var props = (Map<?,?>)chart.get("properties");
			var cardKinds = (List<?>)((Map<?,?>)props.get("kind")).get("enum");
			var helperProps = (Map<?,?>)((Map<?,?>)props.get("helper")).get("properties");
			var helperKinds = (List<?>)((Map<?,?>)helperProps.get("kind")).get("enum");
			assertEquals(new HashSet<>(ChartDef.KINDS), new HashSet<>(cardKinds));
			assertEquals(new HashSet<>(ChartDef.KINDS), new HashSet<>(helperKinds));
			for (var k : List.of("groupBy", "size", "color", "tileFields"))
				assertTrue(helperProps.containsKey(k), "schema helper property " + k);
		}
	}
}
