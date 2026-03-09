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

import java.io.*;
import java.math.*;
import java.nio.charset.*;

import org.apache.juneau.serializer.*;

/**
 * Specialized output stream for serializing BSON (Binary JSON) streams.
 *
 * <p>
 * BSON uses little-endian byte order for all multi-byte values. Documents are length-prefixed: the total
 * byte count (int32) must appear before the content. Since the size is unknown until all elements are written,
 * each document is buffered in a {@link ByteArrayOutputStream} child. When complete, the size is calculated
 * and <c>[int32 size][content][0x00]</c> is written to the parent stream.
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
@SuppressWarnings({
	"resource" // OutputStream resource managed by calling code
})
public class BsonOutputStream extends OutputStream {

	private static final Charset UTF8 = StandardCharsets.UTF_8;
	private static final byte BINARY_SUBTYPE_GENERIC = 0x00;
	private static final byte DOCUMENT_TERMINATOR = 0x00;

	private final OutputStream os;
	private ByteArrayOutputStream buffer;
	private boolean inDocument;

	/**
	 * Constructor.
	 *
	 * @param os The output stream being wrapped.
	 */
	public BsonOutputStream(OutputStream os) {
		this.os = os;
	}

	@Override /* Overridden from OutputStream */
	public void write(int b) {
		try {
			target().write(b);
		} catch (IOException e) {
			throw new SerializeException(e);
		}
	}

	@Override /* Overridden from OutputStream */
	public void write(byte[] b) {
		if (b != null && b.length > 0)
			write(b, 0, b.length);
	}

	@Override /* Overridden from OutputStream */
	public void write(byte[] b, int off, int len) {
		try {
			target().write(b, off, len);
		} catch (IOException e) {
			throw new SerializeException(e);
		}
	}

	private OutputStream target() {
		return buffer != null ? buffer : os;
	}

	/**
	 * Begin buffering a new document.
	 */
	public void startDocument() {
		buffer = new ByteArrayOutputStream();
		inDocument = true;
	}

	/**
	 * Finalize the current document and return the bytes (int32 size + content + 0x00).
	 *
	 * @return The complete document bytes.
	 */
	public byte[] endDocument() {
		if (buffer == null)
			return new byte[0];
		buffer.write(DOCUMENT_TERMINATOR);
		var content = buffer.toByteArray();
		// size = 4 (int32) + content length
		var size = 4 + content.length;
		var result = new byte[size];
		writeLE4To(result, 0, size);
		System.arraycopy(content, 0, result, 4, content.length);
		buffer = null;
		inDocument = false;
		return result;
	}

	/**
	 * Finalize the current document and write it to the parent stream.
	 *
	 * @param parent The parent output stream.
	 */
	public void writeDocumentTo(OutputStream parent) {
		var bytes = endDocument();
		try {
			parent.write(bytes);
		} catch (IOException e) {
			throw new SerializeException(e);
		}
	}

	/**
	 * Writes a BSON element header (type byte + cstring name).
	 *
	 * @param type The BSON type code.
	 * @param name The element name (cstring).
	 */
	public void writeElement(int type, String name) {
		write(type & 0xFF);
		writeCString(name);
	}

	/**
	 * Writes a 4-byte little-endian integer.
	 *
	 * @param value The 32-bit value.
	 */
	public void writeLE4(int value) {
		write(value & 0xFF);
		write((value >> 8) & 0xFF);
		write((value >> 16) & 0xFF);
		write((value >> 24) & 0xFF);
	}

	/**
	 * Writes a 4-byte little-endian integer to a byte array.
	 */
	static void writeLE4To(byte[] buf, int offset, int value) {
		buf[offset] = (byte)(value & 0xFF);
		buf[offset + 1] = (byte)((value >> 8) & 0xFF);
		buf[offset + 2] = (byte)((value >> 16) & 0xFF);
		buf[offset + 3] = (byte)((value >> 24) & 0xFF);
	}

	/**
	 * Writes an 8-byte little-endian long.
	 *
	 * @param value The 64-bit value.
	 */
	public void writeLE8(long value) {
		write((int)(value & 0xFF));
		write((int)((value >> 8) & 0xFF));
		write((int)((value >> 16) & 0xFF));
		write((int)((value >> 24) & 0xFF));
		write((int)((value >> 32) & 0xFF));
		write((int)((value >> 40) & 0xFF));
		write((int)((value >> 48) & 0xFF));
		write((int)((value >> 56) & 0xFF));
	}

	/**
	 * Writes an IEEE 754 double (8 bytes, little-endian).
	 *
	 * @param value The double value.
	 */
	public void writeDouble(double value) {
		writeLE8(Double.doubleToLongBits(value));
	}

	/**
	 * Writes a 32-bit integer (4 bytes, little-endian).
	 *
	 * @param value The integer value.
	 */
	public void writeInt32(int value) {
		writeLE4(value);
	}

	/**
	 * Writes a 64-bit integer (8 bytes, little-endian).
	 *
	 * @param value The long value.
	 */
	public void writeInt64(long value) {
		writeLE8(value);
	}

	/**
	 * Writes a boolean (1 byte: 0x00=false, 0x01=true).
	 *
	 * @param value The boolean value.
	 */
	public void writeBoolean(boolean value) {
		write(value ? 0x01 : 0x00);
	}

	/**
	 * Writes BSON null (no value bytes).
	 */
	public void writeNull() {
		// Type byte only, no value
	}

	/**
	 * Writes a BSON string (int32 length + UTF-8 bytes + 0x00).
	 *
	 * @param value The string to write (treated as empty if <jk>null</jk>).
	 */
	public void writeString(String value) {
		if (value == null)
			value = "";
		var bytes = value.getBytes(UTF8);
		// BSON string length includes trailing null
		writeLE4(bytes.length + 1);
		write(bytes);
		write(DOCUMENT_TERMINATOR);
	}

	/**
	 * Writes a cstring (UTF-8 bytes + 0x00, no length prefix).
	 *
	 * @param value The string to write (treated as empty if <jk>null</jk>).
	 */
	public void writeCString(String value) {
		if (value == null)
			value = "";
		for (var i = 0; i < value.length(); i++)
			if (value.charAt(i) == 0)
				throw new SerializeException("CString must not contain null bytes");
		write(value.getBytes(UTF8));
		write(DOCUMENT_TERMINATOR);
	}

	/**
	 * Writes BSON binary (int32 length + subtype byte + raw bytes).
	 *
	 * @param data The raw bytes (subtype 0x00 generic).
	 */
	public void writeBinary(byte[] data) {
		if (data == null)
			data = new byte[0];
		writeLE4(data.length);
		write(BINARY_SUBTYPE_GENERIC);
		write(data);
	}

	/**
	 * Writes BSON datetime (int64, UTC milliseconds since epoch).
	 *
	 * @param millis The UTC milliseconds since epoch.
	 */
	public void writeDateTime(long millis) {
		writeLE8(millis);
	}

	/**
	 * Writes BSON decimal128 (16 bytes, IEEE 754-2008) from BigDecimal.
	 *
	 * @param value The decimal value.
	 */
	public void writeDecimal128(BigDecimal value) {
		if (value == null)
			value = BigDecimal.ZERO;
		var d128 = BsonDecimal128.fromBigDecimal(value);
		writeLE8(d128.getLow());
		writeLE8(d128.getHigh());
	}

	/**
	 * Creates a child BsonOutputStream for buffering a nested document.
	 *
	 * @return A new child stream (no parent; use {@link #writeChildDocument} to embed).
	 */
	public BsonOutputStream createChild() {
		return new BsonOutputStream(null);
	}

	/**
	 * Writes the child document's content as an embedded document to this stream.
	 *
	 * @param child The child stream (must have been finalized via endDocument).
	 */
	public void writeChildDocument(BsonOutputStream child) {
		// Child buffers to its own ByteArrayOutputStream; we need to write [size][content][0x00]
		// Child.endDocument() returns [size][content] with 0x00 already in content
		// So we just need to write the child's endDocument() result
		var bytes = child.endDocument();
		// bytes already has size prefix and terminator from endDocument
		try {
			target().write(bytes);
		} catch (IOException e) {
			throw new SerializeException(e);
		}
	}

	/**
	 * Returns whether we are currently buffering a document.
	 *
	 * @return <jk>true</jk> if buffering, <jk>false</jk> otherwise.
	 */
	public boolean isInDocument() {
		return inDocument;
	}
}
