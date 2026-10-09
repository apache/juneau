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
package org.apache.juneau.rest.server.terminal;

import static java.nio.charset.StandardCharsets.*;
import static java.nio.file.StandardOpenOption.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;

import org.apache.juneau.marshall.json.JsonSerializer;
import org.apache.juneau.marshall.marshaller.Json;
import org.apache.juneau.rest.server.runreport.*;
import org.apache.juneau.rest.server.views.*;

/**
 * Runs one tool under a pseudo-terminal through {@code juneau_run.py --pty} and exposes its output as a
 * {@link FileTerminalSource} and its steps as a {@link FileRunViewSource}.
 *
 * <p>
 * The command line is
 * {@code python3 <script> --console none --pty --size <cols>x<rows> --full-log <log> --events <events> <tool> -- <cmd...>},
 * with the log ({@value #LOG_NAME}), its {@code .size} sidecar and the events file ({@value #EVENTS_NAME}) in the
 * given run directory.  The script is found through the system property {@value #SCRIPT_PROPERTY}, then the
 * environment variable {@value #SCRIPT_ENV}.
 *
 * <p>
 * The run directory is single-use.  It is also the tool's working directory, and the runner's own stdout and stderr
 * go to {@code runner.out} there, in append mode and uncapped; that file is diagnostic only.
 *
 * <p>
 * Both sources share one done flag, set once the process has exited.  If the runner exits without writing a
 * {@code done} event, a warning {@code note} and {@code done {status:"fail"}} are appended first.  Only the last
 * {@value #TAIL_BYTES} bytes of the events file are scanned for that {@code done} event, because the protocol puts it
 * last.
 *
 * <p>
 * The exit hook runs on one daemon thread per process, named {@code juneau-terminal-exit-<pid>}.  Its executor is
 * shut down once the hook has run, and {@link #close()} waits for that.
 *
 * <p>
 * POSIX only: there is no Windows pseudo-terminal support.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	TerminalProcess <jv>p</jv> = TerminalProcess.<jsm>start</jsm>(<js>"maven"</js>, List.<jsm>of</jsm>(<js>"mvn"</js>, <js>"test"</js>), 120, 40, <jv>runDir</jv>);
 * 	<jv>terminals</jv>.put(<jv>id</jv>, <jv>p</jv>.terminal());
 * 	<jv>runViews</jv>.put(<jv>id</jv>, <jv>p</jv>.events());
 * </p>
 */
public final class TerminalProcess implements AutoCloseable {

	/** The system property naming {@code juneau_run.py}. */
	public static final String SCRIPT_PROPERTY = "juneau.run.script";

	/** The environment variable naming {@code juneau_run.py}, read when the property is not set. */
	public static final String SCRIPT_ENV = "JUNEAU_RUN_SCRIPT";

	/** The byte log's file name in the run directory. */
	public static final String LOG_NAME = "terminal.log";

	/** The events file's name in the run directory. */
	public static final String EVENTS_NAME = "terminal.events.jsonl";

	/** How much of the end of the events file is scanned for a {@code done} event. */
	static final int TAIL_BYTES = 64 * 1024;

	/** How long {@link #close()} waits for the exit hook. */
	static final int CLOSE_WAIT_SECONDS = 5;

	private static final Set<String> TOOLS = Set.of("maven", "pytest", "playwright", "generic");
	private static final Logger LOG = Logger.getLogger(TerminalProcess.class.getName());

	/** What the launcher reads from its environment; replaced in tests. */
	record Host(String osName, String scriptProperty, String scriptEnv, String path) {
		static Host system() {
			return new Host(System.getProperty("os.name", ""), System.getProperty(SCRIPT_PROPERTY), System.getenv(SCRIPT_ENV), System.getenv("PATH"));
		}
	}

	private final Process process;
	private final Path log, events;
	private final AtomicBoolean done = new AtomicBoolean();
	private final FileTerminalSource terminal;
	private final FileRunViewSource runView;
	private final ExecutorService executor;
	private final CompletableFuture<Integer> exit;
	private volatile Thread exitThread;

	/** Appends the missing done event; replaced in tests. */
	@FunctionalInterface
	interface DoneWriter {
		void ensureDone(Path events, int exitCode) throws IOException;
	}

	private TerminalProcess(Process process, Path log, Path events, DoneWriter doneWriter) {
		this.process = process;
		this.log = log;
		this.events = events;
		terminal = FileTerminalSource.create(log).terminal(done::get).build();
		runView = FileRunViewSource.create(events).terminal(done::get).build();
		executor = Executors.newSingleThreadExecutor(r -> {
			var t = new Thread(r, "juneau-terminal-exit-" + process.pid());
			t.setDaemon(true);
			return t;
		});
		exit = process.onExit().thenApplyAsync(p -> {
			exitThread = Thread.currentThread();
			try {
				doneWriter.ensureDone(events, p.exitValue());
			} catch (Exception e) {
				LOG.log(Level.WARNING, e, () -> "Could not append the done event to '" + events + "'.");
			} finally {
				done.set(true);
				executor.shutdown();
			}
			return p.exitValue();
		}, executor);
	}

	/**
	 * Starts a tool under a pseudo-terminal.
	 *
	 * @param tool The juneau_run tool parser: {@code maven}, {@code pytest}, {@code playwright} or {@code generic}.
	 * @param cmd The command and its arguments; at least one element.
	 * 	It is a token list: each element reaches the tool as one argument, through {@link ProcessBuilder}.  It is never
	 * 	joined into a string or given to a shell, so shell metacharacters in an element are literal.
	 * @param cols The terminal width, 1-9999.
	 * @param rows The terminal height, 1-9999.
	 * @param dir The run directory; created if missing.  Each run needs its own: the log and events files must be
	 * 	missing or empty.  It is also the tool's working directory, and the runner's own stdout and stderr go to
	 * 	{@code runner.out} in it, in append mode and uncapped; that file is diagnostic only.
	 * @return The running process.
	 * @throws IOException If the directory could not be prepared or the process could not start.
	 * @throws IllegalArgumentException If an argument is out of range.
	 * @throws IllegalStateException If the script is not configured, {@code python3} is not on the {@code PATH}, or
	 * 	the run directory already holds a run.
	 * @throws UnsupportedOperationException On Windows.
	 */
	public static TerminalProcess start(String tool, List<String> cmd, int cols, int rows, Path dir) throws IOException {
		return start(tool, cmd, cols, rows, dir, Host.system());
	}

	static TerminalProcess start(String tool, List<String> cmd, int cols, int rows, Path dir, Host host) throws IOException {
		return start(tool, cmd, cols, rows, dir, host, TerminalProcess::ensureDone);
	}

	static TerminalProcess start(String tool, List<String> cmd, int cols, int rows, Path dir, Host host, DoneWriter doneWriter) throws IOException {
		if (host.osName().toLowerCase(Locale.ROOT).startsWith("windows"))
			throw new UnsupportedOperationException("TerminalProcess needs a POSIX pseudo-terminal; Windows is not supported.");
		if (! TOOLS.contains(tool))
			throw iaex("TerminalProcess tool must be one of %s; got '%s'.", new TreeSet<>(TOOLS), tool);
		if (cmd == null || cmd.isEmpty())
			throw iaex("TerminalProcess cmd must have at least one element.");
		if (cols < 1 || cols > 9999 || rows < 1 || rows > 9999)
			throw iaex("TerminalProcess size must be 1-9999 x 1-9999; got '%sx%s'.", cols, rows);
		var script = host.scriptProperty() != null && ! host.scriptProperty().isBlank() ? host.scriptProperty() : host.scriptEnv();
		if (script == null || script.isBlank())
			throw new IllegalStateException("TerminalProcess cannot find juneau_run.py: set the system property " + SCRIPT_PROPERTY + " or the environment variable " + SCRIPT_ENV + ".");
		var python = findOnPath("python3", host.path());
		if (python == null)
			throw new IllegalStateException("TerminalProcess cannot find python3 on the PATH.");
		Files.createDirectories(dir);
		var log = dir.resolve(LOG_NAME);
		var events = dir.resolve(EVENTS_NAME);
		for (var f : List.of(log, events))
			if (Files.exists(f) && Files.size(f) > 0)
				throw new IllegalStateException("TerminalProcess run directory '" + dir + "' already holds a run: '" + f.getFileName() + "' is not empty.");
		// Written here as well as by juneau_run, so a request that arrives before the child starts sees the real size.
		Files.writeString(log.resolveSibling(LOG_NAME + ".size"), "{\"cols\":" + cols + ",\"rows\":" + rows + "}");
		var argv = new ArrayList<String>();
		argv.addAll(List.of(python.toString(), script, "--console", "none", "--pty", "--size", cols + "x" + rows,
			"--full-log", log.toString(), "--events", events.toString(), tool, "--"));
		argv.addAll(cmd);
		var pb = new ProcessBuilder(argv).directory(dir.toFile()).redirectErrorStream(true)
			.redirectOutput(ProcessBuilder.Redirect.appendTo(dir.resolve("runner.out").toFile()));
		return new TerminalProcess(pb.start(), log, events, doneWriter);
	}

	/** Returns the first executable {@code name} in the {@code PATH} directories, or <jk>null</jk>. */
	static Path findOnPath(String name, String path) {
		if (path == null)
			return null;
		for (var d : path.split(File.pathSeparator)) {
			if (d.isEmpty())
				continue;
			var p = Paths.get(d, name);
			if (Files.isRegularFile(p) && Files.isExecutable(p))
				return p;
		}
		return null;
	}

	/** Appends a warning note and a failing done event when the last {@value #TAIL_BYTES} bytes of the events file hold no done event. */
	static void ensureDone(Path events, int exitCode) throws IOException {
		var text = "";
		if (Files.exists(events)) {
			try (var ch = FileChannel.open(events, StandardOpenOption.READ)) {
				var from = Math.max(0, ch.size() - TAIL_BYTES);
				var buf = ByteBuffer.allocate((int)(ch.size() - from));
				while (buf.hasRemaining() && ch.read(buf, from + buf.position()) >= 0) {}
				text = new String(buf.array(), 0, buf.position(), UTF_8);
			}
		}
		// A fragment at the start of the tail never parses, so it is never taken for a done event.
		for (var line : text.split("\n")) {
			if (isDone(line))
				return;
		}
		var sb = new StringBuilder();
		if (! text.isEmpty() && ! text.endsWith("\n"))
			sb.append('\n');
		sb.append(JsonSerializer.DEFAULT.toString(RunEvent.note(RunEvent.Level.WARN, "runner exited without done, exit " + exitCode).toContractMap())).append('\n');
		sb.append(JsonSerializer.DEFAULT.toString(RunEvent.done(RunEvent.DoneStatus.FAIL).toContractMap())).append('\n');
		Files.writeString(events, sb, UTF_8, CREATE, APPEND);
	}

	private static boolean isDone(String line) {
		if (line.isBlank())
			return false;
		try {
			return RunEvent.fromMap(Json.to(line, Map.class)).kind() == RunEvent.Kind.DONE;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * The raw byte source over the log.
	 *
	 * @return The source.
	 */
	public FileTerminalSource terminal() {
		return terminal;
	}

	/**
	 * The step events source over the events file.
	 *
	 * @return The source.
	 */
	public FileRunViewSource events() {
		return runView;
	}

	/**
	 * The byte log.
	 *
	 * @return The log path.
	 */
	public Path log() {
		return log;
	}

	/**
	 * The events file.
	 *
	 * @return The events path.
	 */
	public Path eventsFile() {
		return events;
	}

	/**
	 * Whether the process has exited and the events file ends with a {@code done} event.
	 *
	 * @return <jk>true</jk> once finished.
	 */
	public boolean isDone() {
		return done.get();
	}

	/**
	 * Waits for the process to exit and for the done event to be in place.
	 *
	 * @param timeout The longest wait.
	 * @param unit The unit of {@code timeout}.
	 * @return The runner's exit code.
	 * @throws InterruptedException If interrupted.
	 * @throws TimeoutException If the process is still running.
	 */
	public int waitFor(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
		try {
			return exit.get(timeout, unit);
		} catch (ExecutionException e) {
			throw new IllegalStateException(e.getCause());
		}
	}

	/** The exit hook's executor; for tests. */
	ExecutorService executor() {
		return executor;
	}

	/** The thread the exit hook ran on, or <jk>null</jk> before it has run; for tests. */
	Thread exitThread() {
		return exitThread;
	}

	/**
	 * Kills the runner and its tool if they are still running, then waits up to {@value #CLOSE_WAIT_SECONDS} seconds for
	 * the exit hook.
	 *
	 * <p>
	 * The hook writes the done event and shuts its executor down, so once this returns normally both have happened.  The
	 * executor is not shut down here: a hook submitted after that would be refused, and the done flag would never be set.
	 */
	@Override /* AutoCloseable */
	public void close() {
		process.descendants().forEach(ProcessHandle::destroyForcibly);
		process.destroyForcibly();
		try {
			exit.get(CLOSE_WAIT_SECONDS, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (ExecutionException | TimeoutException e) {
			LOG.log(Level.WARNING, e, () -> "TerminalProcess exit hook did not finish for '" + events + "'.");
		}
	}
}
