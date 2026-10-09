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

import static java.nio.charset.StandardCharsets.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.regex.*;
import java.util.stream.*;

import org.apache.juneau.rest.server.views.ConsoleOutputLine.*;

/**
 * A {@link ConsoleOutputSource} over one growing UTF-8 text file, paged through a shared {@link FileLineIndex}.
 *
 * <p>
 * <b>Path safety.</b> The application supplies the {@link Path} when it builds the source; nothing in a request
 * ever becomes a path. An application that derives paths from ids must resolve them against a fixed root,
 * {@code normalize()} the result, and require {@code result.startsWith(root)}. The file is opened read-only for each
 * call and closed before the call returns (by the caller, for {@link #stream()}).
 *
 * <p>
 * <b>Tokens</b> are {@code <epoch>.<byteOffset>.<n>}: line {@code n} starts at {@code byteOffset} in file generation
 * {@code epoch}. A token whose epoch is stale (the file was truncated, replaced or rewritten), or whose offset is not
 * the start of line {@code n}, throws {@link ConsoleOutputSource.UnknownTokenException UnknownTokenException}.
 *
 * <p>
 * <b>Partial last line.</b> While the {@link Status} is not terminal, bytes after the last {@code \n} are served as the
 * open trailing line ({@code open: true}). They are re-read on every call, never indexed, and {@code next} points before
 * them. A trailing incomplete UTF-8 sequence is held back. Once terminal, they are served as an ordinary closed line.
 * The status is read before the index is refreshed, so a terminal status guarantees the writer has finished.
 *
 * <p>
 * <b>Bare {@code \r}.</b> A line keeps only the text after its last bare {@code \r}, with or without ANSI decoding.
 *
 * <p>
 * Keep <b>one instance per log</b> (for example in a {@code ConcurrentHashMap} keyed by run id); a new instance
 * re-indexes the file from byte 0.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	FileConsoleOutputSource <jv>src</jv> = FileConsoleOutputSource.<jsm>create</jsm>(<jv>root</jv>.resolve(<js>"run-42.log"</js>))
 * 		.status(() -&gt; <jv>run</jv>.toStatus())
 * 		.build();
 * </p>
 *
 * @since 10.0.0
 */
public final class FileConsoleOutputSource implements ConsoleOutputSource {

	/** Lines longer than this many bytes are cut. */
	public static final int MAX_LINE_BYTES = 262_144;

	/** Forward-page line cap; equals {@code ConsoleOutputEndpoints.MAX_PAGE_LINES}. */
	public static final int MAX_PAGE_LINES = 2000;

	private static final int MAX_LIMIT = 10_000;
	private static final Pattern TOKEN = Pattern.compile("^([0-9a-z]{6})\\.(0|[1-9][0-9]{0,17})\\.([1-9][0-9]{0,17})$");
	private static final AtomicInteger OPEN_READERS = new AtomicInteger();

	/**
	 * Job status reported on each page.
	 *
	 * @param state Display state.
	 * @param stateStyle Optional style.
	 * @param terminal Whether the writer has finished.
	 * @param startedAt Start instant, or <jk>null</jk> when pending.
	 * @param durationMs Duration, used on the terminal page only.
	 */
	public record Status(String state, Style stateStyle, boolean terminal, Instant startedAt, Long durationMs) {

		/** The default: running and non-terminal. */
		public static final Status RUNNING = new Status("RUNNING", Style.ACCENT, false, null, null);
	}

	/**
	 * Builder for {@link FileConsoleOutputSource}.
	 */
	public static final class Builder {
		private final Path file;
		private Supplier<Status> status = () -> Status.RUNNING;
		private boolean ansi = true;
		private BiFunction<String,ConsoleOutputLine,ConsoleOutputLine> decorate;
		private FileLineIndex index;

		Builder(Path file) {
			this.file = Objects.requireNonNull(file, "file");
		}

		/**
		 * Sets the status supplier, called once per request before the file is read.
		 *
		 * @param value The supplier.
		 * @return This object.
		 */
		public Builder status(Supplier<Status> value) {
			status = Objects.requireNonNull(value, "status");
			return this;
		}

		/**
		 * Whether each line goes through an {@link AnsiDecoder} (default <jk>true</jk>).
		 *
		 * @param value The flag.
		 * @return This object.
		 */
		public Builder ansi(boolean value) {
			ansi = value;
			return this;
		}

		/**
		 * Sets a decorator called with the raw decoded text and the converted line.
		 *
		 * @param value The decorator; must return a valid line with the same {@code n}.
		 * @return This object.
		 */
		public Builder decorate(BiFunction<String,ConsoleOutputLine,ConsoleOutputLine> value) {
			decorate = value;
			return this;
		}

		/**
		 * Sets the index (default {@link FileLineIndex#of(Path)}).
		 *
		 * @param value An index over the same file.
		 * @return This object.
		 */
		public Builder index(FileLineIndex value) {
			index = value;
			return this;
		}

		/**
		 * Builds the source.
		 *
		 * @return A new source.
		 */
		public FileConsoleOutputSource build() {
			if (index != null && ! index.file().equals(file))
				throw iaex("FileConsoleOutputSource index is for a different file: '%s' vs '%s'.", index.file(), file);
			return new FileConsoleOutputSource(this);
		}
	}

	private record Pos(long offset, long n) {}

	private record Raw(long offset, long n, long end, String text) {}

	private record Kept(Raw raw, ConsoleOutputLine line, int chars) {}

	private final Path file;
	private final Supplier<Status> status;
	private final boolean ansi;
	private final BiFunction<String,ConsoleOutputLine,ConsoleOutputLine> decorate;
	private final FileLineIndex index;

	private FileConsoleOutputSource(Builder b) {
		file = b.file;
		status = b.status;
		ansi = b.ansi;
		decorate = b.decorate;
		index = b.index != null ? b.index : FileLineIndex.of(b.file);
	}

	/**
	 * Creates a builder.
	 *
	 * @param file The file, supplied by the application.
	 * @return A new builder.
	 */
	public static Builder create(Path file) {
		return new Builder(file);
	}

	@Override
	public ConsoleOutputPage page(String after, int max) {
		var st = status();
		var s = refresh();
		var start = after == null ? new Pos(0, 1) : position(after, s, st);
		if (isEnd(start, s))
			return status(ConsoleOutputPage.forward().next(after).before(after), st, true);
		var limit = Math.max(1, Math.min(max, MAX_PAGE_LINES));
		var end = st.terminal() ? s.fileSize() : s.indexedBytes();
		var lines = new ArrayList<ConsoleOutputLine>();
		var next = start;
		var more = false;
		try (var r = new LineReader(file, start, end)) {
			var dec = new AnsiDecoder();
			var chars = 0L;
			while (lines.size() < limit) {
				var raw = r.next();
				if (raw == null)
					break;
				var line = convert(raw, dec);
				var c = ConsoleOutputPage.visibleChars(line);
				if (! lines.isEmpty() && chars + c > ConsoleOutputPage.MAX_PAGE_CHARS) {
					more = true;
					break;
				}
				chars += c;
				lines.add(line);
				next = new Pos(raw.end(), raw.n() + 1);
			}
			if (! more)
				more = r.hasMore();
			if (! more && ! st.terminal()) {
				var raw = openRaw(s);
				if (raw != null) {
					var open = convert(raw, dec).open(true);
					if (lines.size() >= limit || (! lines.isEmpty() && chars + ConsoleOutputPage.visibleChars(open) > ConsoleOutputPage.MAX_PAGE_CHARS))
						more = true;
					else
						lines.add(open);
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		var p = ConsoleOutputPage.forward().lines(lines).next(token(s, next)).more(more)
			.before(start.n() > 1 ? token(s, start) : null);
		return status(p, st, ! more);
	}

	@Override
	public ConsoleOutputPage tail(int n) {
		var st = status();
		var s = refresh();
		var trailing = s.fileSize() > s.indexedBytes();
		var partial = st.terminal() && trailing;
		var total = s.indexedLines() + (trailing ? 1 : 0);
		var startN = Math.max(1, total - Math.max(0, Math.min(n, MAX_LIMIT)) + 1);
		var start = new Pos(seek(Math.min(startN, s.indexedLines() + 1)), startN);
		var end = partial ? new Pos(s.fileSize(), s.indexedLines() + 2) : new Pos(s.indexedBytes(), s.indexedLines() + 1);
		var kept = newest(start, end.offset(), st.terminal() ? null : s);
		var p = ConsoleOutputPage.forward().lines(lines(kept)).next(token(s, end)).before(first(s, kept, end));
		return status(p, st, true);
	}

	@Override
	public ConsoleOutputPage before(String token, int limit) {
		var st = status();
		var s = refresh();
		if (token == null)
			throw new UnknownTokenException(null);
		var to = position(token, s, st);
		var startN = Math.max(1, to.n() - Math.max(1, Math.min(limit, MAX_LIMIT)));
		var start = new Pos(seek(Math.min(startN, s.indexedLines() + 1)), startN);
		var kept = newest(start, to.offset(), null);
		var p = ConsoleOutputPage.earlier().lines(lines(kept)).before(first(s, kept, to));
		return status(p, st, false);
	}

	@Override
	public Stream<ConsoleOutputLine> stream() {
		var st = status();
		var s = refresh();
		var end = st.terminal() ? s.fileSize() : s.indexedBytes();
		var r = new LineReader(file, new Pos(0, 1), end);
		var dec = new AnsiDecoder();
		var it = new Iterator<ConsoleOutputLine>() {
			private Raw pending;
			private boolean done;
			private boolean openTried;
			private boolean pendingOpen;

			@Override
			public boolean hasNext() {
				if (pending == null && ! done) {
					try {
						pending = r.next();
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
					if (pending == null && ! openTried && ! st.terminal()) {
						openTried = true;
						pending = openRaw(s);
						pendingOpen = pending != null;
					}
					done = pending == null;
				}
				return pending != null;
			}

			@Override
			public ConsoleOutputLine next() {
				if (! hasNext())
					throw new NoSuchElementException();
				var raw = pending;
				var open = pendingOpen;
				pending = null;
				pendingOpen = false;
				var line = convert(raw, dec);
				return open ? line.open(true) : line;
			}
		};
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(it, Spliterator.ORDERED | Spliterator.NONNULL), false)
			.onClose(r::close);
	}

	/** Number of currently open file readers (test hook). */
	static int openReaders() {
		return OPEN_READERS.get();
	}

	/**
	 * Returns the length of {@code b[0, len)} without a trailing incomplete UTF-8 sequence.
	 *
	 * @param b The bytes.
	 * @param len The length to consider.
	 * @return The trimmed length.
	 */
	static int completeUtf8(byte[] b, int len) {
		var i = len - 1;
		var cont = 0;
		while (i >= 0 && cont < 3 && (b[i] & 0xC0) == 0x80) {
			i--;
			cont++;
		}
		if (i < 0)
			return len;
		var lead = b[i] & 0xFF;
		var need = lead >= 0xF0 ? 4 : lead >= 0xE0 ? 3 : lead >= 0xC0 ? 2 : 1;
		return need > cont + 1 ? i : len;
	}

	/**
	 * Decodes one line's bytes: UTF-8 with replacement, BOM stripped on line 1, one trailing {@code \r} stripped, and
	 * a cut at the last complete UTF-8 sequence at or below {@link #MAX_LINE_BYTES}.
	 *
	 * @param b The line bytes, without the {@code \n}; only the first {@code MAX_LINE_BYTES + 1} are used.
	 * @param total The full byte length of the line.
	 * @param first Whether this is line 1.
	 * @return The decoded text.
	 */
	static String decode(byte[] b, long total, boolean first) {
		var len = (int)Math.min(b.length, total);
		var from = first && len >= 3 && (b[0] & 0xFF) == 0xEF && (b[1] & 0xFF) == 0xBB && (b[2] & 0xFF) == 0xBF ? 3 : 0;
		var cut = total > MAX_LINE_BYTES;
		if (cut) {
			var c = MAX_LINE_BYTES;
			while (c > from && (b[c] & 0xC0) == 0x80)
				c--;
			len = c;
		} else if (len > from && b[len - 1] == '\r') {
			len--;
		}
		var s = new String(b, from, len - from, UTF_8);
		return cut ? s + "… [truncated, line was " + total + " bytes]" : s;
	}

	/**
	 * Reads the unterminated bytes after the last indexed line of a snapshot.
	 *
	 * @return The open line's raw form, or <jk>null</jk> when there are no complete characters after the last {@code \n}.
	 */
	private Raw openRaw(FileLineIndex.Snapshot s) {
		var from = s.indexedBytes();
		var total = s.fileSize() - from;
		if (total <= 0)
			return null;
		var b = new byte[(int)Math.min(total, MAX_LINE_BYTES + 1L)];
		var len = 0;
		try (var ch = FileChannel.open(file, StandardOpenOption.READ)) {
			var buf = ByteBuffer.wrap(b);
			while (buf.hasRemaining()) {
				var r = ch.read(buf, from + len);
				if (r <= 0)
					break;
				len += r;
			}
		} catch (NoSuchFileException e) {
			return null;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		for (var i = 0; i < len; i++) {
			if (b[i] == '\n') {
				len = i;
				total = i;
				break;
			}
		}
		if (total <= MAX_LINE_BYTES) {
			len = completeUtf8(b, len);
			total = len;
		}
		if (len == 0)
			return null;
		var bytes = total <= MAX_LINE_BYTES ? Arrays.copyOf(b, len) : b;
		return new Raw(from, s.indexedLines() + 1, s.fileSize(), decode(bytes, total, s.indexedLines() == 0));
	}

	private Status status() {
		var st = status.get();
		return st == null ? Status.RUNNING : st;
	}

	private FileLineIndex.Snapshot refresh() {
		try {
			return index.refresh();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private long seek(long n) {
		try {
			return index.seek(n);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private Pos position(String token, FileLineIndex.Snapshot s, Status st) {
		var m = token == null ? null : TOKEN.matcher(token);
		if (m == null || ! m.matches() || ! index.acceptsEpoch(m.group(1)))
			throw new UnknownTokenException(token);
		var offset = Long.parseLong(m.group(2));
		var n = Long.parseLong(m.group(3));
		if (st.terminal() && offset == s.fileSize() && offset > s.indexedBytes() && n == s.indexedLines() + 2)
			return new Pos(offset, n);
		if (offset > s.indexedBytes() || seek(n) != offset)
			throw new UnknownTokenException(token);
		return new Pos(offset, n);
	}

	private static boolean isEnd(Pos p, FileLineIndex.Snapshot s) {
		return p.offset() > s.indexedBytes();
	}

	private static String token(FileLineIndex.Snapshot s, Pos p) {
		return s.epoch() + "." + p.offset() + "." + p.n();
	}

	private String first(FileLineIndex.Snapshot s, Deque<Kept> kept, Pos fallback) {
		var p = kept.isEmpty() ? fallback : new Pos(kept.peekFirst().raw().offset(), kept.peekFirst().raw().n());
		return p.n() > 1 ? token(s, p) : null;
	}

	/** Reads [start, endOffset) keeping the newest lines that fit the page budget, then the open line of {@code open}. */
	private Deque<Kept> newest(Pos start, long endOffset, FileLineIndex.Snapshot open) {
		var kept = new ArrayDeque<Kept>();
		try (var r = new LineReader(file, start, endOffset)) {
			var dec = new AnsiDecoder();
			var chars = 0L;
			Raw raw;
			while ((raw = r.next()) != null)
				chars = keep(kept, chars, raw, convert(raw, dec));
			if (open != null && (raw = openRaw(open)) != null)
				keep(kept, chars, raw, convert(raw, dec).open(true));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return kept;
	}

	private static long keep(Deque<Kept> kept, long chars, Raw raw, ConsoleOutputLine line) {
		var c = ConsoleOutputPage.visibleChars(line);
		kept.addLast(new Kept(raw, line, c));
		chars += c;
		while (kept.size() > 1 && chars > ConsoleOutputPage.MAX_PAGE_CHARS)
			chars -= kept.removeFirst().chars();
		return chars;
	}

	private static List<ConsoleOutputLine> lines(Deque<Kept> kept) {
		var l = new ArrayList<ConsoleOutputLine>(kept.size());
		for (var k : kept)
			l.add(k.line());
		return l;
	}

	private ConsoleOutputLine convert(Raw raw, AnsiDecoder dec) {
		var line = ansi ? dec.line(Level.INFO, raw.text()) : ConsoleOutputLine.info(ConsoleOutputLine.afterLastCr(raw.text()));
		line.n(raw.n());
		if (decorate != null) {
			line = decorate.apply(raw.text(), line);
			if (line == null)
				throw new IllegalStateException("FileConsoleOutputSource decorator returned null for line " + raw.n() + ".");
			if (line.n == null || line.n != raw.n())
				throw new IllegalStateException("FileConsoleOutputSource decorator changed n of line " + raw.n() + ".");
		}
		try {
			return line.validate();
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException("FileConsoleOutputSource decorator produced an invalid line " + raw.n() + ": " + e.getMessage(), e);
		}
	}

	private static ConsoleOutputPage status(ConsoleOutputPage p, Status st, boolean reachesEnd) {
		var terminal = st.terminal() && reachesEnd && p.kind == ConsoleOutputPage.Kind.FORWARD;
		return p.status(st.state(), st.stateStyle(), terminal, st.startedAt(), terminal ? st.durationMs() : null);
	}

	/** Sequential reader of lines in [start, end); opens the file lazily and only when there is something to read. */
	private static final class LineReader implements Closeable {
		private final Path file;
		private final long end;
		private final ByteBuffer buf = ByteBuffer.allocate(FileLineIndex.BLOCK_BYTES);
		private FileChannel ch;
		private long pos;
		private long n;
		private boolean eof;

		LineReader(Path file, Pos start, long end) {
			this.file = file;
			this.end = end;
			pos = start.offset();
			n = start.n();
			buf.limit(0);
		}

		boolean hasMore() {
			return ! eof && pos < end;
		}

		Raw next() throws IOException {
			if (! hasMore())
				return null;
			if (ch == null) {
				try {
					ch = FileChannel.open(file, StandardOpenOption.READ);
				} catch (NoSuchFileException e) {
					eof = true;
					return null;
				}
				OPEN_READERS.incrementAndGet();
			}
			var lineStart = pos;
			var acc = new ByteArrayOutputStream();
			var total = 0L;
			while (pos < end) {
				if (! buf.hasRemaining()) {
					buf.clear();
					if (end - pos < buf.capacity())
						buf.limit((int)(end - pos));
					var r = ch.read(buf, pos);
					buf.flip();
					if (r <= 0) {
						eof = true;
						return null;
					}
				}
				var b = buf.get();
				pos++;
				if (b == '\n')
					break;
				total++;
				if (acc.size() <= MAX_LINE_BYTES)
					acc.write(b);
			}
			var lineN = n++;
			return new Raw(lineStart, lineN, pos, decode(acc.toByteArray(), total, lineN == 1));
		}

		@Override
		public void close() {
			if (ch == null)
				return;
			try {
				ch.close();
			} catch (IOException e) {
				// Read-only channel; nothing to recover.
			} finally {
				ch = null;
				OPEN_READERS.decrementAndGet();
			}
		}
	}
}
