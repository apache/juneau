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
import java.util.List;
import java.util.Map;

import org.apache.juneau.commons.secret.InMemorySecretStore;
import org.apache.juneau.commons.secret.SecretStore;
import org.apache.juneau.releng.credential.AccountStore;
import org.apache.juneau.releng.credential.CredentialService;
import org.apache.juneau.releng.credential.CredentialSpec;
import org.apache.juneau.releng.credential.Validator;
import org.apache.juneau.releng.milestone.GithubPrSource;
import org.apache.juneau.releng.milestone.MilestoneService;
import org.apache.juneau.releng.setup.SetupProbeService;
import org.apache.juneau.releng.util.ProcessRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Direct (no HTTP) tests of the small REST resources: {@link CredentialRest}, {@link MilestoneRest} and
 * {@link SetupRest}.
 */
class SmallRestResourcesTest {

	private static ProcessRunner runner(java.util.function.Function<List<String>, String> text,
			java.util.List<List<String>> lines, java.util.function.Function<List<String>, ProcessRunner.ProcResult> run) {
		return new ProcessRunner() {
			@Override
			public List<String> runLines(List<String> command) {
				return lines.isEmpty() ? List.of() : lines.get(0);
			}

			@Override
			public String runText(List<String> command) {
				return text.apply(command);
			}

			@Override
			public ProcResult run(List<String> command, String stdin, Map<String, String> env) {
				return run.apply(command);
			}
		};
	}

	@Test
	void a01_credentialSetValidateRemove(@TempDir Path tmp) {
		var stores = new EnumMap<CredentialSpec, SecretStore>(CredentialSpec.class);
		var validators = new EnumMap<CredentialSpec, Validator>(CredentialSpec.class);
		for (var spec : CredentialSpec.values()) {
			stores.put(spec, new InMemorySecretStore());
			validators.put(spec, (secret, acct) -> Validator.ValidationResult.ok("ok:" + acct));
		}
		var rest = new CredentialRest(new CredentialService(stores, validators, new AccountStore(tmp)));

		var body = new CredentialRest.StoreRequest();
		body.account = "me";
		body.secret = "pw";
		var st = rest.set("apache", body);
		assertEquals("apache", st.name);
		assertTrue(st.present);

		var v = rest.validate("apache");
		assertTrue(v.valid());
		assertEquals("ok:me", v.message());

		var removed = rest.remove("apache");
		assertEquals("apache", removed.name);
		assertFalse(removed.present);
	}

	@Test
	void b01_milestoneChangelogPrsAndPreviousTag() {
		var prJson = "[{\"number\":7,\"title\":\"Bump foo from 1.0 to 2.0\",\"author\":{\"login\":\"jb\"}},{\"number\":8,\"title\":\"No author\"}]";
		var calls = new java.util.ArrayList<List<String>>();
		var r = runner(cmd -> {
			calls.add(cmd);
			return prJson;
		}, List.of(List.of("juneau-9.1.0", "juneau-9.2.0")), c -> new ProcessRunner.ProcResult(0, ""));
		var rest = new MilestoneRest(new MilestoneService(), new GithubPrSource(r, "apache/juneau"), r, "/repo");

		var prs = rest.prs("9.3.0");
		assertEquals(List.of("gh", "pr", "list"), calls.get(0).subList(0, 3));
		assertEquals(2, prs.size());
		assertEquals(7, prs.get(0).number);
		assertEquals("jb", prs.get(0).authorLogin);
		assertEquals("", prs.get(1).authorLogin);
		assertTrue(rest.changelog("9.3.0").contains("foo"));
		assertEquals("9.2.0", rest.previousTag("9.3.0").replace("juneau-", ""));
	}

	@Test
	void b02_previousTagNoneYieldsPlaceholder() {
		var r = runner(c -> "", List.of(List.of()), c -> new ProcessRunner.ProcResult(0, ""));
		var rest = new MilestoneRest(new MilestoneService(), new GithubPrSource(r, "a/b"), r, "/repo");
		assertEquals("(none)", rest.previousTag("9.3.0"));
	}

	@Test
	void c01_setupInstallDelegatesToService(@TempDir Path tmp) {
		var stores = new EnumMap<CredentialSpec, SecretStore>(CredentialSpec.class);
		for (var spec : CredentialSpec.values())
			stores.put(spec, new InMemorySecretStore());
		var creds = new CredentialService(stores, new EnumMap<>(CredentialSpec.class), new AccountStore(tmp));
		// "which" always fails, so no package manager is found.
		var r = runner(c -> "", List.of(), c -> new ProcessRunner.ProcResult(1, ""));
		var rest = new SetupRest(new SetupProbeService(r, creds, tmp, tmp.resolve("settings.xml")));
		var result = rest.install("git");
		assertFalse(result.ok);
		assertTrue(result.output.contains("No package manager"));
	}
}
