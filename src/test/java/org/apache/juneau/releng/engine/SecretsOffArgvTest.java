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

package org.apache.juneau.releng.engine;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.juneau.releng.config.TargetProfile;
import org.apache.juneau.releng.engine.steps.BinaryArtifactsStageStep;
import org.apache.juneau.releng.engine.steps.DeploySnapshotStep;
import org.apache.juneau.releng.engine.steps.DistPromoteStep;
import org.apache.juneau.releng.engine.steps.GithubReleaseCreateStep;
import org.apache.juneau.releng.util.ProcessRunner;
import org.junit.jupiter.api.Test;

/** Asserts no secret is ever passed on a subprocess argv. */
class SecretsOffArgvTest {

	static final String LDAP = "SENTINEL_LDAP_PW";
	static final String GPG = "SENTINEL_GPG_PASSPHRASE";
	static final String GH = "SENTINEL_GH_TOKEN";

	private final List<List<String>> argvs = new ArrayList<>();
	private final List<String> stdins = new ArrayList<>();
	private final List<Map<String, String>> envs = new ArrayList<>();

	private ProcessRunner recorder() {
		return new ProcessRunner() {
			@Override
			public List<String> runLines(List<String> c) {
				return List.of();
			}

			@Override
			public String runText(List<String> c) {
				return "";
			}

			@Override
			public ProcResult run(List<String> c, String s, Map<String, String> e) {
				argvs.add(c);
				stdins.add(s);
				envs.add(e == null ? Map.of() : e);
				return new ProcResult(0, "ok");
			}

			@Override
			public ProcResult run(List<String> c, String s, Map<String, String> e,
					java.util.function.Consumer<String> k) {
				return run(c, s, e);
			}
		};
	}

	private StepContext ctx(ProcessRunner r, Path stateDir) {
		var c = new StepContext();
		c.mode = ExecutionMode.LIVE; // command-building + secret-routing is a LIVE concern; SAFE never spawns
		c.run = RunState.create("9.2.1", "juneau-9.2.1-branch", List.of("deploy-snapshot"));
		c.run.nexusRepoId = "orgapachejuneau-1042";
		c.runner = r;
		c.log = s -> {
		};
		c.target = TargetProfile.prodDefault();
		c.stateDir = stateDir;
		c.repoDir = "/repo";
		c.stagingRepo = Path.of("/staging/git/juneau");
		c.availid = "avail";
		c.ldapPassword = LDAP;
		c.gpgKeyId = "KEY";
		c.gpgPassphrase = GPG;
		c.githubToken = GH;
		c.formInputs = Map.of();
		return c;
	}

	@Test
	void noSecretEverAppearsInArgv(@org.junit.jupiter.api.io.TempDir Path dir) {
		var r = recorder();
		var c = ctx(r, dir);
		new DeploySnapshotStep().apply(c); // gpg passphrase must be on stdin
		new GithubReleaseCreateStep().apply(c); // token must be in env
		new BinaryArtifactsStageStep().apply(c); // LDAP password must be on stdin (§7.1 svn auth)
		new DistPromoteStep().apply(c); // LDAP password must be on stdin (§7.2 svn auth)

		for (var argv : argvs)
			for (var arg : argv) {
				assertFalse(arg.contains(LDAP), "LDAP password leaked into argv: " + argv);
				assertFalse(arg.contains(GPG), "GPG passphrase leaked into argv: " + argv);
				assertFalse(arg.contains(GH), "GitHub token leaked into argv: " + argv);
			}
		for (var stdin : stdins)
			if (stdin != null) {
				assertFalse(stdin.contains(GH), "GitHub token leaked into stdin: " + stdin);
			}
		// Positive: the GPG passphrase went via stdin at least once.
		assertTrue(stdins.stream().anyMatch(s -> s != null && s.contains(GPG)));
		// Positive: the LDAP password went via stdin at least once (the new svn checkout/commit calls).
		assertTrue(stdins.stream().anyMatch(s -> s != null && s.contains(LDAP)));
		// Positive: the GitHub token went via env at least once.
		assertTrue(envs.stream().anyMatch(e -> GH.equals(e.get("GH_TOKEN"))));
	}
}
