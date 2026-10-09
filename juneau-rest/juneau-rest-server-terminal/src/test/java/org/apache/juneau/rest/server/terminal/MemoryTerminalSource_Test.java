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
import static org.apache.juneau.BasicTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class MemoryTerminalSource_Test extends TestBase {

	private static void write(MemoryTerminalSource s, String text) {
		var b = text.getBytes(UTF_8);
		s.write(b, 0, b.length);
	}

	private static String text(TerminalChunk c) {
		return new String(c.bytes(), UTF_8);
	}

	@Test void a01_readsWithinCapacity() {
		var s = MemoryTerminalSource.create(16, 80, 24);
		write(s, "hello ");
		write(s, "world");
		var c = s.read(0, 100);
		assertEquals("hello world", text(c));
		assertEquals(11, c.next());
		assertEquals(11, c.end());
		assertFalse(c.truncated());
		assertEquals(80, c.cols());
		assertEquals(24, c.rows());
		assertEquals("world", text(s.read(6, 100)));
	}

	@Test void a02_ringWrapsAndTruncates() {
		var s = MemoryTerminalSource.create(8, 80, 24);
		write(s, "0123456789");
		write(s, "ABC");
		var c = s.read(0, 100);
		assertTrue(c.truncated());
		assertEquals("56789ABC", text(c));
		assertEquals(13, c.next());
		var tail = s.read(9, 3);
		assertFalse(tail.truncated());
		assertEquals("9AB", text(tail));
		assertEquals(12, tail.next());
	}

	@Test void a03_writeLongerThanCapacityKeepsTheLastBytes() {
		var s = MemoryTerminalSource.create(4, 80, 24);
		write(s, "abcdefghij");
		var c = s.read(0, 100);
		assertTrue(c.truncated());
		assertEquals("ghij", text(c));
		assertEquals(10, c.end());
	}

	@Test void a04_pastTheEndAndDone() {
		var s = MemoryTerminalSource.create(8, 80, 24);
		write(s, "abc");
		var past = s.read(9, 10);
		assertEquals(0, past.bytes().length);
		assertEquals(3, past.end());
		assertFalse(s.read(3, 10).done());
		s.finish();
		assertFalse(s.read(0, 1).done());
		assertTrue(s.read(3, 10).done());
		assertThrows(IllegalStateException.class, () -> write(s, "x"));
	}

	@Test void a05_badArguments() {
		assertThrowsWithMessage(IllegalArgumentException.class, "capacity", () -> MemoryTerminalSource.create(0, 80, 24));
		assertThrowsWithMessage(IllegalArgumentException.class, "size", () -> MemoryTerminalSource.create(8, 0, 24));
		assertThrowsWithMessage(IllegalArgumentException.class, "size", () -> MemoryTerminalSource.create(8, 80, 10000));
		assertThrowsWithMessage(IllegalArgumentException.class, "fromOffset", () -> MemoryTerminalSource.create(8, 80, 24).read(-1, 1));
	}

	@Test void a07_writeChecksItsBoundsBeforeChangingAnything() {
		var s = MemoryTerminalSource.create(8, 80, 24);
		write(s, "abc");
		var b = new byte[4];
		assertThrows(IndexOutOfBoundsException.class, () -> s.write(b, 2, 3));
		assertThrows(IndexOutOfBoundsException.class, () -> s.write(b, -1, 1));
		assertThrows(IndexOutOfBoundsException.class, () -> s.write(b, 0, -1));
		assertEquals(3, s.read(0, 10).end());
		assertEquals("abc", text(s.read(0, 10)));
		// Longer than the ring: the oversize path would advance the end before the copy failed.
		var small = MemoryTerminalSource.create(2, 80, 24);
		write(small, "a");
		assertThrows(IndexOutOfBoundsException.class, () -> small.write(new byte[3], 1, 4));
		assertEquals(1, small.read(0, 10).end());
		assertEquals("a", text(small.read(0, 10)));
	}

	@Test void a06_concurrentWriterAndReaderSeeEveryByteInOrder() throws Exception {
		var s = MemoryTerminalSource.create(1 << 20, 80, 24);
		var total = 200_000;
		var writer = new Thread(() -> {
			var b = new byte[1];
			for (var i = 0; i < total; i++) {
				b[0] = (byte)(i % 251);
				s.write(b, 0, 1);
			}
			s.finish();
		});
		writer.start();
		var out = new ByteArrayOutputStream();
		var from = 0L;
		while (true) {
			var c = s.read(from, 4096);
			assertFalse(c.truncated());
			out.write(c.bytes());
			from = c.next();
			if (c.done())
				break;
		}
		writer.join();
		var got = out.toByteArray();
		assertEquals(total, got.length);
		for (var i = 0; i < total; i++)
			assertEquals((byte)(i % 251), got[i], "byte " + i);
	}
}
