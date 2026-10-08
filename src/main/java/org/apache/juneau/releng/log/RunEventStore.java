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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import org.apache.juneau.marshall.marshaller.Json;
import org.apache.juneau.rest.server.views.FileRunViewSource;
import org.apache.juneau.rest.server.views.RunEvent;
import org.apache.juneau.rest.server.views.RunEvent.DoneStatus;
import org.apache.juneau.rest.server.views.RunEvent.EndStatus;
import org.apache.juneau.rest.server.views.RunEvent.StepState;
import org.apache.juneau.rest.server.views.RunViewSource;

/**
 * Persists each release run's run-view events as one JSON Lines file, {@code logs/<version>-events.jsonl} under the state
 * directory, and serves that file back to the run-view region.
 *
 * <p>
 * The file is the only state: nothing is cached in memory, so a restart, or a second instance over the same directory,
 * sees the same history. A run spans several candidates and many manual step invocations, so every invocation of a step
 * becomes its own step in the stream. The first is {@code <stepId>}; a re-run or resume after the step ended is
 * {@code <stepId>.2}, {@code <stepId>.3} and so on, shown as "(attempt N)". A step of the run-view model cannot reopen
 * once it has ended, and keeping the earlier attempt visible is more honest than rewriting it.
 *
 * <p>
 * A failure to write an event is logged and dropped; the run-view is a report on the release and must never stop it.
 */
public class RunEventStore {

	private static final System.Logger LOG = System.getLogger(RunEventStore.class.getName());

	// One lock per file across all instances, so two stores over one state directory cannot interleave a scan and an append.
	private static final Map<Path,Object> LOCKS = new ConcurrentHashMap<>();

	private final Path logsDir;
	private final Map<Path,FileRunViewSource> sources = new ConcurrentHashMap<>();

	/**
	 * Creates a store under {@code stateDir}.
	 *
	 * @param stateDir The state directory; the events live in its {@code logs} folder beside the step logs.
	 */
	public RunEventStore(Path stateDir) {
		this.logsDir = stateDir.resolve("logs");
	}

	/**
	 * The run-view id of a run: its version with dots written as underscores, since a run id has no dots.
	 *
	 * @param version The run version.
	 * @return The run id.
	 */
	public static String runId(String version) {
		return version.replace('.', '_');
	}

	/**
	 * Starts a new attempt of a step.
	 *
	 * @param version The run version.
	 * @param stepId The registry step id.
	 * @param title The step title.
	 * @param ordinal The step's 1-based position in the pipeline.
	 * @return The id of the attempt, which is what {@link #note} acts on. An earlier attempt of the step that is still open
	 * (a gate nobody resolved) is ended as skipped.
	 */
	public String begin(String version, String stepId, String title, int ordinal) {
		var file = file(version);
		synchronized (lock(file)) {
			var attempts = attempts(file, stepId);
			var open = openIds(file);
			for (var previous : attempts)
				if (open.contains(previous))
					append(file, RunEvent.end(previous, EndStatus.SKIP)); // superseded by this attempt
			var attempt = attempts.isEmpty() ? 1 : attempts.size() + 1;
			var id = attempt == 1 ? stepId : stepId + "." + attempt;
			append(file, RunEvent.step(id, attempt == 1 ? title : title + " (attempt " + attempt + ")").withN(ordinal));
			return id;
		}
	}

	/**
	 * Marks the latest attempt of a step as blocked on a person.
	 *
	 * @param version The run version.
	 * @param stepId The registry step id.
	 * @param title The step title.
	 */
	public void waiting(String version, String stepId, String title) {
		var file = file(version);
		synchronized (lock(file)) {
			var attempts = attempts(file, stepId);
			if (! attempts.isEmpty())
				append(file, RunEvent.step(last(attempts), title).withState(StepState.WAITING));
		}
	}

	/**
	 * Ends the latest attempt of a step; does nothing when the step never ran.
	 *
	 * @param version The run version.
	 * @param stepId The registry step id.
	 * @param status How it ended.
	 * @param ms The duration in milliseconds, or a negative number when unknown.
	 */
	public void end(String version, String stepId, EndStatus status, long ms) {
		var file = file(version);
		synchronized (lock(file)) {
			var attempts = attempts(file, stepId);
			if (! attempts.isEmpty()) {
				var e = RunEvent.end(last(attempts), status);
				append(file, ms >= 0 ? e.withMs(ms) : e);
			}
		}
	}

	/**
	 * Ends every attempt that is still open, for example when a candidate is dropped.
	 *
	 * @param version The run version.
	 * @param status How they ended.
	 */
	public void endOpen(String version, EndStatus status) {
		var file = file(version);
		synchronized (lock(file)) {
			for (var open : openIds(file))
				append(file, RunEvent.end(open, status));
		}
	}

	/**
	 * Adds a note.
	 *
	 * @param version The run version.
	 * @param level The note level.
	 * @param text The text, clipped to the event limit.
	 * @param href An optional link, or null.
	 * @param attemptId The attempt the note belongs under, or null for the run.
	 */
	public void note(String version, RunEvent.Level level, String text, String href, String attemptId) {
		if (text == null || text.isBlank())
			return;
		var e = RunEvent.note(level, text.length() > 1000 ? text.substring(0, 999) + "…" : text);
		if (href != null && ! href.isBlank())
			e = e.withHref(href);
		if (attemptId != null)
			e = e.withStep(attemptId);
		var file = file(version);
		synchronized (lock(file)) {
			append(file, e);
		}
	}

	/**
	 * Appends events as they are, for example one step's test results.
	 *
	 * @param version The run version.
	 * @param events The events.
	 */
	public void appendAll(String version, Collection<RunEvent> events) {
		var file = file(version);
		synchronized (lock(file)) {
			for (var e : events)
				append(file, e);
		}
	}

	/**
	 * Marks the run finished.
	 *
	 * @param version The run version.
	 * @param status The final status.
	 */
	public void done(String version, DoneStatus status) {
		var file = file(version);
		synchronized (lock(file)) {
			append(file, RunEvent.done(status));
		}
	}

	/**
	 * The source of a run's events.
	 *
	 * @param version The run version.
	 * @param terminal True once the run will produce no more events.
	 * @return The source, which reads an empty stream until the first event is written.
	 */
	public RunViewSource source(String version, BooleanSupplier terminal) {
		var file = file(version);
		return sources.computeIfAbsent(file, f -> FileRunViewSource.create(f).terminal(terminal).build());
	}

	private Path file(String version) {
		return logsDir.resolve(version + "-events.jsonl");
	}

	private static Object lock(Path file) {
		return LOCKS.computeIfAbsent(file, f -> new Object());
	}

	private static String last(List<String> attempts) {
		return attempts.get(attempts.size() - 1);
	}

	private static void append(Path file, RunEvent event) {
		try {
			Files.createDirectories(file.getParent());
			Files.writeString(file, Json.DEFAULT.write(event.validate().toContractMap()) + "\n", StandardCharsets.UTF_8,
				StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException | RuntimeException e) {
			LOG.log(Level.WARNING, "Dropped a run-view event for " + file.getFileName() + ": " + e.getMessage());
		}
	}

	/**
	 * The ids of the attempts of {@code stepId}, oldest first.
	 */
	private static List<String> attempts(Path file, String stepId) {
		List<String> out = new ArrayList<>();
		for (var id : scan(file).keySet())
			if (id.equals(stepId) || (id.startsWith(stepId + ".") && id.substring(stepId.length() + 1).chars().allMatch(Character::isDigit)))
				out.add(id);
		return out;
	}

	private static List<String> openIds(Path file) {
		List<String> out = new ArrayList<>();
		scan(file).forEach((id, open) -> {
			if (open)
				out.add(id);
		});
		return out;
	}

	/**
	 * Every step id in the file in first-appearance order, mapped to whether it is still open.
	 */
	private static Map<String,Boolean> scan(Path file) {
		Map<String,Boolean> out = new LinkedHashMap<>();
		if (! Files.isRegularFile(file))
			return out;
		try {
			for (var line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
				if (! (line.contains("\"ev\":\"step\"") || line.contains("\"ev\":\"end\"")))
					continue;
				Map<?,?> m = Json.DEFAULT.read(line, Map.class);
				var ev = m.get("ev");
				var id = (String)m.get("id");
				if ("step".equals(ev))
					out.putIfAbsent(id, true);
				else if ("end".equals(ev))
					out.computeIfPresent(id, (k, v) -> false);
			}
		} catch (IOException | RuntimeException e) {
			LOG.log(Level.WARNING, "Cannot scan " + file.getFileName() + ": " + e.getMessage());
		}
		return out;
	}
}
