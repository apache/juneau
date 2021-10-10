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
package org.apache.juneau.parser;

import static org.apache.juneau.collections.OMap.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;

/**
 * Subclass of parser session objects for character-based parsers.
 *
 * <p>
 * This class is NOT thread safe.  It is typically discarded after one-time use.
 */
public abstract class ReaderParserSession extends ParserSession {

	//-------------------------------------------------------------------------------------------------------------------
	// Builder
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public abstract static class Builder extends ParserSession.Builder {

		ReaderParser ctx;
		Charset fileCharset, streamCharset;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 */
		protected Builder(ReaderParser ctx) {
			super(ctx);
			this.ctx = ctx;
			fileCharset = ctx.fileCharset;
			streamCharset = ctx.streamCharset;
		}

		@Override
		public abstract ReaderParserSession build();

		/**
		 * File charset.
		 *
		 * <p>
		 * The character set to use for reading Files from the file system.
		 *
		 * <p>
		 * Used when passing in files to {@link Parser#parse(Object, Class)}.
		 *
		 * <p>
		 * If not specified, defaults to the JVM system default charset.
		 *
		 * @param value
		 * 	The new property value.
		 * 	<br>Can be <jk>null</jk>.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder fileCharset(Charset value) {
			fileCharset = value;
			return this;
		}

		/**
		 * Input stream charset.
		 *
		 * <p>
		 * The character set to use for converting InputStreams and byte arrays to readers.
		 *
		 * <p>
		 * Used when passing in input streams and byte arrays to {@link Parser#parse(Object, Class)}.
		 *
		 * <p>
		 * If not specified, defaults to UTF-8.
		 *
		 * @param value
		 * 	The new property value.
		 * 	<br>Can be <jk>null</jk>.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder streamCharset(Charset value) {
			streamCharset = value;
			return this;
		}

		// <FluentSetters>

		@Override /* GENERATED */
		public <T> Builder ifType(Class<T> type, Consumer<T> apply) {
			super.ifType(type, apply);
			return this;
		}

		@Override /* GENERATED */
		public Builder javaMethod(Method value) {
			super.javaMethod(value);
			return this;
		}

		@Override /* GENERATED */
		public Builder outer(Object value) {
			super.outer(value);
			return this;
		}

		@Override /* GENERATED */
		public Builder debug(Boolean value) {
			super.debug(value);
			return this;
		}

		@Override /* GENERATED */
		public Builder locale(Locale value) {
			super.locale(value);
			return this;
		}

		@Override /* GENERATED */
		public Builder mediaType(MediaType value) {
			super.mediaType(value);
			return this;
		}

		@Override /* GENERATED */
		public Builder properties(Map<String,Object> value) {
			super.properties(value);
			return this;
		}

		@Override /* GENERATED */
		public Builder property(String key, Object value) {
			super.property(key, value);
			return this;
		}

		@Override /* GENERATED */
		public Builder timeZone(TimeZone value) {
			super.timeZone(value);
			return this;
		}

		@Override /* GENERATED */
		public Builder unmodifiable() {
			super.unmodifiable();
			return this;
		}

		@Override /* GENERATED */
		public Builder schema(HttpPartSchema value) {
			super.schema(value);
			return this;
		}

		// </FluentSetters>
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final ReaderParser ctx;
	private final Charset fileCharset, streamCharset;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected ReaderParserSession(Builder builder) {
		super(builder);
		ctx = builder.ctx;
		fileCharset = builder.fileCharset;
		streamCharset = builder.streamCharset;
	}

	@Override /* ParserSession */
	public final boolean isReaderParser() {
		return true;
	}

	/**
	 * Wraps the specified input object into a {@link ParserPipe} object so that it can be easily converted into
	 * a stream or reader.
	 *
	 * @param input
	 * 	The input.
	 * 	<br>This can be any of the following types:
	 * 	<ul>
	 * 		<li><jk>null</jk>
	 * 		<li>{@link Reader}
	 * 		<li>{@link CharSequence}
	 * 		<li>{@link InputStream} containing UTF-8 encoded text (or whatever the encoding specified by
	 * 			{@link ReaderParser.Builder#streamCharset(Charset)}).
	 * 		<li><code><jk>byte</jk>[]</code> containing UTF-8 encoded text (or whatever the encoding specified by
	 * 			{@link ReaderParser.Builder#streamCharset(Charset)}).
	 * 		<li>{@link File} containing system encoded text (or whatever the encoding specified by
	 * 			{@link ReaderParser.Builder#streamCharset(Charset)}).
	 * 	</ul>
	 * @return
	 * 	A new {@link ParserPipe} wrapper around the specified input object.
	 */
	@SuppressWarnings("resource")
	@Override /* ParserSesson */
	public final ParserPipe createPipe(Object input) {
		return setPipe(new ParserPipe(input, isDebug(), ctx.isStrict(), ctx.isAutoCloseStreams(), ctx.isUnbuffered(), streamCharset, fileCharset));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

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
	public OMap toMap() {
		return super.toMap().a("ReaderParserSession", filteredMap("fileCharset", fileCharset, "streamCharset", streamCharset));
	}
}
