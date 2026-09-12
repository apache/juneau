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
import org.apache.juneau.rest.mock.MockRestClient;
import org.apache.juneau.rest.server.filter.LoopbackBoundary;
import org.apache.juneau.rest.server.views.PageTable;
import org.apache.juneau.rest.server.views.ViewsMixin;
import org.junit.jupiter.api.Test;

/**
 * Admin pair pages: path-per-child, local {@code .juneau-page-nav}, empty slots, no {@code PageTable} shell.
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

	@Test
	void a01_defaultAdminUrlRedirectsToReleasesPair() throws Exception {
		try (var client = client()) {
			try (var resp = client.request("GET", "/").run()) {
				assertEquals(302, resp.getStatusCode());
				assertEquals(AdminRest.RELEASES_URL, resp.header("Location").getValue());
			}
		}
	}

	@Test
	void a02_unknownAdminChildIs404() throws Exception {
		try (var client = client()) {
			try (var resp = client.request("GET", "/foo").run()) {
				assertEquals(404, resp.getStatusCode());
			}
		}
	}

	@Test
	void b01_releasesPairServesAuthorNavAndEmptySlot() throws Exception {
		try (var client = client()) {
			try (var resp = client.request("GET", "/releases").run()) {
				assertEquals(200, resp.getStatusCode());
				var body = resp.getBodyAsString();
				assertAdminShell(body, "releases", "/rest/releases/view");
				assertTrue(body.contains("href=\"/rest/admin/releases\" aria-current=\"page\">Releases</a>"), body);
				assertFalse(body.contains("href=\"/rest/admin/credentials\" aria-current=\"page\""), body);
			}
		}
	}

	@Test
	void b02_credentialsPairServesAuthorNavAndEmptySlot() throws Exception {
		try (var client = client()) {
			try (var resp = client.request("GET", "/credentials").run()) {
				assertEquals(200, resp.getStatusCode());
				var body = resp.getBodyAsString();
				assertAdminShell(body, "credentials", "/rest/credentials/view");
				assertTrue(body.contains("href=\"/rest/admin/credentials\" aria-current=\"page\">Credentials</a>"), body);
				assertFalse(body.contains("href=\"/rest/admin/releases\" aria-current=\"page\">Releases</a>"), body);
			}
		}
	}

	@Test
	void b03_releasesPairMountsTheSharedReleasesEnvelopeUrl() throws Exception {
		try (var client = client()) {
			try (var resp = client.request("GET", "/releases").run()) {
				var body = resp.getBodyAsString();
				assertTrue(body.contains("\"tableUrl\":\"/rest/releases/view\""), body);
				assertTrue(body.contains("\"slotId\":\"releases\""), body);
			}
		}
	}

	@Test
	void b04_regionsAndHelpersAreServedAtTheAdminMount() throws Exception {
		try (var client = client()) {
			try (var resp = client.request("GET", ViewsMixin.REGIONS_JS_PATH).run()) {
				assertEquals(200, resp.getStatusCode());
				assertTrue(resp.getBodyAsString().contains("JuneauViews"));
			}
			try (var resp = client.request("GET", ViewsMixin.HELPERS_JS_PATH).run()) {
				assertEquals(200, resp.getStatusCode());
			}
			try (var resp = client.request("GET", ViewsMixin.VIEWS_JS_PATH).run()) {
				assertEquals(200, resp.getStatusCode());
			}
			try (var resp = client.request("GET", ViewsMixin.CONFIG_JS_PATH).run()) {
				assertEquals(200, resp.getStatusCode());
				assertTrue(resp.getBodyAsString().contains("JuneauViews"));
			}
			try (var resp = client.request("GET", ViewsMixin.CONFIG_CSS_PATH).run()) {
				assertEquals(200, resp.getStatusCode());
			}
		}
	}

	@Test
	void c01_releasesPairStampsCsrfOnBody() throws Exception {
		try (var client = client()) {
			try (var resp = client.request("GET", "/releases").run()) {
				var body = resp.getBodyAsString();
				assertTrue(body.contains("data-juneau-csrf="), body);
				assertTrue(body.contains("data-juneau-csrf-header=\"" + LoopbackBoundary.DEFAULT_CSRF_HEADER + "\""), body);
			}
		}
	}

	@Test
	void c02_baseTemplateWiresTheAdminNavLinkAndRegionsRuntime() throws IOException {
		String base;
		try (var in = AdminRestTest.class.getResourceAsStream("/templates/base.ftlh")) {
			assertNotNull(in, "templates/base.ftlh not found on the test classpath");
			base = new String(IoUtils.readBytes(in), StandardCharsets.UTF_8);
		}
		assertTrue(base.contains("href=\"/rest/admin\""), "Missing Admin nav link: " + base);
		assertTrue(base.contains("activeTab == 'admin'"), "Missing admin-tab conditional asset wiring: " + base);
		assertFalse(base.contains("pagesJsUrl"), "juneau-pages.js must not be included: " + base);
		assertFalse(base.contains("juneau-pages.js"), "juneau-pages.js must not be referenced: " + base);
		assertTrue(base.contains("regionsJsUrl"), "Missing juneau-regions.js include: " + base);
		assertTrue(base.contains("helpersJsUrl"), "Missing juneau-helpers.js include: " + base);
		assertTrue(base.contains("data-juneau-csrf="), "Missing CSRF ancestor stamp: " + base);
		assertTrue(base.contains("data-juneau-csrf-header="), "Missing CSRF header stamp: " + base);
		assertTrue(base.contains("configJsUrl"), "Missing juneau-config.js include: " + base);
		assertTrue(base.contains("configCssUrl"), "Missing juneau-config.css include: " + base);
		var viewsJsIdx = base.indexOf("viewsJsUrl");
		var configJsIdx = base.indexOf("configJsUrl");
		var regionsJsIdx = base.indexOf("regionsJsUrl");
		var helpersJsIdx = base.indexOf("helpersJsUrl");
		assertTrue(viewsJsIdx >= 0 && configJsIdx > viewsJsIdx,
			"juneau-config.js must load after juneau-views.js: " + base);
		assertTrue(regionsJsIdx > configJsIdx,
			"juneau-regions.js must load after juneau-config.js: " + base);
		assertTrue(helpersJsIdx > regionsJsIdx,
			"juneau-helpers.js must load after juneau-regions.js: " + base);
	}

	/**
	 * Regression: {@code juneau-icons.js} was never included on the page (only renders/ribbon/views were), so the
	 * icon registry was absent at ribbon-build time and every ribbon/paging-pill button fell back to rendering its
	 * label as plain text instead of a glyph. Asserts the include exists AND is ordered before {@code ribbonJsUrl}
	 * (the ribbon/pill buttons resolve their icons from the registry when they're built, so it must already exist).
	 */
	@Test
	void c03_baseTemplateIncludesIconsJsBeforeRibbonJs() throws IOException {
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

	@Test
	void c04_newFixturesContainNoSalesforceTokens() throws IOException {
		String js;
		try (var in = AdminRestTest.class.getResourceAsStream("/static/js/table-slot.js")) {
			assertNotNull(in, "table-slot.js not found on the test classpath");
			js = new String(IoUtils.readBytes(in), StandardCharsets.UTF_8);
		}
		assertFalse(js.contains("slds-"), js);
		assertFalse(js.toLowerCase().contains("salesforce"), js);
	}

	private static void assertAdminShell(String body, String slotId, String tableUrl) {
		assertTrue(body.contains("class=\"juneau-page-nav\""), "Missing page nav: " + body);
		assertTrue(body.contains("aria-label=\"Admin\""), body);
		assertTrue(body.contains("id=\"" + slotId + "\""), "Missing empty slot: " + body);
		assertTrue(body.contains("id=\"rm-table-slot\""), body);
		assertTrue(body.contains("\"tableUrl\":\"" + tableUrl + "\""), body);
		assertFalse(body.contains("data-juneau-page"), "Page sidecar leaked: " + body);
		assertFalse(body.contains("juneau-page:admin"), "PAGE_META leaked: " + body);
		assertFalse(body.contains(PageTable.TAB_BAR_CLASS), "Tab bar class leaked: " + body);
		assertFalse(body.contains("juneau-pages.js"), "juneau-pages.js leaked: " + body);
		assertFalse(body.contains("data-juneau-region"), "Do not stamp data-juneau-region on the table slot: " + body);
		assertTrue(body.contains("juneau-regions.js"), "Missing regions runtime: " + body);
		assertTrue(body.contains("/js/table-slot.js"), body);
		assertFalse(body.contains("slds-"), body);
	}
}
