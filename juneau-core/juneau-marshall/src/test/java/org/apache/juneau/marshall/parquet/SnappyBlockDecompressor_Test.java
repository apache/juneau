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

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for the pure-Java Snappy block decompressor, covering every literal/copy form and guard.
 */
class SnappyBlockDecompressor_Test extends TestBase {

	private static byte[] literalBlock(byte[] payload) {
		// varint length (single byte for <128) + inline literal tag + payload.
		var b = new byte[2 + payload.length];
		b[0] = (byte) payload.length;
		b[1] = (byte) ((payload.length - 1) << 2);
		System.arraycopy(payload, 0, b, 2, payload.length);
		return b;
	}

	@Test
	void a01_expectedSizeNegativeSkipsMismatchCheck() throws Exception {
		// expectedSize < 0 bypasses the length-equality guard (line 61 false branch).
		var payload = "abc".getBytes();
		var decoded = SnappyBlockDecompressor.decompress(literalBlock(payload), -1);
		assertArrayEquals(payload, decoded);
	}

	@Test
	void a02_fourByteOffsetCopy() throws Exception {
		// 8-byte literal then a 4-byte-offset copy(len=8, offset=8) — exercises the elemType==3 branch.
		var lit = new byte[]{0, 1, 2, 3, 4, 5, 6, 7};
		var expected = new byte[16];
		System.arraycopy(lit, 0, expected, 0, 8);
		System.arraycopy(lit, 0, expected, 8, 8);
		var p = 0;
		var block = new byte[1 + 1 + 8 + 5];
		block[p++] = 16;                          // varint length
		block[p++] = (byte) ((8 - 1) << 2);       // inline literal len-1=7
		System.arraycopy(lit, 0, block, p, 8);
		p += 8;
		block[p++] = (byte) (0x03 | ((8 - 1) << 2)); // 4-byte-offset copy tag; len-1=7
		block[p++] = 8;                            // offset bytes (LE) = 8
		block[p++] = 0;
		block[p++] = 0;
		block[p] = 0;
		assertArrayEquals(expected, SnappyBlockDecompressor.decompress(block, 16));
	}

	@Test
	void a03_multiByteVarintLength() throws Exception {
		// 200-byte payload: varint length needs two bytes (0xC8 0x01).
		var payload = new byte[200];
		for (var i = 0; i < 200; i++)
			payload[i] = (byte) i;
		// literal of 200 bytes: len-1=199 >= 60 -> 1-extra-byte form... 199 fits one byte (0xC7).
		var p = 0;
		var block = new byte[2 + 2 + 200];
		block[p++] = (byte) 0xC8;  // varint 200 low 7 bits
		block[p++] = 0x01;         // varint 200 high
		block[p++] = (byte) (60 << 2); // literal tag, 1 extra length byte follows
		block[p++] = (byte) 199;   // len-1 = 199
		System.arraycopy(payload, 0, block, p, 200);
		assertArrayEquals(payload, SnappyBlockDecompressor.decompress(block, 200));
	}

	@Test
	void a04_invalidUncompressedLengthThrows() {
		// varint declares > 256MB -> hard error.  300MB ~ 0x12C00000; encode as varint.
		var big = 300 * 1024 * 1024;
		var bytes = new byte[]{
			(byte) (big & 0x7F | 0x80),
			(byte) ((big >>> 7) & 0x7F | 0x80),
			(byte) ((big >>> 14) & 0x7F | 0x80),
			(byte) ((big >>> 21) & 0x7F | 0x80),
			(byte) ((big >>> 28) & 0x7F)
		};
		var ex = assertThrows(IOException.class, () -> SnappyBlockDecompressor.decompress(bytes, -1));
		assertTrue(ex.getMessage().contains("Invalid Snappy uncompressed length"));
	}

	@Test
	void a05_truncatedLiteralLengthThrows() {
		// Literal tag declaring a 4-extra-byte length but no bytes follow.
		var block = new byte[]{50, (byte) (63 << 2)}; // 63 -> 4 extra length bytes, none present
		var ex = assertThrows(IOException.class, () -> SnappyBlockDecompressor.decompress(block, 50));
		assertTrue(ex.getMessage().contains("Truncated Snappy literal length"));
	}

	@Test
	void a06_literalOverrunsThrows() {
		// Declares 10-byte output, literal claims 10 bytes but only 2 payload bytes present.
		var block = new byte[]{10, (byte) ((10 - 1) << 2), 1, 2};
		var ex = assertThrows(IOException.class, () -> SnappyBlockDecompressor.decompress(block, 10));
		assertTrue(ex.getMessage().contains("overruns"));
	}

	@Test
	void a07_truncatedOneByteCopyThrows() {
		// Declared output 8 forces the loop to read the copy: 4-byte literal then a 1-byte-offset copy tag
		// with no offset byte following.
		var block = new byte[]{8, (byte) ((4 - 1) << 2), 1, 2, 3, 4, (byte) 0x01};
		var ex = assertThrows(IOException.class, () -> SnappyBlockDecompressor.decompress(block, 8));
		assertTrue(ex.getMessage().contains("Truncated Snappy copy"));
	}

	@Test
	void a08_truncatedTwoByteCopyThrows() {
		var block = new byte[]{8, (byte) ((4 - 1) << 2), 1, 2, 3, 4, (byte) 0x02, 5};
		var ex = assertThrows(IOException.class, () -> SnappyBlockDecompressor.decompress(block, 8));
		assertTrue(ex.getMessage().contains("Truncated Snappy copy"));
	}

	@Test
	void a09_truncatedFourByteCopyThrows() {
		var block = new byte[]{8, (byte) ((4 - 1) << 2), 1, 2, 3, 4, (byte) 0x03, 5, 6};
		var ex = assertThrows(IOException.class, () -> SnappyBlockDecompressor.decompress(block, 8));
		assertTrue(ex.getMessage().contains("Truncated Snappy copy"));
	}

	@Test
	void a10_invalidCopyOffsetThrows() {
		// 4-byte literal then a 2-byte-offset copy whose offset (99) exceeds the bytes emitted so far.
		var block = new byte[]{6, (byte) ((4 - 1) << 2), 1, 2, 3, 4, (byte) (0x02 | ((2 - 1) << 2)), 99, 0};
		var ex = assertThrows(IOException.class, () -> SnappyBlockDecompressor.decompress(block, 6));
		assertTrue(ex.getMessage().contains("offset") || ex.getMessage().contains("overruns"));
	}

	@Test
	void a11_varintTooLongThrows() {
		// Five continuation bytes with the high bit always set -> varint exceeds 32 bits.
		var block = new byte[]{(byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80};
		var ex = assertThrows(IOException.class, () -> SnappyBlockDecompressor.decompress(block, -1));
		assertTrue(ex.getMessage().contains("varint too long"));
	}

	@Test
	void a12_truncatedVarintThrows() {
		// A single continuation byte with no follow-up.
		var block = new byte[]{(byte) 0x80};
		var ex = assertThrows(IOException.class, () -> SnappyBlockDecompressor.decompress(block, -1));
		assertTrue(ex.getMessage().contains("Truncated Snappy varint"));
	}

	@Test
	void a14_negativeLiteralLengthRejected() {
		// Valid small uncompressedLen (10) so the loop runs, then a 4-extra-byte literal length of 0x7FFFFFFF
		// (LE bytes FF FF FF 7F) → (int)(v+1) = 0x80000000 (negative) → the len<0 guard rejects it.
		var block = new byte[]{10, (byte) (63 << 2), (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x7F};
		var ex = assertThrows(IOException.class, () -> SnappyBlockDecompressor.decompress(block, 10));
		assertTrue(ex.getMessage().contains("overruns"), "Expected overruns message: " + ex.getMessage());
	}

	@Test
	void a15_negativeCopyOffsetRejected() {
		// 4-byte literal then a 4-byte-offset copy whose offset bytes are 0xFFFFFFFF (negative as int).
		var block = new byte[]{6, (byte) ((4 - 1) << 2), 1, 2, 3, 4,
			(byte) (0x03 | ((2 - 1) << 2)), (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
		var ex = assertThrows(IOException.class, () -> SnappyBlockDecompressor.decompress(block, 6));
		assertTrue(ex.getMessage().contains("offset"), "Expected offset message: " + ex.getMessage());
	}

	@Test
	void a17_copyOverrunsBufferRejected() {
		// Valid offset (4, into the 4-byte literal) but a copy length that runs past the declared output size.
		// Declared uncompressedLen=5: literal fills 4, copy len would write past index 5 -> "copy overruns".
		var block = new byte[]{5, (byte) ((4 - 1) << 2), 1, 2, 3, 4,
			(byte) (0x02 | ((10 - 1) << 2)), 4, 0};  // 2-byte-offset copy, len=10, offset=4
		var ex = assertThrows(IOException.class, () -> SnappyBlockDecompressor.decompress(block, 5));
		assertTrue(ex.getMessage().contains("copy overruns"), "Expected copy-overruns message: " + ex.getMessage());
	}

	@Test
	void a16_negativeUncompressedLengthRejected() {
		// A 5-byte varint setting bit 31 yields a negative uncompressed length -> rejected.
		var block = new byte[]{(byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, 0x08};
		var ex = assertThrows(IOException.class, () -> SnappyBlockDecompressor.decompress(block, -1));
		assertTrue(ex.getMessage().contains("Invalid Snappy uncompressed length"), "Expected invalid-length message: " + ex.getMessage());
	}

	@Test
	void a18_literalExceedsDeclaredOutputLengthThrows() {
		// uncompressedLen=2 but literal len=3 (fits in input) → outPos+len > uncompressedLen
		// varint(2) = 0x02; tag = litLenMinus1=2 → (2 << 2)=0x08; payload = 3 bytes
		var block = new byte[]{0x02, 0x08, 1, 2, 3};
		var ex = assertThrows(IOException.class, () -> SnappyBlockDecompressor.decompress(block, -1));
		assertTrue(ex.getMessage().contains("overruns"), "Expected overruns message: " + ex.getMessage());
	}

	@Test
	void a13_truncatedBlockMidStream() {
		// Declares 5 bytes of output but provides no element bytes after the length.
		var block = new byte[]{5};
		var ex = assertThrows(IOException.class, () -> SnappyBlockDecompressor.decompress(block, 5));
		assertTrue(ex.getMessage().contains("Truncated Snappy block"));
	}
}
