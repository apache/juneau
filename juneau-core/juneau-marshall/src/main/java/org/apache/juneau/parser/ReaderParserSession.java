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

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.io.*;
import java.nio.charset.*;
import org.apache.juneau.commons.collections.FluentMap;

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
@SuppressWarnings({
	"java:S115" // Constants use UPPER_snakeCase convention
})
public class ReaderParserSession extends ParserSession {

	// Property name constants
	private static final String PROP_fileCharset = "fileCharset";
	private static final String PROP_streamCharset = "streamCharset";
	private static final String PROP_ReaderParserSession_fileCharset = "ReaderParserSession.fileCharset";
	private static final String PROP_ReaderParserSession_streamCharset = "ReaderParserSession.streamCharset";

	// Argument name constants for assertArgNotNull
	private static final String ARG_ctx = "ctx";

	/**
	 * Builder class.
	 */
	public abstract static class Builder<SELF extends Builder<SELF>> extends ParserSession.Builder<SELF> {

		private Charset fileCharset;
		private Charset streamCharset;
		private ReaderParser ctx;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(ReaderParser ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
			this.ctx = ctx;
			fileCharset = ctx.getFileCharset();
			streamCharset = ctx.getStreamCharset();
		}

		@Override
		public ReaderParserSession build() {
			return new ReaderParserSession(this);
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
		public SELF fileCharset(Charset value) {
			fileCharset = value;
			return self();
		}

		@Override /* Overridden from Builder */
		public SELF property(String key, Object value) {
			if (key == null) {
				super.property(key, value);
				return self();
			}
			switch (key) {
				case PROP_fileCharset, PROP_ReaderParserSession_fileCharset:
					return fileCharset(cvt(value, Charset.class));
				case PROP_streamCharset, PROP_ReaderParserSession_streamCharset:
					return streamCharset(cvt(value, Charset.class));
				default:
					super.property(key, value);
					return self();
			}
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
		 * 	<br>Can be <jk>null</jk> (defaults to UTF-8).
		 * @return This object.
		 */
		public SELF streamCharset(Charset value) {
			streamCharset = value;
			return self();
		}

	}

	/**
	 * Concrete default builder leaf for the non-subclassed {@code create()} path (CRTP terminal).
	 */
	public static final class DefaultBuilder extends Builder<DefaultBuilder> {

		DefaultBuilder(ReaderParser ctx) {
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
	public static Builder<?> create(ReaderParser ctx) {
		return new DefaultBuilder(assertArgNotNull(ARG_ctx, ctx));
	}

	private final ReaderParser ctx;
	private final Charset fileCharset;
	private final Charset streamCharset;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected ReaderParserSession(Builder<?> builder) {
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
	 * 			{@link ReaderParser.Builder<?>#streamCharset(Charset)}).
	 * 		<li><code><jk>byte</jk>[]</code> containing UTF-8 encoded text (or whatever the encoding specified by
	 * 			{@link ReaderParser.Builder<?>#streamCharset(Charset)}).
	 * 		<li>{@link File} containing system encoded text (or whatever the encoding specified by
	 * 			{@link ReaderParser.Builder<?>#streamCharset(Charset)}).
	 * 	</ul>
	 * @return
	 * 	A new {@link ParserPipe} wrapper around the specified input object.
	 */
	@SuppressWarnings({
		"resource" // Resource management handled by ParserPipe
	})
	@Override /* Overridden from ParserSesson */
	public final ParserPipe createPipe(Object input) {
		return setPipe(new ParserPipe(input, isDebug(), true, ctx.isAutoCloseStreams(), ctx.isUnbuffered(), streamCharset, fileCharset));
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

	@Override /* Overridden from ParserSession */
	public final boolean isReaderParser() { return true; }

	@Override /* Overridden from ParserSession */
	protected FluentMap<String,Object> properties() {
		return super.properties()
			.a(PROP_fileCharset, fileCharset)
			.a(PROP_streamCharset, streamCharset);
	}
}