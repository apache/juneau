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
package org.apache.juneau.parser;

import static org.apache.juneau.collections.JsonMap.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.httppart.*;

/**
 * Subclass of parser session objects for character-based parsers.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SerializersAndParsers">Serializers and Parsers</a>
 * </ul>
 */
public class ReaderParserSession extends ParserSession {
	/**
	 * Builder class.
	 */
	public static class Builder extends ParserSession.Builder {

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

		@Override /* Overridden from Builder */
		public <T> Builder apply(Class<T> type, Consumer<T> apply) {
			super.apply(type, apply);
			return this;
		}

		@Override
		public ReaderParserSession build() {
			return new ReaderParserSession(this);
		}

		@Override /* Overridden from Builder */
		public Builder debug(Boolean value) {
			super.debug(value);
			return this;
		}
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
		 * @return This object.
		 */
		public Builder fileCharset(Charset value) {
			fileCharset = value;
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder javaMethod(Method value) {
			super.javaMethod(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder locale(Locale value) {
			super.locale(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder localeDefault(Locale value) {
			super.localeDefault(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder mediaType(MediaType value) {
			super.mediaType(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder mediaTypeDefault(MediaType value) {
			super.mediaTypeDefault(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder outer(Object value) {
			super.outer(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder properties(Map<String,Object> value) {
			super.properties(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder property(String key, Object value) {
			super.property(key, value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder schema(HttpPartSchema value) {
			super.schema(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder schemaDefault(HttpPartSchema value) {
			super.schemaDefault(value);
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
		 * @return This object.
		 */
		public Builder streamCharset(Charset value) {
			streamCharset = value;
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder timeZone(TimeZone value) {
			super.timeZone(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder timeZoneDefault(TimeZone value) {
			super.timeZoneDefault(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder unmodifiable() {
			super.unmodifiable();
			return this;
		}
	}
	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * @return A new builder.
	 */
	public static Builder create(ReaderParser ctx) {
		return new Builder(ctx);
	}
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
	@Override /* Overridden from ParserSesson */
	public final ParserPipe createPipe(Object input) {
		return setPipe(new ParserPipe(input, isDebug(), ctx.isStrict(), ctx.isAutoCloseStreams(), ctx.isUnbuffered(), streamCharset, fileCharset));
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

	@Override /* Overridden from ParserSession */
	public final boolean isReaderParser() {
		return true;
	}
	@Override /* Overridden from ContextSession */
	protected JsonMap properties() {
		return filteredMap("fileCharset", fileCharset, "streamCharset", streamCharset);
	}
}