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

package org.apache.juneau.releng.log;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.juneau.releng.engine.RunStateStore;
import org.apache.juneau.releng.engine.StepState;
import org.apache.juneau.rest.server.views.ConsoleOutputLine.Style;
import org.apache.juneau.rest.server.views.ConsoleOutputSource;
import org.apache.juneau.rest.server.views.FileConsoleOutputSource;
import org.apache.juneau.rest.server.views.FileConsoleOutputSource.Status;

/**
 * Serves each release step's own log file as a console-output source.
 *
 * <p>
 * The file path always comes from the persisted {@link StepState#logRef}, never from the request, so a request can
 * only choose among the logs of runs and steps that exist. One {@link FileConsoleOutputSource} is kept per log file
 * because its line index lives in the instance; the status header is recomputed from the stored step state on every
 * request, so it follows the step as it runs and settles.
 */
public class StepOutputSources {

	private final RunStateStore store;
	private final Map<Path, FileConsoleOutputSource> sources = new ConcurrentHashMap<>();

	/**
	 * Creates the registry over the store the engine persists runs to.
	 *
	 * @param store The run state store.
	 */
	public StepOutputSources(RunStateStore store) {
		this.store = store;
	}

	/**
	 * Finds the console-output source for one step's log.
	 *
	 * @param version The run version.
	 * @param stepId The step id.
	 * @return The source, or empty when there is no such run or step, or the step has not written a log yet.
	 */
	public Optional<ConsoleOutputSource> find(String version, String stepId) {
		var step = store.load(version).map(rs -> rs.step(stepId)).orElse(null);
		if (step == null || step.logRef == null)
			return Optional.empty();
		var root = store.stateDir().toAbsolutePath().normalize();
		var file = root.resolve(step.logRef).normalize();
		if (! file.startsWith(root))
			return Optional.empty();
		return Optional.of(sources.computeIfAbsent(file, f -> FileConsoleOutputSource.create(f).status(() -> status(version, stepId)).build()));
	}

	/**
	 * Maps a step's persisted state onto the console header.
	 *
	 * <p>
	 * Only a step that has settled (succeeded, failed or skipped) or is parked waiting for a person is terminal, so the
	 * console stops polling then. A step that has not started yet stays non-terminal so its output appears when it runs.
	 *
	 * @param version The run version.
	 * @param stepId The step id.
	 * @return The status.
	 */
	Status status(String version, String stepId) {
		var step = store.load(version).map(rs -> rs.step(stepId)).orElse(null);
		if (step == null)
			return new Status("MISSING", Style.MUTED, true, null, null);
		var started = parse(step.startedAt);
		return switch (step.status) {
			case PENDING -> new Status("PENDING", Style.MUTED, false, null, null);
			case RUNNING -> new Status("RUNNING", Style.ACCENT, false, started, null);
			case SUCCEEDED -> settled("SUCCEEDED", Style.SUCCESS, step, started);
			case FAILED -> settled("FAILED", Style.ERROR, step, started);
			case SKIPPED -> settled("SKIPPED", Style.MUTED, step, started);
			case AWAITING_VOTE -> settled("AWAITING VOTE", Style.WARN, step, started);
			case AWAITING_REVIEW -> settled("AWAITING REVIEW", Style.WARN, step, started);
		};
	}

	private static Status settled(String state, Style style, StepState step, Instant started) {
		var completed = parse(step.completedAt);
		var duration = started != null && completed != null ? Duration.between(started, completed).toMillis() : null;
		return new Status(state, style, true, started, duration);
	}

	private static Instant parse(String iso) {
		if (iso == null)
			return null;
		try {
			return Instant.parse(iso);
		} catch (DateTimeParseException e) {
			return null;
		}
	}
}
