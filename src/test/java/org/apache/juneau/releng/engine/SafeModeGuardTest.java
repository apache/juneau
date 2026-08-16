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

/** The default-safe live guard + in-memory arming (never persisted, so re-arm is required after a restart). */
class SafeModeGuardTest {

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

	private ReleaseEngine engine(Path dir, ExecutionMode mode) {
		var runner = okRunner();
		var branches = new BranchResolver(runner, "/repo");
		return ReleaseEngine.forTests(new RunStateStore(dir), StepRegistry.standard(branches), runner, branches, dir,
				mode);
	}

	// Marks every step ahead of stepId SUCCEEDED so the forward-apply guard (ReleaseEngine) doesn't refuse
	// to run stepId in isolation; this test is about the LIVE-armed/SAFE guard, not the whole pipeline.
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
	void liveUnarmedRefusesMutatingStep(@TempDir Path dir) {
		var eng = engine(dir, ExecutionMode.LIVE);
		eng.start("9.2.1", null, null, ExecutionMode.LIVE);
		var res = eng.apply("9.2.1", "release-prepare", Map.of());
		assertFalse(res.success);
		assertTrue(res.message.toLowerCase().contains("arm"));
		assertEquals(StepStatus.PENDING, eng.state("9.2.1").step("release-prepare").status);
	}

	@Test
	void liveArmedRunsMutatingStep(@TempDir Path dir) {
		var eng = engine(dir, ExecutionMode.LIVE);
		eng.start("9.2.1", null, null, ExecutionMode.LIVE);
		assertTrue(eng.arm("9.2.1", "9.2.1 LIVE").success);
		satisfyAllPredecessorsOf(dir, "9.2.1", "release-prepare");
		var res = eng.apply("9.2.1", "release-prepare", Map.of());
		assertTrue(res.success);
		assertEquals(StepStatus.SUCCEEDED, eng.state("9.2.1").step("release-prepare").status);
	}

	@Test
	void safeAllowsMutatingStepWithoutArming(@TempDir Path dir) {
		var eng = engine(dir, ExecutionMode.SAFE);
		eng.start("9.2.1", null);
		satisfyAllPredecessorsOf(dir, "9.2.1", "release-prepare");
		var res = eng.apply("9.2.1", "release-prepare", Map.of());
		assertTrue(res.success, "SAFE simulates the mutating step; no arming required");
	}

	@Test
	void armIsRejectedInSafeMode(@TempDir Path dir) {
		var eng = engine(dir, ExecutionMode.SAFE);
		eng.start("9.2.1", null);
		var res = eng.arm("9.2.1", "9.2.1 LIVE");
		assertFalse(res.success);
		assertFalse(eng.isArmed("9.2.1"));
	}

	@Test
	void liveBoxSafeRunAllowsMutatingWithoutArming(@TempDir Path dir) {
		var eng = engine(dir, ExecutionMode.LIVE);
		eng.start("9.2.1", null); // defaults Dry-run even on a LIVE box
		satisfyAllPredecessorsOf(dir, "9.2.1", "release-prepare");
		var res = eng.apply("9.2.1", "release-prepare", Map.of());
		assertTrue(res.success, "a Dry-run on a LIVE box still simulates; no arming required");
	}

	@Test
	void armIsRejectedOnASafeRunEvenOnALiveBox(@TempDir Path dir) {
		var eng = engine(dir, ExecutionMode.LIVE);
		eng.start("9.2.1", null);
		var res = eng.arm("9.2.1", "9.2.1 LIVE");
		assertFalse(res.success);
		assertFalse(eng.isArmed("9.2.1"));
	}

	@Test
	void armRequiresTheExactConfirmPhrase(@TempDir Path dir) {
		var eng = engine(dir, ExecutionMode.LIVE);
		eng.start("9.2.1", null, null, ExecutionMode.LIVE);
		assertFalse(eng.arm("9.2.1", "yes").success);
		assertFalse(eng.isArmed("9.2.1"));
	}

	@Test
	void armDoesNotSurviveARestart(@TempDir Path dir) {
		var eng = engine(dir, ExecutionMode.LIVE);
		eng.start("9.2.1", null, null, ExecutionMode.LIVE);
		assertTrue(eng.arm("9.2.1", "9.2.1 LIVE").success);
		assertTrue(eng.isArmed("9.2.1"));

		// A fresh engine over the same persisted store models a process restart: arm is in-memory only.
		var restarted = engine(dir, ExecutionMode.LIVE);
		assertFalse(restarted.isArmed("9.2.1"));
		var res = restarted.apply("9.2.1", "release-prepare", Map.of());
		assertFalse(res.success, "must re-arm after a restart");
	}
}
