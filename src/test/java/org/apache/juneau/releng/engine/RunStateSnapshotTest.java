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

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.apache.juneau.marshall.marshaller.Json;
import org.junit.jupiter.api.Test;

class RunStateSnapshotTest {

	@Test
	void a01_projectsVersionStatusRcAndOrderedSteps() {
		var rs = RunState.create("9.2.1", "juneau-9.2.1-branch", List.of("preflight", "workspace-setup"));
		rs.rc = 2;
		rs.status = RunStatus.AWAITING_VOTE;
		rs.step("preflight").status = StepStatus.SUCCEEDED;

		var snap = RunStateSnapshot.of(rs);

		assertBean(snap, "version,status,rc", "9.2.1,AWAITING_VOTE,2");
		assertBeans(snap.steps, "stepId,status", "preflight,SUCCEEDED", "workspace-setup,PENDING");
	}

	@Test
	void a02_serializesToTheCompactJsonShapeTheClientPatchesAgainst() {
		var rs = RunState.create("9.2.1", "juneau-9.2.1-branch", List.of("preflight"));
		var snap = RunStateSnapshot.of(rs);

		var json = Json.DEFAULT.write(snap);

		assertTrue(json.contains("\"version\":\"9.2.1\""));
		assertTrue(json.contains("\"status\":\"RUNNING\""));
		assertTrue(json.contains("\"rc\":1"));
		assertFalse(json.contains("\"armed\""));
		assertTrue(json.contains("\"stepId\":\"preflight\""));
	}
}
