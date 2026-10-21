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
package org.apache.juneau.releng.run;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.LongPredicate;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.apache.juneau.marshall.collections.JsonMap;
import org.apache.juneau.marshall.marshaller.Json;

/**
 * Reads {@code <rm.home>/runs/*}{@code /run.json} and the per-attempt {@code jrm-attempt.json} sidecars. Never writes.
 */
public class RunFiles {

	/** Written into a live attempt's dir by JRM's shutdown hook. */
	public static final String STOPPED_MARKER = "jrm-stopped";

	/** JRM's own record of a UI attempt (covers dry runs, which never write {@code run.json}). */
	public static final String SIDECAR = "jrm-attempt.json";

	private static final Logger LOG = Logger.getLogger(RunFiles.class.getName());

	private final Path home;
	private final LongPredicate pidAlive;
	private final Predicate<Path> liveDir;

	/**
	 * Constructor.
	 *
	 * @param home {@code rm.home}.
	 * @param liveDir Whether JRM's own live process runs in that attempt dir.
	 */
	public RunFiles(Path home, Predicate<Path> liveDir) {
		this(home, pid -> ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false), liveDir);
	}

	RunFiles(Path home, LongPredicate pidAlive, Predicate<Path> liveDir) {
		this.home = home;
		this.pidAlive = pidAlive;
		this.liveDir = liveDir;
	}

	/**
	 * Returns {@code rm.home}.
	 *
	 * @return {@code rm.home}.
	 */
	public Path home() {
		return home;
	}

	/**
	 * Returns every readable run file, newest {@code run.json} first. A corrupt file is skipped with a warning.
	 *
	 * @return The run files.
	 */
	public List<RunFile> all() {
		var runs = home.resolve("runs");
		if (! Files.isDirectory(runs))
			return List.of();
		try (Stream<Path> s = Files.list(runs)) {
			return s.map(d -> d.resolve("run.json")).filter(Files::isRegularFile)
				.sorted(Comparator.comparing(RunFiles::mtime).reversed())
				.map(this::read).flatMap(Optional::stream).toList();
		} catch (IOException e) {
			LOG.warning(() -> "cannot list " + runs + ": " + e.getMessage());
			return List.of();
		}
	}

	/**
	 * Returns the active run: the newest with a version whose phase is not {@code released}.
	 *
	 * @return The active run, if any.
	 */
	public Optional<RunFile> active() {
		return all().stream().filter(r -> r.version != null && ! "released".equals(r.phase)).findFirst();
	}

	/**
	 * The Release page's History: the active run's attempts merged by dir with every sidecar, newest first.
	 *
	 * @return The attempts, newest first.
	 */
	public List<Attempt> history() {
		Map<String, Attempt> byDir = new LinkedHashMap<>();
		active().ifPresent(r -> r.attempts.stream().filter(a -> a.dir != null).forEach(a -> byDir.put(a.dir, a)));
		for (var dir : attemptDirs()) {
			var side = dir.resolve(SIDECAR);
			var parsed = Files.isRegularFile(side) ? parse(side) : Optional.<JsonMap>empty();
			if (parsed.isEmpty())
				continue;
			var m = parsed.get();
			var a = byDir.computeIfAbsent(dir.toString(), k -> sidecarOnly(dir, m));
			a.dryRun = b(m.get("dryRun"));
			if (a.exit == null && m.get("exit") instanceof Number x)
				a.exit = x.intValue();
		}
		var out = new ArrayList<>(byDir.values());
		out.forEach(this::derive);
		out.sort(Comparator.comparing((Attempt a) -> a.dir).reversed());
		return out;
	}

	private static Attempt sidecarOnly(Path dir, JsonMap m) {
		var a = new Attempt();
		a.dir = dir.toString();
		a.command = m.getString("command");
		a.origin = m.getString("origin", "ui");
		a.started = m.getString("startedAt");
		a.ended = m.getString("endedAt");
		var exit = m.get("exit") instanceof Number x ? Integer.valueOf(x.intValue()) : null;
		if (exit == null)
			a.status = "running";
		else
			a.status = switch (exit) {
				case 0 -> "ok";
				case 130 -> "cancelled";
				default -> "failed";
			};
		return a;
	}

	private void derive(Attempt a) {
		var dir = Path.of(a.dir);
		a.id = "attempt-" + dir.getFileName();
		a.label = a.n > 0 ? "#" + a.n : dir.getFileName().toString();
		a.hasTerminalLog = Files.isRegularFile(dir.resolve("terminal.log"));
		var stopped = Files.exists(dir.resolve(STOPPED_MARKER));
		var running = "running".equals(a.status);
		if (stopped && (running || "cancelled".equals(a.status)))
			a.outcome = "interrupted (JRM stopped)";
		else if (running && ! (a.pid != null ? pidAlive.test(a.pid) : liveDir.test(dir)))
			a.outcome = "interrupted";
		else
			a.outcome = a.status;
	}

	private List<Path> attemptDirs() {
		var base = home.resolve("attempts");
		if (! Files.isDirectory(base))
			return List.of();
		try (Stream<Path> s = Files.list(base)) {
			return s.filter(Files::isDirectory).toList();
		} catch (IOException e) {
			LOG.warning(() -> "cannot list " + base + ": " + e.getMessage());
			return List.of();
		}
	}

	private Optional<RunFile> read(Path file) {
		var parsed = parse(file);
		if (parsed.isEmpty())
			return Optional.empty();
		var m = parsed.get();
		try {
			var r = new RunFile();
			r.path = file;
			var d = m.getMap("details");
			if (d != null) {
				r.version = d.getString("version");
				r.rc = d.get("rc") instanceof Number n ? n.intValue() : 0;
			}
			r.phase = m.getString("phase");
			r.last = m.getString("last");
			r.voteDeadline = m.getString("voteDeadline");
			var list = m.getList("attempts");
			if (list != null)
				for (var o : list)
					if (o instanceof Map<?, ?> am)
						r.attempts.add(attempt(am));
			return Optional.of(r);
		} catch (RuntimeException e) {
			LOG.warning(() -> "skipping " + file + ": unexpected structure (" + e.getClass().getName() + ")");
			return Optional.empty();
		}
	}

	private static Attempt attempt(Map<?, ?> m) {
		var a = new Attempt();
		a.n = m.get("n") instanceof Number n ? n.intValue() : 0;
		a.dir = m.get("dir") == null ? null : String.valueOf(m.get("dir"));
		a.command = m.get("command") instanceof String s ? s : null;
		a.origin = m.get("origin") instanceof String s ? s : "cli";
		a.status = m.get("status") instanceof String s ? s : null;
		a.step = m.get("step") instanceof String s ? s : null;
		a.started = m.get("started") instanceof String s ? s : null;
		a.ended = m.get("ended") instanceof String s ? s : null;
		a.pid = m.get("pid") instanceof Number p ? Long.valueOf(p.longValue()) : null;
		a.exit = m.get("exit") instanceof Number x ? Integer.valueOf(x.intValue()) : null;
		return a;
	}

	private static Optional<JsonMap> parse(Path file) {
		try {
			return Optional.ofNullable(Json.to(Files.readString(file), JsonMap.class));
		} catch (IOException | RuntimeException e) {
			// A release never resumes from a guess: a corrupt file is shown as absent and logged, never repaired.
			LOG.warning(() -> "cannot read " + file + ": " + e.getMessage());
			return Optional.empty();
		}
	}

	private static long mtime(Path p) {
		try {
			return Files.getLastModifiedTime(p).toMillis();
		} catch (IOException e) {
			return 0L;
		}
	}
}
