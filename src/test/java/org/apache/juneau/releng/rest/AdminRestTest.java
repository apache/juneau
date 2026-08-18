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

package org.apache.juneau.releng.rest;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.juneau.commons.inject.StackOverlay;
import org.apache.juneau.commons.utils.IoUtils;
import org.apache.juneau.marshall.html.HtmlSerializer;
import org.apache.juneau.rest.mock.MockRestClient;
import org.apache.juneau.rest.server.views.PageTable;
import org.apache.juneau.rest.server.views.ViewTable;
import org.apache.juneau.rest.server.views.ViewsMixin;
import org.junit.jupiter.api.Test;

/**
 * TODO-399 Phase C dogfood (tasks 10-12): the RM {@code Admin} tab composes the existing Releases/Credentials
 * {@link org.apache.juneau.rest.server.views.ViewDef ViewDef}s into one {@link org.apache.juneau.rest.server.views.PageDef PageDef}
 * page, rendered by {@link PageTable} and served through {@link AdminRest}.
 */
class AdminRestTest {

	/**
	 * {@link MockRestClient#create(Object)} caches its {@code RestContext} per resource class (see
	 * {@code ReleaseRestTest}'s identical helper javadoc); pass a fresh {@link StackOverlay} to opt out.
	 */
	@SuppressWarnings({
		"resource" // Caller owns and closes the returned MockRestClient (via try-with-resources); Eclipse JDT @Owning warning is by design.
	})
	private static MockRestClient client() {
		return MockRestClient.builder(new AdminRest()).overridingBeanStore(new StackOverlay()).build();
	}

	// -----------------------------------------------------------------------------------------------------------
	// Task 10: PageDef composition
	// -----------------------------------------------------------------------------------------------------------

	@Test
	void adminPageComposesOneTabPerExistingView() {
		var page = AdminRest.adminPage();
		assertEquals("admin", page.id);
		assertEquals(2, page.tabs.size());
		assertEquals("releases", page.tabs.get(0).id);
		assertEquals("releases", page.tabs.get(0).view.id);
		assertEquals("credentials", page.tabs.get(1).id);
		assertEquals("credentials", page.tabs.get(1).view.id);
	}

	@Test
	void adminPageBuildsWithoutValidationErrors() {
		// PageDef.build() rejects duplicate tab ids / duplicate referenced ViewDef ids (Phase C task 2 rules);
		// building here (rather than only in AdminRest.adminPage()) proves the composition is actually valid,
		// not just that adminPage() happens not to throw.
		assertDoesNotThrow(AdminRest::adminPage);
	}

	// -----------------------------------------------------------------------------------------------------------
	// Task 11: PageTable wiring + no regression to the standalone per-view output
	// -----------------------------------------------------------------------------------------------------------

	@Test
	void pageServesPageTableShellWithBothPanelsAndSidecars() throws Exception {
		try (var client = client()) {
			try (var resp = client.request("GET", "/").run()) {
				assertEquals(200, resp.getStatusCode());
				var body = resp.getBodyAsString();
				assertTrue(body.contains("data-juneau-page='admin'"), "Missing page shell: " + body);
				assertTrue(body.contains("data-juneau-view='releases'"), "Missing releases panel: " + body);
				assertTrue(body.contains("data-juneau-view='credentials'"), "Missing credentials panel: " + body);
				assertTrue(body.contains("juneau-page:admin"), "Missing PAGE_META sidecar: " + body);
				assertTrue(body.contains("juneau-view:releases"), "Missing releases VIEW_META sidecar: " + body);
				assertTrue(body.contains("juneau-view:credentials"), "Missing credentials VIEW_META sidecar: " + body);
			}
		}
	}

	@Test
	void wrappedReleasesViewMarkupIsByteForByteIdenticalToStandalone() {
		var wrapped = HtmlSerializer.DEFAULT_SIMPLE_SQ.toString(PageTable.of(AdminRest.adminPage()));
		var standalone = HtmlSerializer.DEFAULT_SIMPLE_SQ.toString(ViewTable.of(ReleaseRest.releasesView()));
		assertTrue(wrapped.contains(standalone),
			"Page-wrapped Releases view markup diverged from the standalone ViewTable.of(...) output.");
	}

	@Test
	void wrappedCredentialsViewMarkupIsByteForByteIdenticalToStandalone() {
		var wrapped = HtmlSerializer.DEFAULT_SIMPLE_SQ.toString(PageTable.of(AdminRest.adminPage()));
		var standalone = HtmlSerializer.DEFAULT_SIMPLE_SQ.toString(ViewTable.of(CredentialRest.credentialsView()));
		assertTrue(wrapped.contains(standalone),
			"Page-wrapped Credentials view markup diverged from the standalone ViewTable.of(...) output.");
	}

	@Test
	void assetsAreServedAtTheAdminMount() throws Exception {
		try (var client = client()) {
			try (var resp = client.request("GET", ViewsMixin.PAGES_JS_PATH).run()) {
				assertEquals(200, resp.getStatusCode());
				assertTrue(resp.getBodyAsString().contains("JuneauViews"));
			}
			try (var resp = client.request("GET", ViewsMixin.VIEWS_JS_PATH).run()) {
				assertEquals(200, resp.getStatusCode());
			}
		}
	}

	// -----------------------------------------------------------------------------------------------------------
	// Task 12: the composed page uses the self-contained -views tab shell (PageTable's classes), not hand-rolled
	// per-page tab markup — verified here structurally (the shell's own marker classes); base.ftlh's nav-link and
	// asset-include wiring for the Admin tab is exercised end-to-end by pageServesPageTableShellWithBothPanelsAndSidecars
	// once rendered through the FreeMarker template at /admin.
	// -----------------------------------------------------------------------------------------------------------

	@Test
	void composedPageUsesTheSharedTabShellClassesNotBespokeMarkup() {
		var html = HtmlSerializer.DEFAULT_SIMPLE_SQ.toString(PageTable.of(AdminRest.adminPage()));
		assertTrue(html.contains("class='" + PageTable.TAB_BAR_CLASS + "'") || html.contains(PageTable.TAB_BAR_CLASS));
		assertTrue(html.contains(PageTable.TAB_CLASS));
		assertTrue(html.contains(PageTable.PANEL_CLASS));
	}

	/**
	 * The app's own cross-resource nav (base.ftlh's {@code .jc-nav}) gains an Admin entry, and the Admin tab pulls
	 * in the opt-in {@code juneau-pages.js} runtime after {@code juneau-views.js} &mdash; the top-level nav
	 * mechanism itself (real links across {@code @Rest(children=...)} resources) is intentionally unchanged
	 * (design doc non-goal: no top-level cross-resource navigation redesign; Phase C owns only the in-page tab
	 * switch, which {@link #composedPageUsesTheSharedTabShellClassesNotBespokeMarkup()} already verifies uses the
	 * shared shell classes).
	 */
	@Test
	void baseTemplateWiresTheAdminNavLinkAndPagesRuntime() throws IOException {
		String base;
		try (var in = AdminRestTest.class.getResourceAsStream("/templates/base.ftlh")) {
			assertNotNull(in, "templates/base.ftlh not found on the test classpath");
			base = new String(IoUtils.readBytes(in), StandardCharsets.UTF_8);
		}
		assertTrue(base.contains("href=\"/rest/admin\""), "Missing Admin nav link: " + base);
		assertTrue(base.contains("activeTab == 'admin'"), "Missing admin-tab conditional asset wiring: " + base);
		assertTrue(base.contains("pagesJsUrl"), "Missing juneau-pages.js include for the Admin tab: " + base);
	}

	/**
	 * Regression: {@code juneau-icons.js} was never included on the page (only renders/ribbon/views were), so the
	 * icon registry was absent at ribbon-build time and every ribbon/paging-pill button fell back to rendering its
	 * label as plain text instead of a glyph. Asserts the include exists AND is ordered before {@code ribbonJsUrl}
	 * (the ribbon/pill buttons resolve their icons from the registry when they're built, so it must already exist).
	 */
	@Test
	void baseTemplateIncludesIconsJsBeforeRibbonJs() throws IOException {
		String base;
		try (var in = AdminRestTest.class.getResourceAsStream("/templates/base.ftlh")) {
			assertNotNull(in, "templates/base.ftlh not found on the test classpath");
			base = new String(IoUtils.readBytes(in), StandardCharsets.UTF_8);
		}
		assertTrue(base.contains("iconsJsUrl"), "Missing juneau-icons.js include: " + base);
		var iconsIdx = base.indexOf("iconsJsUrl");
		var ribbonIdx = base.indexOf("ribbonJsUrl");
		assertTrue(ribbonIdx >= 0, "Missing juneau-ribbon.js include: " + base);
		assertTrue(iconsIdx < ribbonIdx,
			"juneau-icons.js must be included before juneau-ribbon.js (icon registry must exist when the ribbon builds its buttons): " + base);
	}
}
