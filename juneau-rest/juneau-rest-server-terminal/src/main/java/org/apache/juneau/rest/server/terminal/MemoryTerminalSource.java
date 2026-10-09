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

import java.util.*;

/**
 * A {@link TerminalSource} over a fixed-capacity ring buffer, for producers that run in the same process.
 *
 * <p>
 * The producer calls {@link #write(byte[], int, int)} and finally {@link #finish()}.  Once more than the capacity has
 * been written, the oldest bytes are overwritten; a reader asking for them gets a {@code truncated} chunk that starts
 * at the oldest offset still held.  All methods are thread-safe.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	MemoryTerminalSource <jv>term</jv> = MemoryTerminalSource.<jsm>create</jsm>(4 * 1024 * 1024, 120, 40);
 * 	<jv>term</jv>.write(<jv>bytes</jv>, 0, <jv>bytes</jv>.length);
 * 	<jv>term</jv>.finish();
 * </p>
 */
public final class MemoryTerminalSource implements TerminalSource {

	private final byte[] ring;
	private final int cols, rows;
	private long end;
	private boolean finished;

	private MemoryTerminalSource(int capacity, int cols, int rows) {
		ring = new byte[capacity];
		this.cols = cols;
		this.rows = rows;
	}

	/**
	 * Creates a source.
	 *
	 * @param capacity The ring size in bytes, at least 1.
	 * @param cols The producer's terminal width, 1-9999.
	 * @param rows The producer's terminal height, 1-9999.
	 * @return A new source.
	 * @throws IllegalArgumentException If a value is out of range.
	 */
	public static MemoryTerminalSource create(int capacity, int cols, int rows) {
		if (capacity < 1)
			throw iaex("MemoryTerminalSource capacity must be at least 1; got '%s'.", capacity);
		if (cols < 1 || cols > 9999 || rows < 1 || rows > 9999)
			throw iaex("MemoryTerminalSource size must be 1-9999 x 1-9999; got '%sx%s'.", cols, rows);
		return new MemoryTerminalSource(capacity, cols, rows);
	}

	/**
	 * Appends bytes.
	 *
	 * @param b The bytes.
	 * @param off The start in {@code b}.
	 * @param len The number of bytes.
	 * @throws IllegalStateException If {@link #finish()} has been called.
	 * @throws IndexOutOfBoundsException If {@code off} and {@code len} do not fit in {@code b}.
	 */
	public synchronized void write(byte[] b, int off, int len) {
		Objects.checkFromIndexSize(off, len, b.length);
		if (finished)
			throw new IllegalStateException("MemoryTerminalSource is finished.");
		if (len > ring.length) {
			off += len - ring.length;
			end += len - ring.length;
			len = ring.length;
		}
		var p = (int)(end % ring.length);
		var first = Math.min(len, ring.length - p);
		System.arraycopy(b, off, ring, p, first);
		System.arraycopy(b, off + first, ring, 0, len - first);
		end += len;
	}

	/** Marks the stream finished: readers that reach the end see {@code done}. */
	public synchronized void finish() {
		finished = true;
	}

	@Override /* TerminalSource */
	public synchronized TerminalChunk read(long fromOffset, int maxBytes) {
		if (fromOffset < 0)
			throw iaex("fromOffset must be at least 0; got '%s'.", fromOffset);
		if (fromOffset > end)
			return new TerminalChunk(new byte[0], end, end, finished, cols, rows, false, false);
		var oldest = Math.max(0, end - ring.length);
		var truncated = fromOffset < oldest;
		var start = truncated ? oldest : fromOffset;
		var len = (int)Math.min(Math.max(0, Math.min(maxBytes, MAX_READ_BYTES)), end - start);
		var out = new byte[len];
		var p = (int)(start % ring.length);
		var first = Math.min(len, ring.length - p);
		System.arraycopy(ring, p, out, 0, first);
		System.arraycopy(ring, 0, out, first, len - first);
		var next = start + len;
		return new TerminalChunk(out, next, end, finished && next == end, cols, rows, truncated, false);
	}
}
