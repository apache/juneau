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

import static org.apache.juneau.rest.server.views.ConsoleBusBrowserSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * Fail loud (spec 6.3): a {@code publisher=script} topic that no page script declares paints E-JS-41 on the subscribing card (naming the topic)
 * and logs it; a page that wires topics without {@code juneau-bus.js} fails at mount with the page-level E-JS-46 banner.
 */
@EnabledIfSystemProperty(named=ConsoleBusBrowserSupport.GATE, matches="true", disabledReason=ConsoleBusBrowserSupport.DISABLED)
class ConsoleBusLoudFailure_BrowserTest extends TestBase {

	private static Map<String,Object> undeclared;
	private static Map<String,Object> noBus;

	@BeforeAll
	static void probe() throws Exception {
		undeclared = run("loud-failure", "loud-failure.json", true, "{}", "");
		noBus = run("missing-bus", "master-detail.json", false, "{}", "");
	}

	@Test void a01_undeclaredScriptTopicPaintsEJS41OnTheSubscribingCard() {
		var e = map(undeclared.get("tasksError"));
		assertEquals("E-JS-41", e.get("code"));
		assertTrue(e.get("text").toString().contains("app.region-picked"), () -> "the error must name the topic: " + e);
		assertTrue(e.get("text").toString().contains("'tasks'"), () -> "the error must name the card: " + e);
	}

	@Test void a02_onlyTheSubscribingCardIsMarked() {
		assertNull(undeclared.get("changesError"));
		assertNull(undeclared.get("pageError"));
	}

	@Test void a03_theErrorIsLoggedThroughConsoleError() {
		assertTrue(list(undeclared.get("consoleErrors")).stream().anyMatch(l -> l.toString().startsWith("[juneau-console] ") && l.toString().contains("app.region-picked")),
			() -> undeclared.toString());
	}

	@Test void b01_missingBusIsPageLevelEJS46() {
		var p = map(noBus.get("pageError"));
		assertEquals("alert", p.get("role"));
		assertTrue(list(p.get("codes")).contains("E-JS-46"), () -> noBus.toString());
		assertEquals(1, list(p.get("codes")).stream().filter("E-JS-46"::equals).count(), () -> "the banner must show E-JS-46 once, not once per card: " + noBus);
		assertEquals(Boolean.FALSE, noBus.get("hasBus"));
		assertTrue(list(noBus.get("consoleErrors")).stream().anyMatch(l -> l.toString().contains("page wires topics but juneau-bus.js is not loaded")), () -> noBus.toString());
	}
}
