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
package org.apache.juneau.cbor;

import static org.apache.juneau.commons.utils.IoUtils.*;

import java.io.*;
import java.math.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.serializer.*;

/**
 * Specialized output stream for serializing CBOR streams (RFC 8949).
 *
 * <p>
 * CBOR uses a self-describing binary format where each data item starts with an initial byte
 * containing a 3-bit major type and 5-bit additional information. Integers use compact encoding;
 * strings, arrays, and maps use definite-length encoding.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is not intended for external use.
 * 	<li class='note'>This class is not thread-safe; each serialization session uses its own instance.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/CborBasics">CBOR Basics</a>
 * 	<li class='link'><a class="doclink" href="https://www.rfc-editor.org/rfc/rfc8949.html">RFC 8949</a>
 * </ul>
 */
public class CborOutputStream extends OutputStream {

	private final OutputStream os;

	/**
	 * Constructor.
	 *
	 * @param os The output stream being wrapped.
	 */
	protected CborOutputStream(OutputStream os) {
		this.os = os;
	}

	@Override /* Overridden from OutputStream */
	public void write(int b) {
		try {
			os.write(b);
		} catch (IOException e) {
			throw new SerializeException(e);
		}
	}

	@Override /* Overridden from OutputStream */
	public void write(byte[] b, int off, int len) {
		try {
			os.write(b, off, len);
		} catch (IOException e) {
			throw new SerializeException(e);
		}
	}

	/**
	 * Writes a single byte to the stream.
	 *
	 * @param b The byte to write (lower 8 bits used).
	 */
	void write1(int b) {
		try {
			os.write(b & 0xFF);
		} catch (IOException e) {
			throw new SerializeException(e);
		}
	}

	/**
	 * Writes a 2-byte big-endian unsigned integer.
	 *
	 * @param value The value to write.
	 */
	void write2(int value) {
		write1(value >> 8);
		write1(value);
	}

	/**
	 * Writes a 4-byte big-endian unsigned integer.
	 *
	 * @param value The value to write.
	 */
	void write4(int value) {
		write1(value >> 24);
		write1(value >> 16);
		write1(value >> 8);
		write1(value);
	}

	/**
	 * Writes an 8-byte big-endian unsigned integer.
	 *
	 * @param value The value to write.
	 */
	void write8(long value) {
		write1((int)(value >> 56));
		write1((int)(value >> 48));
		write1((int)(value >> 40));
		write1((int)(value >> 32));
		write1((int)(value >> 24));
		write1((int)(value >> 16));
		write1((int)(value >> 8));
		write1((int)(value));
	}

	/**
	 * Encodes and writes a CBOR initial byte with major type and argument.
	 *
	 * <p>
	 * Argument encoding per RFC 8949:
	 * <ul>
	 * 	<li>0-23: Single byte (majorType &lt;&lt; 5 | argument)
	 * 	<li>24-255: 2 bytes (type | 24, 1-byte argument)
	 * 	<li>256-65535: 3 bytes (type | 25, 2-byte argument)
	 * 	<li>65536-2^32-1: 5 bytes (type | 26, 4-byte argument)
	 * 	<li>2^32 and above: 9 bytes (type | 27, 8-byte argument)
	 * </ul>
	 *
	 * @param majorType The CBOR major type (0-7).
	 * @param argument The additional information or length value.
	 */
	void writeHead(int majorType, long argument) {
		int typeBits = (majorType & 0x07) << 5;
		if (argument <= 23)
			write1(typeBits | (int)argument);
		else if (argument <= 0xFF) {
			write1(typeBits | 24);
			write1((int)argument);
		} else if (argument <= 0xFFFF) {
			write1(typeBits | 25);
			write2((int)argument);
		} else if (argument <= 0xFFFFFFFFL) {
			write1(typeBits | 26);
			write4((int)argument);
		} else {
			write1(typeBits | 27);
			write8(argument);
		}
	}

	/**
	 * Appends CBOR null (0xF6).
	 *
	 * @return This stream.
	 */
	CborOutputStream appendNull() {
		write1(0xF6);
		return this;
	}

	/**
	 * Appends CBOR undefined (0xF7).
	 *
	 * @return This stream.
	 */
	CborOutputStream appendUndefined() {
		write1(0xF7);
		return this;
	}

	/**
	 * Appends a boolean value (0xF4 for false, 0xF5 for true).
	 *
	 * @param value The boolean value.
	 * @return This stream.
	 */
	CborOutputStream appendBoolean(boolean value) {
		write1(value ? 0xF5 : 0xF4);
		return this;
	}

	/**
	 * Appends an integer using CBOR compact encoding.
	 * Positive values use major type 0; negative values use major type 1 with argument -1-value.
	 *
	 * @param value The integer value.
	 * @return This stream.
	 */
	CborOutputStream appendInt(int value) {
		if (value >= 0)
			writeHead(0, value & 0xFFFFFFFFL);
		else
			writeHead(1, -1L - value);
		return this;
	}

	/**
	 * Appends a long integer using CBOR compact encoding.
	 *
	 * @param value The long value.
	 * @return This stream.
	 */
	CborOutputStream appendLong(long value) {
		if (value >= 0)
			writeHead(0, value);
		else
			writeHead(1, -1L - value);
		return this;
	}

	/**
	 * Appends a float as IEEE 754 single-precision (0xFA + 4 bytes).
	 *
	 * @param value The float value.
	 * @return This stream.
	 */
	CborOutputStream appendFloat(float value) {
		write1(0xFA);
		write4(Float.floatToRawIntBits(value));
		return this;
	}

	/**
	 * Appends a double as IEEE 754 double-precision (0xFB + 8 bytes).
	 *
	 * @param value The double value.
	 * @return This stream.
	 */
	CborOutputStream appendDouble(double value) {
		write1(0xFB);
		write8(Double.doubleToRawLongBits(value));
		return this;
	}

	/**
	 * Appends a generic Number, dispatching to the appropriate append method.
	 *
	 * @param n The number.
	 * @return This stream.
	 */
	CborOutputStream appendNumber(Number n) {
		var c = n.getClass();
		if (c == Integer.class || c == Short.class || c == Byte.class || c == AtomicInteger.class)
			return appendInt(n.intValue());
		if (c == Long.class || c == AtomicLong.class)
			return appendLong(n.longValue());
		if (c == Float.class)
			return appendFloat(n.floatValue());
		if (c == Double.class)
			return appendDouble(n.doubleValue());
		if (c == BigInteger.class)
			return appendLong(n.longValue());
		if (c == BigDecimal.class)
			return appendDouble(n.doubleValue());
		return appendInt(0);
	}

	@SuppressWarnings({
		"java:S127" // Loop counter advances for surrogate pairs
	})
	private static int getUtf8ByteLength(CharSequence cs) {
		var count = 0;
		for (int i = 0, len = cs.length(); i < len; i++) {
			var ch = cs.charAt(i);
			if (ch <= 0x7F)
				count++;
			else if (ch <= 0x7FF)
				count += 2;
			else if (Character.isHighSurrogate(ch)) {
				count += 4;
				++i;
			} else
				count += 3;
		}
		return count;
	}

	@SuppressWarnings({
		"java:S127"  // For-loop counter modification acceptable in this algorithm
	})
	private int writeUtf8To(CharSequence in) {
		var count = 0;
		for (int i = 0, len = in.length(); i < len; i++) {
			var c = (in.charAt(i) & 0xFFFF);
			if (c <= 0x7F) {
				write((byte)(c & 0xFF));
				count++;
			} else if (c <= 0x7FF) {
				write((byte)(0xC0 + ((c >> 6) & 0x1F)));
				write((byte)(0x80 + (c & 0x3F)));
				count += 2;
			} else if (c >= 0xD800 && c <= 0xDFFF) {
				int jchar2 = in.charAt(++i) & 0xFFFF;
				int n = (c << 10) + jchar2 + 0xFCA02400;
				write((byte)(0xF0 + ((n >> 18) & 0x07)));
				write((byte)(0x80 + ((n >> 12) & 0x3F)));
				write((byte)(0x80 + ((n >> 6) & 0x3F)));
				write((byte)(0x80 + (n & 0x3F)));
				count += 4;
			} else {
				write((byte)(0xE0 + ((c >> 12) & 0x0F)));
				write((byte)(0x80 + ((c >> 6) & 0x3F)));
				write((byte)(0x80 + (c & 0x3F)));
				count += 3;
			}
		}
		return count;
	}

	/**
	 * Appends a UTF-8 text string (major type 3).
	 *
	 * @param s The string.
	 * @return This stream.
	 */
	CborOutputStream appendString(CharSequence s) {
		int length = getUtf8ByteLength(s);
		writeHead(3, length);
		int length2 = writeUtf8To(s);
		if (length != length2)
			throw new SerializeException("Unexpected length.  Expected={0}, Actual={1}", length, length2);
		return this;
	}

	/**
	 * Appends a byte string (major type 2).
	 *
	 * @param data The bytes.
	 * @return This stream.
	 */
	CborOutputStream appendBinary(byte[] data) {
		writeHead(2, data.length);
		try {
			os.write(data);
		} catch (IOException e) {
			throw new SerializeException(e);
		}
		return this;
	}

	/**
	 * Appends a byte string from an input stream.
	 *
	 * @param data The input stream.
	 * @return This stream.
	 */
	CborOutputStream appendBinary(InputStream data) {
		var baos = new ByteArrayOutputStream();
		pipe(data, baos, x -> {
			throw new SerializeException(x);
		});
		return appendBinary(baos.toByteArray());
	}

	/**
	 * Starts a definite-length array (major type 4).
	 *
	 * @param size The number of elements.
	 * @return This stream.
	 */
	CborOutputStream startArray(int size) {
		writeHead(4, size);
		return this;
	}

	/**
	 * Starts a definite-length map (major type 5).
	 *
	 * @param size The number of key-value pairs.
	 * @return This stream.
	 */
	CborOutputStream startMap(int size) {
		writeHead(5, size);
		return this;
	}

	/**
	 * Writes a CBOR semantic tag (major type 6).
	 *
	 * @param tagNumber The tag number.
	 * @return This stream.
	 */
	CborOutputStream writeTag(long tagNumber) {
		writeHead(6, tagNumber);
		return this;
	}
}
