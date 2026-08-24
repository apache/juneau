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

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.widgets.*;
import org.junit.jupiter.api.*;

/**
 * Markup + escaping tests for the {@link AppHeaderTable} emitter &mdash; the {@code <header class="jc-header">} page
 * chrome the {@code juneau-chrome.js} runtime enhances.
 *
 * <p>
 * Pins the {@code data-juneau-*} DOM contract the runtime depends on, and the locked behavior of 445m now that
 * the shared views layer stack (445h) has shipped: chips / {@link Behavior#LINK} / {@link Behavior#SAFE} are fully
 * functional, and a {@link Behavior#MENU} trigger is emitted <b>enabled</b> with real menu ARIA
 * ({@code aria-haspopup="menu"}, {@code aria-expanded="false"}, {@code aria-controls}) and its
 * {@code .jc-menu}/{@code .jc-menu-item}/{@code .jc-menu-divider} list markup emitted (hidden until opened via the
 * views {@code pushLayer} stack) &mdash; no {@code <details>} fake disclosure.  No {@code .jc-nav}/{@code .jc-nav-tab}
 * is ever emitted (445f standing nav rule), and every human string is entity-escaped.
 */
class AppHeaderTable_Emit_Test extends TestBase {

	private static AppHeaderDef header() {
		return AppHeaderDef.create("app")
			.brand(Brand.create().logo(true).title("Juneau").crumbs("Admin", "Releases"))
			.actions(
				HeaderAction.link("docs", "table", "Docs", "/docs"),
				HeaderAction.safe("reload", "refresh", "Refresh", "refresh-counts")
					.badge(Badge.count(120).max(99).tone(Tone.WARN)),
				HeaderAction.menu("more", "tune", "More").menu(MenuItem.link("a", "Item A", "/a")))
			.avatar(AvatarChip.of("Ada Lovelace").initials("AL").status(Status.AWAY))
			.refreshUrl("/chrome/counts")
			.build();
	}

	private static String html(AppHeaderDef h) {
		return Html.of(AppHeaderTable.of(h));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Header shell + refresh attr
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_headerMarkerAndClass() {
		var h = html(header());
		assertTrue(h.startsWith("<header"), h);
		assertTrue(h.contains("class=\"jc-header\""), h);
		assertTrue(h.contains("data-juneau-app-header=\"app\""), h);
	}

	@Test void a02_refreshAttrOnlyWhenRefreshUrlSet() {
		assertTrue(html(header()).contains("data-juneau-refresh=\"/chrome/counts\""), "refresh attr expected");
		var noRefresh = AppHeaderDef.create("app")
			.actions(HeaderAction.link("docs", "table", "Docs", "/docs")).build();
		assertFalse(html(noRefresh).contains("data-juneau-refresh"), "no refresh attr when refreshUrl unset");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Brand
	//------------------------------------------------------------------------------------------------------------------

	@Test void a03_brandLogoTitleCrumbs() {
		var h = html(header());
		assertTrue(h.contains("class=\"jc-brand\""), h);
		assertTrue(h.contains("class=\"jc-logo\""), h);
		assertTrue(h.contains("class=\"jc-brand-title\""), h);
		assertTrue(h.contains(">Juneau<"), h);
		assertTrue(h.contains(">Admin<"), h);
		assertTrue(h.contains(">Releases<"), h);
		assertTrue(h.contains("jc-brand-sep"), h);
	}

	@Test void a04_logoOmittedWhenFalse() {
		var h = html(AppHeaderDef.create("app")
			.brand(Brand.create().logo(false).title("T"))
			.actions(HeaderAction.link("d", "table", "Docs", "/d")).build());
		assertFalse(h.contains("jc-logo"), "no logo node when logo(false)");
	}

	//------------------------------------------------------------------------------------------------------------------
	// LINK action -> <a>
	//------------------------------------------------------------------------------------------------------------------

	@Test void a05_linkActionIsAnchor() {
		var h = html(header());
		assertTrue(h.contains("<a href=\"/docs\""), h);
		assertTrue(h.contains("data-juneau-header-action=\"docs\""), h);
		assertTrue(h.contains("data-juneau-behavior=\"link\""), h);
		assertTrue(h.contains("data-juneau-icon=\"table\""), h);
		assertTrue(h.contains("aria-label=\"Docs\""), h);
		assertTrue(h.contains("title=\"Docs\""), h);
		assertTrue(h.contains("class=\"jc-icon\""), "empty icon span present (glyph hydrated client-side)");
	}

	//------------------------------------------------------------------------------------------------------------------
	// SAFE action -> functional <button>, with badge
	//------------------------------------------------------------------------------------------------------------------

	@Test void a06_safeActionIsFunctionalButton() {
		var h = html(header());
		assertTrue(h.contains("data-juneau-header-action=\"reload\""), h);
		assertTrue(h.contains("data-juneau-behavior=\"safe\""), h);
		assertTrue(h.contains("data-juneau-safe=\"refresh-counts\""), h);
		// SAFE is fully functional under M1 B - the trigger must NOT be disabled.
		var safeFrag = h.substring(h.indexOf("data-juneau-header-action=\"reload\""));
		safeFrag = safeFrag.substring(0, safeFrag.indexOf("</button>"));
		assertFalse(safeFrag.contains("disabled"), "SAFE trigger must not be disabled");
	}

	@Test void a07_badgeNamespacedTonedClampedWithMax() {
		var h = html(header());
		assertTrue(h.contains("data-juneau-badge=\"header:reload\""), h);
		assertTrue(h.contains("data-juneau-badge-tone=\"warn\""), "WARN tone emitted (CSS maps it to amber)");
		assertTrue(h.contains("data-juneau-badge-max=\"99\""), h);
		assertTrue(h.contains(">99+<"), "count 120 clamped to '99+' server-side");
	}

	//------------------------------------------------------------------------------------------------------------------
	// MENU action -> ENABLED trigger with real menu ARIA + emitted list (wired to the views layer stack)
	//------------------------------------------------------------------------------------------------------------------

	@Test void a08_menuTriggerEnabled_withMenuAria() {
		var h = html(header());
		assertTrue(h.contains("data-juneau-header-action=\"more\""), h);
		assertTrue(h.contains("data-juneau-behavior=\"menu\""), h);
		var menuFrag = h.substring(h.indexOf("data-juneau-header-action=\"more\""));
		menuFrag = menuFrag.substring(0, menuFrag.indexOf("</button>"));
		// A working MENU trigger is a live button - never disabled / aria-disabled.
		assertFalse(menuFrag.contains("disabled"), "a working MENU trigger must not be disabled");
		assertFalse(menuFrag.contains("aria-disabled"), "a working MENU trigger must not be aria-disabled");
		assertTrue(menuFrag.contains("aria-haspopup=\"menu\""), h);
		assertTrue(menuFrag.contains("aria-expanded=\"false\""), h);
		assertTrue(menuFrag.contains("aria-controls=\"juneau-menu:app:more\""), h);
	}

	@Test void a09_menuListEmittedWithRealAria_noDetails() {
		var h = html(header());
		assertFalse(h.contains("<details"), "no <details> fake disclosure - the views layer stack owns opening");
		assertTrue(h.contains("class=\"jc-menu\""), "the menu list markup is emitted");
		assertTrue(h.contains("id=\"juneau-menu:app:more\""), h);
		assertTrue(h.contains("role=\"menu\""), "real menu ARIA now that the JS layer manager exists");
		assertTrue(h.contains("class=\"jc-menu-item\""), h);
		assertTrue(h.contains("role=\"menuitem\""), h);
		assertTrue(h.contains(">Item A<"), "menu item label emitted");
		assertTrue(h.contains("href=\"/a\""), "menu item link href emitted");
	}

	@Test void a09b_menuSafeItemAndDivider() {
		var h = html(AppHeaderDef.create("app")
			.actions(HeaderAction.menu("more", "tune", "More")
				.menu(MenuItem.link("a", "Item A", "/a"), MenuItem.divider(), MenuItem.safe("b", "Do It", "do-it")))
			.build());
		assertTrue(h.contains("class=\"jc-menu-divider\""), h);
		assertTrue(h.contains("role=\"separator\""), "a divider is an inert separator row");
		assertTrue(h.contains("data-juneau-safe=\"do-it\""), "a SAFE menu item carries its dispatch token");
		assertTrue(h.contains(">Do It<"), h);
		// The SAFE item is a real <button> (host-dispatch); the link item is an <a href> (same-origin navigation).
		assertTrue(h.contains("<button type=\"button\" class=\"jc-menu-item\""), h);
		assertTrue(h.contains("<a href=\"/a\" class=\"jc-menu-item\""), h);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Avatar
	//------------------------------------------------------------------------------------------------------------------

	@Test void a10_avatarNoMenuIsNonInteractiveChip() {
		var h = html(header());
		assertTrue(h.contains("data-juneau-avatar"), h);
		assertTrue(h.contains("jc-avatar-status-away"), "AWAY status class (CSS maps it to amber)");
		assertTrue(h.contains("role=\"img\""), "a menu-less avatar is a non-interactive chip, not a dead button");
		assertTrue(h.contains("aria-label=\"Ada Lovelace\""), h);
		assertTrue(h.contains(">AL<"), "initials rendered as escaped text");
	}

	@Test void a11_avatarWithMenuIsEnabledTrigger() {
		var h = html(AppHeaderDef.create("app")
			.avatar(AvatarChip.of("Ada").initials("A").menu(MenuItem.link("p", "Profile", "/p"))).build());
		var btn = h.substring(h.indexOf("data-juneau-avatar"));
		btn = btn.substring(0, btn.indexOf("</button>"));
		assertFalse(btn.contains("disabled"), "an avatar with a menu is now a live trigger (menus shipped)");
		assertFalse(btn.contains("aria-disabled"), h);
		assertTrue(btn.contains("aria-haspopup=\"menu\""), h);
		assertTrue(btn.contains("aria-expanded=\"false\""), h);
		assertTrue(btn.contains("aria-controls=\"juneau-menu:app:avatar\""), h);
		assertTrue(h.contains(">Profile<"), "avatar menu item now emitted");
		assertTrue(h.contains("href=\"/p\""), h);
	}

	@Test void a12_avatarImageWithHiddenInitialsFallback() {
		var h = html(AppHeaderDef.create("app")
			.avatar(AvatarChip.of("Ada").initials("AL").imageUrl("/avatars/ada.png")).build());
		assertTrue(h.contains("<img src=\"/avatars/ada.png\""), h);
		assertTrue(h.contains("jc-avatar-img"), h);
		assertTrue(h.contains("jc-avatar-initials"), "initials kept (hidden) for the image-error fallback");
	}

	//------------------------------------------------------------------------------------------------------------------
	// No nav (445f standing rule)
	//------------------------------------------------------------------------------------------------------------------

	@Test void a13_neverEmitsNav() {
		var h = html(header());
		assertFalse(h.contains("jc-nav"), "the app-header must never emit .jc-nav / .jc-nav-tab");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Escaping (security-critical)
	//------------------------------------------------------------------------------------------------------------------

	@Test void a14_humanStringsEntityEscaped() {
		var h = html(AppHeaderDef.create("app")
			.brand(Brand.create().logo(false).title("<script>alert(1)</script>"))
			.actions(HeaderAction.link("d", "table", "Docs", "/d")).build());
		assertFalse(h.contains("<script>alert(1)</script>"), "a script-shaped title must never become a live tag");
		assertTrue(h.contains("&lt;script&gt;"), "title emitted as entity-escaped text");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Sidecar
	//------------------------------------------------------------------------------------------------------------------

	@Test void a15_sidecarCarriesRawCountsAndContract() {
		var sc = AppHeaderTable.sidecar(header());
		assertNotNull(sc, "sidecar emitted when refreshUrl is set");
		var s = Html.of(sc);
		assertTrue(s.contains("id=\"juneau-header:app\""), s);
		assertTrue(s.contains("\"contractVersion\":\"1\""), s);
		assertTrue(s.contains("\"header:reload\":120"), "sidecar carries the RAW (un-clamped) count");
	}

	@Test void a16_noSidecarWithoutRefreshUrl() {
		var noRefresh = AppHeaderDef.create("app")
			.actions(HeaderAction.link("d", "table", "Docs", "/d")).build();
		assertNull(AppHeaderTable.sidecar(noRefresh), "no sidecar when refreshUrl is unset");
	}
}
