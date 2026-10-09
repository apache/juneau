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
 * <p>
 * <b>Open trailing line.</b> {@link #openLine(ConsoleOutputLine)} appends a line that stays open: {@link #appendText(String)},
 * {@link #dot()} and {@link #setTail(String)} change it, and {@link #closeLine(String)} closes it. Any other append, and
 * {@link #complete(String, Style)}, closes it first. A page that ends on the open line sets {@code next} to the index
 * of that line, so the next poll re-sends it under the same {@code n}.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	ConsoleOutputLog <jv>log</jv> = <jk>new</jk> ConsoleOutputLog();
 * 	<jv>log</jv>.start();
 * 	<jv>log</jv>.append(ConsoleOutputLine.<jsm>info</jsm>(<js>"Compiling 214 sources"</js>));
 * 	<jv>log</jv>.openLine(ConsoleOutputLine.<jsm>info</jsm>(<js>"ORDERS: filling UID"</js>));
 * 	<jv>log</jv>.dot();
 * 	<jv>log</jv>.closeLine(<js>" 1819 rows in 4s"</js>);
 * 	<jv>log</jv>.openLine(ConsoleOutputLine.<jsm>info</jsm>(<js>"Performing task x: "</js>));
 * 	<jv>log</jv>.setTail(<js>"100 of 200 complete"</js>);
 * 	<jv>log</jv>.closeLine();
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

	/** Past this many pending chars an open raw line stops being re-decoded; the close still decodes all of it. */
	private static final int RAW_PUBLISH_LIMIT = 2 * ConsoleOutputLine.MAX_TEXT_CHARS;

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
	private boolean open;
	private ConsoleOutputLine openHead;
	private int openHeadChars;
	private final StringBuilder openTail = new StringBuilder();
	private Object openOwner;

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
			closeOpenLocked();
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
			closeOpenLocked();
			return appendLocked(decoder.line(level, raw));
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Closes any open line, then appends a validated copy of a line as the open trailing line. Its text is the line's
	 * fixed head; {@link #appendText(String)}, {@link #dot()} and {@link #setTail(String)} change only what follows it.
	 *
	 * @param line The line.
	 * @return The assigned {@code n}, or {@code -1} when the cap refused the line (no line is then open).
	 * @throws IllegalArgumentException If the line is invalid, or has frags and no room for one more.
	 */
	public long openLine(ConsoleOutputLine line) {
		Objects.requireNonNull(line, "line");
		if (line.frags != null && line.frags.size() >= ConsoleOutputLine.MAX_FRAGS)
			throw iaex("ConsoleOutputLog openLine needs room for one more fragment; got %s.", line.frags.size());
		lock.writeLock().lock();
		try {
			closeOpenLocked();
			var n = appendLocked(line);
			if (n > 0)
				markOpenLocked(null);
			return n;
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Appends plain text to the open line. Past {@link ConsoleOutputLine#MAX_TEXT_CHARS} the line closes at the cap and
	 * the rest continues on a new open line at the same level. Never throws.
	 *
	 * @param text The text.
	 * @return <jk>false</jk> when no line is open (nothing happens).
	 */
	public boolean appendText(String text) {
		lock.writeLock().lock();
		try {
			if (! open)
				return false;
			if (text != null && ! text.isEmpty())
				appendTailLocked(text);
			return true;
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Same as {@code appendText(".")}.
	 *
	 * @return <jk>false</jk> when no line is open.
	 */
	public boolean dot() {
		return appendText(".");
	}

	/**
	 * Replaces everything after the open line's head. A tail that would pass {@link ConsoleOutputLine#MAX_TEXT_CHARS}
	 * is truncated to fit and never wraps, because a rewrite is the line's whole new state. Never throws.
	 *
	 * @param text The new tail, or <jk>null</jk> for none.
	 * @return <jk>false</jk> when no line is open (nothing happens).
	 */
	public boolean setTail(String text) {
		lock.writeLock().lock();
		try {
			if (! open)
				return false;
			openTail.setLength(0);
			if (text != null) {
				var room = Math.max(0, ConsoleOutputLine.MAX_TEXT_CHARS - openHeadChars);
				openTail.append(text.length() <= room ? text : text.substring(0, ConsoleOutputLine.cutPoint(text, room)));
			}
			storeOpenLocked();
			return true;
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Closes the open line with its current text.
	 *
	 * @return <jk>false</jk> when no line is open.
	 */
	public boolean closeLine() {
		return closeLine(null);
	}

	/**
	 * Appends a suffix to the open line and closes it. Never throws.
	 *
	 * @param suffix The suffix, or <jk>null</jk>.
	 * @return <jk>false</jk> when no line is open (nothing happens).
	 */
	public boolean closeLine(String suffix) {
		lock.writeLock().lock();
		try {
			if (! open)
				return false;
			if (suffix != null && ! suffix.isEmpty())
				appendTailLocked(suffix);
			closeOpenLocked();
			return true;
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Reads until EOF, splitting on {@code \n}, and appends each line through this log's {@link AnsiDecoder}. Blocks.
	 *
	 * <p>
	 * After each read that leaves unterminated text pending, that text is published as the open trailing line, so
	 * progress dots show while they arrive. {@code \n} or EOF closes it. A bare {@code \r} rewrites the line: only the
	 * text after the last one is kept, and a trailing {@code \r} is ignored. So that a huge line without a newline
	 * costs linear work, only its first {@code 2 * MAX_TEXT_CHARS} chars are decoded while it is open; closing it
	 * decodes all of it. If another writer closes the open line
	 * first, the text that arrives afterwards continues on a new line.
	 *
	 * @param level The level of every line.
	 * @param reader The reader. Not closed.
	 * @return The number of lines read.
	 * @throws IOException If reading fails.
	 */
	public int appendRaw(Level level, Reader reader) throws IOException {
		Objects.requireNonNull(reader, "reader");
		var owner = new Object();
		var count = 0;
		var sb = new StringBuilder();
		var published = -1;
		var buf = new char[8192];
		int r;
		while ((r = reader.read(buf)) != -1) {
			for (var i = 0; i < r; i++) {
				if (buf[i] == '\n') {
					closeRaw(owner, level, sb, published);
					sb.setLength(0);
					published = -1;
					count++;
				} else {
					sb.append(buf[i]);
				}
			}
			if (sb.length() > 0 && sb.length() != published && published < RAW_PUBLISH_LIMIT)
				published = publishRaw(owner, level, sb, published);
		}
		if (sb.length() > 0) {
			closeRaw(owner, level, sb, published);
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
			closeOpenLocked();
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
			var next = open && to == lines.size() && to > from ? to - 1 : to;
			var p = ConsoleOutputPage.forward().lines(copies(from, to)).next(String.valueOf(next)).more(more)
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
			var next = open && to > from ? to - 1 : to;
			var p = ConsoleOutputPage.forward().lines(copies(from, to)).next(String.valueOf(next))
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
		c.open = null;
		c.n = (long)lines.size() + 1;
		c.validate();
		lines.add(c);
		return c.n;
	}

	/** Marks the last stored line as the open line. */
	private void markOpenLocked(Object owner) {
		var last = lines.get(lines.size() - 1);
		last.open = Boolean.TRUE;
		openHead = last.copy();
		openHead.open = null;
		openHeadChars = ConsoleOutputPage.visibleChars(openHead);
		openTail.setLength(0);
		openOwner = owner;
		open = true;
	}

	/** Closes the open line, if any, keeping its current text. */
	private void closeOpenLocked() {
		if (! open)
			return;
		lines.get(lines.size() - 1).open = null;
		open = false;
		openHead = null;
		openTail.setLength(0);
		openOwner = null;
	}

	/** Re-stores the open line as head plus tail; a frag head gets the tail as one plain trailing frag. */
	private void storeOpenLocked() {
		var l = openHead.copy();
		var tail = openTail.toString();
		if (! tail.isEmpty()) {
			if (l.text != null)
				l.text = l.text + tail;
			else
				l.frags.add(Frag.text(tail));
		}
		l.open = Boolean.TRUE;
		lines.set(lines.size() - 1, l.validate());
	}

	/** Appends to the tail, wrapping onto a new open line at the same level when the cap is reached. */
	private void appendTailLocked(String text) {
		var s = text;
		while (true) {
			var room = ConsoleOutputLine.MAX_TEXT_CHARS - openHeadChars - openTail.length();
			if (s.length() <= room) {
				openTail.append(s);
				storeOpenLocked();
				return;
			}
			var cut = ConsoleOutputLine.cutPoint(s, Math.max(0, room));
			openTail.append(s, 0, cut);
			storeOpenLocked();
			var level = openHead.level;
			var owner = openOwner;
			closeOpenLocked();
			s = s.substring(cut);
			if (appendLocked(ConsoleOutputLine.of(level, "")) < 0)
				return;
			markOpenLocked(owner);
		}
	}

	/** True if the open line belongs to the given raw reader. */
	private boolean ownsOpenLocked(Object owner) {
		return open && openOwner == owner;
	}

	/**
	 * Publishes a raw reader's pending text as its open line. Returns how many pending chars are now shown, or
	 * {@code -1} when nothing is shown. If another writer closed the line, the shown chars are dropped first.
	 */
	private int publishRaw(Object owner, Level level, StringBuilder sb, int published) {
		lock.writeLock().lock();
		try {
			if (published >= 0 && ! ownsOpenLocked(owner))
				sb.delete(0, published);
			var end = Math.min(sb.length(), RAW_PUBLISH_LIMIT);
			if (end > 0 && Character.isHighSurrogate(sb.charAt(end - 1)))
				end--;
			if (end == 0)
				return -1;
			var line = decoder.copy().line(level, sb.substring(0, end));
			if (ownsOpenLocked(owner)) {
				line.n = lines.get(lines.size() - 1).n;
				lines.set(lines.size() - 1, line.validate());
				markOpenLocked(owner);
			} else {
				closeOpenLocked();
				if (appendLocked(line) < 0)
					return -1;
				markOpenLocked(owner);
			}
			return end;
		} finally {
			lock.writeLock().unlock();
		}
	}

	/** Closes a raw reader's pending text as a line, replacing its open line in place when it still owns it. */
	private void closeRaw(Object owner, Level level, StringBuilder sb, int published) {
		lock.writeLock().lock();
		try {
			if (published >= 0 && ! ownsOpenLocked(owner)) {
				sb.delete(0, published);
				if (sb.length() == 0)
					return;
			}
			var line = decoder.line(level, sb.toString());
			if (ownsOpenLocked(owner)) {
				line.n = lines.get(lines.size() - 1).n;
				lines.set(lines.size() - 1, line.validate());
				closeOpenLocked();
			} else {
				closeOpenLocked();
				appendLocked(line);
			}
		} finally {
			lock.writeLock().unlock();
		}
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
