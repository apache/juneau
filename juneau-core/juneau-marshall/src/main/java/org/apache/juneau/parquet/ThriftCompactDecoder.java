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
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Minimal Thrift Compact Protocol decoder for reading Parquet metadata.
 */
final class ThriftCompactDecoder {

	static final class FieldHeader {
		final int type;
		final int fieldId;
		final boolean isStop;

		FieldHeader(int type, int fieldId, boolean isStop) {
			this.type = type;
			this.fieldId = fieldId;
			this.isStop = isStop;
		}
	}

	static final class ListHeader {
		final int elemType;
		final int size;

		ListHeader(int elemType, int size) {
			this.elemType = elemType;
			this.size = size;
		}
	}

	private final InputStream in;
	private int lastFieldId;
	private final Deque<Integer> lastFieldIdStack = new ArrayDeque<>();

	ThriftCompactDecoder(InputStream in) {
		this.in = in;
		this.lastFieldId = 0;
	}

	ThriftCompactDecoder(byte[] data) {
		this(new ByteArrayInputStream(data));
	}

	ThriftCompactDecoder(byte[] data, int offset, int length) {
		this(new ByteArrayInputStream(data, offset, length));
	}

	FieldHeader readFieldHeader() throws IOException {
		var b = in.read();
		if (b < 0)
			return new FieldHeader(0, 0, true);
		if (b == 0)
			return new FieldHeader(0, 0, true);
		var type = b & 0x0F;
		var delta = (b >> 4) & 0x0F;
		if (delta != 0) {
			lastFieldId += delta;
			return new FieldHeader(type, lastFieldId, false);
		}
		var fieldId = (int) readVarint();
		lastFieldId = fieldId;
		return new FieldHeader(type, fieldId, false);
	}

	@SuppressWarnings({
		"java:S3776" // Cognitive Complexity: Thrift type dispatch is inherently branchy
	})
	void skipField(int fieldType) throws IOException {
		switch (fieldType) {
			case ThriftCompactEncoder.BOOLEAN_TRUE, ThriftCompactEncoder.BOOLEAN_FALSE:
				break;
			case ThriftCompactEncoder.I8:
				in.read();
				break;
			case ThriftCompactEncoder.I16, ThriftCompactEncoder.I32, ThriftCompactEncoder.I64:
				readVarint();
				break;
			case ThriftCompactEncoder.DOUBLE:
				for (int i = 0; i < 8; i++)
					in.read();
				break;
			case ThriftCompactEncoder.BINARY: {
				var len = readVarint();
				if (len < 0 || len > MAX_BINARY_LENGTH)
					throw new IOException("Invalid binary length: " + len);
				in.skipNBytes((int) len);
				break;
			}
			case ThriftCompactEncoder.LIST, ThriftCompactEncoder.SET: {
				var b = in.read();
				if (b < 0) break;
				var elemType = b & 0x0F;
				var size = (b >> 4) & 0x0F;
				if (size == 15)
					size = (int) readVarint();
				if (size < 0 || size > MAX_LIST_SIZE)
					throw new IOException("Invalid list size: " + size);
				for (int i = 0; i < size; i++)
					skipField(elemType);
				break;
			}
			case ThriftCompactEncoder.MAP: {
				var size = (int) readVarint();
				if (size < 0 || size > MAX_LIST_SIZE)
					throw new IOException("Invalid map size: " + size);
				if (size == 0) break;
				var b = in.read();
				if (b < 0) break;
				var keyType = b & 0x0F;
				var valType = (b >> 4) & 0x0F;
				for (int i = 0; i < size; i++) {
					skipField(keyType);
					skipField(valType);
				}
				break;
			}
			case ThriftCompactEncoder.STRUCT:
				readStructBegin();
				FieldHeader fh;
				while (!(fh = readFieldHeader()).isStop)
					skipField(fh.type);
				readStructEnd();
				break;
			default:
				break;
		}
	}

	boolean readBool() throws IOException {
		var b = in.read();
		return b == ThriftCompactEncoder.BOOLEAN_TRUE;
	}

	byte readByte() throws IOException {
		var b = in.read();
		return (byte)(b & 0xFF);
	}

	short readI16() throws IOException {
		return (short) fromZigzag(readVarint());
	}

	int readI32() throws IOException {
		return (int) fromZigzag(readVarint());
	}

	long readI64() throws IOException {
		return fromZigzag(readVarint());
	}

	double readDouble() throws IOException {
		var b0 = (long)(in.read() & 0xFF);
		var b1 = (long)(in.read() & 0xFF);
		var b2 = (long)(in.read() & 0xFF);
		var b3 = (long)(in.read() & 0xFF);
		var b4 = (long)(in.read() & 0xFF);
		var b5 = (long)(in.read() & 0xFF);
		var b6 = (long)(in.read() & 0xFF);
		var b7 = (long)(in.read() & 0xFF);
		var bits = b0 | (b1 << 8) | (b2 << 16) | (b3 << 24) | (b4 << 32) | (b5 << 40) | (b6 << 48) | (b7 << 56);
		return Double.longBitsToDouble(bits);
	}

	byte[] readBinary() throws IOException {
		var lenLong = readVarint();
		if (lenLong <= 0)
			return new byte[0];
		if (lenLong > MAX_BINARY_LENGTH)
			throw new IOException("Invalid binary length: " + lenLong);
		var len = (int) lenLong;
		var b = new byte[len];
		var n = 0;
		while (n < len) {
			var r = in.read(b, n, len - n);
			if (r <= 0)
				break;
			n += r;
		}
		return b;
	}

	String readString() throws IOException {
		var b = readBinary();
		return new String(b, StandardCharsets.UTF_8);
	}

	private static final int MAX_LIST_SIZE = 1_000_000;
	private static final long MAX_BINARY_LENGTH = 64L * 1024 * 1024;

	ListHeader readListHeader() throws IOException {
		var b = in.read();
		if (b < 0)
			return new ListHeader(0, 0);
		var elemType = b & 0x0F;
		var size = (b >> 4) & 0x0F;
		if (size == 15)
			size = (int) readVarint();
		if (size < 0 || size > MAX_LIST_SIZE)
			throw new IOException("Invalid list size: " + size);
		return new ListHeader(elemType, size);
	}

	void readStructBegin() {
		lastFieldIdStack.addLast(lastFieldId);
		lastFieldId = 0;
	}

	void readStructEnd() {
		lastFieldId = lastFieldIdStack.isEmpty() ? 0 : lastFieldIdStack.removeLast();
	}

	long readVarint() throws IOException {
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

	static long fromZigzag(long value) {
		return (value >>> 1) ^ -(value & 1);
	}

	int available() throws IOException {
		return in.available();
	}
}
