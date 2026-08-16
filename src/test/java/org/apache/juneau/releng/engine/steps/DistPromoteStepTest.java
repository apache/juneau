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

/** §5.19/§7.2: svn mv dev->release, prior-release-on-line removal, matching the wiki's manual dev->release move. */
class DistPromoteStepTest {

	private final List<List<String>> calls = new ArrayList<>();
	private final List<String> logLines = new ArrayList<>();

	private ProcessRunner recordingRunner(List<String> tags) {
		return new ProcessRunner() {
			@Override
			public List<String> runLines(List<String> c) {
				return c.contains("tag") ? tags : List.of();
			}

			@Override
			public String runText(List<String> c) {
				return "";
			}

			@Override
			public ProcResult run(List<String> c, String s, Map<String, String> e) {
				calls.add(c);
				return new ProcResult(0, "");
			}

			@Override
			public ProcResult run(List<String> c, String s, Map<String, String> e,
					java.util.function.Consumer<String> k) {
				return run(c, s, e);
			}
		};
	}

	private StepContext ctx(ExecutionMode mode, Path stateDir, List<String> tags) {
		var c = new StepContext();
		c.mode = mode;
		c.run = RunState.create("9.2.1", "juneau-9.2.1-branch", List.of("dist-promote"));
		c.runner = recordingRunner(tags);
		c.target = TargetProfile.prodDefault();
		c.stateDir = stateDir;
		c.repoDir = "/repo";
		c.availid = "jdoe";
		c.ldapPassword = "s3cr3t";
		c.formInputs = Map.of();
		c.log = logLines::add;
		return c;
	}

	@Test
	void liveMovesArtifactsAndRemovesPriorReleaseOnSameLine(@TempDir Path dir) {
		var tags = List.of("juneau-9.2.0", "juneau-9.1.5", "juneau-9.2.1-RC1", "juneau-9.0.0");
		var c = ctx(ExecutionMode.LIVE, dir, tags);
		var res = new DistPromoteStep().apply(c);
		assertTrue(res.success, res.message);

		var dev = dir.resolve("dist-dev");
		var release = dir.resolve("dist-release");
		var rc = "juneau-9.2.1-RC1";

		assertTrue(calls.stream()
				.anyMatch(x -> x.contains("checkout") && x.contains("https://dist.apache.org/repos/dist/dev/juneau")));
		assertTrue(calls.stream().anyMatch(
				x -> x.contains("checkout") && x.contains("https://dist.apache.org/repos/dist/release/juneau")));

		for (var name : List.of("apache-juneau-9.2.1-src.zip", "apache-juneau-9.2.1-src.zip.asc",
				"apache-juneau-9.2.1-src.zip.sha512")) {
			assertTrue(
					calls.stream()
							.anyMatch(x -> x.equals(
									List.of("svn", "mv", dev.resolve("source").resolve(rc).resolve(name).toString(),
											release.resolve("9.2.1").resolve(name).toString()))),
					"missing svn mv for " + name);
		}
		for (var name : List.of("apache-juneau-9.2.1-bin.zip", "apache-juneau-9.2.1-bin.zip.asc",
				"apache-juneau-9.2.1-bin.zip.sha512")) {
			assertTrue(
					calls.stream()
							.anyMatch(x -> x.equals(
									List.of("svn", "mv", dev.resolve("binaries").resolve(rc).resolve(name).toString(),
											release.resolve("9.2.1").resolve(name).toString()))),
					"missing svn mv for " + name);
		}

		// 9.2.0 is the highest non-prerelease release strictly below 9.2.1 on the SAME 9.2.x line;
		// 9.1.5 (different minor) and 9.2.1-RC1 (prerelease/self) must not be picked.
		assertTrue(calls.stream().anyMatch(x -> x.equals(List.of("svn", "rm", release.resolve("9.2.0").toString()))));
		assertFalse(calls.stream().anyMatch(x -> x.contains("9.1.5")));

		var commit = calls.stream().filter(x -> x.contains("commit")).findFirst().orElseThrow();
		assertTrue(commit.contains("Apache Juneau 9.2.1"));
	}

	@Test
	void noPriorReleaseOnLineSkipsRemoval(@TempDir Path dir) {
		var c = ctx(ExecutionMode.LIVE, dir, List.of());
		var res = new DistPromoteStep().apply(c);
		assertTrue(res.success, res.message);
		assertFalse(calls.stream().anyMatch(x -> x.contains("rm") && x.get(0).equals("svn")));
	}

	@Test
	void safeSpawnsNothingAndLogsWouldRun(@TempDir Path dir) {
		var c = ctx(ExecutionMode.SAFE, dir, List.of("juneau-9.2.0"));
		var res = new DistPromoteStep().apply(c);
		assertTrue(res.success, res.message);
		assertEquals(0, calls.size(), "no real subprocess may be spawned in SAFE");
		var wouldRun = logLines.stream().filter(l -> l.startsWith("would run:")).toList();
		assertTrue(wouldRun.stream().anyMatch(l -> l.contains("svn") && l.contains("mv")));
		assertTrue(wouldRun.stream().anyMatch(l -> l.contains("svn") && l.contains("commit")));
		assertTrue(logLines.stream().noneMatch(l -> l.contains("s3cr3t")));
	}
}
