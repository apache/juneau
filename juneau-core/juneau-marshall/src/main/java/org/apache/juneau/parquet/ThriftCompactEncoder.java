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
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Minimal Thrift Compact Protocol encoder for writing Parquet metadata.
 *
 * <p>
 * Implements the encoding rules from the
 * <a class="doclink" href="https://github.com/apache/thrift/blob/master/doc/specs/thrift-compact-protocol.md">Thrift Compact Protocol</a>
 * specification. Used by Parquet to encode FileMetaData, PageHeader, SchemaElement, etc.
 */
final class ThriftCompactEncoder {

	/** Thrift Compact Protocol type codes. */
	static final int BOOLEAN_TRUE = 1;
	static final int BOOLEAN_FALSE = 2;
	static final int I8 = 3;
	static final int I16 = 4;
	static final int I32 = 5;
	static final int I64 = 6;
	static final int DOUBLE = 7;
	static final int BINARY = 8;
	static final int LIST = 9;
	static final int SET = 10;
	static final int MAP = 11;
	static final int STRUCT = 12;

	private final OutputStream out;
	private int lastFieldId;
	private final Deque<Integer> lastFieldIdStack = new ArrayDeque<>();

	ThriftCompactEncoder(OutputStream out) {
		this.out = out;
		this.lastFieldId = 0;
	}

	/**
	 * Writes a field header (short form when delta 1-15, long form otherwise).
	 */
	void writeFieldBegin(int fieldType, int fieldId) throws IOException {
		var delta = fieldId - lastFieldId;
		lastFieldId = fieldId;
		if (delta > 0 && delta <= 15) {
			out.write((delta << 4) | (fieldType & 0x0F));
		} else {
			out.write(fieldType & 0x0F);
			writeVarint(delta);
		}
	}

	/**
	 * Writes the stop field (0x00) and restores parent field ID for nested structs.
	 */
	void writeFieldStop() throws IOException {
		out.write(0);
		lastFieldId = lastFieldIdStack.isEmpty() ? 0 : lastFieldIdStack.removeLast();
	}

	void writeBool(boolean value) throws IOException {
		out.write(value ? BOOLEAN_TRUE : BOOLEAN_FALSE);
	}

	void writeByte(int value) throws IOException {
		out.write(value & 0xFF);
	}

	void writeI16(short value) throws IOException {
		writeVarint(toZigzag(value));
	}

	void writeI32(int value) throws IOException {
		writeVarint(toZigzag(value));
	}

	void writeI64(long value) throws IOException {
		writeVarint(toZigzag(value));
	}

	void writeDouble(double value) throws IOException {
		var bits = Double.doubleToLongBits(value);
		out.write((int)(bits & 0xFF));
		out.write((int)((bits >> 8) & 0xFF));
		out.write((int)((bits >> 16) & 0xFF));
		out.write((int)((bits >> 24) & 0xFF));
		out.write((int)((bits >> 32) & 0xFF));
		out.write((int)((bits >> 40) & 0xFF));
		out.write((int)((bits >> 48) & 0xFF));
		out.write((int)((bits >> 56) & 0xFF));
	}

	void writeBinary(byte[] data) throws IOException {
		writeVarint(data == null ? 0 : data.length);
		if (data != null && data.length > 0)
			out.write(data);
	}

	void writeString(String value) throws IOException {
		byte[] bytes = value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8);
		writeVarint(bytes.length);
		if (bytes.length > 0)
			out.write(bytes);
	}

	/**
	 * Writes a list header. Short form when size 0-14, long form otherwise.
	 */
	void writeListBegin(int elemType, int size) throws IOException {
		if (size >= 0 && size <= 14) {
			out.write((size << 4) | (elemType & 0x0F));
		} else {
			out.write(0xF0 | (elemType & 0x0F));
			writeVarint(size);
		}
	}

	void writeStructBegin() {
		lastFieldIdStack.addLast(lastFieldId);
		lastFieldId = 0;
	}

	void writeStructEnd() throws IOException {
		writeFieldStop();
	}

	/**
	 * Writes a value in ULEB128 format (7 bits per byte, LSB first).
	 */
	void writeVarint(long value) throws IOException {
		while ((value & ~0x7FL) != 0) {
			out.write((int)((value & 0x7F) | 0x80));
			value >>>= 7;
		}
		out.write((int)(value & 0x7F));
	}

	/**
	 * Zigzag encoding for signed integers.
	 */
	static long toZigzag(long value) {
		return (value << 1) ^ (value >> 63);
	}

	/**
	 * Writes the encoder state to a byte array. Used when we need to encode to a buffer
	 * and then get the bytes (e.g. for footer).
	 */
	static byte[] encodeToBytes(ThriftWriteAction action) throws IOException {
		var baos = new ByteArrayOutputStream();
		var enc = new ThriftCompactEncoder(baos);
		action.run(enc);
		return baos.toByteArray();
	}

	@FunctionalInterface
	interface ThriftWriteAction {
		void run(ThriftCompactEncoder enc) throws IOException;
	}
}
