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
	void a01_pageRendersProbePillsWithoutSldsOrSsc() throws Exception {
		try (var client = client()) {
			try (var resp = client.request("GET", "/").run()) {
				assertEquals(200, resp.getStatusCode());
				var body = resp.getBodyAsString();
				assertTrue(body.contains("Probes"), body);
				assertTrue(body.contains("Details"), body);
				assertTrue(body.contains("data-probe-id=\"checkout\""), body);
				assertTrue(body.contains("data-probe-id=\"github\""), body);
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
				assertTrue(body.contains("\"id\":\"checkout\""), body);
				assertTrue(body.contains("\"id\":\"gpg-key\""), body);
			}
		}
	}

	@Test
	void a03_installUnknownIs404() throws Exception {
		try (var client = client()) {
			try (var resp = client.request("POST", "/install/checkout").run()) {
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
