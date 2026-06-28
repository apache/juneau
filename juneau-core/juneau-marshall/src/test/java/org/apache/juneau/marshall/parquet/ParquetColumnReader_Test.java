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
 * Unit tests for {@link ParquetColumnReader} null-slot and per-type read paths.
 */
class ParquetColumnReader_Test extends TestBase {

	/** Builds an OPTIONAL (maxDefLevel=1) page whose single value is null (def level 0), no value bytes. */
	private static byte[] singleNullPage() {
		var def = new RleBitPackingEncoder(1);
		def.writeInt(0); // def 0 == null
		return def.toByteArrayWithLength();
	}

	/** Builds an OPTIONAL page with one present value (def 1) followed by the given value bytes. */
	private static byte[] singlePresentPage(byte[] valueBytes) {
		var def = new RleBitPackingEncoder(1);
		def.writeInt(1); // def 1 == present
		var levels = def.toByteArrayWithLength();
		var out = new byte[levels.length + valueBytes.length];
		System.arraycopy(levels, 0, out, 0, levels.length);
		System.arraycopy(valueBytes, 0, out, levels.length, valueBytes.length);
		return out;
	}

	@Test
	void a01_nullBooleanIntLongFloatDouble() throws Exception {
		var page = singleNullPage();
		// Each typed read returns its zero/empty default for a null slot (after advance() sets nextNull).
		var r = new ParquetColumnReader(page, 1, 1);
		r.advance();
		assertTrue(r.isNull());
		assertFalse(r.readBoolean());
	}

	@Test
	void a02_nullInt32() throws Exception {
		var r = new ParquetColumnReader(singleNullPage(), 1, 1);
		r.advance();
		assertEquals(0, r.readInt32());
	}

	@Test
	void a03_nullInt64() throws Exception {
		var r = new ParquetColumnReader(singleNullPage(), 1, 1);
		r.advance();
		assertEquals(0L, r.readInt64());
	}

	@Test
	void a04_nullFloatDouble() throws Exception {
		var r1 = new ParquetColumnReader(singleNullPage(), 1, 1);
		r1.advance();
		assertEquals(0f, r1.readFloat());
		var r2 = new ParquetColumnReader(singleNullPage(), 1, 1);
		r2.advance();
		assertEquals(0d, r2.readDouble());
	}

	@Test
	void a05_nullByteArrayAndString() throws Exception {
		var r = new ParquetColumnReader(singleNullPage(), 1, 1);
		r.advance();
		assertEquals(0, r.readByteArray().length);
		var r2 = new ParquetColumnReader(singleNullPage(), 1, 1);
		r2.advance();
		assertEquals("", r2.readByteArrayAsString());
	}

	@Test
	void a06_nullFixedLenByteArray() throws Exception {
		var r = new ParquetColumnReader(singleNullPage(), 1, 1);
		r.advance();
		assertEquals(0, r.readFixedLenByteArray(16).length);
	}

	@Test
	void a07_nullInt96ConsumesNothingReturnsNull() throws Exception {
		var r = new ParquetColumnReader(singleNullPage(), 1, 1);
		r.advance();
		assertNull(r.readInt96AsInstant());
	}

	@Test
	void a08_presentInt32RoundTrips() throws Exception {
		// Little-endian 42.
		var value = new byte[]{42, 0, 0, 0};
		var r = new ParquetColumnReader(singlePresentPage(value), 1, 1);
		r.advance();
		assertFalse(r.isNull());
		assertEquals(1, r.getDefLevel());
		assertEquals(42, r.readInt32());
	}

	@Test
	void a09_requiredColumnNoDefLevels() throws Exception {
		// maxDefLevel=0: no definition levels, value bytes start at offset 0.
		var value = new byte[]{7, 0, 0, 0};
		var r = new ParquetColumnReader(value, 1, 0);
		r.advance();
		assertFalse(r.isNull());
		assertEquals(0, r.getDefLevel());
		assertEquals(7, r.readInt32());
	}

	@Test
	void a10_byteArrayLengthGuard() {
		// A BYTE_ARRAY whose declared length exceeds the cap throws.  REQUIRED column (no def levels):
		// 4-byte LE length = 0x7FFFFFFF, then no payload.
		var page = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x7F};
		var r = new ParquetColumnReader(page, 1, 0);
		assertThrows(IOException.class, () -> { r.advance(); r.readByteArray(); });
	}

	@Test
	void a11_emptyByteArrayWhenLengthZero() throws Exception {
		// REQUIRED column, 4-byte LE length = 0 -> empty array.
		var page = new byte[]{0, 0, 0, 0};
		var r = new ParquetColumnReader(page, 1, 0);
		r.advance();
		assertEquals(0, r.readByteArray().length);
	}

	@Test
	void a12_byteArrayStreamUnderflowBreaks() throws Exception {
		// REQUIRED column declaring length 8 but only 3 payload bytes available -> read loop breaks early.
		var page = new byte[]{8, 0, 0, 0, 1, 2, 3};
		var r = new ParquetColumnReader(page, 1, 0);
		r.advance();
		var b = r.readByteArray();
		assertEquals(8, b.length); // buffer is allocated to declared length; tail stays zero on underflow
	}

	@Test
	void a13_fixedLenByteArrayStreamUnderflowBreaks() throws Exception {
		// REQUIRED column: ask for 16 FLBA bytes but only 4 available -> read loop breaks early.
		var page = new byte[]{1, 2, 3, 4};
		var r = new ParquetColumnReader(page, 1, 0);
		r.advance();
		assertEquals(16, r.readFixedLenByteArray(16).length);
	}

	@Test
	void a14_constructorShortPageNoDefLevels() throws Exception {
		// maxDefLevel>0 but pageData.length<4: the def-level block is skipped (defLevelDecoder stays null),
		// every slot is treated as present.
		var page = new byte[]{42, 0, 0}; // 3 bytes < 4
		var r = new ParquetColumnReader(page, 1, 1);
		r.advance();
		assertFalse(r.isNull());
	}

	@Test
	void a15_constructorInvalidDefLenTreatedAsNoDefLevels() throws Exception {
		// maxDefLevel>0, length>=4, but the leading 4-byte defLen is absurdly large (> remaining) so
		// hasDefLevels is false and the bytes are treated as values.
		var page = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x7F, 9, 9, 9, 9};
		var r = new ParquetColumnReader(page, 1, 1);
		r.advance();
		assertFalse(r.isNull());
	}

	@Test
	void a16_constructorNegativeDefLenTreatedAsNoDefLevels() throws Exception {
		// maxDefLevel>0, length>=4, leading 4-byte defLen reads negative (top bit set) -> defLen>=0 false.
		var page = new byte[]{0, 0, 0, (byte) 0x80, 9, 9, 9, 9};
		var r = new ParquetColumnReader(page, 1, 1);
		r.advance();
		assertFalse(r.isNull());
	}

	@Test
	void a17_advancePastEndIsNoop() throws Exception {
		// advance() called more times than numValues short-circuits at readCount >= numValues.
		var r = new ParquetColumnReader(new byte[]{1, 0, 0, 0}, 1, 0);
		r.advance();
		assertFalse(r.hasNext());
		r.advance(); // past the end — no-op, must not throw
		assertFalse(r.hasNext());
	}
}
