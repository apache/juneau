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

package org.apache.juneau.commons.utils;

import static org.apache.juneau.commons.utils.IoUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;
import java.util.zip.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Tests {@link IoUtils#inflate(byte[], boolean, long, int)}: round-trip, output cap, ratio guard, and the
 * cap-disabling convention.
 */
class IoUtils_Inflate_Test extends TestBase {

	private static byte[] deflate(byte[] data, boolean nowrap) {
		var deflater = new Deflater(Deflater.BEST_COMPRESSION, nowrap);
		// Deflater isn't AutoCloseable at this module's source/target level, so route end() through
		// try-with-resources via a Closeable method reference; close() never actually throws here.
		try (Closeable end = deflater::end) {
			deflater.setInput(data);
			deflater.finish();
			var out = new ByteArrayOutputStream();
			var buf = new byte[8192];
			while (! deflater.finished()) {
				var n = deflater.deflate(buf);
				out.write(buf, 0, n);
			}
			return out.toByteArray();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Test void a01_roundTrip_rawDeflate() throws IOException {
		var original = "The quick brown fox jumps over the lazy dog.".repeat(50).getBytes(StandardCharsets.UTF_8);
		var compressed = deflate(original, true);
		var result = inflate(compressed, true, 1_000_000L, 1000);
		assertArrayEquals(original, result);
	}

	@Test void a02_roundTrip_zlibWrapped() throws IOException {
		var original = "The quick brown fox jumps over the lazy dog.".repeat(50).getBytes(StandardCharsets.UTF_8);
		var compressed = deflate(original, false);
		var result = inflate(compressed, false, 1_000_000L, 1000);
		assertArrayEquals(original, result);
	}

	@Test void a03_oversizedOutputRejected() {
		// A large-but-compressible payload whose decompressed size exceeds the absolute cap is aborted mid-inflate.
		var original = new byte[1_000_000];  // 1 MB of zeros -> tiny compressed
		var compressed = deflate(original, true);
		var e = assertThrows(IOException.class, () -> inflate(compressed, true, 1000L, 0));
		assertTrue(e.getMessage().contains("exceeds maximum allowed"));
	}

	@Test void a04_ratioBombRejected() {
		// The ratio guard catches a highly-compressible payload even when the absolute cap is disabled.
		var original = new byte[1_000_000];  // deflates to ~1 KB -> ratio ~1000:1
		var compressed = deflate(original, true);
		var e = assertThrows(IOException.class, () -> inflate(compressed, true, 0L, 100));
		assertTrue(e.getMessage().contains("ratio"));
	}

	@Test void a05_capOfZeroDisablesAbsoluteCap() throws IOException {
		// maxOutputBytes <= 0 disables the absolute cap (maxLength convention); with the ratio guard also disabled
		// the full output is returned.
		var original = new byte[1_000_000];
		var compressed = deflate(original, true);
		var result = inflate(compressed, true, 0L, 0);
		assertEquals(1_000_000, result.length);
	}

	@Test void a06_negativeCapDisablesAbsoluteCap() throws IOException {
		var original = new byte[500_000];
		var compressed = deflate(original, true);
		var result = inflate(compressed, true, -1L, 0);
		assertEquals(500_000, result.length);
	}

	@Test void a07_nullInputReturnsEmpty() throws IOException {
		assertArrayEquals(new byte[0], inflate(null, true, 1000L, 100));
	}

	@Test void a08_emptyInputReturnsEmpty() throws IOException {
		assertArrayEquals(new byte[0], inflate(new byte[0], true, 1000L, 100));
	}

	@Test void a09_invalidDataThrows() {
		// Random bytes are not a valid zlib stream.
		var garbage = new byte[]{0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8};
		assertThrows(IOException.class, () -> inflate(garbage, false, 1000L, 100));
	}

	@Test void a10_outputExactlyAtCapAllowed() throws IOException {
		// The cap is a ceiling that may be reached but not exceeded.
		var original = new byte[1000];
		var compressed = deflate(original, true);
		var result = inflate(compressed, true, 1000L, 0);
		assertEquals(1000, result.length);
	}
}
