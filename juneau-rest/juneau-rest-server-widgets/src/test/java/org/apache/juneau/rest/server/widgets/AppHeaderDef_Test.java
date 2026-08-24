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

import java.nio.file.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * {@link AppHeaderDef} bean contract and fail-closed {@link AppHeaderDef#validate()} branches, plus the L8 A
 * "no {@code roles()} API" and views-module isolation rules.
 */
class AppHeaderDef_Test extends TestBase {

	@Test void a01_contractVersion_isOne() {
		assertEquals("1", AppHeaderDef.CONTRACT_VERSION);
	}

	@Test void a02_builder_roundTrip() {
		var h = AppHeaderDef.create("hdr-main")
			.brand(Brand.create().logo(true).title("Admin").crumbs("Releases"))
			.actions(
				HeaderAction.link("git", "git", "Repository", "/git"),
				HeaderAction.safe("drawer", "menu", "Open drawer", "open-drawer"),
				HeaderAction.menu("bell", "notifications", "Notifications").badge(Badge.count(3).tone(Tone.DANGER)).menu(MenuItem.link("all", "See all", "/notifications")))
			.avatar(AvatarChip.of("Ada L.").initials("AL").status(Status.ONLINE))
			.refreshUrl("/header/counts")
			.build();
		assertEquals("hdr-main", h.id);
		assertEquals("Admin", h.brand.title);
		assertSize(3, h.actions);
		assertEquals("AL", h.avatar.initials);
		assertEquals("/header/counts", h.refreshUrl);
	}

	@Test void a03_blankId_rejected() {
		assertThrows(IllegalArgumentException.class, () -> AppHeaderDef.create("  ").validate());
		assertThrows(IllegalArgumentException.class, () -> AppHeaderDef.create(null).validate());
	}

	@Test void a04_badContractVersion_rejected() {
		var h = AppHeaderDef.create("h");
		h.contractVersion = "2";
		assertThrows(IllegalArgumentException.class, h::validate);
	}

	@Test void a05_duplicateActionId_rejected() {
		var h = AppHeaderDef.create("h").actions(
			HeaderAction.link("dup", "a", "A", "/a"),
			HeaderAction.link("dup", "b", "B", "/b"));
		var e = assertThrows(IllegalArgumentException.class, h::validate);
		assertTrue(e.getMessage().contains("dup"), e::getMessage);
	}

	@Test void a06_linkWithoutHref_rejected() {
		var a = HeaderAction.link("x", "i", "X", "/ok");
		a.href = null;
		assertThrows(IllegalArgumentException.class, () -> AppHeaderDef.create("h").actions(a).validate());
	}

	@Test void a07_linkBadHref_rejected() {
		for (var bad : new String[]{"http://evil.test/x", "https://x", "//evil.test", "../up", "data:text/html,x", "javascript:alert(1)"}) {
			var h = AppHeaderDef.create("h").actions(HeaderAction.link("x", "i", "X", bad));
			var e = assertThrows(IllegalArgumentException.class, h::validate, () -> "expected reject for " + bad);
			assertTrue(e.getMessage().contains("same-origin"), e::getMessage);
		}
	}

	@Test void a08_menuWithEmptyMenu_rejected() {
		var a = HeaderAction.menu("m", "i", "M");   // no menu items attached
		assertThrows(IllegalArgumentException.class, () -> AppHeaderDef.create("h").actions(a).validate());
	}

	@Test void a09_safeWithHref_rejected() {
		var a = HeaderAction.safe("s", "i", "S", "open-drawer");
		a.href = "/x";
		assertThrows(IllegalArgumentException.class, () -> AppHeaderDef.create("h").actions(a).validate());
	}

	@Test void a10_safeBadTokens_rejected() {
		for (var bad : new String[]{"", "  ", "Open", "open_drawer", "open drawer", "1open", "a".repeat(65)}) {
			var a = HeaderAction.safe("s", "i", "S", bad);
			assertThrows(IllegalArgumentException.class, () -> AppHeaderDef.create("h").actions(a).validate(), () -> "expected reject for '" + bad + "'");
		}
	}

	@Test void a11_avatar_fansOut() {
		var h = AppHeaderDef.create("h").avatar(AvatarChip.of("Ada").initials("AD").imageUrl("http://evil/x.png"));
		assertThrows(IllegalArgumentException.class, h::validate);
	}

	@Test void a12_refreshUrl_mustBeSameOrigin() {
		var h = AppHeaderDef.create("h").refreshUrl("http://evil/counts");
		assertThrows(IllegalArgumentException.class, h::validate);
	}

	@Test void a13_isAWidget() {
		assertTrue(Widget.class.isAssignableFrom(AppHeaderDef.class));
	}

	// L8 A: no roles() API anywhere on the header beans - no field, no method, in v1.
	@Test void a14_noRolesApi() throws Exception {
		for (var c : new Class<?>[]{AppHeaderDef.class, HeaderAction.class, AvatarChip.class, MenuItem.class}) {
			for (var m : c.getMethods())
				assertNotEquals("roles", m.getName(), () -> "unexpected roles() method on " + c.getName());
			for (var f : c.getFields())
				assertNotEquals("roles", f.getName(), () -> "unexpected roles field on " + c.getName());
		}
	}

	// S7: the widgets module is data + validate() only - no views / rest.server import.
	@Test void a15_widgetsModule_hasNoViewsOrServerImport() throws Exception {
		var root = Path.of("").toAbsolutePath();
		var src = root;
		if (!Files.isDirectory(src.resolve("src/main/java")))
			src = root.resolve("juneau-rest/juneau-rest-server-widgets");
		assertTrue(Files.isDirectory(src.resolve("src/main/java")), src::toString);
		try (var walk = Files.walk(src.resolve("src/main/java"))) {
			for (var f : walk.filter(p -> p.toString().endsWith(".java")).toList()) {
				var text = Files.readString(f);
				assertFalse(text.contains("import org.apache.juneau.rest.server.views."),
					() -> "widgets module must not import views: " + f);
				assertFalse(text.contains("import org.apache.juneau.rest.server.RestContext"),
					() -> "widgets module must not import rest.server runtime: " + f);
				assertFalse(text.contains("import org.apache.juneau.bean.html5."),
					() -> "widgets beans must not import html5 (emit lives in views): " + f);
			}
		}
	}
}
