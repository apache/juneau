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
package org.apache.juneau.parquet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Reads column values from Parquet page data (PLAIN encoding).
 *
 * <p>
 * Page data layout: 4-byte LE definition levels length, RLE-encoded definition levels, then PLAIN-encoded values.
 * For REQUIRED columns there are no definition levels.
 */
final class ParquetColumnReader {

	private final RleBitPackingDecoder defLevelDecoder;
	private final InputStream valueStream;
	private final int numValues;
	private final int maxDefLevel;
	private int readCount;
	private boolean nextNull;

	ParquetColumnReader(byte[] pageData, int numValues, int maxDefLevel) {
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
				this.defLevelDecoder = new RleBitPackingDecoder(pageData, off, defLen, 1);
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
		var b = valueStream.read();
		return b == 1;
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
			throw new IOException("Byte array length " + len + " exceeds maximum " + MAX_BYTE_ARRAY_LEN);
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
			var defLevel = defLevelDecoder.readInt();
			nextNull = defLevel < maxDefLevel;
		} else {
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
