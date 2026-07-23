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

/**
 * Dependency-free, pure-Java decompressor for the Snappy <b>block</b> format (NOT the framed/xerial
 * stream format).
 *
 * <p>
 * Parquet stores each compressed page as a raw Snappy block: a little-endian varint preamble giving the
 * uncompressed length, followed by a sequence of tagged elements:
 * <ul>
 * 	<li><b>Literal</b> (tag bits {@code 00}) — the upper 6 tag bits, or a 1–4 byte little-endian length
 * 		that follows, give {@code len-1}; {@code len} raw bytes are copied to the output.
 * 	<li><b>Copy</b> (tag bits {@code 01}/{@code 10}/{@code 11}) — a back-reference {@code (length, offset)}
 * 		into already-decompressed output.  The three copy forms differ in offset width (1/2/4 bytes) and
 * 		how the length is encoded.
 * </ul>
 *
 * <p>
 * This implements the format described at
 * <a href="https://github.com/google/snappy/blob/main/format_description.txt">snappy/format_description.txt</a>.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>Decode only — Snappy compression (write) is intentionally unsupported.
 * </ul>
 */
final class SnappyBlockDecompressor {

	private SnappyBlockDecompressor() {}

	/**
	 * Decompresses a Snappy block.
	 *
	 * @param input The compressed Snappy block (varint length preamble + tagged elements).
	 * @param expectedSize The expected uncompressed size (from the Parquet page header).
	 * @return The decompressed bytes.
	 * @throws IOException If the block is malformed or its declared length disagrees with {@code expectedSize}.
	 */
	@SuppressWarnings({
		"java:S3776" // Cognitive complexity inherent to the Snappy block-codec decode loop; splitting the tag/literal/copy
		             // branches into helpers would fragment the tightly-coupled cursor/bounds logic and risk decoder bugs.
	})
	static byte[] decompress(byte[] input, int expectedSize) throws IOException {
		var in = new int[]{0}; // boxed read cursor
		int uncompressedLen = readVarint(input, in);
		// A crafted varint can set bit 31 (negative) or exceed our sanity ceiling; reject both.
		if (uncompressedLen < 0 || uncompressedLen > 256 * 1024 * 1024)
			throw ioex("Invalid Snappy uncompressed length: %s", uncompressedLen);
		if (expectedSize >= 0 && uncompressedLen != expectedSize)
			throw ioex("Snappy length mismatch: header declares %s but page expects %s", uncompressedLen, expectedSize);
		var out = new byte[uncompressedLen];
		int outPos = 0;
		int pos = in[0];
		while (outPos < uncompressedLen) {
			if (pos >= input.length)
				throw new IOException("Truncated Snappy block");
			int tag = input[pos++] & 0xFF;
			int elemType = tag & 0x03;
			if (elemType == 0) {
				// Literal: length-1 encoded in the upper 6 bits, or in trailing bytes when those bits are 60..63.
				int litLenMinus1 = tag >>> 2;
				int len;
				if (litLenMinus1 < 60) {
					len = litLenMinus1 + 1;
				} else {
					int extraBytes = litLenMinus1 - 59; // 60->1, 61->2, 62->3, 63->4
					if (pos + extraBytes > input.length)
						throw new IOException("Truncated Snappy literal length");
					long v = 0;
					for (int i = 0; i < extraBytes; i++)
						v |= (long)(input[pos++] & 0xFF) << (8 * i);
					len = (int)(v + 1);
				}
				// A crafted 4-byte literal length can overflow int to negative; reject that and both overruns.
				if (len < 0 || pos + len > input.length || outPos + len > uncompressedLen)
					throw new IOException("Snappy literal overruns buffer");
				System.arraycopy(input, pos, out, outPos, len);
				pos += len;
				outPos += len;
			} else {
				// Copy: a back-reference (length, offset) into already-emitted output.
				int length;
				int offset;
				if (elemType == 1) {
					// 1-byte offset: length-4 in tag bits 2..4 (3 bits), high 3 offset bits in tag bits 5..7.
					length = ((tag >>> 2) & 0x07) + 4;
					if (pos >= input.length)
						throw new IOException("Truncated Snappy copy");
					offset = ((tag >>> 5) & 0x07) << 8 | (input[pos++] & 0xFF);
				} else if (elemType == 2) {
					// 2-byte little-endian offset; length-1 in the upper 6 tag bits.
					length = (tag >>> 2) + 1;
					if (pos + 2 > input.length)
						throw new IOException("Truncated Snappy copy");
					offset = (input[pos] & 0xFF) | ((input[pos + 1] & 0xFF) << 8);
					pos += 2;
				} else {
					// 4-byte little-endian offset; length-1 in the upper 6 tag bits.
					length = (tag >>> 2) + 1;
					if (pos + 4 > input.length)
						throw new IOException("Truncated Snappy copy");
					offset = (input[pos] & 0xFF) | ((input[pos + 1] & 0xFF) << 8)
						| ((input[pos + 2] & 0xFF) << 16) | ((input[pos + 3] & 0xFF) << 24);
					pos += 4;
				}
				// A crafted 4-byte offset can be zero or overflow int to negative; reject those and out-of-range.
				if (offset <= 0 || offset > outPos)
					throw ioex("Invalid Snappy copy offset: %s", offset);
				if (outPos + length > uncompressedLen)
					throw new IOException("Snappy copy overruns buffer");
				// Byte-by-byte copy: overlapping runs (offset < length) are intentional and must replicate.
				int src = outPos - offset;
				for (int i = 0; i < length; i++)
					out[outPos++] = out[src++];
			}
		}
		return out;
	}

	/** Reads a little-endian base-128 varint, advancing {@code cursor[0]}. */
	private static int readVarint(byte[] data, int[] cursor) throws IOException {
		int pos = cursor[0];
		int result = 0;
		int shift = 0;
		while (shift < 32) {
			if (pos >= data.length)
				throw new IOException("Truncated Snappy varint");
			int b = data[pos++] & 0xFF;
			result |= (b & 0x7F) << shift;
			if ((b & 0x80) == 0) {
				cursor[0] = pos;
				return result;
			}
			shift += 7;
		}
		throw new IOException("Snappy varint too long");
	}
}
