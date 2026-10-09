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

/**
 * The {@code <@card type="terminal">} card: a JSON5 {@code {contractVersion, terminal}} body, validated through
 * {@code TerminalDef} at render time and emitted as a {@code cards[]} entry with a {@code terminal} object, plus the
 * {@code xterm} and {@code terminal} toolkit packs (the terminal pack pulls {@code views} and {@code xterm}).
 */
class CardDirective_Terminal_Test extends TestBase {

	/** The contract entry for card {@code id}. */
	private static JsonMap card(String body, String id) {
		for (var o : assertPage(body).isValid().contract().getList("cards"))
			if (id.equals(((JsonMap)o).getString("id")))
				return (JsonMap)o;
		throw new AssertionError("no card '" + id + "' in " + body);
	}

	@Test void a01_fullCard_refreshClamped() {
		var body = render("tm-card");
		assertPage(body).isValid().hasCard("build", "terminal");
		assertEquals(
			Json5.to("{id:'build',type:'terminal',title:'Build',terminal:{bytesUrl:'/runs/juneau-terminal/r-7/bytes',"
				+ "eventsUrl:'/runs/juneau-run-view/r-7/events',refreshMs:1000,title:'mvn test',tailBytes:1048576}}", Map.class),
			Json5.to(Json.of(card(body, "build")), Map.class));
		assertFalse(body.contains("<template data-card=\"build\">"), () -> body);
	}

	@Test void a02_minimalCard() {
		assertEquals(
			Json5.to("{id:'log',type:'terminal',terminal:{bytesUrl:'/t/juneau-terminal/r1/bytes'}}", Map.class),
			Json5.to(Json.of(card(render("tm-card-minimal"), "log")), Map.class));
	}

	@Test void a03_pageWithNoToolkitLoadsViewsAndXtermOnceBeforeTheRuntime() {
		var body = render("tm-card-minimal");
		var xterm = body.indexOf("/lib/xterm.js?v=");
		var views = body.indexOf("/juneau-views.js?v=");
		var runtime = body.indexOf("/juneau-terminal.js?v=");
		assertTrue(xterm >= 0 && xterm < runtime, () -> body);
		assertTrue(views >= 0 && views < runtime, () -> body);
		for (var s : List.of("/lib/xterm.js?v=", "/css/xterm.css?v=", "/juneau-terminal.js?v=", "/juneau-terminal.css?v="))
			assertEquals(body.indexOf(s), body.lastIndexOf(s), () -> s + " emitted once: " + body);
		assertTrue(body.indexOf("/css/xterm.css?v=") >= 0, () -> body);
	}

	@ParameterizedTest
	@CsvSource(delimiter='|', value={
		"tm-card-noid|<@card type=\"terminal\"> requires id=.",
		"tm-card-src|<@card id='log'> type='terminal' takes its options as the body; src= and template= are not allowed.",
		"tm-card-empty|<@card id='log'> type='terminal' requires a JSON5 body { contractVersion: '1', terminal: {...} }.",
		"tm-card-badkey|TerminalDef 'log' unknown key 'colour'",
		"tm-card-unsafe|bytesUrl must be a same-origin path",
	})
	void a04_errors(String fixture, String message) {
		assertError(fixture, message);
	}
}
