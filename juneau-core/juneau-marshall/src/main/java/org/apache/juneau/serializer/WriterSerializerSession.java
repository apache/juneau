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

import static org.apache.juneau.collections.JsonMap.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.svl.*;

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
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.SerializersAndParsers">Serializers and Parsers</a>
 * </ul>
 */
public class WriterSerializerSession extends SerializerSession {

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * @return A new builder.
	 */
	public static Builder create(WriterSerializer ctx) {
		return new Builder(ctx);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends SerializerSession.Builder {

		WriterSerializer ctx;
		boolean useWhitespace;
		Charset fileCharset, streamCharset;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 */
		protected Builder(WriterSerializer ctx) {
			super(ctx);
			this.ctx = ctx;
			useWhitespace = ctx.useWhitespace;
			fileCharset = ctx.fileCharset;
			streamCharset = ctx.streamCharset;
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
		 * 	<br>Can be <jk>null</jk>.
		 * @return This object.
		 */
		@FluentSetter
		public Builder fileCharset(Charset value) {
			if (value != null)
				fileCharset = value;
			return this;
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
		 * 	<br>Can be <jk>null</jk>.
		 * @return This object.
		 */
		@FluentSetter
		public Builder streamCharset(Charset value) {
			if (value != null)
				streamCharset = value;
			return this;
		}

		/**
		 * Use whitespace.
		 *
		 * <p>
		 * If true, whitespace is added to the output to improve readability.
		 *
		 * @param value
		 * 	The new property value.
		 * 	<br>Can be <jk>null</jk>.
		 * @return This object.
		 */
		@FluentSetter
		public Builder useWhitespace(Boolean value) {
			if (value != null)
				useWhitespace = value;
			return this;
		}

		// <FluentSetters>

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public <T> Builder apply(Class<T> type, Consumer<T> apply) {
			super.apply(type, apply);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder debug(Boolean value) {
			super.debug(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder properties(Map<String,Object> value) {
			super.properties(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder property(String key, Object value) {
			super.property(key, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder unmodifiable() {
			super.unmodifiable();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder locale(Locale value) {
			super.locale(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder localeDefault(Locale value) {
			super.localeDefault(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder mediaType(MediaType value) {
			super.mediaType(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder mediaTypeDefault(MediaType value) {
			super.mediaTypeDefault(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder timeZone(TimeZone value) {
			super.timeZone(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder timeZoneDefault(TimeZone value) {
			super.timeZoneDefault(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.SerializerSession.Builder */
		public Builder javaMethod(Method value) {
			super.javaMethod(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.SerializerSession.Builder */
		public Builder resolver(VarResolverSession value) {
			super.resolver(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.SerializerSession.Builder */
		public Builder schema(HttpPartSchema value) {
			super.schema(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.SerializerSession.Builder */
		public Builder schemaDefault(HttpPartSchema value) {
			super.schemaDefault(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.SerializerSession.Builder */
		public Builder uriContext(UriContext value) {
			super.uriContext(value);
			return this;
		}

		// </FluentSetters>
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final WriterSerializer ctx;
	private final boolean useWhitespace;
	private final Charset streamCharset, fileCharset;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected WriterSerializerSession(Builder builder) {
		super(builder);
		ctx = builder.ctx;
		streamCharset = builder.streamCharset;
		fileCharset = builder.fileCharset;
		useWhitespace = builder.useWhitespace;
	}

	@Override /* SerializerSession */
	public final boolean isWriterSerializer() {
		return true;
	}

	@Override /* SerializerSession */
	protected SerializerPipe createPipe(Object output) {
		return new SerializerPipe(output, streamCharset, fileCharset);
	}

	/**
	 * Convenience method for serializing an object to a <c>String</c>.
	 *
	 * @param o The object to serialize.
	 * @return The output serialized to a string.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	@Override /* SerializerSession */
	public final String serialize(Object o) throws SerializeException {
		StringWriter w = new StringWriter();
		try {
			serialize(o, w);
		} catch (IOException e) {
			throw new SerializeException(e); // Shouldn't happen.
		}
		return w.toString();
	}

	@Override /* SerializerSession */
	public final String serializeToString(Object o) throws SerializeException {
		return serialize(o);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Maximum indentation.
	 *
	 * @see WriterSerializer.Builder#maxIndent(int)
	 * @return
	 * 	The maximum indentation level in the serialized document.
	 */
	protected final int getMaxIndent() {
		return ctx.getMaxIndent();
	}

	/**
	 * Quote character.
	 *
	 * @see WriterSerializer.Builder#quoteChar(char)
	 * @return
	 * 	The character used for quoting attributes and values.
	 */
	protected char getQuoteChar() {
		return ctx.getQuoteChar();
	}

	/**
	 * Use whitespace.
	 *
	 * @see WriterSerializer.Builder#useWhitespace()
	 * @return
	 * 	The character used for quoting attributes and values.
	 */
	protected final boolean isUseWhitespace() {
		return useWhitespace;
	}

	/**
	 * Returns the file charset defined on this session.
	 *
	 * @return the file charset defined on this session.
	 */
	public Charset getFileCharset() {
		return fileCharset;
	}

	/**
	 * Returns the stream charset defined on this session.
	 *
	 * @return the stream charset defined on this session.
	 */
	public Charset getStreamCharset() {
		return streamCharset;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* ContextSession */
	protected JsonMap properties() {
		return filteredMap("fileCharset", fileCharset, "streamCharset", streamCharset, "useWhitespace", useWhitespace);
	}
}
