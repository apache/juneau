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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.juneau.releng.engine.ExecutionMode;
import org.apache.juneau.releng.engine.RunState;
import org.apache.juneau.releng.engine.StepContext;
import org.apache.juneau.releng.util.ProcessRunner;
import org.junit.jupiter.api.Test;

/**
 * Tier-B fidelity: a mutating step run in SAFE mode must spawn <b>zero</b> real subprocesses and instead
 * emit a redacted "would run:" line carrying the correctly-built command.
 */
class SafeModeTierBTest {

	private final int[] runCount = { 0 };
	private final List<String> logLines = new ArrayList<>();

	private ProcessRunner countingRunner() {
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
				runCount[0]++;
				return new ProcResult(0, "");
			}

			@Override
			public ProcResult run(List<String> c, String s, Map<String, String> e,
					java.util.function.Consumer<String> k) {
				runCount[0]++;
				return new ProcResult(0, "");
			}
		};
	}

	private StepContext safeCtx() {
		var c = new StepContext();
		c.mode = ExecutionMode.SAFE;
		c.run = RunState.create("9.2.1", "juneau-9.2.1-branch", List.of("release-prepare"));
		c.runner = countingRunner();
		c.stagingRepo = Path.of("/staging/git/juneau");
		c.formInputs = Map.of();
		c.log = logLines::add;
		return c;
	}

	@Test
	void safeMutatingStepSpawnsNothingButLogsBuiltCommand() {
		var res = new ReleasePrepareStep().apply(safeCtx());

		assertTrue(res.success, "SAFE simulation should synthesize success");
		assertEquals(0, runCount[0], "no real subprocess may be spawned in SAFE");

		var wouldRun = logLines.stream().filter(l -> l.startsWith("would run:")).findFirst().orElseThrow();
		assertTrue(wouldRun.contains("mvn"));
		assertTrue(wouldRun.contains("release:prepare"));
		assertTrue(wouldRun.contains("-Dtag=juneau-9.2.1-RC1"));
		assertTrue(wouldRun.contains("-DdevelopmentVersion=9.2.2-SNAPSHOT"));
	}
}
