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
 * The {@code <@card type="run-view">} bridge card: a JSON5 {@code {contractVersion, runView}} body, validated
 * through {@code RunViewDef} at render time and emitted as a {@code cards[]} entry with a {@code runView} object.
 */
class CardDirective_RunView_Test extends TestBase {

	/** The contract entry for card {@code id}. */
	private static JsonMap card(String body, String id) {
		for (var o : assertPage(body).isValid().contract().getList("cards"))
			if (id.equals(((JsonMap)o).getString("id")))
				return (JsonMap)o;
		throw new AssertionError("no card '" + id + "' in " + body);
	}

	@Test void a01_fullCard() {
		var body = render("rv-card");
		assertPage(body).isValid().hasCard("run-42", "run-view");
		assertEquals(
			Json5.to("{id:'run-42',type:'run-view',title:'Run',runView:{eventsUrl:'/juneau-run-view/42/events',"
				+ "refreshMs:1000,poll:true,compact:false,title:'Build run',rawHref:'/runs/42/raw#L{line}'}}", Map.class),
			Json5.to(Json.of(card(body, "run-42")), Map.class));
		assertFalse(body.contains("<template data-card=\"run-42\">"), () -> body);
	}

	@Test void a02_minimalCard_hasNoTitleAndOnlyTheUrlAndDefaults() {
		var c = card(render("rv-card-minimal"), "run");
		assertEquals("run-view", c.getString("type"));
		assertNull(c.get("title"));
		assertEquals("/runs/1/events", ((JsonMap)c.get("runView")).getString("eventsUrl"));
	}

	@Test void a03_pollFalse_needsNoUrl() {
		var rv = (JsonMap)card(render("rv-card-nopoll"), "run").get("runView");
		assertNull(rv.get("eventsUrl"));
		assertEquals(false, rv.get("poll"));
	}

	@Test void a04_requiresIsStillAccepted() {
		assertPage(render("rv-card-requires")).isValid().hasCard("run", "run-view");
	}

	@ParameterizedTest
	@CsvSource(delimiter='|', value={
		"rv-card-noid|<@card type=\"run-view\"> requires id=.",
		"rv-card-src|<@card id='run'> type='run-view' takes its options as the body; src= and template= are not allowed.",
		"rv-card-template|<@card id='run'> type='run-view' takes its options as the body; src= and template= are not allowed.",
		"rv-card-empty|<@card id='run'> type='run-view' requires a JSON5 body { contractVersion: '1', runView: {...} }.",
		"rv-card-notjson|<@card id='run'> type='run-view' requires a JSON5 body { contractVersion: '1', runView: {...} }.",
		"rv-card-noversion|<@card id='run'> type='run-view' requires contractVersion: '1'; got 'null'.",
		"rv-card-badversion|<@card id='run'> type='run-view' requires contractVersion: '1'; got '2'.",
		"rv-card-extra|<@card id='run'> type='run-view' unknown key 'poll'; allowed: contractVersion, runView, subscribes, publishes.",
		"rv-card-norunview|<@card id='run'> type='run-view' requires a runView object.",
		"rv-card-badkey|RunViewDef 'run' unknown key 'colour'",
		"rv-card-nourl|eventsUrl is required unless poll is false",
		"rv-card-unsafe|eventsUrl must be a same-origin path",
		"rv-card-badraw|rawHref must contain exactly one {line}",
		"rv-card-idurl|{id}",
	})
	void a05_errors(String fixture, String message) {
		assertError(fixture, message);
	}
}
