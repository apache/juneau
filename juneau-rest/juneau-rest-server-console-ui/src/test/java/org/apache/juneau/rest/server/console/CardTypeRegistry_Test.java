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
package org.apache.juneau.rest.server.console;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;

class CardTypeRegistry_Test {

	@Test void standard_hasHtmlBuiltIn() {
		assertTrue(CardTypeRegistry.standard().isRegistered("html"));
		assertTrue(CardTypeRegistry.standard().types().contains("html"));
	}

	@Test void handler_unknownType_getsGenericPassthrough() {
		var r = CardTypeRegistry.standard();
		assertFalse(r.isRegistered("kpi"));
		assertNotNull(r.handler("kpi"));
	}

	@Test void genericHandler_jsonBody_passesThroughFlat() {
		var r = CardTypeRegistry.standard();
		var src = CardSource.create("kpi", "k").body("{value:3}").build();
		var card = r.toCard(src);
		assertEquals("k", card.get("id"));
		assertEquals("kpi", card.get("type"));
		assertEquals(3, card.get("value"));
	}

	@Test void genericHandler_markupBody_becomesTemplate() {
		var r = CardTypeRegistry.standard();
		var sink = new JsonMap();
		var src = CardSource.create("kpi", "k").body("<p>hi</p>")
			.templateSink((id, markup) -> sink.put(id, markup)).build();
		var card = r.toCard(src);
		assertEquals("k", card.get("template"));
		assertEquals("<p>hi</p>", sink.get("k"));
	}

	@Test void genericHandler_noBody_givesEmptyFragment() {
		var r = CardTypeRegistry.standard();
		var src = CardSource.create("kpi", "k").build();
		var card = r.toCard(src);
		assertEquals(Set.of("id", "type"), card.keySet());
	}

	@Test void add_duplicateType_throwsE20() {
		var b = CardTypeRegistry.standard().copy();
		b.add(new TestCardType("kpi"));
		var ex = assertThrows(IllegalArgumentException.class, () -> b.add(new TestCardType("kpi")));
		assertTrue(ex.getMessage().startsWith("Card type 'kpi' is already registered by '"), ex.getMessage());
	}

	@Test void add_badName_throwsE21() {
		var b = CardTypeRegistry.standard().copy();
		var ex = assertThrows(IllegalArgumentException.class, () -> b.add(new TestCardType("KPI")));
		assertEquals("Card type 'KPI' must match ^[a-z][a-z0-9-]{0,31}$.", ex.getMessage());
	}

	@Test void add_reservedType_throwsE22() {
		var b = CardTypeRegistry.standard().copy();
		var ex = assertThrows(IllegalArgumentException.class, () -> b.add(new TestCardType("datatables")));
		assertTrue(ex.getMessage().startsWith("Card type 'datatables' is reserved and cannot be replaced; reserved: '"),
			ex.getMessage());
	}

	@Test void toCard_reservedBodyKey_throwsE23() {
		var r = CardTypeRegistry.standard();
		var src = CardSource.create("kpi", "k").body("{id:'nope', value:3}").build();
		var ex = assertThrows(IllegalArgumentException.class, () -> r.toCard(src));
		assertEquals("<@card id='k'> body key 'id' is reserved; set it as an attribute.", ex.getMessage());
	}

	@Test void toCard_visibleWhenIsAdmittedInAnyCardBody() {
		var r = CardTypeRegistry.standard().copy().add(new TableLikeCardType()).build();
		var src = CardSource.create("tablelike", "t").body("{visibleWhen:{field:'x',op:'present'}, rows:[]}").build();
		var card = r.toCard(src);
		assertNotNull(card.get("visibleWhen"));
	}

	@Test void add_consoleOutputAndChartAreReserved_throwE22() {
		var b = CardTypeRegistry.standard().copy();
		assertThrows(IllegalArgumentException.class, () -> b.add(new TestCardType("console-output")));
		assertThrows(IllegalArgumentException.class, () -> b.add(new TestCardType("chart")));
	}

	@Test void add_terminalIsReserved_throwE22() {
		var b = CardTypeRegistry.standard().copy();
		assertThrows(IllegalArgumentException.class, () -> b.add(new TestCardType("terminal")));
	}

	@Test void copy_isIsolatedFromStandard() {
		var custom = CardTypeRegistry.standard().copy().add(new TestCardType("kpi")).build();
		assertTrue(custom.isRegistered("kpi"));
		assertFalse(CardTypeRegistry.standard().isRegistered("kpi"));
		// A second copy starts from standard() again, not from the first copy.
		assertFalse(CardTypeRegistry.standard().copy().build().isRegistered("kpi"));
	}

	@Test void toCard_titleAndSrc_becomeBaseKeys() {
		var r = CardTypeRegistry.standard();
		var src = CardSource.create("kpi", "k").title("Count").src("/rest/count").build();
		var card = r.toCard(src);
		assertEquals("Count", card.get("title"));
		assertEquals("/rest/count", card.get("src"));
	}

	/** Minimal handler whose fragment never carries `visibleWhen` itself — exercises toCard's promotion. */
	private static final class TableLikeCardType implements CardTypeHandler {
		@Override public String type() { return "tablelike"; }
		@Override public JsonMap toFragment(CardSource source) {
			var body = source.json();
			var table = new JsonMap();
			table.putAll(body);
			table.remove("visibleWhen");
			var frag = new JsonMap();
			frag.put("table", table);
			return frag;
		}
	}

	private static final class TestCardType implements CardTypeHandler {
		private final String type;
		TestCardType(String type) { this.type = type; }
		@Override public String type() { return type; }
		@Override public JsonMap toFragment(CardSource source) { return new JsonMap(); }
	}
}
