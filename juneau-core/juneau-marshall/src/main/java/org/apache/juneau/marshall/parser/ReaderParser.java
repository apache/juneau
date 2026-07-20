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
package org.apache.juneau.marshall.parser;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.IoUtils.*;
import static org.apache.juneau.commons.utils.SystemUtils.*;

import java.nio.charset.*;

import org.apache.juneau.commons.collections.*;
import org.apache.juneau.marshall.*;

/**
 * Subclass of {@link Parser} for character-based parsers.
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * This class is typically the parent class of all character-based parsers.
 * It has 1 abstract method to implement on the session object...
 * <ul>
 * 	<li>{@link ParserSession#doRead(ParserPipe, ClassMeta)}
 * </ul>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SerializersAndParsers">Serializers and Parsers</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S115" // Constants use UPPER_snakeCase convention
})
public class ReaderParser extends Parser {

	// Property name constants
	private static final String PROP_streamCharset = "streamCharset";

	// Argument name constants for assertArgNotNull
	private static final String ARG_copyFrom = "copyFrom";

	/**
	 * Builder class.
	 */
	@SuppressWarnings({
		"java:S119" // 'SELF' (CRTP self-type) is intentional and clearer than a single-letter name.
	})
	public abstract static class Builder<SELF extends Builder<SELF>> extends Parser.Builder<SELF> {

		private Charset streamCharset;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			streamCharset = env("ReaderParser.streamCharset", UTF8);
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(Builder<?> copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			streamCharset = copyFrom.streamCharset;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(ReaderParser copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			streamCharset = copyFrom.streamCharset;
		}

		@Override /* Overridden from Context.Builder<?> */
		public ReaderParser build() {
			return build(ReaderParser.class);
		}

		@Override /* Overridden from Context.Builder<?> */
		public abstract SELF copy();

		@Override /* Overridden from Context.Builder<?> */
		public HashKey hashKey() {
			// @formatter:off
			return HashKey.of(
				super.hashKey(),
				streamCharset
			);
			// @formatter:on
		}

		/**
		 * Input stream charset.
		 *
		 * <p>
		 * The character set to use for converting <c>InputStreams</c> and byte arrays to readers.
		 *
		 * <p>
		 * Used when passing in input streams and byte arrays to {@link Parser#read(Object, Class)}.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a parser that reads UTF-8 files.</jc>
		 * 	ReaderParser <jv>parser</jv> = JsonParser
		 * 		.<jsm>create</jsm>()
		 * 		.streamCharset(Charset.<jsm>forName</jsm>(<js>"UTF-8"</js>))
		 * 		.build();
		 *
		 * 	<jc>// Use it to read a UTF-8 encoded input stream.</jc>
		 * 	MyBean <jv>myBean</jv> = <jv>parser</jv>.read(<jk>new</jk> FileInputStream(<js>"MyBean.txt"</js>), MyBean.<jk>class</jk>);
		 * </p>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default value is <js>"UTF-8"</js>.
		 * 	<br>Can be <jk>null</jk> (defaults to UTF-8).
		 * @return This object.
		 */
		public SELF streamCharset(Charset value) {
			streamCharset = value;
			return self();
		}


	}

	/**
	 * Concrete default builder leaf for the non-subclassed {@link ReaderParser#create()} / {@link ReaderParser#copy()} path.
	 */
	public static final class DefaultBuilder extends Builder<DefaultBuilder> {

		DefaultBuilder() {}

		DefaultBuilder(ReaderParser copyFrom) {
			super(copyFrom);
		}

		DefaultBuilder(Builder<?> copyFrom) {
			super(copyFrom);
		}

		@Override /* Overridden from Context.Builder<?> */
		public DefaultBuilder copy() {
			return new DefaultBuilder(this);
		}
	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	@SuppressWarnings({
		"java:S1452" // Builder<?> wildcard return intentional; callers chain via fluent API without needing the concrete type
	})
	public static Builder<?> create() {
		return new DefaultBuilder();
	}

	private final Charset streamCharset;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 * 	<br>Cannot be <jk>null</jk>.
	 */
	protected ReaderParser(Builder<?> builder) {
		super(builder);
		streamCharset = builder.streamCharset;
	}

	@Override /* Overridden from Context */
	public ReaderParserSession.Builder<?> createSession() {
		return ReaderParserSession.create(this);
	}

	@Override /* Overridden from Context */
	public ReaderParserSession getSession() { return createSession().build(); }

	@Override /* Overridden from Parser */
	public final boolean isReaderParser() { return true; }

	/**
	 * Input stream charset.
	 *
	 * @see Builder#streamCharset(Charset)
	 * @return
	 * 	The character set to use for converting <c>InputStreams</c> and byte arrays to readers.
	 */
	protected final Charset getStreamCharset() { return streamCharset; }

	@Override /* Overridden from Parser */
	protected FluentMap<String,Object> properties() {
		return super.properties()
			.a(PROP_streamCharset, streamCharset);
	}
}