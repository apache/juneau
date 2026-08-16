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
import org.apache.juneau.releng.engine.RunState;
import org.apache.juneau.releng.engine.StepContext;
import org.apache.juneau.releng.nexus.NexusStagingClient;
import org.apache.juneau.releng.util.ProcessRunner;
import org.junit.jupiter.api.Test;

class NexusStagingCloseStepTest {

	private StepContext ctx(NexusStagingClient nexus, Map<String, String> form) {
		var c = new StepContext();
		c.run = RunState.create("9.2.1", "b", List.of("nexus-staging-close"));
		c.nexus = nexus;
		c.formInputs = form;
		c.log = s -> {
		};
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
				return new ProcResult(0, "");
			}
		};
		return c;
	}

	@Test
	void discoversAndRecordsRepoIdThenCloses() {
		var json = "[{\"repositoryId\":\"orgapachejuneau-1042\",\"type\":\"open\",\"created\":\"2026-08-14\"}]";
		var nexus = NexusStagingClient.forTests((m, p, b) -> json);
		var c = ctx(nexus, Map.of());
		var res = new NexusStagingCloseStep().apply(c);
		assertTrue(res.success);
		assertEquals("orgapachejuneau-1042", c.run.nexusRepoId);
	}

	@Test
	void manualOverrideWins() {
		var nexus = NexusStagingClient.forTests((m, p, b) -> "[]");
		var c = ctx(nexus, Map.of("repoIdOverride", "orgapachejuneau-9999"));
		var res = new NexusStagingCloseStep().apply(c);
		assertTrue(res.success);
		assertEquals("orgapachejuneau-9999", c.run.nexusRepoId);
	}
}
