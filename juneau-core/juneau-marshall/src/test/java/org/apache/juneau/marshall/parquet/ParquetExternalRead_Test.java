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

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.bean.*;
import org.junit.jupiter.api.*;

/**
 * Cluster A external-read interop tests (GAP-1/2/3 + Snappy): multi-page column chunks, multi-row-group
 * files, dictionary-encoded pages, and Snappy-compressed pages.
 *
 * <p>
 * Multi-page / multi-row-group are exercised by round-tripping through Juneau's own serializer with a
 * small {@code pageSize} / {@code rowGroupSize} (the write knobs now split accordingly, and the page-loop
 * / row-group-loop reader reassembles).  Snappy block decompression is verified directly against
 * hand-crafted spec-compliant byte vectors (the project has no Snappy encoder).
 */
@SuppressWarnings({
	"unchecked" // Parser returns raw types; explicit casts required for typed assertions
})
class ParquetExternalRead_Test extends TestBase {

	@BeanType(properties = "id,name")
	public static class Rec {
		public int id;
		public String name;

		public Rec() {}
		public Rec(int id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	private static List<Rec> sample(int n) {
		var list = new ArrayList<Rec>(n);
		for (var i = 0; i < n; i++)
			list.add(new Rec(i, "name-" + i));
		return list;
	}

	// =================================================================================
	// A. Multi-page column chunks (GAP-1) — small pageSize forces several data pages.
	// =================================================================================

	@Test
	void a01_multiPageRoundTrip() throws Exception {
		var in = sample(200);
		var ser = ParquetSerializer.create().pageSize(1024).build();
		var bytes = ser.write(in);
		var out = (List<Rec>) ParquetParser.DEFAULT.read(bytes, List.class, Rec.class);
		assertEquals(200, out.size());
		assertEquals(0, out.get(0).id);
		assertEquals("name-0", out.get(0).name);
		assertEquals(199, out.get(199).id);
		assertEquals("name-199", out.get(199).name);
	}

	// =================================================================================
	// B. Multi-row-group files (GAP-2) — small rowGroupSize forces several row groups.
	// =================================================================================

	@Test
	void b01_multiRowGroupRoundTrip() throws Exception {
		var in = sample(300);
		var ser = ParquetSerializer.create().rowGroupSize(2048).build();
		var bytes = ser.write(in);
		var out = (List<Rec>) ParquetParser.DEFAULT.read(bytes, List.class, Rec.class);
		assertEquals(300, out.size());
		for (var i = 0; i < 300; i++) {
			assertEquals(i, out.get(i).id);
			assertEquals("name-" + i, out.get(i).name);
		}
	}

	@Test
	void b02_multiPageAndMultiRowGroup() throws Exception {
		var in = sample(500);
		var ser = ParquetSerializer.create().pageSize(1024).rowGroupSize(4096).build();
		var bytes = ser.write(in);
		var out = (List<Rec>) ParquetParser.DEFAULT.read(bytes, List.class, Rec.class);
		assertEquals(500, out.size());
		assertEquals(250, out.get(250).id);
		assertEquals("name-499", out.get(499).name);
	}

	// =================================================================================
	// C. Snappy block decompression — hand-crafted spec-compliant vectors.
	// =================================================================================

	@Test
	void c01_snappyLiteralOnlyBlock() throws Exception {
		// "hello world" (11 bytes) as a Snappy block: varint length 0x0B, then a literal element.
		// Literal tag: (len-1)<<2 | 0b00 = (10)<<2 = 0x28, followed by the 11 raw bytes.
		var payload = "hello world".getBytes(java.nio.charset.StandardCharsets.UTF_8);
		var block = new byte[1 + 1 + payload.length];
		block[0] = (byte) payload.length; // varint: 11 fits in one byte
		block[1] = (byte) ((payload.length - 1) << 2); // literal tag, type bits 00
		System.arraycopy(payload, 0, block, 2, payload.length);

		var decoded = CompressionCodec.SNAPPY.decompress(block, payload.length);
		assertArrayEquals(payload, decoded);
	}

	@Test
	void c02_snappyCopyBackReference() throws Exception {
		// Build "abcabcabc" (9 bytes) using a 3-byte literal "abc" then a copy(len=6, offset=3) back-reference.
		// This exercises the overlapping-copy path (offset < length must replicate).
		var expected = "abcabcabc".getBytes(java.nio.charset.StandardCharsets.UTF_8);
		var block = new byte[]{
			0x09,                       // varint uncompressed length = 9
			(byte) ((3 - 1) << 2),      // literal tag, len-1=2 -> 0x08
			'a', 'b', 'c',              // 3 literal bytes
			// copy with 1-byte offset: tag bits 01; (len-4)=2 in bits 2..4; offset high bits=0; offset low byte=3
			(byte) (0x01 | ((6 - 4) << 2)), // = 0x09
			0x03                        // offset = 3
		};
		var decoded = CompressionCodec.SNAPPY.decompress(block, expected.length);
		assertArrayEquals(expected, decoded);
	}

	@Test
	void c03_snappyLengthMismatchThrows() {
		var block = new byte[]{0x01, 0x00}; // declares 1 byte, literal of 1 byte
		var ex = assertThrows(java.io.IOException.class, () -> CompressionCodec.SNAPPY.decompress(block, 99));
		assertTrue(ex.getMessage().contains("mismatch"), "Expected length-mismatch message: " + ex.getMessage());
	}

	@Test
	void c05_snappyMultiByteLiteralLength() throws Exception {
		// A literal of 100 bytes: tag literal-len-1 = 99 < 60? no -> 99 needs the extended form.
		// Actually 100 bytes -> len-1=99 >= 60, so use the 1-extra-byte form: tag = (60)<<2 = 0xF0, then 0x63 (99).
		var payload = new byte[100];
		for (var i = 0; i < 100; i++)
			payload[i] = (byte) ('A' + (i % 26));
		var block = new byte[2 + 1 + payload.length];
		block[0] = 100; // varint length = 100 (fits one byte)
		block[1] = (byte) (60 << 2); // literal tag, extended 1-byte length follows
		block[2] = 99;               // len-1 = 99
		System.arraycopy(payload, 0, block, 3, payload.length);
		var decoded = CompressionCodec.SNAPPY.decompress(block, 100);
		assertArrayEquals(payload, decoded);
	}

	@Test
	void c06_snappyTwoByteOffsetCopy() throws Exception {
		// 60-byte literal (len-1=59, still the inline literal form) then a 2-byte-offset copy(len=10, offset=60).
		var lit = new byte[60];
		for (var i = 0; i < 60; i++)
			lit[i] = (byte) i;
		var expected = new byte[70];
		System.arraycopy(lit, 0, expected, 0, 60);
		System.arraycopy(lit, 0, expected, 60, 10);
		var block = new byte[1 + 1 + 60 + 3];
		var p = 0;
		block[p++] = 70;                       // varint length
		block[p++] = (byte) ((60 - 1) << 2);   // literal tag len-1=59 (inline) -> 0xEC
		System.arraycopy(lit, 0, block, p, 60);
		p += 60;
		block[p++] = (byte) (0x02 | ((10 - 1) << 2)); // 2-byte-offset copy tag; len-1=9
		block[p++] = 60;                       // offset low byte
		block[p] = 0;                          // offset high byte (offset = 60)
		var decoded = CompressionCodec.SNAPPY.decompress(block, 70);
		assertArrayEquals(expected, decoded);
	}

	@Test
	void c07_snappyInvalidCopyOffsetThrows() {
		// copy with offset 5 before anything was emitted -> invalid.
		var block = new byte[]{0x05, (byte) (0x02 | (1 << 2)), 0x05, 0x00};
		var ex = assertThrows(java.io.IOException.class, () -> CompressionCodec.SNAPPY.decompress(block, 5));
		assertTrue(ex.getMessage().contains("offset") || ex.getMessage().contains("overruns"),
			"Expected copy-offset error: " + ex.getMessage());
	}

	@Test
	void c08_snappyTruncatedThrows() {
		// Declares 50 bytes but provides no element data.
		var block = new byte[]{50};
		var ex = assertThrows(java.io.IOException.class, () -> CompressionCodec.SNAPPY.decompress(block, 50));
		assertTrue(ex.getMessage().contains("Truncated"), "Expected truncated message: " + ex.getMessage());
	}

	@Test
	void c04_snappyWriteUnsupported() {
		var ex = assertThrows(java.io.IOException.class, () -> CompressionCodec.SNAPPY.compress(new byte[]{1, 2, 3}));
		assertTrue(ex.getMessage().contains("not supported"), "Expected unsupported-write message: " + ex.getMessage());
	}

	// =================================================================================
	// D. Dictionary-encoded data page decode (GAP-3) — REQUIRED column, no def levels.
	// =================================================================================

	@Test
	void d01_dictionaryIndicesMapBackToEntries() throws Exception {
		// Dictionary of 3 string entries; a data page whose RLE/bit-packed indices reference them.
		var dictionary = List.<Object>of("red", "green", "blue");
		// Indices [0,1,2,1,0,2]; bit width = ceil(log2(3)) = 2.
		int[] indices = {0, 1, 2, 1, 0, 2};
		int bitWidth = 2;
		var enc = new RleBitPackingEncoder(bitWidth);
		for (var idx : indices)
			enc.writeInt(idx);
		var idxBytes = enc.toByteArray();

		// Page body for a REQUIRED column (maxDefLevel=0): [1-byte bit width][RLE indices].
		var page = new byte[1 + idxBytes.length];
		page[0] = (byte) bitWidth;
		System.arraycopy(idxBytes, 0, page, 1, idxBytes.length);

		var values = new ArrayList<>();
		ParquetParserSession.readDictionaryEncodedPage(page, indices.length, 0, 1, dictionary, values);

		assertEquals(List.of("red", "green", "blue", "green", "red", "blue"), values);
	}

	@Test
	void d02_dictionaryIndexOutOfRangeThrows() {
		var dictionary = List.<Object>of("only");
		var enc = new RleBitPackingEncoder(2);
		enc.writeInt(3); // out of range for a 1-entry dictionary
		var idxBytes = enc.toByteArray();
		var page = new byte[1 + idxBytes.length];
		page[0] = 2;
		System.arraycopy(idxBytes, 0, page, 1, idxBytes.length);

		var values = new ArrayList<>();
		var ex = assertThrows(org.apache.juneau.marshall.parser.ParseException.class,
			() -> ParquetParserSession.readDictionaryEncodedPage(page, 1, 0, 1, dictionary, values));
		assertTrue(ex.getMessage().contains("out of range"), "Expected out-of-range message: " + ex.getMessage());
	}
}
