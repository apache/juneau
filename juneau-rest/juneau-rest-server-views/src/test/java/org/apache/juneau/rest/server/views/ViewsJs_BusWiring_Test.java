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
package org.apache.juneau.rest.server.views;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * The JS half of R-10 / R-11 (message bus addendum, spec §6.1, §6.3, §11.2): {@code JuneauViews.bus.wiring.validate}
 * against the shared corpus {@code bus-wiring-corpus.json}, whose {@code expectJs} lists are the JS projection of
 * the Java expectations that console-ui-freemarker's {@code BusWiring_Corpus_Test} pins.  Gated on {@code node} being on
 * {@code PATH}.
 */
class ViewsJs_BusWiring_Test extends TestBase {

	private static Map<String,Object> report;

	@BeforeAll
	static void runHarness() {
		report = BusHarness.run("bus-wiring.cjs", ViewsMixin.BUS_JS_RESOURCE);
	}

	private static Map<String,Object> report() {
		assumeTrue(report != null, "node not on PATH");
		return report;
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String,Object>> cases() {
		return (List<Map<String,Object>>) report().get("cases");
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String,Object>> problems(Map<String,Object> c, String key) {
		return (List<Map<String,Object>>) c.get(key);
	}

	/** The corpus pins {code, message}; the paint target {@code card} is checked separately by a02. */
	private static List<Map<String,Object>> withoutCard(List<Map<String,Object>> got) {
		return got.stream().map(p -> {
			var m = new LinkedHashMap<String,Object>(p);
			m.remove("card");
			return (Map<String,Object>) m;
		}).toList();
	}

	@Test void a00_surface() {
		assertEquals(true, report().get("hasValidate"), "JuneauViews.bus.wiring.validate exists");
		assertEquals(List.of(), report().get("emptyContract"));
		assertEquals(27, cases().size(), "every corpus case ran");
	}

	@TestFactory Stream<DynamicTest> a01_corpus() {
		return cases().stream().map(c -> DynamicTest.dynamicTest((String) c.get("name"), () -> {
			assertNull(c.get("threw"), () -> c.get("name") + " threw: " + c.get("threw"));
			assertEquals(problems(c, "expectJs"), withoutCard(problems(c, "got")), () -> String.valueOf(c.get("name")));
		}));
	}

	@Test void a02_cardFieldNamesThePaintTarget() {
		for (var c : cases()) {
			for (var p : problems(c, "got")) {
				var message = (String) p.get("message");
				if ("E-JS-52".equals(p.get("code"))) {
					assertFalse(p.containsKey("card"), () -> c.get("name") + ": E-JS-52 is a page banner, not a card error");
				} else {
					var subject = message.substring("card '".length(), message.indexOf('\'', "card '".length()));
					assertEquals(subject, p.get("card"), () -> c.get("name") + ": " + message);
				}
			}
		}
	}

	@Test void a03_corpusExercisesEveryJsCode() {
		var codes = new TreeSet<String>();
		for (var c : cases())
			for (var p : problems(c, "expectJs"))
				codes.add((String) p.get("code"));
		assertEquals(new TreeSet<>(List.of("E-JS-41", "E-JS-47", "E-JS-52")), codes);
	}

	@Test void a04_validateLogsNothing() {
		assertEquals(List.of(), report().get("consoleErrors"), "problems are returned, never logged, by validate");
	}
}
