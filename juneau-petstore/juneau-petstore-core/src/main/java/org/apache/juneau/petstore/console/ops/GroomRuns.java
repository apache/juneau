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
import org.apache.juneau.rest.server.views.*;
import org.apache.juneau.rest.server.views.ConsoleOutputLine.*;

/**
 * The groom-pet demo runs behind the Jobs page's console-output cards.
 *
 * <p>
 * A run writes about 60 styled lines to a {@link ConsoleOutputLog}, one step per tick: INFO and FINE lines, a
 * WARNING, {@code check}/{@code cancel} icon lines, a progress strip of block fragments with tooltips and fragment
 * links, a multi-line SEVERE entry, an inline image, marker lines, and a canned ANSI transcript piped through
 * {@link ConsoleOutputLog#appendRaw(Level, Reader)}.  Each tick also appends one ANSI line to a temp file, read
 * through one cached {@link FileConsoleOutputSource} per run.  A verbose run first writes {@value #VERBOSE_LINES}
 * FINE lines, so a 5000-line tail offers "Load earlier lines".
 *
 * <p>
 * Run ids are random 128-bit hex strings, so a log can be read only by someone who was given its id.
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
	"java:S1192" // The script's literals are demo text; constants would obscure it.
})
public final class GroomRuns implements AutoCloseable {

	/** System property: milliseconds between steps. */
	public static final String PERIOD_PROPERTY = "petstore.groom.periodMs";

	/** The default step period; 53 steps take about 35 seconds. */
	public static final long DEFAULT_PERIOD_MS = 650;

	/** Suffix of the log id that selects a run's file-backed source. */
	public static final String FILE_SUFFIX = "-file";

	/** The same-origin image the script shows. */
	public static final String PHOTO = "/console/ops/jobs/groom-photo.svg";

	/** The anchor prefix the Jobs page's main console uses; the progress strip links to it. */
	public static final String ANCHOR_PREFIX = "groom-L";

	/** The image served at {@link #PHOTO}. */
	public static final String PHOTO_SVG = "<svg xmlns='http://www.w3.org/2000/svg' width='160' height='72' viewBox='0 0 160 72' role='img'>"
		+ "<title>Before and after</title>"
		+ "<rect width='160' height='72' rx='8' fill='#eef2f6'/>"
		+ "<circle cx='44' cy='36' r='22' fill='#9a7b5a'/><circle cx='116' cy='36' r='22' fill='#d9b38c'/>"
		+ "<path d='M70 36h20m-6-6 6 6-6 6' stroke='#334' stroke-width='3' fill='none'/></svg>";

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
		final ConsoleOutputLog log = new ConsoleOutputLog();
		final Path file;
		final AtomicReference<FileConsoleOutputSource.Status> fileStatus;
		final FileConsoleOutputSource fileSource;
		final List<Long> washLines = new CopyOnWriteArrayList<>();
		final AtomicInteger step = new AtomicInteger();
		volatile ScheduledFuture<?> task;
		volatile String key;

		Run(String id, String pet, boolean verbose, Path file) {
			this.id = id;
			this.pet = pet;
			this.verbose = verbose;
			this.file = file;
			fileStatus = new AtomicReference<>(new FileConsoleOutputSource.Status("RUNNING", Style.ACCENT, false, submitted, null));
			fileSource = FileConsoleOutputSource.create(file).ansi(true).status(fileStatus::get).build();
		}

		/** @return The run id, which is also its in-memory log id. */
		public String id() { return id; }

		/** @return The pet name. */
		public String pet() { return pet; }

		/** @return The in-memory log. */
		public ConsoleOutputLog log() { return log; }

		/** @return The cached file-backed source over this run's transcript. */
		public FileConsoleOutputSource fileSource() { return fileSource; }

		/** @return The temp file the run appends to. */
		public Path file() { return file; }

		/** @return The Groom runs table row. */
		public JsonMap row() {
			return JsonMap.of("id", id, "pet", pet, "submitted", submitted.toString(), "state", log.tail(0).state, "lines", log.size(), "verbose", verbose);
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
			r.log.start();
			if (verbose)
				r.log.appendRaw(Level.FINE, new StringReader(verboseText()));
			runs.put(r.id, r);
			order.addFirst(r);
			if (idempotencyKey != null) {
				r.key = idempotencyKey;
				keys.put(idempotencyKey, r.id);
			}
			while (order.size() > MAX_RUNS)
				drop(order.removeLast());
			r.task = ticker.scheduleAtFixedRate(() -> tick(r), periodMs, periodMs, TimeUnit.MILLISECONDS);
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
	 * Resolves a console-output log id: {@code <runId>} is the in-memory log, {@code <runId>-file} the file source.
	 *
	 * @param logId The log id from the URL.
	 * @return The source, or empty for an unknown id.
	 */
	public Optional<ConsoleOutputSource> source(String logId) {
		if (logId == null)
			return Optional.empty();
		if (logId.endsWith(FILE_SUFFIX))
			return get(logId.substring(0, logId.length() - FILE_SUFFIX.length())).<ConsoleOutputSource>map(Run::fileSource);
		return get(logId).<ConsoleOutputSource>map(Run::log);
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
			Files.writeString(r.file, fileLine(i) + "\n", UTF_8, StandardOpenOption.APPEND);
		} catch (IOException | RuntimeException e) { // A failed step must not cancel the schedule silently.
			r.log.append(ConsoleOutputLine.severe(f("Step %s failed: %s", i + 1, e.getMessage())));
		}
		if (i == SCRIPT.size() - 1)
			finish(r);
	}

	private static void finish(Run r) {
		r.log.complete("DONE", Style.SUCCESS);
		r.fileStatus.set(new FileConsoleOutputSource.Status("DONE", Style.SUCCESS, true, r.submitted, Duration.between(r.submitted, Instant.now()).toMillis()));
		var t = r.task;
		if (t != null)
			t.cancel(false);
	}

	private void drop(Run r) {
		var t = r.task;
		if (t != null)
			t.cancel(false);
		runs.remove(r.id);
		if (r.key != null)
			keys.remove(r.key);
		try {
			Files.deleteIfExists(r.file);
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

	private static String verboseText() {
		var sb = new StringBuilder(VERBOSE_LINES * 40);
		for (var i = 1; i <= VERBOSE_LINES; i++)
			sb.append("verbose: inspected coat patch ").append(i).append(" of ").append(VERBOSE_LINES).append('\n');
		return sb.toString();
	}

	static ConsoleOutputLine progress(List<Long> washLines) {
		var frags = new ArrayList<Frag>();
		frags.add(Frag.text("Wash passes "));
		var n = washLines.size();
		for (var i = 0; i < n; i++) {
			var clean = i < n - 2;
			var b = Frag.block().style(clean ? Style.SUCCESS : Style.WARN)
				.tooltip(f("Pass %s: %s", i + 1, clean ? "clean" : "rinse again"))
				.label(f("Pass %s", i + 1));
			var line = washLines.get(i);
			if (line > 0)
				b.href("#" + ANCHOR_PREFIX + line);
			frags.add(b);
		}
		return ConsoleOutputLine.frags(frags.toArray(Frag[]::new));
	}

	private static List<Step> script() {
		var s = new ArrayList<Step>();
		s.add(r -> r.log.append(ConsoleOutputLine.info("Grooming " + r.pet).marker(true)));
		s.add(r -> r.log.append(ConsoleOutputLine.fine("Reading the grooming profile")));
		for (var tool : List.of("brush", "comb", "shampoo", "conditioner", "towel", "dryer", "clippers", "nail file"))
			s.add(r -> r.log.append(ConsoleOutputLine.fine("Checked out the " + tool)));
		s.add(r -> r.log.append(ConsoleOutputLine.info("All tools ready").icon("check").style(Style.SUCCESS)));
		s.add(r -> r.log.append(ConsoleOutputLine.info("Wash").marker(true)));
		for (var i = 1; i <= WASH_PASSES; i++) {
			var n = i;
			s.add(r -> r.washLines.add(r.log.append(ConsoleOutputLine.info(f("Wash pass %s of %s", n, WASH_PASSES)))));
		}
		s.add(r -> r.log.append(ConsoleOutputLine.warning("The water is 2 degrees cooler than the profile asks for")));
		s.add(r -> r.log.append(progress(r.washLines)));
		s.add(r -> r.log.append(ConsoleOutputLine.info("Dry").marker(true)));
		s.add(r -> r.log.appendRaw(Level.INFO, new StringReader(String.join("\n", TRANSCRIPT))));
		for (var i = 1; i <= 10; i++) {
			var n = i;
			s.add(r -> r.log.append(ConsoleOutputLine.info(f("Brushed section %s of 10", n))));
		}
		s.add(r -> r.log.append(ConsoleOutputLine.severe(String.join("\n",
			"Dryer overheated; switching to the spare",
			"    at Dryer.heat(Dryer.java:42)",
			"    at Groomer.dry(Groomer.java:17)",
			"    at GroomRun.step(GroomRun.java:88)"))));
		s.add(r -> r.log.append(ConsoleOutputLine.info("Bow tie: none in stock").icon("cancel").style(Style.ERROR)));
		s.add(r -> r.log.append(ConsoleOutputLine.info("Before and after").image(PHOTO, "A freshly groomed pet")));
		s.add(r -> r.log.append(ConsoleOutputLine.info("Trim").marker(true)));
		for (var i = 1; i <= 12; i++) {
			var n = i;
			s.add(r -> r.log.append(ConsoleOutputLine.fine(f("Trimmed nail %s of 12", n))));
		}
		s.add(r -> r.log.append(ConsoleOutputLine.info(r.pet + " is groomed").icon("check").style(Style.SUCCESS)));
		return List.copyOf(s);
	}
}
