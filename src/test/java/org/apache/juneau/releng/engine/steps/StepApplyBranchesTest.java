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
import java.util.Set;
import org.apache.juneau.releng.email.EmailService;
import org.apache.juneau.releng.engine.BranchResolver;
import org.apache.juneau.releng.engine.ReleaseStep;
import org.apache.juneau.releng.engine.RunState;
import org.apache.juneau.releng.engine.RunStatus;
import org.apache.juneau.releng.engine.StepContext;
import org.apache.juneau.releng.nexus.NexusStagingClient;
import org.apache.juneau.releng.util.ProcessRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Exercises the apply/preview bodies of the engine steps with a scripted {@link ProcessRunner}.
 */
class StepApplyBranchesTest {

	/** Records every command; a command fails when its joined text contains one of {@code failOn}. */
	static class Runner implements ProcessRunner {
		final List<String> cmds = new ArrayList<>();
		Set<String> failOn = Set.of();
		List<String> tags = List.of();

		@Override
		public List<String> runLines(List<String> c) {
			return tags;
		}

		@Override
		public String runText(List<String> c) {
			return "";
		}

		@Override
		public ProcResult run(List<String> c, String s, Map<String, String> e) {
			var j = String.join(" ", c);
			cmds.add(j);
			for (var f : failOn)
				if (j.contains(f))
					return new ProcResult(1, "");
			if (c.contains("rev-parse"))
				return new ProcResult(0, "deadbeef\n");
			return new ProcResult(0, "out");
		}
	}

	private final List<String> logs = new ArrayList<>();
	private final List<String> notes = new ArrayList<>();

	private StepContext ctx(Runner r, Path dir, String version) {
		var c = new StepContext();
		c.run = RunState.create(version, "master", List.of("x"));
		c.runner = r;
		c.log = logs::add;
		c.stagingRepo = dir.resolve("staging");
		c.stateDir = dir;
		c.repoDir = "/repo";
		c.availid = "jb";
		c.ldapPassword = "pw";
		c.gpgKeyId = "KEY";
		c.gpgPassphrase = "pp";
		c.githubToken = "tok";
		c.committerEmail = "jb@apache.org";
		c.formInputs = Map.of();
		c.email = new EmailService(dir, r);
		c.notes = (l, t, h) -> notes.add(t);
		return c;
	}

	private static void previewAndId(ReleaseStep s, StepContext c) {
		assertNotNull(s.id());
		assertNotNull(s.title());
		assertFalse(s.preview(c).lines.isEmpty());
	}

	@Test
	void a01_simpleMvnSteps() {
		var r = new Runner();
		var d = Path.of("/tmp/x");
		for (ReleaseStep s : List.<ReleaseStep>of(new BuildVerifyStep(), new ReleasePerformStep(),
				new JavadocVerifyStep(), new DeploySnapshotStep(), new ReleaseDiffReviewStep())) {
			var c = ctx(r, d, "9.2.1");
			previewAndId(s, c);
			assertTrue(s.apply(c).success, s.id());
			r.failOn = Set.of("mvn");
			var res = s.apply(c);
			r.failOn = Set.of();
			if (s instanceof JavadocVerifyStep || s instanceof ReleaseDiffReviewStep)
				assertTrue(res.success);
			else
				assertFalse(res.success, s.id());
		}
		assertTrue(new JavadocVerifyStep().reviewGate());
		assertTrue(new JavadocVerifyStep().skippable());
		assertTrue(new BuildVerifyStep().runsTests());
	}

	@Test
	void a02_emailSteps(@TempDir Path dir) {
		var r = new Runner();
		var c = ctx(r, dir, "9.2.1");
		c.formInputs = Map.of("highlights", "h", "voteOutcome", "passed", "tally", "3 +1");
		for (ReleaseStep s : List.<ReleaseStep>of(new ComposeProposeEmailStep(), new ComposeResultEmailStep(),
				new ComposeAnnouncementEmailStep(), new ComposeVoteEmailStep())) {
			previewAndId(s, c);
			assertTrue(s.apply(c).success, s.id());
		}
		assertTrue(logs.stream().anyMatch(l -> l.contains("placeholders")));
		assertEquals(4, notes.size());
	}

	@Test
	void a03_voteEmailUnknownCommitAndUnreadableSha(@TempDir Path dir) throws Exception {
		var r = new Runner();
		r.failOn = Set.of("rev-parse");
		var c = ctx(r, dir, "9.2.1");
		var shaFile = dir.resolve("dist/source/juneau-9.2.1-RC1/apache-juneau-9.2.1-src.zip.sha512");
		Files.createDirectories(shaFile.getParent());
		Files.createDirectories(shaFile.resolveSibling("x"));
		// A directory in place of the file is not a regular file -> blank.
		var s = new ComposeVoteEmailStep();
		assertTrue(s.apply(c).success);
		assertTrue(s.preview(c).lines.stream().anyMatch(l -> l.contains("unknown")));
	}

	@Test
	void a04_finalizeAndVoteGateAndTally(@TempDir Path dir) {
		var r = new Runner();
		var c = ctx(r, dir, "9.2.1");
		previewAndId(new FinalizeRunStep(), c);
		assertTrue(new FinalizeRunStep().apply(c).success);
		assertEquals(RunStatus.RELEASED, c.run.status);

		previewAndId(new VoteGateStep(), c);
		assertTrue(new VoteGateStep().apply(c).success);
		assertNotNull(c.run.voteDeadline);

		var t = new TallyVoteResultStep();
		previewAndId(t, c);
		assertFalse(t.apply(c).success);
		c.formInputs = Map.of("voteOutcome", "passed", "tally", "3 +1\nmore");
		assertTrue(t.apply(c).success);
		c.formInputs = Map.of("voteOutcome", "rejected");
		assertTrue(t.apply(c).success);
		assertTrue(notes.stream().anyMatch(n -> n.startsWith("Vote rejected")));
		assertTrue(notes.stream().anyMatch(n -> n.contains("3 +1")));
	}

	@Test
	void a05_githubReleaseCreate(@TempDir Path dir) {
		var r = new Runner();
		var c = ctx(r, dir, "9.2.1");
		var s = new GithubReleaseCreateStep();
		previewAndId(s, c);
		r.failOn = Set.of("release view");
		assertTrue(s.apply(c).success);
		r.failOn = Set.of("release view", "release create");
		assertFalse(s.apply(c).success);
		r.failOn = Set.of();
		var res = s.apply(c);
		assertEquals("Already exists.", res.message);
		assertTrue(s.mutating());
	}

	@Test
	void a06_manualChecklist(@TempDir Path dir) {
		var c = ctx(new Runner(), dir, "9.2.1");
		var s = new ManualFollowupChecklistStep();
		previewAndId(s, c);
		assertFalse(s.apply(c).success);
		c.formInputs = Map.of("checklist", String.join(",", ManualFollowupChecklistStep.ITEMS));
		assertTrue(s.apply(c).success);
	}

	@Test
	void a07_nexusRelease(@TempDir Path dir) {
		var calls = new ArrayList<String>();
		var c = ctx(new Runner(), dir, "9.2.1");
		c.nexus = NexusStagingClient.forTests((m, p, b) -> {
			calls.add(m + " " + p);
			return "";
		});
		var s = new NexusReleaseStep();
		previewAndId(s, c);
		assertFalse(s.apply(c).success);
		c.run.nexusRepoId = "orgapachejuneau-1";
		assertTrue(s.apply(c).success);
		assertFalse(calls.isEmpty());
	}

	@Test
	void a08_nexusCloseNoneFound(@TempDir Path dir) {
		var c = ctx(new Runner(), dir, "9.2.1");
		c.nexus = NexusStagingClient.forTests((m, p, b) -> "[]");
		var s = new NexusStagingCloseStep();
		assertTrue(s.preview(c).lines.get(0).contains("none found"));
		assertFalse(s.apply(c).success);
	}

	@Test
	void a09_preflightMissingBranch(@TempDir Path dir) {
		var r = new Runner();
		var c = ctx(r, dir, "9.2.1");
		var s = new PreflightStep(new BranchResolver(new ProcessRunner() {
			public List<String> runLines(List<String> x) {
				return List.of();
			}

			public String runText(List<String> x) {
				return "";
			}

			public ProcResult run(List<String> x, String s, Map<String, String> e) {
				return new ProcResult(1, "");
			}
		}, "/repo"));
		var res = s.apply(c);
		assertFalse(res.success);
		assertTrue(res.message.contains("does not exist"));
	}

	@Test
	void a10_releasePrepare(@TempDir Path dir) {
		var r = new Runner();
		var s = new ReleasePrepareStep();
		var c = ctx(r, dir, "9.3.0");
		assertTrue(s.preview(c).lines.stream().anyMatch(l -> l.contains("REQUIRED")));
		assertFalse(s.apply(c).success);
		c.formInputs = Map.of("developmentVersion", "9.4.0-SNAPSHOT");
		assertTrue(s.apply(c).success);
		assertEquals("9.4.0-SNAPSHOT", c.run.developmentVersion);
		r.failOn = Set.of("release:prepare");
		assertFalse(s.apply(c).success);
		var m = ctx(r, dir, "9.2.1");
		assertTrue(s.preview(m).lines.get(0).contains("9.2.2-SNAPSHOT"));
	}

	@Test
	void a12_workspaceSetup(@TempDir Path dir) throws Exception {
		var r = new Runner();
		var c = ctx(r, dir, "9.2.1");
		var s = new WorkspaceSetupStep();
		previewAndId(s, c);
		// Fresh: clone path.
		assertTrue(s.apply(c).success);
		assertTrue(r.cmds.stream().anyMatch(x -> x.startsWith("git clone")));
		r.failOn = Set.of("git clone");
		assertFalse(s.apply(c).success);
		// Existing clone.
		Files.createDirectories(c.stagingRepo.resolve(".git"));
		r.failOn = Set.of("checkout");
		assertFalse(s.apply(c).success);
		r.failOn = Set.of("pull");
		assertFalse(s.apply(c).success);
		r.failOn = Set.of();
		assertTrue(s.apply(c).success);
		assertTrue(logs.stream().anyMatch(l -> l.contains("idempotent")));
	}

	@Test
	void a13_binaryArtifactsStage(@TempDir Path dir) {
		var r = new Runner();
		var c = ctx(r, dir, "9.2.1");
		c.run.nexusRepoId = "orgapachejuneau-1";
		var s = new BinaryArtifactsStageStep();
		previewAndId(s, c);
		assertTrue(s.apply(c).success);
		assertTrue(Files.isRegularFile(dir.resolve("dist/source/juneau-9.2.1-RC1/apache-juneau-9.2.1-src.zip.sha512")));
		r.failOn = Set.of("svn commit");
		assertFalse(s.apply(c).success);
		r.failOn = Set.of("svn checkout");
		assertFalse(s.apply(c).success);
		r.failOn = Set.of("--print-md");
		assertTrue(s.apply(c).success);
	}

	@Test
	void a14_distPromote(@TempDir Path dir) {
		var r = new Runner();
		r.tags = List.of("juneau-9.2.0", "juneau-9.1.0", "juneau-9.2.0-RC1");
		var c = ctx(r, dir, "9.2.1");
		var s = new DistPromoteStep();
		assertTrue(s.preview(c).lines.stream().anyMatch(l -> l.contains("svn rm")));
		assertTrue(s.apply(c).success);
		assertTrue(r.cmds.stream().anyMatch(x -> x.startsWith("svn rm") && x.endsWith("9.2.0")));
		r.failOn = Set.of("svn commit");
		assertFalse(s.apply(c).success);
		r.failOn = Set.of("dist/release/juneau ");
		r.failOn = Set.of("dist-release");
		assertFalse(s.apply(c).success);
		r.failOn = Set.of("dist-dev");
		assertFalse(s.apply(c).success);
		r.failOn = Set.of();
		r.tags = List.of();
		assertTrue(s.preview(c).lines.stream().anyMatch(l -> l.contains("No prior")));
		assertTrue(s.apply(c).success);
	}

	@Test
	void a15_devDistVerifyMissing(@TempDir Path dir) {
		var r = new Runner();
		var c = ctx(r, dir, "9.2.1");
		var res = new DevDistVerifyStep().apply(c);
		assertFalse(res.success);
	}

	@Test
	void a16_previewsAndEdgeBranches(@TempDir Path dir) {
		var r = new Runner();
		var c = ctx(r, dir, "9.2.1");
		// Preflight preview with every tool missing.
		r.failOn = Set.of("which");
		var pf = new PreflightStep(new BranchResolver(r, "/repo"));
		assertTrue(pf.preview(c).lines.stream().anyMatch(l -> l.startsWith("[MISSING]")));
		r.failOn = Set.of();
		assertTrue(pf.preview(c).lines.stream().anyMatch(l -> l.startsWith("[ok]")));

		// Nexus close preview when a repo is discovered.
		c.nexus = NexusStagingClient.forTests((m, p, b) -> "[{\"repositoryId\":\"orgapachejuneau-7\",\"type\":\"open\",\"created\":\"2026-08-14\"}]");
		assertTrue(new NexusStagingCloseStep().preview(c).lines.get(0).contains("orgapachejuneau-7"));

		// developmentVersion carried on the run rather than the form.
		var m = ctx(r, dir, "9.3.0");
		m.run.developmentVersion = "9.4.0-SNAPSHOT";
		assertTrue(new ReleasePrepareStep().apply(m).success);
		m.run.developmentVersion = null;
		m.formInputs = Map.of("developmentVersion", " ");
		assertFalse(new ReleasePrepareStep().apply(m).success);
	}

	@Test
	void a17_devDistVerifyAllPresent(@TempDir Path dir) throws Exception {
		var r = new Runner();
		var c = ctx(r, dir, "9.2.1");
		for (var kind : List.of("src", "bin"))
			for (var ext : List.of("", ".asc", ".sha512")) {
				var sub = kind.equals("src") ? "source" : "binaries";
				var f = dir.resolve("dist").resolve(sub).resolve("juneau-9.2.1-RC1")
						.resolve("apache-juneau-9.2.1-" + kind + ".zip" + ext);
				Files.createDirectories(f.getParent());
				Files.writeString(f, "x");
			}
		assertTrue(new DevDistVerifyStep().apply(c).success);
		assertTrue(r.cmds.stream().anyMatch(x -> x.startsWith("open ")));
	}

	@Test
	void a18_unreadableShaFileIsBlank(@TempDir Path dir) throws Exception {
		var c = ctx(new Runner(), dir, "9.2.1");
		var f = dir.resolve("dist/source/juneau-9.2.1-RC1/apache-juneau-9.2.1-src.zip.sha512");
		Files.createDirectories(f.getParent());
		Files.writeString(f, "secret");
		Files.setPosixFilePermissions(f, java.nio.file.attribute.PosixFilePermissions.fromString("---------"));
		try {
			assertTrue(new ComposeVoteEmailStep().apply(c).success);
		} finally {
			Files.setPosixFilePermissions(f, java.nio.file.attribute.PosixFilePermissions.fromString("rw-r--r--"));
		}
	}

	@Test
	void a19_stageCannotWriteChecksum(@TempDir Path dir) throws Exception {
		var c = ctx(new Runner(), dir, "9.2.1");
		c.run.nexusRepoId = "r";
		// The RC directory path is occupied by a regular file, so the checksum file cannot be created under it.
		var rcDir = dir.resolve("dist/source/juneau-9.2.1-RC1");
		Files.createDirectories(rcDir.getParent());
		Files.writeString(rcDir, "blocker");
		var step = new BinaryArtifactsStageStep();
		assertThrows(IllegalStateException.class, () -> step.apply(c));
	}

	@Test
	void a20_priorReleaseSelection(@TempDir Path dir) {
		var r = new Runner();
		r.tags = List.of("juneau-8.2.0", "juneau-9.1.0", "juneau-9.2.5", "juneau-9.2.0", "juneau-9.2.2",
				"juneau-9.2.1", "juneau-9.2.1-RC1");
		var c = ctx(r, dir, "9.2.3");
		assertTrue(new DistPromoteStep().preview(c).lines.stream().anyMatch(l -> l.contains("juneau/9.2.2")));
	}

	@Test
	void a21_devDistVerifyPreview(@TempDir Path dir) {
		assertFalse(new DevDistVerifyStep().preview(ctx(new Runner(), dir, "9.2.1")).lines.isEmpty());
	}
}
