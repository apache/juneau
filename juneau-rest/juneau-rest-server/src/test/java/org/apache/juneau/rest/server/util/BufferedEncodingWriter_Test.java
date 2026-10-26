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
package org.apache.juneau.rest.server.util;

import static org.apache.juneau.bct.BctAssertions.assertSize;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;

import org.junit.jupiter.api.*;

/**
 * Tests for {@link BufferedEncodingWriter}: output must match <c>OutputStreamWriter</c> byte for byte.
 */
class BufferedEncodingWriter_Test {

	private static byte[] reference(String text, String cs) throws IOException {
		var bos = new ByteArrayOutputStream();
		try (var w = new OutputStreamWriter(bos, cs)) {
			w.write(text);
		}
		return bos.toByteArray();
	}

	private static byte[] actual(String text, String cs, int chunk) throws IOException {
		var bos = new ByteArrayOutputStream();
		try (var w = new BufferedEncodingWriter(bos, cs)) {
			var chars = text.toCharArray();
			for (var i = 0; i < chars.length; i += chunk)
				w.write(chars, i, Math.min(chunk, chars.length - i));
		}
		return bos.toByteArray();
	}

	private static String sample(int len, long seed) {
		var r = new Random(seed);
		var sb = new StringBuilder();
		while (sb.length() < len) {
			switch (r.nextInt(4)) {
				case 0 -> sb.append((char)('a' + r.nextInt(26)));
				case 1 -> sb.append((char)(0x80 + r.nextInt(0x700)));
				case 2 -> sb.appendCodePoint(0x1F300 + r.nextInt(0x300));
				default -> sb.append((char)(0x4E00 + r.nextInt(0x100)));
			}
		}
		return sb.toString();
	}

	@Test void a01_matchesOutputStreamWriter_acrossSizesAndChunks() throws IOException {
		for (var cs : new String[]{"UTF-8", "UTF-16", "ISO-8859-1", "US-ASCII"})
			for (var len : new int[]{0, 1, 5, 255, 256, 257, 8191, 8192, 8193, 20000, 70000})
				for (var chunk : new int[]{1, 7, 300, 8192, 100000}) {
					var text = sample(len, len * 31L + chunk);
					assertArrayEquals(reference(text, cs), actual(text, cs, chunk), cs + " len=" + len + " chunk=" + chunk);
				}
	}

	@Test void a02_stringWritesMatch() throws IOException {
		var text = sample(30000, 7);
		var bos = new ByteArrayOutputStream();
		try (var w = new BufferedEncodingWriter(bos, "UTF-8")) {
			for (var i = 0; i < text.length(); i += 1000)
				w.write(text, i, Math.min(1000, text.length() - i));
			w.write('!');
		}
		assertArrayEquals(reference(text + "!", "UTF-8"), bos.toByteArray());
	}

	@Test void a03_surrogatePairSplitAcrossWritesAndFlush() throws IOException {
		var bos = new ByteArrayOutputStream();
		var pair = "😀";
		try (var w = new BufferedEncodingWriter(bos, "UTF-8")) {
			w.write(pair.charAt(0));
			w.flush();
			assertSize(0, bos);
			w.write(pair.charAt(1));
		}
		assertArrayEquals(pair.getBytes(StandardCharsets.UTF_8), bos.toByteArray());
	}

	@Test void a04_loneSurrogateReplacedOnClose() throws IOException {
		assertArrayEquals(reference("a\uD83D", "UTF-8"), actual("a\uD83D", "UTF-8", 10));
	}

	@Test void a05_flushWritesPendingBytesAndFlushesStream() throws IOException {
		var flushed = new boolean[1];
		var bos = new ByteArrayOutputStream() {
			@Override public void flush() { flushed[0] = true; }
		};
		try (var w = new BufferedEncodingWriter(bos, "UTF-8")) {
			w.write("hi");
			assertSize(0, bos);
			w.flush();
			assertEquals("hi", bos.toString(StandardCharsets.UTF_8));
			assertTrue(flushed[0]);
		}
	}

	@Test void a06_unsupportedEncoding() {
		var bos = new ByteArrayOutputStream();
		assertThrows(UnsupportedEncodingException.class, () -> new BufferedEncodingWriter(bos, "no-such-charset"));
		assertThrows(UnsupportedEncodingException.class, () -> new BufferedEncodingWriter(bos, "bad name!"));
		assertThrows(NullPointerException.class, () -> new BufferedEncodingWriter(bos, null));
	}

	@Test void a07_writeAfterCloseThrows() throws IOException {
		try (var w = new BufferedEncodingWriter(new ByteArrayOutputStream(), "UTF-8")) {
			w.close();
			w.close();
			assertThrows(IOException.class, () -> w.write("x"));
			assertThrows(IOException.class, w::flush);
		}
	}

	@Test void a08_closeClosesStream() throws IOException {
		var closed = new boolean[1];
		var bos = new ByteArrayOutputStream() {
			@Override public void close() { closed[0] = true; }
		};
		new BufferedEncodingWriter(bos, "UTF-8").close();
		assertTrue(closed[0]);
	}

	@Test void a09_badBounds() throws IOException {
		try (var w = new BufferedEncodingWriter(new ByteArrayOutputStream(), "UTF-8")) {
			assertThrows(IndexOutOfBoundsException.class, () -> w.write(new char[2], 1, 5));
			assertThrows(IndexOutOfBoundsException.class, () -> w.write("ab", -1, 1));
		}
	}

	@Test void a10_printWriterWrapper() throws IOException {
		var bos = new ByteArrayOutputStream();
		var pw = new FinishablePrintWriter(bos, "UTF-8", false);
		pw.print("café");
		pw.flush();
		assertArrayEquals("café".getBytes(StandardCharsets.UTF_8), bos.toByteArray());
		assertThrows(UnsupportedEncodingException.class, () -> new FinishablePrintWriter(bos, "bogus", false));
	}
}
