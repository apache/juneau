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

import static org.apache.juneau.rest.server.console.test.PageContractAssert.*;
import static org.apache.juneau.rest.server.view.freemarker.console.C1Fixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/** The {@code <@card type="chart">} card and its {@code charts} toolkit pack. */
class CardDirective_Chart_Test extends TestBase {

	private static JsonMap card(String body, String id) {
		for (var o : assertPage(body).isValid().contract().getList("cards"))
			if (id.equals(((JsonMap)o).getString("id")))
				return (JsonMap)o;
		throw new AssertionError("no card '" + id + "' in " + body);
	}

	@Test void a01_srcCard() {
		var body = render("ch-card");
		assertEquals(
			Json5.to("{id:'trend',type:'chart',title:'Orders',src:'/console/store/stats/daily',kind:'line',x:{field:'day'},"
				+ "series:[{field:'placed',label:'Placed'}],refreshMs:60000}", Map.class),
			Json5.to(Json.of(card(body, "trend")), Map.class));
	}

	@Test void a02_dataCard() {
		assertEquals("donut", card(render("ch-card-data"), "mix").getString("kind"));
	}

	@Test void a03_chartsScriptEmittedOnce() {
		var body = render("ch-card");
		var s = "/juneau-console/juneau-charts.js?v=";
		assertTrue(body.contains(s), () -> body);
		assertEquals(body.indexOf(s), body.lastIndexOf(s), () -> body);
	}

	@ParameterizedTest
	@CsvSource(delimiter='|', value={
		"ch-card-template|<@card id='trend'> type='chart' takes a JSON5 body (plus src= for remote data); template= is not allowed.",
		"ch-card-nokind|<@card id='trend'> type='chart' requires a non-blank 'kind'.",
		"ch-card-both|<@card id='trend'> type='chart' requires exactly one of src= or a data body.",
	})
	void a04_errors(String fixture, String message) {
		assertError(fixture, message);
	}
}
