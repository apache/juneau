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

import static org.apache.juneau.rest.server.console.test.PageContractAssert.assertPage;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.apache.juneau.commons.inject.StackOverlay;
import org.apache.juneau.commons.secret.InMemorySecretStore;
import org.apache.juneau.commons.secret.SecretStore;
import org.apache.juneau.commons.utils.IoUtils;
import org.apache.juneau.releng.credential.AccountStore;
import org.apache.juneau.releng.credential.CredentialService;
import org.apache.juneau.releng.credential.CredentialSpec;
import org.apache.juneau.releng.setup.SetupProbeService;
import org.apache.juneau.releng.util.ProcessRunner;
import org.apache.juneau.rest.mock.MockRestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SetupRestTest {

	private SetupProbeService service;

	private static int count(String body, String needle) {
		var n = 0;
		for (var i = body.indexOf(needle); i >= 0; i = body.indexOf(needle, i + needle.length()))
			n++;
		return n;
	}

	@BeforeEach
	void setUp(@TempDir Path tmp) {
		var stores = new EnumMap<CredentialSpec, SecretStore>(CredentialSpec.class);
		for (var spec : CredentialSpec.values())
			stores.put(spec, new InMemorySecretStore());
		var creds = new CredentialService(stores, new EnumMap<>(CredentialSpec.class), new AccountStore(tmp));
		service = new SetupProbeService(new FakeRunner(), creds, tmp.resolve("missing-repo"), tmp.resolve("missing-settings.xml"));
	}

	@SuppressWarnings({
		"resource" // Caller owns and closes the returned MockRestClient (via try-with-resources).
	})
	private MockRestClient client() {
		return MockRestClient.builder(new SetupRest(service)).overridingBeanStore(new StackOverlay()).build();
	}

	@Test
	void a01_pageRendersProbeChipsWithoutSldsOrSsc() throws Exception {
		try (var client = client()) {
			try (var resp = client.request("GET", "/").run()) {
				assertEquals(200, resp.getStatusCode());
				var body = resp.getBodyAsString();
				assertTrue(body.contains("Probes"), body);
				assertTrue(body.contains("Details"), body);
				assertTrue(body.contains("class=\"jc-page-header\""), body);
				assertTrue(body.contains("<h1>Setup</h1>"), body);
				assertTrue(body.contains("jc-page-sub"), body);
				assertTrue(body.contains("Every prerequisite this release manager needs"), body);
				var page = assertPage(body)
					.isValid()
					.hasActiveNav("setup")
					.hasNavHref("setup", "/rest/setup")
					.hasNavHref("releases", "/rest/releases")
					.hasNavHref("new", "/rest/runs")
					.hasNavChildren("new", "input", "exec")
					.hasFooterText("Apache Juneau Release Manager — loopback tool for cutting Apache Juneau releases.");
				assertEquals(3, page.contract().getList("nav").size(),
					() -> "Setup, Releases, and New Release must be the three sections: " + body);
				assertTrue(body.contains("juneau-views.css"),
					"Setup now pulls the views toolkit so the probes can adopt the Juneau probe helper: " + body);
				assertTrue(body.contains("/juneau-console/chrome.css"), body);
				assertTrue(body.contains("data-juneau-probe-group"), body);
				assertTrue(body.contains("class=\"jc-probe jc-probe-neutral\""), body);
				assertTrue(body.contains("data-juneau-probe=\"juneau-checkout\""), body);
				assertTrue(body.contains("data-juneau-probe=\"github-token\""), body);
				assertFalse(body.contains("rm-probe-pill"), "old pill markup must be gone: " + body);
				assertTrue(body.contains("/js/rm-setup.js"), body);
				assertFalse(body.contains("slds-"), body);
				assertFalse(body.contains("ssc-"), body);
				assertFalse(body.contains("Workflow"), body);
			}
		}
	}

	@Test
	void a02_dataReturnsJsonProbes() throws Exception {
		try (var client = client()) {
			try (var resp = client.request("GET", "/data").header("Accept", "application/json").run()) {
				assertEquals(200, resp.getStatusCode());
				var body = resp.getBodyAsString();
				assertTrue(body.contains("\"id\":\"juneau-checkout\""), body);
				assertTrue(body.contains("\"id\":\"gpg-signing-key\""), body);
			}
		}
	}

	@Test
	void a03_installUnknownIs404() throws Exception {
		try (var client = client()) {
			try (var resp = client.request("POST", "/install/juneau-checkout").run()) {
				assertEquals(404, resp.getStatusCode());
			}
		}
	}

	@Test
	void a04_staticAssetsContainNoSalesforceTokens() throws IOException {
		for (var path : List.of("/static/js/rm-setup.js", "/static/css/chrome.css", "/templates/setup.ftlh")) {
			try (var in = SetupRestTest.class.getResourceAsStream(path)) {
				assertNotNull(in, path);
				var text = new String(IoUtils.readBytes(in), StandardCharsets.UTF_8);
				assertFalse(text.contains("slds-"), path + " " + text);
				assertFalse(text.contains("ssc-"), path + " " + text);
				assertFalse(text.toLowerCase().contains("salesforce"), path);
				if (path.endsWith("rm-setup.js")) {
					assertTrue(text.contains("JuneauViews.init"),
						"probe helper is on JuneauViews.init, not JuneauViews: " + path);
					assertTrue(text.contains("juneau:probe-select"),
						"Details must listen for the helper's selection-change event: " + path);
					assertFalse(text.contains("views.enhanceProbeGroup"),
						"must not call enhanceProbeGroup on JuneauViews (undefined): " + path);
					assertFalse(text.contains("data-probe-id"),
						"old pill click path must stay gone: " + path);
				}
				if (path.endsWith("chrome.css")) {
					assertFalse(text.contains("max-width: 1180px"), "Setup grid must fill the well: " + path);
					assertFalse(text.contains("border-top-color: var(--jc-page-nav-accent)"),
						"JRM must not override Page Tab accent side; Juneau WORK-J0543 owns it: " + path);
					assertFalse(text.contains("border-bottom: var(--jc-nav-indicator-width) solid var(--jc-accent)"),
						"selected Page Tab must not carry a JRM thick bar: " + path);
					assertTrue(text.contains("font-weight: 700"), "selected Page Tab keeps heavier type: " + path);
					assertFalse(text.contains("th.sortable::after") || text.contains("th.sortable {"),
						"JRM must not fork header sort onto the whole th; Juneau WORK-J0547 owns it: " + path);
					assertFalse(text.contains(".dt-column-order:before") || text.contains(".dt-column-order:after"),
						"JRM must not restyle the order control; Juneau WORK-J0547 owns it: " + path);
					assertFalse(text.contains("div.dt-container .dt-search input"),
						"JRM must not restyle DT search; Juneau WORK-J0546 owns --jc-control-border: " + path);
					assertFalse(text.contains("\n.jc-card {"),
						"JRM must not fork content-card padding/shadow; Juneau WORK-J0544/J0545 own them: " + path);
				}
			}
		}
	}

	@Test
	void a05_newReleaseConsumesJuneauWellWithoutPageCanvas() throws IOException {
		for (var path : List.of("/templates/new-release.ftlh", "/static/css/new-release.css")) {
			try (var in = SetupRestTest.class.getResourceAsStream(path)) {
				assertNotNull(in, path);
				var text = new String(IoUtils.readBytes(in), StandardCharsets.UTF_8);
				assertFalse(text.contains("rm-page-canvas"), path);
				assertFalse(text.contains("slds-"), path);
				assertFalse(text.contains("ssc-"), path);
				if (path.endsWith("new-release.ftlh")) {
					assertTrue(text.contains("class=\"jc-page-header\""), text);
					assertTrue(text.contains("<h1>New Release</h1>"), text);
					assertTrue(text.contains("jc-page-sub"), text);
				}
			}
		}
	}

	@Test
	void a06_pageLinksTheLightRedStockThemeExactlyOnce() throws Exception {
		try (var client = client()) {
			try (var resp = client.request("GET", "/").run()) {
				assertEquals(200, resp.getStatusCode());
				var body = resp.getBodyAsString();
				assertEquals(1, count(body, "juneau-theme-light-red.css"), body);
				assertFalse(body.contains("juneau-theme-open.css"), body);
				assertTrue(body.contains("--jc-pill-red-bg:#fdeceb"), body);
			}
		}
	}

	private static final class FakeRunner implements ProcessRunner {
		@Override
		public List<String> runLines(List<String> command) {
			return List.of();
		}

		@Override
		public String runText(List<String> command) {
			return "";
		}

		@Override
		public ProcResult run(List<String> command, String stdin, Map<String, String> env) {
			return new ProcResult(1, "");
		}
	}
}
