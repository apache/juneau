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

package org.apache.juneau.releng.engine.steps;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.juneau.releng.config.TargetProfile;
import org.apache.juneau.releng.engine.ExecutionMode;
import org.apache.juneau.releng.engine.RunState;
import org.apache.juneau.releng.engine.StepContext;
import org.apache.juneau.releng.util.ProcessRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** §5.12/§7.1: full svn+wget+rename+gpg+add+commit sequence, matching juneau-release.sh:182-209. */
class BinaryArtifactsStageStepTest {

	private final List<List<String>> calls = new ArrayList<>();
	private final List<String> logLines = new ArrayList<>();

	private ProcessRunner recordingRunner() {
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
				calls.add(c);
				if (c.contains("gpg"))
					return new ProcResult(0, "SHA512(apache-juneau-9.2.1-src.zip)= abc123");
				return new ProcResult(0, "");
			}

			@Override
			public ProcResult run(List<String> c, String s, Map<String, String> e,
					java.util.function.Consumer<String> k) {
				return run(c, s, e);
			}
		};
	}

	private StepContext ctx(ExecutionMode mode, Path stateDir) {
		var c = new StepContext();
		c.mode = mode;
		c.run = RunState.create("9.2.1", "juneau-9.2.1-branch", List.of("binary-artifacts-stage"));
		c.run.nexusRepoId = "orgapachejuneau-1042";
		c.runner = recordingRunner();
		c.target = TargetProfile.prodDefault();
		c.stateDir = stateDir;
		c.availid = "jdoe";
		c.ldapPassword = "s3cr3t";
		c.formInputs = Map.of();
		c.log = logLines::add;
		return c;
	}

	@Test
	void liveBuildsCanonicalSvnWgetRenameGpgAddCommitSequence(@TempDir Path dir) {
		var c = ctx(ExecutionMode.LIVE, dir);
		var res = new BinaryArtifactsStageStep().apply(c);
		assertTrue(res.success, res.message);

		var dist = dir.resolve("dist");
		var rc = "juneau-9.2.1-RC1";

		var checkout = calls.stream().filter(x -> x.contains("checkout")).findFirst().orElseThrow();
		assertTrue(checkout.contains("https://dist.apache.org/repos/dist/dev/juneau"));
		assertTrue(checkout.contains("--username"));
		assertTrue(checkout.contains("jdoe"));
		assertTrue(checkout.contains("--password-from-stdin"));
		assertFalse(checkout.contains("s3cr3t"), "the password must never be on argv");

		assertTrue(calls.stream().anyMatch(x -> x.equals(List.of("svn", "rm", dist + "/source/*"))));
		assertTrue(calls.stream().anyMatch(x -> x.equals(List.of("svn", "rm", dist + "/binaries/*"))));
		assertTrue(calls.stream().anyMatch(x -> x.equals(List.of("mkdir", dist + "/source/" + rc))));
		assertTrue(calls.stream().anyMatch(x -> x.equals(List.of("mkdir", dist + "/binaries/" + rc))));

		var srcWget = calls.stream().filter(x -> x.contains("wget") && x.contains("*-source-release*")).findFirst()
				.orElseThrow();
		assertTrue(srcWget.contains(
				"https://repository.apache.org/content/repositories/orgapachejuneau-1042/org/apache/juneau/"));
		assertTrue(srcWget.contains(dist + "/source/" + rc));
		var binWget = calls.stream().filter(x -> x.contains("wget") && x.contains("juneau-distrib*-bin.zip*"))
				.findFirst().orElseThrow();
		assertTrue(binWget.contains(dist + "/binaries/" + rc));

		assertTrue(calls.stream()
				.anyMatch(x -> x.equals(List.of("mv", dist + "/source/" + rc + "/juneau-9.2.1-source-release.zip",
						dist + "/source/" + rc + "/apache-juneau-9.2.1-src.zip"))));
		assertTrue(calls.stream()
				.anyMatch(x -> x.equals(List.of("mv", dist + "/binaries/" + rc + "/juneau-distrib-9.2.1-bin.zip",
						dist + "/binaries/" + rc + "/apache-juneau-9.2.1-bin.zip"))));

		var gpgCalls = calls.stream().filter(x -> x.contains("gpg")).toList();
		assertEquals(2, gpgCalls.size());
		assertEquals(List.of("gpg", "--print-md", "SHA512", dist + "/source/" + rc + "/apache-juneau-9.2.1-src.zip"),
				gpgCalls.get(0));

		// gpg --print-md SHA512 output is written to the .sha512 file (ProcessRunner has no shell redirect).
		var shaFile = dist.resolve("source").resolve(rc).resolve("apache-juneau-9.2.1-src.zip.sha512");
		assertTrue(Files.isRegularFile(shaFile));

		assertTrue(calls.stream().anyMatch(x -> x.equals(List.of("rm", dist + "/source/" + rc + "/*.sha1"))));
		assertTrue(calls.stream().anyMatch(x -> x.equals(List.of("rm", dist + "/source/" + rc + "/*.md5"))));

		assertTrue(calls.stream().anyMatch(x -> x.equals(List.of("svn", "add", dist + "/source/" + rc))));
		assertTrue(calls.stream().anyMatch(x -> x.equals(List.of("svn", "add", dist + "/binaries/" + rc))));

		var commit = calls.stream().filter(x -> x.contains("commit")).findFirst().orElseThrow();
		assertTrue(commit.contains(rc)); // svn commit -m "<RC>"
	}

	@Test
	void safeSpawnsNothingAndLogsWouldRunForEveryMutation(@TempDir Path dir) {
		var c = ctx(ExecutionMode.SAFE, dir);
		var res = new BinaryArtifactsStageStep().apply(c);
		assertTrue(res.success, res.message);
		assertEquals(0, calls.size(), "no real subprocess may be spawned in SAFE");

		var wouldRun = logLines.stream().filter(l -> l.startsWith("would run:")).toList();
		assertTrue(wouldRun.stream().anyMatch(l -> l.contains("svn") && l.contains("checkout")));
		assertTrue(wouldRun.stream().anyMatch(l -> l.contains("wget") && l.contains("*-source-release*")));
		assertTrue(wouldRun.stream().anyMatch(l -> l.contains("gpg") && l.contains("SHA512")));
		assertTrue(wouldRun.stream().anyMatch(l -> l.contains("svn") && l.contains("add")));
		assertTrue(wouldRun.stream().anyMatch(l -> l.contains("svn") && l.contains("commit")));
		assertTrue(logLines.stream().noneMatch(l -> l.contains("s3cr3t")), "the password must never be logged");

		// SAFE never really staged anything, so the .sha512 file is not written to disk.
		assertFalse(Files.exists(dir.resolve("dist")));
	}
}
