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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.apache.juneau.releng.engine.RunState;
import org.apache.juneau.releng.engine.RunStateStore;
import org.apache.juneau.releng.engine.StepStatus;
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
	void a03_servesTheStepLogAsBytesAndKeepsOneSourcePerLog(@TempDir Path dir) throws Exception {
		Files.createDirectories(dir.resolve("logs"));
		Files.writeString(dir.resolve("logs/step.log"), "first\r\nsecond\r\n");
		var sources = new StepOutputSources(storeWithRun(dir, StepStatus.SUCCEEDED, "logs/step.log"));

		var src = sources.find("9.2.1", "preflight").orElseThrow();
		var chunk = src.read(0, 1000);

		assertEquals("first\r\nsecond\r\n", new String(chunk.bytes(), StandardCharsets.UTF_8));
		assertTrue(chunk.done());
		assertSame(src, sources.find("9.2.1", "preflight").orElseThrow());
	}

	@Test
	void b01_settledFollowsTheStoredStepState(@TempDir Path dir) {
		var expect = Map.of(
			StepStatus.PENDING, false,
			StepStatus.RUNNING, false,
			StepStatus.SUCCEEDED, true,
			StepStatus.FAILED, true,
			StepStatus.SKIPPED, true,
			StepStatus.AWAITING_VOTE, true,
			StepStatus.AWAITING_REVIEW, true);
		for (var e : expect.entrySet()) {
			var sources = new StepOutputSources(storeWithRun(dir, e.getKey(), "logs/step.log"));
			assertEquals(e.getValue(), sources.settled("9.2.1", "preflight"), e.getKey().name());
		}
	}

	@Test
	void b02_aStepThatNoLongerExistsIsSettled(@TempDir Path dir) {
		var sources = new StepOutputSources(storeWithRun(dir, StepStatus.RUNNING, "logs/step.log"));
		assertTrue(sources.settled("9.2.1", "not-a-step"));
		assertTrue(sources.settled("nope", "preflight"));
	}
}
