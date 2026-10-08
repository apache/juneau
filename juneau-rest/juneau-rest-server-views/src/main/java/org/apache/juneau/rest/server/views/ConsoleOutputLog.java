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
package org.apache.juneau.rest.server.views;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.nio.charset.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.locks.*;
import java.util.regex.*;
import java.util.stream.*;

import org.apache.juneau.rest.server.views.ConsoleOutputLine.*;

/**
 * A thread-safe, in-memory, append-only console-output log.
 *
 * <p>
 * Tokens are decimal record indexes: {@code next} is the index of the first record not yet returned, so
 * {@code "0"} is the start. A new log is {@code PENDING}; {@link #start()} makes it {@code RUNNING};
 * {@link #complete(String, Style)} makes it terminal.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	ConsoleOutputLog <jv>log</jv> = <jk>new</jk> ConsoleOutputLog();
 * 	<jv>log</jv>.start();
 * 	<jv>log</jv>.append(ConsoleOutputLine.<jsm>info</jsm>(<js>"Compiling 214 sources"</js>));
 * 	<jv>log</jv>.complete(<js>"DONE"</js>, Style.<jsf>SUCCESS</jsf>);
 * </p>
 *
 * @since 10.0.0
 */
public class ConsoleOutputLog implements ConsoleOutputSource {

	/** Default line cap. */
	public static final int DEFAULT_MAX_LINES = 100_000;

	/** The state of a log that has not started. */
	public static final String STATE_PENDING = "PENDING";

	/** The state set by {@link #start()}. */
	public static final String STATE_RUNNING = "RUNNING";

	private static final Pattern INDEX = Pattern.compile("^(0|[1-9][0-9]{0,9})$");

	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private final List<ConsoleOutputLine> lines = new ArrayList<>();
	private final AnsiDecoder decoder = new AnsiDecoder();
	private final int maxLines;
	private final Clock clock;
	private boolean truncated;
	private String state = STATE_PENDING;
	private Style stateStyle;
	private boolean terminal;
	private Instant startedAt;
	private Long durationMs;

	/** Creates a log with {@link #DEFAULT_MAX_LINES}. */
	public ConsoleOutputLog() {
		this(DEFAULT_MAX_LINES);
	}

	/**
	 * Creates a log with a line cap.
	 *
	 * @param maxLines The cap, at least 1.
	 */
	public ConsoleOutputLog(int maxLines) {
		this(maxLines, Clock.systemUTC());
	}

	/**
	 * Creates a log with a line cap and a clock.
	 *
	 * @param maxLines The cap, at least 1.
	 * @param clock The clock for {@code startedAt}, {@code durationMs} and {@code now}.
	 */
	public ConsoleOutputLog(int maxLines, Clock clock) {
		if (maxLines < 1)
			throw iaex("ConsoleOutputLog maxLines must be >= 1; got %s.", maxLines);
		this.maxLines = maxLines;
		this.clock = Objects.requireNonNull(clock, "clock");
	}

	/**
	 * Appends a validated copy of a line and numbers the copy. The caller's object is never modified.
	 *
	 * @param line The line.
	 * @return The assigned {@code n}, or {@code -1} when the cap refused the line.
	 * @throws IllegalArgumentException If the line is invalid.
	 */
	public long append(ConsoleOutputLine line) {
		Objects.requireNonNull(line, "line");
		lock.writeLock().lock();
		try {
			return appendLocked(line);
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Converts one raw process line through this log's {@link AnsiDecoder} and appends it.
	 *
	 * @param level The level.
	 * @param raw The raw line, possibly with ANSI escape sequences.
	 * @return The assigned {@code n}, or {@code -1} when the cap refused the line.
	 */
	public long appendAnsi(Level level, String raw) {
		lock.writeLock().lock();
		try {
			return appendLocked(decoder.line(level, raw));
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Reads until EOF, splitting on {@code \n}, and calls {@link #appendAnsi(Level, String)} per line. Blocks.
	 *
	 * @param level The level of every line.
	 * @param reader The reader. Not closed.
	 * @return The number of lines read.
	 * @throws IOException If reading fails.
	 */
	public int appendRaw(Level level, Reader reader) throws IOException {
		Objects.requireNonNull(reader, "reader");
		var count = 0;
		var sb = new StringBuilder();
		var buf = new char[8192];
		int r;
		while ((r = reader.read(buf)) != -1) {
			for (var i = 0; i < r; i++) {
				if (buf[i] == '\n') {
					appendAnsi(level, sb.toString());
					sb.setLength(0);
					count++;
				} else {
					sb.append(buf[i]);
				}
			}
		}
		if (sb.length() > 0) {
			appendAnsi(level, sb.toString());
			count++;
		}
		return count;
	}

	/**
	 * Reads a byte stream until EOF; see {@link #appendRaw(Level, Reader)}.
	 *
	 * @param level The level of every line.
	 * @param in The stream. Not closed.
	 * @param charset The charset.
	 * @return The number of lines read.
	 * @throws IOException If reading fails.
	 */
	public int appendRaw(Level level, InputStream in, Charset charset) throws IOException {
		Objects.requireNonNull(in, "in");
		Objects.requireNonNull(charset, "charset");
		return appendRaw(level, new InputStreamReader(in, charset));
	}

	/** Marks the log started: sets {@code startedAt} once and, unless terminal, {@code RUNNING}. */
	public void start() {
		lock.writeLock().lock();
		try {
			if (startedAt == null)
				startedAt = clock.instant();
			if (! terminal) {
				state = STATE_RUNNING;
				stateStyle = Style.ACCENT;
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Marks the log terminal. Only the first call has an effect.
	 *
	 * @param state The final display state.
	 * @param stateStyle The optional style.
	 */
	public void complete(String state, Style stateStyle) {
		lock.writeLock().lock();
		try {
			if (terminal)
				return;
			terminal = true;
			this.state = state;
			this.stateStyle = stateStyle;
			durationMs = startedAt == null ? null : Math.max(0, Duration.between(startedAt, clock.instant()).toMillis());
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Sets the display state without changing the terminal flag.
	 *
	 * @param state The display state.
	 * @param stateStyle The optional style.
	 */
	public void setState(String state, Style stateStyle) {
		lock.writeLock().lock();
		try {
			this.state = state;
			this.stateStyle = stateStyle;
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * The number of stored records, including a truncation marker.
	 *
	 * @return The count.
	 */
	public int size() {
		lock.readLock().lock();
		try {
			return lines.size();
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public ConsoleOutputPage page(String after, int max) {
		lock.readLock().lock();
		try {
			var from = after == null ? 0 : index(after);
			var limit = Math.max(1, max);
			var to = from;
			var chars = 0L;
			while (to < lines.size() && to - from < limit) {
				var c = ConsoleOutputPage.visibleChars(lines.get(to));
				if (to > from && chars + c > ConsoleOutputPage.MAX_PAGE_CHARS)
					break;
				chars += c;
				to++;
			}
			var more = to < lines.size();
			var p = ConsoleOutputPage.forward().lines(copies(from, to)).next(String.valueOf(to)).more(more)
				.before(from > 0 ? String.valueOf(from) : null);
			return status(p, ! more);
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public ConsoleOutputPage tail(int n) {
		lock.readLock().lock();
		try {
			var to = lines.size();
			var from = newestThatFit(Math.max(0, to - Math.max(0, n)), to);
			var p = ConsoleOutputPage.forward().lines(copies(from, to)).next(String.valueOf(to))
				.before(from > 0 ? String.valueOf(from) : null);
			return status(p, true);
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public ConsoleOutputPage before(String token, int limit) {
		lock.readLock().lock();
		try {
			var to = index(token);
			var from = newestThatFit(Math.max(0, to - Math.max(1, limit)), to);
			var p = ConsoleOutputPage.earlier().lines(copies(from, to)).before(from > 0 ? String.valueOf(from) : null);
			return status(p, false);
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public Stream<ConsoleOutputLine> stream() {
		lock.readLock().lock();
		try {
			return copies(0, lines.size()).stream();
		} finally {
			lock.readLock().unlock();
		}
	}

	private long appendLocked(ConsoleOutputLine line) {
		if (lines.size() >= maxLines) {
			if (! truncated) {
				truncated = true;
				var marker = ConsoleOutputLine.severe("… output truncated");
				marker.n = (long)lines.size() + 1;
				lines.add(marker.validate());
			}
			return -1;
		}
		var c = line.copy();
		c.n = (long)lines.size() + 1;
		c.validate();
		lines.add(c);
		return c.n;
	}

	private int index(String token) {
		if (token == null || ! INDEX.matcher(token).matches())
			throw new UnknownTokenException(token);
		var i = Long.parseLong(token);
		if (i > lines.size())
			throw new UnknownTokenException(token);
		return (int)i;
	}

	/** Moves {@code from} forward until {@code [from, to)} fits the page budget (always keeping one line). */
	private int newestThatFit(int from, int to) {
		var chars = 0L;
		var start = to;
		while (start > from) {
			var c = ConsoleOutputPage.visibleChars(lines.get(start - 1));
			if (start < to && chars + c > ConsoleOutputPage.MAX_PAGE_CHARS)
				break;
			chars += c;
			start--;
		}
		return start;
	}

	private List<ConsoleOutputLine> copies(int from, int to) {
		var l = new ArrayList<ConsoleOutputLine>(to - from);
		for (var i = from; i < to; i++)
			l.add(lines.get(i).copy());
		return l;
	}

	private ConsoleOutputPage status(ConsoleOutputPage p, boolean reachesEnd) {
		var t = terminal && reachesEnd && p.kind == ConsoleOutputPage.Kind.FORWARD;
		return p.status(state, stateStyle, t, startedAt, t ? durationMs : null).now(clock.instant());
	}
}
