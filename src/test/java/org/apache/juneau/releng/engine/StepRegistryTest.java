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
import org.junit.jupiter.api.Test;

class StepRegistryTest {

	private StepRegistry registry() {
		// Steps needing services can take nulls here — the registry test only checks id order + lookup.
		return StepRegistry.standard(new BranchResolver(null, "/repo"));
	}

	@Test
	void a01_hasTwentyThreeStepsInSpecOrder() {
		var ids = registry().ids();
		assertBean(ids, "length,0,1,2,14,15,22",
			"23,preflight,compose-propose-email,workspace-setup,vote-gate,tally-vote-result,finalize-run");
		assertFalse(ids.contains("milestone-close"));
	}

	@Test
	void a02_lookupByIdWorks() {
		assertEquals("build-verify", registry().byId("build-verify").id());
		assertNull(registry().byId("nope"));
	}

	@Test
	void a03_dropRcResetRangeStartsAtWorkspaceSetup() {
		var ids = registry().ids();
		// Steps 0-1 kept; reset from index 2 (workspace-setup) onward.
		assertBean(ids, "0,1,2", "preflight,compose-propose-email,workspace-setup");
	}
}
