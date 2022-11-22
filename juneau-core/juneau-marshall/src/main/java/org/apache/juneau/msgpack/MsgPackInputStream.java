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

import static org.apache.juneau.common.internal.IOUtils.*;
import static org.apache.juneau.msgpack.DataType.*;

import java.io.*;

import org.apache.juneau.parser.*;

/**
 * Specialized input stream for parsing MessagePack streams.
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
public final class MsgPackInputStream extends ParserInputStream {

	private DataType currentDataType;
	private long length;
	private int lastByte;
	private int extType;
	int pos = 0;

	// Data type quick-lookup table.
	private static final DataType[] TYPES = new DataType[] {
		/*0x0?*/ INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,
		/*0x1?*/ INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,
		/*0x2?*/ INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,
		/*0x3?*/ INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,
		/*0x4?*/ INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,
		/*0x5?*/ INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,
		/*0x6?*/ INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,
		/*0x7?*/ INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,
		/*0x8?*/ MAP,MAP,MAP,MAP,MAP,MAP,MAP,MAP,MAP,MAP,MAP,MAP,MAP,MAP,MAP,MAP,
		/*0x9?*/ ARRAY,ARRAY,ARRAY,ARRAY,ARRAY,ARRAY,ARRAY,ARRAY,ARRAY,ARRAY,ARRAY,ARRAY,ARRAY,ARRAY,ARRAY,ARRAY,
		/*0xA?*/ STRING,STRING,STRING,STRING,STRING,STRING,STRING,STRING,STRING,STRING,STRING,STRING,STRING,STRING,STRING,STRING,
		/*0xB?*/ STRING,STRING,STRING,STRING,STRING,STRING,STRING,STRING,STRING,STRING,STRING,STRING,STRING,STRING,STRING,STRING,
		/*0xC?*/ NULL, INVALID, BOOLEAN, BOOLEAN, BIN, BIN, BIN, EXT, EXT, EXT, FLOAT, DOUBLE, INT, INT, LONG, LONG,
		/*0xD?*/ INT, INT, INT, LONG, EXT, EXT, EXT, EXT, EXT, STRING, STRING, STRING, ARRAY, ARRAY, MAP, MAP,
		/*0xE?*/ INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,
		/*0xF?*/ INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT,INT
	};

	/**
	 * Constructor.
	 *
	 * @param pipe The parser input.
	 * @throws IOException Thrown by underlying stream.
	 */
	protected MsgPackInputStream(ParserPipe pipe) throws IOException {
		super(pipe);
	}

	/**
	 * Reads the data type flag from the stream.
	 *
	 * <p>
	 * This is the byte that indicates what kind of data follows.
	 */
	DataType readDataType() throws IOException {
		int i = read();
		if (i == -1)
			throw new IOException("Unexpected end of file found at position "+pos);
		currentDataType = TYPES[i];
		switch (currentDataType) {
			case NULL:
			case FLOAT: {
				length = 4;
				break;
			}
			case DOUBLE: {
				length = 8;
				break;
			}
			case BOOLEAN: {
				lastByte = i;
				break;
			}
			case INT: {
				//	positive fixnum stores 7-bit positive integer
				//	+--------+
				//	|0XXXXXXX|
				//	+--------+
				//
				//	negative fixnum stores 5-bit negative integer
				//	+--------+
				//	|111YYYYY|
				//	+--------+
				//
				//	* 0XXXXXXX is 8-bit unsigned integer
				//	* 111YYYYY is 8-bit signed integer
				//
				//	uint 8 stores a 8-bit unsigned integer
				//	+--------+--------+
				//	|  0xcc  |ZZZZZZZZ|
				//	+--------+--------+
				//
				//	uint 16 stores a 16-bit big-endian unsigned integer
				//	+--------+--------+--------+
				//	|  0xcd  |ZZZZZZZZ|ZZZZZZZZ|
				//	+--------+--------+--------+
				//
				//	uint 32 stores a 32-bit big-endian unsigned integer
				//	+--------+--------+--------+--------+--------+
				//	|  0xce  |ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|
				//	+--------+--------+--------+--------+--------+
				//
				//	uint 64 stores a 64-bit big-endian unsigned integer
				//	+--------+--------+--------+--------+--------+--------+--------+--------+--------+
				//	|  0xcf  |ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|
				//	+--------+--------+--------+--------+--------+--------+--------+--------+--------+
				//
				//	int 8 stores a 8-bit signed integer
				//	+--------+--------+
				//	|  0xd0  |ZZZZZZZZ|
				//	+--------+--------+
				//
				//	int 16 stores a 16-bit big-endian signed integer
				//	+--------+--------+--------+
				//	|  0xd1  |ZZZZZZZZ|ZZZZZZZZ|
				//	+--------+--------+--------+
				//
				//	int 32 stores a 32-bit big-endian signed integer
				//	+--------+--------+--------+--------+--------+
				//	|  0xd2  |ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|
				//	+--------+--------+--------+--------+--------+
				//
				//	int 64 stores a 64-bit big-endian signed integer
				//	+--------+--------+--------+--------+--------+--------+--------+--------+--------+
				//	|  0xd3  |ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|
				//	+--------+--------+--------+--------+--------+--------+--------+--------+--------+
				lastByte = i;
				if (i <= POSFIXINT_U)
					length = 0;
				else if (i >= NEGFIXINT_L)
					length = -1;
				else if (i == INT8 || i == UINT8)
					length = 1;
				else if (i == INT16 || i == UINT16)
					length = 2;
				else if (i == INT32)
					length = 4;
				else
					length = 0;
				break;
			}
			case LONG: {
				if (i == UINT32)
					length = 4;
				else if (i == INT64 || i == UINT64)
					length = 8;
				else
					length = 0;
				break;
			}
			case STRING:{
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
				//
				// where
				// * XXXXX is a 5-bit unsigned integer which represents N
				// * YYYYYYYY is a 8-bit unsigned integer which represents N
				// * ZZZZZZZZ_ZZZZZZZZ is a 16-bit big-endian unsigned integer which represents N
				// * AAAAAAAA_AAAAAAAA_AAAAAAAA_AAAAAAAA is a 32-bit big-endian unsigned integer which represents N
				// * N is the length of data
				if (i <= FIXSTR_U)
					length = i & 0x1F;
				else if (i == STR8)
					length = readUInt1();
				else if (i == STR16)
					length = readUInt2();
				else
					length = readUInt4();
				break;
			}
			case ARRAY: {
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
				if (i <= FIXARRAY_U)
					length = i & 0x0F;
				else if (i == ARRAY16)
					length = readUInt2();
				else
					length = readUInt4();
				break;
			}
			case BIN:{
				//	bin 8 stores a byte array whose length is up to (2^8)-1 bytes:
				//	+--------+--------+========+
				//	|  0xc4  |XXXXXXXX|  data  |
				//	+--------+--------+========+
				//
				//	bin 16 stores a byte array whose length is up to (2^16)-1 bytes:
				//	+--------+--------+--------+========+
				//	|  0xc5  |YYYYYYYY|YYYYYYYY|  data  |
				//	+--------+--------+--------+========+
				//
				//	bin 32 stores a byte array whose length is up to (2^32)-1 bytes:
				//	+--------+--------+--------+--------+--------+========+
				//	|  0xc6  |ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|  data  |
				//	+--------+--------+--------+--------+--------+========+
				//
				//	where
				//	* XXXXXXXX is a 8-bit unsigned integer which represents N
				//	* YYYYYYYY_YYYYYYYY is a 16-bit big-endian unsigned integer which represents N
				//	* ZZZZZZZZ_ZZZZZZZZ_ZZZZZZZZ_ZZZZZZZZ is a 32-bit big-endian unsigned integer which represents N
				//	* N is the length of data
				if (i == BIN8)
					length = readUInt1();
				else if (i == BIN16)
					length = readUInt2();
				else
					length = readUInt4();
				break;
			}
			case EXT:{
				//	fixext 1 stores an integer and a byte array whose length is 1 byte
				//	+--------+--------+--------+
				//	|  0xd4  |  type  |  data  |
				//	+--------+--------+--------+
				//
				//	fixext 2 stores an integer and a byte array whose length is 2 bytes
				//	+--------+--------+--------+--------+
				//	|  0xd5  |  type  |       data      |
				//	+--------+--------+--------+--------+
				//
				//	fixext 4 stores an integer and a byte array whose length is 4 bytes
				//	+--------+--------+--------+--------+--------+--------+
				//	|  0xd6  |  type  |                data               |
				//	+--------+--------+--------+--------+--------+--------+
				//
				//	fixext 8 stores an integer and a byte array whose length is 8 bytes
				//	+--------+--------+--------+--------+--------+--------+--------+--------+--------+--------+
				//	|  0xd7  |  type  |                                  data                                 |
				//	+--------+--------+--------+--------+--------+--------+--------+--------+--------+--------+
				//
				//	fixext 16 stores an integer and a byte array whose length is 16 bytes
				//	+--------+--------+--------+--------+--------+--------+--------+--------+--------+--------+
				//	|  0xd8  |  type  |                                  data
				//	+--------+--------+--------+--------+--------+--------+--------+--------+--------+--------+
				//	+--------+--------+--------+--------+--------+--------+--------+--------+
				//	                              data (cont.)                              |
				//	+--------+--------+--------+--------+--------+--------+--------+--------+
				//
				//	ext 8 stores an integer and a byte array whose length is up to (2^8)-1 bytes:
				//	+--------+--------+--------+========+
				//	|  0xc7  |XXXXXXXX|  type  |  data  |
				//	+--------+--------+--------+========+
				//
				//	ext 16 stores an integer and a byte array whose length is up to (2^16)-1 bytes:
				//	+--------+--------+--------+--------+========+
				//	|  0xc8  |YYYYYYYY|YYYYYYYY|  type  |  data  |
				//	+--------+--------+--------+--------+========+
				//
				//	ext 32 stores an integer and a byte array whose length is up to (2^32)-1 bytes:
				//	+--------+--------+--------+--------+--------+--------+========+
				//	|  0xc9  |ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|  type  |  data  |
				//	+--------+--------+--------+--------+--------+--------+========+
				//
				//	where
				//	* XXXXXXXX is a 8-bit unsigned integer which represents N
				//	* YYYYYYYY_YYYYYYYY is a 16-bit big-endian unsigned integer which represents N
				//	* ZZZZZZZZ_ZZZZZZZZ_ZZZZZZZZ_ZZZZZZZZ is a big-endian 32-bit unsigned integer which represents N
				//	* N is a length of data
				//	* type is a signed 8-bit signed integer
				//	* type < 0 is reserved for future extension including 2-byte type information
				if (i == FIXEXT1)
					length = 1;
				else if (i == FIXEXT2)
					length = 2;
				else if (i == FIXEXT4)
					length = 4;
				else if (i == FIXEXT8)
					length = 8;
				else if (i == FIXEXT16)
					length = 16;
				else if (i == EXT8)
					length = readUInt1();
				else if (i == EXT16)
						length = readUInt2();
				else if (i == EXT32)
					length = readUInt4();
				extType = read();

				break;
			}
			case MAP:{
				//	fixmap stores a map whose length is up to 15 elements
				//	+--------+~~~~~~~~~~~~~~~~~+
				//	|1000XXXX|   N*2 objects   |
				//	+--------+~~~~~~~~~~~~~~~~~+
				//
				//	map 16 stores a map whose length is up to (2^16)-1 elements
				//	+--------+--------+--------+~~~~~~~~~~~~~~~~~+
				//	|  0xde  |YYYYYYYY|YYYYYYYY|   N*2 objects   |
				//	+--------+--------+--------+~~~~~~~~~~~~~~~~~+
				//
				//	map 32 stores a map whose length is up to (2^32)-1 elements
				//	+--------+--------+--------+--------+--------+~~~~~~~~~~~~~~~~~+
				//	|  0xdf  |ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|ZZZZZZZZ|   N*2 objects   |
				//	+--------+--------+--------+--------+--------+~~~~~~~~~~~~~~~~~+
				//
				//	where
				//	* XXXX is a 4-bit unsigned integer which represents N
				//	* YYYYYYYY_YYYYYYYY is a 16-bit big-endian unsigned integer which represents N
				//	* ZZZZZZZZ_ZZZZZZZZ_ZZZZZZZZ_ZZZZZZZZ is a 32-bit big-endian unsigned integer which represents N
				//	* N is the size of a map
				//	* odd elements in objects are keys of a map
				//	* the next element of a key is its associated value
				if (i <= FIXMAP_U)
					length = i & 0x0F;
				else if (i == MAP16)
					length = readUInt2();
				else
					length = readUInt4();
				break;
			}
			default:
				throw new IOException("Invalid flag 0xC1 detected in stream.");
		}
		return currentDataType;
	}

	/**
	 * Returns the length value for the field.
	 *
	 * <p>
	 * For ints/floats/bins/strings, this is the number of bytes that the field takes up (minus the data-type flag).
	 * For arrays, it's the number of array entries.
	 * For maps, it's the number of map entries.
	 */
	long readLength() {
		return length;
	}

	/**
	 * Read a boolean from the stream.
	 */
	boolean readBoolean() {
		return lastByte == TRUE;
	}

	/**
	 * Read a string from the stream.
	 */
	String readString() throws IOException {
		return new String(readBinary(), UTF8);
	}

	/**
	 * Read a binary field from the stream.
	 */
	byte[] readBinary() throws IOException {
		byte[] b = new byte[(int)length];
		read(b);
		return b;
	}

	/**
	 * Read an integer from the stream.
	 */
	int readInt() throws IOException {
		if (length == 0)
			return lastByte;
		if (length == 1)
			return read();
		if (length == 2)
			return (read() << 8) | read();
		int i = read(); i <<= 8; i |= read(); i <<= 8; i |= read(); i <<= 8; i |= read();
		return i;
	}

	/**
	 * Read a float from the stream.
	 */
	float readFloat() throws IOException {
		return Float.intBitsToFloat(readInt());
	}

	/**
	 * Read a double from the stream.
	 */
	double readDouble() throws IOException {
		return Double.longBitsToDouble(readLong());
	}

	/**
	 * Read 64-bit long from the stream.
	 */
	long readLong() throws IOException {
		if (length == 4)
			return readUInt4();
		long l = read(); l <<= 8; l |= read(); l <<= 8; l |= read(); l <<= 8; l |= read(); l <<= 8; l |= read(); l <<= 8; l |= read(); l <<= 8; l |= read(); l <<= 8; l |= read();
		return l;
	}

	/**
	 * Return the extended-format type.
	 * Currently not used.
	 */
	int getExtType() {
		return extType;
	}

	/**
	 * Read one byte from the stream.
	 */
	private int readUInt1() throws IOException {
		return read();
	}

	/**
	 * Read two bytes from the stream.
	 */
	private int readUInt2() throws IOException {
		return (read() << 8) | read();
	}

	/**
	 * Read four bytes from the stream.
	 */
	private long readUInt4() throws IOException {
		long l = read(); l <<= 8; l |= read(); l <<= 8; l |= read(); l <<= 8; l |= read();
		return l;
	}
}
