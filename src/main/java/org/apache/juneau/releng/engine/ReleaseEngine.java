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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.juneau.releng.config.TargetProfile;
import org.apache.juneau.releng.email.EmailService;
import org.apache.juneau.releng.log.LogBroadcaster;
import org.apache.juneau.releng.log.RunLog;
import org.apache.juneau.releng.milestone.MilestoneService;
import org.apache.juneau.releng.nexus.NexusStagingClient;
import org.apache.juneau.releng.util.ProcessRunner;

/** Single-active-run orchestrator. One run advances at a time; state is persisted after every step. */
public class ReleaseEngine {

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

	// Invoked when a new run starts, e.g. to reset the SAFE Nexus loopback mock. Defaults to a no-op.
	private Runnable runStartHook = () -> {
		// No-op by default; the SAFE-mode wiring installs a mock-reset hook.
	};

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

	public Optional<RunState> activeRun() {
		return store.activeRun();
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
	 * no-ops).
	 */
	public synchronized RunState start(String version, String developmentVersion, Integer milestoneNumber) {
		var active = store.activeRun();
		if (active.isPresent())
			throw isex("A run is already active: %s (%s). Finish or drop it first.", active.get().version,
					active.get().currentStepId);
		var branch = branches.resolve(version);
		var rs = RunState.create(version, branch, registry.ids());
		rs.developmentVersion = developmentVersion;
		rs.milestoneNumber = milestoneNumber;
		store.save(rs);
		runStartHook.run(); // e.g. reset the SAFE Nexus loopback mock so this run starts from a clean slate
		return rs;
	}

	/** Installs a callback invoked on each {@link #start}, e.g. to reset the SAFE Nexus loopback mock. */
	public void setRunStartHook(Runnable hook) {
		this.runStartHook = hook == null ? () -> {
			// No-op: clearing the hook restores default behavior.
		} : hook;
	}

	public ExecutionMode mode() {
		return mode;
	}

	public TargetProfile target() {
		return target;
	}

	/**
	 * Arms {@code version} for LIVE mutation. Rejected unless mode is LIVE and {@code confirm} equals the
	 * required phrase {@code "<version> LIVE"}. Returns the outcome message-bearing result.
	 */
	public synchronized StepResult arm(String version, String confirm) {
		if (mode != ExecutionMode.LIVE)
			return StepResult.fail("Arming is only possible in LIVE mode; this box is running in SAFE mode.");
		if (!Objects.equals(confirm, version + " LIVE"))
			return StepResult.fail("Type '" + version + " LIVE' to arm this run for live mutation.");
		armedVersion = version;
		return StepResult.ok("Run " + version + " is armed for LIVE mutation.");
	}

	public boolean isArmed(String version) {
		return version != null && version.equals(armedVersion);
	}

	public synchronized void disarm(String version) {
		if (isArmed(version))
			armedVersion = null;
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
	public synchronized StepResult apply(String version, String stepId, Map<String, String> form) {
		var rs = require(version);
		var step = requireStep(stepId);
		// Default-safe guard chokepoint: a mutating step is refused in LIVE until the run is armed. In SAFE it
		// is allowed to run (simulated) so the pipeline can be rehearsed end-to-end with no canonical effect.
		if (step.mutating() && mode == ExecutionMode.LIVE && !isArmed(version))
			return StepResult.fail(guardMessage(step));
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
			if (stepId.equals("vote-gate")) {
				ss.status = StepStatus.AWAITING_VOTE;
				rs.status = RunStatus.AWAITING_VOTE;
			} else if (step.reviewGate()) {
				// The read-only work ran; hold the step for an explicit confirm-review before it counts passed.
				ss.status = StepStatus.AWAITING_REVIEW;
			} else {
				ss.status = StepStatus.SUCCEEDED;
			}
			ss.completedAt = Instant.now().toString();
			if (stepId.equals("finalize-run")) {
				rs.status = RunStatus.RELEASED;
				disarm(version); // the run is complete; drop the arm
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
		var step = requireStep(stepId);
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
			return StepResult.fail("Unknown step: " + stepId);
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
		ctx.mode = mode;
		ctx.target = target;
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
		ctx.nexus = secrets.nexus();
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
