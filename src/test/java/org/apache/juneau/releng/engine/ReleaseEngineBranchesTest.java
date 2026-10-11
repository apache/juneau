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
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.apache.juneau.releng.config.TargetProfile;
import org.apache.juneau.releng.util.ProcessRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Branch coverage for {@link ReleaseEngine} using small scripted steps in place of the real pipeline.
 */
class ReleaseEngineBranchesTest {

	/** A configurable stand-in step. */
	static class Stub implements ReleaseStep {
		final String id;
		boolean skippable, gate, tests;
		Function<StepContext, StepResult> body = c -> StepResult.ok("ok");

		Stub(String id) {
			this.id = id;
		}

		@Override
		public String id() {
			return id;
		}

		@Override
		public String title() {
			return id;
		}

		@Override
		public boolean skippable() {
			return skippable;
		}

		@Override
		public boolean reviewGate() {
			return gate;
		}

		@Override
		public boolean runsTests() {
			return tests;
		}

		@Override
		public Preview preview(StepContext ctx) {
			return new Preview(id, false).line("p");
		}

		@Override
		public StepResult apply(StepContext ctx) {
			return body.apply(ctx);
		}
	}

	private static final ProcessRunner RUNNER = new ProcessRunner() {
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
	};

	private ReleaseEngine engine(Path dir, RunStateStore store, ReleaseStep... steps) {
		var branches = new BranchResolver(RUNNER, "/repo");
		return ReleaseEngine.forTests(store, new StepRegistry(List.of(steps)), RUNNER, branches, dir);
	}

	@Test
	void a01_accessorsAndPreview(@TempDir Path dir) {
		var store = new RunStateStore(dir);
		var eng = engine(dir, store, new Stub("s1"));
		assertEquals(TargetProfile.prodDefault(), eng.target());
		assertTrue(eng.activeRun().isEmpty());
		eng.start("9.2.1", null, 0);
		assertEquals(1, eng.state("9.2.1").rc);
		assertEquals("p", eng.preview("9.2.1", "s1", null).lines.get(0));
		assertThrows(IllegalArgumentException.class, () -> eng.preview("9.2.1", "nope", null));
		assertThrows(RuntimeException.class, () -> eng.preview("0.0.0", "s1", null));
		assertTrue(eng.activeRun().isPresent());
	}

	@Test
	void a02_nullTargetDefaultsToProd(@TempDir Path dir) {
		var base = engine(dir, new RunStateStore(dir));
		var eng = new ReleaseEngine(new RunStateStore(dir), base.registry(), RUNNER,
				new BranchResolver(RUNNER, "/repo"), dir, dir, "/repo", "a@b", null, null, base.secrets(), null);
		assertEquals(TargetProfile.prodDefault(), eng.target());
	}

	@Test
	void a03_stepThrowsBecomesFailure(@TempDir Path dir) {
		var boom = new Stub("boom");
		boom.body = c -> {
			throw new IllegalStateException("kaboom");
		};
		var store = new RunStateStore(dir);
		var eng = engine(dir, store, boom);
		eng.start("9.2.1", null);
		var res = eng.apply("9.2.1", "boom", null);
		assertFalse(res.success);
		assertEquals("kaboom", res.message);
		assertEquals(RunStatus.FAILED, eng.state("9.2.1").status);
		assertFalse(eng.apply("9.2.1", "nope", null).success);
	}

	@Test
	void a04_tallyWithNullFormAndNoVoteGate(@TempDir Path dir) {
		var store = new RunStateStore(dir);
		var eng = engine(dir, store, new Stub("tally-vote-result"));
		eng.start("9.2.1", null);
		assertTrue(eng.apply("9.2.1", "tally-vote-result", null).success);
		var rs = eng.state("9.2.1");
		rs.steps.get(0).status = StepStatus.PENDING;
		store.save(rs);
		assertTrue(eng.apply("9.2.1", "tally-vote-result", Map.of("voteOutcome", "passed")).success);
	}

	@Test
	void a05_tallyResolvesOpenVoteGate(@TempDir Path dir) {
		var store = new RunStateStore(dir);
		var eng = engine(dir, store, new Stub("vote-gate"), new Stub("tally-vote-result"), new Stub("after"));
		eng.start("9.2.1", null);
		assertTrue(eng.apply("9.2.1", "vote-gate", null).success);
		assertEquals(StepStatus.AWAITING_VOTE, eng.state("9.2.1").step("vote-gate").status);
		// vote-gate is open: only the tally step may proceed past it.
		var blocked = eng.apply("9.2.1", "after", null);
		assertFalse(blocked.success);
		assertTrue(blocked.message.contains("vote-gate"));
		assertTrue(eng.apply("9.2.1", "tally-vote-result", Map.of("voteOutcome", "passed")).success);
		assertEquals(StepStatus.SUCCEEDED, eng.state("9.2.1").step("vote-gate").status);
		assertTrue(eng.apply("9.2.1", "after", null).success);
	}

	@Test
	void a06_guardIgnoresPredecessorMissingFromState(@TempDir Path dir) {
		var store = new RunStateStore(dir);
		var eng = engine(dir, store, new Stub("s1"), new Stub("s2"));
		eng.start("9.2.1", null);
		var rs = eng.state("9.2.1");
		rs.steps.removeIf(s -> s.id.equals("s1"));
		store.save(rs);
		assertTrue(eng.apply("9.2.1", "s2", null).success);
	}

	@Test
	void a07_skippedSkippablePredecessorSatisfiesGuard(@TempDir Path dir) {
		var s1 = new Stub("s1");
		s1.skippable = true;
		var store = new RunStateStore(dir);
		var eng = engine(dir, store, s1, new Stub("s2"));
		eng.start("9.2.1", null);
		assertFalse(eng.skip("9.2.1", "nope").success);
		assertTrue(eng.skip("9.2.1", "s1").success);
		assertTrue(eng.apply("9.2.1", "s2", null).success);
		assertFalse(eng.skip("9.2.1", "s2").success);
	}

	@Test
	void a08_confirmReview(@TempDir Path dir) {
		var gate = new Stub("g");
		gate.gate = true;
		var store = new RunStateStore(dir);
		var eng = engine(dir, store, gate, new Stub("s2"));
		eng.start("9.2.1", null);
		assertFalse(eng.confirmReview("9.2.1", "g").success, "not awaiting review yet");
		assertTrue(eng.apply("9.2.1", "g", null).success);
		assertEquals(StepStatus.AWAITING_REVIEW, eng.state("9.2.1").step("g").status);
		var rs = eng.state("9.2.1");
		rs.step("g").startedAt = "garbage";
		store.save(rs);
		assertTrue(eng.confirmReview("9.2.1", "g").success);
		assertThrows(IllegalArgumentException.class, () -> eng.confirmReview("9.2.1", "nope"));
		rs = eng.state("9.2.1");
		rs.steps.removeIf(s -> s.id.equals("s2"));
		store.save(rs);
		assertFalse(eng.confirmReview("9.2.1", "s2").success);
	}

	@Test
	void a09_testStepAppliesWithMissingStagingDir(@TempDir Path dir) {
		var t = new Stub("t");
		t.tests = true;
		var eng = engine(dir, new RunStateStore(dir), t);
		eng.start("9.2.1", null);
		assertTrue(eng.apply("9.2.1", "t", null).success);
	}

	@Test
	void a10_recoverOnBoot(@TempDir Path dir) {
		var store = new RunStateStore(dir);
		var eng = engine(dir, store, new Stub("s1"), new Stub("s2"));
		eng.start("9.2.1", null);
		// Non-running current step: left alone.
		eng.recoverOnBoot();
		assertEquals(RunStatus.RUNNING, eng.state("9.2.1").status);
		var rs = eng.state("9.2.1");
		rs.currentStepId = null;
		store.save(rs);
		eng.recoverOnBoot();
		assertEquals(RunStatus.RUNNING, eng.state("9.2.1").status);
		rs = eng.state("9.2.1");
		rs.currentStepId = "s1";
		rs.step("s1").status = StepStatus.RUNNING;
		store.save(rs);
		eng.recoverOnBoot();
		assertEquals(RunStatus.FAILED, eng.state("9.2.1").status);
		assertEquals("interrupted by server restart", eng.state("9.2.1").step("s1").error);
	}

	@Test
	void a11_skippedButNotSkippablePredecessorBlocks(@TempDir Path dir) {
		var store = new RunStateStore(dir);
		var eng = engine(dir, store, new Stub("s1"), new Stub("s2"));
		eng.start("9.2.1", null);
		var rs = eng.state("9.2.1");
		rs.step("s1").status = StepStatus.SKIPPED;
		store.save(rs);
		var res = eng.apply("9.2.1", "s2", null);
		assertFalse(res.success);
		assertTrue(res.message.contains("s1 (SKIPPED)"));
	}
}
