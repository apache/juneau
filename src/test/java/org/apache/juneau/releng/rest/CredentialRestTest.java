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

import java.nio.file.Path;
import java.util.EnumMap;

import org.apache.juneau.commons.inject.StackOverlay;
import org.apache.juneau.commons.secret.InMemorySecretStore;
import org.apache.juneau.commons.secret.SecretStore;
import org.apache.juneau.releng.credential.AccountStore;
import org.apache.juneau.releng.credential.CredentialService;
import org.apache.juneau.releng.credential.CredentialSpec;
import org.apache.juneau.rest.mock.MockRestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Credentials envelope JSON lives on this resource without {@code ViewsMixin}; the human page stays the secret-store UI.
 */
class CredentialRestTest {

	private CredentialService service;

	@BeforeEach
	void setUp(@TempDir Path stateDir) {
		var stores = new EnumMap<CredentialSpec,SecretStore>(CredentialSpec.class);
		for (var spec : CredentialSpec.values())
			stores.put(spec, new InMemorySecretStore());
		service = new CredentialService(stores, new EnumMap<>(CredentialSpec.class), new AccountStore(stateDir));
	}

	@SuppressWarnings({
		"resource" // Caller owns and closes the returned MockRestClient (via try-with-resources).
	})
	private MockRestClient client() {
		return MockRestClient.builder(new CredentialRest(service)).overridingBeanStore(new StackOverlay()).build();
	}

	@Test
	void a01_viewEnvelopeIsSlotMetaWithoutCsrf() throws Exception {
		try (var client = client()) {
			try (var resp = client.request("GET", "/view").header("Accept", "application/json").run()) {
				assertEquals(200, resp.getStatusCode());
				ReleaseRestTest.assertSlotEnvelope(resp.getBodyAsString(), "credentials");
			}
		}
	}

	@Test
	void a02_pageRemainsTheSecretStoreUi() throws Exception {
		try (var client = client()) {
			try (var resp = client.request("GET", "/").run()) {
				assertEquals(200, resp.getStatusCode());
				var body = resp.getBodyAsString();
				assertTrue(body.contains("/js/credentials.js"), body);
				assertFalse(body.contains("class=\"juneau-page-nav\""), body);
				assertFalse(body.contains("id=\"rm-table-slot\""), body);
				assertFalse(body.contains("juneau-view:credentials"), body);
			}
		}
	}
}
