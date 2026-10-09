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
package org.apache.juneau.rest.server.console.test;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class PageContractAssert_Bus_Test extends TestBase {

	private static String page(String contractJson) {
		return "<html><body><script type=\"application/json\" id=\"juneau-page\">" + contractJson + "</script></body></html>";
	}

	private static final String WIRED = page("""
		{"contractVersion":"1","title":"t","nav":[],"activeNav":[],
		 "topics":[{"topic":"app.region-picked","retain":true,"publisher":"script"},
		           {"topic":"ops.jobs","retain":true,"publisher":"server"}],
		 "bridges":[{"id":"ops","transport":"sse","session":"/s","downstream":["ops.jobs"]}],
		 "cards":[{"id":"a","type":"html","template":"a","publishes":[{"topic":"ssc.focus","retain":true}]},
		          {"id":"b","type":"html","template":"b","subscribes":[{"topic":"ssc.focus","as":"refresh"},{"topic":"app.region-picked","as":"refresh"}]}]}
		""");

	@Test void a01_positive() {
		PageContractAssert.assertPage(WIRED)
			.publishes("a", "ssc.focus")
			.subscribes("b", "ssc.focus")
			.subscribes("b", "app.region-picked", "refresh")
			.declaresTopic("ops.jobs")
			.hasBridge("ops")
			.hasNoWiringErrors();
	}

	@Test void b01_negative_messages() {
		var a = PageContractAssert.assertPage(WIRED);
		assertEquals("card 'a' does not subscribe to 'ssc.focus'; subscribes: []",
			assertThrows(AssertionError.class, () -> a.subscribes("a", "ssc.focus")).getMessage());
		assertEquals("card 'b' subscribes to 'ssc.focus' as [refresh], not 'filter'",
			assertThrows(AssertionError.class, () -> a.subscribes("b", "ssc.focus", "filter")).getMessage());
		assertEquals("card 'b' does not publish 'ssc.focus'; publishes: []",
			assertThrows(AssertionError.class, () -> a.publishes("b", "ssc.focus")).getMessage());
		assertEquals("no topics entry 'app.nope'; topics: [app.region-picked, ops.jobs]",
			assertThrows(AssertionError.class, () -> a.declaresTopic("app.nope")).getMessage());
		assertEquals("no bridge 'nope'; bridges: [ops]",
			assertThrows(AssertionError.class, () -> a.hasBridge("nope")).getMessage());
	}

	@Test void b02_wiringErrors_listEveryProblem() {
		var broken = page("""
			{"contractVersion":"1","title":"t","nav":[],"activeNav":[],
			 "cards":[{"id":"b","type":"html","template":"b","subscribes":[{"topic":"ssc.focus","as":"refresh"}]}]}
			""");
		var e = assertThrows(AssertionError.class, () -> PageContractAssert.assertPage(broken).hasNoWiringErrors());
		assertTrue(e.getMessage().startsWith("page wiring has errors:\nE-41 card 'b' subscribes to 'ssc.focus' but nothing publishes it"), e.getMessage());
	}

	@Test void a02_twoRolesOnOneTopic() {
		var two = page("""
			{"contractVersion":"1","title":"t","nav":[],"activeNav":[],
			 "cards":[{"id":"a","type":"html","template":"a","publishes":[{"topic":"ssc.focus","retain":true}]},
			          {"id":"b","type":"html","template":"b","subscribes":[{"topic":"ssc.focus","as":"refresh"},{"topic":"ssc.focus","as":"filter"}]}]}
			""");
		PageContractAssert.assertPage(two).subscribes("b", "ssc.focus", "filter").subscribes("b", "ssc.focus", "refresh");
	}
}
