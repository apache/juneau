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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;
import java.util.logging.*;

import org.apache.juneau.marshall.marshaller.Json;

/**
 * A {@link TerminalSource} that tails one growing log file of raw terminal bytes.
 *
 * <p>
 * <b>Size.</b> The producer's cols&times;rows come from the sidecar {@code <log>.size}, which holds
 * {@code {"cols":C,"rows":R}}.  While it is missing or malformed the size is {@link #DEFAULT_COLS}&times;
 * {@link #DEFAULT_ROWS} and one warning is logged; the first valid read is kept, since the sidecar is written once.
 *
 * <p>
 * <b>Done.</b> The source is done when the {@link Builder#terminal(BooleanSupplier) terminal} supplier says so and
 * the reader has reached the end.  <b>Gone.</b> A log that existed and then disappears answers a {@code gone},
 * {@code done}, empty chunk whose {@code next} and {@code end} are the requested offset.
 *
 * <p>
 * <b>Path safety.</b> The application supplies the {@link Path}; nothing in a request ever becomes a path.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	FileTerminalSource <jv>src</jv> = FileTerminalSource.<jsm>create</jsm>(<jv>dir</jv>.resolve(<js>"run.log"</js>))
 * 		.terminal(() -&gt; <jv>run</jv>.isFinished())
 * 		.build();
 * </p>
 */
public final class FileTerminalSource implements TerminalSource {

	/** The width used while the size sidecar is missing or malformed. */
	public static final int DEFAULT_COLS = 120;

	/** The height used while the size sidecar is missing or malformed. */
	public static final int DEFAULT_ROWS = 40;

	private static final Logger LOG = Logger.getLogger(FileTerminalSource.class.getName());

	/**
	 * Builder for {@link FileTerminalSource}.
	 */
	public static final class Builder {
		private final Path file;
		private BooleanSupplier terminal = () -> false;

		Builder(Path file) {
			this.file = Objects.requireNonNull(file, "file");
		}

		/**
		 * Sets the supplier that reports whether the producer will write no more, called once per read before the
		 * file is sized.
		 *
		 * @param value The supplier.
		 * @return This object.
		 */
		public Builder terminal(BooleanSupplier value) {
			terminal = Objects.requireNonNull(value, "terminal");
			return this;
		}

		/**
		 * Builds the source.
		 *
		 * @return A new source.
		 */
		public FileTerminalSource build() {
			return new FileTerminalSource(this);
		}
	}

	private final Path file, sizeFile;
	private final BooleanSupplier terminal;
	private volatile int[] size;
	private volatile boolean seen, warned;

	private FileTerminalSource(Builder b) {
		file = b.file;
		sizeFile = file.resolveSibling(file.getFileName() + ".size");
		terminal = b.terminal;
	}

	/**
	 * Creates a builder.
	 *
	 * @param file The log file, supplied by the application.
	 * @return A new builder.
	 */
	public static Builder create(Path file) {
		return new Builder(file);
	}

	/**
	 * The log file.
	 *
	 * @return The path given to {@link #create(Path)}.
	 */
	public Path file() {
		return file;
	}

	@Override /* TerminalSource */
	public TerminalChunk read(long fromOffset, int maxBytes) throws IOException {
		if (fromOffset < 0)
			throw iaex("fromOffset must be at least 0; got '%s'.", fromOffset);
		// Read the flag first: bytes written after this point are simply not done yet, and the next read sees them.
		var done = terminal.getAsBoolean();
		var sz = size();
		long end;
		try {
			end = Files.size(file);
		} catch (NoSuchFileException e) {
			if (seen)
				return gone(fromOffset, sz);
			return new TerminalChunk(new byte[0], 0, 0, done, sz[0], sz[1], false, false);
		}
		seen = true;
		if (fromOffset >= end)
			return new TerminalChunk(new byte[0], end, end, done, sz[0], sz[1], false, false);
		var len = (int)Math.min(Math.max(0, Math.min(maxBytes, MAX_READ_BYTES)), end - fromOffset);
		var buf = ByteBuffer.allocate(len);
		try (var ch = FileChannel.open(file, StandardOpenOption.READ)) {
			while (buf.hasRemaining()) {
				if (ch.read(buf, fromOffset + buf.position()) < 0)
					break;
			}
		} catch (NoSuchFileException e) {
			return gone(fromOffset, sz);
		}
		var bytes = Arrays.copyOf(buf.array(), buf.position());
		var next = fromOffset + bytes.length;
		return new TerminalChunk(bytes, next, end, done && next == end, sz[0], sz[1], false, false);
	}

	private static TerminalChunk gone(long fromOffset, int[] sz) {
		return new TerminalChunk(new byte[0], fromOffset, fromOffset, true, sz[0], sz[1], false, true);
	}

	private int[] size() {
		var s = size;
		if (s != null)
			return s;
		s = parseSize(sizeFile);
		if (s != null) {
			size = s;
			return s;
		}
		if (! warned) {
			warned = true;
			LOG.warning(() -> "Terminal size sidecar '" + sizeFile + "' is missing or malformed; using " + DEFAULT_COLS + "x" + DEFAULT_ROWS + ".");
		}
		return new int[] { DEFAULT_COLS, DEFAULT_ROWS };
	}

	/** Returns {cols, rows} from a {"cols":C,"rows":R} file, or null when it is missing or malformed. */
	static int[] parseSize(Path p) {
		try {
			var m = Json.to(Files.readString(p), Map.class);
			if (m.get("cols") instanceof Number c && m.get("rows") instanceof Number r) {
				var cols = c.doubleValue();
				var rows = r.doubleValue();
				if (cols == Math.rint(cols) && rows == Math.rint(rows) && cols >= 1 && cols <= 9999 && rows >= 1 && rows <= 9999)
					return new int[] { (int)cols, (int)rows };
			}
			return null;
		} catch (Exception e) {
			return null;
		}
	}
}
