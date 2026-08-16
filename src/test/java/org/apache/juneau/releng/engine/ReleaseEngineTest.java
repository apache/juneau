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
import org.apache.juneau.releng.util.ProcessRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReleaseEngineTest {

	private ProcessRunner okRunner() {
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
				if (c.contains("ls-remote"))
					return new ProcResult(0, "sha\trefs/heads/juneau-9.2.1-branch\n");
				return new ProcResult(0, "ok");
			}

			@Override
			public ProcResult run(List<String> c, String s, Map<String, String> e,
					java.util.function.Consumer<String> k) {
				return run(c, s, e);
			}
		};
	}

	private ReleaseEngine engine(Path dir) {
		var runner = okRunner();
		var branches = new BranchResolver(runner, "/repo");
		return ReleaseEngine.forTests(new RunStateStore(dir), StepRegistry.standard(branches), runner, branches, dir);
	}

	private ReleaseEngine engineLive(Path dir) {
		var runner = okRunner();
		var branches = new BranchResolver(runner, "/repo");
		return ReleaseEngine.forTests(new RunStateStore(dir), StepRegistry.standard(branches), runner, branches, dir,
				ExecutionMode.LIVE);
	}

	/** Subscribes to {@code version}'s state broadcaster and decodes every published snapshot in order. */
	@SuppressWarnings({
		"resource" // The subscription stays open for the whole test; the JVM tears it down. Closing it would stop collecting snapshots.
	})
	private List<RunStateSnapshot> subscribeSnapshots(ReleaseEngine eng, String version) {
		var seen = new ArrayList<RunStateSnapshot>();
		eng.stateBroadcaster(version).subscribe(json -> seen.add(Json.DEFAULT.read(json, RunStateSnapshot.class)));
		return seen;
	}

	/**
	 * Marks every step before {@code stepId} (in registry order) SUCCEEDED, so a test can exercise
	 * {@code stepId} in isolation without needing every predecessor's own apply() to succeed for real
	 * (e.g. steps that need a wired Nexus client). Writes straight through a fresh store pointed at the
	 * same {@code dir}, since {@link RunStateStore} is a stateless file-backed reader/writer.
	 */
	private void satisfyAllPredecessorsOf(Path dir, String version, String stepId) {
		var store = new RunStateStore(dir);
		var rs = store.load(version).orElseThrow();
		var ids = StepRegistry.standard(new BranchResolver(okRunner(), "/repo")).ids();
		var idx = ids.indexOf(stepId);
		for (var i = 0; i < idx; i++)
			rs.step(ids.get(i)).status = StepStatus.SUCCEEDED;
		store.save(rs);
	}

	@Test
	void startCreatesRunSeededPending(@TempDir Path dir) {
		var eng = engine(dir);
		var rs = eng.start("9.2.1", null);
		assertEquals("juneau-9.2.1-branch", rs.branch);
		assertEquals(24, rs.steps.size());
		assertEquals(StepStatus.PENDING, rs.step("preflight").status);
	}

	@Test
	void startWithMilestoneNumberPersistsIt(@TempDir Path dir) {
		var eng = engine(dir);
		var rs = eng.start("9.2.1", null, 42);
		assertEquals(42, rs.milestoneNumber);
		assertEquals(42, eng.state("9.2.1").milestoneNumber, "must be persisted, readable on a fresh load");
	}

	@Test
	void startWithoutMilestoneNumberLeavesItNull(@TempDir Path dir) {
		var eng = engine(dir);
		var rs = eng.start("9.2.1", null);
		assertNull(rs.milestoneNumber);
	}

	@Test
	void startDefaultsToSafeEvenOnALiveBox(@TempDir Path dir) {
		var eng = engineLive(dir);
		var rs = eng.start("9.2.1", null);
		assertEquals(ExecutionMode.SAFE, rs.mode);
	}

	@Test
	void startLiveIsCappedToSafeWhenTheBoxIsSafe(@TempDir Path dir) {
		var eng = engine(dir);
		var rs = eng.start("9.2.1", null, null, ExecutionMode.LIVE);
		assertEquals(ExecutionMode.SAFE, rs.mode, "a SAFE box cannot mint a LIVE run");
	}

	@Test
	void startLiveIsHonoredWhenTheBoxIsLive(@TempDir Path dir) {
		var eng = engineLive(dir);
		var rs = eng.start("9.2.1", null, null, ExecutionMode.LIVE);
		assertEquals(ExecutionMode.LIVE, rs.mode);
	}

	@Test
	void secondStartWhileActiveIsRejected(@TempDir Path dir) {
		var eng = engine(dir);
		eng.start("9.2.1", null);
		var ex = assertThrows(IllegalStateException.class, () -> eng.start("9.2.2", null));
		assertTrue(ex.getMessage().contains("9.2.1"));
	}

	@Test
	void applyStepAdvancesAndPersists(@TempDir Path dir) {
		var eng = engine(dir);
		eng.start("9.2.1", null);
		var res = eng.apply("9.2.1", "preflight", Map.of());
		assertTrue(res.success);
		assertEquals(StepStatus.SUCCEEDED, eng.state("9.2.1").step("preflight").status);
		// reload from disk confirms persistence
		assertEquals(StepStatus.SUCCEEDED, new RunStateStore(dir).load("9.2.1").orElseThrow().step("preflight").status);
	}

	@Test
	void restartDemotesRunningStepToFailed(@TempDir Path dir) {
		var store = new RunStateStore(dir);
		var rs = RunState.create("9.2.1", "juneau-9.2.1-branch",
				StepRegistry.standard(new BranchResolver(okRunner(), "/repo")).ids());
		rs.step("release-prepare").status = StepStatus.RUNNING;
		rs.currentStepId = "release-prepare";
		store.save(rs);

		var eng = engine(dir);
		eng.recoverOnBoot();

		var back = store.load("9.2.1").orElseThrow();
		assertEquals(StepStatus.FAILED, back.step("release-prepare").status);
		assertTrue(back.step("release-prepare").error.toLowerCase().contains("restart"));
	}

	@Test
	void z0StartRequiresDevelopmentVersionAtPrepare(@TempDir Path dir) {
		var eng = engine(dir);
		eng.start("10.0.0", null); // start allowed; enforcement is at release-prepare
		// (preflight would fail on branch check for a real 10.0.0; here master resolves + ls-remote canned)
		assertEquals("master", eng.state("10.0.0").branch);
	}

	@Test
	void applyIsIndividuallyInvokableOnAnyStepRegardlessOfPointer(@TempDir Path dir) {
		// A step can be run on its own, out of pipeline order, as long as its own required predecessors
		// have already succeeded — currentStepId is bookkeeping only, never a gate.
		var eng = engine(dir);
		eng.start("9.2.1", null);
		eng.apply("9.2.1", "preflight", Map.of());
		eng.apply("9.2.1", "compose-propose-email", Map.of());
		// "workspace-setup" is not currentStepId ("compose-propose-email" now is) — apply() must still
		// work directly on it since its predecessors are already satisfied.
		var res = eng.apply("9.2.1", "workspace-setup", Map.of());
		assertTrue(res.success, res.message);
		assertEquals(StepStatus.SUCCEEDED, eng.state("9.2.1").step("workspace-setup").status);
	}

	@Test
	void reRunningASucceededStepOverwritesStatusAndLogInPlace(@TempDir Path dir) {
		// No separate "manual run" track; status and log overwrite in place.
		var eng = engine(dir);
		eng.start("9.2.1", null);
		eng.apply("9.2.1", "preflight", Map.of());
		var firstLogRef = eng.state("9.2.1").step("preflight").logRef;
		assertNotNull(firstLogRef);

		var res = eng.apply("9.2.1", "preflight", Map.of()); // ad-hoc re-run of an already-succeeded step

		assertTrue(res.success);
		assertEquals(StepStatus.SUCCEEDED, eng.state("9.2.1").step("preflight").status);
		assertEquals(firstLogRef, eng.state("9.2.1").step("preflight").logRef); // same file, overwritten not renamed
	}

	@Test
	void eachStepGetsItsOwnBroadcasterAndLogPath(@TempDir Path dir) {
		var eng = engine(dir);
		eng.start("9.2.1", null);
		eng.apply("9.2.1", "preflight", Map.of());
		eng.apply("9.2.1", "compose-propose-email", Map.of());
		eng.apply("9.2.1", "workspace-setup", Map.of());
		assertNotEquals(eng.state("9.2.1").step("preflight").logRef, eng.state("9.2.1").step("workspace-setup").logRef);
		assertNotSame(eng.broadcaster("9.2.1", "preflight"), eng.broadcaster("9.2.1", "workspace-setup"));
	}

	/**
	 * Root cause of the reported vote-gate stall: applying {@code vote-gate} only (re)opens the vote — it
	 * is NOT itself the advance action, however many times it's (re-)applied. This is the exact call the
	 * old (broken) "Simulate (SAFE)" wiring made. The actual gate-pass action is the distinct
	 * {@code tally-vote-result} step, with a {@code passed} outcome.
	 */
	@Test
	void voteGateApplyAloneNeverAdvancesPastAwaitingVote(@TempDir Path dir) {
		var eng = engine(dir);
		eng.start("9.2.1", null);
		satisfyAllPredecessorsOf(dir, "9.2.1", "vote-gate");

		eng.apply("9.2.1", "vote-gate", Map.of());
		assertEquals(StepStatus.AWAITING_VOTE, eng.state("9.2.1").step("vote-gate").status);
		assertEquals(RunStatus.AWAITING_VOTE, eng.state("9.2.1").status);

		eng.apply("9.2.1", "vote-gate", Map.of()); // re-clicking the old wiring a second time
		assertEquals(StepStatus.AWAITING_VOTE, eng.state("9.2.1").step("vote-gate").status, "still stuck");
		assertEquals(StepStatus.PENDING, eng.state("9.2.1").step("tally-vote-result").status,
				"vote-gate alone must never touch tally-vote-result");

		var res = eng.apply("9.2.1", "tally-vote-result", Map.of("voteOutcome", "passed"));

		assertTrue(res.success, res.message);
		assertEquals(StepStatus.SUCCEEDED, eng.state("9.2.1").step("tally-vote-result").status);
		assertEquals(StepStatus.SUCCEEDED, eng.state("9.2.1").step("vote-gate").status,
				"a passing tally must flip vote-gate to a terminal status, or it would block every later required step forever");
	}

	@Test
	void rejectedTallyLeavesVoteGateAwaitingVote(@TempDir Path dir) {
		var eng = engine(dir);
		eng.start("9.2.1", null);
		satisfyAllPredecessorsOf(dir, "9.2.1", "vote-gate");
		eng.apply("9.2.1", "vote-gate", Map.of());

		var res = eng.apply("9.2.1", "tally-vote-result", Map.of("voteOutcome", "rejected"));

		assertTrue(res.success, res.message); // recording a rejected outcome itself still succeeds
		assertEquals(StepStatus.AWAITING_VOTE, eng.state("9.2.1").step("vote-gate").status,
				"only a passing tally may resolve the gate; a rejected one forks to Drop-RC instead");
	}

	@Test
	void forwardApplyIsBlockedPastAnUnsatisfiedRequiredPredecessor(@TempDir Path dir) {
		var eng = engine(dir);
		eng.start("9.2.1", null);
		// "preflight" (index 0) is still PENDING; "workspace-setup" (index 2) must be refused.
		var res = eng.apply("9.2.1", "workspace-setup", Map.of());
		assertFalse(res.success);
		assertTrue(res.message.contains("preflight"), res.message);
		assertEquals(StepStatus.PENDING, eng.state("9.2.1").step("workspace-setup").status,
				"a blocked apply must not even flip to RUNNING");
	}

	@Test
	void reRunningAnAlreadySucceededStepIsNotBlockedByTheForwardApplyGuard(@TempDir Path dir) {
		var eng = engine(dir);
		eng.start("9.2.1", null);
		eng.apply("9.2.1", "preflight", Map.of());
		eng.apply("9.2.1", "compose-propose-email", Map.of());
		eng.apply("9.2.1", "workspace-setup", Map.of());

		var res = eng.apply("9.2.1", "preflight", Map.of()); // re-run an earlier, already-succeeded step

		assertTrue(res.success, res.message);
		assertEquals(StepStatus.SUCCEEDED, eng.state("9.2.1").step("preflight").status);
	}

	/**
	 * The reported bug: a run reached {@code RunStatus.RELEASED} even though {@code nexus-release} was
	 * never run and {@code manual-followup-checklist} had failed. finalize-run must refuse and list every
	 * offending predecessor, and must not touch {@code RunStatus} while refusing.
	 */
	@Test
	void finalizeRunRefusesWhenARequiredPriorStepIsNotTerminal(@TempDir Path dir) {
		var eng = engine(dir);
		eng.start("9.2.1", null);
		var rs = eng.state("9.2.1");
		for (var s : rs.steps)
			if (!s.id.equals("finalize-run"))
				s.status = StepStatus.SUCCEEDED;
		rs.step("nexus-release").status = StepStatus.PENDING;
		rs.step("manual-followup-checklist").status = StepStatus.FAILED;
		new RunStateStore(dir).save(rs);

		var res = eng.apply("9.2.1", "finalize-run", Map.of());

		assertFalse(res.success);
		assertTrue(res.message.contains("nexus-release"), res.message);
		assertTrue(res.message.contains("manual-followup-checklist"), res.message);
		assertNotEquals(RunStatus.RELEASED, eng.state("9.2.1").status);
		assertEquals(StepStatus.PENDING, eng.state("9.2.1").step("finalize-run").status,
				"a refused finalize-run must not even flip to RUNNING");
	}

	@Test
	void finalizeRunSucceedsWhenAllRequiredPriorStepsAreTerminal(@TempDir Path dir) {
		var eng = engine(dir);
		eng.start("9.2.1", null);
		var rs = eng.state("9.2.1");
		for (var s : rs.steps)
			if (!s.id.equals("finalize-run"))
				s.status = StepStatus.SUCCEEDED;
		// javadoc-verify/test-workspace-verify are skippable; SKIPPED counts as terminal-success for them.
		rs.step("javadoc-verify").status = StepStatus.SKIPPED;
		rs.step("test-workspace-verify").status = StepStatus.SKIPPED;
		new RunStateStore(dir).save(rs);

		var res = eng.apply("9.2.1", "finalize-run", Map.of());

		assertTrue(res.success, res.message);
		assertEquals(RunStatus.RELEASED, eng.state("9.2.1").status);
	}

	@Test
	void startPublishesASnapshotToStateBroadcasterSubscribers(@TempDir Path dir) {
		var eng = engine(dir);
		var seen = subscribeSnapshots(eng, "9.2.1");

		eng.start("9.2.1", null);

		assertEquals(1, seen.size());
		assertEquals("9.2.1", seen.get(0).version);
		assertEquals(RunStatus.RUNNING, seen.get(0).status);
		assertEquals(StepStatus.PENDING, seen.get(0).steps.get(0).status);
	}

	@Test
	void applyPublishesARunningSnapshotThenATerminalOne(@TempDir Path dir) {
		var eng = engine(dir);
		eng.start("9.2.1", null);
		var seen = subscribeSnapshots(eng, "9.2.1");

		eng.apply("9.2.1", "preflight", Map.of());

		assertEquals(2, seen.size(), "one snapshot when the step flips to RUNNING, one when it lands terminal");
		assertEquals(StepStatus.RUNNING, statusOf(seen.get(0), "preflight"));
		assertEquals(StepStatus.SUCCEEDED, statusOf(seen.get(1), "preflight"));
	}

	@Test
	void skipAndConfirmReviewEachPublishASnapshot(@TempDir Path dir) {
		var eng = engine(dir);
		eng.start("9.2.1", null);
		satisfyAllPredecessorsOf(dir, "9.2.1", "javadoc-verify");
		eng.apply("9.2.1", "javadoc-verify", Map.of()); // AWAITING_REVIEW (review-gate step)
		var seen = subscribeSnapshots(eng, "9.2.1");

		eng.confirmReview("9.2.1", "javadoc-verify");
		assertEquals(1, seen.size());
		assertEquals(StepStatus.SUCCEEDED, statusOf(seen.get(0), "javadoc-verify"));

		eng.skip("9.2.1", "test-workspace-verify"); // a skippable step
		assertEquals(2, seen.size());
		assertEquals(StepStatus.SKIPPED, statusOf(seen.get(1), "test-workspace-verify"));
	}

	@Test
	void armPublishesTheUpdatedArmedFlagEvenThoughItNeverCallsSave(@TempDir Path dir) {
		var eng = engineLive(dir);
		eng.start("9.2.1", null, null, ExecutionMode.LIVE);
		var seen = subscribeSnapshots(eng, "9.2.1");

		var res = eng.arm("9.2.1", "9.2.1 LIVE");

		assertTrue(res.success, res.message);
		assertEquals(1, seen.size());
		assertTrue(seen.get(0).armed);
		assertEquals(ExecutionMode.LIVE, seen.get(0).mode);
	}

	@Test
	void aRejectedArmAttemptPublishesNothing(@TempDir Path dir) {
		var eng = engineLive(dir);
		eng.start("9.2.1", null, null, ExecutionMode.LIVE);
		var seen = subscribeSnapshots(eng, "9.2.1");

		var res = eng.arm("9.2.1", "wrong phrase");

		assertFalse(res.success);
		assertTrue(seen.isEmpty(), "a rejected arm attempt must not push a stale/misleading snapshot");
	}

	@Test
	void successfulApplyClearsAPriorFailedRunStatusAndStepError(@TempDir Path dir) {
		// Fingerprint of the 9.2.1 rehearsal: tally-vote-result failed once (empty voteOutcome →
		// rs.status=FAILED + ss.error set), then succeeded later — but the success path left the run
		// FAILED and the leftover error on a SUCCEEDED step, so the New-Release rail vanished.
		var eng = engine(dir);
		eng.start("9.2.1", null);
		satisfyAllPredecessorsOf(dir, "9.2.1", "vote-gate");
		eng.apply("9.2.1", "vote-gate", Map.of());

		var failed = eng.apply("9.2.1", "tally-vote-result", Map.of());
		assertFalse(failed.success);
		assertEquals(RunStatus.FAILED, eng.state("9.2.1").status);
		assertNotNull(eng.state("9.2.1").step("tally-vote-result").error);

		var passed = eng.apply("9.2.1", "tally-vote-result", Map.of("voteOutcome", "passed"));
		assertTrue(passed.success, passed.message);
		assertEquals(StepStatus.SUCCEEDED, eng.state("9.2.1").step("tally-vote-result").status);
		assertNull(eng.state("9.2.1").step("tally-vote-result").error,
				"a later success must clear the leftover failure message");
		assertEquals(RunStatus.RUNNING, eng.state("9.2.1").status,
				"recovering from a failed step must unstick the run so the New-Release rail stays visible");
	}

	@Test
	void unknownStepApplyReturnsFailRatherThanThrowing(@TempDir Path dir) {
		var eng = engine(dir);
		eng.start("9.2.1", null);
		var res = assertDoesNotThrow(() -> eng.apply("9.2.1", "nexus-staging-release", Map.of()));
		assertFalse(res.success);
		assertTrue(res.message.contains("Unknown step"), res.message);
		assertTrue(res.message.contains("nexus-staging-release"), res.message);
	}

	@Test
	void snapshotJsonIsEmptyForAnUnknownVersionAndReflectsArmedForAKnownOne(@TempDir Path dir) {
		var eng = engineLive(dir);
		assertTrue(eng.snapshotJson("nope").isEmpty());

		eng.start("9.2.1", null, null, ExecutionMode.LIVE);
		eng.arm("9.2.1", "9.2.1 LIVE");
		var snap = Json.DEFAULT.read(eng.snapshotJson("9.2.1").orElseThrow(), RunStateSnapshot.class);
		assertTrue(snap.armed);
		assertEquals(ExecutionMode.LIVE, snap.mode);
	}

	private StepStatus statusOf(RunStateSnapshot snap, String stepId) {
		return snap.steps.stream().filter(s -> s.stepId.equals(stepId)).findFirst().orElseThrow().status;
	}
}
