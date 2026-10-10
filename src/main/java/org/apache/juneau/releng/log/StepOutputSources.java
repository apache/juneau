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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.juneau.releng.engine.RunStateStore;
import org.apache.juneau.rest.server.terminal.FileTerminalSource;
import org.apache.juneau.rest.server.terminal.TerminalSource;

/**
 * Serves each release step's own log file as a terminal source.
 *
 * <p>
 * The file path always comes from the persisted {@link org.apache.juneau.releng.engine.StepState#logRef}, never from
 * the request, so a request can only choose among the logs of runs and steps that exist. One
 * {@link FileTerminalSource} is kept per log file because it remembers whether the file has been seen; whether the step
 * has settled is recomputed from the stored step state on every read, so the terminal follows the step as it runs and
 * stops once it settles.
 */
public class StepOutputSources {

	private final RunStateStore store;
	private final Map<Path, FileTerminalSource> sources = new ConcurrentHashMap<>();

	/**
	 * Creates the registry over the store the engine persists runs to.
	 *
	 * @param store The run state store.
	 */
	public StepOutputSources(RunStateStore store) {
		this.store = store;
	}

	/**
	 * Finds the terminal source for one step's log.
	 *
	 * @param version The run version.
	 * @param stepId The step id.
	 * @return The source, or empty when there is no such run or step, or the step has not written a log yet.
	 */
	public Optional<TerminalSource> find(String version, String stepId) {
		var step = store.load(version).map(rs -> rs.step(stepId)).orElse(null);
		if (step == null || step.logRef == null)
			return Optional.empty();
		var root = store.stateDir().toAbsolutePath().normalize();
		var file = root.resolve(step.logRef).normalize();
		if (! file.startsWith(root))
			return Optional.empty();
		return Optional.of(sources.computeIfAbsent(file, f -> FileTerminalSource.create(f).terminal(() -> settled(version, stepId)).build()));
	}

	/**
	 * Whether a step's output is complete.
	 *
	 * <p>
	 * Only a step that has settled (succeeded, failed or skipped) or is parked waiting for a person is complete, so the
	 * terminal stops polling then. A step that has not started yet stays incomplete so its output appears when it runs.
	 * A step that no longer exists is complete.
	 *
	 * @param version The run version.
	 * @param stepId The step id.
	 * @return {@code true} if no more output will be written.
	 */
	boolean settled(String version, String stepId) {
		var step = store.load(version).map(rs -> rs.step(stepId)).orElse(null);
		if (step == null)
			return true;
		return switch (step.status) {
			case PENDING, RUNNING -> false;
			case SUCCEEDED, FAILED, SKIPPED, AWAITING_VOTE, AWAITING_REVIEW -> true;
		};
	}
}
