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
package org.apache.juneau.console;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class ModelBlock_Test extends TestBase {

	private static HandlerRef h() {
		return HandlerRef.named("app.hello");
	}

	@Test void action_duplicateId_throws() {
		var b = new ModelBlock("card", "c1").action(ModelAction.of("go").handler("app.go"));
		var dup = ModelAction.of("go").handler("app.go");
		assertThrows(IllegalArgumentException.class, () -> b.action(dup));
	}

	@Test void action_noHandler_throws() {
		var block = new ModelBlock("card", "c1");
		var action = ModelAction.of("go");
		assertThrows(IllegalArgumentException.class, () -> block.action(action));
	}

	@Test void action_idGrammar_throws() {
		assertThrows(IllegalArgumentException.class, () -> ModelAction.of("1 bad"));
	}

	@Test void page_takesNoActionsOrSubscribes() {
		var b = new ModelBlock("page", "pg");
		var action = ModelAction.of("a").handler("app.x");
		var handler = h();
		assertThrows(IllegalArgumentException.class, () -> b.action(action));
		assertThrows(IllegalArgumentException.class, () -> b.subscribe("app.x", handler));
	}

	@Test void on_documentedEventsPerKind() {
		var table = Map.of(
			"page", List.of("loaded"), "card", List.of("loaded", "rendered"), "table", List.of("loaded", "rendered"),
			"row", List.of("rendered", "expanded", "collapsed"), "rowdetail", List.of("loaded", "rendered"),
			"detailtab", List.of("loaded", "rendered", "activated"), "ribbonitem", List.of("activated"),
			"pagetab", List.of("activated"), "view", List.of("activated"), "chart", List.of("loaded", "rendered"));
		var all = List.of("loaded", "rendered", "expanded", "collapsed", "activated");
		table.forEach((kind, ok) -> {
			for (var ev : all) {
				var b = new ModelBlock(kind, "e1");
				var handler = h();
				if (ok.contains(ev))
					assertDoesNotThrow(() -> b.on(ev, handler), kind + "/" + ev);
				else
					assertThrows(IllegalArgumentException.class, () -> b.on(ev, handler), kind + "/" + ev);
			}
		});
	}

	@Test void on_undocumentedEventForKind_throws() {
		var block = new ModelBlock("card", "c1");
		var handler = h();
		assertThrows(IllegalArgumentException.class, () -> block.on("bogusEvent", handler));
	}

	@Test void unknownKind_throws() {
		assertThrows(IllegalArgumentException.class, () -> new ModelBlock("nope", "x"));
	}

	@Test void subscribe_topicChecked() {
		var block = new ModelBlock("card", "c1");
		var handler = h();
		assertThrows(IllegalArgumentException.class, () -> block.subscribe("Bad Topic", handler));
		assertDoesNotThrow(() -> block.subscribe("selection:orders", handler));
	}

	@Test void subscribe_templatePlaceholders_perScope() {
		var handler = h();
		var card1 = new ModelBlock("card", "c1");
		var rowdetail = new ModelBlock("rowdetail", "d");
		var card2 = new ModelBlock("card", "c1");
		var row = new ModelBlock("row", "r");
		var detailtab = new ModelBlock("detailtab", "t");
		var card3 = new ModelBlock("card", "c1");
		assertThrows(IllegalArgumentException.class, () -> card1.subscribe("app.x:{rowKey}", handler));
		assertThrows(IllegalArgumentException.class, () -> rowdetail.subscribe("app.x:{tabId}", handler));
		assertThrows(IllegalArgumentException.class, () -> card2.subscribe("app.x:{bogus}", handler));
		assertDoesNotThrow(() -> row.subscribe("app.x:{rowKey}", handler));
		assertDoesNotThrow(() -> detailtab.subscribe("app.x:{rowKey}.{tabId}", handler));
		assertDoesNotThrow(() -> card3.subscribe("app.x:{cardId}", handler));
	}

	@Test void empty_toMap_isNull() {
		var b = new ModelBlock("card", "c1");
		assertTrue(b.isEmpty());
		assertNull(b.toMap());
	}

	@Test void toMap_shape() {
		var b = new ModelBlock("card", "c1")
			.action(ModelAction.of("go").icon("i").handler("app.go"))
			.on("rendered", h())
			.subscribe("selection:t", HandlerRef.inline("x"));
		var m = b.toMap();
		assertEquals(List.of("actions", "on", "subscribes"), new ArrayList<>(m.keySet()));
		assertEquals(Map.of("rendered", "app.hello"), m.get("on"));
		assertEquals(List.of(Map.of("topic", "selection:t", "handler", Map.of("inline", "x"))), m.get("subscribes"));
		assertTrue(b.hasInline());
	}

	@Test void messages_contain_element_id() {
		var block = new ModelBlock("card", "orders");
		var handler = h();
		var e = assertThrows(IllegalArgumentException.class, () -> block.on("bogus", handler));
		assertTrue(e.getMessage().contains("orders"), e.getMessage());
	}
}
