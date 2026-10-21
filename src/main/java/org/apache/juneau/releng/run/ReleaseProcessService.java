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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.juneau.marshall.marshaller.Json;
import org.apache.juneau.releng.log.EventBroadcaster;
import org.apache.juneau.rest.server.terminal.TerminalProcess;

/**
 * Runs {@code scripts/release.py} under a pty, one process at a time.
 *
 * <p>Each UI attempt gets a fresh {@code <home>/attempts/<stamp>-<NNN>} directory, which is the
 * {@link TerminalProcess} dir, the script's {@code --attempt-dir}, and the home of JRM's {@code jrm-attempt.json}
 * sidecar.
 */
public class ReleaseProcessService {

	private static final Logger LOG = Logger.getLogger(ReleaseProcessService.class.getName());

	/**
	 * The live attempt.
	 *
	 * @param attemptId The terminal id, {@code attempt-<dir name>}.
	 * @param dir The attempt directory.
	 * @param command {@code start}, {@code resume} or {@code revert}.
	 * @param dryRun Whether {@code --dry-run} was passed.
	 * @param process The process.
	 * @param broker Its prompt broker.
	 */
	public record Live(String attemptId, Path dir, String command, boolean dryRun, TerminalProcess process, PromptBroker broker) {}

	/** Thrown when a start, resume or drop is requested while a release process is already running (HTTP 409). */
	public static class BusyException extends IllegalStateException {

		private static final long serialVersionUID = 1L;

		/**
		 * Constructor.
		 *
		 * @param attemptId The running attempt.
		 */
		public BusyException(String attemptId) {
			super("A release process is already running (" + attemptId + ")");
		}
	}

	private final Path home;
	private final Path scriptsDir;
	private final PromptBroker.Stored stored;
	private final EventBroadcaster events;
	private Live live;

	/**
	 * Constructor.
	 *
	 * @param home {@code rm.home}.
	 * @param scriptsDir {@code rm.scripts.dir}, holding {@code release.py}.
	 * @param stored Stored credential answers.
	 * @param events The page's event bus.
	 */
	public ReleaseProcessService(Path home, Path scriptsDir, PromptBroker.Stored stored, EventBroadcaster events) {
		this.home = home.toAbsolutePath();
		this.scriptsDir = scriptsDir.toAbsolutePath();
		this.stored = stored;
		this.events = events;
	}

	/**
	 * Starts a new release run.
	 *
	 * @param dryRun {@code --dry-run}.
	 * @return The live attempt.
	 * @throws BusyException If a process is already running.
	 */
	public synchronized Live start(boolean dryRun) {
		return launch("start", List.of(), dryRun, Map.of());
	}

	/**
	 * Resumes the active run.
	 *
	 * @param dryRun {@code --dry-run}.
	 * @return The live attempt.
	 * @throws BusyException If a process is already running.
	 */
	public synchronized Live resume(boolean dryRun) {
		return launch("resume", List.of("--resume"), dryRun, Map.of());
	}

	/**
	 * Drops the active run's RC; the typed {@code RC<n>} answers the script's own {@code confirm.revert}.
	 *
	 * @param typedRc What the user typed, already checked by the caller.
	 * @param dryRun {@code --dry-run}.
	 * @return The live attempt.
	 * @throws BusyException If a process is already running.
	 */
	public synchronized Live dropRc(String typedRc, boolean dryRun) {
		return launch("revert", List.of("--revert"), dryRun, Map.of("confirm.revert", typedRc));
	}

	/** Cancels the live process, if any: stdin closes, the runner SIGTERMs, release.py records {@code cancelled}. */
	public void cancel() {
		Live l;
		synchronized (this) {
			l = live;
		}
		if (l != null && ! l.process().isDone())
			l.process().close();
	}

	/**
	 * Returns the live attempt.
	 *
	 * @return The live attempt, if a process is running.
	 */
	public synchronized Optional<Live> live() {
		return Optional.ofNullable(live);
	}

	/**
	 * Tells whether JRM's live process runs in a directory.
	 *
	 * @param dir An attempt directory.
	 * @return Whether JRM's live process runs in it.
	 */
	public synchronized boolean isLiveDir(Path dir) {
		return live != null && live.dir().equals(dir);
	}

	/** Spring destroy method (run from the JVM shutdown hook): marks the live attempt {@code jrm-stopped}, then closes. */
	public void shutdown() {
		Live l;
		synchronized (this) {
			l = live;
		}
		if (l == null || l.process().isDone())
			return;
		try {
			Files.writeString(l.dir().resolve(RunFiles.STOPPED_MARKER), LocalDateTime.now().format(AttemptDirs.STAMP));
		} catch (IOException e) {
			LOG.log(Level.WARNING, "cannot write the jrm-stopped marker", e);
		}
		l.process().close();
	}

	private Live launch(String command, List<String> extra, boolean dryRun, Map<String, String> autoAnswers) {
		if (live != null)
			throw new BusyException(live.attemptId());
		try {
			var dir = AttemptDirs.create(home);
			var cmd = new ArrayList<>(List.of("python3", scriptsDir.resolve("release.py").toString(), "--console", "condensed",
				"--home", home.toString(), "--attempt-dir", dir.toString()));
			if (dryRun)
				cmd.add("--dry-run");
			cmd.addAll(extra);
			var sidecar = new LinkedHashMap<String, Object>();
			sidecar.put("command", command);
			sidecar.put("origin", "ui");
			sidecar.put("dryRun", dryRun);
			sidecar.put("startedAt", LocalDateTime.now().format(AttemptDirs.STAMP));
			writeSidecar(dir, sidecar);
			var broker = new PromptBroker(stored, events, autoAnswers);
			var attemptId = "attempt-" + dir.getFileName();
			// Published before the process starts so the page sees "started" before any prompt the process raises.
			events.publish("process", Map.of("state", "started", "attempt", attemptId));
			TerminalProcess proc;
			try {
				proc = TerminalProcess.start("generic", cmd, 120, 40, dir, broker);
			} catch (RuntimeException e) {
				// Nothing ran: drop the sidecar (and the dir, if empty) so History shows no phantom attempt.
				try {
					Files.deleteIfExists(dir.resolve(RunFiles.SIDECAR));
					Files.deleteIfExists(dir.resolve(RunFiles.SIDECAR + ".tmp"));
					Files.deleteIfExists(dir);
				} catch (@SuppressWarnings("unused") IOException | RuntimeException cleanup) {
					// non-empty dir or unreadable: leave it
				}
				var failed = new LinkedHashMap<String, Object>();
				failed.put("state", "ended");
				failed.put("attempt", attemptId);
				failed.put("exit", null);
				events.publish("process", failed);
				throw e;
			}
			broker.attach(proc::answer);
			var l = new Live(attemptId, dir, command, dryRun, proc, broker);
			live = l;
			var waiter = new Thread(() -> awaitExit(l, sidecar), "release-py-" + dir.getFileName());
			waiter.setDaemon(true);
			waiter.start();
			return l;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void awaitExit(Live l, Map<String, Object> sidecar) {
		Integer exit = null;
		try {
			exit = l.process().waitFor(3650, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			LOG.log(Level.WARNING, "release.py wait failed", e);
		}
		sidecar.put("endedAt", LocalDateTime.now().format(AttemptDirs.STAMP));
		if (exit != null)
			sidecar.put("exit", exit);
		try {
			writeSidecar(l.dir(), sidecar);
		} catch (IOException e) {
			LOG.log(Level.WARNING, "cannot update " + RunFiles.SIDECAR, e);
		}
		l.process().close();
		synchronized (this) {
			if (live == l)
				live = null;
		}
		var ended = new LinkedHashMap<String, Object>();
		ended.put("state", "ended");
		ended.put("attempt", l.attemptId());
		ended.put("exit", exit);
		events.publish("process", ended);
	}

	private static void writeSidecar(Path dir, Map<String, Object> m) throws IOException {
		var tmp = dir.resolve(RunFiles.SIDECAR + ".tmp");
		Files.writeString(tmp, Json.of(m));
		Files.move(tmp, dir.resolve(RunFiles.SIDECAR), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
	}
}
