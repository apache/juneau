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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.nio.charset.*;
import java.time.*;

/**
 * Reads column values from Parquet page data (PLAIN encoding).
 *
 * <p>
 * Page data layout: 4-byte LE definition levels length, RLE-encoded definition levels, then PLAIN-encoded values.
 * For REQUIRED columns there are no definition levels.
 */
@SuppressWarnings({
	"resource" // valueStream is an in-memory ByteArrayInputStream over page bytes; there is no OS resource to close.
})
final class ParquetColumnReader {

	private final RleBitPackingDecoder defLevelDecoder;
	private final InputStream valueStream;
	private final int numValues;
	private final int maxDefLevel;
	private int readCount;
	private boolean nextNull;
	private int defLevel;
	private int boolByte;
	private int boolBitPos = 8;

	ParquetColumnReader(byte[] pageData, int numValues, int maxDefLevel) {
		this(pageData, numValues, maxDefLevel, 1);
	}

	/**
	 * @param pageData The (decompressed) page bytes.
	 * @param numValues Number of level/value entries in the page.
	 * @param maxDefLevel Maximum definition level; the leaf is present iff its def level equals this.
	 * @param defBitWidth Bit width of the RLE-encoded definition levels.  For multi-level OPTIONAL nesting
	 * 	(GAP-14) this is wider than 1 so intermediate-null vs leaf-null can be distinguished.
	 */
	ParquetColumnReader(byte[] pageData, int numValues, int maxDefLevel, int defBitWidth) {
		this.numValues = numValues;
		this.maxDefLevel = maxDefLevel;
		this.readCount = 0;
		this.nextNull = false;
		int off = 0;
		if (maxDefLevel > 0 && pageData.length >= 4) {
			var defLen = (pageData[off] & 0xFF) | ((pageData[off + 1] & 0xFF) << 8)
				| ((pageData[off + 2] & 0xFF) << 16) | ((pageData[off + 3] & 0xFF) << 24);
			var hasDefLevels = defLen >= 0 && defLen <= pageData.length - 4;
			if (hasDefLevels) {
				off += 4;
				this.defLevelDecoder = new RleBitPackingDecoder(pageData, off, defLen, defBitWidth);
				off += defLen;
			} else {
				this.defLevelDecoder = null;
			}
		} else {
			this.defLevelDecoder = null;
		}
		this.valueStream = new ByteArrayInputStream(pageData, off, pageData.length - off);
	}

	boolean isNull() {
		return nextNull;
	}

	/**
	 * Returns the definition level read by the most recent {@link #advance()} call.
	 *
	 * <p>
	 * Used by multi-level OPTIONAL reconstruction (GAP-14) to tell a null intermediate group
	 * (def &lt; maxDefLevel-1) apart from a present group with a null leaf (def == maxDefLevel-1).
	 *
	 * @return The last definition level (0 when the column carries no definition levels).
	 */
	int getDefLevel() {
		return defLevel;
	}

	boolean hasNext() {
		return readCount < numValues;
	}

	/**
	 * Advances to the next value (reads definition level and increments count).
	 * Must be called once per value before reading or checking null.
	 */
	void advance() throws IOException {
		advanceDefLevel();
	}

	boolean readBoolean() throws IOException {
		if (nextNull)
			return false;
		// PLAIN BOOLEAN is bit-packed 1 bit/value, LSB-first (GAP-4).  Pull a fresh byte every 8 present
		// booleans; null slots consume no bit (matches the writer, which only packs present values).
		if (boolBitPos == 8) {
			boolByte = valueStream.read();
			boolBitPos = 0;
		}
		var bit = (boolByte >> boolBitPos) & 1;
		boolBitPos++;
		return bit == 1;
	}

	int readInt32() throws IOException {
		if (nextNull)
			return 0;
		var b0 = valueStream.read() & 0xFF;
		var b1 = valueStream.read() & 0xFF;
		var b2 = valueStream.read() & 0xFF;
		var b3 = valueStream.read() & 0xFF;
		return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
	}

	long readInt64() throws IOException {
		if (nextNull)
			return 0;
		var b0 = (long)(valueStream.read() & 0xFF);
		var b1 = (long)(valueStream.read() & 0xFF);
		var b2 = (long)(valueStream.read() & 0xFF);
		var b3 = (long)(valueStream.read() & 0xFF);
		var b4 = (long)(valueStream.read() & 0xFF);
		var b5 = (long)(valueStream.read() & 0xFF);
		var b6 = (long)(valueStream.read() & 0xFF);
		var b7 = (long)(valueStream.read() & 0xFF);
		return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24) | (b4 << 32) | (b5 << 40) | (b6 << 48) | (b7 << 56);
	}

	/**
	 * Reads a legacy INT96 value (GAP-8) — 12 bytes: an 8-byte little-endian nanoseconds-of-day followed
	 * by a 4-byte little-endian Julian day number — and converts it to an {@link Instant}.  Used by
	 * Impala/Hive/older-Spark timestamp columns.  Returns <jk>null</jk> for a null slot.
	 */
	Instant readInt96AsInstant() throws IOException {
		if (nextNull) {
			// Still consume the 12 bytes so the stream stays aligned.
			for (var i = 0; i < 12; i++)
				valueStream.read();
			return null;
		}
		long nanosOfDay = readInt64();
		int julianDay = readInt32();
		// Julian day 2440588 == 1970-01-01.  Days since epoch * 86400s + nanos-of-day.
		long epochDay = julianDay - 2440588L;
		long epochSecond = epochDay * 86_400L + nanosOfDay / 1_000_000_000L;
		long nanoAdjust = nanosOfDay % 1_000_000_000L;
		return Instant.ofEpochSecond(epochSecond, nanoAdjust);
	}

	float readFloat() throws IOException {
		return Float.intBitsToFloat(readInt32());
	}

	double readDouble() throws IOException {
		return Double.longBitsToDouble(readInt64());
	}

	private static final int MAX_BYTE_ARRAY_LEN = 64 * 1024 * 1024;

	byte[] readByteArray() throws IOException {
		if (nextNull)
			return new byte[0];
		var len = readInt32Raw();
		if (len <= 0)
			return new byte[0];
		if (len > MAX_BYTE_ARRAY_LEN)
			throw ioex("Byte array length %s exceeds maximum %s", len, MAX_BYTE_ARRAY_LEN);
		var b = new byte[len];
		var n = 0;
		while (n < len) {
			var r = valueStream.read(b, n, len - n);
			if (r <= 0)
				break;
			n += r;
		}
		return b;
	}

	String readByteArrayAsString() throws IOException {
		return new String(readByteArray(), StandardCharsets.UTF_8);
	}

	byte[] readFixedLenByteArray(int length) throws IOException {
		if (nextNull)
			return new byte[0];
		var b = new byte[length];
		var n = 0;
		while (n < length) {
			var r = valueStream.read(b, n, length - n);
			if (r <= 0)
				break;
			n += r;
		}
		return b;
	}

	private void advanceDefLevel() throws IOException {
		if (readCount >= numValues)
			return;
		if (maxDefLevel > 0 && defLevelDecoder != null) {
			defLevel = defLevelDecoder.readInt();
			nextNull = defLevel < maxDefLevel;
		} else {
			defLevel = maxDefLevel;
			nextNull = false;
		}
		readCount++;
	}

	private int readInt32Raw() throws IOException {
		var b0 = valueStream.read() & 0xFF;
		var b1 = valueStream.read() & 0xFF;
		var b2 = valueStream.read() & 0xFF;
		var b3 = valueStream.read() & 0xFF;
		return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
	}
}
