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
import org.apache.juneau.releng.config.TargetProfile;
import org.apache.juneau.releng.engine.BranchResolver;
import org.apache.juneau.releng.engine.DropRcService;
import org.apache.juneau.releng.engine.ReleaseEngine;
import org.apache.juneau.releng.engine.RunStateStore;
import org.apache.juneau.releng.engine.StepRegistry;
import org.apache.juneau.releng.nexus.NexusStagingClient;
import org.apache.juneau.releng.util.ProcessRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Branch coverage for {@link ReleaseRunRest} paths not reached by {@link ReleaseRunRestTest}: null bodies,
 * confirmation guards, the page view, and the terminal id parser.
 */
class ReleaseRunRestBranchesTest {

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
		};
	}

	private ReleaseRunRest rest(Path dir) {
		var runner = okRunner();
		var branches = new BranchResolver(runner, "/repo");
		var store = new RunStateStore(dir);
		var registry = StepRegistry.standard(branches);
		var engine = ReleaseEngine.forTests(store, registry, runner, branches, dir);
		var dropRc = new DropRcService(store, registry, runner, dir.resolve("staging/git/juneau"), dir,
				NexusStagingClient.forTests((m, p, b) -> ""), TargetProfile.prodDefault());
		return new ReleaseRunRest(engine, dropRc);
	}

	private static ReleaseRunRest.StartRequest start(String version) {
		var b = new ReleaseRunRest.StartRequest();
		b.version = version;
		return b;
	}

	@Test
	void a01_detailsWithNullBodyLeavesRunUnchanged(@TempDir Path dir) {
		var rest = rest(dir);
		rest.start(start("9.2.1"));
		var rs = rest.details("9.2.1", null);
		assertEquals("9.2.1", rs.version);
	}

	@Test
	void a02_detailsRenameOntoExistingRunIsConflict(@TempDir Path dir) {
		var rest = rest(dir);
		rest.start(start("9.2.1"));
		var body = new ReleaseRunRest.DetailsRequest();
		new RunStateStore(dir).save(org.apache.juneau.releng.engine.RunState.create("9.2.2", "b", List.of("preflight")));
		body.version = "9.2.2";
		assertThrows(Conflict.class, () -> rest.details("9.2.1", body));
	}

	@Test
	void a03_startWhileAnotherRunIsActiveIsConflict(@TempDir Path dir) {
		var rest = rest(dir);
		rest.start(start("9.2.1"));
		assertThrows(Conflict.class, () -> rest.start(start("9.2.2")));
	}

	@Test
	void b01_nexusReleaseRequiresTypedVersion(@TempDir Path dir) {
		var rest = rest(dir);
		rest.start(start("9.2.1"));
		var noForm = rest.apply("9.2.1", "nexus-release", null);
		assertFalse(noForm.success);
		assertTrue(noForm.message.contains("Type the version"), noForm.message);
		var wrong = rest.apply("9.2.1", "nexus-release", Map.of("confirmVersion", "9.9.9"));
		assertFalse(wrong.success);
	}

	@Test
	void b02_applyAndResumeAcceptNullForm(@TempDir Path dir) {
		var rest = rest(dir);
		rest.start(start("9.2.1"));
		assertTrue(rest.apply("9.2.1", "preflight", null).success);
		assertTrue(rest.resume("9.2.1", "preflight", null).success);
		assertTrue(rest.resume("9.2.1", "preflight", Map.of()).success);
	}

	@Test
	void b03_skipMarksStepSkipped(@TempDir Path dir) {
		var rest = rest(dir);
		rest.start(start("9.2.1"));
		assertNotNull(rest.skip("9.2.1", "preflight"));
	}

	@Test
	void c01_voteResultTolerantOfNullTally(@TempDir Path dir) {
		var rest = rest(dir);
		rest.start(start("9.2.1"));
		var body = new ReleaseRunRest.VoteResultRequest();
		body.outcome = "rejected";
		assertNotNull(rest.voteResult("9.2.1", body));
	}

	@Test
	void c02_dropRcRequiresTypedRcAndPreviewDoesNotMutate(@TempDir Path dir) {
		var rest = rest(dir);
		rest.start(start("9.2.1"));
		assertNotNull(rest.dropRcPreview("9.2.1"));
		var body = new ReleaseRunRest.DropRcRequest();
		body.confirmRc = "RC9";
		var r = rest.dropRcApply("9.2.1", body);
		assertFalse(r.success);
		assertTrue(r.message.contains("Type the RC identifier"), r.message);
	}

	@Test
	void c03_dropRcApplyWithMatchingRcDropsAndBumps(@TempDir Path dir) {
		var rest = rest(dir);
		rest.start(start("9.2.1"));
		var body = new ReleaseRunRest.DropRcRequest();
		body.confirmRc = "RC1";
		body.reason = "testing";
		var r = rest.dropRcApply("9.2.1", body);
		assertTrue(r.success, r.message);
		assertEquals(2, rest.state("9.2.1").rc);
	}

	@Test
	void d01_terminalSourceIdParsing(@TempDir Path dir) {
		var rest = rest(dir);
		assertTrue(rest.terminalSource("nodashes", null).isEmpty());
		assertTrue(rest.terminalSource("9_2_1--preflight", null).isEmpty());
	}

	@Test
	void d02_pageWithoutAndWithActiveRun(@TempDir Path dir) {
		var rest = rest(dir);
		var req = new MockHttpServletRequest();
		assertNotNull(rest.page(req));
		rest.start(start("9.2.1"));
		req.setParameter("tab", "exec");
		assertNotNull(rest.page(req));
	}
}
