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
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.cbor.DataType.*;

import java.io.*;

import org.apache.juneau.parser.*;

/**
 * Specialized input stream for parsing CBOR streams (RFC 8949).
 *
 * <p>
 * Reads CBOR-encoded binary data, extracting major types and additional information from
 * initial bytes and decoding values according to RFC 8949.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is not intended for external use.
 * 	<li class='note'>Indefinite-length encoding (break code 0xFF) is not supported and will throw.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/CborBasics">CBOR Basics</a>
 * 	<li class='link'><a class="doclink" href="https://www.rfc-editor.org/rfc/rfc8949.html">RFC 8949</a>
 * </ul>
 */
public class CborInputStream extends ParserInputStream {

	private long length;
	private int lastInitialByte;
	private DataType lastDataType;

	/**
	 * Constructor.
	 *
	 * @param pipe The parser input.
	 * @throws IOException Thrown by underlying stream.
	 */
	protected CborInputStream(ParserPipe pipe) throws IOException {
		super(pipe);
	}

	/**
	 * Reads the initial byte and returns it.
	 *
	 * @return The initial byte (0-255), or -1 on EOF.
	 * @throws IOException If read fails.
	 */
	int readInitialByte() throws IOException {
		int b = read();
		if (b == -1)
			throw ioex("Unexpected end of CBOR input");
		return b & 0xFF;
	}

	/**
	 * Extracts the major type from an initial byte (high 3 bits).
	 *
	 * @param initialByte The initial byte.
	 * @return Major type 0-7.
	 */
	static int getMajorType(int initialByte) {
		return (initialByte >> 5) & 0x07;
	}

	/**
	 * Extracts the additional information from an initial byte (low 5 bits).
	 *
	 * @param initialByte The initial byte.
	 * @return Additional info 0-31.
	 */
	static int getAdditionalInfo(int initialByte) {
		return initialByte & 0x1F;
	}

	/**
	 * Reads the argument based on additional info.
	 * For 0-23, returns additionalInfo; for 24-27, reads 1/2/4/8 bytes big-endian.
	 *
	 * @param additionalInfo The additional info from the initial byte.
	 * @return The argument value.
	 * @throws IOException If read fails.
	 */
	long readArgument(int additionalInfo) throws IOException {
		if (additionalInfo <= 23)
			return additionalInfo;
		if (additionalInfo == 24)
			return readUInt1();
		if (additionalInfo == 25)
			return readUInt2();
		if (additionalInfo == 26)
			return readUInt4();
		if (additionalInfo == 27)
			return readUInt8();
		if (additionalInfo == 31)
			throw ioex("Indefinite-length CBOR encoding not supported");
		throw ioex("Reserved additional info value: {0}", additionalInfo);
	}

	/**
	 * Reads 1 unsigned byte.
	 */
	int readUInt1() throws IOException {
		int b = read();
		if (b == -1)
			throw ioex("Unexpected end of CBOR input");
		return b & 0xFF;
	}

	/**
	 * Reads 2 bytes big-endian as unsigned integer.
	 */
	int readUInt2() throws IOException {
		return (readUInt1() << 8) | readUInt1();
	}

	/**
	 * Reads 4 bytes big-endian as unsigned long.
	 */
	long readUInt4() throws IOException {
		long l = readUInt1();
		l = (l << 8) | readUInt1();
		l = (l << 8) | readUInt1();
		l = (l << 8) | readUInt1();
		return l;
	}

	/**
	 * Reads 8 bytes big-endian as unsigned long.
	 */
	long readUInt8() throws IOException {
		long l = readUInt1();
		l = (l << 8) | readUInt1();
		l = (l << 8) | readUInt1();
		l = (l << 8) | readUInt1();
		l = (l << 8) | readUInt1();
		l = (l << 8) | readUInt1();
		l = (l << 8) | readUInt1();
		l = (l << 8) | readUInt1();
		return l;
	}

	/**
	 * Reads the next CBOR data item's type and length/argument, returning the DataType.
	 * The argument is stored and can be retrieved via {@link #readLength()}.
	 *
	 * @return The DataType of the next item.
	 * @throws IOException If read fails.
	 */
	@SuppressWarnings({
		"java:S3776" // Cognitive complexity acceptable for CBOR data type reading
	})
	DataType readDataType() throws IOException {
		int ib = readInitialByte();
		lastInitialByte = ib;
		int majorType = getMajorType(ib);
		int additionalInfo = getAdditionalInfo(ib);

		DataType dt;
		switch (majorType) {
			case 0 -> dt = UINT;
			case 1 -> dt = NINT;
			case 2 -> dt = BINARY;
			case 3 -> dt = STRING;
			case 4 -> dt = ARRAY;
			case 5 -> dt = MAP;
			case 6 -> dt = TAG;
			case 7 -> {
				switch (additionalInfo) {
					case 20 -> dt = BOOLEAN;
					case 21 -> dt = BOOLEAN;
					case 22 -> dt = NULL;
					case 23 -> dt = UNDEFINED;
					case 25, 26, 27 -> dt = FLOAT;
					case 31 -> dt = BREAK;
					default -> dt = SIMPLE;
				}
			}
			default -> throw ioex("Invalid major type: {0}", majorType);
		}

		lastDataType = dt;
		if (dt == FLOAT)
			length = additionalInfo == 25 ? 2 : additionalInfo == 26 ? 4 : 8;
		else if (dt != BOOLEAN && dt != NULL && dt != UNDEFINED && dt != BREAK)
			length = readArgument(additionalInfo);
		else
			length = dt == BOOLEAN ? -1 : 0;

		return dt;
	}

	/**
	 * Returns the length/argument for the last read data type.
	 * For strings/binary: byte count. For arrays: element count. For maps: pair count. For TAG: tag number.
	 */
	long readLength() {
		return length;
	}

	/**
	 * Reads a boolean (must have been identified as BOOLEAN by readDataType).
	 */
	boolean readBoolean() {
		return lastInitialByte == 0xF5;
	}

	/**
	 * Reads an unsigned integer value (UINT) as long.
	 * The value was already consumed by readDataType/readArgument.
	 */
	long readUnsignedLong() {
		return length;
	}

	/**
	 * Reads a signed integer. For UINT, returns the value. For NINT, returns -1 - value.
	 */
	long readSignedLong() {
		if (lastDataType == UINT)
			return readUnsignedLong();
		if (lastDataType == NINT)
			return -1 - readUnsignedLong();
		throw new IllegalStateException("Expected integer type, got " + lastDataType);
	}

	/**
	 * Reads an int (for UINT/NINT when value fits in int).
	 */
	int readInt() {
		return (int)readSignedLong();
	}

	/**
	 * Reads a long (for UINT/NINT).
	 */
	long readLong() {
		return readSignedLong();
	}

	/**
	 * Reads a float. Handles half (0xF9), single (0xFA), and double (0xFB) precision.
	 */
	float readFloat() throws IOException {
		if (lastDataType == FLOAT) {
			if (lastInitialByte == 0xF9) {
				int half = (readUInt1() << 8) | readUInt1();
				return halfFloatToFloat(half);
			}
			if (lastInitialByte == 0xFA)
				return Float.intBitsToFloat((int)readUInt4());
			if (lastInitialByte == 0xFB)
				return (float)Double.longBitsToDouble(readUInt8());
		}
		throw new IllegalStateException("Expected float type, got " + lastDataType);
	}

	/**
	 * Reads a double. Handles half, single, and double precision.
	 */
	double readDouble() throws IOException {
		if (lastDataType == FLOAT) {
			if (lastInitialByte == 0xF9)
				return halfFloatToFloat((readUInt1() << 8) | readUInt1());
			if (lastInitialByte == 0xFA)
				return Float.intBitsToFloat((int)readUInt4());
			if (lastInitialByte == 0xFB)
				return Double.longBitsToDouble(readUInt8());
		}
		throw new IllegalStateException("Expected float type, got " + lastDataType);
	}

	private static float halfFloatToFloat(int half) {
		int exp = (half >> 10) & 0x1F;
		int mant = half & 0x3FF;
		int sign = (half & 0x8000) << 16;
		if (exp == 0)
			return Float.intBitsToFloat(sign | (mant << 13));
		if (exp == 31)
			return mant == 0 ? Float.intBitsToFloat(sign | 0x7F800000)
				: Float.NaN;
		return Float.intBitsToFloat(sign | ((exp + 112) << 23) | (mant << 13));
	}

	/**
	 * Reads a UTF-8 text string.
	 */
	String readString() throws IOException {
		byte[] b = readBinary();
		return new String(b, UTF8);
	}

	/**
	 * Reads a byte string.
	 */
	byte[] readBinary() throws IOException {
		var b = new byte[(int)length];
		var bytesRead = read(b);
		if (bytesRead != b.length)
			throw ioex("Expected to read {0} bytes but only read {1}", b.length, bytesRead);
		return b;
	}
}
