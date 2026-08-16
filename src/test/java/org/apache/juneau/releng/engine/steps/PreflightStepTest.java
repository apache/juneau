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
import java.util.List;
import java.util.Map;
import org.apache.juneau.releng.engine.BranchResolver;
import org.apache.juneau.releng.engine.RunState;
import org.apache.juneau.releng.engine.StepContext;
import org.apache.juneau.releng.util.ProcessRunner;
import org.junit.jupiter.api.Test;

class PreflightStepTest {

	private ProcessRunner runner(String lsRemoteOut) {
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
				// git ls-remote for branch check
				if (c.contains("ls-remote"))
					return new ProcResult(0, lsRemoteOut);
				return new ProcResult(0, "ok"); // version probes
			}

			@Override
			public ProcResult run(List<String> c, String s, Map<String, String> e,
					java.util.function.Consumer<String> sink) {
				return run(c, s, e);
			}
		};
	}

	private StepContext ctx(ProcessRunner r) {
		var c = new StepContext();
		c.run = RunState.create("9.2.1", "juneau-9.2.1-branch", List.of("preflight"));
		c.runner = r;
		c.log = s -> {
		};
		c.repoDir = "/repo";
		c.formInputs = Map.of();
		return c;
	}

	@Test
	void previewPassesWhenBranchExists() {
		var step = new PreflightStep(new BranchResolver(runner("sha\trefs/heads/juneau-9.2.1-branch\n"), "/repo"));
		var c = ctx(runner("sha\trefs/heads/juneau-9.2.1-branch\n"));
		var p = step.preview(c);
		assertTrue(p.lines.stream().anyMatch(l -> l.contains("juneau-9.2.1-branch")));
		assertTrue(step.apply(c).success);
	}

	@Test
	void applyFailsWithInstructionsWhenBranchMissing() {
		var step = new PreflightStep(new BranchResolver(runner(""), "/repo"));
		var c = ctx(runner(""));
		var res = step.apply(c);
		assertFalse(res.success);
		assertTrue(res.message.contains("does not create maintenance branches"));
	}
}
