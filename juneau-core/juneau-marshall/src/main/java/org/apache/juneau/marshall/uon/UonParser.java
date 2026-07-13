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
package org.apache.juneau.marshall.uon;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.SystemUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.commons.httppart.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.httppart.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.stream.*;

/**
 * Parses UON (a notation for URL-encoded query parameter values) text into POJO models.
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * Handles <c>Content-Type</c> types:  <bc>text/uon</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * This parser uses a state machine, which makes it very fast and efficient.
 *
 * <h5 class='figure'>Example input (Map of name/age):</h5>
 * <p class='bcode'>
 * 	(name=Alice,age=30)
 * </p>
 *
 * <h5 class='figure'>Complex (nested object + array):</h5>
 * <p class='bcode'>
 * 	(name=Alice,age=30,address=(street=123+Main+St,city=Boston,state=MA),tags=@(a,b,c))
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/UonSupport">UON Basics</a>

 * </ul>
 */
@SuppressWarnings({
	"java:S115", // Constants use UPPER_snakeCase convention
	"resource" // Closeable resources are owned by the caller's parser session; Eclipse JDT @Owning warning is by design.
})
public class UonParser extends ReaderParser implements HttpPartParser, UonMetaProvider, RecordReadable {

	// Property name constants
	private static final String PROP_decoding = "decoding";
	private static final String PROP_validateEnd = "validateEnd";

	// Argument name constants for assertArgNotNull
	private static final String ARG_copyFrom = "copyFrom";

	/**
	 * Builder class.
	 */
	@SuppressWarnings({
		"java:S119" // 'SELF' (CRTP self-type) is intentional and clearer than a single-letter name.
	})
	public abstract static class Builder<SELF extends Builder<SELF>> extends ReaderParser.Builder<SELF> {

		private static final Cache<HashKey,UonParser> CACHE = Cache.of(HashKey.class, UonParser.class).build();

		private boolean decoding;
		private boolean validateEnd;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			consumes("text/uon");
			decoding = env("UonParser.decoding", false);
			validateEnd = env("UonParser.validateEnd", false);
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(Builder<?> copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			decoding = copyFrom.decoding;
			validateEnd = copyFrom.validateEnd;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(UonParser copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			decoding = copyFrom.decoding;
			validateEnd = copyFrom.validateEnd;
		}

		@Override /* Overridden from Context.Builder<?> */
		public UonParser build() {
			return cache(CACHE).build(UonParser.class);
		}

		@Override /* Overridden from Context.Builder<?> */
		public abstract SELF copy();

		/**
		 * Decode <js>"%xx"</js> sequences.
		 *
		 * <p>
		 * When enabled, URI encoded characters will be decoded.  Otherwise it's assumed that they've already been decoded
		 * before being passed to this parser.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a decoding UON parser.</jc>
		 * 	ReaderParser <jv>parser</jv> = UonParser.
		 * 		.<jsm>create</jsm>()
		 * 		.decoding()
		 * 		.build();
		 *
		 *  <jc>// Produces: ["foo bar", "baz quz"].</jc>
		 * 	String[] <jv>foo</jv> = <jv>parser</jv>.parse(<js>"@(foo%20bar,baz%20qux)"</js>, String[].<jk>class</jk>);
		 * </p>
		 *
		 * @return This object.
		 */
		public SELF decoding() {
			return decoding(true);
		}

		/**
		 * Same as {@link #decoding()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public SELF decoding(boolean value) {
			decoding = value;
			return self();
		}

		@Override /* Overridden from Context.Builder<?> */
		public HashKey hashKey() {
			// @formatter:off
			return HashKey.of(
				super.hashKey(),
				decoding,
				validateEnd
			);
			// @formatter:on
		}


		/**
		 * Validate end.
		 *
		 * <p>
		 * When enabled, after parsing a POJO from the input, verifies that the remaining input in
		 * the stream consists of only comments or whitespace.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a parser using strict mode.</jc>
		 * 	ReaderParser <jv>parser</jv> = UonParser.
		 * 		.<jsm>create</jsm>()
		 * 		.validateEnd()
		 * 		.build();
		 *
		 * 	<jc>// Should fail because input has multiple POJOs.</jc>
		 * 	String <jv>in</jv> = <js>"(foo=bar)(baz=qux)"</js>;
		 * 	MyBean <jv>myBean</jv> = <jv>parser</jv>.parse(<jv>in</jv>, MyBean.<jk>class</jk>);
		 * </p>
		 *
		 * @return This object.
		 */
		public SELF validateEnd() {
			return validateEnd(true);
		}

		/**
		 * Same as {@link #validateEnd()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public SELF validateEnd(boolean value) {
			validateEnd = value;
			return self();
		}
	}

	/**
	 * Concrete default builder leaf for the non-subclassed {@link UonParser#create()} / {@link UonParser#copy()} path.
	 */
	@SuppressWarnings({
		"java:S110" // Inheritance depth follows the parser builder chain; intentional layered design
	})
	public static final class DefaultBuilder extends Builder<DefaultBuilder> {

		DefaultBuilder() {}

		DefaultBuilder(UonParser copyFrom) {
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

	/** Default parser, decoding. */
	@SuppressWarnings({
		"java:S110" // Inheritance depth acceptable for UonParser.Decoding hierarchy
	})
	public static class Decoding extends UonParser {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		public Decoding(Builder<?> builder) {
			super(builder.decoding());
		}
	}

	/** Reusable instance of {@link UonParser}, all default settings. */
	public static final UonParser DEFAULT = new UonParser(create());
	/** Reusable instance of {@link UonParser} with decodeChars set to true. */
	public static final UonParser DEFAULT_DECODING = new UonParser.Decoding(create());

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

	protected final boolean decoding;
	protected final boolean validateEnd;

	private final Map<BeanPropertyMeta,UonBeanPropertyMeta> uonBeanPropertyMetas = new ConcurrentHashMap<>();
	private final Map<ClassMeta<?>,UonClassMeta> uonClassMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public UonParser(Builder<?> builder) {
		super(builder);
		decoding = builder.decoding;
		validateEnd = builder.validateEnd;
	}

	@Override /* Overridden from Context */
	public Builder<?> copy() {
		return new DefaultBuilder(this);
	}

	@Override /* Overridden from Context */
	public UonParserSession.Builder<?> createSession() {
		return UonParserSession.create(this);
	}

	@Override
	public <T> ClassMeta<T> getClassMeta(Class<T> c) {
		return getMarshallingContext().getClassMeta(c);
	}

	@Override
	public <T> ClassMeta<T> getClassMeta(Type t, Type...args) {
		return getMarshallingContext().getClassMeta(t, args);
	}

	@Override /* Overridden from HttpPartParser */
	public UonParserSession getPartSession() { return UonParserSession.create(this).build(); }

	@Override /* Overridden from Context */
	public UonParserSession getSession() { return createSession().build(); }

	/**
	 * Convenience delegator that opens a whole-value {@link RecordReader} over the input using
	 * <b>default session arguments</b> (mirrors {@link #parse(Object, Class)}).
	 *
	 * <p>
	 * The real implementation lives on {@link UonParserSession#parseRecords(Object)}.  Callers
	 * that need request-derived configuration (locale, timezone, schema, swaps) should call
	 * {@link #createSession()} and invoke {@link UonParserSession#parseRecords(Object)} on the
	 * built session instead.
	 *
	 * @param input The input.
	 * @return A new {@link RecordReader} cursor.
	 * @throws IOException If a problem occurred opening the underlying input.
	 */
	@Override /* RecordReadable */
	public RecordReader parseRecords(Object input) throws IOException {
		return getSession().parseRecords(input);
	}

	@Override /* RecordReadable */
	public boolean isRecordStreaming() {
		return false;
	}

	@Override /* Overridden from UonMetaProvider */
	public UonBeanPropertyMeta getUonBeanPropertyMeta(BeanPropertyMeta bpm) {
		if (bpm == null)
			return UonBeanPropertyMeta.DEFAULT;
		return uonBeanPropertyMetas.computeIfAbsent(bpm, k -> new UonBeanPropertyMeta(k.getDelegateFor(), this));
	}

	@Override /* Overridden from UonMetaProvider */
	public UonClassMeta getUonClassMeta(ClassMeta<?> cm) {
		return uonClassMetas.computeIfAbsent(cm, k -> new UonClassMeta(k, this));
	}

	/**
	 * Converts the specified input to the specified class type.
	 *
	 * @param <T> The POJO type to transform the input into.
	 * @param partType The part type being parsed.
	 * @param schema
	 * 	Schema information about the part.
	 * 	<br>May be <jk>null</jk>.
	 * 	<br>Not all part parsers use the schema information.
	 * @param in The input being parsed.
	 * @param toType The POJO type to transform the input into.
	 * @return The parsed value.
	 * @throws ParseException Malformed input encountered.
	 * @throws SchemaValidationException If the input or resulting HTTP part object fails schema validation.
	 */
	public <T> T parse(HttpPartType partType, HttpPartSchema schema, String in, Class<T> toType) throws ParseException, SchemaValidationException {
		return getPartSession().parse(partType, schema, in, getClassMeta(toType));
	}

	/**
	 * Converts the specified input to the specified class type.
	 *
	 * @param <T> The POJO type to transform the input into.
	 * @param partType The part type being parsed.
	 * @param schema
	 * 	Schema information about the part.
	 * 	<br>May be <jk>null</jk>.
	 * 	<br>Not all part parsers use the schema information.
	 * @param in The input being parsed.
	 * @param toType The POJO type to transform the input into.
	 * @return The parsed value.
	 * @throws ParseException Malformed input encountered.
	 * @throws SchemaValidationException If the input or resulting HTTP part object fails schema validation.
	 */
	public <T> T parse(HttpPartType partType, HttpPartSchema schema, String in, ClassMeta<T> toType) throws ParseException, SchemaValidationException {
		return getPartSession().parse(partType, schema, in, toType);
	}

	/**
	 * Converts the specified input to the specified class type.
	 *
	 * @param <T> The POJO type to transform the input into.
	 * @param partType The part type being parsed.
	 * @param schema
	 * 	Schema information about the part.
	 * 	<br>May be <jk>null</jk>.
	 * 	<br>Not all part parsers use the schema information.
	 * @param in The input being parsed.
	 * @param toType The POJO type to transform the input into.
	 * @param toTypeArgs The generic type arguments of the POJO type to transform the input into.
	 * @return The parsed value.
	 * @throws ParseException Malformed input encountered.
	 * @throws SchemaValidationException If the input or resulting HTTP part object fails schema validation.
	 */
	public <T> T parse(HttpPartType partType, HttpPartSchema schema, String in, Type toType, Type...toTypeArgs) throws ParseException, SchemaValidationException {
		return getPartSession().parse(partType, schema, in, getClassMeta(toType, toTypeArgs));
	}

	/**
	 * Decode <js>"%xx"</js> sequences enabled
	 *
	 * @see Builder#decoding()
	 * @return
	 * 	<jk>true</jk> if URI encoded characters should be decoded, <jk>false</jk> if they've already been decoded
	 * 	before being passed to this parser.
	 */
	protected final boolean isDecoding() { return decoding; }

	/**
	 * Validate end enabled.
	 *
	 * @see Builder#validateEnd()
	 * @return
	 * 	<jk>true</jk> if after parsing a POJO from the input, verifies that the remaining input in
	 * 	the stream consists of only comments or whitespace.
	 */
	protected final boolean isValidateEnd() { return validateEnd; }

	@Override /* Overridden from ReaderParser */
	protected FluentMap<String,Object> properties() {
		return super.properties()
			.a(PROP_decoding, decoding)
			.a(PROP_validateEnd, validateEnd);
	}
}