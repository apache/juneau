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

package org.apache.juneau.releng.rest;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.apache.juneau.http.response.Conflict;
import org.apache.juneau.http.response.NotFound;
import org.apache.juneau.releng.config.TargetProfile;
import org.apache.juneau.releng.engine.BranchResolver;
import org.apache.juneau.releng.engine.DropRcService;
import org.apache.juneau.releng.engine.ReleaseEngine;
import org.apache.juneau.releng.engine.RunStateStore;
import org.apache.juneau.releng.engine.StepRegistry;
import org.apache.juneau.releng.log.LogBroadcaster;
import org.apache.juneau.releng.nexus.NexusStagingClient;
import org.apache.juneau.releng.util.ProcessRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Direct unit tests of {@link ReleaseRunRest}'s no-such-run/single-active-run status mapping.
 *
 * <p>{@code juneau-rest-mock} is not a dependency of this module and no sibling REST test uses it (verified
 * via {@code mvn dependency:tree} and a repo-wide search), so per the task's fallback instruction these tests
 * call the {@code @RestGet}/{@code @RestPost} methods directly rather than pulling in a new test dependency
 * for an in-process HTTP round trip.
 */
class ReleaseRunRestTest {

	private ProcessRunner okRunner() {
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
				if (c.contains("ls-remote"))
					return new ProcResult(0, "sha\trefs/heads/juneau-9.2.1-branch\n");
				return new ProcResult(0, "ok");
			}

			@Override
			public ProcResult run(List<String> c, String s, Map<String, String> e,
					java.util.function.Consumer<String> k) {
				return run(c, s, e);
			}
		};
	}

	private ReleaseRunRest rest(Path dir) {
		var runner = okRunner();
		var branches = new BranchResolver(runner, "/repo");
		var store = new RunStateStore(dir);
		var registry = StepRegistry.standard(branches);
		var engine = ReleaseEngine.forTests(store, registry, runner, branches, dir);
		var dropRc = new DropRcService(store, registry, runner, dir.resolve("staging/git/juneau"), dir,
				NexusStagingClient.forTests((m, p, b) -> ""), engine.mode(), engine::isArmed,
				TargetProfile.prodDefault(), (v, s) -> new LogBroadcaster());
		return new ReleaseRunRest(engine, dropRc);
	}

	private ReleaseRunRest.StartRequest startRequest(String version) {
		var body = new ReleaseRunRest.StartRequest();
		body.version = version;
		return body;
	}

	@Test
	void getStateForNonexistentRunIs404(@TempDir Path dir) {
		var rest = rest(dir);
		var ex = assertThrows(NotFound.class, () -> rest.state("9.9.9"));
		assertEquals(404, ex.getStatusCode());
	}

	@Test
	void previewAgainstNonexistentRunIs404(@TempDir Path dir) {
		var rest = rest(dir);
		Map<String, String> form = Map.of();
		var ex = assertThrows(NotFound.class, () -> rest.preview("9.9.9", "preflight", form));
		assertEquals(404, ex.getStatusCode());
	}

	@Test
	void applyAgainstNonexistentRunIs404(@TempDir Path dir) {
		var rest = rest(dir);
		Map<String, String> form = Map.of();
		var ex = assertThrows(NotFound.class, () -> rest.apply("9.9.9", "preflight", form));
		assertEquals(404, ex.getStatusCode());
	}

	@Test
	void skipAgainstNonexistentRunIs404(@TempDir Path dir) {
		var rest = rest(dir);
		var ex = assertThrows(NotFound.class, () -> rest.skip("9.9.9", "preflight"));
		assertEquals(404, ex.getStatusCode());
	}

	@Test
	void dropRcPreviewAgainstNonexistentRunIs404(@TempDir Path dir) {
		var rest = rest(dir);
		var ex = assertThrows(NotFound.class, () -> rest.dropRcPreview("9.9.9"));
		assertEquals(404, ex.getStatusCode());
	}

	@Test
	void previewAgainstRealRunStillWorks(@TempDir Path dir) {
		var rest = rest(dir);
		rest.start(startRequest("9.2.1"));
		var preview = rest.preview("9.2.1", "preflight", Map.of());
		assertNotNull(preview);
	}

	@Test
	void startPersistsFormSuppliedMilestoneNumber(@TempDir Path dir) {
		var rest = rest(dir);
		var body = startRequest("9.2.1");
		body.milestoneNumber = 42;
		var rs = rest.start(body);
		assertEquals(42, rs.milestoneNumber);
	}

	@Test
	void secondConcurrentStartIs409(@TempDir Path dir) {
		var rest = rest(dir);
		rest.start(startRequest("9.2.1"));

		var second = startRequest("9.2.2");
		var ex = assertThrows(Conflict.class, () -> rest.start(second));
		assertEquals(409, ex.getStatusCode());
		assertTrue(ex.getMessage().contains("9.2.1"));
	}
}
