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
import java.util.List;
import org.apache.juneau.marshall.marshaller.Json;
import org.junit.jupiter.api.Test;

class RunStateTest {

	@Test
	void newRunSeedsAllStepsPending() {
		var rs = RunState.create("9.2.1", "juneau-9.2.1-branch",
				List.of("preflight", "compose-propose-email", "workspace-setup"));
		assertEquals("9.2.1", rs.version);
		assertEquals(1, rs.rc);
		assertEquals(RunStatus.RUNNING, rs.status);
		assertEquals(3, rs.steps.size());
		assertTrue(rs.steps.stream().allMatch(s -> s.status == StepStatus.PENDING));
	}

	@Test
	void stepLookupByIdWorks() {
		var rs = RunState.create("9.2.1", "b", List.of("preflight", "workspace-setup"));
		assertEquals(StepStatus.PENDING, rs.step("workspace-setup").status);
		assertNull(rs.step("nope"));
	}

	@Test
	void jsonRoundTripPreservesStepsAndHistory() {
		var rs = RunState.create("9.2.1", "juneau-9.2.1-branch", List.of("preflight", "workspace-setup"));
		rs.rc = 2;
		rs.rcHistory.add(new RcHistoryEntry(1, "2026-08-15T14:02:11Z", "vote rejected"));
		rs.step("preflight").status = StepStatus.SUCCEEDED;
		rs.nexusRepoId = "orgapachejuneau-1042";

		var json = Json.DEFAULT.write(rs);
		var back = Json.DEFAULT.read(json, RunState.class);

		assertEquals(2, back.rc);
		assertEquals(1, back.rcHistory.size());
		assertEquals("vote rejected", back.rcHistory.get(0).reason);
		assertEquals(StepStatus.SUCCEEDED, back.step("preflight").status);
		assertEquals("orgapachejuneau-1042", back.nexusRepoId);
	}

}
