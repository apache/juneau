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
package org.apache.juneau.console.freemarker;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.console.*;
import org.junit.jupiter.api.*;

class SwitcherCardType_Test extends TestBase {

	private static final CardTypeRegistry REG = CardTypeRegistry.standard();

	private static CardSource src(String body) {
		return CardSource.create("switcher", "sw").body(body).build();
	}

	private static final String OK = "{views:[{id:'a',label:'A',card:{id:'c1',type:'chart',src:'/x',kind:'line',x:{field:'d'},series:['n']}},"
		+ "{id:'t',label:'T',sections:[{id:'s1',label:'S1',card:{id:'d1',type:'datatables',dataMode:'client',dataUrl:'/x',columns:[{key:'k',label:'K'}]}}]}]}";

	@Test void a01_type() {
		assertEquals("switcher", new SwitcherCardType().type());
	}

	@Test void a02_registeredAndReserved() {
		assertTrue(REG.isRegistered("switcher"));
		assertTrue(CardTypeRegistry.RESERVED.contains("switcher"));
	}

	@Test void a03_customHandlerCannotClaimSwitcher() {
		var b = REG.copy();
		var imposter = new CardTypeHandler() {
			@Override public String type() { return "switcher"; }
			@Override public org.apache.juneau.marshall.collections.JsonMap toFragment(CardSource s) { return new org.apache.juneau.marshall.collections.JsonMap(); }
		};
		var e = assertThrows(IllegalArgumentException.class, () -> b.add(imposter));
		assertTrue(e.getMessage().startsWith("Card type 'switcher' is reserved"), e.getMessage());
	}

	@Test void a04_toCard_keepsViews() {
		var card = REG.toCard(src(OK));
		assertEquals("switcher", card.get("type"));
		assertEquals(2, ((List<?>)card.get("views")).size());
	}

	@Test void a05_nestedCards_bothForms() {
		var card = REG.toCard(src(OK));
		var ids = new SwitcherCardType().nestedCards(card).stream().map(m -> m.get("id")).toList();
		assertEquals(List.of("c1", "d1"), ids);
	}

	@Test void a06_nestedDatatablesLiftedToTable() {
		var card = REG.toCard(src(OK));
		var d = new SwitcherCardType().nestedCards(card).get(1);
		assertEquals("datatables", d.get("type"));
		assertEquals("d1", d.get("id"));
		var table = (Map<?,?>)d.get("table");
		assertNotNull(table);
		assertEquals("/x", table.get("dataUrl"));
		assertFalse(d.containsKey("dataUrl"));
	}

	@Test void a07_authoredBodyNotMutated() {
		var s = src(OK);
		REG.toCard(s);
		var again = new SwitcherCardType().toFragment(s);
		var d = new SwitcherCardType().nestedCards(again).get(1);
		assertEquals("/x", d.get("dataUrl"));
	}

	@Test void a08_invalid() {
		var e = assertThrows(IllegalArgumentException.class, () -> REG.toCard(src("{views:[]}")));
		assertEquals("<@card id='sw'> SwitcherDef 'sw' requires at least one view.", e.getMessage());
		e = assertThrows(IllegalArgumentException.class, () -> REG.toCard(src("{views:[{id:'a.b',label:'A',card:{id:'c',type:'html'}}]}")));
		assertTrue(e.getMessage().contains("view id 'a.b' must match"), e.getMessage());
		e = assertThrows(IllegalArgumentException.class, () -> REG.toCard(src("{views:[{id:'a',label:'A',card:{id:'c',type:'switcher'}}]}")));
		assertTrue(e.getMessage().contains("cannot hold a switcher"), e.getMessage());
	}

	@Test void a09_nestedBadSrcRejectedByItsOwnHandler() {
		var e = assertThrows(IllegalArgumentException.class, () -> REG.toCard(src(
			"{views:[{id:'a',label:'A',card:{id:'c1',type:'chart',src:'http://evil/x',kind:'line',x:{field:'d'},series:['n']}}]}")));
		assertTrue(e.getMessage().contains("same-origin"), e.getMessage());
	}

	@Test void a10_requiresJsonBody() {
		var e = assertThrows(IllegalArgumentException.class, () -> REG.toCard(src("plain")));
		assertTrue(e.getMessage().contains("type='switcher' requires a JSON5 body."), e.getMessage());
	}
}
