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

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.escapeForScript;

import java.util.Map;
import org.apache.juneau.commons.inject.Bean;
import org.apache.juneau.marshall.marshaller.Json;
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
import org.apache.juneau.releng.engine.Preview;
import org.apache.juneau.releng.engine.ReleaseEngine;
import org.apache.juneau.releng.engine.RunState;
import org.apache.juneau.releng.engine.StepResult;

import jakarta.servlet.http.HttpServletRequest;

/**
 * New Release tab: pipeline control panel (View) plus JSON run/step/vote/drop-RC actions.
 *
 * <p>{@code disableContentParam} is set for the reason given on {@code CredentialRest}: Juneau's default lets a
 * {@code POST} body arrive in a {@code &content=} query parameter, which puts every action payload into browser
 * history and access logs. The boundary refuses that shape from a hostile page; this closes the accidental use of it.
 */
@Rest(path = "/runs", title = "New Release", responseProcessors = FreemarkerViewRenderer.class,
	disableContentParam = "true")
public class ReleaseRunRest extends BasicRestResource {

	private final ReleaseEngine engine;
	private final DropRcService dropRc;

	/**
	 * Wires the release engine and Drop-RC service this resource's endpoints delegate to.
	 */
	public ReleaseRunRest(ReleaseEngine engine, DropRcService dropRc) {
		this.engine = engine;
		this.dropRc = dropRc;
	}

	/**
	 * The Freemarker mixin used to render this resource's templates.
	 */
	// Return type stays FreemarkerMixin - FreemarkerViewRenderer does an exact-type bean lookup (see
	// ConsoleFreemarkerMixin's class Javadoc).
	@Bean
	public FreemarkerMixin freemarker() {
		return ConsoleFreemarkerMixin.create().basePath("/templates/").templateSuffix(".ftlh").build();
	}

	/**
	 * Human page — the pipeline control panel for the active run (or an empty start form).
	 */
	@RestGet("/")
	public View page(HttpServletRequest req) {
		var active = engine.displayRun().orElse(null);
		var subtab = resolveSubtab(req.getParameter("tab"));
		var view = ConsolePage.of("new-release", req).attr("stepMeta", stepMetaJson(engine.registry().steps()))
				.attr("subtab", subtab).attr("navTab", "new/" + subtab);
		// FreemarkerView.attr() rejects null values by design; the template only checks run??
		// (attribute presence), so omit the attribute entirely when there's no displayable run.
		return active == null ? view : view.attr("run", active);
	}

	/**
	 * Resolves the New Release Page Subtab. An explicit {@code ?tab=exec} selects Execution;
	 * {@code ?tab=input}, a missing {@code tab} query, and any unrecognized value select Input.
	 */
	static String resolveSubtab(String requested) {
		if (requested != null) {
			var t = requested.strip().toLowerCase();
			if ("exec".equals(t) || "execution".equals(t))
				return "exec";
		}
		return "input";
	}

	/**
	 * Serializes the step registry's {@code {id: {title, mutating}}} map for the {@code nr-step-meta} sidecar.
	 *
	 * <p>Built and escaped Java-side rather than interpolated in the {@code .ftlh}: the block is the raw-text content
	 * of a {@code <script type="application/json">} element, for which FreeMarker's HTML auto-escaping is the wrong
	 * escaper (it entity-encodes {@code &}/{@code <}/{@code "} into forms {@code JSON.parse} reads verbatim) and its
	 * incidental {@code </script>} break-out protection depends only on the file extension. This serializes with the
	 * repo's JSON marshaller and hands the result to {@link org.apache.juneau.commons.utils.StringUtils#escapeForScript(String)}
	 * &mdash; the same shared, hardened escaper the framework's {@code ViewTable}/{@code PageTable} sidecars use
	 * &mdash; so a step title containing {@code </script>} cannot terminate the element early.
	 *
	 * @param steps The registry's steps.
	 * @return The break-out-safe, {@code JSON.parse}-able sidecar payload.
	 */
	static String stepMetaJson(Iterable<? extends org.apache.juneau.releng.engine.ReleaseStep> steps) {
		Map<String, Object> meta = m();
		for (var step : steps)
			meta.put(step.id(), m("title", step.title(), "mutating", Boolean.valueOf(step.mutating())));
		return escapeForScript(Json.of(meta));
	}

	/**
	 * JSON RunState for polling / initial page data.
	 */
	@RestGet("/{version}")
	public RunState state(@Path("version") String version) {
		return requireRun(version);
	}

	/**
	 * Start a new run. Body: {version, developmentVersion?, rc?}. {@code rc} is the release-candidate
	 * integer from the Input form (tags {@code juneau-{version}-RC{rc}}); omitted or non-positive leaves
	 * the default {@code 1}. Rejects a second concurrent run with 409.
	 */
	@Mutating("creates a run and writes its state to disk")
	@RestPost("/")
	public RunState start(@Content StartRequest body) {
		try {
			var rs = engine.start(body.version, body.developmentVersion, body.rc);
			return engine.updateDetails(rs.version, body.releaseSummary, body.highlights, body.knownIssues,
					body.acknowledgements);
		} catch (IllegalStateException e) {
			throw new Conflict(e.getMessage());
		}
	}

	/**
	 * Update the active run's Input-form fields (version, development version, release candidate, and the four
	 * narrative fields) so they can be edited after start. Returns the updated run. A version change
	 * renames the persisted run file; a colliding version is a 409. A missing or non-positive {@code rc}
	 * leaves the stored candidate number unchanged.
	 */
	@Mutating("updates the run's persisted identity and narrative fields")
	@RestPost("/{version}/details")
	public RunState details(@Path("version") String version, @Content DetailsRequest body) {
		requireRun(version);
		var b = body == null ? new DetailsRequest() : body;
		try {
			return engine.updateDetails(version, b.releaseSummary, b.highlights, b.knownIssues, b.acknowledgements,
					b.version, b.developmentVersion, b.rc);
		} catch (IllegalStateException e) {
			throw new Conflict(e.getMessage());
		}
	}

	/**
	 * Executes one release step, applying its {@code form} data; the irreversible {@code nexus-release} step requires typing the version to confirm.
	 */
	@Mutating("executes a release step; this mutates git, SVN, Nexus, GitHub or mailing lists")
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

	/**
	 * Marks a step skipped without executing it.
	 */
	@Mutating("marks a step skipped in the persisted run state")
	@RestPost("/{version}/steps/{stepId}/skip")
	public StepResult skip(@Path("version") String version, @Path("stepId") String stepId) {
		requireRun(version);
		return engine.skip(version, stepId);
	}

	/**
	 * Advance a review-gate step held in {@code AWAITING_REVIEW} once the human has confirmed the read-only work.
	 */
	@Mutating("advances a held review-gate step")
	@RestPost("/{version}/steps/{stepId}/confirm-review")
	public StepResult confirmReview(@Path("version") String version, @Path("stepId") String stepId) {
		requireRun(version);
		return engine.confirmReview(version, stepId);
	}

	/**
	 * Record the vote outcome; 'rejected' triggers Drop-RC.
	 */
	@Mutating("records the vote outcome and runs the tally step")
	@RestPost("/{version}/vote-result")
	public StepResult voteResult(@Path("version") String version, @Content VoteResultRequest body) {
		requireRun(version);
		return engine.apply(version, "tally-vote-result",
				Map.of("voteOutcome", body.outcome, "tally", body.tally == null ? "" : body.tally));
		// UI reads outcome; if 'rejected', UI then calls drop-rc/preview + apply.
	}

	/**
	 * Previews what a Drop-RC would remove, without changing anything.
	 */
	@RestPost("/{version}/drop-rc/preview")
	public Preview dropRcPreview(@Path("version") String version) {
		requireRun(version);
		return dropRc.preview(version);
	}

	/**
	 * Drops the current release candidate; requires typing the RC identifier (e.g. {@code RC1}) to confirm.
	 */
	@Mutating("drops the release candidate from Nexus and dist SVN, and bumps the RC number")
	@RestPost("/{version}/drop-rc/apply")
	public StepResult dropRcApply(@Path("version") String version, @Content DropRcRequest body) {
		var rs = requireRun(version);
		if (body.confirmRc == null || !body.confirmRc.equals("RC" + rs.rc))
			return StepResult.fail("Type the RC identifier (e.g. RC1) to confirm this destructive action.");
		var secrets = engine.secrets();
		dropRc.apply(version, body.reason, secrets::availid, secrets::ldapPassword);
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

	/**
	 * Body for {@link #start(StartRequest)}.
	 */
	public static class StartRequest {
		public String version;
		public String developmentVersion;
		public Integer rc;
		public String releaseSummary;
		public String highlights;
		public String knownIssues;
		public String acknowledgements;
	}

	/**
	 * Body for {@link #details(String, DetailsRequest)}.
	 */
	public static class DetailsRequest {
		public String version;
		public String developmentVersion;
		public Integer rc;
		public String releaseSummary;
		public String highlights;
		public String knownIssues;
		public String acknowledgements;
	}

	/**
	 * Body for {@link #voteResult(String, VoteResultRequest)}.
	 */
	public static class VoteResultRequest {
		public String outcome;
		public String tally;
	}

	/**
	 * Body for {@link #dropRcApply(String, DropRcRequest)}.
	 */
	public static class DropRcRequest {
		public String reason;
		public String confirmRc;
	}
}
