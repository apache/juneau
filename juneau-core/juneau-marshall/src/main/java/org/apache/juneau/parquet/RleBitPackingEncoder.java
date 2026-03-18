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

import java.io.ByteArrayOutputStream;

/**
 * RLE/Bit-Packing Hybrid encoder for definition/repetition levels and booleans.
 *
 * <p>
 * Implements the encoding from the
 * <a class="doclink" href="https://parquet.apache.org/docs/file-format/data-pages/encodings/">Parquet encodings</a>
 * specification. Values are packed in groups of 8 (bit-packed run) or encoded as repeated runs (RLE run).
 */
final class RleBitPackingEncoder {

	private final int bitWidth;
	private final ByteArrayOutputStream out = new ByteArrayOutputStream();
	private int lastValue = -1;
	private int runLength;
	private final int[] bitPackedBuffer = new int[8];
	private int bitPackedCount;

	RleBitPackingEncoder(int bitWidth) {
		if (bitWidth < 1 || bitWidth > 32)
			throw new IllegalArgumentException("bitWidth must be 1-32");
		this.bitWidth = bitWidth;
	}

	void writeInt(int value) {
		if (value == lastValue) {
			runLength++;
			if (runLength >= 8) {
				flushRun();
			}
		} else {
			if (runLength > 0) {
				flushRun();
			}
			lastValue = value;
			runLength = 1;
		}
		if (runLength < 8)
			return;
		flushRun();
	}

	private void flushRun() {
		if (runLength >= 8) {
			writeRleRun(lastValue, runLength);
		} else if (runLength > 0) {
			for (int i = 0; i < runLength; i++) {
				bitPackedBuffer[bitPackedCount++] = lastValue;
				if (bitPackedCount == 8) {
					writeBitPackedRun(bitPackedBuffer, 8);
					bitPackedCount = 0;
				}
			}
		}
		runLength = 0;
		lastValue = -1;
	}

	private void writeRleRun(int value, int count) {
		writeVarint((long) count << 1);
		var bytesPerValue = (bitWidth + 7) / 8;
		for (int i = 0; i < bytesPerValue; i++)
			out.write((value >> (i * 8)) & 0xFF);
	}

	/**
	 * Packs 8 values into bytes. Bits are packed from LSB of each value to MSB, and from LSB of each byte.
	 * Per Parquet spec: values packed from LSB of each byte to MSB; bits within each value stay MSB to LSB.
	 */
	private void writeBitPackedRun(int[] values, int count) {
		var scaledLen = count / 8;
		writeVarint((long) scaledLen << 1 | 1);
		var totalBits = bitWidth * 8;
		var totalBytes = (totalBits + 7) / 8;
		for (int byteIdx = 0; byteIdx < totalBytes; byteIdx++) {
			var acc = 0;
			for (int bitIdx = 0; bitIdx < 8; bitIdx++) {
				var globalBit = byteIdx * 8 + bitIdx;
				if (globalBit >= totalBits)
					break;
				var valIdx = globalBit / bitWidth;
				var valBit = bitWidth - 1 - (globalBit % bitWidth);
				var val = values[valIdx] & ((1 << bitWidth) - 1);
				if ((val & (1 << valBit)) != 0)
					acc |= 1 << bitIdx;
			}
			out.write(acc & 0xFF);
		}
	}

	private void writeVarint(long value) {
		while ((value & ~0x7FL) != 0) {
			out.write((int)((value & 0x7F) | 0x80));
			value >>>= 7;
		}
		out.write((int)(value & 0x7F));
	}

	/**
	 * Finalizes and returns the encoded bytes.
	 */
	byte[] toByteArray() {
		if (runLength > 0 || bitPackedCount > 0) {
			if (runLength > 0) {
				for (int i = 0; i < runLength; i++)
					bitPackedBuffer[bitPackedCount++] = lastValue;
				runLength = 0;
			}
			if (bitPackedCount > 0) {
				while (bitPackedCount < 8)
					bitPackedBuffer[bitPackedCount++] = 0;
				writeBitPackedRun(bitPackedBuffer, 8);
			}
		}
		return out.toByteArray();
	}

	/**
	 * Returns encoded bytes with a 4-byte little-endian length prefix (for Data Page v1 definition/repetition levels).
	 */
	byte[] toByteArrayWithLength() {
		var data = toByteArray();
		var result = new byte[4 + data.length];
		result[0] = (byte)(data.length & 0xFF);
		result[1] = (byte)((data.length >> 8) & 0xFF);
		result[2] = (byte)((data.length >> 16) & 0xFF);
		result[3] = (byte)((data.length >> 24) & 0xFF);
		System.arraycopy(data, 0, result, 4, data.length);
		return result;
	}
}
