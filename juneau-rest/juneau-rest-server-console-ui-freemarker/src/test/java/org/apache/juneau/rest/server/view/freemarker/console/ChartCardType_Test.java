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

	@Test void a01_type_andNoBusRolesOrOps() {
		var h = new ChartCardType();
		assertEquals("chart", h.type());
		assertEquals(Set.of(), h.acceptedRoles());
		assertEquals(Set.of(), h.acceptedOps());
	}

	@Test void a02_srcLine_passesBodyThrough() {
		assertEquals(
			Json5.to("{kind:'line',x:{field:'day'},series:[{field:'placed'}],refreshMs:60000}", Map.class),
			Json5.to(Json.of(frag("/stats/daily", "{kind:'line',x:{field:'day'},series:[{field:'placed'}],refreshMs:60000}")), Map.class));
	}

	@Test void a03_inlineDonut_noXOrSeriesNeeded() {
		assertEquals("donut", frag(null, "{kind:'donut',label:'s',value:'n',data:[{s:'a',n:1}]}").get("kind"));
	}

	@Test void a04_customKind_onlyCommonChecks() {
		assertEquals("heatmap", frag("/h", "{kind:'heatmap',cells:'x'}").get("kind"));
	}

	@Test void a05_visibleWhenNotEchoed() {
		assertFalse(frag("/a", "{kind:'bar',x:'k',series:['v'],visibleWhen:{field:'f',op:'present'}}").containsKey("visibleWhen"));
	}

	@ParameterizedTest
	@CsvSource(delimiter='|', value={
		"/a|<null>|<@card id='c'> type='chart' requires a JSON5 body with a 'kind'.",
		"/a|{x:'d'}|<@card id='c'> type='chart' requires a non-blank 'kind'.",
		"/a|{kind:' '}|<@card id='c'> type='chart' requires a non-blank 'kind'.",
		"<null>|{kind:'donut'}|<@card id='c'> type='chart' requires exactly one of src= or a data body.",
		"/a|{kind:'donut',data:[]}|<@card id='c'> type='chart' requires exactly one of src= or a data body.",
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
		"/a|{kind:'Line'}|<@card id='c'> type='chart' kind must match '^[a-z][a-z0-9-]{0,31}$'; got 'Line'.",
		"/a|{kind:'my kind'}|<@card id='c'> type='chart' kind must match '^[a-z][a-z0-9-]{0,31}$'; got 'my kind'.",
		"/a|{kind:'1x'}|<@card id='c'> type='chart' kind must match '^[a-z][a-z0-9-]{0,31}$'; got '1x'.",
		"/a|{kind:'-x'}|<@card id='c'> type='chart' kind must match '^[a-z][a-z0-9-]{0,31}$'; got '-x'.",
		"/a|{kind:'abbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb'}|<@card id='c'> type='chart' kind must match '^[a-z][a-z0-9-]{0,31}$'; got 'abbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb'.",
		"/a|{kind:'donut'}|<@card id='c'> type='chart' kind='donut' requires string 'label' and 'value' fields.",
		"/a|{kind:'donut',label:'s'}|<@card id='c'> type='chart' kind='donut' requires string 'label' and 'value' fields.",
		"/a|{kind:'donut',label:'s',value:' '}|<@card id='c'> type='chart' kind='donut' requires string 'label' and 'value' fields.",
		"/a|{kind:'donut',label:3,value:'n'}|<@card id='c'> type='chart' kind='donut' requires string 'label' and 'value' fields.",
		"/a|{kind:'bar',x:'k',series:['v',{label:'L'}]}|<@card id='c'> type='chart' kind='bar' series[1] requires a non-blank 'field'.",
		"/a|{kind:'line',x:'k',series:[' ']}|<@card id='c'> type='chart' kind='line' series[0] requires a non-blank 'field'.",
		"/a|{kind:'line',x:'k',series:[3]}|<@card id='c'> type='chart' kind='line' series[0] requires a non-blank 'field'.",
		"/a|{kind:'line',x:'k',series:[{field:''}]}|<@card id='c'> type='chart' kind='line' series[0] requires a non-blank 'field'.",
	}, nullValues="<null>")
	void a06_errors(String src, String body, String message) {
		assertEquals(message, fail(src, body));
	}

	@Test void a07_heightAndSameOriginHref_pass() {
		var f = frag("/a", "{kind:'bar',x:'k',series:['v'],height:120,onClick:{href:'/orders?k={k}'}}");
		assertEquals(120, ((Number)f.get("height")).intValue());
		assertEquals(Map.of("href", "/orders?k={k}"), f.get("onClick"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"heatmap", "my-kind-2", "a", "abcdefghijklmnopqrstuvwxyz012345"})
	void a08_kindPattern_passes(String kind) {
		assertEquals(kind, frag("/h", "{kind:'" + kind + "'}").get("kind"));
	}
}
