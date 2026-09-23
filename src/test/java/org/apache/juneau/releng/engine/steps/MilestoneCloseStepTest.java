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

import static org.apache.juneau.test.bct.BctAssertions.assertSize;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.juneau.releng.config.TargetProfile;
import org.apache.juneau.releng.engine.RunState;
import org.apache.juneau.releng.engine.StepContext;
import org.apache.juneau.releng.util.ProcessRunner;
import org.junit.jupiter.api.Test;

/** §5.21/§8.1: milestoneNumber (New-Release form field) drives the gh api PATCH; null legitimately no-ops. */
class MilestoneCloseStepTest {

	private final List<List<String>> calls = new ArrayList<>();
	private final List<String> logLines = new ArrayList<>();

	private StepContext ctx(Integer milestoneNumber) {
		var c = new StepContext();
		c.run = RunState.create("9.2.1", "b", List.of("milestone-close"));
		c.run.milestoneNumber = milestoneNumber;
		c.target = TargetProfile.prodDefault();
		c.githubToken = "tok";
		c.formInputs = Map.of();
		c.log = logLines::add;
		c.runner = new ProcessRunner() {
			@Override
			public List<String> runLines(List<String> x) {
				return List.of();
			}

			@Override
			public String runText(List<String> x) {
				return "";
			}

			@Override
			public ProcResult run(List<String> x, String s, Map<String, String> e) {
				calls.add(x);
				return new ProcResult(0, "");
			}

			@Override
			public ProcResult run(List<String> x, String s, Map<String, String> e,
					java.util.function.Consumer<String> k) {
				return run(x, s, e);
			}
		};
		return c;
	}

	@Test
	void a01_noMilestoneNumberLegitimatelyNoOps() {
		var res = new MilestoneCloseStep().apply(ctx(null));
		assertTrue(res.success);
		assertSize(0, calls);
	}

	@Test
	void a02_liveClosesResolvedMilestoneNumber() {
		var res = new MilestoneCloseStep().apply(ctx(13));
		assertTrue(res.success, res.message);
		var cmd = calls.get(0);
		assertTrue(cmd.contains("gh"));
		assertTrue(cmd.contains("repos/apache/juneau/milestones/13"));
		assertTrue(cmd.contains("state=closed"));
	}

}
