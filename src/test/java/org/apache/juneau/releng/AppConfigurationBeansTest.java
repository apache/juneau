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
import org.apache.juneau.releng.credential.CredentialSpec;
import org.apache.juneau.releng.run.RunFiles;
import org.apache.juneau.releng.util.DefaultProcessRunner;
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
	void b01_simpleResourceBeans() {
		var runner = cfg.processRunner();
		assertNotNull(cfg.rootRest());
		assertNotNull(cfg.homeRest());
		assertNotNull(cfg.milestoneService());
		assertNotNull(cfg.githubPrSource(runner, "apache/juneau"));
		assertNotNull(cfg.targetProfile());
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
		var svc = cfg.releaseListService(runner, dir.toString(), "apache/juneau", new RunFiles(dir, p -> false));
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
	void e01_releaseTabWiring(@TempDir Path dir) throws Exception {
		var answers = cfg.credentialEnv(mapStores(), cfg.accountStore(dir.toString()));
		var events = cfg.runEvents();
		var previousScript = System.getProperty("juneau.terminal.script");
		var processes = cfg.releaseProcessService(dir.toString(), dir.toString(), "/nonexistent/juneau_terminal.py", answers, events);
		try {
			assertEquals("/nonexistent/juneau_terminal.py", System.getProperty("juneau.terminal.script"));
			var runFiles = cfg.runFiles(dir.toString(), new org.springframework.beans.factory.support.StaticListableBeanFactory(
				java.util.Map.of("p", processes)).getBeanProvider(org.apache.juneau.releng.run.ReleaseProcessService.class));
			assertNotNull(cfg.releaseRunRest(runFiles, processes));
			assertEquals(java.util.List.of("/events/*"), new java.util.ArrayList<>(cfg.runEventsRegistration(events).getUrlMappings()));
			try (var w = cfg.runFileWatcher(dir.toString(), events)) {
				assertNotNull(w);
			}
		} finally {
			if (previousScript == null)
				System.clearProperty("juneau.terminal.script");
			else
				System.setProperty("juneau.terminal.script", previousScript);
			processes.shutdown();
		}
	}
}
