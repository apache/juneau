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
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

class ChartCardType_Test extends TestBase {

	private static Map<String,Object> frag(String src, String body) {
		var b = CardSource.create("chart", "c");
		if (src != null)
			b.src(src);
		if (body != null)
			b.body(body);
		return new ChartCardType().toFragment(b.build());
	}

	private static String fail(String src, String body) {
		return assertThrows(IllegalArgumentException.class, () -> frag(src, body)).getMessage();
	}

	@Test void a01_type_andDataRole() {
		var h = new ChartCardType();
		assertEquals("chart", h.type());
		assertEquals(Set.of("data"), h.acceptedRoles());
	}

	@Test void a02_srcLine_passesBodyThrough() {
		assertEquals(
			Json5.to("{kind:'line',x:{field:'day'},series:[{field:'placed'}],refreshMs:60000}", Map.class),
			Json5.to(Json.of(frag("/stats/daily", "{kind:'line',x:{field:'day'},series:[{field:'placed'}],refreshMs:60000}")), Map.class));
	}

	@Test void a03_inlineDonut_noXOrSeriesNeeded() {
		assertEquals("donut", frag(null, "{kind:'donut',label:'s',value:'n',data:[{s:'a',n:1}]}").get("kind"));
	}

	@Test void a04_heatmapKind_checksItsKeys() {
		assertEquals("heatmap", frag("/h", "{kind:'heatmap',x:{field:'a'},y:{field:'b'},value:'c'}").get("kind"));
	}

	@Test void a05_visibleWhenNotEchoed() {
		assertFalse(frag("/a", "{kind:'bar',x:'k',series:['v'],visibleWhen:{field:'f',op:'present'}}").containsKey("visibleWhen"));
	}

	@ParameterizedTest
	@CsvSource(delimiter='|', value={
		"/a|<null>|<@card id='c'> type='chart' requires a JSON5 body.",
		"/a|{x:'d'}|<@card id='c'> type='chart' requires 'helper', a legacy 'kind' or an 'option'.",
		"/a|{kind:' '}|<@card id='c'> type='chart' requires a non-blank 'kind'.",
		"<null>|{kind:'donut'}|<@card id='c'> type='chart' requires exactly one of src=, a data body or a source.",
		"/a|{kind:'donut',data:[]}|<@card id='c'> type='chart' requires exactly one of src=, a data body or a source.",
		"<null>|{kind:'donut',data:{}}|<@card id='c'> type='chart' data must be an array of rows.",
		"/a|{kind:'line',series:['v']}|<@card id='c'> type='chart' kind='line' requires x and at least one series.",
		"/a|{kind:'bar',x:'k',series:[]}|<@card id='c'> type='chart' kind='bar' requires x and at least one series.",
		"/a|{kind:'bar',x:'k',series:['v'],refreshMs:'soon'}|<@card id='c'> type='chart' refreshMs must be an integer; got 'soon'.",
		"/a|{kind:'bar',x:'k',series:['v'],refreshMs:1000}|<@card id='c'> type='chart' refreshMs='1000' is below the minimum '5000'.",
		"/a|{kind:'bar',x:'k',series:['v'],refreshMs:6000.5}|<@card id='c'> type='chart' refreshMs must be an integer; got '6000.5'.",
		"<null>|{kind:'donut',label:'s',value:'n',data:[],refreshMs:1000}|<@card id='c'> type='chart' refreshMs needs src=; inline data never refreshes.",
		"<null>|{kind:'donut',label:'s',value:'n',data:[],refreshMs:6000}|<@card id='c'> type='chart' refreshMs needs src=; inline data never refreshes.",
		"/a|{kind:'bar',x:'k',series:['v'],height:'tall'}|<@card id='c'> type='chart' height must be an integer; got 'tall'.",
		"/a|{kind:'bar',x:'k',series:['v'],height:100}|<@card id='c'> type='chart' height='100' is below the minimum '120'.",
		"/a|{kind:'bar',x:'k',series:['v'],onClick:'/x'}|<@card id='c'> type='chart' onClick must be an object with a string href.",
		"/a|{kind:'bar',x:'k',series:['v'],onClick:{href:3}}|<@card id='c'> type='chart' onClick must be an object with a string href.",
		"/a|{kind:'bar',x:'k',series:['v'],onClick:{href:'https://evil.test/x'}}|<@card id='c'> type='chart' onClick.href must be a same-origin path; got 'https://evil.test/x'.",
		"/a|{kind:'bar',x:'k',series:['v'],onClick:{href:'//evil.test/{k}'}}|<@card id='c'> type='chart' onClick.href must be a same-origin path; got '//evil.test/{k}'.",
		"/a|{kind:'bar',x:'k',series:['v'],onClick:{href:'javascript:alert(1)'}}|<@card id='c'> type='chart' onClick.href must be a same-origin path; got 'javascript:alert(1)'.",
		"/a|{kind:'Line'}|<@card id='c'> type='chart' kind 'Line' must be one of 'bar, donut, heatmap, line, pie, scatter, treemap'.",
		"/a|{kind:'my kind'}|<@card id='c'> type='chart' kind 'my kind' must be one of 'bar, donut, heatmap, line, pie, scatter, treemap'.",
		"/a|{kind:'1x'}|<@card id='c'> type='chart' kind '1x' must be one of 'bar, donut, heatmap, line, pie, scatter, treemap'.",
		"/a|{kind:'-x'}|<@card id='c'> type='chart' kind '-x' must be one of 'bar, donut, heatmap, line, pie, scatter, treemap'.",
		"/a|{kind:'abbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb'}|<@card id='c'> type='chart' kind 'abbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb' must be one of 'bar, donut, heatmap, line, pie, scatter, treemap'.",
		"/a|{kind:'donut'}|<@card id='c'> type='chart' kind='donut' requires string 'label' and 'value' fields.",
		"/a|{kind:'donut',label:'s'}|<@card id='c'> type='chart' kind='donut' requires string 'label' and 'value' fields.",
		"/a|{kind:'donut',label:'s',value:' '}|<@card id='c'> type='chart' kind='donut' requires string 'label' and 'value' fields.",
		"/a|{kind:'donut',label:3,value:'n'}|<@card id='c'> type='chart' kind='donut' requires string 'label' and 'value' fields.",
		"/a|{kind:'bar',x:'k',series:['v',{label:'L'}]}|<@card id='c'> type='chart' kind='bar' series[1] requires a non-blank 'field'.",
		"/a|{kind:'line',x:'k',series:[' ']}|<@card id='c'> type='chart' kind='line' series[0] requires a non-blank 'field'.",
		"/a|{kind:'line',x:'k',series:[3]}|<@card id='c'> type='chart' kind='line' series[0] requires a non-blank 'field'.",
		"/a|{kind:'line',x:'k',series:[{field:''}]}|<@card id='c'> type='chart' kind='line' series[0] requires a non-blank 'field'.",
		"/a|{helper:'x'}|<@card id='c'> type='chart' helper must be a JSON object.",
		"/a|{kind:'heatmap',x:'a'}|<@card id='c'> type='chart' kind='heatmap' requires x, y and a string 'value' field.",
		"/a|{kind:'heatmap',x:'a',y:'b',value:3}|<@card id='c'> type='chart' kind='heatmap' requires x, y and a string 'value' field.",
		"/a|{kind:'pie',label:'s'}|<@card id='c'> type='chart' kind='pie' requires string 'label' and 'value' fields.",
		"/a|{kind:'scatter',x:'a',series:[]}|<@card id='c'> type='chart' kind='scatter' requires x and at least one series.",
		"<null>|{kind:'line',x:'d',series:['v'],source:'data:'}|<@card id='c'> type='chart' source must be a data: topic; got 'data:'.",
		"<null>|{kind:'line',x:'d',series:['v'],source:3}|<@card id='c'> type='chart' source must be a data: topic; got '3'.",
		"/a|{option:{},maps:{w:3}}|<@card id='c'> type='chart' maps.w must be a same-origin path; got '3'.",
		"/a|{option:{},maps:'x'}|<@card id='c'> type='chart' maps must be a JSON object.",
		"/a|{option:{},partialFrames:'yes'}|<@card id='c'> type='chart' partialFrames must be a boolean.",
		"/a|{option:{},group:3}|<@card id='c'> type='chart' group must be a string.",
		"/a|{option:{},theme:{}}|<@card id='c'> type='chart' theme must be a string.",
		"/a|{option:{},largeData:5}|<@card id='c'> type='chart' largeData must be a JSON object.",
		"/a|{option:{},a11y:'x'}|<@card id='c'> type='chart' a11y must be a JSON object.",
		"/a|{option:{},patch:{a:{'$fn':'Bad'}}}|<@card id='c'> E-74: 'Bad' is not a valid $fn name",
		"/a|{option:{},decorators:[{use:'ok',f:{'$fn':'Bad'}}]}|<@card id='c'> E-74: 'Bad' is not a valid $fn name",
		"/a|{option:{a:[{'$fn':'Bad'}]}}|<@card id='c'> E-74: 'Bad' is not a valid $fn name",
		"/a|{option:{a:{'$fn':3}}}|<@card id='c'> E-74: 'null' is not a valid $fn name",
	}, nullValues="<null>")
	void a06_errors(String src, String body, String message) {
		assertString(message, fail(src, body));
	}

	@Test void a07_heightAndSameOriginHref_pass() {
		var f = frag("/a", "{kind:'bar',x:'k',series:['v'],height:120,onClick:{href:'/orders?k={k}'}}");
		assertEquals(120, ((Number)f.get("height")).intValue());
		assertEquals(Map.of("href", "/orders?k={k}"), f.get("onClick"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"line", "bar", "donut", "heatmap", "scatter", "pie"})
	void a08_fixedKinds_pass(String kind) {
		var body = switch (kind) {
			case "donut", "pie" -> "{kind:'" + kind + "',label:'s',value:'n'}";
			case "heatmap" -> "{kind:'heatmap',x:'a',y:'b',value:'c'}";
			default -> "{kind:'" + kind + "',x:'a',series:['v']}";
		};
		assertEquals(kind, frag("/h", body).get("kind"));
	}

	@Test void b01_threeWayDataRule() {
		assertString("<@card id='c'> type='chart' requires exactly one of src=, a data body or a source.", fail(null, "{kind:'line',x:'d',series:['v'],data:[],source:'data:x'}"));
		assertNotNull(frag(null, "{kind:'line',x:'d',series:['v'],source:'data:x'}"));
		assertString("<@card id='c'> type='chart' source must be a data: topic; got 'filter:x'.", fail(null, "{kind:'line',x:'d',series:['v'],source:'filter:x'}"));
	}

	@Test void b02_helperXorKind_orOption() {
		assertNotNull(frag("/s", "{option:{series:[]}}"));
		assertNotNull(frag("/s", "{helper:{kind:'heatmap',x:{field:'a'},y:{field:'b'},value:'c'}}"));
		assertString("<@card id='c'> type='chart' takes 'helper' or a legacy 'kind', not both.", fail("/s", "{kind:'line',helper:{kind:'line'}}"));
		assertString("<@card id='c'> type='chart' requires 'helper', a legacy 'kind' or an 'option'.", fail("/s", "{}"));
	}

	@Test void b03_unknownKind_rejected() {
		assertString("<@card id='c'> type='chart' kind 'sparkline' must be one of 'bar, donut, heatmap, line, pie, scatter, treemap'.", fail("/s", "{kind:'sparkline'}"));
		assertString("<@card id='c'> type='chart' kind 'sparkline' must be one of 'bar, donut, heatmap, line, pie, scatter, treemap'.", fail("/s", "{helper:{kind:'sparkline'}}"));
	}

	@Test void b04_optionPatchObjects() {
		assertString("<@card id='c'> type='chart' option must be a JSON object.", fail("/s", "{option:[1]}"));
		assertString("<@card id='c'> type='chart' patch must be a JSON object.", fail("/s", "{kind:'line',x:'d',series:['v'],patch:'x'}"));
	}

	@Test void b05_decorators() {
		assertString("<@card id='c'> type='chart' decorators must be an array of objects with a string 'use'.", fail("/s", "{option:{},decorators:[{}]}"));
		assertString("<@card id='c'> E-74: 'Bad' is not a valid decorator name", fail("/s", "{option:{},decorators:[{use:'Bad'}]}"));
	}

	@Test void b06_on() {
		assertString("<@card id='c'> type='chart' on must map event names to topic strings.", fail("/s", "{option:{},on:{click:1}}"));
		assertString("<@card id='c'> E-74: 'Click' is not a valid event name", fail("/s", "{option:{},on:{Click:'app.x'}}"));
	}

	@Test void b07_renderer_maps_src() {
		assertString("<@card id='c'> type='chart' renderer must be 'canvas' or 'svg'; got 'webgl'.", fail("/s", "{option:{},renderer:'webgl'}"));
		assertString("<@card id='c'> type='chart' maps.world must be a same-origin path; got 'https://x/w'.", fail("/s", "{option:{},maps:{world:'https://x/w'}}"));
		assertString("<@card id='c'> type='chart' src must be a same-origin path; got 'https://evil/x'.", fail("https://evil/x", "{option:{}}"));
	}

	@Test void b08_acceptedOps_eight() {
		assertList(new TreeSet<>(new ChartCardType().acceptedOps()), "downplay", "export", "hideTip", "highlight", "legendSelect", "refresh", "showTip", "zoomTo");
		assertList(new ChartCardType().acceptedRoles(), "data");
	}

	@Test void b09_legacyBody_unchanged() {
		var body = "{kind:'line',x:{field:'day'},series:[{field:'placed',label:'Placed'}],refreshMs:60000}";
		assertEquals(Json5.to(body, Map.class), Json5.to(Json.of(frag("/s", body)), Map.class));
	}

	@Test void b10_legacyChecks_stillApply_underHelper() {
		assertString("<@card id='c'> type='chart' kind='line' requires x and at least one series.", fail("/s", "{helper:{kind:'line'}}"));
		assertString("<@card id='c'> type='chart' kind='donut' requires string 'label' and 'value' fields.", fail("/s", "{helper:{kind:'donut'}}"));
	}

	@Test void b11_partialFrames_needsSource() {
		assertString("<@card id='c'> type='chart' partialFrames needs a source.", fail("/s", "{option:{},partialFrames:true}"));
	}

	@Test void b12_fnNamesChecked() {
		assertString("<@card id='c'> E-74: 'Bad' is not a valid $fn name", fail("/s", "{option:{tooltip:{formatter:{'$fn':'Bad'}}}}"));
	}

	private static List<String> implicit(String card) {
		return new ChartCardType().implicitTopics(Json5.to(card, JsonMap.class));
	}

	@Test void b13a_implicitTopics_noPublishes() {
		assertEquals(List.of(), implicit("{id:'c'}"));
	}

	@Test void b13b_implicitTopics_zoomRetained() {
		assertEquals(List.of("zoom:c"), implicit("{id:'c',publishes:[{topic:'zoom:c',retain:true}]}"));
	}

	@Test void b13c_implicitTopics_zoomAndLegend() {
		assertEquals(List.of("zoom:c", "legend:c"), implicit("{id:'c',publishes:[{topic:'legend:c',retain:true},{topic:'zoom:c',retain:true}]}"));
	}

	@Test void b13d_implicitTopics_notRetained() {
		assertEquals(List.of(), implicit("{id:'c',publishes:[{topic:'zoom:c',retain:false},{topic:'legend:c'}]}"));
	}

	@Test void b13e_implicitTopics_anotherCardsTopic() {
		assertEquals(List.of(), implicit("{id:'c',publishes:[{topic:'zoom:other',retain:true}]}"));
	}

	@Test void b13f_implicitTopics_nonStringId() {
		assertEquals(List.of(), implicit("{id:3,publishes:[{topic:'zoom:3',retain:true}]}"));
		assertEquals(List.of(), implicit("{publishes:[{topic:'zoom:null',retain:true}]}"));
	}

	@Test void b14a_fnWalk_skipsDatasetSource() {
		assertNotNull(frag("/s", "{option:{dataset:[{source:[{'$fn':'Bad'}]}]}}"));
	}

	@Test void b14b_fnWalk_skipsSeriesData() {
		assertNotNull(frag("/s", "{option:{series:[{type:'line',data:[{'$fn':'Bad'}]}]}}"));
	}

	@Test void b14c_fnWalk_markerWithOtherKeys_isNotAMarker() {
		assertNotNull(frag("/s", "{option:{a:{'$fn':'Bad',other:1}}}"));
	}

	@Test void b14d_fnWalk_otherDatasetKeysStillWalked() {
		assertString("<@card id='c'> E-74: 'Bad' is not a valid $fn name", fail("/s", "{option:{dataset:[{dimensions:[{'$fn':'Bad'}]}]}}"));
	}

	@Test void b14e_fnWalk_otherSeriesKeysStillWalked() {
		assertString("<@card id='c'> E-74: 'Bad' is not a valid $fn name", fail("/s", "{option:{series:[{label:{'$fn':'Bad'}}]}}"));
	}

	@Test void b14f_fnWalk_helperIsWalked() {
		// helper values can be $fn markers (ChartDef.symbolSizeBy), so a bad name there is E-74.
		assertString("<@card id='c'> E-74: 'Bad' is not a valid $fn name",
			fail("/s", "{helper:{kind:'line',x:{field:'d'},series:['v'],symbolSize:{'$fn':'Bad'}}}"));
		assertNotNull(frag("/s", "{helper:{kind:'line',x:{field:'d'},series:['v'],symbolSize:{'$fn':'ok'}}}"));
	}

	@Test void b14g_fnWalk_legacyFlatHelperKeysWalked_otherKeysNot() {
		assertString("<@card id='c'> E-74: 'Bad' is not a valid $fn name",
			fail("/s", "{kind:'line',x:{field:'d'},series:['v'],symbolSize:{'$fn':'Bad'}}"));
		// data rows are not helper keys, so a marker-shaped row value in a legacy flat body is not walked
		assertNotNull(frag(null, "{kind:'donut',label:'s',value:'n',data:[{s:{'$fn':'Bad'},n:1}]}"));
	}

	private static final String TM = "{helper:{kind:'treemap',groupBy:['region','cluster'],label:'host',size:'cpu',"
		+ "color:{field:'health',legend:[{value:'ok',label:'Healthy',tone:'ok'},{value:'down',label:'Down',tone:'danger'}]},"
		+ "tileFields:['host','health','cpu']}}";

	private static String tm(String key, String value) {
		return TM.replace(key, value);
	}

	@Test void t01_treemap_valid_passes() {
		var f = frag("/api/hosts", TM);
		assertEquals("treemap", ((Map<?,?>)f.get("helper")).get("kind"));
		assertNotNull(frag(null, "{helper:{kind:'treemap',groupBy:['a'],label:'b'},data:[{a:'x',b:'y'}]}"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"[]", "'region'", "[1]", "[' ']"})
	void t02_treemap_groupByNotNonEmptyStringArray_rejected(String bad) {
		assertString("<@card id='c'> type='chart' helper kind 'treemap' requires 'groupBy' as a non-empty array of non-blank strings.",
			fail("/s", "{helper:{kind:'treemap',groupBy:" + bad + ",label:'host'}}"));
	}

	@Test void t03_treemap_labelMissing_rejected() {
		assertString("<@card id='c'> type='chart' helper kind 'treemap' requires a non-blank string 'label'.",
			fail("/s", "{helper:{kind:'treemap',groupBy:['a']}}"));
		assertString("<@card id='c'> type='chart' helper kind 'treemap' requires a non-blank string 'label'.",
			fail("/s", "{helper:{kind:'treemap',groupBy:['a'],label:' '}}"));
	}

	@Test void t04_treemap_legendTone_unknown_rejected() {
		assertString("<@card id='c'> type='chart' helper kind 'treemap' color.legend[1].tone 'success' must be 'ok', 'warning', 'danger', '--jc-chart-1'..'--jc-chart-8' or absent.",
			fail("/s", tm("tone:'danger'", "tone:'success'")));
		assertString("<@card id='c'> type='chart' helper kind 'treemap' color.legend[0].tone '--jc-chart-9' must be 'ok', 'warning', 'danger', '--jc-chart-1'..'--jc-chart-8' or absent.",
			fail("/s", tm("tone:'ok'", "tone:'--jc-chart-9'")));
	}

	@Test void t05_treemap_legendTone_paletteSlotAndAbsent_pass() {
		assertNotNull(frag("/s", tm("tone:'ok'", "tone:'--jc-chart-3'")));
		assertNotNull(frag("/s", tm(",tone:'ok'", "")));
	}

	@ParameterizedTest
	@ValueSource(strings = {"['a b']", "[1]", "'host'", "['a{b}']"})
	void t06_treemap_tileFields_badEntry_rejected(String bad) {
		assertString("<@card id='c'> type='chart' helper kind 'treemap' tileFields must be an array of strings matching [A-Za-z0-9_.-]+.",
			fail("/s", "{helper:{kind:'treemap',groupBy:['a'],label:'b',tileFields:" + bad + "}}"));
	}

	@Test void t07_treemap_sizeNotString_rejected() {
		assertString("<@card id='c'> type='chart' helper kind 'treemap' size must be a non-blank string.",
			fail("/s", tm("size:'cpu'", "size:3")));
	}

	@Test void t08_treemap_colorWithoutField_rejected() {
		assertString("<@card id='c'> type='chart' helper kind 'treemap' color must be an object with a non-blank string 'field'.",
			fail("/s", tm("field:'health'", "x:1")));
		assertString("<@card id='c'> type='chart' helper kind 'treemap' color must be an object with a non-blank string 'field'.",
			fail("/s", "{helper:{kind:'treemap',groupBy:['a'],label:'b',color:'health'}}"));
		assertString("<@card id='c'> type='chart' helper kind 'treemap' color.legend must be an array of objects with a string 'value'.",
			fail("/s", "{helper:{kind:'treemap',groupBy:['a'],label:'b',color:{field:'h',legend:[3]}}}"));
	}

	private static final String P = "<@card id='c'> type='chart' ";
	private static final String BASE = "kind:'bar',x:'k',series:['v']";
	private static final String OK = "{id:'requeue',endpoint:'/api/requeue',method:'POST'}";

	private static String click(String action) { return "{" + BASE + ",on:{click:'app.pick'},onClick:{action:" + action + "}}"; }

	private static String withActions(String actions) { return "{" + BASE + ",actions:" + actions + "}"; }

	@Test void u01_onClickHref_stillPasses() {
		assertEquals(Map.of("href", "/o?k={k}"), frag("/a", "{" + BASE + ",onClick:{href:'/o?k={k}'}}").get("onClick"));
	}

	@Test void u02_onClickAction_valid_passes() {
		var f = frag("/a", click("{id:'requeue',endpoint:'/api/requeue/{k}',method:'DELETE',confirm:'Sure?',present:'dialog',form:'/forms/requeue',onSuccess:'redraw'}"));
		assertEquals("requeue", ((Map<?,?>)((Map<?,?>)f.get("onClick")).get("action")).get("id"));
		assertNotNull(frag("/a", click("{id:'x',endpoint:'/api/x',method:'PUT',confirm:false}")));
	}

	@Test void u03_hrefAndAction_rejected() {
		assertEquals(P + "onClick takes either href or action, not both.",
			fail("/a", "{" + BASE + ",on:{click:'app.pick'},onClick:{href:'/x',action:" + OK + "}}"));
	}

	@Test void u04_getMethod_rejected() {
		for (var m : List.of("GET", "HEAD", "get", "''"))
			assertEquals(P + "onClick.action method must be POST, PUT, PATCH or DELETE; got '" + m.replace("'", "") + "'.",
				fail("/a", click("{id:'x',endpoint:'/api/x',method:'" + m.replace("'", "") + "'}")));
		assertEquals(P + "onClick.action method must be POST, PUT, PATCH or DELETE; got 'null'.", fail("/a", click("{id:'x',endpoint:'/api/x'}")));
	}

	@Test void u05_servletEndpoint_rejected() {
		assertEquals(P + "onClick.action endpoint must be a same-origin path; got 'servlet:/x'.",
			fail("/a", click("{id:'x',endpoint:'servlet:/x',method:'POST'}")));
		assertEquals(P + "onClick.action endpoint must be a same-origin path; got '/a/../b'.",
			fail("/a", click("{id:'x',endpoint:'/a/../b',method:'POST'}")));
	}

	@Test void u06_nonSameOriginEndpoint_rejected() {
		assertEquals(P + "onClick.action endpoint must be a same-origin path; got 'https://evil.test/x'.",
			fail("/a", click("{id:'x',endpoint:'https://evil.test/x',method:'POST'}")));
		assertEquals(P + "onClick.action endpoint must be a same-origin path; got '//evil.test/x'.",
			fail("/a", click("{id:'x',endpoint:'//evil.test/x',method:'POST'}")));
	}

	@Test void u07_nonSameOriginForm_rejected() {
		assertEquals(P + "onClick.action form must be a same-origin path; got 'https://evil.test/f'.",
			fail("/a", click("{id:'x',endpoint:'/api/x',method:'POST',present:'dialog',form:'https://evil.test/f'}")));
		assertEquals(P + "onClick.action form must be a same-origin path; got 'servlet:/f'.",
			fail("/a", click("{id:'x',endpoint:'/api/x',method:'POST',present:'dialog',form:'servlet:/f'}")));
	}

	@Test void u08_confirmTrue_rejected() {
		assertEquals(P + "onClick.action confirm must be a string or false.",
			fail("/a", click("{id:'x',endpoint:'/api/x',method:'POST',confirm:true}")));
		assertEquals(P + "onClick.action confirm must be a string or false.",
			fail("/a", click("{id:'x',endpoint:'/api/x',method:'POST',confirm:3}")));
	}

	@Test void u09_presentInlineAndPage_rejected() {
		for (var v : List.of("inline", "page"))
			assertEquals(P + "onClick.action present must be 'dialog' or absent; got '" + v + "'.",
				fail("/a", click("{id:'x',endpoint:'/api/x',method:'POST',present:'" + v + "'}")));
	}

	@Test void u10_onSuccessNavigate_rejected() {
		assertEquals(P + "onClick.action onSuccess must not be 'navigate'; a chart has nowhere to navigate.",
			fail("/a", click("{id:'x',endpoint:'/api/x',method:'POST',onSuccess:'navigate'}")));
	}

	@Test void u11_actionWithoutDeclaredOnClick_rejected() {
		assertEquals(P + "onClick.action needs on.click to declare the click topic.",
			fail("/a", "{" + BASE + ",onClick:{action:" + OK + "}}"));
		assertEquals(P + "onClick.action needs on.click to declare the click topic.",
			fail("/a", "{" + BASE + ",on:{dblclick:'app.pick'},onClick:{action:" + OK + "}}"));
	}

	@Test void u12_topicActionTopicNotInOn_rejected() {
		assertEquals(P + "topicActions[0].topic 'app.other' must be one this card publishes through 'on'.",
			fail("/a", "{" + BASE + ",on:{click:'app.pick'},topicActions:[{topic:'app.other',action:" + OK + "}]}"));
		assertNotNull(frag("/a", "{" + BASE + ",on:{click:'app.pick'},topicActions:[{topic:'app.pick',action:" + OK + "}]}"));
		assertEquals(P + "topicActions must be an array of objects with a string topic and an action object.",
			fail("/a", "{" + BASE + ",on:{click:'app.pick'},topicActions:{topic:'app.pick'}}"));
		assertEquals(P + "topicActions must be an array of objects with a string topic and an action object.",
			fail("/a", "{" + BASE + ",on:{click:'app.pick'},topicActions:[{topic:'app.pick'}]}"));
	}

	@Test void u13_unknownKeyUnderOnClick_rejected() {
		assertEquals(P + "onClick has unknown key 'topic'.", fail("/a", "{" + BASE + ",onClick:{href:'/x',topic:'app.pick'}}"));
		assertEquals(P + "onClick has unknown key 'topic'.", fail("/a", click("{id:'x',endpoint:'/api/x',method:'POST'}").replace("onClick:{", "onClick:{topic:'t',")));
	}

	@Test void u14_actionsNotArray_rejected() {
		assertEquals(P + "actions must be an array of action objects.", fail("/a", withActions("{id:'x'}")));
		assertEquals(P + "actions[0] must be an object.", fail("/a", withActions("['x']")));
	}

	@Test void u15_nonDialogCatalogEntry_rejected() {
		assertEquals(P + "actions[1] 'requeue' must be present=dialog; a dialog button can only open a dialog action.",
			fail("/a", withActions("[{id:'a',endpoint:'/api/a',method:'POST',present:'dialog'}," + OK + "]")));
		assertNotNull(frag("/a", withActions("[{id:'a',endpoint:'/api/a',method:'POST',present:'dialog'}]")));
	}

	@Test void u16_duplicateIdAcrossOnClickTopicActionsActions_rejected() {
		var dup = P + "action id 'requeue' is declared more than once.";
		var d = "{id:'requeue',endpoint:'/api/requeue',method:'POST',present:'dialog'}";
		assertEquals(dup, fail("/a", "{" + BASE + ",on:{click:'app.pick'},onClick:{action:" + OK + "},actions:[" + d + "]}"));
		assertEquals(dup, fail("/a", "{" + BASE + ",on:{click:'app.pick'},topicActions:[{topic:'app.pick',action:" + OK + "}],actions:[" + d + "]}"));
		assertEquals(dup, fail("/a", "{" + BASE + ",on:{click:'app.pick'},onClick:{action:" + OK + "},topicActions:[{topic:'app.pick',action:" + OK + "}]}"));
		assertEquals(dup, fail("/a", withActions("[" + d + "," + d + "]")));
	}

	@Test void u17_emptyActionsArray_passes() {
		assertEquals(List.of(), frag("/a", withActions("[]")).get("actions"));
	}

	@Test void u18_blankIdOrEndpoint_rejected() {
		assertEquals(P + "onClick.action requires a non-blank string id.", fail("/a", click("{id:' ',endpoint:'/api/x',method:'POST'}")));
		assertEquals(P + "onClick.action requires a non-blank string id.", fail("/a", click("{endpoint:'/api/x',method:'POST'}")));
		assertEquals(P + "onClick.action requires a non-blank string endpoint.", fail("/a", click("{id:'x',endpoint:'',method:'POST'}")));
		assertEquals(P + "actions[0] requires a non-blank string endpoint.", fail("/a", withActions("[{id:'x',method:'POST',present:'dialog'}]")));
	}

	@Test void u19_visibleWhenOrEnabledWhenNotArray_rejected() {
		assertEquals(P + "onClick.action visibleWhen must be an array of rules; it is tested against the clicked point.",
			fail("/a", click("{id:'x',endpoint:'/api/x',method:'POST',visibleWhen:{field:'status',op:'eq',value:'open'}}")));
		assertEquals(P + "onClick.action enabledWhen must be an array of rules.",
			fail("/a", click("{id:'x',endpoint:'/api/x',method:'POST',enabledWhen:{field:'status',op:'eq',value:'open'}}")));
		assertEquals(P + "actions[0] visibleWhen must be an array of rules; it is tested against the clicked point.",
			fail("/a", withActions("[{id:'x',endpoint:'/api/x',method:'POST',present:'dialog',visibleWhen:'nope'}]")));
		assertNotNull(frag("/a", click("{id:'x',endpoint:'/api/x',method:'POST',visibleWhen:[{field:'status',op:'eq',value:'open'}],enabledWhen:[]}")));
	}
}
