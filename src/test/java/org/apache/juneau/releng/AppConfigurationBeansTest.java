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

package org.apache.juneau.releng;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.juneau.commons.secret.SecretStore;
import org.apache.juneau.releng.config.TargetProfile;
import org.apache.juneau.releng.credential.CredentialSpec;
import org.apache.juneau.releng.engine.ReleaseEngine;
import org.apache.juneau.releng.util.DefaultProcessRunner;
import org.apache.juneau.releng.util.ProcessRunner;
import org.apache.juneau.rest.server.filter.SynchronizerToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.Ordered;

/**
 * Exercises the {@link AppConfiguration} bean factory methods directly (no Spring context, no Keychain, no network).
 */
class AppConfigurationBeansTest {

	private static final class MapStore implements SecretStore {
		final Map<String, char[]> data = new HashMap<>();

		@Override
		public void store(String key, char[] secret) {
			data.put(key, secret);
		}

		@Override
		public Optional<char[]> find(String key) {
			return Optional.ofNullable(data.get(key));
		}

		@Override
		public boolean exists(String key) {
			return data.containsKey(key);
		}

		@Override
		public boolean delete(String key) {
			return data.remove(key) != null;
		}
	}

	private final AppConfiguration cfg = new AppConfiguration();

	private static Map<CredentialSpec, SecretStore> mapStores() {
		var stores = new EnumMap<CredentialSpec, SecretStore>(CredentialSpec.class);
		for (var spec : CredentialSpec.values())
			stores.put(spec, new MapStore());
		return stores;
	}

	@Test
	void a01_processRunnerIsDefaultImplementation() {
		assertInstanceOf(DefaultProcessRunner.class, cfg.processRunner());
	}

	@Test
	void a02_loopbackBoundaryAndFilterRegistration() {
		var token = cfg.csrfToken();
		assertNotNull(token);
		var boundary = cfg.loopbackBoundary(token, "127.0.0.1", 8790);
		assertNotNull(boundary);
		var reg = cfg.loopbackBoundaryFilter(boundary);
		assertEquals(Ordered.HIGHEST_PRECEDENCE, reg.getOrder());
		assertTrue(reg.getUrlPatterns().contains("/*"));
		assertNotNull(reg.getFilter());
	}

	@Test
	void a03_csrfTokenIsMintedPerCall() {
		assertNotSame(cfg.csrfToken(), cfg.csrfToken());
		assertInstanceOf(SynchronizerToken.class, cfg.csrfToken());
	}

	@Test
	void b01_simpleResourceBeans(@TempDir Path dir) {
		var runner = cfg.processRunner();
		assertNotNull(cfg.rootRest());
		assertNotNull(cfg.homeRest());
		assertNotNull(cfg.milestoneService());
		assertNotNull(cfg.githubPrSource(runner, "apache/juneau"));
		assertNotNull(cfg.targetProfile());
		assertNotNull(cfg.branchResolver(runner, dir.toString()));
		assertNotNull(cfg.emailService(runner, dir.toString()));
		assertNotNull(cfg.stepRegistry(cfg.branchResolver(runner, dir.toString())));
	}

	@Test
	void b02_servletRegistrations() {
		var root = cfg.rootRest();
		var reg = cfg.rootRestRegistration(root);
		assertTrue(reg.getUrlMappings().contains("/rest/*"));
		assertSame(root, reg.getServlet());
		var assets = cfg.consoleAssetsRegistration();
		assertTrue(assets.getUrlMappings().contains("/juneau-console/*"));
		assertInstanceOf(ConsoleAssetsRest.class, assets.getServlet());
	}

	@Test
	void c01_credentialWiringAndRestResources(@TempDir Path dir) {
		var runner = cfg.processRunner();
		var accounts = cfg.accountStore(dir.toString());
		var creds = cfg.credentialService(mapStores(), runner, accounts);
		assertNotNull(creds);
		assertNotNull(cfg.credentialRest(creds));
		var setup = cfg.setupProbeService(runner, creds, dir.toString());
		assertNotNull(setup);
		assertNotNull(cfg.setupRest(setup));
	}

	@Test
	void c02_releaseAndMilestoneWiring(@TempDir Path dir) {
		var runner = cfg.processRunner();
		var store = cfg.runStateStore(dir.toString());
		var svc = cfg.releaseListService(runner, dir.toString(), "apache/juneau", store);
		assertNotNull(svc);
		assertNotNull(cfg.releaseRest(svc));
		assertNotNull(cfg.milestoneRest(cfg.milestoneService(), cfg.githubPrSource(runner, "apache/juneau"), runner,
			dir.toString()));
	}

	@Test
	void c03_credentialSecretStoresHaveOneEntryPerSpec() {
		var stores = cfg.credentialSecretStores();
		assertEquals(CredentialSpec.values().length, stores.size());
		for (var spec : CredentialSpec.values())
			assertNotNull(stores.get(spec));
	}

	@Test
	void d01_secretResolverReadsStoresAndAccounts(@TempDir Path dir) {
		var stores = mapStores();
		var accounts = cfg.accountStore(dir.toString());
		accounts.put(CredentialSpec.APACHE_LDAP, "jbognar");
		accounts.put(CredentialSpec.GPG, "ABCD1234");
		stores.get(CredentialSpec.APACHE_LDAP).store("jbognar", "ldap-pw".toCharArray());
		stores.get(CredentialSpec.GPG).store("ABCD1234", "gpg-pw".toCharArray());
		stores.get(CredentialSpec.GITHUB).store("token", "ghp".toCharArray());

		var resolver = cfg.secretResolver(stores, accounts, TargetProfile.prodDefault());
		assertEquals("jbognar", resolver.availid());
		assertEquals("ldap-pw", resolver.ldapPassword());
		assertEquals("ABCD1234", resolver.gpgKeyId());
		assertEquals("gpg-pw", resolver.gpgPassphrase());
		assertEquals("ghp", resolver.githubToken());
		// Both Keychain-style credentials present, so the live Nexus client is built without touching settings.xml.
		assertNotNull(resolver.nexus());
	}

	@Test
	void d02_secretResolverIsEmptyWhenNothingStored(@TempDir Path dir) {
		var resolver = cfg.secretResolver(mapStores(), cfg.accountStore(dir.toString()), TargetProfile.prodDefault());
		assertEquals("", resolver.availid());
		assertEquals("", resolver.ldapPassword());
		assertEquals("", resolver.gpgKeyId());
		assertEquals("", resolver.gpgPassphrase());
		assertEquals("", resolver.githubToken());
	}

	@Test
	void d03_secretResolverSystemPropertiesOverrideAccounts(@TempDir Path dir) {
		var oldId = System.getProperty("rm.availid");
		var oldKey = System.getProperty("rm.gpg.keyid");
		try {
			System.setProperty("rm.availid", "sysuser");
			System.setProperty("rm.gpg.keyid", "SYSKEY");
			var accounts = cfg.accountStore(dir.toString());
			accounts.put(CredentialSpec.APACHE_LDAP, "stored");
			accounts.put(CredentialSpec.GPG, "STOREDKEY");
			var resolver = cfg.secretResolver(mapStores(), accounts, TargetProfile.prodDefault());
			assertEquals("sysuser", resolver.availid());
			assertEquals("SYSKEY", resolver.gpgKeyId());
		} finally {
			restore("rm.availid", oldId);
			restore("rm.gpg.keyid", oldKey);
		}
	}

	private static void restore(String key, String value) {
		if (value == null)
			System.clearProperty(key);
		else
			System.setProperty(key, value);
	}

	@Test
	void e01_engineDropRcAndSseWiring(@TempDir Path dir) {
		var runner = cfg.processRunner();
		var state = dir.resolve("state").toString();
		var staging = dir.resolve("staging").toString();
		var store = cfg.runStateStore(state);
		var branches = cfg.branchResolver(runner, dir.toString());
		var registry = cfg.stepRegistry(branches);
		var secrets = new ReleaseEngine.SecretResolver() {
			@Override public String availid() { return "u"; }
			@Override public String ldapPassword() { return "p"; }
			@Override public String gpgKeyId() { return "k"; }
			@Override public String gpgPassphrase() { return "g"; }
			@Override public String githubToken() { return "t"; }
			@Override public org.apache.juneau.releng.nexus.NexusStagingClient nexus() {
				return org.apache.juneau.releng.nexus.NexusStagingClient.forTests((m, p, b) -> "");
			}
		};
		var target = cfg.targetProfile();
		var engine = cfg.releaseEngine(store, registry, runner, branches, cfg.emailService(runner, state),
			cfg.milestoneService(), secrets, target, state, staging, dir.toString(), "x@apache.org");
		assertNotNull(engine);
		var dropRc = cfg.dropRcService(store, registry, runner, secrets, engine, target, staging, state);
		assertNotNull(dropRc);
		assertNotNull(cfg.releaseRunRest(engine, dropRc));
		var sse = cfg.sseLogRegistration(engine, store);
		assertTrue(sse.getUrlMappings().contains("/events/*"));
	}
}
