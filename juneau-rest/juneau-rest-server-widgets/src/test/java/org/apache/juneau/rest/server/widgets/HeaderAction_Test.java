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

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * {@link HeaderAction} factory / fluent-setter coverage and per-type {@link HeaderAction#validate()} edge cases.
 * Also pins that {@code HeaderAction} is <b>not</b> a {@link Widget} (S1).
 */
class HeaderAction_Test extends TestBase {

	@Test void a01_link_factory() {
		var a = HeaderAction.link("git", "git", "Repository", "/git");
		assertEquals(Behavior.LINK, a.behavior);
		assertEquals("/git", a.href);
		a.validate();
	}

	@Test void a02_safe_factory() {
		var a = HeaderAction.safe("drawer", "menu", "Open", "open-drawer");
		assertEquals(Behavior.SAFE, a.behavior);
		assertEquals("open-drawer", a.safe);
		a.validate();
	}

	@Test void a03_menu_factory_withItems() {
		var a = HeaderAction.menu("bell", "notifications", "Notifications")
			.menu(MenuItem.link("all", "See all", "/n"), MenuItem.divider(), MenuItem.safe("clear", "Clear", "clear-all"));
		assertEquals(Behavior.MENU, a.behavior);
		a.validate();
	}

	@Test void a04_tooltipRequired() {
		var a = HeaderAction.link("x", "i", "  ", "/x");
		var e = assertThrows(IllegalArgumentException.class, a::validate);
		assertTrue(e.getMessage().contains("tooltip"), e::getMessage);
	}

	@Test void a05_badge_fansOut() {
		var bad = new Badge();   // neither count nor dot
		var a = HeaderAction.link("x", "i", "X", "/x").badge(bad);
		assertThrows(IllegalArgumentException.class, a::validate);
	}

	@Test void a06_menuDuplicateItemId_rejected() {
		var a = HeaderAction.menu("m", "i", "M").menu(MenuItem.link("d", "A", "/a"), MenuItem.link("d", "B", "/b"));
		var e = assertThrows(IllegalArgumentException.class, a::validate);
		assertTrue(e.getMessage().contains("d"), e::getMessage);
	}

	@Test void a07_menuWithDividers_ok() {
		// Two dividers do not collide on id (they are id-exempt).
		var a = HeaderAction.menu("m", "i", "M").menu(MenuItem.divider(), MenuItem.link("x", "X", "/x"), MenuItem.divider());
		a.validate();
	}

	@Test void a08_notAWidget() {
		assertFalse(Widget.class.isAssignableFrom(HeaderAction.class));
	}
}
