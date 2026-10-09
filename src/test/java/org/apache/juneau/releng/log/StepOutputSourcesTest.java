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
package org.apache.juneau.releng.log;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.apache.juneau.releng.engine.RunState;
import org.apache.juneau.releng.engine.RunStateStore;
import org.apache.juneau.releng.engine.StepStatus;
import org.apache.juneau.rest.server.views.ConsoleOutputLine.Style;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@SuppressWarnings({
	"java:S1192" // Fixture version and step ids read more clearly inline than as constants.
})
class StepOutputSourcesTest {

	private RunStateStore storeWithRun(Path dir, StepStatus status, String logRef) {
		var store = new RunStateStore(dir);
		var rs = RunState.create("9.2.1", "juneau-9.2.1-branch", List.of("preflight", "workspace-setup"));
		var step = rs.step("preflight");
		step.status = status;
		step.logRef = logRef;
		step.startedAt = "2026-10-08T10:00:00Z";
		if (status != StepStatus.RUNNING && status != StepStatus.PENDING)
			step.completedAt = "2026-10-08T10:01:23Z";
		store.save(rs);
		return store;
	}

	@Test
	void a01_unknownRunStepOrNeverWrittenLogHasNoSource(@TempDir Path dir) {
		var store = storeWithRun(dir, StepStatus.PENDING, null);
		var sources = new StepOutputSources(store);
		assertTrue(sources.find("nope", "preflight").isEmpty());
		assertTrue(sources.find("9.2.1", "not-a-step").isEmpty());
		assertTrue(sources.find("9.2.1", "preflight").isEmpty(), "no logRef until the step first runs");
	}

	@Test
	void a02_logRefEscapingTheStateDirIsRefused(@TempDir Path dir) throws Exception {
		var outside = Files.writeString(dir.getParent().resolve("outside-" + dir.getFileName() + ".log"), "secret\n");
		var store = storeWithRun(dir, StepStatus.SUCCEEDED, "../" + outside.getFileName());
		assertTrue(new StepOutputSources(store).find("9.2.1", "preflight").isEmpty());
	}

	@Test
	void a03_servesTheStepLogAsLinesAndKeepsOneSourcePerLog(@TempDir Path dir) throws Exception {
		Files.createDirectories(dir.resolve("logs"));
		Files.writeString(dir.resolve("logs/step.log"), "first\nsecond\n");
		var sources = new StepOutputSources(storeWithRun(dir, StepStatus.SUCCEEDED, "logs/step.log"));

		var src = sources.find("9.2.1", "preflight").orElseThrow();
		var page = src.page(null, 100);

		var contract = page.toContractMap();
		assertEquals(2, ((List<?>)contract.get("lines")).size());
		assertEquals(Boolean.TRUE, contract.get("terminal"));
		assertSame(src, sources.find("9.2.1", "preflight").orElseThrow());
	}

	@Test
	void b01_statusFollowsTheStoredStepState(@TempDir Path dir) {
		var expect = Map.of(
			StepStatus.PENDING, List.of("PENDING", false),
			StepStatus.RUNNING, List.of("RUNNING", false),
			StepStatus.SUCCEEDED, List.of("SUCCEEDED", true),
			StepStatus.FAILED, List.of("FAILED", true),
			StepStatus.SKIPPED, List.of("SKIPPED", true),
			StepStatus.AWAITING_VOTE, List.of("AWAITING VOTE", true),
			StepStatus.AWAITING_REVIEW, List.of("AWAITING REVIEW", true));
		for (var e : expect.entrySet()) {
			var sources = new StepOutputSources(storeWithRun(dir, e.getKey(), "logs/step.log"));
			var st = sources.status("9.2.1", "preflight");
			assertEquals(e.getValue().get(0), st.state(), e.getKey().name());
			assertEquals(e.getValue().get(1), st.terminal(), e.getKey().name());
		}
	}

	@Test
	void b02_settledStepReportsStyleAndServerDuration(@TempDir Path dir) {
		var st = new StepOutputSources(storeWithRun(dir, StepStatus.FAILED, "logs/step.log")).status("9.2.1", "preflight");
		assertEquals(Style.ERROR, st.stateStyle());
		assertEquals(Instant.parse("2026-10-08T10:00:00Z"), st.startedAt());
		assertEquals(83_000L, st.durationMs());
	}

	@Test
	void b03_runningStepHasNoDurationYet(@TempDir Path dir) {
		var st = new StepOutputSources(storeWithRun(dir, StepStatus.RUNNING, "logs/step.log")).status("9.2.1", "preflight");
		assertEquals(Style.ACCENT, st.stateStyle());
		assertNull(st.durationMs());
	}

	@Test
	void b04_unparseableTimestampsDegradeToNoDuration(@TempDir Path dir) {
		var store = storeWithRun(dir, StepStatus.SUCCEEDED, "logs/step.log");
		var rs = store.load("9.2.1").orElseThrow();
		rs.step("preflight").startedAt = "not-a-time";
		store.save(rs);
		var st = new StepOutputSources(store).status("9.2.1", "preflight");
		assertNull(st.startedAt());
		assertNull(st.durationMs());
	}
}
