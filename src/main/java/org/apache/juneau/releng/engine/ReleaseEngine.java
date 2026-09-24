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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.juneau.marshall.marshaller.Json;
import org.apache.juneau.releng.config.TargetProfile;
import org.apache.juneau.releng.email.EmailService;
import org.apache.juneau.releng.log.LogBroadcaster;
import org.apache.juneau.releng.log.RunLog;
import org.apache.juneau.releng.log.RunStateBroadcaster;
import org.apache.juneau.releng.milestone.MilestoneService;
import org.apache.juneau.releng.nexus.NexusStagingClient;
import org.apache.juneau.releng.util.ProcessRunner;

/**
 * Single-active-run orchestrator. One run advances at a time; state is persisted after every step.
 */
public class ReleaseEngine {

	private static final String VOTE_GATE = "vote-gate";
	private static final String UNKNOWN_STEP = "Unknown step: ";

	private final RunStateStore store;
	private final StepRegistry registry;
	private final ProcessRunner runner;
	private final BranchResolver branches;
	private final Path stateDir;
	private final Path stagingRoot; // rm.staging.dir
	private final String repoDir;
	private final String committerEmail;
	private final EmailService email;
	private final MilestoneService milestone;
	// Secrets + nexus client are supplied by the REST layer per mutating action; test factory passes nulls.
	private final SecretResolver secrets;
	private final TargetProfile target;

	// In-memory per-step broadcasters, keyed "version/stepId". Lost on restart; log files survive.
	private final Map<String, LogBroadcaster> broadcasters = new ConcurrentHashMap<>();

	// In-memory per-run run-state broadcasters, keyed by version. Lost on restart; a reconnecting SSE
	// client gets a fresh initial snapshot instead (see AppConfiguration's state resolver).
	private final Map<String, RunStateBroadcaster> stateBroadcasters = new ConcurrentHashMap<>();

	/**
	 * Everything the REST layer must provide to build a mutating StepContext.
	 */
	public interface SecretResolver {
		/**
		 * The maintainer's Apache LDAP account id.
		 */
		String availid();

		/**
		 * The maintainer's Apache LDAP password.
		 */
		String ldapPassword();

		/**
		 * The GPG key id used to sign release artifacts.
		 */
		String gpgKeyId();

		/**
		 * The passphrase for {@link #gpgKeyId()}.
		 */
		String gpgPassphrase();

		/**
		 * Token for GitHub API calls.
		 */
		String githubToken();

		/**
		 * The Nexus staging client for this run.
		 */
		NexusStagingClient nexus();
	}

	/**
	 * Constructor injecting all of this engine's collaborators.
	 */
	@SuppressWarnings({
		"java:S107" // Constructor-injected collaborators; a parameter object would obscure the wiring.
	})
	public ReleaseEngine(RunStateStore store, StepRegistry registry, ProcessRunner runner, BranchResolver branches,
			Path stateDir, Path stagingRoot, String repoDir, String committerEmail, EmailService email,
			MilestoneService milestone, SecretResolver secrets, TargetProfile target) {
		this.store = store;
		this.registry = registry;
		this.runner = runner;
		this.branches = branches;
		this.stateDir = stateDir;
		this.stagingRoot = stagingRoot;
		this.repoDir = repoDir;
		this.committerEmail = committerEmail;
		this.email = email;
		this.milestone = milestone;
		this.secrets = secrets;
		this.target = target == null ? TargetProfile.prodDefault() : target;
		// The single choke point for the New-Release tab's live rail push: every status-mutating
		// transition — this engine's own methods AND DropRcService's drop-RC action, since it shares this
		// same RunStateStore instance — ultimately calls store.save(), so hooking it here catches all of
		// them without a separate publish call at each mutation site.
		store.setOnSave(this::publishSnapshot);
	}

	/**
	 * Minimal test factory (no secrets/nexus/email).
	 */
	public static ReleaseEngine forTests(RunStateStore store, StepRegistry registry, ProcessRunner runner,
			BranchResolver branches, Path stateDir) {
		var noSecrets = new SecretResolver() {
			public String availid() {
				return "test";
			}

			public String ldapPassword() {
				return "";
			}

			public String gpgKeyId() {
				return "";
			}

			public String gpgPassphrase() {
				return "";
			}

			public String githubToken() {
				return "";
			}

			public NexusStagingClient nexus() {
				return null;
			}
		};
		return new ReleaseEngine(store, registry, runner, branches, stateDir, stateDir.resolve("staging"), "/repo",
				"test@apache.org", new EmailService(stateDir, runner), new MilestoneService(), noSecrets,
				TargetProfile.prodDefault());
	}

	/**
	 * One broadcaster per (version, step).
	 */
	public LogBroadcaster broadcaster(String version, String stepId) {
		return broadcasters.computeIfAbsent(version + "/" + stepId, k -> new LogBroadcaster());
	}

	/**
	 * One run-state broadcaster per version, for the New-Release tab's live rail push.
	 */
	public RunStateBroadcaster stateBroadcaster(String version) {
		return stateBroadcasters.computeIfAbsent(version, k -> new RunStateBroadcaster());
	}

	/**
	 * {@code version}'s current snapshot as JSON, or empty when there's no persisted run for it.
	 */
	public Optional<String> snapshotJson(String version) {
		return store.load(version)
				.map(rs -> Json.DEFAULT.write(RunStateSnapshot.of(rs)));
	}

	/**
	 * Builds {@code rs}'s snapshot and pushes it to that version's {@link RunStateBroadcaster}. Registered
	 * as {@link RunStateStore}'s {@code onSave} hook (see the constructor).
	 */
	private void publishSnapshot(RunState rs) {
		stateBroadcaster(rs.version)
				.publish(Json.DEFAULT.write(RunStateSnapshot.of(rs)));
	}

	/**
	 * The single active run, if any. See {@link RunStateStore#activeRun()}.
	 */
	public Optional<RunState> activeRun() {
		return store.activeRun();
	}

	/**
	 * The run the New-Release page should render — see {@link RunStateStore#displayRun()}.
	 */
	public Optional<RunState> displayRun() {
		return store.displayRun();
	}

	/**
	 * The persisted run for {@code version}, or null.
	 */
	public RunState state(String version) {
		return store.load(version).orElse(null);
	}

	/**
	 * Start a new run; enforces the single-active-run lock.
	 */
	public synchronized RunState start(String version, String developmentVersion) {
		return start(version, developmentVersion, null);
	}

	/**
	 * Start a new run. {@code rc} is the release-candidate number from the Input form; null or a
	 * non-positive value leaves the default {@code 1}.
	 */
	public synchronized RunState start(String version, String developmentVersion, Integer rc) {
		var active = store.activeRun();
		if (active.isPresent())
			throw isex("A run is already active: %s (%s). Finish or drop it first.", active.get().version,
					active.get().currentStepId);
		var branch = branches.resolve(version);
		var rs = RunState.create(version, branch, registry.ids());
		rs.developmentVersion = developmentVersion;
		applyRc(rs, rc);
		store.save(rs);
		return rs;
	}

	/**
	 * Update the optional narrative fields ({@code releaseSummary}, {@code highlights}, {@code knownIssues},
	 * {@code acknowledgements}) on an existing run and persist. Any of the values may be null/blank; those
	 * are simply stored and later omitted from the composed emails. Returns the updated run.
	 *
	 * <p>Does not touch {@code version}, {@code developmentVersion}, or {@code rc} — {@code start()}
	 * uses this after already writing those identity fields.
	 */
	public synchronized RunState updateDetails(String version, String releaseSummary, String highlights,
			String knownIssues, String acknowledgements) {
		var rs = require(version);
		rs.releaseSummary = releaseSummary;
		rs.highlights = highlights;
		rs.knownIssues = knownIssues;
		rs.acknowledgements = acknowledgements;
		store.save(rs);
		return rs;
	}

	/**
	 * Persist narrative fields plus identity fields from the Input form. {@code newVersion} may rename the
	 * run (the store key is {@code release-&lt;version&gt;.json}); a colliding target version is rejected.
	 * Blank {@code developmentVersion} clears the stored value.
	 *
	 * <p>{@code release-prepare} still <em>derives</em> the next SNAPSHOT for maintenance releases
	 * ({@code z>0}) and overwrites {@code developmentVersion} at apply time. Preflight re-resolves
	 * {@code branch} from {@code version}.
	 */
	@SuppressWarnings({
		"java:S107" // Narrative plus identity fields from the Input form; a parameter object would obscure the persistence mapping.
	})
	public synchronized RunState updateDetails(String version, String releaseSummary, String highlights,
			String knownIssues, String acknowledgements, String newVersion, String developmentVersion,
			Integer rc) {
		var rs = require(version);
		rs.releaseSummary = releaseSummary;
		rs.highlights = highlights;
		rs.knownIssues = knownIssues;
		rs.acknowledgements = acknowledgements;
		rs.developmentVersion = ib(developmentVersion) ? null : developmentVersion.trim();
		applyRc(rs, rc);
		var trimmed = newVersion == null ? "" : newVersion.trim();
		if (!trimmed.isEmpty() && !trimmed.equals(version)) {
			if (store.load(trimmed).isPresent())
				throw isex("A run already exists for %s", trimmed);
			rs.version = trimmed;
			rs.branch = branches.resolve(trimmed);
			store.save(rs);
			store.delete(version);
		} else {
			store.save(rs);
		}
		return rs;
	}

	/**
	 * This engine's {@link TargetProfile}.
	 */
	public TargetProfile target() {
		return target;
	}

	/**
	 * Preview a step: no mutation, no persistence, no log reset. Used by unit tests; not exposed as a UI action.
	 */
	public Preview preview(String version, String stepId, Map<String, String> form) {
		var rs = require(version);
		var step = requireStep(stepId);
		return step.preview(context(rs, stepId, form, false));
	}

	/**
	 * Apply a step — first run, resume, or an ad-hoc re-run of an already-terminal step (any step,
	 * independently, regardless of {@code currentStepId}). Persists status transitions; halts on failure.
	 * Overwrites that step's own log in place via {@code resetLog=true}.
	 */
	@SuppressWarnings({
		"java:S3776" // Linear per-outcome status bookkeeping (vote-gate/tally/finalize forks); splitting it would scatter the run's state machine.
	})
	public synchronized StepResult apply(String version, String stepId, Map<String, String> form) {
		var rs = require(version);
		var step = registry.byId(stepId);
		if (step == null)
			return StepResult.fail(UNKNOWN_STEP + stepId);
		var blocked = forwardApplyGuardMessage(rs, stepId);
		if (blocked.isPresent())
			return StepResult.fail(blocked.get());
		var ss = rs.step(stepId);
		ss.status = StepStatus.RUNNING;
		ss.startedAt = Instant.now().toString();
		rs.currentStepId = stepId;
		store.save(rs);

		StepResult result;
		var ctx = context(rs, stepId, form, true); // true = reset (truncate) this step's log first
		ss.logRef = stepLogRelativePath(rs, stepId);
		try {
			result = step.apply(ctx);
		} catch (RuntimeException e) {
			result = StepResult.fail(e.getMessage());
		}

		if (result.success) {
			ss.error = null; // a later success of the same step must not keep a leftover failure message
			if (stepId.equals(VOTE_GATE)) {
				ss.status = StepStatus.AWAITING_VOTE;
				rs.status = RunStatus.AWAITING_VOTE;
			} else if (step.reviewGate()) {
				// The read-only work ran; hold the step for an explicit confirm-review before it counts passed.
				ss.status = StepStatus.AWAITING_REVIEW;
			} else {
				ss.status = StepStatus.SUCCEEDED;
			}
			ss.completedAt = Instant.now().toString();
			if (stepId.equals("tally-vote-result") && "passed".equals(form == null ? null : form.get("voteOutcome"))) {
				// A passing tally is the one action that resolves the vote gate — flip vote-gate's own
				// status to terminal so the forward-apply guard (and finalize-run's prerequisite check)
				// treat it as satisfied. A rejected tally leaves vote-gate AWAITING_VOTE; that path forks
				// to Drop-RC instead of advancing the linear pipeline.
				var gate = rs.step(VOTE_GATE);
				if (gate != null) {
					gate.status = StepStatus.SUCCEEDED;
					gate.completedAt = ss.completedAt;
				}
			}
			if (stepId.equals("finalize-run")) {
				rs.status = RunStatus.RELEASED;
			} else if (rs.status == RunStatus.FAILED) {
				// Unstick: a subsequent success must not leave the run FAILED, or the New-Release page
				// (which keys off non-terminal status) hides the remaining PENDING steps.
				rs.status = RunStatus.RUNNING;
			}
		} else {
			ss.status = StepStatus.FAILED;
			ss.error = result.message;
			rs.status = RunStatus.FAILED;
		}
		store.save(rs);
		return result;
	}

	/**
	 * Mark a step SKIPPED (only steps whose registry entry is skippable).
	 */
	public synchronized StepResult skip(String version, String stepId) {
		var rs = require(version);
		var step = registry.byId(stepId);
		if (step == null)
			return StepResult.fail(UNKNOWN_STEP + stepId);
		if (!step.skippable())
			return StepResult.fail(stepId + " is not skippable.");
		var ss = rs.step(stepId);
		ss.status = StepStatus.SKIPPED;
		store.save(rs);
		return StepResult.ok(stepId + " skipped.");
	}

	/**
	 * Advance a review-gate step held in {@code AWAITING_REVIEW} to {@code SUCCEEDED}.
	 */
	public synchronized StepResult confirmReview(String version, String stepId) {
		var rs = require(version);
		requireStep(stepId);
		var ss = rs.step(stepId);
		if (ss == null)
			return StepResult.fail(UNKNOWN_STEP + stepId);
		if (ss.status != StepStatus.AWAITING_REVIEW)
			return StepResult.fail(stepId + " is not awaiting review.");
		ss.status = StepStatus.SUCCEEDED;
		ss.completedAt = Instant.now().toString();
		store.save(rs);
		return StepResult.ok(stepId + " review confirmed.");
	}

	/**
	 * Persist {@code rc} only when it is a positive integer; empty/invalid input must not wipe a stored value.
	 */
	private static void applyRc(RunState rs, Integer rc) {
		if (rc != null && rc >= 1)
			rs.rc = rc;
	}

	/**
	 * Is {@code status} a terminal-success outcome for {@code step}: always {@code SUCCEEDED}, or
	 * {@code SKIPPED} but only when the step is explicitly markable {@link ReleaseStep#skippable()}.
	 * {@code PENDING}/{@code RUNNING}/{@code FAILED}/{@code AWAITING_VOTE}/{@code AWAITING_REVIEW} never
	 * qualify — those are the exact statuses that let a run reach {@code finalize-run} despite an
	 * unresolved required step.
	 */
	private boolean isTerminalSuccess(ReleaseStep step, StepStatus status) {
		return status == StepStatus.SUCCEEDED || (status == StepStatus.SKIPPED && step.skippable());
	}

	/**
	 * Strict forward-apply guard: refuses {@code stepId} while an earlier required step hasn't reached a
	 * terminal-success state, so a run can never advance past an unsatisfied predecessor — including all
	 * the way to {@code finalize-run}, whose own predecessors are every other step in the pipeline. Returns
	 * empty when {@code stepId} may proceed.
	 *
	 * <p>vote-gate's own {@code AWAITING_VOTE} state is specifically NOT treated as blocking when the step
	 * being applied is {@code tally-vote-result} — recording a tally is the one legitimate action that
	 * resolves an open vote, so it must stay reachable while the gate itself is still open.
	 */
	private Optional<String> forwardApplyGuardMessage(RunState rs, String stepId) {
		var ids = registry.ids();
		var idx = ids.indexOf(stepId);
		if (idx <= 0)
			return Optional.empty(); // unknown id, or the first step: no predecessor to satisfy
		var offending = new ArrayList<String>();
		for (var i = 0; i < idx; i++) {
			var priorId = ids.get(i);
			var priorState = rs.step(priorId);
			// A predecessor is satisfied when it's absent, terminal-success, or the still-open vote-gate that
			// the tally step is specifically allowed to resolve — none of those block forward apply.
			var satisfied = priorState == null || isTerminalSuccess(registry.byId(priorId), priorState.status)
					|| (priorId.equals(VOTE_GATE) && stepId.equals("tally-vote-result")
							&& priorState.status == StepStatus.AWAITING_VOTE);
			if (satisfied)
				continue;
			offending.add(priorId + " (" + priorState.status + ")");
		}
		if (offending.isEmpty())
			return Optional.empty();
		return Optional.of("Blocked: '" + stepId + "' requires these prior step(s) to succeed first: "
				+ String.join(", ", offending));
	}

	/**
	 * On boot: demote any RUNNING step to FAILED (its subprocess died with the JVM).
	 */
	public void recoverOnBoot() {
		for (var rs : store.loadAll()) {
			var current = rs.currentStepId == null ? null : rs.step(rs.currentStepId);
			if (current != null && current.status == StepStatus.RUNNING) {
				current.status = StepStatus.FAILED;
				current.error = "interrupted by server restart";
				rs.status = RunStatus.FAILED;
				store.save(rs);
			}
		}
	}

	/**
	 * {@code logs/<version>-RC<n>-<stepId>.log}, relative to {@code stateDir}.
	 */
	private String stepLogRelativePath(RunState rs, String stepId) {
		return "logs/" + rs.version + "-RC" + rs.rc + "-" + stepId + ".log";
	}

	/**
	 * Builds a step's {@link StepContext}, wiring {@code ctx.log} to that step's own {@link RunLog}. When
	 * {@code resetLog} is true (an actual apply/resume/re-run, never a preview), the log is truncated first
	 * so a re-run overwrites in place rather than appending after a stale prior invocation's output.
	 */
	private StepContext context(RunState rs, String stepId, Map<String, String> form, boolean resetLog) {
		var ctx = new StepContext();
		ctx.run = rs;
		ctx.runner = runner;
		ctx.target = target;
		ctx.nexus = secrets.nexus();
		var log = new RunLog(stateDir.resolve(stepLogRelativePath(rs, stepId)), broadcaster(rs.version, stepId));
		if (resetLog)
			log.reset();
		ctx.log = log.lineSink();
		ctx.stagingRepo = stagingRoot.resolve("git/juneau");
		ctx.stateDir = stateDir;
		ctx.repoDir = repoDir;
		ctx.committerEmail = committerEmail;
		ctx.availid = secrets.availid();
		ctx.ldapPassword = secrets.ldapPassword();
		ctx.gpgKeyId = secrets.gpgKeyId();
		ctx.gpgPassphrase = secrets.gpgPassphrase();
		ctx.githubToken = secrets.githubToken();
		ctx.email = email;
		ctx.milestone = milestone;
		ctx.formInputs = form == null ? Map.of() : form;
		return ctx;
	}

	private RunState require(String version) {
		return store.load(version).orElseThrow(() -> isex("No run for %s", version));
	}

	private ReleaseStep requireStep(String stepId) {
		var s = registry.byId(stepId);
		if (s == null)
			throw iaex("Unknown step: %s", stepId);
		return s;
	}

	/**
	 * This engine's {@link StepRegistry}.
	 */
	public StepRegistry registry() {
		return registry;
	}

	/**
	 * Exposes the wired {@link SecretResolver} so the REST layer can plumb it into {@link DropRcService}'s
	 * {@code Supplier<String>} seams without duplicating the Keychain-backed resolution here.
	 */
	public SecretResolver secrets() {
		return secrets;
	}
}
