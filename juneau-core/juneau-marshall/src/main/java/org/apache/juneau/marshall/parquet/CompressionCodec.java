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
import java.util.zip.*;

/**
 * Parquet compression codecs supported by this implementation.
 *
 * <p>
 * UNCOMPRESSED and GZIP support both directions.  SNAPPY is <b>decode-only</b> (a dependency-free,
 * pure-Java Snappy <i>block-format</i> decompressor) so Juneau can read spec-compliant Parquet from
 * parquet-mr / Spark / Hive, which default to Snappy.  LZ4, Zstd, Brotli, and LZO require external
 * libraries and remain unsupported (hard error on read).
 */
public enum CompressionCodec {

	/** No compression. */
	UNCOMPRESSED(0) {

		@Override
		byte[] compress(byte[] data) {
			return data;
		}

		@Override
		byte[] decompress(byte[] data, int uncompressedSize) {
			return data;
		}
	},

	/** Snappy block format — decode only (write is not supported). */
	SNAPPY(1) {

		@Override
		byte[] compress(byte[] data) throws IOException {
			throw new IOException("Snappy compression (write) is not supported; use UNCOMPRESSED or GZIP.");
		}

		@Override
		byte[] decompress(byte[] data, int uncompressedSize) throws IOException {
			return SnappyBlockDecompressor.decompress(data, uncompressedSize);
		}
	},

	/** GZIP compression. */
	GZIP(2) {

		@Override
		byte[] compress(byte[] data) throws IOException {
			var baos = new ByteArrayOutputStream();
			try (var gzip = new GZIPOutputStream(baos)) {
				gzip.write(data);
			}
			return baos.toByteArray();
		}

		@Override
		byte[] decompress(byte[] data, int uncompressedSize) throws IOException {
			// Decode incrementally into a buffer that grows with the bytes actually produced, rather than
			// pre-allocating the declared uncompressedSize up front.  A header may declare a large size it
			// cannot actually deliver; growing on demand means such a mismatch is detected after allocating
			// only what was really produced, not the (possibly huge) declared amount.
			var out = new ByteArrayOutputStream(Math.min(Math.max(uncompressedSize, 0), 8192));
			var buf = new byte[8192];
			try (var gzip = new GZIPInputStream(new ByteArrayInputStream(data))) {
				var total = 0;
				int r;
				while (total < uncompressedSize && (r = gzip.read(buf, 0, Math.min(buf.length, uncompressedSize - total))) > 0) {
					out.write(buf, 0, r);
					total += r;
				}
				if (total < uncompressedSize)
					throw new IOException("GZIP decompression produced fewer bytes than expected");
			}
			return out.toByteArray();
		}
	};

	/** Thrift FileMetaData Codec enum value. */
	final int thriftValue;

	/**
	 * Constructor.
	 *
	 * @param thriftValue The Thrift codec value.
	 */
	CompressionCodec(int thriftValue) {
		this.thriftValue = thriftValue;
	}

	abstract byte[] compress(byte[] data) throws IOException;

	abstract byte[] decompress(byte[] data, int uncompressedSize) throws IOException;

	/**
	 * Resolves a Thrift {@code CompressionCodec} enum value to a supported codec.
	 *
	 * <p>
	 * {@code UNCOMPRESSED} (0) and {@code GZIP} (2) support both directions; {@code SNAPPY} (1) is
	 * decode-only.  Any other value — a known-but-unsupported codec (LZO/BROTLI/LZ4/ZSTD/LZ4_RAW) or an
	 * unrecognized id — is a hard error rather than a silent fallback to {@code UNCOMPRESSED}, which would
	 * otherwise treat still-compressed bytes as raw page data and surface silent garbage (GAP-5).
	 *
	 * @param value The Thrift codec enum value.
	 * @return The matching supported codec.
	 * @throws IOException If the codec is not supported by this implementation.
	 */
	static CompressionCodec fromThrift(int value) throws IOException {
		for (var c : values())
			if (c.thriftValue == value)
				return c;
		throw ioex("Unsupported Parquet compression codec: %s. Only UNCOMPRESSED, GZIP, and SNAPPY (decode-only) are supported.", codecName(value));
	}

	private static String codecName(int value) {
		return switch (value) {
			case 3 -> "LZO (3)";
			case 4 -> "BROTLI (4)";
			case 5 -> "LZ4 (5)";
			case 6 -> "ZSTD (6)";
			case 7 -> "LZ4_RAW (7)";
			default -> Integer.toString(value);
		};
	}
}
