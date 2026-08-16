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
import org.apache.juneau.marshall.marshaller.Json;
import org.apache.juneau.releng.config.TargetProfile;
import org.apache.juneau.releng.log.LogBroadcaster;
import org.apache.juneau.releng.nexus.NexusStagingClient;
import org.apache.juneau.releng.util.ProcessRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DropRcServiceTest {

	private ProcessRunner runner() {
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
				return new ProcResult(0, "");
			}

			@Override
			public ProcResult run(List<String> c, String s, Map<String, String> e,
					java.util.function.Consumer<String> k) {
				return new ProcResult(0, "");
			}
		};
	}

	private RunState seededThroughReleasePrepare(RunStateStore store) {
		var ids = StepRegistry.standard(new BranchResolver(runner(), "/repo")).ids();
		var rs = RunState.create("9.2.1", "juneau-9.2.1-branch", ids);
		for (var id : List.of("preflight", "compose-propose-email", "workspace-setup", "build-verify", "javadoc-verify",
				"test-workspace-verify", "deploy-snapshot", "release-prepare")) {
			rs.step(id).status = StepStatus.SUCCEEDED;
			rs.step(id).logRef = "logs/9.2.1-RC1-" + id + ".log"; // so the reset-clears-logRef test is meaningful
		}
		rs.nexusRepoId = "orgapachejuneau-1042";
		rs.currentStepId = "vote-gate";
		rs.status = RunStatus.AWAITING_VOTE;
		store.save(rs);
		return rs;
	}

	@Test
	void previewNamesRepoTagAndNextRc(@TempDir Path dir) {
		var store = new RunStateStore(dir);
		seededThroughReleasePrepare(store);
		var svc = new DropRcService(store, StepRegistry.standard(new BranchResolver(runner(), "/repo")), runner(),
				Path.of("/staging/git/juneau"), dir, NexusStagingClient.forTests((m, p, b) -> ""), ExecutionMode.LIVE,
				v -> true, TargetProfile.prodDefault(), (v, s) -> new LogBroadcaster());
		var preview = svc.preview("9.2.1");
		assertTrue(preview.lines.stream().anyMatch(l -> l.contains("orgapachejuneau-1042")));
		assertTrue(preview.lines.stream().anyMatch(l -> l.contains("juneau-9.2.1-RC1")));
		assertTrue(preview.lines.stream().anyMatch(l -> l.contains("RC2")));
	}

	@Test
	void applyResetsFromWorkspaceSetupAndBumpsRc(@TempDir Path dir) {
		var store = new RunStateStore(dir);
		seededThroughReleasePrepare(store);
		var svc = new DropRcService(store, StepRegistry.standard(new BranchResolver(runner(), "/repo")), runner(),
				Path.of("/staging/git/juneau"), dir, NexusStagingClient.forTests((m, p, b) -> ""), ExecutionMode.LIVE,
				v -> true, TargetProfile.prodDefault(), (v, s) -> new LogBroadcaster());

		svc.apply("9.2.1", "vote rejected: -1 jdoe", () -> "avail", () -> "pw");

		var rs = store.load("9.2.1").orElseThrow();
		assertEquals(2, rs.rc);
		assertEquals(1, rs.rcHistory.size());
		assertEquals(RunStatus.RUNNING, rs.status);
		// steps 1-2 kept, including their logRef (their output genuinely didn't change for this RC)
		assertEquals(StepStatus.SUCCEEDED, rs.step("preflight").status);
		assertEquals(StepStatus.SUCCEEDED, rs.step("compose-propose-email").status);
		assertEquals("logs/9.2.1-RC1-preflight.log", rs.step("preflight").logRef);
		// workspace-setup onward reset, including logRef cleared — each step's own log is recreated lazily
		// on its next run under the new RC via ReleaseEngine.apply().
		assertEquals(StepStatus.PENDING, rs.step("workspace-setup").status);
		assertEquals(StepStatus.PENDING, rs.step("build-verify").status);
		assertEquals(StepStatus.PENDING, rs.step("release-prepare").status);
		assertNull(rs.step("workspace-setup").logRef);
		assertNull(rs.step("release-prepare").logRef);
	}

	/** §7.3/fidelity fix: Drop-RC's SVN cleanup + Tier-B log sink (was previously a silent no-op in SAFE). */
	@Test
	void safeSvnCleanupSpawnsNothingButCommandLogsToItsOwnLogFile(@TempDir Path dir) throws java.io.IOException {
		var store = new RunStateStore(dir);
		seededThroughReleasePrepare(store);
		var runCount = new int[] { 0 };
		var runner = new ProcessRunner() {
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
				runCount[0]++;
				return new ProcResult(0, "");
			}

			@Override
			public ProcResult run(List<String> c, String s, Map<String, String> e,
					java.util.function.Consumer<String> k) {
				runCount[0]++;
				return new ProcResult(0, "");
			}
		};
		var svc = new DropRcService(store, StepRegistry.standard(new BranchResolver(runner, "/repo")), runner,
				Path.of("/staging/git/juneau"), dir, NexusStagingClient.forTests((m, p, b) -> ""), ExecutionMode.SAFE,
				v -> true, TargetProfile.prodDefault(), (v, s) -> new LogBroadcaster());

		svc.apply("9.2.1", "vote rejected: -1 jdoe", () -> "avail", () -> "s3cr3t-ldap-password");

		assertEquals(0, runCount[0], "no real subprocess may be spawned for Drop-RC's Tier-B calls in SAFE");
		var log = java.nio.file.Files.readString(dir.resolve("logs/9.2.1-RC1-drop-rc.log"),
				java.nio.charset.StandardCharsets.UTF_8);
		assertTrue(log.contains("would run: svn checkout"));
		assertTrue(log.contains("would run: svn rm") && log.contains("source/juneau-9.2.1-RC1"));
		assertTrue(log.contains("would run: svn rm") && log.contains("binaries/juneau-9.2.1-RC1"));
		assertTrue(log.contains("would run: svn commit"));
		assertTrue(log.contains("would run: git") && log.contains("tag") && log.contains("-d"));
		assertTrue(log.contains("would run: git") && log.contains("push") && log.contains(":refs/tags/"));
		assertTrue(log.contains("would run: mvn") && log.contains("release:rollback"));
		assertFalse(log.contains("s3cr3t-ldap-password"), "the password must never be logged");
	}

	@Test
	void safeSvnCleanupPathsIncludeCurrentRcTag(@TempDir Path dir) throws java.io.IOException {
		var store = new RunStateStore(dir);
		seededThroughReleasePrepare(store);
		var svc = new DropRcService(store, StepRegistry.standard(new BranchResolver(runner(), "/repo")), runner(),
				Path.of("/staging/git/juneau"), dir, NexusStagingClient.forTests((m, p, b) -> ""), ExecutionMode.SAFE,
				v -> true, TargetProfile.prodDefault(), (v, s) -> new LogBroadcaster());
		svc.apply("9.2.1", "vote rejected", () -> "avail", () -> "pw");
		var log = java.nio.file.Files.readString(dir.resolve("logs/9.2.1-RC1-drop-rc.log"),
				java.nio.charset.StandardCharsets.UTF_8);
		assertTrue(log.contains("https://dist.apache.org/repos/dist/dev/juneau"));
	}

	@Test
	void liveBoxSafeRunDoesNotSpawnSubprocess(@TempDir Path dir) {
		var store = new RunStateStore(dir);
		var rs = seededThroughReleasePrepare(store);
		rs.mode = ExecutionMode.SAFE;
		store.save(rs);
		var runCount = new int[] { 0 };
		var runner = new ProcessRunner() {
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
				runCount[0]++;
				return new ProcResult(0, "");
			}

			@Override
			public ProcResult run(List<String> c, String s, Map<String, String> e,
					java.util.function.Consumer<String> k) {
				runCount[0]++;
				return new ProcResult(0, "");
			}
		};
		var svc = new DropRcService(store, StepRegistry.standard(new BranchResolver(runner, "/repo")), runner,
				Path.of("/staging/git/juneau"), dir, NexusStagingClient.forTests((m, p, b) -> ""), ExecutionMode.LIVE,
				v -> true, TargetProfile.prodDefault(), (v, s) -> new LogBroadcaster());

		svc.apply("9.2.1", "vote rejected: -1 jdoe", () -> "avail", () -> "pw");

		assertEquals(0, runCount[0], "a Dry-run on a LIVE box still command-logs Drop-RC; no real subprocess");
	}

	/**
	 * Drop-RC's own {@code store.save(rs)} is the same choke point {@link ReleaseEngine} hooks in its
	 * constructor (see {@code RunStateStore.setOnSave}) — since AppConfiguration wires the SAME
	 * {@code RunStateStore} bean into both the engine and this service, a drop-RC push must reach the
	 * New-Release tab's rail without any drop-RC-specific broadcast wiring.
	 */
	@Test
	@SuppressWarnings({
		"resource" // The subscription stays open for the whole test; the JVM tears it down. Closing it would stop collecting snapshots.
	})
	void applyPublishesAResetSnapshotViaTheEngineSSharedRunStateStoreHook(@TempDir Path dir) {
		var store = new RunStateStore(dir);
		var branches = new BranchResolver(runner(), "/repo");
		var eng = ReleaseEngine.forTests(store, StepRegistry.standard(branches), runner(), branches, dir,
				ExecutionMode.LIVE); // installs the onSave hook on this exact store instance
		seededThroughReleasePrepare(store);
		var seen = new ArrayList<RunStateSnapshot>();
		eng.stateBroadcaster("9.2.1").subscribe(json -> seen.add(Json.DEFAULT.read(json, RunStateSnapshot.class)));

		var svc = new DropRcService(store, StepRegistry.standard(branches), runner(), Path.of("/staging/git/juneau"),
				dir, NexusStagingClient.forTests((m, p, b) -> ""), ExecutionMode.LIVE, v -> true,
				TargetProfile.prodDefault(), (v, s) -> new LogBroadcaster());

		svc.apply("9.2.1", "vote rejected: -1 jdoe", () -> "avail", () -> "pw");

		assertEquals(1, seen.size());
		var snap = seen.get(0);
		assertEquals(RunStatus.RUNNING, snap.status);
		assertEquals(StepStatus.PENDING, snap.steps.stream().filter(s -> s.stepId.equals("workspace-setup"))
				.findFirst().orElseThrow().status);
	}
}
