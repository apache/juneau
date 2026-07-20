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

import java.io.*;

import org.apache.juneau.marshall.serializer.*;

/**
 * Low-level byte emitter for the protobuf binary wire format.
 *
 * <p>
 * Wraps an {@link OutputStream} and provides the primitive encodings that make up a protobuf message:
 * varints, zigzag varints, little-endian fixed32/fixed64, field tags, and length-delimited blocks.
 *
 * <p>
 * Length-delimited sub-messages and packed repeated fields require the length to precede the payload, so
 * callers encode the payload into a scratch buffer (e.g. a {@link ByteArrayOutputStream} wrapped in its own
 * {@code ProtobufWriter}) and then emit it via {@link #writeLenDelimited(byte[])}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is not intended for external use.
 * 	<li class='note'>This class is not thread-safe; each serialization session uses its own instance.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Protobuf">Protobuf Binary Format Basics</a>
 * 	<li class='link'><a class="doclink" href="https://protobuf.dev/programming-guides/encoding/">Protocol Buffers Encoding</a>
 * </ul>
 */
@SuppressWarnings({
	"resource" // The wrapped OutputStream is owned by the caller/session; the fluent writeXxx() methods return 'this' (a Closeable) by design, so Eclipse JDT's resource-leak/@Owning analysis flags them, but there is no resource for this writer to close.
})
public class ProtobufWriter extends OutputStream {

	private final OutputStream os;

	/**
	 * Constructor.
	 *
	 * @param os The output stream being wrapped.  Must not be <jk>null</jk>.
	 */
	public ProtobufWriter(OutputStream os) {
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

	@Override /* Overridden from OutputStream */
	public void flush() {
		try {
			os.flush();
		} catch (IOException e) {
			throw new SerializeException(e);
		}
	}

	/**
	 * Writes a base-128 varint (LEB128, unsigned interpretation of the bits).
	 *
	 * <p>
	 * The value's bits are treated as unsigned, so a negative {@code long} (e.g. a sign-extended int32
	 * {@code -1}) produces the canonical 10-byte form.
	 *
	 * @param value The value whose 64 bits are encoded.
	 * @return This object.
	 */
	public ProtobufWriter writeVarint(long value) {
		while ((value & ~0x7FL) != 0) {
			write((int)((value & 0x7F) | 0x80));
			value >>>= 7;
		}
		write((int)(value & 0x7F));
		return this;
	}

	/**
	 * Writes a 32-bit signed integer using zigzag encoding (maps small-magnitude negatives to small varints).
	 *
	 * @param value The value to encode.
	 * @return This object.
	 */
	public ProtobufWriter writeZigZag32(int value) {
		return writeVarint(((value << 1) ^ (value >> 31)) & 0xFFFFFFFFL);
	}

	/**
	 * Writes a 64-bit signed integer using zigzag encoding.
	 *
	 * @param value The value to encode.
	 * @return This object.
	 */
	public ProtobufWriter writeZigZag64(long value) {
		return writeVarint((value << 1) ^ (value >> 63));
	}

	/**
	 * Writes a 32-bit value as 4 little-endian bytes (fixed32/sfixed32/float).
	 *
	 * @param value The 32 bits to write.
	 * @return This object.
	 */
	public ProtobufWriter writeFixed32(int value) {
		write(value & 0xFF);
		write((value >> 8) & 0xFF);
		write((value >> 16) & 0xFF);
		write((value >> 24) & 0xFF);
		return this;
	}

	/**
	 * Writes a 64-bit value as 8 little-endian bytes (fixed64/sfixed64/double).
	 *
	 * @param value The 64 bits to write.
	 * @return This object.
	 */
	public ProtobufWriter writeFixed64(long value) {
		write((int)(value & 0xFF));
		write((int)((value >> 8) & 0xFF));
		write((int)((value >> 16) & 0xFF));
		write((int)((value >> 24) & 0xFF));
		write((int)((value >> 32) & 0xFF));
		write((int)((value >> 40) & 0xFF));
		write((int)((value >> 48) & 0xFF));
		write((int)((value >> 56) & 0xFF));
		return this;
	}

	/**
	 * Writes a field tag (<c>(fieldNumber &lt;&lt; 3) | wireType</c>) as a varint.
	 *
	 * @param fieldNumber The protobuf field number.
	 * @param wireType The wire type of the value that follows.
	 * @return This object.
	 */
	public ProtobufWriter writeTag(int fieldNumber, WireType wireType) {
		return writeVarint(((long)fieldNumber << 3) | wireType.code());
	}

	/**
	 * Writes a length-delimited block:  a varint length followed by the raw bytes.
	 *
	 * @param value The bytes to write.
	 * @return This object.
	 */
	public ProtobufWriter writeLenDelimited(byte[] value) {
		writeVarint(value.length);
		write(value, 0, value.length);
		return this;
	}

	/**
	 * Writes a length-delimited UTF-8 string.
	 *
	 * @param value The string to write.
	 * @return This object.
	 */
	public ProtobufWriter writeString(String value) {
		return writeLenDelimited(value.getBytes(UTF8));
	}
}
