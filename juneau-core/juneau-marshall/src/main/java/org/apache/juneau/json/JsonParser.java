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
package org.apache.juneau.json;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.json5.Json5Parser;
import org.apache.juneau.parser.*;
import org.apache.juneau.commons.bean.BeanPropertyMeta;

/**
 * Parses any valid RFC 8259 JSON text into a POJO model.
 *
 * <p>
 * This parser strictly enforces RFC 8259 JSON syntax. Double-quoted strings only; single-quoted strings,
 * comments, trailing commas, and unquoted keys are rejected. For JSON5 syntax (single quotes, comments,
 * etc.), use {@link Json5Parser}.
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * Handles <c>Content-Type</c> types:  <bc>application/json, text/json, application/jcs+json</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * This parser uses a state machine, which makes it very fast and efficient.  It parses JSON in about 70% of the
 * time that it takes the built-in Java DOM parsers to parse equivalent XML.
 *
 * <p>
 * This parser handles all valid RFC 8259 JSON syntax.
 *
 * <p>
 * Also handles negative, decimal, hexadecimal, octal, and double numbers, including exponential notation.
 *
 * <p>
 * This parser handles the following input, and automatically returns the corresponding Java class.
 * <ul class='spaced-list'>
 * 	<li>
 * 		JSON objects (<js>"{...}"</js>) are converted to {@link JsonMap JsonMaps}.
 * 		<b>Note:</b>  If a <code><xa>_type</xa>=<xs>'xxx'</xs></code> attribute is specified on the object, then an
 * 		attempt is made to convert the object to an instance of the specified Java bean class.
 * 		See the {@link org.apache.juneau.MarshallingContext.Builder#typePropertyName(String)} setting for more information about parsing
 * 		beans from JSON.
 * 	<li>
 * 		JSON arrays (<js>"[...]"</js>) are converted to {@link JsonList JsonLists}.
 * 	<li>
 * 		JSON string literals (<js>"\"xyz\""</js>) are converted to {@link String Strings}.
 * 	<li>
 * 		JSON numbers (<js>"123"</js>, including octal/hexadecimal/exponential notation) are converted to
 * 		{@link Integer Integers}, {@link Long Longs}, {@link Float Floats}, or {@link Double Doubles} depending on
 * 		whether the number is decimal, and the size of the number.
 * 	<li>
 * 		JSON booleans (<js>"false"</js>) are converted to {@link Boolean Booleans}.
 * 	<li>
 * 		JSON nulls (<js>"null"</js>) are converted to <jk>null</jk>.
 * </ul>
 *
 * <p>
 * Input can be any of the following:
 * <ul class='spaced-list'>
 * 	<li>
 * 		<js>"{...}"</js> - Converted to an {@link JsonMap} or an instance of a Java bean if a <xa>_type</xa>
 * 		attribute is present.
 * 	<li>
 * 		<js>"[...]"</js> - Converted to an {@link JsonList}.
 * 	<li>
 * 		<js>"123..."</js> - Converted to a {@link Number} (either {@link Integer}, {@link Long}, {@link Float},
 * 		or {@link Double}).
 * 	<li>
 * 		<js>"true"</js>/<js>"false"</js> - Converted to a {@link Boolean}.
 * 	<li>
 * 		<js>"null"</js> - Returns <jk>null</jk>.
 * 	<li>
 * 		<js>"\"xxx\""</js> - Converted to a {@link String} (double quotes only).
 * </ul>
 *
 * <p>
 * TIP:  If you know you're parsing a JSON object or array, it can be easier to parse it using the
 * {@link JsonMap#JsonMap(CharSequence) JsonMap(CharSequence)} or {@link JsonList#JsonList(CharSequence)
 * JsonList(CharSequence)} constructors instead of using this class.
 * The end result should be the same.
 *
 * <h5 class='figure'>Example input (Map of name/age):</h5>
 * <p class='bjson'>
 * 	{<js>"name"</js>:<js>"Alice"</js>,<js>"age"</js>:30}
 * </p>
 *
 * <h5 class='figure'>Complex (nested object + array):</h5>
 * <p class='bjson'>
 * 	{<js>"name"</js>:<js>"Alice"</js>,<js>"age"</js>:30,<js>"address"</js>:{<js>"street"</js>:<js>"123 Main St"</js>,<js>"city"</js>:<js>"Boston"</js>,<js>"state"</js>:<js>"MA"</js>},<js>"tags"</js>:[<js>"a"</js>,<js>"b"</js>,<js>"c"</js>]}
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JsonBasics">JSON Basics</a>
 * 	<li class='link'>{@link Json5Parser} - For JSON5 syntax (single quotes, comments, etc.)
 * </ul>
 */
@SuppressWarnings({
	"java:S110", // Inheritance depth acceptable
	"java:S115" // Constants use UPPER_snakeCase convention
})
public class JsonParser extends ReaderParser implements JsonMetaProvider {

	// Argument name constants for assertArgNotNull
	private static final String ARG_copyFrom = "copyFrom";

	/**
	 * Builder class.
	 */
	public abstract static class Builder<SELF extends Builder<SELF>> extends ReaderParser.Builder<SELF> {

		private static final Cache<HashKey,JsonParser> CACHE = Cache.of(HashKey.class, JsonParser.class).build();

		private boolean validateEnd;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			consumes("application/json,text/json,application/jcs+json");
			validateEnd = env("JsonParser.validateEnd", false);
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(Builder<?> copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			validateEnd = copyFrom.validateEnd;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(JsonParser copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			validateEnd = copyFrom.validateEnd;
		}

		@Override /* Overridden from Context.Builder<?> */
		public JsonParser build() {
			return cache(CACHE).build(JsonParser.class);
		}

		@Override /* Overridden from Context.Builder<?> */
		public abstract SELF copy();

		@Override /* Overridden from Context.Builder<?> */
		public HashKey hashKey() {
			return HashKey.of(super.hashKey(), validateEnd);
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
		 * 	<jc>// Create a parser that validates that there's no garbage at the end of the input.</jc>
		 * 	ReaderParser <jv>parser</jv> = JsonParser.
		 * 		.<jsm>create</jsm>()
		 * 		.validateEnd()
		 * 		.build();
		 *
		 * 	<jc>// Should fail because input has multiple POJOs.</jc>
		 * 	String <jv>json</jv> = <js>"{foo:'bar'}{baz:'qux'}"</js>;
		 * 	MyBean <jv>myBean</jv> =<jv>parser</jv>.parse(<jv>json</jv>, MyBean.<jk>class</jk>);
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
	 * Concrete default builder leaf for the non-subclassed {@link JsonParser#create()} / {@link JsonParser#copy()} path.
	 */
	public static final class DefaultBuilder extends Builder<DefaultBuilder> {

		DefaultBuilder() {}

		DefaultBuilder(JsonParser copyFrom) {
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

	/** Default parser, all default settings.*/
	public static final JsonParser DEFAULT = new JsonParser(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder<?> create() {
		return new DefaultBuilder();
	}

	protected final boolean validateEnd;

	private final Map<BeanPropertyMeta,JsonBeanPropertyMeta> jsonBeanPropertyMetas = new ConcurrentHashMap<>();
	private final Map<ClassMeta<?>,JsonClassMeta> jsonClassMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public JsonParser(Builder<?> builder) {
		super(builder);
		validateEnd = builder.validateEnd;
	}

	@Override /* Overridden from Context */
	public Builder<?> copy() {
		return new DefaultBuilder(this);
	}

	@Override /* Overridden from Context */
	public JsonParserSession.Builder<?> createSession() {
		return JsonParserSession.create(this);
	}

	@Override /* Overridden from JsonMetaProvider */
	public JsonBeanPropertyMeta getJsonBeanPropertyMeta(BeanPropertyMeta bpm) {
		if (bpm == null)
			return JsonBeanPropertyMeta.DEFAULT;
		return jsonBeanPropertyMetas.computeIfAbsent(bpm, k -> new JsonBeanPropertyMeta(k.getDelegateFor(), this));
	}

	@Override /* Overridden from JsonMetaProvider */
	public JsonClassMeta getJsonClassMeta(ClassMeta<?> cm) {
		return jsonClassMetas.computeIfAbsent(cm, k -> new JsonClassMeta(k, this));
	}

	@Override /* Overridden from Context */
	public JsonParserSession getSession() { return createSession().build(); }

	/**
	 * Validate end.
	 *
	 * @see Builder#validateEnd()
	 * @return
	 * 	<jk>true</jk> if after parsing a POJO from the input, verifies that the remaining input in
	 * 	the stream consists of only comments or whitespace.
	 */
	protected final boolean isValidateEnd() { return validateEnd; }
}