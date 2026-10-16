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
import static org.apache.juneau.rest.server.runreport.RunViewChecks.*;

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
 * <b>Nudges.</b> {@link #nudges(String, TerminalNudgePublisher)} starts a daemon watcher that publishes when the file
 * grows.  A source has no process exit, so it never publishes a {@code done} nudge: the page's polling already sees
 * {@code done} through the {@link Builder#terminal(BooleanSupplier) terminal} supplier.  Call {@link #close()} to stop
 * the watcher.
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
public final class FileTerminalSource implements TerminalSource, AutoCloseable {

	/** The width used while the size sidecar is missing or malformed. */
	public static final int DEFAULT_COLS = 120;

	/** The height used while the size sidecar is missing or malformed. */
	public static final int DEFAULT_ROWS = 40;

	/** How often the nudge thread checks the file size, in milliseconds. */
	static final int NUDGE_POLL_MILLIS = 100;

	/** How long {@link #close()} waits for the nudge thread, in milliseconds. */
	static final int NUDGE_JOIN_MILLIS = 1000;

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

	// Nudges.  nudgeThread, nudgeSet and nudgeClosed are guarded by nudgeLock.
	private final Object nudgeLock = new Object();
	private Thread nudgeThread;
	private boolean nudgeSet, nudgeClosed;
	private volatile boolean nudgeStop;

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

	/**
	 * Reports growth of the file to a publisher.
	 *
	 * <p>
	 * Starts a daemon thread named {@code juneau-terminal-nudge-file-<name>} that checks the file size every 100 ms and
	 * calls the publisher with the new end when it has grown.  A file that is missing or has shrunk publishes nothing
	 * and the watcher keeps going.  A {@link RuntimeException} thrown by the publisher is logged at {@code FINE} and
	 * otherwise ignored.  No {@code done} nudge is ever published, since the source does not know when the producer
	 * finishes; polling covers it.  {@link #close()} stops the thread.
	 *
	 * @param terminalId The terminal id passed to the publisher; must be a valid run id.
	 * @param publisher The publisher.
	 * @return This object.
	 * @throws NullPointerException If an argument is <jk>null</jk>.
	 * @throws IllegalArgumentException If the id is not a valid run id.
	 * @throws IllegalStateException If a publisher is already set, or this source has been closed.
	 */
	public FileTerminalSource nudges(String terminalId, TerminalNudgePublisher publisher) {
		Objects.requireNonNull(terminalId, "terminalId");
		Objects.requireNonNull(publisher, "publisher");
		if (! isRunId(terminalId))
			throw iaex("FileTerminalSource nudge id is not a valid run id: '%s'.", terminalId);
		synchronized (nudgeLock) {
			if (nudgeClosed)
				throw new IllegalStateException("FileTerminalSource is closed.");
			if (nudgeSet)
				throw new IllegalStateException("FileTerminalSource nudges are already set.");
			nudgeSet = true;
			nudgeThread = new Thread(() -> watch(terminalId, publisher), "juneau-terminal-nudge-file-" + file.getFileName());
			nudgeThread.setDaemon(true);
			nudgeThread.start();
		}
		return this;
	}

	/** The nudge thread: reports file growth until the thread is stopped. */
	private void watch(String id, TerminalNudgePublisher publisher) {
		var last = -1L;
		while (! nudgeStop) {
			var size = sizeOrZero(file);
			if (size > last) {
				try {
					publisher.nudge(id, size, false);
				} catch (RuntimeException e) {
					LOG.log(Level.FINE, e, () -> "Terminal nudge publisher failed for '" + id + "'.");
				}
				last = size;
			}
			try {
				Thread.sleep(NUDGE_POLL_MILLIS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
	}

	private static long sizeOrZero(Path p) {
		try {
			return Files.size(p);
		} catch (IOException e) {
			return 0;
		}
	}

	/**
	 * Stops the nudge thread, if any, and waits up to {@value #NUDGE_JOIN_MILLIS} ms for it.
	 *
	 * <p>
	 * Reads keep working after this; only {@link #nudges(String, TerminalNudgePublisher)} is refused.
	 */
	@Override /* AutoCloseable */
	public void close() {
		Thread t;
		synchronized (nudgeLock) {
			nudgeClosed = true;
			nudgeStop = true;
			t = nudgeThread;
		}
		if (t == null)
			return;
		t.interrupt();
		try {
			t.join(NUDGE_JOIN_MILLIS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		if (t.isAlive())
			LOG.log(Level.WARNING, () -> "FileTerminalSource nudge thread did not stop for '" + file + "'.");
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
