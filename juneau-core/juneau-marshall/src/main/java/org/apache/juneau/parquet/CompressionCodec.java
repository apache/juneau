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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Parquet compression codecs supported by this implementation.
 *
 * <p>
 * Only UNCOMPRESSED and GZIP are supported; Snappy, LZ4, and Zstd require external libraries.
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
			var result = new byte[uncompressedSize];
			try (var gzip = new GZIPInputStream(new ByteArrayInputStream(data))) {
				int n = 0;
				while (n < uncompressedSize) {
					int r = gzip.read(result, n, uncompressedSize - n);
					if (r <= 0)
						break;
					n += r;
				}
				if (n < uncompressedSize)
					throw new IOException("GZIP decompression produced fewer bytes than expected");
			}
			return result;
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

	static CompressionCodec fromThrift(int value) {
		for (var c : values())
			if (c.thriftValue == value)
				return c;
		return UNCOMPRESSED;
	}
}
