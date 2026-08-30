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
import org.junit.jupiter.api.Test;

class StepRegistryTest {

	private StepRegistry registry() {
		// Steps needing services can take nulls here — the registry test only checks id order + lookup.
		return StepRegistry.standard(new BranchResolver(null, "/repo"));
	}

	@Test
	void a01_hasTwentyFourStepsInSpecOrder() {
		var ids = registry().ids();
		assertEquals(24, ids.size());
		assertEquals("preflight", ids.get(0));
		assertEquals("compose-propose-email", ids.get(1));
		assertEquals("workspace-setup", ids.get(2));
		assertEquals("finalize-run", ids.get(23));
		assertEquals("vote-gate", ids.get(14));
		assertEquals("tally-vote-result", ids.get(15));
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
		assertEquals(List.of("preflight", "compose-propose-email"), ids.subList(0, 2));
		assertEquals("workspace-setup", ids.get(2));
	}
}
