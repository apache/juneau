// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.msgpack;

import static org.apache.juneau.msgpack.DataType.*;

import java.io.*;
import java.math.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.common.internal.*;
import org.apache.juneau.serializer.*;

/**
 * Specialized output stream for serializing MessagePack streams.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>
 * 		This class is not intended for external use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.MsgPackDetails">MessagePack Details</a>
 * </ul>
 */
public final class MsgPackOutputStream extends OutputStream {

	private final OutputStream os;

	/**
	 * Constructor.
	 *
	 * @param os The output stream being wrapped.
	 */
	protected MsgPackOutputStream(OutputStream os) {
		this.os = os;
	}

	@Override /* OutputStream */
	public void write(int b) {
		try {
			os.write(b);
		} catch (IOException e) {
			throw new SerializeException(e);
		}
	}

	/**
	 * Same as {@link #write(int)}.
	 */
	final MsgPackOutputStream append(byte b) {
		try {
			os.write(b);
		} catch (IOException e) {
			throw new SerializeException(e);
		}
		return this;
	}

	/**
	 * Same as {@link #write(byte[])}.
	 */
	final MsgPackOutputStream append(byte[] b) {
		try {
			os.write(b);
		} catch (IOException e) {
			throw new SerializeException(e);
		}
		return this;
	}

	/**
	 * Appends one byte to the stream.
	 */
	final MsgPackOutputStream append1(int i) {
		try {
			os.write(i);
		} catch (IOException e) {
			throw new SerializeException(e);
		}
		return this;
	}

	/**
	 * Appends two bytes to the stream.
	 */
	final MsgPackOutputStream append2(int i) {
		return append1(i>>8).append1(i);
	}

	/**
	 * Appends four bytes to the stream.
	 */
	final MsgPackOutputStream append4(int i) {
		return append1(i>>24).append1(i>>16).append1(i>>8).append1(i);
	}

	/**
	 * Appends eight bytes to the stream.
	 */
	final MsgPackOutputStream append8(long l) {
		return append1((int)(l>>56)).append1((int)(l>>48)).append1((int)(l>>40)).append1((int)(l>>32)).append1((int)(l>>24)).append1((int)(l>>16)).append1((int)(l>>8)).append1((int)(l));
	}

	/**
	 * Appends a NULL flag to the stream.
	 */
	final MsgPackOutputStream appendNull() {
		return append1(NIL);
	}

	/**
	 * Appends a boolean to the stream.
	 */
	final MsgPackOutputStream appendBoolean(boolean b) {
		return append1(b ? TRUE : FALSE);
	}

	/**
	 * Appends an integer to the stream.
	 */
	final MsgPackOutputStream appendInt(int i) {
		// POSFIXINT_L  = 0x00,  //   pos fixint     0xxxxxxx     0x00 - 0x7f
		// POSFIXINT_U  = 0x7F,
		// UINT8        = 0xCC,  //   uint 8         11001100     0xcc
		// UINT16       = 0xCD,  //   uint 16        11001101     0xcd
		// UINT32       = 0xCE,  //   uint 32        11001110     0xce
		// UINT64       = 0xCF,  //   uint 64        11001111     0xcf
		// INT8         = 0xD0,  //   int 8          11010000     0xd0
		// INT16        = 0xD1,  //   int 16         11010001     0xd1
		// INT32        = 0xD2,  //   int 32         11010010     0xd2
		// INT64        = 0xD3,  //   int 64         11010011     0xd3
		// NEGFIXINT_L  = 0xE0,  //   neg fixint     111xxxxx     0xe0 - 0xff
		// NEGFIXINT_U  = 0xFF;
		if (i >= 0) {
			if (i < (1<<7))
				return append1(i);
			if (i < (1<<15))
				return append1(INT16).append2(i);
			return append1(INT32).append4(i);
		}
		if (i > -(1<<6))
			return append((byte)(0xE0 | -i));
		if (i > -(1<<7))
			return append1(INT8).append1(i);
		if (i > -(1<<15))
			return append1(INT16).append2(i);
		return append1(INT32).append4(i);
	}

	final long L2X31 = ((long)(1<<30))*2;

	/**
	 * Appends a long to the stream.
	 */
	final MsgPackOutputStream appendLong(long l) {
		if (l < L2X31 && l > -(L2X31))
			return appendInt((int)l);
		return append1(INT64).append8(l);
	}

	/**
	 * Appends a generic Number to the stream.
	 */
	final MsgPackOutputStream appendNumber(Number n) {
		Class<?> c = n.getClass();
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

	/**
	 * Appends a float to the stream.
	 */
	final MsgPackOutputStream appendFloat(float f) {
		// FLOAT32      = 0xCA,  //   float 32       11001010     0xca
		return append1(FLOAT32).append4(Float.floatToIntBits(f));

	}

	/**
	 * Appends a double to the stream.
	 */
	final MsgPackOutputStream appendDouble(double d) {
		// FLOAT64      = 0xCB,  //   float 64       11001011     0xcb
		return append1(FLOAT64).append8(Double.doubleToLongBits(d));
	}

	/**
	 * Appends a string to the stream.
	 */
	final MsgPackOutputStream appendString(CharSequence cs) {

		// fixstr stores a byte array whose length is up to 31 bytes:
		// +--------+========+
		// |101XXXXX|  data  |
		// +--------+========+
		//
		// str 8 stores a byte array whose length is up to (2^8)-1 bytes:
		// +--------+--------+========+
		// |  0xd9  |YYYYYYYY|  data  |
		// +--------+--------+========+
		//
		// str 16 stores a byte array whose length is up to (2^16)-1 bytes:
		// +--------+--------+--------+========+
		// |  0xda  |ZZZZZZZZ|ZZZZZZZZ|  data  |
		// +--------+--------+--------+========+
		//
		// str 32 stores a byte array whose length is up to (2^32)-1 bytes:
		// +--------+--------+--------+--------+--------+========+
		// |  0xdb  |AAAAAAAA|AAAAAAAA|AAAAAAAA|AAAAAAAA|  data  |
		// +--------+--------+--------+--------+--------+========+
		// where
		// * XXXXX is a 5-bit unsigned integer which represents N
		// * YYYYYYYY is a 8-bit unsigned integer which represents N
		// * ZZZZZZZZ_ZZZZZZZZ is a 16-bit big-endian unsigned integer which represents N
		// * AAAAAAAA_AAAAAAAA_AAAAAAAA_AAAAAAAA is a 32-bit big-endian unsigned integer which represents N
		// * N is the length of data

		int length = getUtf8ByteLength(cs);
		if (length < 32)
			append1(0xA0 + length);
		else if (length < (1<<8))
			append1(STR8).append1(length);
		else if (length < (1<<16))
			append1(STR16).append2(length);
		else
			append1(STR32).append4(length);

		int length2 = writeUtf8To(cs, os);

		if (length != length2)
			throw new SerializeException("Unexpected length.  Expected={0}, Actual={1}", length, length2);

		return this;
	}

	/**
	 * Appends a binary field to the stream.
	 */
	final MsgPackOutputStream appendBinary(byte[] b) {
		// bin 8 stores a byte array whose length is up to (2^8)-1 bytes:
		// +--------+--------+========+
		// |  0xc4  |XXXXXXXX|  data  |
		// +--------+--------+========+
		//
		// bin 16 stores a byte array whose length is up to (2^16)-1 bytes:
		// +--------+--------+--------+========+
		// |  0xc5  |YYYYYYYY|YYYYYYYY|  data  |
		// +--------+--------+--------+========+
		//
		// bin 32 stores a byte array whose length is up to (2^32)-1 bytes:
		// +--------+--------+--------+--------+--------+========+
		// |  0xc6  |ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|  data  |
		// +--------+--------+--------+--------+--------+========+
		//
		// where
		// * XXXXXXXX is a 8-bit unsigned integer which represents N
		// * YYYYYYYY_YYYYYYYY is a 16-bit big-endian unsigned integer which represents N
		// * ZZZZZZZZ_ZZZZZZZZ_ZZZZZZZZ_ZZZZZZZZ is a 32-bit big-endian unsigned integer which represents N
		// * N is the length of data

		if (b.length < (1<<8))
			return append1(BIN8).append1(b.length).append(b);
		if (b.length < (1<<16))
			return append1(BIN16).append2(b.length).append(b);
		return append1(BIN32).append4(b.length).append(b);
	}

	/**
	 * Appends a binary field to the stream.
	 */
	final MsgPackOutputStream appendBinary(InputStream is) {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		IOUtils.pipe(is, baos, x -> { throw new SerializeException(x); });

		byte[] b = baos.toByteArray();

		// bin 8 stores a byte array whose length is up to (2^8)-1 bytes:
		// +--------+--------+========+
		// | 0xc4   |XXXXXXXX|  data  |
		// +--------+--------+========+
		//
		// bin 16 stores a byte array whose length is up to (2^16)-1 bytes:
		// +--------+--------+--------+========+
		// | 0xc5   |YYYYYYYY|YYYYYYYY|  data  |
		// +--------+--------+--------+========+
		//
		// bin 32 stores a byte array whose length is up to (2^32)-1 bytes:
		// +--------+--------+--------+--------+--------+========+
		// | 0xc6   |ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|  data  |
		// +--------+--------+--------+--------+--------+========+
		//
		// where
		// * XXXXXXXX is a 8-bit unsigned integer which represents N
		// * YYYYYYYY_YYYYYYYY is a 16-bit big-endian unsigned integer which represents N
		// * ZZZZZZZZ_ZZZZZZZZ_ZZZZZZZZ_ZZZZZZZZ is a 32-bit big-endian unsigned integer which represents N
		// * N is the length of data

		if (b.length < (1<<8))
			return append1(BIN8).append1(b.length).append(b);
		if (b.length < (1<<16))
			return append1(BIN16).append2(b.length).append(b);
		return append1(BIN32).append4(b.length).append(b);
	}

	/**
	 * Appends an array data type flag to the stream.
	 */
	final MsgPackOutputStream startArray(int size) {
		// fixarray stores an array whose length is up to 15 elements:
		// +--------+~~~~~~~~~~~~~~~~~+
		// |1001XXXX|    N objects    |
		// +--------+~~~~~~~~~~~~~~~~~+
		//
		// array 16 stores an array whose length is up to (2^16)-1 elements:
		// +--------+--------+--------+~~~~~~~~~~~~~~~~~+
		// |  0xdc  |YYYYYYYY|YYYYYYYY|    N objects    |
		// +--------+--------+--------+~~~~~~~~~~~~~~~~~+
		//
		// array 32 stores an array whose length is up to (2^32)-1 elements:
		// +--------+--------+--------+--------+--------+~~~~~~~~~~~~~~~~~+
		// |  0xdd  |ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|    N objects    |
		// +--------+--------+--------+--------+--------+~~~~~~~~~~~~~~~~~+
		//
		// where
		// * XXXX is a 4-bit unsigned integer which represents N
		// * YYYYYYYY_YYYYYYYY is a 16-bit big-endian unsigned integer which represents N
		// * ZZZZZZZZ_ZZZZZZZZ_ZZZZZZZZ_ZZZZZZZZ is a 32-bit big-endian unsigned integer which represents N
		//     N is the size of a array

		if (size < 16)
			return append1(0x90 + size);
		if (size < (1<<16))
			return append1(ARRAY16).append2(size);
		return append1(ARRAY32).append4(size);
	}

	/**
	 * Appends a map data type flag to the stream.
	 */
	final MsgPackOutputStream startMap(int size) {
		// fixmap stores a map whose length is up to 15 elements
		// +--------+~~~~~~~~~~~~~~~~~+
		// |1000XXXX|   N*2 objects   |
		// +--------+~~~~~~~~~~~~~~~~~+
		//
		// map 16 stores a map whose length is up to (2^16)-1 elements
		// +--------+--------+--------+~~~~~~~~~~~~~~~~~+
		// |  0xde  |YYYYYYYY|YYYYYYYY|   N*2 objects   |
		// +--------+--------+--------+~~~~~~~~~~~~~~~~~+
		//
		// map 32 stores a map whose length is up to (2^32)-1 elements
		// +--------+--------+--------+--------+--------+~~~~~~~~~~~~~~~~~+
		// |  0xdf  |ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|   N*2 objects   |
		// +--------+--------+--------+--------+--------+~~~~~~~~~~~~~~~~~+
		//
		// where
		// * XXXX is a 4-bit unsigned integer which represents N
		// * YYYYYYYY_YYYYYYYY is a 16-bit big-endian unsigned integer which represents N
		// * ZZZZZZZZ_ZZZZZZZZ_ZZZZZZZZ_ZZZZZZZZ is a 32-bit big-endian unsigned integer which represents N
		// * N is the size of a map
		// * odd elements in objects are keys of a map
		// * the next element of a key is its associated value

		if (size < 16)
			return append1(0x80 + size);
		if (size < (1<<16))
			return append1(MAP16).append2(size);
		return append1(MAP32).append4(size);
	}

	private int writeUtf8To(CharSequence in, OutputStream out) {
		int count = 0;
		for (int i = 0, len = in.length(); i < len; i++) {
			int c = (in.charAt(i) & 0xFFFF);
			if (c <= 0x7F) {
				write((byte) (c & 0xFF));
				count++;
			} else if (c <= 0x7FF) {
				write((byte) (0xC0 + ((c>>6) & 0x1F)));
				write((byte) (0x80 + (c & 0x3F)));
				count += 2;
			} else if (c >= 0xD800 && c <= 0xDFFF) {
				int jchar2 = in.charAt(++i) & 0xFFFF;
				int n = (c<<10) + jchar2 + 0xFCA02400;
				write((byte) (0xF0 + ((n>>18) & 0x07)));
				write((byte) (0x80 + ((n>>12) & 0x3F)));
				write((byte) (0x80 + ((n>>6) & 0x3F)));
				write((byte) (0x80 + (n & 0x3F)));
				count += 4;
			} else {
				write((byte) (0xE0 + ((c>>12) & 0x0F)));
				write((byte) (0x80 + ((c>>6) & 0x3F)));
				write((byte) (0x80 + (c & 0x3F)));
				count += 3;
			}
		}
		return count;
	}

	private int getUtf8ByteLength(CharSequence cs) {
		int count = 0;
		for (int i = 0, len = cs.length(); i < len; i++) {
			char ch = cs.charAt(i);
			if (ch <= 0x7F) {
				count++;
			} else if (ch <= 0x7FF) {
				count += 2;
			} else if (Character.isHighSurrogate(ch)) {
				count += 4;
				++i;
			} else {
				count += 3;
			}
		}
		return count;
	}

}