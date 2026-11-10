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
package org.apache.juneau.console.views;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Cards nested inside a card ({@code renderNested}) and their page-bus wiring, driven through
 * {@code console-nested.cjs} against the real {@code juneau-bus.js} and {@code juneau-console.js}; gated on
 * {@code node} being on {@code PATH}.
 */
class ConsoleNested_Test extends TestBase {

	private static Map<String,Object> report;

	@BeforeAll
	static void runHarness() {
		report = BusHarness.run("console-nested.cjs", ViewsMixin.BUS_JS_RESOURCE, "/org/apache/juneau/console/juneau-console.js");
	}

	@SuppressWarnings({
		"unchecked" // Casts a parsed JSON value to its known Map shape.
	})
	private static Map<String,Object> scenario(String name) {
		assumeTrue(report != null, "node not on PATH");
		assertNull(report.get("harnessError"), () -> String.valueOf(report.get("harnessError")));
		var s = (Map<String,Object>) report.get(name);
		assertNotNull(s, name);
		return s;
	}

	@Test void n01_nestedCardIsARealCardAndDiesWithItsParent() {
		var s = scenario("n1");
		assertEquals(true, s.get("hasEl"));
		assertEquals(true, s.get("sameInst"));
		assertEquals(Map.of("schemaVersion", 1, "id", "inner", "type", "leaf", "state", "mounted"), s.get("lifecycle"));
		assertEquals(List.of("render:inner", "destroy:inner"), s.get("log"));
		assertEquals("E-JS-22", s.get("afterApi"));
		assertNull(s.get("afterTopic"));
		assertEquals(List.of(), s.get("calls"));
	}

	@Test void n02_nestedSwitcherIsE126_andLiveIdIsE127() {
		var s = scenario("n2");
		assertEquals(List.of(Map.of("id", "sw", "code", "E-JS-126"), Map.of("id", "top", "code", "E-JS-127"), Map.of("id", "dup", "code", "E-JS-127")), s.get("calls"));
		assertEquals(true, s.get("dupOk"));
	}

	@Test void n03_parentAbortReachesTheNestedSignal() {
		var s = scenario("n3");
		assertEquals(false, s.get("before"));
		assertEquals(true, s.get("after"));
	}

	@Test void n04_nestedCardPublishesSubscribesAndReleasesOnBus() {
		var s = scenario("n4");
		assertEquals(true, s.get("declared"));
		assertEquals(List.of(Map.of("n", 1)), s.get("seen"));
		assertEquals(List.of("sub:{\"n\":1}", "listener:{\"n\":1}"), s.get("got"));
		assertEquals("mounted", ((Map<?,?>)s.get("lifecycle")).get("state"));
		assertEquals("mounted", ((Map<?,?>)s.get("listenerLifecycle")).get("state"));
		assertNull(s.get("afterLifecycle"));
		assertEquals("E-JS-22", s.get("apiAfter"));
	}

	@Test void n05_nestedCardsReportedByTheTypeAreDeclaredBeforeTheyAreBuilt() {
		var s = scenario("n5");
		assertEquals(true, s.get("declared"));
		assertEquals("E-JS-22", s.get("noBuilt"));
	}
}
