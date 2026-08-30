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
import java.util.Map;
import org.apache.juneau.releng.util.ProcessRunner;
import org.junit.jupiter.api.Test;

class BranchResolverTest {

	/** Stub whose run() returns a canned ProcResult regardless of args. */
	private ProcessRunner runner(int exit, String out) {
		return new ProcessRunner() {
			@Override
			public List<String> runLines(List<String> c) {
				return List.of();
			}

			@Override
			public String runText(List<String> c) {
				return out;
			}

			@Override
			public ProcResult run(List<String> c, String stdin, Map<String, String> env) {
				return new ProcResult(exit, out);
			}
		};
	}

	@Test
	void a01_newMinorReleasesResolveToMaster() {
		var r = new BranchResolver(runner(0, "<sha>\trefs/heads/master\n"), "/repo");
		assertEquals("master", r.resolve("10.0.0"));
	}

	@Test
	void a02_maintenanceReleasesResolveToVersionBranch() {
		var r = new BranchResolver(runner(0, "<sha>\trefs/heads/juneau-9.2.1-branch\n"), "/repo");
		assertEquals("juneau-9.2.1-branch", r.resolve("9.2.1"));
	}

	@Test
	void b01_existsWhenLsRemoteReturnsARef() {
		var r = new BranchResolver(runner(0, "abc123\trefs/heads/juneau-9.2.1-branch\n"), "/repo");
		assertTrue(r.remoteBranchExists("juneau-9.2.1-branch"));
	}

	@Test
	void b02_missingWhenLsRemoteEmpty() {
		var r = new BranchResolver(runner(0, ""), "/repo");
		assertFalse(r.remoteBranchExists("juneau-9.9.9-branch"));
	}

	@Test
	void c01_missingBranchInstructionsNameTheBranch() {
		var r = new BranchResolver(runner(0, ""), "/repo");
		var msg = r.missingBranchInstructions("juneau-9.9.9-branch");
		assertTrue(msg.contains("juneau-9.9.9-branch"));
		assertTrue(msg.contains("does not create maintenance branches"));
	}
}
