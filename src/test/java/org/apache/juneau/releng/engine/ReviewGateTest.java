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

/** A review-gate step holds at {@code AWAITING_REVIEW} after its read-only work runs, until confirm-review. */
class ReviewGateTest {

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

	// Marks every step ahead of stepId SUCCEEDED so the forward-apply guard (ReleaseEngine) doesn't refuse
	// to run stepId in isolation; this test is about the review-gate hold, not the whole pipeline.
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
	void a01_reviewGateStepHoldsUntilConfirmed(@TempDir Path dir) {
		var eng = engine(dir);
		eng.start("9.2.1", null);
		satisfyAllPredecessorsOf(dir, "9.2.1", "javadoc-verify");

		var res = eng.apply("9.2.1", "javadoc-verify", Map.of());
		assertTrue(res.success);
		assertEquals(StepStatus.AWAITING_REVIEW, eng.state("9.2.1").step("javadoc-verify").status);

		var confirmed = eng.confirmReview("9.2.1", "javadoc-verify");
		assertTrue(confirmed.success);
		assertEquals(StepStatus.SUCCEEDED, eng.state("9.2.1").step("javadoc-verify").status);
	}

	@Test
	void a02_confirmReviewOnAStepNotAwaitingReviewFails(@TempDir Path dir) {
		var eng = engine(dir);
		eng.start("9.2.1", null);
		eng.apply("9.2.1", "preflight", Map.of()); // not a review gate; goes straight to SUCCEEDED

		var res = eng.confirmReview("9.2.1", "preflight");
		assertFalse(res.success);
		assertTrue(res.message.toLowerCase().contains("not awaiting review"));
	}
}
