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
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Writes column values using PLAIN encoding for Parquet data pages.
 *
 * <p>
 * Values are written in little-endian format per the
 * <a class="doclink" href="https://parquet.apache.org/docs/file-format/data-pages/encodings/">Parquet encodings</a>
 * specification. Tracks value count and byte sizes for page header creation.
 */
final class ParquetColumnWriter {

	private static final int PAGE_TYPE_DATA = 0;
	private static final int ENCODING_PLAIN = 0;
	private static final int ENCODING_RLE = 3;

	private final ParquetSchemaElement schema;
	private final ByteArrayOutputStream out = new ByteArrayOutputStream();
	private int valueCount;

	ParquetColumnWriter(ParquetSchemaElement schema) {
		this.schema = schema;
		this.valueCount = 0;
	}

	void writeBoolean(boolean value) {
		out.write(value ? 1 : 0);
		valueCount++;
	}

	void writeInt32(int value) {
		out.write(value & 0xFF);
		out.write((value >> 8) & 0xFF);
		out.write((value >> 16) & 0xFF);
		out.write((value >> 24) & 0xFF);
		valueCount++;
	}

	void writeInt64(long value) {
		out.write((int)(value & 0xFF));
		out.write((int)((value >> 8) & 0xFF));
		out.write((int)((value >> 16) & 0xFF));
		out.write((int)((value >> 24) & 0xFF));
		out.write((int)((value >> 32) & 0xFF));
		out.write((int)((value >> 40) & 0xFF));
		out.write((int)((value >> 48) & 0xFF));
		out.write((int)((value >> 56) & 0xFF));
		valueCount++;
	}

	void writeFloat(float value) {
		writeInt32(Float.floatToIntBits(value));
	}

	void writeDouble(double value) {
		writeInt64(Double.doubleToLongBits(value));
	}

	void writeByteArray(byte[] value) throws IOException {
		if (value == null)
			value = new byte[0];
		writeInt32(value.length);
		if (value.length > 0)
			out.write(value);
		valueCount++;
	}

	void writeByteArray(String value) throws IOException {
		byte[] bytes = value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8);
		writeByteArray(bytes);
	}

	void writeFixedLenByteArray(byte[] value) {
		if (value == null || schema.typeLength == null)
			return;
		int len = Math.min(value.length, schema.typeLength);
		for (int i = 0; i < schema.typeLength; i++)
			out.write(i < len ? value[i] : 0);
		valueCount++;
	}

	void writeNull() {
		valueCount++;
	}

	int getValueCount() {
		return valueCount;
	}

	long getUncompressedSize() {
		return out.size();
	}

	/**
	 * Finalizes the current page and returns the raw (uncompressed) value bytes.
	 * Resets the writer for the next page.
	 */
	byte[] finalizePage() {
		var result = out.toByteArray();
		out.reset();
		valueCount = 0;
		return result;
	}

	/**
	 * Creates a PageHeader (Thrift) for a data page with the given metrics.
	 */
	static byte[] createPageHeader(int numValues, int uncompressedSize, int compressedSize) throws IOException {
		var baos = new ByteArrayOutputStream();
		var enc = new ThriftCompactEncoder(baos);
		enc.writeStructBegin();
		enc.writeFieldBegin(ThriftCompactEncoder.I32, 1);
		enc.writeI32(PAGE_TYPE_DATA);
		enc.writeFieldBegin(ThriftCompactEncoder.I32, 2);
		enc.writeI32(uncompressedSize);
		enc.writeFieldBegin(ThriftCompactEncoder.I32, 3);
		enc.writeI32(compressedSize);
		enc.writeFieldBegin(ThriftCompactEncoder.STRUCT, 5);
		enc.writeStructBegin();
		enc.writeFieldBegin(ThriftCompactEncoder.I32, 1);
		enc.writeI32(numValues);
		enc.writeFieldBegin(ThriftCompactEncoder.I32, 2);
		enc.writeI32(ENCODING_PLAIN);
		enc.writeFieldBegin(ThriftCompactEncoder.I32, 3);
		enc.writeI32(ENCODING_RLE);
		enc.writeFieldBegin(ThriftCompactEncoder.I32, 4);
		enc.writeI32(ENCODING_RLE);
		enc.writeStructEnd();
		enc.writeStructEnd();
		return baos.toByteArray();
	}
}
