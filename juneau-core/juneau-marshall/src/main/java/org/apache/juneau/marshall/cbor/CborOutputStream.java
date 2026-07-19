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
package org.apache.juneau.marshall.cbor;

import static org.apache.juneau.commons.utils.IoUtils.*;

import java.io.*;
import java.math.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.marshall.serializer.*;

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
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Cbor">CBOR Basics</a>
 * 	<li class='link'><a class="doclink" href="https://www.rfc-editor.org/rfc/rfc8949.html">RFC 8949</a>
 * </ul>
 */
@SuppressWarnings({
	"resource" // appendXxx(...) methods return this stream for chaining; Eclipse JDT @Owning warning is by design.
})
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
			return appendBigInteger((BigInteger)n);
		if (c == BigDecimal.class)
			return appendString(n.toString());
		return appendInt(0);
	}

	/**
	 * Appends a {@link BigInteger} losslessly.
	 *
	 * <p>
	 * Values within the signed {@code long} range use the compact integer encoding.  Values that
	 * still fit CBOR's native integer range (unsigned 64-bit for positives via major type 0, down to
	 * {@code -2^64} for negatives via major type 1) are emitted with the full 64-bit magnitude rather
	 * than truncating.  Values beyond {@code ±2^64} (which have no native CBOR integer head) fall back
	 * to a lossless decimal string so no magnitude is lost.
	 *
	 * @param value The value.
	 * @return This stream.
	 */
	CborOutputStream appendBigInteger(BigInteger value) {
		if (value.bitLength() <= 63)
			return appendLong(value.longValueExact());
		if (value.signum() > 0) {
			if (value.bitLength() <= 64) {
				write1(0x1B);  // major type 0 | additional info 27 (8-byte argument)
				write8(value.longValue());  // low 64 bits == full unsigned magnitude here
				return this;
			}
		} else {
			var arg = value.negate().subtract(BigInteger.ONE);  // CBOR major type 1 argument == -1 - value
			if (arg.bitLength() <= 64) {
				write1(0x3B);  // major type 1 | additional info 27 (8-byte argument)
				write8(arg.longValue());
				return this;
			}
		}
		// Beyond ±2^64: no native CBOR integer head exists; emit a lossless decimal string.
		return appendString(value.toString());
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
			else if (Character.isHighSurrogate(ch) && i + 1 < len && Character.isLowSurrogate(cs.charAt(i + 1))) {
				count += 4;
				++i;
			} else
				// BMP char, or an unpaired surrogate which is emitted as the 3-byte U+FFFD replacement.
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
			} else if (c >= 0xD800 && c <= 0xDBFF && i + 1 < len && Character.isLowSurrogate(in.charAt(i + 1))) {
				int jchar2 = in.charAt(++i) & 0xFFFF;
				int n = (c << 10) + jchar2 + 0xFCA02400;
				write((byte)(0xF0 + ((n >> 18) & 0x07)));
				write((byte)(0x80 + ((n >> 12) & 0x3F)));
				write((byte)(0x80 + ((n >> 6) & 0x3F)));
				write((byte)(0x80 + (n & 0x3F)));
				count += 4;
			} else {
				// BMP char, or an unpaired surrogate.  An unpaired surrogate is not a valid Unicode
				// scalar value, so emit the U+FFFD replacement character rather than crashing or
				// producing malformed UTF-8.
				if (c >= 0xD800 && c <= 0xDFFF)
					c = 0xFFFD;
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
			throw new SerializeException("Unexpected length.  Expected=%s, Actual=%s", length, length2);
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

	/**
	 * Writes a CBOR simple value (major type 7).
	 *
	 * <p>
	 * Caller responsibility: reserved encodings (20-23 = bool/null/undefined; 25-27 =
	 * float16/32/64; 31 = break) collide with native scalar/structural emits and must not be used
	 * here.
	 *
	 * @param value The simple value (range {@code 0..255}; reserved values noted above).
	 * @return This stream.
	 */
	CborOutputStream writeSimple(int value) {
		writeHead(7, value);
		return this;
	}
}
