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

/**
 * Specialized output stream for serializing MessagePack streams.
 *
 * <ul class='notes'>
 * 	<li>
 * 		This class is not intended for external use.
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
	public void write(int b) throws IOException {
		os.write(b);
	}

	/**
	 * Same as {@link #write(int)}.
	 */
	final MsgPackOutputStream append(byte b) throws IOException {
		os.write(b);
		return this;
	}

	/**
	 * Same as {@link #write(byte[])}.
	 */
	final MsgPackOutputStream append(byte[] b) throws IOException {
		os.write(b);
		return this;
	}

	/**
	 * Appends one byte to the stream.
	 */
	final MsgPackOutputStream append1(int i) throws IOException {
		os.write(i);
		return this;
	}

	/**
	 * Appends two bytes to the stream.
	 */
	final MsgPackOutputStream append2(int i) throws IOException {
		return append1(i>>8).append1(i);
	}

	/**
	 * Appends four bytes to the stream.
	 */
	final MsgPackOutputStream append4(int i) throws IOException {
		return append1(i>>24).append1(i>>16).append1(i>>8).append1(i);
	}

	/**
	 * Appends eight bytes to the stream.
	 */
	final MsgPackOutputStream append8(long l) throws IOException {
		return append1((int)(l>>56)).append1((int)(l>>48)).append1((int)(l>>40)).append1((int)(l>>32)).append1((int)(l>>24)).append1((int)(l>>16)).append1((int)(l>>8)).append1((int)(l));
	}

	/**
	 * Appends a NULL flag to the stream.
	 */
	final MsgPackOutputStream appendNull() throws IOException {
		return append1(NIL);
	}

	/**
	 * Appends a boolean to the stream.
	 */
	final MsgPackOutputStream appendBoolean(boolean b) throws IOException {
		return append1(b ? TRUE : FALSE);
	}

	/**
	 * Appends an integer to the stream.
	 */
	final MsgPackOutputStream appendInt(int i) throws IOException {
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
	final MsgPackOutputStream appendLong(long l) throws IOException {
		if (l < L2X31 && l > -(L2X31))
			return appendInt((int)l);
		return append1(INT64).append8(l);
	}

	/**
	 * Appends a generic Number to the stream.
	 */
	final MsgPackOutputStream appendNumber(Number n) throws IOException {
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
	final MsgPackOutputStream appendFloat(float f) throws IOException {
		// FLOAT32      = 0xCA,  //   float 32       11001010     0xca
		return append1(FLOAT32).append4(Float.floatToIntBits(f));

	}

	/**
	 * Appends a double to the stream.
	 */
	final MsgPackOutputStream appendDouble(double d) throws IOException {
		// FLOAT64      = 0xCB,  //   float 64       11001011     0xcb
		return append1(FLOAT64).append8(Double.doubleToLongBits(d));
	}

	/**
	 * Appends a string to the stream.
	 */
	final MsgPackOutputStream appendString(CharSequence cs) throws IOException {

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

		byte[] b = cs.toString().getBytes("UTF-8");
		if (b.length < 32)
			return append1(0xA0 + b.length).append(b);
		if (b.length < (1<<8))
			return append1(STR8).append1(b.length).append(b);
		if (b.length < (1<<16))
			return append1(STR16).append2(b.length).append(b);
		return append1(STR32).append4(b.length).append(b);
	}

	/**
	 * Appends a binary field to the stream.
	 */
	final MsgPackOutputStream appendBinary(byte[] b) throws IOException {
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
	 * Appends an array data type flag to the stream.
	 */
	final MsgPackOutputStream startArray(int size) throws IOException {
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
	final MsgPackOutputStream startMap(int size) throws IOException {
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
}