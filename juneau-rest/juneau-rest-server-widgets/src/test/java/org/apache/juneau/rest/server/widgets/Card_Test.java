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

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * {@link Card} bean contract and fail-closed {@link Card#validate()} branches.
 */
class Card_Test extends TestBase {

	private static CardFieldList body() {
		return CardFieldList.create().fields(CardField.of("k", "L", "v"));
	}

	@Test void a01_builder_roundTrip() {
		var c = Card.create("c1", "Title").body(body());
		assertEquals("c1", c.id);
		assertEquals("Title", c.title);
		assertNotNull(c.body);
		c.validate();
	}

	@Test void a02_blankId_rejected() {
		var c = Card.create("  ", "Title").body(body());
		assertThrows(IllegalArgumentException.class, c::validate);
	}

	@Test void a03_blankTitle_rejected() {
		var c = Card.create("c1", "  ").body(body());
		assertThrows(IllegalArgumentException.class, c::validate);
	}

	@Test void a04_nullBody_rejected() {
		var c = Card.create("c1", "Title");
		assertThrows(IllegalArgumentException.class, c::validate);
	}

	@Test void a05_validate_fansOutToBody() {
		var c = Card.create("c1", "Title").body(CardFieldList.create());   // no fields
		assertThrows(IllegalArgumentException.class, c::validate);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Per-card action catalog: the header action vocabulary, reused verbatim
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_actionsDefaultToNone() {
		var c = Card.create("c1", "Title").body(body());
		assertNull(c.actions, "a card declares no actions unless the author asks for them");
		c.validate();
	}

	@Test void b02_actionsAreHeaderActions_allThreeBehaviors() {
		var c = Card.create("c1", "Title").body(body()).actions(
			HeaderAction.link("open", "external", "Open in new tab", "/reports/1"),
			HeaderAction.safe("export", "download", "Export as CSV", "card-export"),
			HeaderAction.menu("more", "overflow", "More actions").menu(MenuItem.safe("pin", "Pin", "card-pin")));
		assertEquals(3, c.actions.size());
		assertEquals("open", c.actions.get(0).id);
		assertEquals(Behavior.LINK, c.actions.get(0).behavior);
		assertEquals(Behavior.SAFE, c.actions.get(1).behavior);
		assertEquals(Behavior.MENU, c.actions.get(2).behavior);
		c.validate();
	}

	@Test void b03_validate_fansOutToEachAction_blankTooltipRejected() {
		// The tooltip IS the accessible name (a HeaderAction has no label); Card must not swallow that rejection.
		var c = Card.create("c1", "Title").body(body()).actions(
			HeaderAction.link("open", "external", "  ", "/reports/1"));
		var e = assertThrows(IllegalArgumentException.class, c::validate);
		assertTrue(e.getMessage().contains("tooltip"), e::getMessage);
	}

	@Test void b04_nullBehavior_rejected() {
		// Only the locked Behavior values are representable, and none of them may be absent.
		var a = HeaderAction.link("open", "external", "Open", "/reports/1");
		a.behavior = null;
		var c = Card.create("c1", "Title").body(body()).actions(a);
		var e = assertThrows(IllegalArgumentException.class, c::validate);
		assertTrue(e.getMessage().contains("behavior"), e::getMessage);
	}

	@Test void b05_nullAction_rejected() {
		var c = Card.create("c1", "Title").body(body());
		c.actions = Arrays.asList(HeaderAction.link("open", "external", "Open", "/reports/1"), null);
		assertThrows(IllegalArgumentException.class, c::validate);
	}

	@Test void b06_duplicateActionId_rejected() {
		var c = Card.create("c1", "Title").body(body()).actions(
			HeaderAction.link("open", "external", "Open", "/a"),
			HeaderAction.link("open", "external", "Open again", "/b"));
		var e = assertThrows(IllegalArgumentException.class, c::validate);
		assertTrue(e.getMessage().contains("open"), e::getMessage);
	}

	@Test void b07_emptyActionsIsNotAnError() {
		// An author-supplied empty catalog is a card with no actions, not a malformed card.
		var c = Card.create("c1", "Title").body(body()).actions();
		assertTrue(c.actions.isEmpty());
		c.validate();
	}

	@Test void b08_actionMalformedForItsBehavior_rejected() {
		// A MENU with no items is the HeaderAction-level contract; reached only because Card fans out.
		var c = Card.create("c1", "Title").body(body()).actions(
			HeaderAction.menu("more", "overflow", "More actions"));
		assertThrows(IllegalArgumentException.class, c::validate);
	}
}
