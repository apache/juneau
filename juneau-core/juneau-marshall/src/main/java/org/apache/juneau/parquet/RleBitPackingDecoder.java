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

/**
 * RLE/Bit-Packing Hybrid decoder for definition/repetition levels and booleans.
 */
final class RleBitPackingDecoder {

	private final InputStream in;
	private final int bitWidth;
	private int bitPackedCount;
	private int bitPackedRead;
	private int bitPackedGroupsRemaining;
	private final int[] bitPackedBuffer = new int[8];
	private int rleValue;
	private int rleCount;
	RleBitPackingDecoder(byte[] data, int bitWidth) {
		this(new ByteArrayInputStream(data), bitWidth);
	}

	RleBitPackingDecoder(byte[] data, int offset, int length, int bitWidth) {
		this(new ByteArrayInputStream(data, offset, length), bitWidth);
	}

	RleBitPackingDecoder(InputStream in, int bitWidth) {
		this.in = in;
		this.bitWidth = bitWidth;
		this.bitPackedCount = 0;
		this.bitPackedRead = 0;
		this.bitPackedGroupsRemaining = 0;
		this.rleCount = 0;
	}

	int readInt() throws IOException {
		if (rleCount > 0) {
			rleCount--;
			return rleValue;
		}
		if (bitPackedRead < bitPackedCount) {
			return bitPackedBuffer[bitPackedRead++];
		}
		if (bitPackedGroupsRemaining > 0) {
			readBitPackedGroup();
			return bitPackedBuffer[bitPackedRead++];
		}
		var header = readVarint();
		if ((header & 1) == 1) {
			bitPackedGroupsRemaining = (int)(header >>> 1);
			readBitPackedGroup();
			bitPackedRead = 1;
			return bitPackedBuffer[0];
		}
		var count = (int)(header >>> 1);
		var bytesPerValue = (bitWidth + 7) / 8;
		rleValue = 0;
		for (int i = 0; i < bytesPerValue; i++) {
			var x = in.read();
			if (x >= 0)
				rleValue |= (x & 0xFF) << (i * 8);
		}
		rleValue &= (1 << bitWidth) - 1;
		rleCount = count - 1;
		return rleValue;
	}

	private void readBitPackedGroup() throws IOException {
		var totalBits = bitWidth * 8;
		var totalBytes = (totalBits + 7) / 8;
		var mask = (1 << bitWidth) - 1;
		for (int i = 0; i < 8; i++)
			bitPackedBuffer[i] = 0;
		for (int byteIdx = 0; byteIdx < totalBytes; byteIdx++) {
			var b = in.read();
			if (b < 0)
				break;
			for (int bitIdx = 0; bitIdx < 8; bitIdx++) {
				var globalBit = byteIdx * 8 + bitIdx;
				if (globalBit >= totalBits)
					break;
				var valIdx = globalBit / bitWidth;
				var valBit = bitWidth - 1 - (globalBit % bitWidth);
				if (((b >> bitIdx) & 1) != 0)
					bitPackedBuffer[valIdx] |= 1 << valBit;
			}
		}
		for (int i = 0; i < 8; i++)
			bitPackedBuffer[i] &= mask;
		bitPackedCount = 8;
		bitPackedRead = 0;
		bitPackedGroupsRemaining--;
	}

	private long readVarint() throws IOException {
		var result = 0L;
		var shift = 0;
		while (shift < 70) {
			var b = in.read();
			if (b < 0)
				break;
			result |= (long)(b & 0x7F) << shift;
			if ((b & 0x80) == 0)
				return result;
			shift += 7;
		}
		return result;
	}

	boolean hasNext() throws IOException {
		return rleCount > 0 || bitPackedRead < bitPackedCount || bitPackedGroupsRemaining > 0 || in.available() > 0;
	}
}
