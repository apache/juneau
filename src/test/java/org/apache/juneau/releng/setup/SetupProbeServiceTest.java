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

package org.apache.juneau.releng.setup;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.juneau.commons.secret.InMemorySecretStore;
import org.apache.juneau.commons.secret.SecretStore;
import org.apache.juneau.http.response.NotFound;
import org.apache.juneau.releng.credential.AccountStore;
import org.apache.juneau.releng.credential.CredentialService;
import org.apache.juneau.releng.credential.CredentialSpec;
import org.apache.juneau.releng.util.ProcessRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SetupProbeServiceTest {

	private FakeRunner runner;
	private SetupProbeService setup;
	private Path repo;
	private Path settings;

	@BeforeEach
	void setUp(@TempDir Path tmp) throws Exception {
		repo = tmp.resolve("juneau");
		Files.createDirectories(repo.resolve(".git"));
		Files.writeString(repo.resolve("pom.xml"), "<project><artifactId>juneau</artifactId></project>");
		settings = tmp.resolve("settings.xml");
		Files.writeString(settings, """
				<settings>
				  <servers>
				    <server>
				      <id>apache.releases.https</id>
				      <username>me</username>
				      <password>x</password>
				    </server>
				  </servers>
				</settings>
				""");
		runner = new FakeRunner();
		runner.onPath.add("mvn");
		runner.onPath.add("git");
		runner.onPath.add("gh");
		runner.onPath.add("gpg");
		runner.onPath.add("brew");
		var stores = new EnumMap<CredentialSpec, SecretStore>(CredentialSpec.class);
		for (var spec : CredentialSpec.values())
			stores.put(spec, new InMemorySecretStore());
		var creds = new CredentialService(stores, new EnumMap<>(CredentialSpec.class), new AccountStore(tmp.resolve("state")));
		setup = new SetupProbeService(runner, creds, repo, settings);
	}

	@Test
	void a01_inventoryHasPrereqsThenCredentials() {
		var ids = setup.inventory().stream().map(p -> p.id).toList();
		assertEquals(List.of("checkout", "mvn", "git", "gh", "gpg", "settings-xml", "apache", "gpg-key", "github"), ids);
		assertEquals("pending", setup.inventory().get(0).status);
	}

	@Test
	void a02_dataEagerPassWhenToolsAndCheckoutPresent() {
		var data = setup.data();
		assertEquals("brew", data.packageManager);
		assertEquals("pass", byId(data, "checkout").status);
		assertEquals("pass", byId(data, "mvn").status);
		assertEquals("pass", byId(data, "settings-xml").status);
		assertEquals("fail", byId(data, "apache").status);
		assertTrue(byId(data, "mvn").installable);
		assertFalse(byId(data, "checkout").installable);
		assertEquals("apache", byId(data, "apache").credentialName);
		assertEquals("gpg", byId(data, "gpg-key").credentialName);
	}

	@Test
	void a03_settingsFailIsMissingServerIdNotMereFile() throws Exception {
		Files.writeString(settings, "<settings><servers></servers></settings>");
		var p = byId(setup.data(), "settings-xml");
		assertEquals("fail", p.status);
		assertTrue(p.message.contains("apache.releases.https"), p.message);
	}

	@Test
	void a04_preferBrewWhenAptAlsoPresent() {
		runner.onPath.add("apt-get");
		assertEquals("brew", setup.data().packageManager);
	}

	@Test
	void a05_aptWhenNoBrew() {
		runner.onPath.remove("brew");
		runner.onPath.add("apt-get");
		var data = setup.data();
		assertEquals("apt", data.packageManager);
		assertTrue(byId(data, "git").copyCommand.contains("apt-get"));
	}

	@Test
	void a06_installBrewUsesTimeoutAndClosedStdinPath() {
		var r = setup.install("mvn");
		assertTrue(r.ok);
		assertTrue(runner.commands.contains(List.of("brew", "install", "maven")), runner.commands.toString());
		assertEquals(Duration.ofMinutes(10), runner.lastTimeout);
	}

	@Test
	void a07_installAptIsNonInteractive() {
		runner.onPath.remove("brew");
		runner.onPath.add("apt-get");
		setup.install("gpg");
		assertTrue(runner.commands.contains(List.of("apt-get", "install", "-y", "gnupg")), runner.commands.toString());
		assertEquals("noninteractive", runner.lastEnv.get("DEBIAN_FRONTEND"));
	}

	@Test
	void a08_installUnknownProbeIs404() {
		assertThrows(NotFound.class, () -> setup.install("checkout"));
		assertThrows(NotFound.class, () -> setup.install("settings-xml"));
		assertThrows(NotFound.class, () -> setup.install("nope"));
	}

	private static SetupProbeService.Probe byId(SetupProbeService.SetupData data, String id) {
		return data.probes.stream().filter(p -> id.equals(p.id)).findFirst().orElseThrow();
	}

	private static final class FakeRunner implements ProcessRunner {
		final Set<String> onPath = new HashSet<>();
		final List<List<String>> commands = new ArrayList<>();
		Map<String, String> lastEnv = Map.of();
		Duration lastTimeout;
		ProcResult installResult = new ProcResult(0, "ok");

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
			commands.add(List.copyOf(command));
			if (env != null && !command.isEmpty() && !"which".equals(command.get(0)))
				lastEnv = env;
			if (!command.isEmpty() && "which".equals(command.get(0)))
				return new ProcResult(onPath.contains(command.get(1)) ? 0 : 1, "");
			return installResult;
		}

		@Override
		public ProcResult run(List<String> command, String stdin, Map<String, String> env, Duration timeout) {
			lastTimeout = timeout;
			return run(command, stdin, env);
		}

		@Override
		public ProcResult run(List<String> command, String stdin, Map<String, String> env, Consumer<String> lineSink) {
			return run(command, stdin, env);
		}
	}
}
