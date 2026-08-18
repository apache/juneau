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

import java.util.Map;
import org.apache.juneau.commons.inject.Bean;
import org.apache.juneau.http.Content;
import org.apache.juneau.http.Path;
import org.apache.juneau.http.response.Conflict;
import org.apache.juneau.http.response.NotFound;
import org.apache.juneau.rest.server.Mutating;
import org.apache.juneau.rest.server.Rest;
import org.apache.juneau.rest.server.RestGet;
import org.apache.juneau.rest.server.RestPost;
import org.apache.juneau.rest.server.servlet.BasicRestResource;
import org.apache.juneau.rest.server.view.View;
import org.apache.juneau.rest.server.view.freemarker.FreemarkerMixin;
import org.apache.juneau.rest.server.view.freemarker.FreemarkerViewRenderer;
import org.apache.juneau.rest.server.view.freemarker.console.ConsoleFreemarkerMixin;
import org.apache.juneau.releng.engine.DropRcService;
import org.apache.juneau.releng.engine.ExecutionMode;
import org.apache.juneau.releng.engine.Preview;
import org.apache.juneau.releng.engine.ReleaseEngine;
import org.apache.juneau.releng.engine.RunState;
import org.apache.juneau.releng.engine.StepResult;

import jakarta.servlet.http.HttpServletRequest;

/**
 * New Release tab: pipeline control panel (View) plus JSON run/step/vote/drop-RC actions.
 *
 * <p>{@code disableContentParam} is set for the reason given on {@code CredentialRest}: Juneau's default lets a
 * {@code POST} body arrive in a {@code &content=} query parameter, which puts the arm confirmation phrase and every
 * other action payload into browser history and access logs. The boundary refuses that shape from a hostile page;
 * this closes the accidental use of it.
 */
@Rest(path = "/runs", title = "New Release", responseProcessors = FreemarkerViewRenderer.class,
	disableContentParam = "true")
public class ReleaseRunRest extends BasicRestResource {

	private final ReleaseEngine engine;
	private final DropRcService dropRc;

	public ReleaseRunRest(ReleaseEngine engine, DropRcService dropRc) {
		this.engine = engine;
		this.dropRc = dropRc;
	}

	// Return type stays FreemarkerMixin - FreemarkerViewRenderer does an exact-type bean lookup (see
	// ConsoleFreemarkerMixin's class Javadoc).
	@Bean
	public FreemarkerMixin freemarker() {
		return ConsoleFreemarkerMixin.create().basePath("/templates/").templateSuffix(".ftlh").build();
	}

	/** Human page — the pipeline control panel for the active run (or an empty start form). */
	@RestGet("/")
	public View page(HttpServletRequest req) {
		var liveCapable = engine.mode() == ExecutionMode.LIVE;
		var active = engine.displayRun().orElse(null);
		var runMode = active == null ? ExecutionMode.SAFE : engine.effectiveMode(active);
		var view = ConsolePage.of("new-release", req).attr("steps", engine.registry().steps())
				.attr("mode", runMode.name()).attr("appMode", engine.mode().name())
				.attr("liveCapable", Boolean.valueOf(liveCapable));
		// FreemarkerView.attr() rejects null values by design; the template only checks run??
		// (attribute presence), so omit the attribute entirely when there's no displayable run.
		return active == null ? view
				: view.attr("run", active).attr("armed", Boolean.valueOf(engine.isArmed(active.version)));
	}

	/** JSON RunState for polling / initial page data. */
	@RestGet("/{version}")
	public RunState state(@Path("version") String version) {
		return requireRun(version);
	}

	/**
	 * Start a new run. Body: {version, developmentVersion?, milestoneNumber?, mode?}. {@code milestoneNumber}
	 * is the New-Release form field — pre-filled client-side by title-match resolution (§8.1),
	 * user-overridable. {@code mode} is Dry-run ({@code SAFE}, default) or Actual ({@code LIVE}); LIVE is
	 * capped to SAFE unless the box was started with {@code rm.mode=live}. Rejects a second concurrent run
	 * with 409.
	 */
	@Mutating("creates a run and writes its state to disk")
	@RestPost("/")
	public RunState start(@Content StartRequest body) {
		try {
			var rs = engine.start(body.version, body.developmentVersion, body.milestoneNumber,
					ExecutionMode.fromConfig(body.mode));
			return engine.updateDetails(rs.version, body.releaseSummary, body.highlights, body.knownIssues,
					body.acknowledgements);
		} catch (IllegalStateException e) {
			throw new Conflict(e.getMessage());
		}
	}

	/**
	 * Update the active run's optional narrative fields ({@code releaseSummary}, {@code highlights},
	 * {@code knownIssues}, {@code acknowledgements}) so they can be edited before each email is composed.
	 * Returns the updated run.
	 */
	@Mutating("updates the run's persisted narrative fields")
	@RestPost("/{version}/details")
	public RunState details(@Path("version") String version, @Content DetailsRequest body) {
		requireRun(version);
		var b = body == null ? new DetailsRequest() : body;
		return engine.updateDetails(version, b.releaseSummary, b.highlights, b.knownIssues, b.acknowledgements);
	}

	// No @Mutating: a preview is a dry run by construction and writes nothing. The annotation is a claim about
	// effects, so putting it here to be "safe" would be a false one -- and would make the two preview endpoints
	// indistinguishable from the apply endpoints they exist to be safer than.
	@RestPost("/{version}/steps/{stepId}/preview")
	public Preview preview(@Path("version") String version, @Path("stepId") String stepId,
			@Content Map<String, String> form) {
		requireRun(version);
		return engine.preview(version, stepId, form == null ? Map.of() : form);
	}

	@Mutating("executes a release step; in LIVE mode this mutates git, SVN, Nexus, GitHub or mailing lists")
	@RestPost("/{version}/steps/{stepId}/apply")
	public StepResult apply(@Path("version") String version, @Path("stepId") String stepId,
			@Content Map<String, String> form) {
		requireRun(version);
		// Extra friction for the irreversible nexus-release.
		if (stepId.equals("nexus-release")) {
			var confirm = form == null ? null : form.get("confirmVersion");
			if (confirm == null || !confirm.equals(version))
				return StepResult.fail("Type the version string to confirm this irreversible release.");
		}
		return engine.apply(version, stepId, form == null ? Map.of() : form);
	}

	/**
	 * Resume (a failed step) or Re-run (an already-succeeded/skipped step) — both are the same call:
	 * re-invoke that step's apply() from scratch, overwriting status and log in place with no separate
	 * history. The UI picks the button label from the step's current status; the engine doesn't care which
	 * label was clicked.
	 */
	@Mutating("re-executes a release step, overwriting its status and log in place")
	@RestPost("/{version}/steps/{stepId}/resume")
	public StepResult resume(@Path("version") String version, @Path("stepId") String stepId,
			@Content Map<String, String> form) {
		requireRun(version);
		return engine.apply(version, stepId, form == null ? Map.of() : form);
	}

	@Mutating("marks a step skipped in the persisted run state")
	@RestPost("/{version}/steps/{stepId}/skip")
	public StepResult skip(@Path("version") String version, @Path("stepId") String stepId) {
		requireRun(version);
		return engine.skip(version, stepId);
	}

	/**
	 * Arm this run for LIVE mutation. Requires a typed confirm phrase ({@code "<version> LIVE"}) and is
	 * rejected unless the box is LIVE and this run is Actual (LIVE). Arming is in-memory on the engine and
	 * drops on any restart.
	 *
	 * <p>This is an <b>intent</b> gate: the confirm phrase shows a human deliberately typed something, and is not
	 * a secret and not authentication — it is derivable from the page it is typed on. Whether the request came from
	 * a page this application served is a separate question, answered by the loopback boundary in front of every
	 * endpoint. See {@link org.apache.juneau.releng.engine.ReleaseEngine#arm(String, String)}.
	 */
	@Mutating("arms the run for irreversible LIVE mutation")
	@RestPost("/{version}/arm")
	public StepResult arm(@Path("version") String version, @Content ArmRequest body) {
		requireRun(version);
		return engine.arm(version, body == null ? null : body.confirm);
	}

	/** Advance a review-gate step held in {@code AWAITING_REVIEW} once the human has confirmed the read-only work. */
	@Mutating("advances a held review-gate step")
	@RestPost("/{version}/steps/{stepId}/confirm-review")
	public StepResult confirmReview(@Path("version") String version, @Path("stepId") String stepId) {
		requireRun(version);
		return engine.confirmReview(version, stepId);
	}

	/** Record the vote outcome; 'rejected' triggers Drop-RC. */
	@Mutating("records the vote outcome and runs the tally step")
	@RestPost("/{version}/vote-result")
	public StepResult voteResult(@Path("version") String version, @Content VoteResultRequest body) {
		requireRun(version);
		return engine.apply(version, "tally-vote-result",
				Map.of("voteOutcome", body.outcome, "tally", body.tally == null ? "" : body.tally));
		// UI reads outcome; if 'rejected', UI then calls drop-rc/preview + apply.
	}

	@RestPost("/{version}/drop-rc/preview")
	public Preview dropRcPreview(@Path("version") String version) {
		requireRun(version);
		return dropRc.preview(version);
	}

	@Mutating("drops the release candidate from Nexus and dist SVN, and bumps the RC number")
	@RestPost("/{version}/drop-rc/apply")
	public StepResult dropRcApply(@Path("version") String version, @Content DropRcRequest body) {
		var rs = requireRun(version);
		if (body.confirmRc == null || !body.confirmRc.equals("RC" + rs.rc))
			return StepResult.fail("Type the RC identifier (e.g. RC1) to confirm this destructive action.");
		var secrets = engine.secrets();
		try {
			dropRc.apply(version, body.reason, secrets::availid, secrets::ldapPassword);
		} catch (IllegalStateException e) {
			return StepResult.fail(e.getMessage()); // the LIVE-unarmed guard refusal
		}
		return StepResult.ok("RC dropped; bumped to the next RC.");
	}

	/**
	 * All run-scoped endpoints 404 (rather than the engine's raw {@code IllegalStateException} propagating
	 * as a 500) when there's no persisted run for {@code version}.
	 */
	private RunState requireRun(String version) {
		var rs = engine.state(version);
		if (rs == null)
			throw new NotFound("No run for %s", version);
		return rs;
	}

	public static class StartRequest {
		public String version;
		public String developmentVersion;
		public Integer milestoneNumber;
		public String releaseSummary;
		public String highlights;
		public String knownIssues;
		public String acknowledgements;
		public String mode; // "SAFE" (default) or "LIVE"; capped by the box-wide rm.mode
	}

	public static class DetailsRequest {
		public String releaseSummary;
		public String highlights;
		public String knownIssues;
		public String acknowledgements;
	}

	public static class VoteResultRequest {
		public String outcome;
		public String tally;
	}

	public static class DropRcRequest {
		public String reason;
		public String confirmRc;
	}

	public static class ArmRequest {
		public String confirm;
	}
}
