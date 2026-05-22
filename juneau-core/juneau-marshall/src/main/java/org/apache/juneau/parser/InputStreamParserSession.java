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

import org.apache.juneau.commons.http.MediaType;
import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.httppart.*;

/**
 * Subclass of parser session objects for byte-based parsers.
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
public class InputStreamParserSession extends ParserSession {

	// Argument name constants for assertArgNotNull
	private static final String ARG_ctx = "ctx";

	/**
	 * Builder class.
	 */
	public static class Builder extends ParserSession.Builder {

		private InputStreamParser ctx;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(InputStreamParser ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
			this.ctx = ctx;
		}

		@Override /* Overridden from Builder */
		public <T> Builder apply(Class<T> type, Consumer<T> apply) {
			super.apply(type, apply);
			return this;
		}

		@Override
		public InputStreamParserSession build() {
			return new InputStreamParserSession(this);
		}

		@Override /* Overridden from Builder */
		public Builder debug(Boolean value) {
			super.debug(value);
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
	 * 	<br>Cannot be <jk>null</jk>.
	 * @return A new builder.
	 */
	public static Builder create(InputStreamParser ctx) {
		return new Builder(assertArgNotNull(ARG_ctx, ctx));
	}

	private final InputStreamParser ctx;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected InputStreamParserSession(Builder builder) {
		super(builder);
		this.ctx = builder.ctx;
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
	 * 		<li>{@link InputStream}
	 * 		<li><code><jk>byte</jk>[]</code>
	 * 		<li>{@link File}
	 * 		<li>{@link CharSequence} containing encoded bytes according to the {@link org.apache.juneau.MarshallingContext.Builder#binaryFormat(BinaryFormat)} setting.
	 * 	</ul>
	 * @return
	 * 	A new {@link ParserPipe} wrapper around the specified input object.
	 */
	@SuppressWarnings({
		"resource" // Resource management handled by ParserPipe
	})
	@Override /* Overridden from ParserSession */
	public final ParserPipe createPipe(Object input) {
		return setPipe(new ParserPipe(input, isDebug(), ctx.isAutoCloseStreams(), ctx.isUnbuffered(), getBinaryFormat()));
	}

	@Override /* Overridden from ParserSession */
	public final boolean isReaderParser() { return false; }

	/**
	 * Binary input format.
	 *
	 * @see MarshallingContext.Builder#binaryFormat(BinaryFormat)
	 * @return
	 * 	The format to use when converting strings to byte arrays.
	 */
	protected final BinaryFormat getBinaryFormat() { return ctx.getMarshallingContext().getBinaryFormat(); }

	/**
	 * Returns whether this binary parser has a native byte-array wire type.
	 *
	 * <p>
	 * Mirror of {@link org.apache.juneau.serializer.OutputStreamSerializerSession#hasNativeBytes()}
	 * for the parse side.  When {@code true}, {@code byte[]} payloads are consumed natively from the
	 * binary stream (e.g. MsgPack {@code bin} family, CBOR byte-string major type 2, BSON binary
	 * subtype {@code 0x05}) and {@link org.apache.juneau.swaps.BinarySwap} skips so the raw bytes
	 * reach the bean property directly.  When {@code false} the parser surfaces {@code byte[]}
	 * payloads as a string (UTF-8 column for Parquet, plain literal for binary RDF) and
	 * {@code BinarySwap} fires to apply the configured {@link BinaryFormat} reverse decoding back
	 * to {@code byte[]}.
	 *
	 * <p>
	 * Defaults to {@code true}.  Subclasses without a native byte-array wire type
	 * (e.g. {@link org.apache.juneau.parquet.ParquetParserSession} and the binary RDF parser
	 * sessions) override and return {@code false}.
	 *
	 * @return {@code true} if this parser has a native byte-array wire type.
	 */
	public boolean hasNativeBytes() {
		return true;
	}
}