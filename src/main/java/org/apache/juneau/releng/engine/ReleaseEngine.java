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
import java.util.Objects;
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

/** Single-active-run orchestrator. One run advances at a time; state is persisted after every step. */
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
	private final ExecutionMode mode;
	private final TargetProfile target;

	// The armed run, if any. Transient in-memory posture — deliberately NOT persisted, so it drops on any
	// restart (re-arm required after a restart/crash). Only meaningful in LIVE mode.
	private volatile String armedVersion;

	// In-memory per-step broadcasters, keyed "version/stepId". Lost on restart; log files survive.
	private final Map<String, LogBroadcaster> broadcasters = new ConcurrentHashMap<>();

	// In-memory per-run run-state broadcasters, keyed by version. Lost on restart; a reconnecting SSE
	// client gets a fresh initial snapshot instead (see AppConfiguration's state resolver).
	private final Map<String, RunStateBroadcaster> stateBroadcasters = new ConcurrentHashMap<>();

	// Invoked when a new run starts, e.g. to reset the SAFE Nexus loopback mock. Defaults to a no-op.
	private Runnable runStartHook = () -> {
		// No-op by default; the SAFE-mode wiring installs a mock-reset hook.
	};

	// Loopback mock base (http://host:port/mock/nexus). Null in forTests so those keep secrets.nexus().
	private String mockNexusBaseUrl;
	private Map<String, String> loopbackHeaders = Map.of();

	/** Everything the REST layer must provide to build a mutating StepContext. */
	public interface SecretResolver {
		String availid();

		String ldapPassword();

		String gpgKeyId();

		String gpgPassphrase();

		String githubToken();

		NexusStagingClient nexus();
	}

	@SuppressWarnings({ "java:S107" // Constructor-injected collaborators; a parameter object would obscure the wiring.
	})
	public ReleaseEngine(RunStateStore store, StepRegistry registry, ProcessRunner runner, BranchResolver branches,
			Path stateDir, Path stagingRoot, String repoDir, String committerEmail, EmailService email,
			MilestoneService milestone, SecretResolver secrets, ExecutionMode mode, TargetProfile target) {
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
		this.mode = mode == null ? ExecutionMode.SAFE : mode;
		this.target = target == null ? TargetProfile.prodDefault() : target;
		// The single choke point for the New-Release tab's live rail push: every status-mutating
		// transition — this engine's own methods AND DropRcService's drop-RC action, since it shares this
		// same RunStateStore instance — ultimately calls store.save(), so hooking it here catches all of
		// them without a separate publish call at each mutation site.
		store.setOnSave(this::publishSnapshot);
	}

	/** Minimal test factory (no secrets/nexus/email). */
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
				ExecutionMode.SAFE, TargetProfile.prodDefault());
	}

	/** Test factory pinned to a specific execution mode (for guard/arm/tier tests). */
	public static ReleaseEngine forTests(RunStateStore store, StepRegistry registry, ProcessRunner runner,
			BranchResolver branches, Path stateDir, ExecutionMode mode) {
		var engine = forTests(store, registry, runner, branches, stateDir);
		return new ReleaseEngine(store, registry, runner, branches, stateDir, stateDir.resolve("staging"), "/repo",
				"test@apache.org", engine.email, engine.milestone, engine.secrets, mode, TargetProfile.prodDefault());
	}

	/** One broadcaster per (version, step). */
	public LogBroadcaster broadcaster(String version, String stepId) {
		return broadcasters.computeIfAbsent(version + "/" + stepId, k -> new LogBroadcaster());
	}

	/** One run-state broadcaster per version, for the New-Release tab's live rail push. */
	public RunStateBroadcaster stateBroadcaster(String version) {
		return stateBroadcasters.computeIfAbsent(version, k -> new RunStateBroadcaster());
	}

	/** {@code version}'s current snapshot as JSON, or empty when there's no persisted run for it. */
	public Optional<String> snapshotJson(String version) {
		return store.load(version)
				.map(rs -> Json.DEFAULT.write(RunStateSnapshot.of(rs, effectiveMode(rs), isArmed(version))));
	}

	/**
	 * Builds {@code rs}'s snapshot and pushes it to that version's {@link RunStateBroadcaster}. Registered
	 * as {@link RunStateStore}'s {@code onSave} hook (see the constructor), and also called directly from
	 * {@link #arm}/{@link #disarm}, since arming is transient in-memory posture that never itself triggers
	 * a {@code save()}.
	 */
	private void publishSnapshot(RunState rs) {
		stateBroadcaster(rs.version)
				.publish(Json.DEFAULT.write(RunStateSnapshot.of(rs, effectiveMode(rs), isArmed(rs.version))));
	}

	/** Same as {@link #publishSnapshot(RunState)}, reloading the current persisted state for {@code version}. */
	private void publishSnapshot(String version) {
		store.load(version).ifPresent(this::publishSnapshot);
	}

	public Optional<RunState> activeRun() {
		return store.activeRun();
	}

	/** The run the New-Release page should render — see {@link RunStateStore#displayRun()}. */
	public Optional<RunState> displayRun() {
		return store.displayRun();
	}

	public RunState state(String version) {
		return store.load(version).orElse(null);
	}

	/** Start a new run; enforces the single-active-run lock. */
	public synchronized RunState start(String version, String developmentVersion) {
		return start(version, developmentVersion, null);
	}

	/**
	 * Start a new run, recording the milestone number resolved (or overridden) on the New-Release form.
	 * {@code milestoneNumber} may be null (no matching milestone; {@code milestone-close} then legitimately
	 * no-ops). Defaults the run to Dry-run (SAFE).
	 */
	public synchronized RunState start(String version, String developmentVersion, Integer milestoneNumber) {
		return start(version, developmentVersion, milestoneNumber, null);
	}

	/**
	 * Start a new run. {@code requestedMode} is the form's Dry-run/Actual choice; null means Dry-run.
	 * Actual (LIVE) is honored only when this box was started with {@code rm.mode=live} — a SAFE box
	 * always caps the run to SAFE.
	 */
	public synchronized RunState start(String version, String developmentVersion, Integer milestoneNumber,
			ExecutionMode requestedMode) {
		var active = store.activeRun();
		if (active.isPresent())
			throw isex("A run is already active: %s (%s). Finish or drop it first.", active.get().version,
					active.get().currentStepId);
		var branch = branches.resolve(version);
		var rs = RunState.create(version, branch, registry.ids());
		rs.developmentVersion = developmentVersion;
		rs.milestoneNumber = milestoneNumber;
		rs.mode = capMode(requestedMode);
		store.save(rs);
		runStartHook.run(); // e.g. reset the SAFE Nexus loopback mock so this run starts from a clean slate
		return rs;
	}

	/**
	 * Update the optional narrative fields ({@code releaseSummary}, {@code highlights}, {@code knownIssues},
	 * {@code acknowledgements}) on an existing run and persist. Any of the values may be null/blank; those
	 * are simply stored and later omitted from the composed emails. Returns the updated run.
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

	/** Installs a callback invoked on each {@link #start}, e.g. to reset the SAFE Nexus loopback mock. */
	public void setRunStartHook(Runnable hook) {
		this.runStartHook = hook == null ? () -> {
			// No-op: clearing the hook restores default behavior.
		} : hook;
	}

	/** Loopback mock base used for Dry-run Nexus callouts. Null in {@link #forTests} keeps {@code secrets.nexus()}. */
	public void setMockNexusBaseUrl(String url) {
		this.mockNexusBaseUrl = url;
	}

	public String mockNexusBaseUrl() {
		return mockNexusBaseUrl;
	}

	/**
	 * Headers the SAFE-mode Nexus client must present to get past the loopback write boundary, since the mock it
	 * talks to is mounted on this application's own port. Empty in {@link #forTests}, where no boundary is
	 * installed and the transport is a stub anyway.
	 *
	 * @see org.apache.juneau.rest.server.filter.LoopbackBoundary#selfCallHeaders()
	 */
	public void setLoopbackHeaders(Map<String, String> headers) {
		this.loopbackHeaders = headers == null ? Map.of() : Map.copyOf(headers);
	}

	/**
	 * The run's effective mode: persisted {@code rs.mode} (null → SAFE), capped so a SAFE box can never
	 * execute LIVE even if on-disk state claims it.
	 */
	public ExecutionMode effectiveMode(RunState rs) {
		return capMode(rs == null ? null : rs.mode);
	}

	private ExecutionMode capMode(ExecutionMode requested) {
		var req = requested != null ? requested : ExecutionMode.SAFE;
		if (req == ExecutionMode.LIVE && mode != ExecutionMode.LIVE)
			return ExecutionMode.SAFE;
		return req;
	}

	public ExecutionMode mode() {
		return mode;
	}

	public TargetProfile target() {
		return target;
	}

	/**
	 * Arms {@code version} for LIVE mutation. Rejected unless the box is LIVE, this run is Actual (LIVE),
	 * and {@code confirm} equals the required phrase {@code "<version> LIVE"}. Returns the outcome
	 * message-bearing result.
	 *
	 * <p><b>Arming is an intent gate, and only that.</b> It establishes that a human meant to do something
	 * irreversible; it establishes nothing about who or what sent the request. The two questions are separate, and
	 * the second one is answered by
	 * {@link org.apache.juneau.rest.server.filter.LoopbackBoundary} — see {@code AppConfiguration}.
	 *
	 * <p>Specifically, <b>the confirm phrase is not a secret and carries no authenticity.</b> It is
	 * {@code "<version> LIVE"}, and the version is displayed on the very page an attacker would be reading, so any
	 * page in the operator's browser could derive it. Before the boundary existed, a hostile page could POST that
	 * phrase to {@code /arm} as a plain cross-origin form submission and then trigger a mutating step. What the
	 * phrase does buy is real but narrower than it looks: it makes an irreversible action require deliberate typing
	 * rather than a misplaced click, which is worth having, and it is worth being clear that this is all it is.
	 *
	 * <p><b>Do not respond to that by making the phrase harder to guess.</b> A longer or hidden phrase would not
	 * help. Anything the page must display so the operator can type it is readable by any script running in that
	 * browser, and anything the operator memorises instead gets written down. A secret shared with the attacker is
	 * not a secret, and dressing this gate up as authentication would obscure the fact that authentication is a
	 * separate control that has to exist on its own — which is what the boundary is. Keep this phrase exactly as
	 * hard to type as it needs to be to prevent an accident, and no harder.
	 */
	public synchronized StepResult arm(String version, String confirm) {
		if (mode != ExecutionMode.LIVE)
			return StepResult.fail("Arming is only possible in LIVE mode; this box is running in SAFE mode.");
		var rs = require(version);
		if (effectiveMode(rs) != ExecutionMode.LIVE)
			return StepResult.fail("Arming is only possible for an Actual (LIVE) run; this run is Dry-run.");
		if (!Objects.equals(confirm, version + " LIVE"))
			return StepResult.fail("Type '" + version + " LIVE' to arm this run for live mutation.");
		armedVersion = version;
		publishSnapshot(version); // armed flag isn't part of RunState, so arming alone never calls store.save()
		return StepResult.ok("Run " + version + " is armed for LIVE mutation.");
	}

	public boolean isArmed(String version) {
		return version != null && version.equals(armedVersion);
	}

	public synchronized void disarm(String version) {
		if (isArmed(version)) {
			armedVersion = null;
			publishSnapshot(version); // same rationale as arm() above
		}
	}

	/** Preview a step: no mutation, no persistence, no log reset. */
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
		// Default-safe guard chokepoint: a mutating step is refused on a LIVE run until it is armed. A
		// Dry-run (SAFE) simulates without arming, even when the box itself was started with rm.mode=live.
		if (step.mutating() && effectiveMode(rs) == ExecutionMode.LIVE && !isArmed(version))
			return StepResult.fail(guardMessage(step));
		// Strict forward-apply guard: refuses to run stepId ahead of an unsatisfied required predecessor.
		// This is also finalize-run's own prerequisite check, since finalize-run's predecessors are every
		// other step in the pipeline — no separate check is needed there.
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
		ss.logRef = stepLogRelativePath(rs, stepId); // per-step logRef
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
				disarm(version); // the run is complete; drop the arm
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

	/** Mark a step SKIPPED (only steps whose registry entry is skippable). */
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

	/** Advance a review-gate step held in {@code AWAITING_REVIEW} to {@code SUCCEEDED}. */
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

	private String guardMessage(ReleaseStep step) {
		return "Refused: '" + step.id() + "' is a mutating step. Enable LIVE mode and arm this run first.";
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

	/** On boot: demote any RUNNING step to FAILED (its subprocess died with the JVM). */
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

	/** {@code logs/<version>-RC<n>-<stepId>.log}, relative to {@code stateDir}. */
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
		var runMode = effectiveMode(rs);
		ctx.mode = runMode;
		if (runMode == ExecutionMode.SAFE && mockNexusBaseUrl != null) {
			ctx.target = target.withNexusBaseUrl(mockNexusBaseUrl);
			ctx.nexus = NexusStagingClient.create(mockNexusBaseUrl, target.nexusProfileId(), "safe-placeholder",
					"safe-placeholder", loopbackHeaders);
		} else {
			ctx.target = target;
			ctx.nexus = secrets.nexus();
		}
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

	public StepRegistry registry() {
		return registry;
	}

	/** Exposes the wired {@link SecretResolver} so the REST layer can plumb it into {@link DropRcService}'s
	 *  {@code Supplier<String>} seams without duplicating the Keychain-backed resolution here. */
	public SecretResolver secrets() {
		return secrets;
	}
}
