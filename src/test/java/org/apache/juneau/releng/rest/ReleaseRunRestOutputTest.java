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
import org.apache.juneau.commons.inject.StackOverlay;
import org.apache.juneau.releng.config.TargetProfile;
import org.apache.juneau.releng.engine.BranchResolver;
import org.apache.juneau.releng.engine.DropRcService;
import org.apache.juneau.releng.engine.ReleaseEngine;
import org.apache.juneau.releng.engine.RunStateStore;
import org.apache.juneau.releng.engine.StepRegistry;
import org.apache.juneau.releng.nexus.NexusStagingClient;
import org.apache.juneau.releng.util.ProcessRunner;
import org.apache.juneau.rest.mock.MockRestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Serves a step's console output through the console-output lines and download endpoints, and checks the New Release
 * page wires the console-output region instead of its own log renderer.
 */
class ReleaseRunRestOutputTest {

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
			public ProcResult run(List<String> c, String s, Map<String, String> e, java.util.function.Consumer<String> k) {
				return run(c, s, e);
			}
		};
	}

	private ReleaseEngine engine;

	@SuppressWarnings({
		"resource" // Caller owns and closes the returned MockRestClient (via try-with-resources).
	})
	private MockRestClient client(Path dir) {
		var runner = okRunner();
		var branches = new BranchResolver(runner, "/repo");
		var store = new RunStateStore(dir);
		var registry = StepRegistry.standard(branches);
		engine = ReleaseEngine.forTests(store, registry, runner, branches, dir);
		var dropRc = new DropRcService(store, registry, runner, dir.resolve("staging/git/juneau"), dir,
				NexusStagingClient.forTests((m, p, b) -> ""), TargetProfile.prodDefault());
		return MockRestClient.builder(new ReleaseRunRest(engine, dropRc)).overridingBeanStore(new StackOverlay()).build();
	}

	@Test
	void a01_linesEndpointServesTheStepLogInTheConsoleOutputContract(@TempDir Path dir) throws Exception {
		try (var client = client(dir)) {
			engine.start("9.2.1", null);
			engine.apply("9.2.1", "preflight", Map.of());
			try (var resp = client.request("GET", "/9.2.1/steps/preflight/output/lines").run()) {
				assertEquals(200, resp.getStatusCode());
				var body = resp.getBodyAsString();
				assertTrue(body.contains("\"contractVersion\":\"1\""), body);
				assertTrue(body.contains("Resolved target branch"), body);
				assertTrue(body.contains("\"terminal\":true"), body);
				assertTrue(body.contains("SUCCEEDED"), body);
			}
		}
	}

	@Test
	void a02_unknownRunOrStepIs404(@TempDir Path dir) throws Exception {
		try (var client = client(dir)) {
			engine.start("9.2.1", null);
			try (var resp = client.request("GET", "/9.9.9/steps/preflight/output/lines").run()) {
				assertEquals(404, resp.getStatusCode());
			}
			try (var resp = client.request("GET", "/9.2.1/steps/not-a-step/output/lines").run()) {
				assertEquals(404, resp.getStatusCode());
			}
			try (var resp = client.request("GET", "/9.2.1/steps/preflight/output/lines").run()) {
				assertEquals(404, resp.getStatusCode(), "a step that has not run has no log yet");
			}
		}
	}

	@Test
	void a03_downloadIsTheWholeLogAsJsonl(@TempDir Path dir) throws Exception {
		try (var client = client(dir)) {
			engine.start("9.2.1", null);
			engine.apply("9.2.1", "preflight", Map.of());
			try (var resp = client.request("GET", "/9.2.1/steps/preflight/output/download").run()) {
				assertEquals(200, resp.getStatusCode());
				assertEquals("application/jsonl", resp.header("Content-Type").orElse(""));
				assertTrue(resp.header("Content-Disposition").orElse("").contains(".jsonl"));
				assertTrue(resp.getBodyAsString().contains("Resolved target branch"));
			}
		}
	}

	@Test
	void c01_runViewEventsEndpointServesTheRunsEvents(@TempDir Path dir) throws Exception {
		try (var client = client(dir)) {
			engine.start("9.2.1", null);
			engine.apply("9.2.1", "preflight", Map.of());
			try (var resp = client.request("GET", "/juneau-run-view/9_2_1/events").run()) {
				assertEquals(200, resp.getStatusCode());
				var body = resp.getBodyAsString();
				assertTrue(body.contains("\"preflight\""), body);
				assertTrue(body.contains("\"end\""), body);
			}
		}
	}

	@Test
	void c02_runViewEventsForAnUnknownRunIs404(@TempDir Path dir) throws Exception {
		try (var client = client(dir)) {
			engine.start("9.2.1", null);
			try (var resp = client.request("GET", "/juneau-run-view/9_9_9/events").run()) {
				assertEquals(404, resp.getStatusCode());
			}
		}
	}

	@Test
	void b01_pageLoadsTheViewsToolkitAndNoLongerOwnsALogRenderer(@TempDir Path dir) throws Exception {
		try (var client = client(dir)) {
			engine.start("9.2.1", null);
			try (var resp = client.request("GET", "/?tab=exec").run()) {
				assertEquals(200, resp.getStatusCode());
				var body = resp.getBodyAsString();
				assertTrue(body.contains("juneau-console-output.js"), "the views toolkit must be loaded: " + body);
			}
		}
		for (var path : List.of("/static/js/new-release.js", "/static/css/new-release.css", "/templates/new-release.ftlh")) {
			try (var in = ReleaseRunRestOutputTest.class.getResourceAsStream(path)) {
				var text = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
				assertFalse(text.contains("rm-console"), path);
				assertFalse(text.contains("/events/' + encodeURIComponent(version) + '/' + encodeURIComponent(stepId)"), path);
				if (path.endsWith(".js"))
					assertTrue(text.contains("JuneauViews.consoleOutput.mount(") && text.contains("JuneauViews.runView.mount("), path);
				if (path.endsWith(".ftlh"))
					assertTrue(text.contains("nr-runview"), path);
			}
		}
	}
}
