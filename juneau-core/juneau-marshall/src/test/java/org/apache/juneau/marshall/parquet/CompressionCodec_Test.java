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
package org.apache.juneau.marshall.parquet;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link CompressionCodec} compress/decompress paths.
 */
class CompressionCodec_Test extends TestBase {

	@Test
	void a01_uncompressedIsIdentity() throws Exception {
		var data = "hello".getBytes(StandardCharsets.UTF_8);
		assertSame(data, CompressionCodec.UNCOMPRESSED.compress(data));
		assertSame(data, CompressionCodec.UNCOMPRESSED.decompress(data, data.length));
	}

	@Test
	void a02_gzipRoundTrip() throws Exception {
		var data = "the quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.UTF_8);
		var compressed = CompressionCodec.GZIP.compress(data);
		assertNotEquals(0, compressed.length);
		var decompressed = CompressionCodec.GZIP.decompress(compressed, data.length);
		assertArrayEquals(data, decompressed);
	}

	@Test
	void a03_gzipEmpty() throws Exception {
		var data = new byte[0];
		var compressed = CompressionCodec.GZIP.compress(data);
		assertArrayEquals(data, CompressionCodec.GZIP.decompress(compressed, 0));
	}

	@Test
	void a04_gzipDeclaredSizeLargerThanActualThrows() throws Exception {
		// Declaring more bytes than the stream yields triggers the "fewer bytes than expected" guard.
		var data = "abc".getBytes(StandardCharsets.UTF_8);
		var compressed = CompressionCodec.GZIP.compress(data);
		var ex = assertThrows(IOException.class, () -> CompressionCodec.GZIP.decompress(compressed, 100));
		assertTrue(ex.getMessage().contains("fewer bytes"), "Expected fewer-bytes message: " + ex.getMessage());
	}

	@Test
	void a05_snappyResolvesGzipAndUncompressedResolve() throws Exception {
		assertEquals(CompressionCodec.UNCOMPRESSED, CompressionCodec.fromThrift(0));
		assertEquals(CompressionCodec.SNAPPY, CompressionCodec.fromThrift(1));
		assertEquals(CompressionCodec.GZIP, CompressionCodec.fromThrift(2));
	}

	@Test
	void a06_uncompressedCompressIsIdentityForArbitraryBytes() throws Exception {
		var data = new byte[]{0, 1, 2, 3};
		assertSame(data, CompressionCodec.UNCOMPRESSED.compress(data));
	}
}
