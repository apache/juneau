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

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.juneau.releng.config.TargetProfile;
import org.apache.juneau.releng.log.RunEventStore;
import org.apache.juneau.releng.log.RunLog;
import org.apache.juneau.releng.nexus.NexusStagingClient;
import org.apache.juneau.releng.util.ProcessRunner;
import org.apache.juneau.releng.util.SvnArgs;
import org.apache.juneau.rest.server.runreport.RunEvent.EndStatus;
import org.apache.juneau.rest.server.runreport.RunEvent.Level;

/**
 * The one coarse Drop-RC action: drop remote state, bump RC, reset from workspace-setup.
 */
public class DropRcService {

	/**
	 * Pseudo-step id under which Drop-RC's own log file is keyed (it isn't a registry step).
	 */
	public static final String LOG_STEP_ID = "drop-rc";

	private final RunStateStore store;
	private final StepRegistry registry;
	private final ProcessRunner runner;
	private final Path stagingRepo;
	private final Path stateDir;
	private final NexusStagingClient nexus;
	private final TargetProfile target;
	private final RunEventStore events;

	/**
	 * Constructor injecting all of this service's collaborators.
	 */
	@SuppressWarnings({
		"java:S107" // Constructor-injected collaborators; a parameter object would obscure the wiring.
	})
	public DropRcService(RunStateStore store, StepRegistry registry, ProcessRunner runner, Path stagingRepo,
			Path stateDir, NexusStagingClient nexus, TargetProfile target) {
		this(store, registry, runner, stagingRepo, stateDir, nexus, target, new RunEventStore(stateDir));
	}

	/**
	 * Constructor injecting all of this service's collaborators, including the run-view event store the engine writes to.
	 */
	@SuppressWarnings({
		"java:S107" // Constructor-injected collaborators; a parameter object would obscure the wiring.
	})
	public DropRcService(RunStateStore store, StepRegistry registry, ProcessRunner runner, Path stagingRepo,
			Path stateDir, NexusStagingClient nexus, TargetProfile target, RunEventStore events) {
		this.events = events;
		this.store = store;
		this.registry = registry;
		this.runner = runner;
		this.stagingRepo = stagingRepo;
		this.stateDir = stateDir;
		this.nexus = nexus;
		this.target = target == null ? TargetProfile.prodDefault() : target;
	}

	/**
	 * This action's own log. Truncated at the start of every {@link #apply} so a re-drop overwrites in place.
	 */
	private Consumer<String> logSink(RunState rs) {
		var path = stateDir.resolve("logs/" + rs.version + "-RC" + rs.rc + "-" + LOG_STEP_ID + ".log");
		var log = new RunLog(path);
		log.reset();
		return log.lineSink();
	}

	/**
	 * Compute the drop plan without executing.
	 */
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

	/**
	 * Execute the drop, bump RC, and reset.
	 */
	public synchronized void apply(String version, String reason, Supplier<String> availid, Supplier<String> password) {
		var rs = store.load(version).orElseThrow();
		var tag = "juneau-" + rs.version + "-RC" + rs.rc;
		var git = stagingRepo.toString();
		var pw = password.get();
		var log = logSink(rs);

		if (rs.nexusRepoId != null && nexus != null) {
			log.accept("Dropping Nexus staging repo " + rs.nexusRepoId);
			nexus.drop(rs.nexusRepoId);
		}
		var dist = stateDir.resolve("dist");
		runner.run(List.of("svn", "checkout", SvnArgs.USERNAME, availid.get(), SvnArgs.PASSWORD_FROM_STDIN,
				target.distDevBase(), dist.toString()), pw + "\n", Map.of());
		runner.run(List.of("svn", "rm", dist.resolve("source").resolve(tag).toString()), null, null);
		runner.run(List.of("svn", "rm", dist.resolve("binaries").resolve(tag).toString()), null, null);
		runner.run(List.of("svn", "commit", dist.toString(), "-m", "Drop " + tag, SvnArgs.USERNAME, availid.get(),
				SvnArgs.PASSWORD_FROM_STDIN), pw + "\n", Map.of());
		runner.run(List.of("git", "-C", git, "tag", "-d", tag), null, null);
		runner.run(List.of("git", "-C", git, "push", "origin", ":refs/tags/" + tag), null, null);
		runner.run(List.of("mvn", "-f", git + "/pom.xml", "release:rollback"), null, null);

		rs.rcHistory.add(new RcHistoryEntry(rs.rc, Instant.now().toString(), reason));
		events.endOpen(rs.version, EndStatus.SKIP);
		events.note(rs.version, Level.WARN, "RC" + rs.rc + " dropped" + (reason == null || reason.isBlank() ? "" : ": " + reason)
			+ "; continuing with RC" + (rs.rc + 1), null, null);
		rs.rc = rs.rc + 1;
		rs.status = RunStatus.RUNNING;
		rs.nexusRepoId = null;
		rs.voteDeadline = null;
		// Nothing to clear at the run level here; the per-step reset below handles each stale log reference.

		// Clearing each step's stale log reference below also stops the per-step SSE endpoint from
		// replaying the previous RC's output under the new RC.
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
