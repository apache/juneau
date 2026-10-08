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
import static org.apache.juneau.BasicTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;

class FileLineIndex_Test extends TestBase {

	@TempDir Path dir;

	private static String line(int i) {
		return "line-" + i + "-" + "x".repeat(i % 13) + "\n";
	}

	/** Appends lines from..to (inclusive) and returns the start offset of each line, index 0 = line from. */
	private static List<Long> append(Path f, int from, int to) throws IOException {
		var offsets = new ArrayList<Long>();
		var pos = Files.exists(f) ? Files.size(f) : 0L;
		var sb = new StringBuilder();
		for (var i = from; i <= to; i++) {
			offsets.add(pos);
			var l = line(i);
			sb.append(l);
			pos += l.getBytes(UTF_8).length;
		}
		Files.writeString(f, sb, UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		return offsets;
	}

	@Test void a01_missingThenEmpty() throws Exception {
		var f = dir.resolve("log.txt");
		var idx = FileLineIndex.of(f);
		var s = idx.refresh();
		assertEquals(0, s.indexedBytes());
		assertEquals(0, s.indexedLines());
		assertEquals(0, s.fileSize());
		assertTrue(s.epoch().matches("^[0-9a-z]{6}$"), s.epoch());
		assertEquals(0, idx.seek(1));
		assertEquals(-1, idx.seek(2));
		Files.createFile(f);
		var s2 = idx.refresh();
		assertEquals(0, s2.indexedLines());
		assertSame(s2, idx.snapshot());
	}

	@Test void a02_growthAndPartialLine() throws Exception {
		var f = dir.resolve("log.txt");
		Files.writeString(f, "a\nb\n");
		var idx = FileLineIndex.of(f);
		var s = idx.refresh();
		assertEquals(4, s.indexedBytes());
		assertEquals(2, s.indexedLines());
		Files.writeString(f, "c", StandardOpenOption.APPEND);
		s = idx.refresh();
		assertEquals(4, s.indexedBytes());
		assertEquals(2, s.indexedLines());
		assertEquals(5, s.fileSize());
		Files.writeString(f, "c\n", StandardOpenOption.APPEND);
		s = idx.refresh();
		assertEquals(7, s.indexedBytes());
		assertEquals(3, s.indexedLines());
		assertEquals(4, idx.seek(3));
		assertEquals(7, idx.seek(4));
	}

	@Test void a03_seekSweep() throws Exception {
		var f = dir.resolve("log.txt");
		var offsets = append(f, 1, 100);
		var idx = FileLineIndex.create(f).stride(3).build();
		var s = idx.refresh();
		for (var n = 1; n <= 100; n++)
			assertEquals(offsets.get(n - 1).longValue(), idx.seek(n), "n=" + n);
		assertEquals(s.indexedBytes(), idx.seek(101));
	}

	@Test void a04_seekBounds() throws Exception {
		var f = dir.resolve("log.txt");
		append(f, 1, 10);
		var idx = FileLineIndex.of(f);
		var s = idx.refresh();
		assertEquals(-1, idx.seek(0));
		assertEquals(-1, idx.seek(-5));
		assertEquals(-1, idx.seek(s.indexedLines() + 2));
		assertEquals(s.indexedBytes(), idx.seek(s.indexedLines() + 1));
		assertEquals(0, idx.seek(1));
	}

	@Test void a05_strideDoubling() throws Exception {
		var f = dir.resolve("log.txt");
		var offsets = append(f, 1, 10_000);
		var idx = FileLineIndex.create(f).stride(2).maxEntries(8).build();
		var s = idx.refresh();
		assertEquals(10_000, s.indexedLines());
		assertTrue(idx.entryCount() <= 8, "entries=" + idx.entryCount());
		assertEquals(2048, idx.stride());
		for (var n = 1; n <= 10_000; n += 7)
			assertEquals(offsets.get(n - 1).longValue(), idx.seek(n), "n=" + n);
		assertEquals(offsets.get(9_999).longValue(), idx.seek(10_000));
	}

	@Test void a06_truncateResets() throws Exception {
		var f = dir.resolve("log.txt");
		append(f, 1, 50);
		var idx = FileLineIndex.of(f);
		var e1 = idx.refresh().epoch();
		Files.writeString(f, "fresh\n", StandardOpenOption.TRUNCATE_EXISTING);
		var s = idx.refresh();
		assertEquals(1, s.indexedLines());
		assertEquals(6, s.indexedBytes());
		assertNotEquals(e1, s.epoch());
		assertFalse(idx.acceptsEpoch(e1));
	}

	@Test void a07_rotateResets() throws Exception {
		var f = dir.resolve("log.txt");
		append(f, 1, 50);
		assumeTrue(Files.readAttributes(f, BasicFileAttributes.class).fileKey() != null, "filesystem has no file keys");
		var idx = FileLineIndex.of(f);
		var e1 = idx.refresh().epoch();
		Files.move(f, dir.resolve("log.txt.1"));
		append(f, 1, 50);
		var s = idx.refresh();
		assertNotEquals(e1, s.epoch());
		assertFalse(idx.acceptsEpoch(e1));
		assertEquals(50, s.indexedLines());
	}

	@Test void a08_rewriteInPlaceResets() throws Exception {
		var f = dir.resolve("log.txt");
		append(f, 1, 50);
		var size = Files.size(f);
		var idx = FileLineIndex.of(f);
		var e1 = idx.refresh().epoch();
		try (var ch = FileChannel.open(f, StandardOpenOption.WRITE)) {
			ch.write(ByteBuffer.wrap("Z".repeat(64).getBytes(UTF_8)), 0);
		}
		assertEquals(size, Files.size(f));
		var s = idx.refresh();
		assertNotEquals(e1, s.epoch());
		assertFalse(idx.acceptsEpoch(e1));
	}

	@Test void a09_epochStableAcrossInstances() throws Exception {
		var f = dir.resolve("log.txt");
		append(f, 1, 20);
		assertEquals(FileLineIndex.of(f).refresh().epoch(), FileLineIndex.of(f).refresh().epoch());
	}

	@Test void a10_earlyEpochAcceptedAfterGrowth() throws Exception {
		var f = dir.resolve("log.txt");
		Files.writeString(f, "short\n");
		var idx = FileLineIndex.of(f);
		var early = idx.refresh().epoch();
		append(f, 1, 20);
		var s = idx.refresh();
		assertNotEquals(early, s.epoch());
		assertTrue(idx.acceptsEpoch(early));
		assertTrue(idx.acceptsEpoch(s.epoch()));
		assertFalse(idx.acceptsEpoch("zzzzzz"));
		assertFalse(idx.acceptsEpoch(null));
		var fresh = FileLineIndex.of(f);
		fresh.refresh();
		assertFalse(fresh.acceptsEpoch(early));
	}

	@Test void a11_concurrentAppendAndRefresh() throws Exception {
		var f = dir.resolve("log.txt");
		Files.createFile(f);
		var idx = FileLineIndex.create(f).stride(16).build();
		var pool = Executors.newFixedThreadPool(5);
		try {
			var writer = pool.submit(() -> {
				for (var i = 1; i <= 20_000; i += 100)
					append(f, i, i + 99);
				return null;
			});
			var readers = new ArrayList<Future<?>>();
			for (var r = 0; r < 4; r++)
				readers.add(pool.submit(() -> {
					var last = 0L;
					while (last < 20_000) {
						var s = idx.refresh();
						assertTrue(s.indexedLines() >= last);
						last = s.indexedLines();
						assertEquals(s.indexedBytes(), idx.seek(last + 1), "snap=" + s);
					}
					return null;
				}));
			writer.get(60, TimeUnit.SECONDS);
			for (var fu : readers)
				fu.get(60, TimeUnit.SECONDS);
		} finally {
			pool.shutdownNow();
		}
	}

	@Test void b01_builderValidation() {
		var f = dir.resolve("x");
		assertThrowsWithMessage(IllegalArgumentException.class, "stride must be >= 1", () -> FileLineIndex.create(f).stride(0));
		assertThrowsWithMessage(IllegalArgumentException.class, "maxEntries must be >= 2", () -> FileLineIndex.create(f).maxEntries(1));
		assertThrows(NullPointerException.class, () -> FileLineIndex.of(null));
		assertEquals(f, FileLineIndex.of(f).file());
	}
}
