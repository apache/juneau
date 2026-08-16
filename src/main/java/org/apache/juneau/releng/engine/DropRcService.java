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
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.apache.juneau.releng.config.TargetProfile;
import org.apache.juneau.releng.log.LogBroadcaster;
import org.apache.juneau.releng.log.RunLog;
import org.apache.juneau.releng.nexus.NexusStagingClient;
import org.apache.juneau.releng.util.ProcessRunner;
import org.apache.juneau.releng.util.SvnArgs;

/** The one coarse Drop-RC action: drop remote state, bump RC, reset from workspace-setup. */
public class DropRcService {

	/** Pseudo-step id under which Drop-RC's own log file + broadcaster are keyed (it isn't a registry step). */
	public static final String LOG_STEP_ID = "drop-rc";

	private final RunStateStore store;
	private final StepRegistry registry;
	private final ProcessRunner runner;
	private final Path stagingRepo;
	private final Path stateDir;
	private final NexusStagingClient nexus;
	private NexusStagingClient safeNexus;
	private final ExecutionMode mode;
	private final Predicate<String> armed;
	private final TargetProfile target;
	private final BiFunction<String, String, LogBroadcaster> broadcasterFn;

	@SuppressWarnings({ "java:S107" // Constructor-injected collaborators; a parameter object would obscure the wiring.
	})
	public DropRcService(RunStateStore store, StepRegistry registry, ProcessRunner runner, Path stagingRepo,
			Path stateDir, NexusStagingClient nexus, ExecutionMode mode, Predicate<String> armed, TargetProfile target,
			BiFunction<String, String, LogBroadcaster> broadcasterFn) {
		this.store = store;
		this.registry = registry;
		this.runner = runner;
		this.stagingRepo = stagingRepo;
		this.stateDir = stateDir;
		this.nexus = nexus;
		this.mode = mode == null ? ExecutionMode.SAFE : mode;
		this.armed = armed == null ? v -> false : armed;
		this.target = target == null ? TargetProfile.prodDefault() : target;
		this.broadcasterFn = broadcasterFn == null ? (v, s) -> new LogBroadcaster() : broadcasterFn;
	}

	/**
	 * Placeholder Nexus client aimed at the in-app loopback mock. Used for Dry-run drops on a LIVE box so
	 * they never hit real Nexus. Null in tests keeps {@link #nexus}.
	 */
	public void setSafeNexus(NexusStagingClient client) {
		this.safeNexus = client;
	}

	private ExecutionMode effectiveMode(RunState rs) {
		var requested = rs.mode != null ? rs.mode : this.mode;
		if (requested == ExecutionMode.LIVE && this.mode != ExecutionMode.LIVE)
			return ExecutionMode.SAFE;
		return requested;
	}

	private boolean live(RunState rs) {
		return effectiveMode(rs) == ExecutionMode.LIVE;
	}

	/**
	 * This action's own log — mirrors how {@code ReleaseEngine.context()} builds a step's {@link RunLog}, so
	 * Drop-RC's Tier-B calls get the same "would run:" command-logging fidelity a registry step's do (rather
	 * than the silent no-op this had before). Truncated at the start of every {@link #apply} so a re-drop
	 * overwrites in place.
	 */
	private Consumer<String> logSink(RunState rs) {
		var path = stateDir.resolve("logs/" + rs.version + "-RC" + rs.rc + "-" + LOG_STEP_ID + ".log");
		var log = new RunLog(path, broadcasterFn.apply(rs.version, LOG_STEP_ID));
		log.reset();
		return log.lineSink();
	}

	/**
	 * The Tier-B seam for Drop-RC's mutating subprocess calls: runs the command in LIVE, logs a redacted
	 * "would run:" line and no-ops in SAFE so a rehearsed drop has zero canonical side effects (the Tier-A
	 * Nexus drop still round-trips the mock).
	 */
	private void tierB(List<String> command, String stdin, Map<String, String> env, Consumer<String> log, boolean live) {
		if (live) {
			runner.run(command, stdin, env);
			return;
		}
		var line = "would run: " + String.join(" ", StepContext.redactArgv(command));
		if (stdin != null)
			line += " <stdin:redacted>";
		log.accept(line);
	}

	/** Compute the drop plan without executing. */
	public Preview preview(String version) {
		var rs = store.load(version).orElseThrow();
		var tag = "juneau-" + rs.version + "-RC" + rs.rc;
		var p = new Preview(LOG_STEP_ID, true);
		p.line("Drop Nexus staging repo: " + rs.nexusRepoId);
		p.line("svn rm dist/dev/juneau/{source,binaries}/" + tag);
		p.line("Delete tag " + tag + " (local + remote)");
		p.line("mvn release:rollback in staging clone");
		p.line("Then bump to RC" + (rs.rc + 1) + " and reset from workspace-setup.");
		return p;
	}

	/** Execute the drop, bump RC, and reset. */
	public synchronized void apply(String version, String reason, Supplier<String> availid, Supplier<String> password) {
		var rs = store.load(version).orElseThrow();
		// Default-safe guard chokepoint (mirrors ReleaseEngine.apply): the real destructive drop only runs on
		// a LIVE run once it is armed. A Dry-run rehearses it (Tier-A drop against the mock, Tier-B logged).
		if (live(rs) && !armed.test(version))
			throw isex("Refused: drop-RC is a mutating action. Enable LIVE mode and arm run %s first.", version);
		var tag = "juneau-" + rs.version + "-RC" + rs.rc;
		var git = stagingRepo.toString();
		var pw = password.get();
		var log = logSink(rs);
		var live = live(rs);
		var safeClient = safeNexus != null ? safeNexus : nexus;
		var client = live ? nexus : safeClient;

		// a) drop Nexus staging repo (Tier A: real client round-trip, against the loopback mock in SAFE)
		if (rs.nexusRepoId != null && client != null) {
			log.accept("Dropping Nexus staging repo " + rs.nexusRepoId);
			client.drop(rs.nexusRepoId);
		}
		// b) svn checkout dist/dev, rm the rejected RC's directories, commit
		var dist = stateDir.resolve("dist");
		tierB(List.of("svn", "checkout", SvnArgs.USERNAME, availid.get(), SvnArgs.PASSWORD_FROM_STDIN,
				target.distDevBase(), dist.toString()), pw + "\n", Map.of(), log, live);
		tierB(List.of("svn", "rm", dist.resolve("source").resolve(tag).toString()), null, null, log, live);
		tierB(List.of("svn", "rm", dist.resolve("binaries").resolve(tag).toString()), null, null, log, live);
		tierB(List.of("svn", "commit", dist.toString(), "-m", "Drop " + tag, SvnArgs.USERNAME, availid.get(),
				SvnArgs.PASSWORD_FROM_STDIN), pw + "\n", Map.of(), log, live);
		// c) delete tag local + remote
		tierB(List.of("git", "-C", git, "tag", "-d", tag), null, null, log, live);
		tierB(List.of("git", "-C", git, "push", "origin", ":refs/tags/" + tag), null, null, log, live);
		// d) roll back the release:prepare version-bump commits
		tierB(List.of("mvn", "-f", git + "/pom.xml", "release:rollback"), null, null, log, live);

		// 4) reset state
		rs.rcHistory.add(new RcHistoryEntry(rs.rc, Instant.now().toString(), reason));
		rs.rc = rs.rc + 1;
		rs.status = RunStatus.RUNNING;
		rs.nexusRepoId = null;
		rs.voteDeadline = null;
		// Nothing to clear at the run level here; the per-step reset below handles each stale log reference.

		// 5) Reset every step from workspace-setup onward back to PENDING, keeping preflight and
		//    compose-propose-email as they are. Clearing each step's stale log reference below also
		//    stops the per-step SSE endpoint from replaying the previous RC's output under the new RC.
		var ids = registry.ids();
		var resetFrom = ids.indexOf(StepRegistry.DROP_RC_RESET_FROM);
		for (var i = 0; i < ids.size(); i++) {
			if (i >= resetFrom) {
				var ss = rs.step(ids.get(i));
				ss.status = StepStatus.PENDING;
				ss.startedAt = null;
				ss.completedAt = null;
				ss.error = null;
				ss.logOffset = null;
				ss.logRef = null;
			}
		}
		rs.currentStepId = StepRegistry.DROP_RC_RESET_FROM;
		store.save(rs);
	}
}
