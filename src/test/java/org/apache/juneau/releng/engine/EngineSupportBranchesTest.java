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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.juneau.releng.util.ProcessRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Branch and error-path coverage for the small classes of the engine package.
 */
class EngineSupportBranchesTest {

	private static ProcessRunner runner(int exit, String out) {
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
				return new ProcResult(exit, out);
			}
		};
	}

	private static void chmod(Path p, String perms) throws Exception {
		Files.setPosixFilePermissions(p, PosixFilePermissions.fromString(perms));
	}

	@Test
	void a01_stepContextNotesAndExec() {
		var c = new StepContext();
		c.warnNote("ignored without a sink");
		c.note("ignored too");
		var seen = new ArrayList<String>();
		c.notes = (l, t, h) -> seen.add(l + ":" + t + ":" + h);
		c.warnNote("w");
		c.note("n", "href");
		assertEquals(List.of("WARN:w:null", "INFO:n:href"), seen);
		c.runner = runner(0, "x\n");
		var lines = new ArrayList<String>();
		c.log = lines::add;
		assertTrue(c.exec(List.of("a")).ok());
		assertTrue(c.exec(List.of("a"), "in", Map.of()).ok());
		assertEquals(List.of("x", "x"), lines);
	}

	@Test
	void a02_redactArgv() {
		assertEquals(List.of("svn", "--password", "<redacted>", "--password-from-stdin", "ls", "--token", "<redacted>",
				"--passphrase", "<redacted>", "--secret", "<redacted>", "plain"),
				StepContext.redactArgv(List.of("svn", "--password", "hunter2", "--password-from-stdin", "ls", "--token",
						"x", "--passphrase", "y", "--secret", "z", "plain")));
		assertEquals(List.of("--from-stdin-password", "a"), StepContext.redactArgv(List.of("--from-stdin-password", "a")));
	}

	@Test
	void a03_branchResolverOutcomes() {
		assertFalse(new BranchResolver(runner(1, "refs/heads/b"), "/r").remoteBranchExists("b"));
		assertFalse(new BranchResolver(runner(0, null), "/r").remoteBranchExists("b"));
		assertFalse(new BranchResolver(runner(0, "refs/heads/other"), "/r").remoteBranchExists("b"));
		assertTrue(new BranchResolver(runner(0, "x\trefs/heads/b"), "/r").remoteBranchExists("b"));
	}

	@Test
	void a04_runStateAndPlainValueClasses() {
		var rs = RunState.create("9.2.1", "master", List.of());
		assertNull(rs.currentStepId);
		assertTrue(new Preview().lines.isEmpty());
		var r = new StepResult();
		assertFalse(r.success);
	}

	@Test
	void a05_storeIoFailures(@TempDir Path dir) throws Exception {
		var file = Files.writeString(dir.resolve("afile"), "x");
		assertThrows(RuntimeException.class, () -> new RunStateStore(file).save(RunState.create("1.0.0", "b", List.of())));

		var store = new RunStateStore(dir);
		store.save(RunState.create("1.0.0", "b", List.of()));
		var json = dir.resolve("release-1.0.0.json");
		assertTrue(Files.isRegularFile(json), "release-<version>.json naming");

		chmod(json, "---------");
		try {
			assertThrows(RuntimeException.class, () -> store.load("1.0.0"));
			assertThrows(RuntimeException.class, store::loadAll);
		} finally {
			chmod(json, "rw-r--r--");
		}
		chmod(dir, "---------");
		try {
			assertThrows(RuntimeException.class, store::loadAll);
		} finally {
			chmod(dir, "rwxr-xr-x");
		}

		var d = dir.resolve("release-2.0.0.json");
		Files.createDirectories(d);
		Files.writeString(d.resolve("child"), "x");
		assertThrows(RuntimeException.class, () -> store.delete("2.0.0"));
	}

	@Test
	void a06_storeFiltersAndPicksRuns(@TempDir Path dir) throws Exception {
		var store = new RunStateStore(dir);
		assertTrue(new RunStateStore(dir.resolve("missing")).loadAll().isEmpty());
		Files.writeString(dir.resolve("release-notes.txt"), "not a run");
		Files.writeString(dir.resolve("other.json"), "{}");
		assertTrue(store.loadAll().isEmpty());
		assertTrue(store.displayRun().isEmpty());

		var failedOld = RunState.create("1.0.0", "b", List.of());
		failedOld.status = RunStatus.FAILED;
		store.save(failedOld);
		Thread.sleep(5);
		var failedNew = RunState.create("1.0.1", "b", List.of());
		failedNew.status = RunStatus.FAILED;
		store.save(failedNew);
		var done = RunState.create("1.0.2", "b", List.of());
		done.status = RunStatus.RELEASED;
		store.save(done);
		assertTrue(store.activeRun().isEmpty());
		assertEquals("1.0.1", store.displayRun().orElseThrow().version);

		var waiting = RunState.create("1.0.3", "b", List.of());
		waiting.status = RunStatus.AWAITING_VOTE;
		store.save(waiting);
		assertEquals("1.0.3", store.activeRun().orElseThrow().version);
		assertEquals("1.0.3", store.displayRun().orElseThrow().version);
	}

	@Test
	void a07_dropRcWithoutNexusOrReason(@TempDir Path dir) {
		var store = new RunStateStore(dir);
		var branches = new BranchResolver(runner(0, ""), "/repo");
		var reg = StepRegistry.standard(branches);
		var withNexus = org.apache.juneau.releng.nexus.NexusStagingClient.forTests((m, p, b) -> "");
		for (var reason : new String[] { null, " " }) {
			var rs = RunState.create("9.2.1", "b", reg.ids());
			rs.nexusRepoId = reason == null ? null : "repo-1"; // a recorded repo with no Nexus client configured is skipped
			store.save(rs);
			var svc = new DropRcService(store, reg, runner(0, ""), dir.resolve("g"), dir, null, null);
			if (reason == null)
				svc = new DropRcService(store, reg, runner(0, ""), dir.resolve("g"), dir, withNexus, null);
			svc.apply("9.2.1", reason, () -> "a", () -> "p");
			assertEquals(2, store.load("9.2.1").orElseThrow().rc);
			store.delete("9.2.1");
		}
	}

	@Test
	void a08_testReportsUnreadableSubdirAndNullRoot(@TempDir Path root) throws Exception {
		assertTrue(TestReportEvents.collect(null, Instant.now(), "b").isEmpty());
		var locked = root.resolve("locked");
		Files.createDirectories(locked);
		chmod(locked, "---------");
		try {
			var ev = TestReportEvents.collect(root, Instant.now(), "b");
			assertFalse(ev.isEmpty());
		} finally {
			chmod(locked, "rwxr-xr-x");
		}
	}

	@Test
	void a09_testReportsCapped(@TempDir Path root) throws Exception {
		var dir = root.resolve("m/target/surefire-reports");
		Files.createDirectories(dir);
		var sb = new StringBuilder("<?xml version=\"1.0\"?><testsuite name=\"A\" tests=\"100000\">");
		for (var i = 0; i < 100_000; i++)
			sb.append("<testcase name=\"t").append(i).append("\" classname=\"A\" time=\"0\"/>");
		sb.append("</testsuite>");
		Files.writeString(dir.resolve("TEST-A.xml"), sb);
		Files.writeString(dir.resolve("TEST-B.xml"), sb);
		var ev = TestReportEvents.collect(root, Instant.now().minusSeconds(60), "b");
		assertTrue(ev.stream().anyMatch(e -> e.toString().contains("truncated")), "cap warning expected");
	}
}
