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
package org.apache.juneau.marshall.protobuf;

import static org.apache.juneau.commons.utils.IoUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;

import org.apache.juneau.marshall.parser.*;

/**
 * Low-level byte reader for the protobuf binary wire format.
 *
 * <p>
 * Wraps an {@link InputStream} and decodes the primitives that make up a protobuf message:  varints,
 * zigzag varints, little-endian fixed32/fixed64, field tags, and length-delimited blocks.  It can also
 * {@link #skipField(WireType) skip} an unknown field by consuming exactly the right number of bytes for its
 * wire type.
 *
 * <p>
 * Embedded sub-messages and packed repeated fields are decoded by reading the length-delimited block via
 * {@link #readLenDelimited()} and constructing a new {@code ProtobufReader} over those bytes.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is not intended for external use.
 * 	<li class='note'>This class is not thread-safe; each parse session uses its own instance.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Protobuf">Protobuf Binary Format Basics</a>
 * 	<li class='link'><a class="doclink" href="https://protobuf.dev/programming-guides/encoding/">Protocol Buffers Encoding</a>
 * </ul>
 */
@SuppressWarnings({
	"resource" // The wrapped InputStream is owned by the caller/session; Eclipse JDT's resource-leak/@Owning analysis flags the field, but this reader does not own or close the stream.
})
public class ProtobufReader {

	/** Sentinel returned by {@link #readTag()} when the stream is exhausted. */
	public static final long EOF = -1L;

	/** Default cap (16 MiB) for wire-declared length-delimited block sizes. */
	static final int DEFAULT_MAX_LENGTH = 16 * 1024 * 1024;

	/** Maximum encoded length, in bytes, of a 64-bit protobuf varint (ceil(64 / 7) LEB128 groups). */
	private static final int MAX_VARINT_BYTES = 10;

	private final InputStream is;
	private int maxLength = DEFAULT_MAX_LENGTH;

	/**
	 * Constructor.
	 *
	 * @param is The input stream being wrapped.  Must not be <jk>null</jk>.
	 */
	public ProtobufReader(InputStream is) {
		this.is = is;
	}

	/**
	 * Constructor over an in-memory byte block (used for embedded messages and packed fields).
	 *
	 * @param bytes The bytes to read.  Must not be <jk>null</jk>.
	 */
	public ProtobufReader(byte[] bytes) {
		this(new ByteArrayInputStream(bytes));
	}

	/**
	 * Sets the maximum allowed wire-declared length (in bytes) for length-delimited blocks.
	 *
	 * <p>
	 * Guards against malformed input where a small payload declares a huge length that would otherwise
	 * trigger {@link OutOfMemoryError} or {@link NegativeArraySizeException}.
	 *
	 * @param value The maximum length in bytes.  Values &le; 0 disable the cap (only the negative-length check remains).
	 */
	public void setMaxLength(int value) {
		maxLength = value <= 0 ? Integer.MAX_VALUE : value;
	}

	private int read() throws IOException {
		return is.read();
	}

	/**
	 * Returns the number of bytes that can be read without blocking.
	 *
	 * <p>
	 * Used to drive packed-repeated decode loops over in-memory sub-blocks (which always wrap a
	 * {@link ByteArrayInputStream}, so this returns the exact number of bytes remaining).
	 *
	 * @return The number of bytes remaining.
	 * @throws IOException If the underlying stream fails.
	 */
	public int available() throws IOException {
		return is.available();
	}

	/**
	 * Reads a base-128 varint (LEB128) as a 64-bit value.
	 *
	 * <p>
	 * Caps the encoding at {@value #MAX_VARINT_BYTES} bytes (the maximum a 64-bit varint can occupy), so a
	 * malformed stream of continuation bytes cannot burn unbounded CPU/bytes before EOF.
	 *
	 * @return The decoded value.
	 * @throws IOException If the stream ends mid-varint, the varint exceeds {@value #MAX_VARINT_BYTES} bytes,
	 * 	or the underlying stream fails.
	 */
	public long readVarint() throws IOException {
		var result = 0L;
		var shift = 0;
		var count = 0;
		int b;
		do {
			b = read();
			if (b == -1)
				throw ioex("Unexpected end of protobuf input while reading varint");
			count++;
			result |= ((long)(b & 0x7F)) << shift;
			shift += 7;
			if (count == MAX_VARINT_BYTES && (b & 0x80) != 0)
				throw ioex("Protobuf varint exceeds maximum length of %s bytes", MAX_VARINT_BYTES);
		} while ((b & 0x80) != 0);
		return result;
	}

	/**
	 * Reads a field tag, or returns {@link #EOF} if the stream is exhausted at a field boundary.
	 *
	 * <p>
	 * Caps the encoding at {@value #MAX_VARINT_BYTES} bytes, the same bound applied by {@link #readVarint()}.
	 *
	 * @return The decoded tag value (<c>(fieldNumber &lt;&lt; 3) | wireType</c>), or {@link #EOF} at end of stream.
	 * @throws IOException If the stream ends mid-tag, the tag exceeds {@value #MAX_VARINT_BYTES} bytes, or the
	 * 	underlying stream fails.
	 */
	public long readTag() throws IOException {
		var b = read();
		if (b == -1)
			return EOF;
		var result = (long)(b & 0x7F);
		var shift = 7;
		var count = 1;
		while ((b & 0x80) != 0) {
			b = read();
			if (b == -1)
				throw ioex("Unexpected end of protobuf input while reading tag");
			count++;
			result |= ((long)(b & 0x7F)) << shift;
			shift += 7;
			if (count == MAX_VARINT_BYTES && (b & 0x80) != 0)
				throw ioex("Protobuf varint exceeds maximum length of %s bytes", MAX_VARINT_BYTES);
		}
		return result;
	}

	/**
	 * Returns the field number encoded in a tag.
	 *
	 * @param tag The tag value.
	 * @return The field number.
	 */
	public static int fieldNumber(long tag) {
		return (int)(tag >>> 3);
	}

	/**
	 * Returns the wire type encoded in a tag.
	 *
	 * @param tag The tag value.
	 * @return The wire type.
	 */
	public static WireType wireType(long tag) {
		return WireType.fromCode((int)(tag & 0x07));
	}

	/**
	 * Reads a 32-bit zigzag-encoded signed integer.
	 *
	 * @return The decoded value.
	 * @throws IOException If the underlying stream fails.
	 */
	public int readZigZag32() throws IOException {
		var raw = (int)readVarint();
		return (raw >>> 1) ^ -(raw & 1);
	}

	/**
	 * Reads a 64-bit zigzag-encoded signed integer.
	 *
	 * @return The decoded value.
	 * @throws IOException If the underlying stream fails.
	 */
	public long readZigZag64() throws IOException {
		var raw = readVarint();
		return (raw >>> 1) ^ -(raw & 1);
	}

	/**
	 * Reads a 4-byte little-endian fixed value.
	 *
	 * @return The decoded 32 bits.
	 * @throws IOException If the stream ends early or the underlying stream fails.
	 */
	public int readFixed32() throws IOException {
		var b0 = readByte();
		var b1 = readByte();
		var b2 = readByte();
		var b3 = readByte();
		return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
	}

	/**
	 * Reads an 8-byte little-endian fixed value.
	 *
	 * @return The decoded 64 bits.
	 * @throws IOException If the stream ends early or the underlying stream fails.
	 */
	public long readFixed64() throws IOException {
		var result = 0L;
		for (var i = 0; i < 8; i++)
			result |= ((long)readByte()) << (8 * i);
		return result;
	}

	private int readByte() throws IOException {
		var b = read();
		if (b == -1)
			throw ioex("Unexpected end of protobuf input");
		return b & 0xFF;
	}

	/**
	 * Reads a length-delimited block:  a varint length followed by that many bytes.
	 *
	 * <p>
	 * The declared length is validated against the configured maximum before any buffer is allocated, so a
	 * small payload declaring a huge (or wrapped-negative) length is rejected up front rather than driving a
	 * large allocation.
	 *
	 * @return The block bytes.
	 * @throws IOException If the stream ends early, the declared length is out of bounds, or the underlying stream fails.
	 */
	public byte[] readLenDelimited() throws IOException {
		var len = ParserInputStream.checkLength(readVarint(), maxLength, "protobuf field");
		var b = new byte[len];
		var off = 0;
		while (off < len) {
			var r = is.read(b, off, len - off);
			if (r == -1)
				throw ioex("Expected to read %s bytes but stream ended at %s", len, off);
			off += r;
		}
		return b;
	}

	/**
	 * Reads a length-delimited block and returns a new reader over those bytes, propagating this reader's
	 * configured maximum length so nested length-delimited fields remain bounded.
	 *
	 * @return A new reader over the block bytes.
	 * @throws IOException If the stream ends early, the declared length is out of bounds, or the underlying stream fails.
	 */
	public ProtobufReader readLenDelimitedReader() throws IOException {
		var r = new ProtobufReader(readLenDelimited());
		r.maxLength = maxLength;
		return r;
	}

	/**
	 * Reads a length-delimited UTF-8 string.
	 *
	 * @return The decoded string.
	 * @throws IOException If the stream ends early or the underlying stream fails.
	 */
	public String readString() throws IOException {
		return new String(readLenDelimited(), UTF8);
	}

	/**
	 * Skips a field's value, consuming exactly the right number of bytes for the specified wire type.
	 *
	 * <p>
	 * For {@link WireType#LEN}, the wire-declared length is validated against the configured maximum (the
	 * same check applied by {@link #readLenDelimited()}) before skipping, so an unknown forward-compatible
	 * field cannot bypass the length cap via an unbounded or wrapped-negative skip.
	 *
	 * @param wireType The wire type of the field to skip.
	 * @throws IOException If the stream ends early, the declared length is out of bounds, or the underlying
	 * 	stream fails.
	 */
	public void skipField(WireType wireType) throws IOException {
		switch (wireType) {
			case VARINT -> readVarint();
			case I64 -> skip(8);
			case I32 -> skip(4);
			case LEN -> {
				var len = ParserInputStream.checkLength(readVarint(), maxLength, "protobuf field");
				skip(len);
			}
			default -> throw ioex("Cannot skip unsupported protobuf wire type: %s", wireType);
		}
	}

	private void skip(int n) throws IOException {
		for (var i = 0; i < n; i++)
			readByte();
	}
}
