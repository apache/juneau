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
package org.apache.juneau.bson;

import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.io.*;
import java.math.*;
import java.nio.charset.*;

import org.apache.juneau.parser.*;

/**
 * Specialized input stream for parsing BSON (Binary JSON) streams.
 *
 * <p>
 * BSON uses little-endian byte order for all multi-byte values.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>
 * 		This class is not intended for external use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/BsonBasics">BSON Basics</a>
 * </ul>
 */
public class BsonInputStream extends ParserInputStream {

	private static final Charset UTF8 = StandardCharsets.UTF_8;

	private int pushback = -1;

	/**
	 * Constructor.
	 *
	 * @param pipe The parser input.
	 * @throws IOException Thrown by underlying stream.
	 */
	protected BsonInputStream(ParserPipe pipe) throws IOException {
		super(pipe);
	}

	@Override
	public int read() throws IOException {
		if (pushback >= 0) {
			var b = pushback;
			pushback = -1;
			return b;
		}
		return super.read();
	}

	/**
	 * Pushes back one byte for the next read.
	 */
	private void pushBack(int b) {
		pushback = b & 0xFF;
	}

	/**
	 * Reads a 4-byte little-endian integer.
	 *
	 * @return The 32-bit value.
	 * @throws IOException If the stream ends prematurely.
	 */
	public int readLE4() throws IOException {
		var b1 = read();
		var b2 = read();
		var b3 = read();
		var b4 = read();
		if (b4 < 0)
			throw ioex("Unexpected end of BSON stream");
		return (b1 & 0xFF) | ((b2 & 0xFF) << 8) | ((b3 & 0xFF) << 16) | ((b4 & 0xFF) << 24);
	}

	/**
	 * Reads an 8-byte little-endian long.
	 *
	 * @return The 64-bit value.
	 * @throws IOException If the stream ends prematurely.
	 */
	public long readLE8() throws IOException {
		var lo = readLE4() & 0xFFFFFFFFL;
		var hi = readLE4() & 0xFFFFFFFFL;
		return lo | (hi << 32);
	}

	/**
	 * Reads the document size (int32) from the stream.
	 *
	 * @return The document size in bytes.
	 * @throws IOException If the stream ends prematurely.
	 */
	public int readDocumentSize() throws IOException {
		return readLE4();
	}

	/**
	 * Reads the 1-byte element type.
	 *
	 * @return The BSON type byte (0x01-0x13, 0x7F, 0xFF).
	 * @throws IOException If the stream ends prematurely.
	 */
	public int readElementType() throws IOException {
		var b = read();
		if (b < 0)
			throw ioex("Unexpected end of BSON stream");
		return b & 0xFF;
	}

	/**
	 * Reads a cstring (null-terminated UTF-8).
	 *
	 * @return The element name.
	 * @throws IOException If the stream ends prematurely.
	 */
	public String readElementName() throws IOException {
		return readCString();
	}

	/**
	 * Reads a cstring (UTF-8 bytes until 0x00).
	 *
	 * @return The string value.
	 * @throws IOException If the stream ends prematurely.
	 */
	public String readCString() throws IOException {
		var baos = new ByteArrayOutputStream();
		int b;
		while ((b = read()) >= 0 && b != 0)
			baos.write(b);
		if (b < 0)
			throw ioex("Unexpected end of BSON stream");
		return new String(baos.toByteArray(), UTF8);
	}

	/**
	 * Returns whether the next byte is the document terminator (0x00).
	 *
	 * @return <jk>true</jk> if next byte is 0x00, <jk>false</jk> otherwise.
	 * @throws IOException If the stream ends prematurely.
	 */
	public boolean isDocumentEnd() throws IOException {
		var b = read();
		if (b >= 0)
			pushBack(b);
		return b == 0x00;
	}

	/**
	 * Consumes the document terminator byte (0x00).
	 *
	 * @throws IOException If the stream ends prematurely or byte is not 0x00.
	 */
	public void readDocumentTerminator() throws IOException {
		var b = read();
		if (b != 0x00)
			throw ioex("Expected document terminator, got {0}", b);
	}

	/**
	 * Reads an IEEE 754 double (8 bytes, little-endian).
	 *
	 * @return The double value.
	 * @throws IOException If the stream ends prematurely.
	 */
	public double readDouble() throws IOException {
		return Double.longBitsToDouble(readLE8());
	}

	/**
	 * Reads a 32-bit integer (4 bytes, little-endian).
	 *
	 * @return The integer value.
	 * @throws IOException If the stream ends prematurely.
	 */
	public int readInt32() throws IOException {
		return readLE4();
	}

	/**
	 * Reads a 64-bit integer (8 bytes, little-endian).
	 *
	 * @return The long value.
	 * @throws IOException If the stream ends prematurely.
	 */
	public long readInt64() throws IOException {
		return readLE8();
	}

	/**
	 * Reads a boolean (1 byte: 0x00=false, 0x01=true).
	 *
	 * @return The boolean value.
	 * @throws IOException If the stream ends prematurely.
	 */
	public boolean readBoolean() throws IOException {
		var b = read();
		if (b < 0)
			throw ioex("Unexpected end of BSON stream");
		return b != 0;
	}

	/**
	 * Reads a BSON string (int32 length + UTF-8 + 0x00).
	 *
	 * @return The string value.
	 * @throws IOException If the stream ends prematurely.
	 */
	public String readString() throws IOException {
		var len = readLE4();
		if (len <= 0)
			throw ioex("Invalid BSON string length: {0}", len);
		var bytes = new byte[len - 1]; // length includes null
		for (var i = 0; i < bytes.length; i++) {
			var b = read();
			if (b < 0)
				throw ioex("Unexpected end of BSON stream");
			bytes[i] = (byte)b;
		}
		var term = read();
		if (term != 0x00)
			throw ioex("Expected string terminator, got {0}", term);
		return new String(bytes, UTF8);
	}

	/**
	 * Reads BSON binary (int32 length + subtype + bytes).
	 *
	 * @return The binary data.
	 * @throws IOException If the stream ends prematurely.
	 */
	public byte[] readBinary() throws IOException {
		var len = readLE4();
		var subtype = read();
		if (subtype < 0)
			throw ioex("Unexpected end of BSON stream");
		var bytes = new byte[len];
		for (var i = 0; i < len; i++) {
			var b = read();
			if (b < 0)
				throw ioex("Unexpected end of BSON stream");
			bytes[i] = (byte)b;
		}
		return bytes;
	}

	/**
	 * Reads BSON datetime (int64, UTC millis since epoch).
	 *
	 * @return The UTC millis since epoch.
	 * @throws IOException If the stream ends prematurely.
	 */
	public long readDateTime() throws IOException {
		return readLE8();
	}

	/**
	 * Reads ObjectId (12 bytes) as 24-character hex string.
	 *
	 * @return The ObjectId as hex string.
	 * @throws IOException If the stream ends prematurely.
	 */
	public String readObjectId() throws IOException {
		var bytes = new byte[12];
		for (var i = 0; i < 12; i++) {
			var b = read();
			if (b < 0)
				throw ioex("Unexpected end of BSON stream");
			bytes[i] = (byte)b;
		}
		var sb = new StringBuilder(24);
		for (var b : bytes)
			sb.append(String.format("%02x", b & 0xFF));
		return sb.toString();
	}

	/**
	 * Reads BSON decimal128 (16 bytes, IEEE 754-2008) as BigDecimal.
	 *
	 * @return The decimal value.
	 * @throws IOException If the stream ends prematurely.
	 */
	public BigDecimal readDecimal128() throws IOException {
		var bytes = new byte[16];
		for (var i = 0; i < 16; i++) {
			var b = read();
			if (b < 0)
				throw ioex("Unexpected end of BSON stream");
			bytes[i] = (byte)b;
		}
		return decodeDecimal128(bytes);
	}

	/**
	 * Skips a value of the given BSON type.
	 *
	 * @param type The BSON type byte (0x01-0x13, 0x7F, 0xFF).
	 * @throws IOException If the stream ends prematurely.
	 */
	public void skipValue(int type) throws IOException {
		switch (type) {
			case 0x01 -> readLE8();
			case 0x02 -> {
				var len = readLE4();
				skip(len);
			}
			case 0x03, 0x04 -> {
				var size = readLE4();
				skip(size - 4);
			}
			case 0x05 -> {
				var len = readLE4();
				read();
				skip(len);
			}
			case 0x06 -> { }
			case 0x07 -> skip(12);
			case 0x08 -> read();
			case 0x09 -> readLE8();
			case 0x0A -> { }
			case 0x0B -> {
				readCString();
				readCString();
			}
			case 0x0C -> {
				readString();
				skip(12);
			}
			case 0x0D -> readString();
			case 0x0E -> readString();
			case 0x0F -> {
				var len = readLE4();
				skip(len - 4);
			}
			case 0x10 -> readLE4();
			case 0x11 -> readLE8();
			case 0x12 -> readLE8();
			case 0x13 -> skip(16);
			case 0x7F, 0xFF -> { }
			default -> throw ioex("Unknown BSON type: {0}", type);
		}
	}

	private void skip(int n) throws IOException {
		for (var i = 0; i < n; i++)
			if (read() < 0)
				throw ioex("Unexpected end of BSON stream");
	}

	/**
	 * Decodes IEEE 754-2008 decimal128 bytes to BigDecimal.
	 *
	 * <p>
	 * Bytes are little-endian: bytes 0-7 = low 64 bits, bytes 8-15 = high 64 bits.
	 * Matches {@link BsonOutputStream#writeDecimal128} output.
	 */
	private static BigDecimal decodeDecimal128(byte[] bytes) {
		if (bytes.length != 16)
			throw new IllegalArgumentException("Decimal128 must be 16 bytes");
		var low = readLE8From(bytes, 0);
		var high = readLE8From(bytes, 8);
		return BsonDecimal128.fromIEEE754BIDEncoding(high, low).toBigDecimal();
	}

	private static long readLE8From(byte[] bytes, int offset) {
		return ((long)(bytes[offset + 7] & 0xFF) << 56) | ((long)(bytes[offset + 6] & 0xFF) << 48)
			| ((long)(bytes[offset + 5] & 0xFF) << 40) | ((long)(bytes[offset + 4] & 0xFF) << 32)
			| ((long)(bytes[offset + 3] & 0xFF) << 24) | ((long)(bytes[offset + 2] & 0xFF) << 16)
			| ((long)(bytes[offset + 1] & 0xFF) << 8) | (bytes[offset] & 0xFF);
	}
}
