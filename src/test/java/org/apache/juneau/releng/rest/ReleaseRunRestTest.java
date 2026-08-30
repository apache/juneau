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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.apache.juneau.http.response.Conflict;
import org.apache.juneau.http.response.NotFound;
import org.apache.juneau.releng.config.TargetProfile;
import org.apache.juneau.releng.email.EmailService;
import org.apache.juneau.releng.engine.BranchResolver;
import org.apache.juneau.releng.engine.DropRcService;
import org.apache.juneau.releng.engine.ExecutionMode;
import org.apache.juneau.releng.engine.ReleaseEngine;
import org.apache.juneau.releng.engine.RunStateStore;
import org.apache.juneau.releng.engine.RunStatus;
import org.apache.juneau.releng.engine.StepRegistry;
import org.apache.juneau.releng.engine.StepStatus;
import org.apache.juneau.releng.log.LogBroadcaster;
import org.apache.juneau.releng.milestone.MilestoneService;
import org.apache.juneau.releng.nexus.NexusMockModel;
import org.apache.juneau.releng.nexus.NexusMockRest;
import org.apache.juneau.releng.nexus.NexusStagingClient;
import org.apache.juneau.releng.util.ProcessRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Direct unit tests of {@link ReleaseRunRest}'s no-such-run/single-active-run status mapping.
 *
 * <p>These tests call the {@code @RestGet}/{@code @RestPost} methods directly rather than through an
 * in-process HTTP round trip — no serialization boundary is under test here (unlike, e.g.,
 * {@code NexusMockRestHttpTest}, which does need {@code juneau-rest-mock}'s real HTTP dispatch to catch a
 * handler-return-type bug that a direct call can't see).
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
		return rest(dir, ExecutionMode.SAFE);
	}

	private ReleaseRunRest rest(Path dir, ExecutionMode mode) {
		var runner = okRunner();
		var branches = new BranchResolver(runner, "/repo");
		var store = new RunStateStore(dir);
		var registry = StepRegistry.standard(branches);
		var engine = ReleaseEngine.forTests(store, registry, runner, branches, dir, mode);
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

	/**
	 * Like {@link #rest(Path)}, but wired with a real {@link NexusStagingClient} backed by an in-memory
	 * {@link NexusMockModel} via {@link NexusMockRest#route}, so {@code nexus-release} (Tier A) can
	 * run its real discover/close/promote flow against the loopback mock — no HTTP dispatch or
	 * {@code MockRestClient} needed (see {@code NexusMockRestHttpTest}'s per-class {@code RestContext}
	 * caching gotcha, which that route-level path sidesteps entirely).
	 */
	private ReleaseRunRest restWithNexus(Path dir, NexusMockModel model) {
		var runner = okRunner();
		var branches = new BranchResolver(runner, "/repo");
		var store = new RunStateStore(dir);
		var registry = StepRegistry.standard(branches);
		var nexus = NexusStagingClient.forTests((m, p, b) -> NexusMockRest.route(model, m, p, b),
				NexusStagingClient.JUNEAU_PROFILE_ID);
		var secrets = new ReleaseEngine.SecretResolver() {
			@Override
			public String availid() {
				return "test";
			}

			@Override
			public String ldapPassword() {
				return "";
			}

			@Override
			public String gpgKeyId() {
				return "";
			}

			@Override
			public String gpgPassphrase() {
				return "";
			}

			@Override
			public String githubToken() {
				return "";
			}

			@Override
			public NexusStagingClient nexus() {
				return nexus;
			}
		};
		var engine = new ReleaseEngine(store, registry, runner, branches, dir, dir.resolve("staging"), "/repo",
				"test@apache.org", new EmailService(dir, runner), new MilestoneService(), secrets,
				ExecutionMode.SAFE, TargetProfile.prodDefault());
		var dropRc = new DropRcService(store, registry, runner, dir.resolve("staging/git/juneau"), dir, nexus,
				engine.mode(), engine::isArmed, TargetProfile.prodDefault(), (v, s) -> new LogBroadcaster());
		return new ReleaseRunRest(engine, dropRc);
	}

	private List<String> logLines(Path dir, ReleaseRunRest rest, String version, String stepId) throws IOException {
		return Files.readAllLines(dir.resolve(rest.state(version).step(stepId).logRef));
	}

	/**
	 * Marks every step before {@code stepId} (in registry order) SUCCEEDED, so a test can exercise
	 * {@code stepId} in isolation under the strict forward-apply guard without needing every predecessor's
	 * own apply() to succeed for real. Writes straight through a fresh store pointed at the same
	 * {@code dir}, since {@link RunStateStore} is a stateless file-backed reader/writer.
	 */
	private void satisfyAllPredecessorsOf(Path dir, String version, String stepId) {
		var store = new RunStateStore(dir);
		var rs = store.load(version).orElseThrow();
		var ids = StepRegistry.standard(new BranchResolver(okRunner(), "/repo")).ids();
		var idx = ids.indexOf(stepId);
		for (var i = 0; i < idx; i++)
			rs.step(ids.get(i)).status = StepStatus.SUCCEEDED;
		store.save(rs);
	}

	/** Applies {@code stepId} with no form input and asserts it succeeded. */
	private void applyOk(ReleaseRunRest rest, String version, String stepId) {
		applyOk(rest, version, stepId, Map.of());
	}

	/** Applies {@code stepId} with {@code form} and asserts it succeeded. */
	private void applyOk(ReleaseRunRest rest, String version, String stepId, Map<String, String> form) {
		var r = rest.apply(version, stepId, form);
		assertTrue(r.success, stepId + ": " + r.message);
	}

	/** Confirms a review-gate {@code stepId} and asserts the confirm succeeded. */
	private void confirmOk(ReleaseRunRest rest, String version, String stepId) {
		var c = rest.confirmReview(version, stepId);
		assertTrue(c.success, stepId + " confirm-review: " + c.message);
	}

	@Test
	void a01_getStateForNonexistentRunIs404(@TempDir Path dir) {
		var rest = rest(dir);
		var ex = assertThrows(NotFound.class, () -> rest.state("9.9.9"));
		assertEquals(404, ex.getStatusCode());
	}

	@Test
	void a02_previewAgainstNonexistentRunIs404(@TempDir Path dir) {
		var rest = rest(dir);
		Map<String, String> form = Map.of();
		var ex = assertThrows(NotFound.class, () -> rest.preview("9.9.9", "preflight", form));
		assertEquals(404, ex.getStatusCode());
	}

	@Test
	void a03_applyAgainstNonexistentRunIs404(@TempDir Path dir) {
		var rest = rest(dir);
		Map<String, String> form = Map.of();
		var ex = assertThrows(NotFound.class, () -> rest.apply("9.9.9", "preflight", form));
		assertEquals(404, ex.getStatusCode());
	}

	@Test
	void a04_skipAgainstNonexistentRunIs404(@TempDir Path dir) {
		var rest = rest(dir);
		var ex = assertThrows(NotFound.class, () -> rest.skip("9.9.9", "preflight"));
		assertEquals(404, ex.getStatusCode());
	}

	@Test
	void a05_dropRcPreviewAgainstNonexistentRunIs404(@TempDir Path dir) {
		var rest = rest(dir);
		var ex = assertThrows(NotFound.class, () -> rest.dropRcPreview("9.9.9"));
		assertEquals(404, ex.getStatusCode());
	}

	@Test
	void b01_previewAgainstRealRunStillWorks(@TempDir Path dir) {
		var rest = rest(dir);
		rest.start(startRequest("9.2.1"));
		var preview = rest.preview("9.2.1", "preflight", Map.of());
		assertNotNull(preview);
	}

	@Test
	void b02_startPersistsFormSuppliedMilestoneNumber(@TempDir Path dir) {
		var rest = rest(dir);
		var body = startRequest("9.2.1");
		body.milestoneNumber = 42;
		var rs = rest.start(body);
		assertEquals(42, rs.milestoneNumber);
	}

	@Test
	void b03_startPersistsNarrativeFields(@TempDir Path dir) {
		var rest = rest(dir);
		var body = startRequest("9.2.1");
		body.releaseSummary = "Patch release.";
		body.highlights = "- One\n- Two";
		body.knownIssues = "- A known thing";
		body.acknowledgements = "Thanks all.";
		rest.start(body);

		var rs = rest.state("9.2.1");
		assertEquals("Patch release.", rs.releaseSummary);
		assertEquals("- One\n- Two", rs.highlights);
		assertEquals("- A known thing", rs.knownIssues);
		assertEquals("Thanks all.", rs.acknowledgements);
	}

	@Test
	void b04_startDefaultsToSafeMode(@TempDir Path dir) {
		var rs = rest(dir).start(startRequest("9.2.1"));
		assertEquals(ExecutionMode.SAFE, rs.mode);
	}

	@Test
	void b05_startLiveIsCappedOnASafeEngine(@TempDir Path dir) {
		var body = startRequest("9.2.1");
		body.mode = "live";
		var rs = rest(dir).start(body);
		assertEquals(ExecutionMode.SAFE, rs.mode);
	}

	@Test
	void b06_startLiveIsHonoredOnALiveEngine(@TempDir Path dir) {
		var body = startRequest("9.2.1");
		body.mode = "live";
		var rs = rest(dir, ExecutionMode.LIVE).start(body);
		assertEquals(ExecutionMode.LIVE, rs.mode);
	}

	@Test
	void c01_detailsEndpointUpdatesNarrativeFieldsOnActiveRun(@TempDir Path dir) {
		var rest = rest(dir);
		rest.start(startRequest("9.2.1"));

		var body = new ReleaseRunRest.DetailsRequest();
		body.releaseSummary = "Revised summary.";
		body.highlights = "- Updated highlight";
		body.knownIssues = "- Updated issue";
		body.acknowledgements = "Updated thanks.";
		var updated = rest.details("9.2.1", body);

		assertEquals("Revised summary.", updated.releaseSummary);
		assertEquals("- Updated highlight", updated.highlights);
		// Reload from the store to confirm the update was persisted, not just returned.
		var reloaded = rest.state("9.2.1");
		assertEquals("- Updated issue", reloaded.knownIssues);
		assertEquals("Updated thanks.", reloaded.acknowledgements);
	}

	@Test
	void a06_detailsAgainstNonexistentRunIs404(@TempDir Path dir) {
		var rest = rest(dir);
		var body = new ReleaseRunRest.DetailsRequest();
		var ex = assertThrows(NotFound.class, () -> rest.details("9.9.9", body));
		assertEquals(404, ex.getStatusCode());
	}

	@Test
	void a07_confirmReviewAgainstNonexistentRunIs404(@TempDir Path dir) {
		var rest = rest(dir);
		var ex = assertThrows(NotFound.class, () -> rest.confirmReview("9.9.9", "javadoc-verify"));
		assertEquals(404, ex.getStatusCode());
	}

	@Test
	void c02_confirmReviewAdvancesAReviewGateStepHeldAtAwaitingReview(@TempDir Path dir) {
		var rest = rest(dir);
		rest.start(startRequest("9.2.1"));
		satisfyAllPredecessorsOf(dir, "9.2.1", "javadoc-verify");
		rest.apply("9.2.1", "javadoc-verify", Map.of());
		assertEquals(StepStatus.AWAITING_REVIEW, rest.state("9.2.1").step("javadoc-verify").status);

		var res = rest.confirmReview("9.2.1", "javadoc-verify");

		assertTrue(res.success);
		assertEquals(StepStatus.SUCCEEDED, rest.state("9.2.1").step("javadoc-verify").status);
	}

	@Test
	void b07_secondConcurrentStartIs409(@TempDir Path dir) {
		var rest = rest(dir);
		rest.start(startRequest("9.2.1"));

		var second = startRequest("9.2.2");
		var ex = assertThrows(Conflict.class, () -> rest.start(second));
		assertEquals(409, ex.getStatusCode());
		assertTrue(ex.getMessage().contains("9.2.1"));
	}

	/**
	 * The fix, end to end, under the strict forward-apply guard: {@code POST /{version}/vote-result} —
	 * what the fixed "Simulate (SAFE)" button now calls — records a passing tally (applied as the
	 * {@code tally-vote-result} step) and the run then proceeds through every remaining required step, in
	 * order, to {@code finalize-run}, with no operator intervention beyond each step's own apply or
	 * confirm-review. This is also the regression test for the reported bug: a run must legitimately clear
	 * every required step — including {@code nexus-release}, {@code manual-followup-checklist}, and
	 * {@code compose-announcement-email} — before {@code finalize-run} accepts it.
	 */
	@SuppressWarnings({
		"java:S5961" // Deliberately one continuous end-to-end run through every required pipeline step, in
					 // order, on a single mutable run; splitting into separate @Test methods would re-derive
					 // (or fake) the intermediate run state each time and weaken exactly the regression this
					 // test exists to catch -- that the SAME run legitimately clears every required step.
	})
	@Test
	void d01_safeVoteResultAdvancesGateAndPipelineReachesFinalize(@TempDir Path dir) throws IOException {
		var model = new NexusMockModel(NexusStagingClient.JUNEAU_PROFILE_ID);
		var rest = restWithNexus(dir, model);
		var start = startRequest("9.2.1");
		start.milestoneNumber = 42;
		rest.start(start);

		for (var stepId : List.of("preflight", "compose-propose-email", "workspace-setup", "build-verify"))
			applyOk(rest, "9.2.1", stepId);
		// javadoc-verify / test-workspace-verify: review-gate steps, resolved via confirm-review.
		for (var stepId : List.of("javadoc-verify", "test-workspace-verify")) {
			applyOk(rest, "9.2.1", stepId);
			confirmOk(rest, "9.2.1", stepId);
		}
		for (var stepId : List.of("deploy-snapshot", "release-prepare"))
			applyOk(rest, "9.2.1", stepId);
		// release-diff-review: a required (non-skippable) review-gate step.
		applyOk(rest, "9.2.1", "release-diff-review");
		confirmOk(rest, "9.2.1", "release-diff-review");

		applyOk(rest, "9.2.1", "release-perform");

		// Seeds+closes the mock staging repo so nexus-release has a CLOSED repo to promote.
		applyOk(rest, "9.2.1", "nexus-staging-close");
		assertEquals("closed", model.currentState());

		applyOk(rest, "9.2.1", "binary-artifacts-stage");

		// dev-dist-verify: a required (non-skippable) review-gate step.
		applyOk(rest, "9.2.1", "dev-dist-verify");
		confirmOk(rest, "9.2.1", "dev-dist-verify");

		applyOk(rest, "9.2.1", "compose-vote-email");

		// Opening the vote only sets AWAITING_VOTE — this is the exact call the OLD "Simulate (SAFE)"
		// wiring made, and it never advances on its own no matter how many times it's re-applied.
		rest.apply("9.2.1", "vote-gate", Map.of());
		rest.apply("9.2.1", "vote-gate", Map.of());
		assertEquals(StepStatus.AWAITING_VOTE, rest.state("9.2.1").step("vote-gate").status);

		// The fix: record a passing tally via the dedicated vote-result endpoint.
		var voteBody = new ReleaseRunRest.VoteResultRequest();
		voteBody.outcome = "passed";
		voteBody.tally = "SAFE-mode simulated passing vote (no real tally read).";
		var voted = rest.voteResult("9.2.1", voteBody);
		assertTrue(voted.success, voted.message);
		assertEquals(StepStatus.SUCCEEDED, rest.state("9.2.1").step("tally-vote-result").status);
		assertEquals(StepStatus.SUCCEEDED, rest.state("9.2.1").step("vote-gate").status,
				"a passing tally must flip vote-gate to a terminal status");

		applyOk(rest, "9.2.1", "compose-result-email");

		// Tier A: nexus-release runs for real against the loopback mock — CLOSED -> RELEASED.
		applyOk(rest, "9.2.1", "nexus-release", Map.of("confirmVersion", "9.2.1"));
		assertEquals("released", model.currentState());

		// Tier B: dist-promote is command-logged only (svn mv/commit), never a real subprocess.
		applyOk(rest, "9.2.1", "dist-promote");
		assertTrue(logLines(dir, rest, "9.2.1", "dist-promote").stream()
				.anyMatch(l -> l.startsWith("would run:") && l.contains("svn") && l.contains("mv")));

		// Tier B: github-release-create.
		applyOk(rest, "9.2.1", "github-release-create");
		assertTrue(logLines(dir, rest, "9.2.1", "github-release-create").stream()
				.anyMatch(l -> l.startsWith("would run:") && l.contains("gh") && l.contains("release")));

		// Tier B: milestone-close.
		applyOk(rest, "9.2.1", "milestone-close");
		assertTrue(logLines(dir, rest, "9.2.1", "milestone-close").stream()
				.anyMatch(l -> l.startsWith("would run:") && l.contains("milestones/42")));

		// manual-followup-checklist: a required (non-skippable) review-gate step — must be confirmed for
		// finalize-run's strict prerequisite gate to be satisfiable. Its own apply() requires every
		// checklist item's text present in the "checklist" form input.
		var checklist = "Update juneau-docs release-notes page + site; Publish aggregate Javadoc; "
				+ "Update the download page; Edit the Confluence release wiki";
		applyOk(rest, "9.2.1", "manual-followup-checklist", Map.of("checklist", checklist));
		confirmOk(rest, "9.2.1", "manual-followup-checklist");

		applyOk(rest, "9.2.1", "compose-announcement-email");

		// finalize-run is reached and completes; the run is RELEASED end to end.
		applyOk(rest, "9.2.1", "finalize-run");
		assertEquals(StepStatus.SUCCEEDED, rest.state("9.2.1").step("finalize-run").status);
		assertEquals(RunStatus.RELEASED, rest.state("9.2.1").status);
	}

	/**
	 * Regression test for the reported bug: finalize-run must refuse — and must NOT set RunStatus to
	 * RELEASED — when a required step ({@code nexus-release}) was never run and another required step
	 * ({@code manual-followup-checklist}) failed, even though every step before {@code vote-gate}
	 * legitimately succeeded.
	 */
	@Test
	void d02_finalizeRunRefusesAndListsOffendingStepsWhenRequiredStepsAreUnresolved(@TempDir Path dir) {
		var rest = rest(dir);
		rest.start(startRequest("9.2.1"));
		satisfyAllPredecessorsOf(dir, "9.2.1", "finalize-run");
		var store = new RunStateStore(dir);
		var rs = store.load("9.2.1").orElseThrow();
		rs.step("nexus-release").status = StepStatus.PENDING;
		rs.step("manual-followup-checklist").status = StepStatus.FAILED;
		store.save(rs);

		var res = rest.apply("9.2.1", "finalize-run", Map.of());

		assertFalse(res.success);
		assertTrue(res.message.contains("nexus-release"), res.message);
		assertTrue(res.message.contains("manual-followup-checklist"), res.message);
		assertNotEquals(RunStatus.RELEASED, rest.state("9.2.1").status);
	}

	@Test
	void d03_applyUnknownStepReturnsJsonFailRatherThanThrowing(@TempDir Path dir) {
		var rest = rest(dir);
		rest.start(startRequest("9.2.1"));
		var res = assertDoesNotThrow(() -> rest.apply("9.2.1", "nexus-staging-release", Map.of()));
		assertFalse(res.success);
		assertTrue(res.message.contains("Unknown step"), res.message);
	}

	/** LIVE mode must keep requiring a real recorded tally: the SAFE one-click shortcut is UI-only (the
	 *  fixed "Simulate (SAFE)" button hard-codes {@code outcome=passed}); the {@code /vote-result} REST
	 *  contract itself is unchanged, so a rejected outcome still forks away from the linear step list
	 *  instead of quietly advancing. */
	@Test
	void d04_voteResultWithRejectedOutcomeDoesNotAdvanceButRecordsTheOutcome(@TempDir Path dir) {
		var rest = rest(dir);
		rest.start(startRequest("9.2.1"));
		satisfyAllPredecessorsOf(dir, "9.2.1", "vote-gate");
		rest.apply("9.2.1", "vote-gate", Map.of());

		var voteBody = new ReleaseRunRest.VoteResultRequest();
		voteBody.outcome = "rejected";
		voteBody.tally = "Binding -1: 1 (blocking issue found).";
		var voted = rest.voteResult("9.2.1", voteBody);

		assertTrue(voted.success, voted.message); // recording the outcome itself always succeeds
		assertEquals(StepStatus.SUCCEEDED, rest.state("9.2.1").step("tally-vote-result").status);
		assertEquals(StepStatus.PENDING, rest.state("9.2.1").step("nexus-release").status,
				"a rejected vote must not advance the linear pipeline");
	}

	// -----------------------------------------------------------------------------------------------------------
	// nr-step-meta <script> sidecar: break-out neutralization
	// -----------------------------------------------------------------------------------------------------------

	/**
	 * The {@code nr-step-meta} JSON is now built Java-side by {@link ReleaseRunRest#stepMetaJson(Iterable)} and passed
	 * through {@code escapeForScript} rather than interpolated in the {@code .ftlh}. A step title carrying a
	 * {@code </script>} break-out must be neutralized (no raw {@code <} survives) yet remain valid, round-trippable
	 * JSON &mdash; the property FreeMarker's HTML auto-escaping would have silently corrupted. Asserts the
	 * neutralization, not merely that a benign title round-trips.
	 */
	@Test
	void e01_stepMetaJsonNeutralizesScriptBreakoutInAStepTitle() throws Exception {
		var evilTitle = "</script><script>alert(1)</script>\u2028x";
		var step = new org.apache.juneau.releng.engine.ReleaseStep() {
			@Override public String id() { return "evil"; }
			@Override public String title() { return evilTitle; }
			@Override public boolean mutating() { return true; }
			@Override public org.apache.juneau.releng.engine.Preview preview(org.apache.juneau.releng.engine.StepContext c) { return null; }
			@Override public org.apache.juneau.releng.engine.StepResult apply(org.apache.juneau.releng.engine.StepContext c) { return null; }
		};

		var json = ReleaseRunRest.stepMetaJson(List.of(step));

		// Break-out neutralized: no raw '<' or raw U+2028 can survive to close the raw-text <script> element early.
		assertFalse(json.contains("<"), () -> "raw '<' survived into the <script> sidecar: " + json);
		assertFalse(json.contains("\u2028"), () -> "raw U+2028 survived into the <script> sidecar: " + json);
		assertTrue(json.contains("\\u003c"), () -> "expected escaped '<' (\\u003c) in sidecar: " + json);

		// Still valid, round-trippable JSON: a JSON parser decodes \u003c back to the original title verbatim
		// (this is exactly what FreeMarker's HTML entity-encoding would have corrupted).
		var parsed = org.apache.juneau.marshall.marshaller.Json.to(json, Map.class);
		@SuppressWarnings({
			"unchecked" // Assigning a raw Json.to(..., Map.class) result to its known parameterized shape.
		})
		var entry = (Map<String,Object>) parsed.get("evil");
		assertEquals(evilTitle, entry.get("title"));
		assertEquals(Boolean.TRUE, entry.get("mutating"));
	}
}
