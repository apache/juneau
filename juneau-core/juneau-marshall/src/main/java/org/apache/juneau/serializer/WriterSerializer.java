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
package org.apache.juneau.serializer;

import static org.apache.juneau.internal.ExceptionUtils.*;
import static java.util.Optional.*;

import java.nio.charset.*;

import org.apache.juneau.collections.*;

/**
 * Subclass of {@link Serializer} for character-based serializers.
 * {@review}
 */
public abstract class WriterSerializer extends Serializer {

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final Charset fileCharset, streamCharset;
	final int maxIndent;
	final Character quoteChar, quoteCharOverride;
	final boolean useWhitespace;

	private final char quoteCharValue;

	/**
	 * Constructor.
	 *
	 * @param builder
	 * 	The builder for this object.
	 */
	protected WriterSerializer(WriterSerializerBuilder builder) {
		super(builder);

		maxIndent = builder.maxIndent;
		quoteChar = builder.quoteChar;
		quoteCharOverride = builder.quoteCharOverride;
		streamCharset = builder.streamCharset;
		fileCharset = builder.fileCharset;
		useWhitespace = builder.useWhitespace;

		quoteCharValue = ofNullable(quoteCharOverride).orElse(ofNullable(quoteChar).orElse('"'));
	}

	@Override
	public abstract WriterSerializerBuilder copy();

	//-----------------------------------------------------------------------------------------------------------------
	// Abstract methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* SerializerSession */
	public abstract WriterSerializerSession createSession(SerializerSessionArgs args);

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public WriterSerializerSession createSession() {
		return createSession(createDefaultSessionArgs());
	}

	@Override /* Serializer */
	public final boolean isWriterSerializer() {
		return true;
	}

	/**
	 * Convenience method for serializing an object to a <c>String</c>.
	 *
	 * @param o The object to serialize.
	 * @return The output serialized to a string.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	@Override /* Serializer */
	public final String serialize(Object o) throws SerializeException {
		return createSession(createDefaultSessionArgs()).serialize(o);
	}

	/**
	 * Identical to {@link #serialize(Object)} except throws a {@link RuntimeException} instead of a {@link SerializeException}.
	 *
	 * <p>
	 * This is typically good enough for debugging purposes.
	 *
	 * @param o The object to serialize.
	 * @return The serialized object.
	 */
	public final String toString(Object o) {
		try {
			return serialize(o);
		} catch (Exception e) {
			throw runtimeException(e);
		}
	}

	/**
	 * Convenience method for serializing an object and sending it to STDOUT.
	 *
	 * @param o The object to serialize.
	 * @return This object (for method chaining).
	 */
	public final WriterSerializer println(Object o) {
		System.out.println(toString(o));  // NOT DEBUG
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * File charset.
	 *
	 * @see WriterSerializerBuilder#fileCharset(Charset)
	 * @return
	 * 	The character set to use when writing to <c>Files</c> on the file system.
	 */
	protected final Charset getFileCharset() {
		return fileCharset;
	}

	/**
	 * Maximum indentation.
	 *
	 * @see WriterSerializerBuilder#maxIndent(int)
	 * @return
	 * 	The maximum indentation level in the serialized document.
	 */
	protected final int getMaxIndent() {
		return maxIndent;
	}

	/**
	 * Quote character.
	 *
	 * @see WriterSerializerBuilder#quoteChar(char)
	 * @return
	 * 	The character used for quoting attributes and values.
	 */
	protected char getQuoteChar() {
		return quoteCharValue;
	}

	/**
	 * Quote character.
	 *
	 * @see WriterSerializerBuilder#quoteChar(char)
	 * @return
	 * 	The character used for quoting attributes and values.
	 */
	protected Character quoteChar() {
		return ofNullable(quoteCharOverride).orElse(quoteChar);
	}

	/**
	 * Output stream charset.
	 *
	 * @see WriterSerializerBuilder#streamCharset(Charset)
	 * @return
	 * 	The character set to use when writing to <c>OutputStreams</c> and byte arrays.
	 */
	protected final Charset getStreamCharset() {
		return streamCharset;
	}

	/**
	 * Trim strings.
	 *
	 * @see WriterSerializerBuilder#useWhitespace()
	 * @return
	 * 	When enabled, whitespace is added to the output to improve readability.
	 */
	protected final boolean isUseWhitespace() {
		return useWhitespace;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public OMap toMap() {
		return super.toMap()
			.a(
				"WriterSerializer",
				OMap
					.create()
					.filtered()
					.a("fileCharset", fileCharset)
					.a("maxIndent", maxIndent)
					.a("quoteChar", quoteChar)
					.a("streamCharset", streamCharset)
					.a("useWhitespace", useWhitespace)
			);
	}
}
