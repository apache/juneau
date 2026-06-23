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

import org.junit.jupiter.api.*;

/**
 * Branch-coverage tests for {@link RleBitPackingEncoder} and {@link RleBitPackingDecoder}.
 */
class RleBitPacking_Test {

	// -----------------------------------------------------------------------
	// a — encoder: constructor validation
	// -----------------------------------------------------------------------

	@Test void a01_encoder_bitWidth_1_valid() {
		assertDoesNotThrow(() -> new RleBitPackingEncoder(1));
	}

	@Test void a02_encoder_bitWidth_32_valid() {
		assertDoesNotThrow(() -> new RleBitPackingEncoder(32));
	}

	@Test void a03_encoder_bitWidth_0_throws() {
		assertThrows(IllegalArgumentException.class, () -> new RleBitPackingEncoder(0));
	}

	@Test void a04_encoder_bitWidth_33_throws() {
		assertThrows(IllegalArgumentException.class, () -> new RleBitPackingEncoder(33));
	}

	// -----------------------------------------------------------------------
	// b — encoder: RLE run (8 identical values → RLE flush)
	// -----------------------------------------------------------------------

	@Test void b01_rle_run_8_identical_values() throws IOException {
		var enc = new RleBitPackingEncoder(1);
		for (int i = 0; i < 8; i++) enc.writeInt(1);
		var bytes = enc.toByteArray();
		assertTrue(bytes.length > 0);
		// decode and verify
		var dec = new RleBitPackingDecoder(bytes, 1);
		for (int i = 0; i < 8; i++)
			assertEquals(1, dec.readInt());
	}

	@Test void b02_rle_run_roundtrip_larger_count() throws IOException {
		var enc = new RleBitPackingEncoder(2);
		for (int i = 0; i < 16; i++) enc.writeInt(3);
		var bytes = enc.toByteArray();
		var dec = new RleBitPackingDecoder(bytes, 2);
		for (int i = 0; i < 16; i++)
			assertEquals(3, dec.readInt());
	}

	// -----------------------------------------------------------------------
	// c — encoder: bit-packed run (8 distinct values)
	// -----------------------------------------------------------------------

	@Test void c01_bit_packed_8_distinct_values() throws IOException {
		var enc = new RleBitPackingEncoder(3);
		for (int i = 0; i < 8; i++) enc.writeInt(i);
		var bytes = enc.toByteArray();
		var dec = new RleBitPackingDecoder(bytes, 3);
		for (int i = 0; i < 8; i++)
			assertEquals(i, dec.readInt());
	}

	// -----------------------------------------------------------------------
	// d — encoder: flushRun with runLength < 8 (bit-packed partial run)
	// -----------------------------------------------------------------------

	@Test void d01_flushRun_small_run_then_different_value() throws IOException {
		// Write 3 identical values then a different one → flushRun with runLength=3 < 8
		var enc = new RleBitPackingEncoder(2);
		enc.writeInt(1);
		enc.writeInt(1);
		enc.writeInt(1);
		enc.writeInt(2); // different value → flushRun(runLength=3)
		var bytes = enc.toByteArray();
		var dec = new RleBitPackingDecoder(bytes, 2);
		assertEquals(1, dec.readInt());
		assertEquals(1, dec.readInt());
		assertEquals(1, dec.readInt());
		assertEquals(2, dec.readInt());
	}

	// -----------------------------------------------------------------------
	// e — encoder: toByteArray with pending runLength > 0 and bitPackedCount > 0
	// -----------------------------------------------------------------------

	@Test void e01_toByteArray_with_pending_rle() throws IOException {
		// 3 values — ends with runLength=3 > 0, bitPackedCount=0 → toByteArray flushes
		var enc = new RleBitPackingEncoder(1);
		enc.writeInt(0);
		enc.writeInt(0);
		enc.writeInt(0);
		var bytes = enc.toByteArray();
		var dec = new RleBitPackingDecoder(bytes, 1);
		assertEquals(0, dec.readInt());
		assertEquals(0, dec.readInt());
		assertEquals(0, dec.readInt());
	}

	@Test void e02_toByteArray_with_pending_bit_packed_count() throws IOException {
		// 3 distinct values → bitPackedBuffer fills with 3, runLength=0 at toByteArray
		var enc = new RleBitPackingEncoder(2);
		enc.writeInt(0);
		enc.writeInt(1);
		enc.writeInt(2);
		var bytes = enc.toByteArray();
		var dec = new RleBitPackingDecoder(bytes, 2);
		assertEquals(0, dec.readInt());
		assertEquals(1, dec.readInt());
		assertEquals(2, dec.readInt());
	}

	@Test void e03_toByteArray_empty() {
		var enc = new RleBitPackingEncoder(1);
		assertArrayEquals(new byte[0], enc.toByteArray());
	}

	// -----------------------------------------------------------------------
	// f — encoder: toByteArrayWithLength
	// -----------------------------------------------------------------------

	@Test void f01_toByteArrayWithLength_has_4_byte_prefix() {
		var enc = new RleBitPackingEncoder(1);
		enc.writeInt(1);
		var bytes = enc.toByteArrayWithLength();
		assertTrue(bytes.length >= 4);
		var payloadLen = (bytes[0] & 0xFF) | ((bytes[1] & 0xFF) << 8)
			| ((bytes[2] & 0xFF) << 16) | ((bytes[3] & 0xFF) << 24);
		assertEquals(bytes.length - 4, payloadLen);
	}

	// -----------------------------------------------------------------------
	// g — decoder: rleCount > 0 (already inside an RLE run)
	// -----------------------------------------------------------------------

	@Test void g01_decoder_rle_count_reuses_value() throws IOException {
		// Write 10 identical values → RLE run; the 2nd through 10th reads use rleCount path
		var enc = new RleBitPackingEncoder(2);
		for (int i = 0; i < 10; i++) enc.writeInt(2);
		var bytes = enc.toByteArray();
		var dec = new RleBitPackingDecoder(bytes, 2);
		for (int i = 0; i < 10; i++)
			assertEquals(2, dec.readInt());
	}

	// -----------------------------------------------------------------------
	// h — decoder: bitPackedRead < bitPackedCount (already buffered bit-packed values)
	// -----------------------------------------------------------------------

	@Test void h01_decoder_reads_from_bit_packed_buffer() throws IOException {
		// 8 distinct values → bit-packed; reads 2-8 come from buffer
		var enc = new RleBitPackingEncoder(3);
		for (int i = 0; i < 8; i++) enc.writeInt(i % 5);
		var bytes = enc.toByteArray();
		var dec = new RleBitPackingDecoder(bytes, 3);
		dec.readInt(); // loads buffer, returns bitPackedBuffer[0]
		// second read should use bitPackedRead < bitPackedCount path
		assertDoesNotThrow(dec::readInt);
	}

	// -----------------------------------------------------------------------
	// j — decoder: RLE value read returns EOF (x < 0 branch in readInt)
	// -----------------------------------------------------------------------

	@Test void j01_decoder_eof_during_rle_value_read() throws IOException {
		// Craft a raw RLE header (count=2, even header, header>>1=2) then no payload bytes
		// header varint for count=2: encoded as 2<<1 = 4, single byte 0x04
		var data = new byte[]{0x04}; // RLE header for count=2, bitWidth=1 → 1 byte payload follows
		var dec = new RleBitPackingDecoder(data, 1);
		// readInt: reads header (RLE), then tries to read 1 payload byte → EOF → x<0 branch taken
		assertEquals(0, dec.readInt()); // rleValue stays 0 since the x<0 branch skipped the set
	}

	// -----------------------------------------------------------------------
	// k — decoder: hasNext() — positive branches
	// -----------------------------------------------------------------------

	@Test void k01_hasNext_via_rleCount() throws IOException {
		// After reading first of 8 identical values, rleCount=7 > 0
		var enc = new RleBitPackingEncoder(1);
		for (int i = 0; i < 8; i++) enc.writeInt(1);
		var dec = new RleBitPackingDecoder(enc.toByteArray(), 1);
		dec.readInt();
		assertTrue(dec.hasNext()); // rleCount > 0
	}

	@Test void k02_hasNext_via_bitPackedRead() throws IOException {
		// After reading first of 8 bit-packed values, bitPackedRead=1 < bitPackedCount=8
		var enc = new RleBitPackingEncoder(2);
		for (int i = 0; i < 8; i++) enc.writeInt(i % 4);
		var dec = new RleBitPackingDecoder(enc.toByteArray(), 2);
		dec.readInt();
		assertTrue(dec.hasNext()); // bitPackedRead < bitPackedCount
	}

	@Test void k04_hasNext_via_stream_available() throws IOException {
		// Only one value written — after reading it, stream still has available bytes
		// (encoded as a full bit-packed group padded with zeros)
		var enc = new RleBitPackingEncoder(1);
		enc.writeInt(1);
		var dec = new RleBitPackingDecoder(enc.toByteArray(), 1);
		// Don't read anything yet — stream has bytes
		assertTrue(dec.hasNext()); // in.available() > 0
	}

	@Test void k05_hasNext_false_when_exhausted() throws IOException {
		var enc = new RleBitPackingEncoder(1);
		for (int i = 0; i < 8; i++) enc.writeInt(1);
		var dec = new RleBitPackingDecoder(enc.toByteArray(), 1);
		for (int i = 0; i < 8; i++) dec.readInt();
		assertFalse(dec.hasNext());
	}

	// -----------------------------------------------------------------------
	// l — decoder: offset/length constructor
	// -----------------------------------------------------------------------

	@Test void l01_offset_length_constructor() throws IOException {
		// Encode 8 ones with bitWidth=1
		var enc = new RleBitPackingEncoder(1);
		for (int i = 0; i < 8; i++) enc.writeInt(1);
		var full = enc.toByteArray();
		// prefix + suffix padding
		var data = new byte[full.length + 4];
		System.arraycopy(full, 0, data, 2, full.length);
		var dec = new RleBitPackingDecoder(data, 2, full.length, 1);
		for (int i = 0; i < 8; i++)
			assertEquals(1, dec.readInt());
	}

	// -----------------------------------------------------------------------
	// m — decoder: EOF inside bit-packed group (b < 0 branch)
	// -----------------------------------------------------------------------

	@Test void m01_decoder_eof_inside_bit_packed_group() {
		// Bit-packed header for 1 group of 8: varint = (1<<1)|1 = 3, then truncated bytes
		// bitWidth=2 → 2 bytes needed for 8 values; provide header but only 1 byte of payload
		var data = new byte[]{0x03, (byte)0xAA}; // header + only 1 of 2 needed bytes
		var dec = new RleBitPackingDecoder(data, 2);
		// should not throw; just returns partial data
		assertDoesNotThrow(dec::readInt);
	}

	// -----------------------------------------------------------------------
	// n — decoder: readVarint edge cases
	// -----------------------------------------------------------------------

	@Test void n01_readVarint_eof_returns_zero() throws IOException {
		// Empty stream → header varint = 0 → treated as RLE with count=0 → returns rleValue=0
		var dec = new RleBitPackingDecoder(new byte[0], 1);
		assertEquals(0, dec.readInt());
	}

}
