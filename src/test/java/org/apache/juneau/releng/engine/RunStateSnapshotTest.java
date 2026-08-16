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

class RunStateSnapshotTest {

	@Test
	void projectsVersionStatusRcModeArmedAndOrderedSteps() {
		var rs = RunState.create("9.2.1", "juneau-9.2.1-branch", List.of("preflight", "workspace-setup"));
		rs.rc = 2;
		rs.status = RunStatus.AWAITING_VOTE;
		rs.step("preflight").status = StepStatus.SUCCEEDED;

		var snap = RunStateSnapshot.of(rs, ExecutionMode.LIVE, true);

		assertEquals("9.2.1", snap.version);
		assertEquals(RunStatus.AWAITING_VOTE, snap.status);
		assertEquals(2, snap.rc);
		assertEquals(ExecutionMode.LIVE, snap.mode);
		assertTrue(snap.armed);
		assertEquals(2, snap.steps.size());
		assertEquals("preflight", snap.steps.get(0).stepId);
		assertEquals(StepStatus.SUCCEEDED, snap.steps.get(0).status);
		assertEquals("workspace-setup", snap.steps.get(1).stepId);
		assertEquals(StepStatus.PENDING, snap.steps.get(1).status);
	}

	@Test
	void serializesToTheCompactJsonShapeTheClientPatchesAgainst() {
		var rs = RunState.create("9.2.1", "juneau-9.2.1-branch", List.of("preflight"));
		var snap = RunStateSnapshot.of(rs, ExecutionMode.SAFE, false);

		var json = Json.DEFAULT.write(snap);

		assertTrue(json.contains("\"version\":\"9.2.1\""));
		assertTrue(json.contains("\"status\":\"RUNNING\""));
		assertTrue(json.contains("\"mode\":\"SAFE\""));
		assertTrue(json.contains("\"armed\":false"));
		assertTrue(json.contains("\"stepId\":\"preflight\""));
	}
}
