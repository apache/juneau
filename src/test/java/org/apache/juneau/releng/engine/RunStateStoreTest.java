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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RunStateStoreTest {

	@Test
	void savesAndLoadsByVersion(@TempDir Path dir) {
		var store = new RunStateStore(dir);
		var rs = RunState.create("9.2.1", "juneau-9.2.1-branch", List.of("preflight", "workspace-setup"));
		store.save(rs);

		assertTrue(dir.resolve("release-9.2.1.json").toFile().isFile());
		var back = store.load("9.2.1").orElseThrow();
		assertEquals("juneau-9.2.1-branch", back.branch);
		assertEquals(2, back.steps.size());
	}

	@Test
	void loadAllReturnsEveryRelease(@TempDir Path dir) {
		var store = new RunStateStore(dir);
		store.save(RunState.create("9.2.1", "b1", List.of("preflight")));
		store.save(RunState.create("9.2.2", "b2", List.of("preflight")));
		var all = store.loadAll();
		assertEquals(2, all.size());
		assertTrue(all.stream().anyMatch(r -> r.version.equals("9.2.1")));
		assertTrue(all.stream().anyMatch(r -> r.version.equals("9.2.2")));
	}

	@Test
	void loadMissingIsEmpty(@TempDir Path dir) {
		assertTrue(new RunStateStore(dir).load("9.9.9").isEmpty());
	}

	@Test
	void activeRunIsTheRunningOrAwaitingVoteOne(@TempDir Path dir) {
		var store = new RunStateStore(dir);
		var released = RunState.create("9.2.0", "b0", List.of("preflight"));
		released.status = RunStatus.RELEASED;
		store.save(released);
		var running = RunState.create("9.2.1", "b1", List.of("preflight"));
		store.save(running);
		assertEquals("9.2.1", store.activeRun().orElseThrow().version);
	}

	@Test
	void displayRunFallsBackToAFailedRunSoTheRailStaysVisible(@TempDir Path dir) {
		var store = new RunStateStore(dir);
		var released = RunState.create("9.2.0", "b0", List.of("preflight"));
		released.status = RunStatus.RELEASED;
		store.save(released);
		var failed = RunState.create("9.2.1", "b1", List.of("preflight"));
		failed.status = RunStatus.FAILED;
		store.save(failed);
		assertTrue(store.activeRun().isEmpty(), "FAILED is not a start-lock; a new version can still begin");
		assertEquals("9.2.1", store.displayRun().orElseThrow().version);
	}

	@Test
	void displayRunPrefersAnActiveRunOverAFailedOne(@TempDir Path dir) {
		var store = new RunStateStore(dir);
		var failed = RunState.create("9.2.0", "b0", List.of("preflight"));
		failed.status = RunStatus.FAILED;
		store.save(failed);
		var running = RunState.create("9.2.1", "b1", List.of("preflight"));
		store.save(running);
		assertEquals("9.2.1", store.displayRun().orElseThrow().version);
	}

	@Test
	void saveIsANoOpTowardTheOnSaveHookByDefault(@TempDir Path dir) {
		// No hook installed: save() must not throw just because nothing is listening.
		var store = new RunStateStore(dir);
		assertDoesNotThrow(() -> store.save(RunState.create("9.2.1", "b1", List.of("preflight"))));
	}

	@Test
	void setOnSaveIsInvokedWithTheJustSavedRunOnEverySave(@TempDir Path dir) {
		var store = new RunStateStore(dir);
		// rs is mutated in place across the two save() calls below, so the hook must capture each step's
		// status at call time (an enum value, immutable) rather than a reference into the shared RunState.
		var seenStatuses = new ArrayList<StepStatus>();
		store.setOnSave(rs -> seenStatuses.add(rs.step("preflight").status));

		var rs = RunState.create("9.2.1", "b1", List.of("preflight"));
		store.save(rs);
		rs.step("preflight").status = StepStatus.SUCCEEDED;
		store.save(rs);

		assertEquals(List.of(StepStatus.PENDING, StepStatus.SUCCEEDED), seenStatuses);
	}

	@Test
	void setOnSaveWithNullRestoresTheNoOpDefault(@TempDir Path dir) {
		var store = new RunStateStore(dir);
		var seen = new ArrayList<RunState>();
		store.setOnSave(seen::add);
		store.setOnSave(null);

		store.save(RunState.create("9.2.1", "b1", List.of("preflight")));

		assertTrue(seen.isEmpty());
	}
}
