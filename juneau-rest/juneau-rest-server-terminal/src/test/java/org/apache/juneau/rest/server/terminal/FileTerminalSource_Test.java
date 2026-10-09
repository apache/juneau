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

import static java.nio.charset.StandardCharsets.*;
import static java.nio.file.StandardOpenOption.*;
import static org.apache.juneau.BasicTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.logging.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;

class FileTerminalSource_Test extends TestBase {

	@TempDir Path dir;

	private Path log;

	@BeforeEach void setup() throws Exception {
		log = dir.resolve("run.log");
		size("{\"cols\":120,\"rows\":40}");  // A valid sidecar so only the tests about a missing one log a warning.
	}

	private void append(String s) throws Exception {
		Files.write(log, s.getBytes(UTF_8), CREATE, APPEND);
	}

	private void size(String json) throws Exception {
		Files.writeString(dir.resolve("run.log.size"), json);
	}

	private static String text(TerminalChunk c) {
		return new String(c.bytes(), UTF_8);
	}

	@Test void a01_readFromStartMiddleEndAndPast() throws Exception {
		size("{\"cols\":80,\"rows\":24}");
		append("\u001b[31mred\u001b[0m\r\n");
		var s = FileTerminalSource.create(log).build();
		var c = s.read(0, 1000);
		assertEquals("\u001b[31mred\u001b[0m\r\n", text(c));
		assertEquals(14, c.next());
		assertEquals(14, c.end());
		assertEquals("red\u001b[0m\r\n", text(s.read(5, 1000)));
		assertEquals("\u001b[3", text(s.read(0, 3)));
		var atEnd = s.read(14, 1000);
		assertEquals(0, atEnd.bytes().length);
		assertEquals(14, atEnd.next());
		var past = s.read(20, 1000);
		assertEquals(0, past.bytes().length);
		assertEquals(14, past.end());
		assertEquals(80, c.cols());
		assertEquals(24, c.rows());
		assertFalse(c.truncated());
		assertFalse(c.gone());
	}

	@Test void a02_maxIsClamped() throws Exception {
		Files.write(log, new byte[TerminalSource.MAX_READ_BYTES + 10]);
		var s = FileTerminalSource.create(log).build();
		assertEquals(TerminalSource.MAX_READ_BYTES, s.read(0, Integer.MAX_VALUE).bytes().length);
		assertEquals(0, s.read(0, -5).bytes().length);
		assertEquals(10, s.read(TerminalSource.MAX_READ_BYTES, Integer.MAX_VALUE).bytes().length);
	}

	@Test void a03_doneOnlyAtTheEnd() throws Exception {
		append("abcdef");
		var finished = new AtomicBoolean();
		var s = FileTerminalSource.create(log).terminal(finished::get).build();
		assertFalse(s.read(6, 10).done());
		finished.set(true);
		assertFalse(s.read(0, 3).done());
		assertTrue(s.read(3, 10).done());
		assertTrue(s.read(6, 10).done());
	}

	@Test void a04_missingOrMalformedSidecarDefaultsAndWarnsOnce() throws Exception {
		append("x");
		Files.delete(dir.resolve("run.log.size"));
		var s = FileTerminalSource.create(log).build();
		var records = LogRecordCapture.quietly(FileTerminalSource.class, () -> {
			var c = s.read(0, 10);
			assertEquals(120, c.cols());
			assertEquals(40, c.rows());
			size("{\"cols\":0,\"rows\":24}");
			assertEquals(120, s.read(0, 10).cols());
		});
		assertEquals(1, records.size());
		assertEquals(Level.WARNING, records.get(0).getLevel());
		assertTrue(records.get(0).getMessage().contains("120x40"), records.get(0).getMessage());
		size("{\"cols\":100,\"rows\":30}");
		assertEquals(100, s.read(0, 10).cols());
		assertEquals(30, s.read(0, 10).rows());
	}

	@Test void a05_parseSize() throws Exception {
		var p = dir.resolve("s");
		for (var bad : List.of("", "[]", "{\"cols\":80}", "{\"cols\":80.5,\"rows\":24}", "{\"cols\":10000,\"rows\":24}", "{\"cols\":\"80\",\"rows\":24}")) {
			Files.writeString(p, bad);
			assertNull(FileTerminalSource.parseSize(p), bad);
		}
		Files.writeString(p, "{\"cols\":9999,\"rows\":1}");
		assertArrayEquals(new int[] { 9999, 1 }, FileTerminalSource.parseSize(p));
		assertNull(FileTerminalSource.parseSize(dir.resolve("missing")));
	}

	@Test void a06_deletedLogIsGone() throws Exception {
		append("abc");
		var s = FileTerminalSource.create(log).build();
		assertFalse(s.read(0, 10).gone());
		Files.delete(log);
		var c = s.read(3, 10);
		assertTrue(c.gone());
		assertTrue(c.done());
		assertEquals(3, c.next());
		assertEquals(3, c.end());
		assertEquals(1, s.read(1, 10).next());
	}

	@Test void a10_fileThatShrinksBelowFromIsPastTheEnd() throws Exception {
		append("abcdef");
		var s = FileTerminalSource.create(log).build();
		assertEquals("abcdef", text(s.read(0, 10)));
		Files.write(log, "abc".getBytes(UTF_8));
		var c = s.read(5, 10);
		assertEquals(0, c.bytes().length);
		assertEquals(3, c.end());
		assertEquals(3, c.next());
		assertFalse(c.gone());
		assertEquals("c", text(s.read(2, 10)));
	}

	@Test void a07_notYetCreatedIsNotGone() throws Exception {
		var s = FileTerminalSource.create(log).build();
		var c = s.read(0, 10);
		assertFalse(c.gone());
		assertFalse(c.done());
		assertEquals(0, c.end());
		append("ab");
		assertEquals("ab", text(s.read(0, 10)));
	}

	@Test void a08_badArguments() {
		var s = FileTerminalSource.create(log).build();
		assertThrowsWithMessage(IllegalArgumentException.class, "fromOffset must be at least 0", () -> s.read(-1, 10));
		assertThrows(NullPointerException.class, () -> FileTerminalSource.create(null));
	}

	@Test void a09_copyToWritesEverything() throws Exception {
		var big = new byte[TerminalSource.MAX_READ_BYTES * 2 + 7];
		Arrays.fill(big, (byte)'z');
		Files.write(log, big);
		var out = new ByteArrayOutputStream();
		FileTerminalSource.create(log).build().copyTo(out);
		assertArrayEquals(big, out.toByteArray());
	}
}
