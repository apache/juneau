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
package org.apache.juneau.serializer;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.io.*;
import java.nio.charset.*;
import org.apache.juneau.commons.collections.FluentMap;

/**
 * Subclass of {@link SerializerSession} for character-based serializers.
 *
 * <h5 class='topic'>Description</h5>
 *
 * This class is typically the parent class of all character-based serializers.
 * <br>It has 1 abstract method to implement...
 * <ul class='spaced-list'>
 * 	<li>
 * 		{@link #doSerialize(SerializerPipe, Object)}
 * </ul>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SerializersAndParsers">Serializers and Parsers</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S110", // Deep inheritance inherent to the serializer/parser session hierarchy
	"java:S115" // Constants use UPPER_snakeCase convention
})
public class WriterSerializerSession extends SerializerSession {

	// Property name constants
	private static final String PROP_fileCharset = "fileCharset";
	private static final String PROP_streamCharset = "streamCharset";
	private static final String PROP_useWhitespace = "useWhitespace";
	private static final String PROP_maxIndent = "maxIndent";
	private static final String PROP_quoteChar = "quoteChar";
	private static final String PROP_WriterSerializerSession_fileCharset = "WriterSerializerSession.fileCharset";
	private static final String PROP_WriterSerializerSession_streamCharset = "WriterSerializerSession.streamCharset";
	private static final String PROP_WriterSerializerSession_useWhitespace = "WriterSerializerSession.useWhitespace";
	private static final String PROP_WriterSerializerSession_maxIndent = "WriterSerializerSession.maxIndent";
	private static final String PROP_WriterSerializerSession_quoteChar = "WriterSerializerSession.quoteChar";

	// Argument name constants for assertArgNotNull
	private static final String ARG_ctx = "ctx";

	/**
	 * Builder class.
	 */
	@SuppressWarnings({
		"java:S119" // 'SELF' (CRTP self-type) is intentional and clearer than a single-letter name.
	})
	public abstract static class Builder<SELF extends Builder<SELF>> extends SerializerSession.Builder<SELF> {

		private boolean useWhitespace;
		private Charset fileCharset;
		private Charset streamCharset;
		private int maxIndent;
		private char quoteChar;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(WriterSerializer ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
			fileCharset = ctx.getFileCharset();
			streamCharset = ctx.getStreamCharset();
			useWhitespace = ctx.useWhitespace;
			maxIndent = ctx.getMaxIndent();
			quoteChar = ctx.getQuoteChar();
		}

		@Override
		public WriterSerializerSession build() {
			return new WriterSerializerSession(this);
		}

		/**
		 * File charset.
		 *
		 * <p>
		 * The character set to use for writing Files to the file system.
		 *
		 * <p>
		 * Used when passing in files to {@link Serializer#serialize(Object, Object)}.
		 *
		 * <p>
		 * If not specified, defaults to the JVM system default charset.
		 *
		 * @param value
		 * 	The new property value.
		 * 	<br>Can be <jk>null</jk> (value will not be set, defaults to JVM system default charset).
		 * @return This object.
		 */
		public SELF fileCharset(Charset value) {
			if (nn(value))
				fileCharset = value;
			return self();
		}

		/**
		 * Maximum indentation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public SELF maxIndent(int value) {
			maxIndent = value;
			return self();
		}

		@Override /* Overridden from Builder */
		public SELF property(String key, Object value) {
			if (key == null) {
				super.property(key, value);
				return self();
			}
			switch (key) {
				case PROP_fileCharset, PROP_WriterSerializerSession_fileCharset:
					return fileCharset(cvt(value, Charset.class));
				case PROP_streamCharset, PROP_WriterSerializerSession_streamCharset:
					return streamCharset(cvt(value, Charset.class));
				case PROP_useWhitespace, PROP_WriterSerializerSession_useWhitespace:
					return useWhitespace(cvt(value, Boolean.class));
				case PROP_maxIndent, PROP_WriterSerializerSession_maxIndent:
					return maxIndent(cvt(value, Integer.class));
				case PROP_quoteChar, PROP_WriterSerializerSession_quoteChar:
					return quoteChar(cvt(value, Character.class));
				default:
					super.property(key, value);
					return self();
			}
		}

		/**
		 * Quote character.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public SELF quoteChar(char value) {
			quoteChar = value;
			return self();
		}

		/**
		 * Output stream charset.
		 *
		 * <p>
		 * The character set to use when writing to OutputStreams.
		 *
		 * <p>
		 * Used when passing in output streams and byte arrays to {@link WriterSerializer#serialize(Object, Object)}.
		 *
		 * <p>
		 * If not specified, defaults to UTF-8.
		 *
		 * @param value
		 * 	The new property value.
		 * 	<br>Can be <jk>null</jk> (value will not be set, defaults to UTF-8).
		 * @return This object.
		 */
		public SELF streamCharset(Charset value) {
			if (nn(value))
				streamCharset = value;
			return self();
		}

		/**
		 * Use whitespace.
		 *
		 * <p>
		 * If true, whitespace is added to the output to improve readability.
		 *
		 * @param value
		 * 	The new property value.
		 * 	<br>Can be <jk>null</jk> (value will not be set, existing value from context will be kept).
		 * @return This object.
		 */
		public SELF useWhitespace(Boolean value) {
			if (nn(value))
				useWhitespace = value;
			return self();
		}
	}

	/**
	 * Concrete default builder leaf for the non-subclassed {@code create()} path (CRTP terminal).
	 */
	public static final class DefaultBuilder extends Builder<DefaultBuilder> {

		DefaultBuilder(WriterSerializer ctx) {
			super(ctx);
		}
	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * 	<br>Cannot be <jk>null</jk>.
	 * @return A new builder.
	 */
	@SuppressWarnings({
		"java:S1452" // Builder<?> wildcard return intentional; callers use it to construct session instances polymorphically
	})
	public static Builder<?> create(WriterSerializer ctx) {
		return new DefaultBuilder(assertArgNotNull(ARG_ctx, ctx));
	}

	private final boolean useWhitespace;
	private final Charset fileCharset;
	private final Charset streamCharset;
	private final int maxIndent;
	private final char quoteChar;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected WriterSerializerSession(Builder<?> builder) {
		super(builder);
		fileCharset = builder.fileCharset;
		streamCharset = builder.streamCharset;
		useWhitespace = builder.useWhitespace;
		maxIndent = builder.maxIndent;
		quoteChar = builder.quoteChar;
	}

	/**
	 * Returns the file charset defined on this session.
	 *
	 * @return the file charset defined on this session.
	 */
	public Charset getFileCharset() { return fileCharset; }

	/**
	 * Returns the stream charset defined on this session.
	 *
	 * @return the stream charset defined on this session.
	 */
	public Charset getStreamCharset() { return streamCharset; }

	@Override /* Overridden from SerializerSession */
	public final boolean isWriterSerializer() { return true; }

	/**
	 * Convenience method for serializing an object to a <c>String</c>.
	 *
	 * @param o The object to serialize.
	 * @return The output serialized to a string.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	@Override /* Overridden from SerializerSession */
	public final String serialize(Object o) throws SerializeException {
		var w = new StringWriter();
		try {
			serialize(o, w);
		} catch (IOException e) {
			throw new SerializeException(e); // Shouldn't happen.
		}
		return w.toString();
	}

	@Override /* Overridden from SerializerSession */
	public final String serializeToString(Object o) throws SerializeException {
		return serialize(o);
	}

	@Override /* Overridden from SerializerSession */
	protected SerializerPipe createPipe(Object output) {
		return new SerializerPipe(output, streamCharset, fileCharset);
	}

	/**
	 * Maximum indentation.
	 *
	 * @see WriterSerializer.Builder#maxIndent(int)
	 * @return
	 * 	The maximum indentation level in the serialized document.
	 */
	protected final int getMaxIndent() { return maxIndent; }

	/**
	 * Quote character.
	 *
	 * @see WriterSerializer.Builder#quoteChar(char)
	 * @return
	 * 	The character used for quoting attributes and values.
	 */
	protected char getQuoteChar() { return quoteChar; }

	/**
	 * Use whitespace.
	 *
	 * @see WriterSerializer.Builder#useWhitespace()
	 * @return
	 * 	The character used for quoting attributes and values.
	 */
	protected final boolean isUseWhitespace() { return useWhitespace; }

	@Override /* Overridden from SerializerSession */
	protected FluentMap<String,Object> properties() {
		return super.properties()
			.a(PROP_fileCharset, fileCharset)
			.a(PROP_streamCharset, streamCharset)
			.a(PROP_useWhitespace, useWhitespace)
			.a(PROP_maxIndent, maxIndent)
			.a(PROP_quoteChar, quoteChar);
	}
}