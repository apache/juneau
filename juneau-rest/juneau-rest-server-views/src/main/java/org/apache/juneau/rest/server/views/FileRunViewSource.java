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
import java.util.*;
import java.util.function.*;
import java.util.regex.*;

import org.apache.juneau.marshall.json.JsonSerializer;
import org.apache.juneau.marshall.marshaller.Json;

/**
 * A {@link RunViewSource} over one growing JSON Lines file, one event per line, paged through a
 * {@link FileLineIndex}.
 *
 * <p>
 * <b>Path safety.</b> The application supplies the {@link Path} when it builds the source; nothing in a request ever
 * becomes a path.  An application that derives paths from ids must resolve them against a fixed root,
 * {@code normalize()} the result, and require {@code result.startsWith(root)}.
 *
 * <p>
 * <b>Numbering.</b> {@code seq} is the 1-based line number.  A {@code seq} member in the file is ignored and
 * overwritten, so the producer writes events without one.  A line that is not valid JSON, is not a known event, or is
 * too long is skipped silently: it consumes its line number, so a gap appears in {@code seq}.
 *
 * <p>
 * <b>Tokens</b> are {@code <epoch>.<n>}: the epoch of the file generation and the last line number consumed.  A stale
 * epoch (the file was truncated, replaced or rewritten) or an {@code n} beyond the indexed lines throws
 * {@link RunViewSource.UnknownTokenException UnknownTokenException}.  This includes a token issued while the file did
 * not exist yet, once the file has been created.
 *
 * <p>
 * Only complete lines are read, so a partially written last line never appears.  Keep <b>one instance per file</b>;
 * a new instance re-indexes the file from byte 0.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	FileRunViewSource <jv>src</jv> = FileRunViewSource.<jsm>create</jsm>(<jv>root</jv>.resolve(<js>"run-42.jsonl"</js>))
 * 		.terminal(() -&gt; <jv>run</jv>.isFinished())
 * 		.build();
 * </p>
 */
public final class FileRunViewSource implements RunViewSource {

	/** Lines longer than this many bytes are skipped without being decoded. */
	private static final int MAX_LINE_BYTES = RunEvent.MAX_EVENT_CHARS * 4;

	private static final Pattern TOKEN = Pattern.compile("^([0-9a-z]{6})\\.(0|[1-9][0-9]{0,17})$");

	/**
	 * Builder for {@link FileRunViewSource}.
	 */
	public static final class Builder {
		private final Path file;
		private BooleanSupplier terminal = () -> false;
		private FileLineIndex index;

		Builder(Path file) {
			this.file = Objects.requireNonNull(file, "file");
		}

		/**
		 * Sets the supplier that reports whether the producer will write no more, called once per request before the
		 * file is read.
		 *
		 * @param value The supplier.
		 * @return This object.
		 */
		public Builder terminal(BooleanSupplier value) {
			terminal = Objects.requireNonNull(value, "terminal");
			return this;
		}

		/**
		 * Sets the index (default {@link FileLineIndex#of(Path)}), so one index can be shared across requests.
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
		public FileRunViewSource build() {
			if (index != null && ! index.file().equals(file))
				throw iaex("FileRunViewSource index is for a different file: '%s' vs '%s'.", index.file(), file);
			return new FileRunViewSource(this);
		}
	}

	private final Path file;
	private final BooleanSupplier terminal;
	private final FileLineIndex index;

	private FileRunViewSource(Builder b) {
		file = b.file;
		terminal = b.terminal;
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

	@Override /* RunViewSource */
	public RunViewPage page(String after, int max) {
		// Read the flag first: if the producer writes its last events and flips the flag after this point, the page
		// is simply not terminal yet and the next request sees both.
		var done = terminal.getAsBoolean();
		var s = refresh();
		var n = after == null ? 0L : position(after, s);
		if (n >= s.indexedLines())
			return RunViewPage.of(List.of(), token(s, n), false, done);
		var start = seek(n + 1);
		if (start < 0)
			throw new UnknownTokenException(after);
		var limit = Math.max(1, Math.min(max, RunViewPage.MAX_PAGE_EVENTS));
		var events = new ArrayList<RunEvent>();
		var last = n;
		try (var r = new LineReader(file, start, s.indexedBytes())) {
			var chars = 0L;
			var lineNo = n;
			while (events.size() < limit) {
				var text = r.next();
				if (text == null)
					break;
				lineNo++;
				var e = parse(text, lineNo);
				if (e != null) {
					var c = serializedLength(e);
					if (! events.isEmpty() && chars + c > RunViewPage.MAX_PAGE_CHARS)
						break;
					chars += c;
					events.add(e);
				}
				last = lineNo;
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		var more = last < s.indexedLines();
		return RunViewPage.of(events, token(s, last), more, done && ! more);
	}

	private static RunEvent parse(String text, long lineNo) {
		if (text.isEmpty() || text.length() > RunEvent.MAX_EVENT_CHARS)
			return null;
		try {
			return RunEvent.fromMap(Json.to(text, Map.class)).withSeq(lineNo);
		} catch (Exception e) {
			return null;
		}
	}

	private static int serializedLength(RunEvent e) {
		return JsonSerializer.DEFAULT.toString(e.toContractMap()).length();
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

	private long position(String token, FileLineIndex.Snapshot s) {
		var m = TOKEN.matcher(token);
		if (! m.matches() || ! index.acceptsEpoch(m.group(1)))
			throw new UnknownTokenException(token);
		var n = Long.parseLong(m.group(2));
		if (n > s.indexedLines())
			throw new UnknownTokenException(token);
		return n;
	}

	private static String token(FileLineIndex.Snapshot s, long n) {
		return s.epoch() + "." + n;
	}

	/** Sequential reader of the complete lines in [start, end); opens the file lazily. */
	private static final class LineReader implements Closeable {
		private final Path file;
		private final long end;
		private final ByteBuffer buf = ByteBuffer.allocate(FileLineIndex.BLOCK_BYTES);
		private FileChannel ch;
		private long pos;
		private boolean first;
		private boolean eof;

		LineReader(Path file, long start, long end) {
			this.file = file;
			this.end = end;
			pos = start;
			first = start == 0;
			buf.limit(0);
		}

		/** Returns the next line's text, {@code ""} for an unusable line (overlong or blank), or <jk>null</jk> at the end. */
		String next() throws IOException {
			if (eof || pos >= end)
				return null;
			if (ch == null) {
				try {
					ch = FileChannel.open(file, StandardOpenOption.READ);
				} catch (NoSuchFileException e) {
					eof = true;
					return null;
				}
			}
			var acc = new ByteArrayOutputStream();
			var overlong = false;
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
				if (acc.size() < MAX_LINE_BYTES)
					acc.write(b);
				else
					overlong = true;
			}
			var wasFirst = first;
			first = false;
			if (overlong)
				return "";
			var bytes = acc.toByteArray();
			var from = wasFirst && bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF ? 3 : 0;
			var len = bytes.length > from && bytes[bytes.length - 1] == '\r' ? bytes.length - 1 : bytes.length;
			return new String(bytes, from, len - from, UTF_8);
		}

		@Override /* Closeable */
		public void close() {
			if (ch == null)
				return;
			try {
				ch.close();
			} catch (IOException e) {
				// Read-only channel; nothing to recover.
			} finally {
				ch = null;
			}
		}
	}
}
