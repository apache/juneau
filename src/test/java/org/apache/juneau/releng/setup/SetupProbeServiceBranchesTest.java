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
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.juneau.commons.secret.InMemorySecretStore;
import org.apache.juneau.commons.secret.SecretStore;
import org.apache.juneau.releng.credential.AccountStore;
import org.apache.juneau.releng.credential.CredentialService;
import org.apache.juneau.releng.credential.CredentialSpec;
import org.apache.juneau.releng.credential.Validator;
import org.apache.juneau.releng.util.ProcessRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Failure, no-package-manager, and credential-state branches of {@link SetupProbeService} that the
 * happy-path {@link SetupProbeServiceTest} does not reach.
 */
class SetupProbeServiceBranchesTest {

	private static final class Runner implements ProcessRunner {
		final Set<String> onPath = new HashSet<>();
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
			if ("which".equals(command.get(0)))
				return new ProcResult(onPath.contains(command.get(1)) ? 0 : 1, "");
			return installResult;
		}

		@Override
		public ProcResult run(List<String> command, String stdin, Map<String, String> env, Duration timeout) {
			return run(command, stdin, env);
		}
	}

	private final Runner runner = new Runner();
	private CredentialService creds;

	private SetupProbeService service(Path tmp, Path repo, Path settings, Validator ok) {
		var stores = new EnumMap<CredentialSpec, SecretStore>(CredentialSpec.class);
		var validators = new EnumMap<CredentialSpec, Validator>(CredentialSpec.class);
		for (var spec : CredentialSpec.values()) {
			stores.put(spec, new InMemorySecretStore());
			validators.put(spec, ok);
		}
		creds = new CredentialService(stores, validators, new AccountStore(tmp.resolve("state")));
		return new SetupProbeService(runner, creds, repo, settings);
	}

	private static SetupProbeService.Probe byId(SetupProbeService.SetupData data, String id) {
		return data.probes.stream().filter(p -> id.equals(p.id)).findFirst().orElseThrow();
	}

	@Test
	void a01_checkoutFailureModes(@TempDir Path tmp) throws Exception {
		var repo = tmp.resolve("juneau");
		var svc = service(tmp, repo, tmp.resolve("settings.xml"), (s, a) -> Validator.ValidationResult.ok("fine"));
		var p = byId(svc.data(), "juneau-checkout");
		assertEquals("fail", p.status);
		assertTrue(p.message.startsWith("No directory"), p.message);
		assertTrue(p.copyCommand.startsWith("git clone "), p.copyCommand);

		Files.createDirectories(repo);
		p = byId(svc.data(), "juneau-checkout");
		assertTrue(p.message.startsWith("Not a git checkout"), p.message);

		Files.createDirectories(repo.resolve(".git"));
		p = byId(svc.data(), "juneau-checkout");
		assertTrue(p.message.startsWith("Missing pom.xml"), p.message);

		Files.writeString(repo.resolve("pom.xml"), "<project/>");
		p = byId(svc.data(), "juneau-checkout");
		assertEquals("pass", p.status);
	}

	@Test
	void a02_noPackageManagerMeansToolsAreNotInstallable(@TempDir Path tmp) {
		var svc = service(tmp, tmp, tmp.resolve("settings.xml"), (s, a) -> Validator.ValidationResult.ok("fine"));
		var data = svc.data();
		assertNull(data.packageManager);
		var mvn = byId(data, "mvn");
		assertEquals("fail", mvn.status);
		assertFalse(mvn.installable);
		assertTrue(mvn.copyCommand.contains("brew install maven") && mvn.copyCommand.contains("apt-get install -y maven"),
			mvn.copyCommand);
		assertEquals("Install this tool and ensure it is on PATH.", mvn.nextStep);
		// missing settings.xml file is reported as a failed settings probe
		assertEquals("fail", byId(data, "settings-xml").status);
	}

	@Test
	void b01_installWithoutPackageManagerReportsIt(@TempDir Path tmp) {
		var svc = service(tmp, tmp, tmp.resolve("settings.xml"), (s, a) -> Validator.ValidationResult.ok("fine"));
		var r = svc.install("git");
		assertFalse(r.ok);
		assertTrue(r.output.contains("No package manager"), r.output);
		assertEquals("git", r.probe.id);
	}

	@Test
	void b02_failedBrewInstallKeepsOutputAndReEvaluates(@TempDir Path tmp) {
		runner.onPath.add("brew");
		runner.installResult = new ProcessRunner.ProcResult(1, "Error: no such formula");
		var svc = service(tmp, tmp, tmp.resolve("settings.xml"), (s, a) -> Validator.ValidationResult.ok("fine"));
		var r = svc.install("gh");
		assertFalse(r.ok);
		assertEquals("Error: no such formula", r.output);
		assertEquals("fail", r.probe.status);
	}

	@Test
	void b03_nullInstallOutputBecomesEmpty(@TempDir Path tmp) {
		runner.onPath.add("brew");
		runner.installResult = new ProcessRunner.ProcResult(0, null);
		var svc = service(tmp, tmp, tmp.resolve("settings.xml"), (s, a) -> Validator.ValidationResult.ok("fine"));
		assertEquals("", svc.install("gh").output);
	}

	@Test
	void b04_aptNeedsRootGetsExplanatoryNote(@TempDir Path tmp) {
		runner.onPath.add("apt-get");
		var svc = service(tmp, tmp, tmp.resolve("settings.xml"), (s, a) -> Validator.ValidationResult.ok("fine"));
		for (var phrase : new String[] { "Permission denied", "are you root?", "need superuser", "run with sudo" }) {
			runner.installResult = new ProcessRunner.ProcResult(100, "E: " + phrase);
			var r = svc.install("git");
			assertFalse(r.ok);
			assertTrue(r.output.contains("apt-get needs root/sudo"), phrase + " -> " + r.output);
		}
		runner.installResult = new ProcessRunner.ProcResult(100, "E: unable to locate package");
		assertFalse(svc.install("git").output.contains("needs root"));
		runner.installResult = new ProcessRunner.ProcResult(100, null);
		assertEquals("", svc.install("git").output);
	}

	@Test
	void c01_credentialStates(@TempDir Path tmp) {
		var svc = service(tmp, tmp, tmp.resolve("settings.xml"),
			(secret, acct) -> "bad".equals(secret) ? Validator.ValidationResult.fail("nope")
				: Validator.ValidationResult.ok("yes"));

		var p = byId(svc.data(), "apache-ldap");
		assertEquals("fail", p.status);
		assertEquals("apache-ldap is not set", p.message);

		creds.store("apache", "me", "good");
		p = byId(svc.data(), "apache-ldap");
		assertEquals("warn", p.status);
		assertEquals("Stored — not tested", p.message);

		creds.validate("apache");
		p = byId(svc.data(), "apache-ldap");
		assertEquals("pass", p.status);
		assertEquals("apache-ldap is valid", p.message);

		creds.store("apache", "me", "bad");
		creds.validate("apache");
		p = byId(svc.data(), "apache-ldap");
		assertEquals("fail", p.status);
		assertEquals("apache-ldap failed validation", p.message);
	}
}
