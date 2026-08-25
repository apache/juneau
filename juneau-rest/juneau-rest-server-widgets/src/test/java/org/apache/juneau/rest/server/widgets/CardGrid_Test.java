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
package org.apache.juneau.rest.server.widgets;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * {@link CardGrid} bean contract and fail-closed {@link CardGrid#validate()} branches.
 */
class CardGrid_Test extends TestBase {

	private static Card card(String id) {
		return Card.create(id, "T").body(CardFieldList.create().fields(CardField.of("k", "L", "v")));
	}

	@Test void a01_contractVersion_isOne() {
		assertEquals("1", CardGrid.CONTRACT_VERSION);
	}

	@Test void a02_builder_roundTrip() {
		var g = CardGrid.create("g1").title("Dashboard").minCardPx(320).cards(card("c1"), card("c2"));
		assertEquals("g1", g.id);
		assertEquals("Dashboard", g.title);
		assertEquals(320, g.minCardPx);
		assertSize(2, g.cards);
		g.validate();
	}

	@Test void a03_minCardPx_unset_ok() {
		var g = CardGrid.create("g1").cards(card("c1"));
		assertNull(g.minCardPx);
		g.validate();
	}

	@Test void a04_blankId_rejected() {
		var g = CardGrid.create("  ").cards(card("c1"));
		assertThrows(IllegalArgumentException.class, g::validate);
	}

	@Test void a05_noCards_rejected() {
		var g1 = CardGrid.create("g1");
		assertThrows(IllegalArgumentException.class, () -> g1.validate());
		var g2 = CardGrid.create("g1").cards();
		assertThrows(IllegalArgumentException.class, () -> g2.validate());
	}

	@Test void a06_duplicateCardId_rejected() {
		var g = CardGrid.create("g1").cards(card("dup"), card("dup"));
		var e = assertThrows(IllegalArgumentException.class, g::validate);
		assertTrue(e.getMessage().contains("dup"), e::getMessage);
	}

	@Test void a07_minCardPxBelowOne_rejected() {
		var g = CardGrid.create("g1").minCardPx(0).cards(card("c1"));
		assertThrows(IllegalArgumentException.class, g::validate);
	}

	@Test void a08_validate_fansOutToCard() {
		var bad = Card.create("c1", "T");   // no body
		var g = CardGrid.create("g1").cards(bad);
		assertThrows(IllegalArgumentException.class, g::validate);
	}
}
