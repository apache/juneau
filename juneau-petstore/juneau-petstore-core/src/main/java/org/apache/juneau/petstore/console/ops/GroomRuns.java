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
package org.apache.juneau.petstore.console.ops;

import static java.nio.charset.StandardCharsets.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.terminal.*;

/**
 * The groom-pet demo runs behind the Jobs page's terminal cards.
 *
 * <p>
 * A run writes about 60 ANSI-styled lines to a {@link MemoryTerminalSource}, one step per tick: INFO and FINE lines
 * (FINE is dim), a WARNING, {@code check}/{@code cross} marks, a progress strip of coloured blocks, a multi-line
 * SEVERE entry, bold marker lines, and a canned ANSI transcript written as-is.  Each tick also appends one ANSI line to
 * a temp file, read through one {@link FileTerminalSource} per run.  A verbose run first writes {@value #VERBOSE_LINES}
 * FINE lines.
 *
 * <p>
 * Run ids are random 128-bit hex strings, so a terminal can be read only by someone who was given its id.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>try</jk> (<jk>var</jk> <jv>runs</jv> = <jk>new</jk> GroomRuns(650)) {
 * 		<jk>var</jk> <jv>run</jv> = <jv>runs</jv>.start(<js>"Rex"</js>, <jk>false</jk>, <jk>null</jk>);
 * 		<jk>var</jk> <jv>source</jv> = <jv>runs</jv>.source(<jv>run</jv>.id()).orElseThrow();
 * 	}
 * </p>
 */
@SuppressWarnings({
	"java:S1192", // The script's literals are demo text; constants would obscure it.
	"resource" // Each Run owns its file-backed source, closed by drop()/close(); seeded() hands the new GroomRuns to its caller.
})
public final class GroomRuns implements AutoCloseable {

	/** System property: milliseconds between steps. */
	public static final String PERIOD_PROPERTY = "petstore.groom.periodMs";

	/** The default step period; 53 steps take about 35 seconds. */
	public static final long DEFAULT_PERIOD_MS = 650;

	/** Suffix of the log id that selects a run's file-backed source. */
	public static final String FILE_SUFFIX = "-file";

	/** The in-memory terminal's ring capacity in bytes; it holds a verbose run whole. */
	static final int RING_BYTES = 1 << 20;

	/** The terminal size, in columns and rows, of both of a run's terminals. */
	static final int COLS = 100;

	/** The terminal height, in rows, of both of a run's terminals. */
	static final int ROWS = 24;

	private static final String RESET = "\u001b[0m";
	private static final String BOLD = "\u001b[1m";
	private static final String DIM = "\u001b[2m";
	private static final String RED = "\u001b[31m";
	private static final String GREEN = "\u001b[32m";
	private static final String YELLOW = "\u001b[33m";

	/** How many runs are kept; the oldest is dropped, and its file deleted, beyond this. */
	static final int MAX_RUNS = 20;

	/** FINE lines a verbose run writes up front. */
	static final int VERBOSE_LINES = 8000;

	/** Wash passes; the progress strip has one block per pass. */
	static final int WASH_PASSES = 10;

	/** The canned ANSI transcript: an OSC title (stripped), bold, colours, and a carriage-return counter. */
	static final List<String> TRANSCRIPT = List.of(
		"\u001b]0;groom-pet\u0007\u001b[1mgroomer 2.1\u001b[0m starting",
		"\u001b[34mfetch\u001b[0m shampoo \u001b[32mok\u001b[0m",
		"\u001b[34mfetch\u001b[0m towels \u001b[32mok\u001b[0m",
		"drying  10%\rdrying  40%\rdrying  70%\rdrying 100%",
		"\u001b[33mwarn\u001b[0m the clippers are dull",
		"\u001b[1;31merror\u001b[0m no bow in stock; using a ribbon",
		"\u001b[1;32mready\u001b[0m"
	);

	/** One step of the script. */
	@FunctionalInterface
	interface Step {
		void run(Run r) throws IOException;
	}

	/** The script, one step per tick. */
	static final List<Step> SCRIPT = script();

	/** One groom run. */
	public static final class Run {
		final String id;
		final String pet;
		final boolean verbose;
		final Instant submitted = Instant.now();
		final MemoryTerminalSource terminal = MemoryTerminalSource.create(RING_BYTES, COLS, ROWS);
		final Path file;
		final Path sizeFile;
		final AtomicBoolean fileDone = new AtomicBoolean();
		final FileTerminalSource fileSource;
		final AtomicInteger lines = new AtomicInteger();
		final AtomicInteger step = new AtomicInteger();
		volatile boolean done;
		final AtomicReference<ScheduledFuture<?>> task = new AtomicReference<>();
		volatile String key;

		Run(String id, String pet, boolean verbose, Path file) {
			this.id = id;
			this.pet = pet;
			this.verbose = verbose;
			this.file = file;
			sizeFile = file.resolveSibling(file.getFileName() + ".size");
			fileSource = FileTerminalSource.create(file).terminal(fileDone::get).build();
		}

		/** @return The run id, which is also its in-memory terminal id. */
		public String id() { return id; }

		/** @return The pet name. */
		public String pet() { return pet; }

		/** @return The in-memory terminal. */
		public MemoryTerminalSource terminal() { return terminal; }

		/** @return The cached file-backed source over this run's transcript. */
		public FileTerminalSource fileSource() { return fileSource; }

		/** @return The temp file the run appends to. */
		public Path file() { return file; }

		/** @return Whether the run has written its last line. */
		public boolean done() { return done; }

		/** @return The Groom runs table row. */
		public JsonMap row() {
			return JsonMap.of("id", id, "pet", pet, "submitted", submitted.toString(), "state", done ? "DONE" : "RUNNING", "lines", lines.get(), "verbose", verbose);
		}

		void line(String text) {
			var b = (text + "\r\n").getBytes(UTF_8);
			terminal.write(b, 0, b.length);
			lines.incrementAndGet();
		}

		void raw(String text) {
			for (var l : text.split("\n"))
				line(l);
		}
	}

	private final long periodMs;
	private final Map<String,Run> runs = new ConcurrentHashMap<>();
	private final Map<String,String> keys = new ConcurrentHashMap<>();
	private final Deque<Run> order = new ArrayDeque<>(); // Newest first; guarded by itself.
	private final ScheduledExecutorService ticker = Executors.newSingleThreadScheduledExecutor(r -> {
		var t = new Thread(r, "petstore-groom");
		t.setDaemon(true);
		return t;
	});

	/**
	 * Constructor.
	 *
	 * @param periodMs Milliseconds between steps, at least 1.
	 */
	public GroomRuns(long periodMs) {
		if (periodMs < 1)
			throw new IllegalArgumentException(f("periodMs must be at least 1; got %s", periodMs));
		this.periodMs = periodMs;
	}

	/**
	 * Creates the runs and starts one verbose run, so the Jobs page always has a log to show.
	 *
	 * @param periodMs Milliseconds between steps.
	 * @return The runs.
	 */
	public static GroomRuns seeded(long periodMs) {
		var g = new GroomRuns(periodMs);
		try {
			g.start("Biscuit", true, null);
		} catch (IOException e) {
			g.close();
			throw new UncheckedIOException(e);
		}
		return g;
	}

	/**
	 * Starts a run, or returns the run already started with the same idempotency key.
	 *
	 * @param pet The pet name; blank means {@code "Rex"}.
	 * @param verbose Whether to write {@value #VERBOSE_LINES} FINE lines first.
	 * @param idempotencyKey The client's key, or <jk>null</jk>.
	 * @return The run.
	 * @throws IOException If the temp file cannot be created.
	 */
	@SuppressWarnings({
		"java:S5443" // Files.createTempFile creates the file owner-only (rw-------) on POSIX
	})
	public Run start(String pet, boolean verbose, String idempotencyKey) throws IOException {
		synchronized (order) {
			if (idempotencyKey != null) {
				var prior = keys.get(idempotencyKey);
				if (prior != null && runs.containsKey(prior))
					return runs.get(prior);
			}
			var name = pet == null || pet.isBlank() ? "Rex" : pet.strip();
			if (name.length() > 40)
				name = name.substring(0, 40);
			var file = Files.createTempFile("petstore-groom-", ".log");
			file.toFile().deleteOnExit();
			var r = new Run(UUID.randomUUID().toString().replace("-", ""), name, verbose, file);
			Files.writeString(r.sizeFile, "{\"cols\":" + COLS + ",\"rows\":" + ROWS + "}", UTF_8);
			r.sizeFile.toFile().deleteOnExit();
			if (verbose)
				verboseLines().forEach(r::line);
			runs.put(r.id, r);
			order.addFirst(r);
			if (idempotencyKey != null) {
				r.key = idempotencyKey;
				keys.put(idempotencyKey, r.id);
			}
			while (order.size() > MAX_RUNS)
				drop(order.removeLast());
			r.task.set(ticker.scheduleAtFixedRate(() -> tick(r), periodMs, periodMs, TimeUnit.MILLISECONDS));
			return r;
		}
	}

	/**
	 * @param id A run id, or <jk>null</jk>.
	 * @return The run.
	 */
	public Optional<Run> get(String id) {
		return id == null ? Optional.empty() : Optional.ofNullable(runs.get(id));
	}

	/** @return The newest run, or <jk>null</jk> after {@link #close()}. */
	public Run latest() {
		synchronized (order) {
			return order.peekFirst();
		}
	}

	/** @return The runs, newest first. */
	public List<Run> list() {
		synchronized (order) {
			return List.copyOf(order);
		}
	}

	/**
	 * Resolves a terminal id: {@code <runId>} is the in-memory terminal, {@code <runId>-file} the file source.
	 *
	 * @param terminalId The terminal id from the URL.
	 * @return The source, or empty for an unknown id.
	 */
	public Optional<TerminalSource> source(String terminalId) {
		if (terminalId == null)
			return Optional.empty();
		if (terminalId.endsWith(FILE_SUFFIX))
			return get(terminalId.substring(0, terminalId.length() - FILE_SUFFIX.length())).<TerminalSource>map(Run::fileSource);
		return get(terminalId).<TerminalSource>map(Run::terminal);
	}

	/** Stops the ticker and deletes every run's file. */
	@Override
	public void close() {
		ticker.shutdownNow();
		synchronized (order) {
			order.forEach(this::drop);
			order.clear();
		}
	}

	void tick(Run r) {
		var i = r.step.getAndIncrement();
		if (i >= SCRIPT.size())
			return;
		try {
			SCRIPT.get(i).run(r);
			Files.writeString(r.file, fileLine(i) + "\r\n", UTF_8, StandardOpenOption.APPEND);
		} catch (IOException | RuntimeException e) { // A failed step must not cancel the schedule silently.
			r.line(BOLD + RED + f("Step %s failed: %s", i + 1, e.getMessage()) + RESET);
		}
		if (i == SCRIPT.size() - 1)
			finish(r);
	}

	private static void finish(Run r) {
		r.done = true;
		r.terminal.finish();
		r.fileDone.set(true);
		var t = r.task.get();
		if (t != null)
			t.cancel(false);
	}

	private void drop(Run r) {
		var t = r.task.get();
		if (t != null)
			t.cancel(false);
		runs.remove(r.id);
		if (r.key != null)
			keys.remove(r.key);
		try {
			Files.deleteIfExists(r.file);
			Files.deleteIfExists(r.sizeFile);
		} catch (IOException e) {
			// Best effort: the file is also marked deleteOnExit.
		}
	}

	private static String fileLine(int i) {
		if (i < TRANSCRIPT.size())
			return TRANSCRIPT.get(i);
		if (i == SCRIPT.size() - 1)
			return "\u001b[1;32mgroom finished\u001b[0m";
		return f("\u001b[1m[%02d/%s]\u001b[0m step done", i + 1, SCRIPT.size());
	}

	private static List<String> verboseLines() {
		var out = new ArrayList<String>(VERBOSE_LINES);
		for (var i = 1; i <= VERBOSE_LINES; i++)
			out.add(DIM + "verbose: inspected coat patch " + i + " of " + VERBOSE_LINES + RESET);
		return out;
	}

	static String progress() {
		var sb = new StringBuilder("Wash passes ");
		for (var i = 0; i < WASH_PASSES; i++)
			sb.append(i < WASH_PASSES - 2 ? GREEN : YELLOW).append("\u25a0").append(RESET);
		return sb.append(" (the last two need another rinse)").toString();
	}

	private static String info(String text) { return text; }
	private static String fine(String text) { return DIM + text + RESET; }
	private static String marker(String text) { return BOLD + text + RESET; }
	private static String mark(boolean ok, String text) { return (ok ? GREEN + "\u2713 " : RED + "\u2717 ") + text + RESET; }

	private static List<Step> script() {
		var s = new ArrayList<Step>();
		s.add(r -> r.line(marker("Grooming " + r.pet)));
		s.add(r -> r.line(fine("Reading the grooming profile")));
		for (var tool : List.of("brush", "comb", "shampoo", "conditioner", "towel", "dryer", "clippers", "nail file"))
			s.add(r -> r.line(fine("Checked out the " + tool)));
		s.add(r -> r.line(mark(true, "All tools ready")));
		s.add(r -> r.line(marker("Wash")));
		for (var i = 1; i <= WASH_PASSES; i++) {
			var n = i;
			s.add(r -> r.line(info(f("Wash pass %s of %s", n, WASH_PASSES))));
		}
		s.add(r -> r.line(YELLOW + "The water is 2 degrees cooler than the profile asks for" + RESET));
		s.add(r -> r.line(progress()));
		s.add(r -> r.line(marker("Dry")));
		s.add(r -> r.raw(String.join("\n", TRANSCRIPT)));
		for (var i = 1; i <= 10; i++) {
			var n = i;
			s.add(r -> r.line(info(f("Brushed section %s of 10", n))));
		}
		s.add(r -> r.raw(BOLD + RED + String.join("\n",
			"Dryer overheated; switching to the spare",
			"    at Dryer.heat(Dryer.java:42)",
			"    at Groomer.dry(Groomer.java:17)",
			"    at GroomRun.step(GroomRun.java:88)") + RESET));
		s.add(r -> r.line(mark(false, "Bow tie: none in stock")));
		s.add(r -> r.line(marker("Trim")));
		for (var i = 1; i <= 12; i++) {
			var n = i;
			s.add(r -> r.line(fine(f("Trimmed nail %s of 12", n))));
		}
		s.add(r -> r.line(mark(true, r.pet + " is groomed")));
		return List.copyOf(s);
	}
}
