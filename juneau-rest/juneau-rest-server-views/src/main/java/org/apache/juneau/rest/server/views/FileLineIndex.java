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
import java.nio.file.attribute.*;
import java.util.*;
import java.util.concurrent.locks.*;
import java.util.zip.*;

/**
 * A sparse, bounded index from 1-based line numbers to byte offsets in one UTF-8 text file that may keep growing
 * while it is read.
 *
 * <p>
 * The index keeps one offset per {@code stride} lines. When {@code maxEntries} offsets are held, the stride doubles
 * and every other entry is dropped, so memory stays at most {@code maxEntries} longs whatever the file size. A
 * {@link #seek(long)} scans at most {@code stride - 1} lines from the nearest entry. {@link #refresh()} reads only
 * bytes appended since the previous call.
 *
 * <p>
 * The index resets (new {@link Snapshot#epoch() epoch}) when the file key changes (replaced or rotated by rename),
 * when the file shrinks below the indexed size (truncated), or when its first bytes change (rewritten in place).
 * A copy-truncate that regrows past the old indexed size with byte-identical first 64 bytes before the next
 * {@code refresh()} cannot be detected; rotate logs by rename instead.
 *
 * <p>
 * Thread-safe. Keep one instance per log file and share it across requests; otherwise every request re-scans the
 * file from byte 0.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	FileLineIndex <jv>index</jv> = FileLineIndex.<jsm>of</jsm>(<jv>logFile</jv>);
 * 	FileLineIndex.Snapshot <jv>s</jv> = <jv>index</jv>.refresh();
 * 	<jk>long</jk> <jv>offset</jv> = <jv>index</jv>.seek(<jv>s</jv>.indexedLines());
 * </p>
 *
 * @since 10.0.0
 */
public final class FileLineIndex {

	/** Default lines per index entry. */
	public static final int DEFAULT_STRIDE = 1000;

	/** Default maximum number of index entries. */
	public static final int DEFAULT_MAX_ENTRIES = 4096;

	/** Size of the head fingerprint in bytes. */
	public static final int FINGERPRINT_BYTES = 64;

	static final int BLOCK_BYTES = 65_536;

	private static final long EPOCH_SPACE = 2_176_782_336L; // 36^6
	private static final byte[] EMPTY = {};

	/**
	 * The result of a {@link #refresh()}.
	 *
	 * @param epoch Six base36 characters identifying this file generation.
	 * @param indexedBytes Offset just past the last {@code \n}.
	 * @param indexedLines Number of complete lines in {@code [0, indexedBytes)}.
	 * @param fileSize File size at the time of the refresh.
	 */
	public record Snapshot(String epoch, long indexedBytes, long indexedLines, long fileSize) {}

	/**
	 * Builder for {@link FileLineIndex}.
	 */
	public static final class Builder {
		private final Path file;
		private int stride = DEFAULT_STRIDE;
		private int maxEntries = DEFAULT_MAX_ENTRIES;

		Builder(Path file) {
			this.file = Objects.requireNonNull(file, "file");
		}

		/**
		 * Sets the initial lines per index entry.
		 *
		 * @param value At least 1.
		 * @return This object.
		 */
		public Builder stride(int value) {
			if (value < 1)
				throw iaex("FileLineIndex stride must be >= 1; got %s.", value);
			stride = value;
			return this;
		}

		/**
		 * Sets the maximum number of index entries.
		 *
		 * @param value At least 2.
		 * @return This object.
		 */
		public Builder maxEntries(int value) {
			if (value < 2)
				throw iaex("FileLineIndex maxEntries must be >= 2; got %s.", value);
			maxEntries = value;
			return this;
		}

		/**
		 * Builds the index. No I/O happens until {@link FileLineIndex#refresh()}.
		 *
		 * @return A new index.
		 */
		public FileLineIndex build() {
			return new FileLineIndex(this);
		}
	}

	private final Path file;
	private final int initialStride;
	private final int maxEntries;
	private final ReentrantLock lock = new ReentrantLock();
	private volatile Snapshot snapshot;
	private volatile String earlyEpoch;

	// Guarded by lock.
	private Object fileKey;
	private byte[] head = EMPTY;
	private long indexedBytes;
	private long indexedLines;
	private long scannedBytes;
	private long[] entries;
	private int count;
	private int stride;

	private FileLineIndex(Builder b) {
		file = b.file;
		initialStride = b.stride;
		maxEntries = b.maxEntries;
		reset(null);
		snapshot = new Snapshot(epoch(null, EMPTY), 0, 0, 0);
	}

	/**
	 * Creates an index with the default stride and entry cap.
	 *
	 * @param file The file. Supplied by the application, never taken from a request.
	 * @return A new index.
	 */
	public static FileLineIndex of(Path file) {
		return create(file).build();
	}

	/**
	 * Creates a builder.
	 *
	 * @param file The file. Supplied by the application, never taken from a request.
	 * @return A new builder.
	 */
	public static Builder create(Path file) {
		return new Builder(file);
	}

	/**
	 * The indexed file.
	 *
	 * @return The path given to the builder.
	 */
	public Path file() {
		return file;
	}

	/**
	 * The result of the last {@link #refresh()}, without I/O.
	 *
	 * @return The snapshot.
	 */
	public Snapshot snapshot() {
		return snapshot;
	}

	/**
	 * Stats the file and indexes only the bytes appended since the previous call.
	 *
	 * @return The new snapshot. A missing file gives an empty snapshot.
	 * @throws IOException If reading fails.
	 */
	public Snapshot refresh() throws IOException {
		lock.lock();
		try {
			BasicFileAttributes attrs;
			try {
				attrs = Files.readAttributes(file, BasicFileAttributes.class);
			} catch (NoSuchFileException e) {
				return missing();
			}
			var key = attrs.fileKey();
			var size = attrs.size();
			try (var ch = FileChannel.open(file, StandardOpenOption.READ)) {
				if (! Objects.equals(key, fileKey) || size < indexedBytes || headChanged(ch, size))
					reset(key);
				readHead(ch, size);
				scan(ch, size);
			} catch (NoSuchFileException e) {
				return missing();
			}
			return publish(size);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Returns the byte offset of the start of line {@code n}.
	 *
	 * @param n The 1-based line number.
	 * @return The offset; {@code indexedBytes} for {@code n = indexedLines + 1}; {@code -1} outside
	 * 	{@code [1, indexedLines + 1]} or when the file no longer has that line.
	 * @throws IOException If reading fails.
	 */
	public long seek(long n) throws IOException {
		lock.lock();
		try {
			if (n < 1 || n > indexedLines + 1)
				return -1;
			var i = (int)Math.min((n - 1) / stride, count - 1L);
			var line = 1 + (long)i * stride;
			var pos = entries[i];
			if (line == n)
				return pos;
			try (var ch = FileChannel.open(file, StandardOpenOption.READ)) {
				var buf = ByteBuffer.allocate(BLOCK_BYTES);
				while (pos < indexedBytes) {
					buf.clear();
					if (indexedBytes - pos < BLOCK_BYTES)
						buf.limit((int)(indexedBytes - pos));
					var r = ch.read(buf, pos);
					if (r <= 0)
						return -1;
					for (var k = 0; k < r; k++)
						if (buf.get(k) == '\n' && ++line == n)
							return pos + k + 1;
					pos += r;
				}
			} catch (NoSuchFileException e) {
				return -1;
			}
			return -1;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Whether a token epoch belongs to the current file generation.
	 *
	 * <p>
	 * Accepts the current epoch and, when this instance published one, the generation's epoch from before the
	 * 64-byte head fingerprint existed.
	 *
	 * @param epoch The epoch from a token.
	 * @return <jk>true</jk> if tokens with this epoch are still valid.
	 */
	boolean acceptsEpoch(String epoch) {
		return epoch != null && (epoch.equals(snapshot.epoch()) || epoch.equals(earlyEpoch));
	}

	/** Current stride (grows when the entry cap is reached). */
	int stride() {
		lock.lock();
		try {
			return stride;
		} finally {
			lock.unlock();
		}
	}

	/** Current number of index entries. */
	int entryCount() {
		lock.lock();
		try {
			return count;
		} finally {
			lock.unlock();
		}
	}

	private Snapshot missing() {
		if (fileKey != null || head.length > 0 || indexedBytes > 0 || scannedBytes > 0)
			reset(null);
		return publish(0);
	}

	private void reset(Object key) {
		fileKey = key;
		head = EMPTY;
		earlyEpoch = null;
		indexedBytes = 0;
		indexedLines = 0;
		scannedBytes = 0;
		stride = initialStride;
		entries = new long[Math.min(16, maxEntries)];
		entries[0] = 0;
		count = 1;
	}

	private boolean headChanged(FileChannel ch, long size) throws IOException {
		if (head.length == 0)
			return false;
		if (size < head.length)
			return true;
		return ! Arrays.equals(head, read(ch, head.length));
	}

	private void readHead(FileChannel ch, long size) throws IOException {
		if (head.length >= FINGERPRINT_BYTES || size <= head.length)
			return;
		head = read(ch, (int)Math.min(FINGERPRINT_BYTES, size));
	}

	private static byte[] read(FileChannel ch, int len) throws IOException {
		var buf = ByteBuffer.allocate(len);
		while (buf.hasRemaining()) {
			if (ch.read(buf, buf.position()) <= 0)
				break;
		}
		return Arrays.copyOf(buf.array(), buf.position());
	}

	private void scan(FileChannel ch, long size) throws IOException {
		var buf = ByteBuffer.allocate(BLOCK_BYTES);
		var pos = scannedBytes;
		while (pos < size) {
			buf.clear();
			if (size - pos < BLOCK_BYTES)
				buf.limit((int)(size - pos));
			var r = ch.read(buf, pos);
			if (r <= 0)
				break;
			for (var k = 0; k < r; k++) {
				if (buf.get(k) == '\n') {
					indexedLines++;
					indexedBytes = pos + k + 1;
					record(indexedLines + 1, indexedBytes);
				}
			}
			pos += r;
		}
		scannedBytes = pos;
	}

	private void record(long line, long offset) {
		if ((line - 1) % stride != 0)
			return;
		if (count == maxEntries) {
			var kept = 0;
			for (var i = 0; i < count; i += 2)
				entries[kept++] = entries[i];
			count = kept;
			stride *= 2;
			if ((line - 1) % stride != 0)
				return;
		}
		if (count == entries.length)
			entries = Arrays.copyOf(entries, Math.min(maxEntries, entries.length * 2));
		entries[count++] = offset;
	}

	private Snapshot publish(long size) {
		var full = head.length >= FINGERPRINT_BYTES;
		var epoch = epoch(fileKey, full ? head : EMPTY);
		if (! full)
			earlyEpoch = epoch;
		var s = new Snapshot(epoch, indexedBytes, indexedLines, size);
		snapshot = s;
		return s;
	}

	private static String epoch(Object key, byte[] fingerprint) {
		var crc = new CRC32();
		crc.update(String.valueOf(key).getBytes(UTF_8));
		crc.update(0);
		crc.update(fingerprint);
		var s = Long.toString(crc.getValue() % EPOCH_SPACE, 36);
		return "0".repeat(6 - s.length()) + s;
	}
}
