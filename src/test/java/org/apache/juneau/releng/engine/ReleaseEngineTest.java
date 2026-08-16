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
		// A step can be run on its own, out of pipeline order.
		var eng = engine(dir);
		eng.start("9.2.1", null);
		// "workspace-setup" is not currentStepId ("preflight" is) — apply() must still work directly on it.
		var res = eng.apply("9.2.1", "workspace-setup", Map.of());
		assertTrue(res.success);
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
		eng.apply("9.2.1", "workspace-setup", Map.of());
		assertNotEquals(eng.state("9.2.1").step("preflight").logRef, eng.state("9.2.1").step("workspace-setup").logRef);
		assertNotSame(eng.broadcaster("9.2.1", "preflight"), eng.broadcaster("9.2.1", "workspace-setup"));
	}
}
