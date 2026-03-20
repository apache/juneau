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
package org.apache.juneau.toml;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import org.apache.juneau.commons.collections.*;
import org.apache.juneau.json.JsonParser;
import org.apache.juneau.parser.*;

/**
 * Parses TOML v1.0.0 text into POJOs.
 *
 * <p>
 * Handles <c>Content-Type</c>: <bc>application/toml</bc>
 *
 * <h5 class='topic'>Limitations compared to JSON</h5>
 * <p>
 * TOML is a configuration format with a more restricted type system than JSON.
 * The following features supported by {@link JsonParser}
 * are not available or limited in TOML:
 * </p>
 * <ul class='spaced-list'>
 * 	<li><b>Null values</b> — TOML has no null type. Absent keys yield null for object properties.
 * 		The parser treats the configured <c>nullValue</c> string (e.g. <c>&lt;NULL&gt;</c>) as Java null
 * 		when it appears as a value.
 * 	<li><b>Non-string map keys</b> — TOML keys are always strings. Non-string key types
 * 		(e.g. <c>Map&lt;Integer,String&gt;</c>) are converted via {@link org.apache.juneau.parser.ParserSession#convertAttrToType(Object, String, org.apache.juneau.ClassMeta) convertAttrToType}.
 * 		Null keys are serialized as the string <c>null</c> and converted back to Java null during parsing.
 * 	<li><b>Polymorphic types</b> — Parsing to interfaces or abstract classes requires a
 * 		{@link org.apache.juneau.BeanContext.Builder#beanDictionary(Class[]) bean dictionary}
 * 		when no <c>_type</c> discriminator is present in the document.
 * 	<li><b>Duration</b> — TOML has no native duration type. Quoted ISO-8601 duration strings
 * 		(e.g. <c>"PT1H30M"</c>) are parsed to <c>Duration</c> via {@link org.apache.juneau.utils.Iso8601Utils}.
 * 	<li><b>Year and YearMonth</b> — Bare <c>Year</c> (4-digit integer) parses as <c>Long</c>, not <c>Year</c>.
 * 		Use quoted strings (e.g. <c>"2024"</c>) or <c>yyyy-MM</c> for <c>YearMonth</c>; swaps may be needed
 * 		for full compatibility.
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Parse TOML into a JsonMap</jc>
 * 	String <jv>toml</jv> = <js>"name = \"myapp\"\nport = 8080"</js>;
 * 	JsonMap <jv>parsed</jv> = TomlParser.<jsf>DEFAULT</jsf>.parse(<jv>toml</jv>, JsonMap.<jk>class</jk>);
 *
 * 	<jc>// Parse into a bean</jc>
 * 	MyConfig <jv>config</jv> = TomlParser.<jsf>DEFAULT</jsf>.parse(<jv>toml</jv>, MyConfig.<jk>class</jk>);
 *
 * 	<jc>// Parse into a parameterized type (e.g. Map with type args)</jc>
 * 	Map&lt;String, Object&gt; <jv>map</jv> = TomlParser.<jsf>DEFAULT</jsf>.parse(<jv>toml</jv>, Map.<jk>class</jk>, String.<jk>class</jk>, Object.<jk>class</jk>);
 *
 * 	<jc>// Use the marshaller for convenience</jc>
 * 	<jv>parsed</jv> = Toml.<jsm>to</jsm>(<jv>toml</jv>, JsonMap.<jk>class</jk>);
 * </p>
 *
 * <h5 class='figure'>Example input (Map of name/age):</h5>
 * <p class='bcode'>
 * 	name = "Alice"
 * 	age = 30
 * </p>
 *
 * <h5 class='figure'>Complex (nested table + array):</h5>
 * <p class='bcode'>
 * 	name = "Alice"
 * 	age = 30
 * 	tags = ["a", "b", "c"]
 *
 * 	[address]
 * 	street = "123 Main St"
 * 	city = "Boston"
 * 	state = "MA"
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a href="https://toml.io/en/v1.0.0">TOML v1.0.0 Specification</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S110", // Builder pattern requires many parameters
	"java:S115"  // ARG_ prefix follows framework convention
})
public class TomlParser extends ReaderParser {

	private static final String ARG_copyFrom = "copyFrom";

	private static final String PROP_nullValue = "nullValue";

	/**
	 * Builder for {@link TomlParser}.
	 */
	public static class Builder extends ReaderParser.Builder {

		private static final Cache<HashKey,TomlParser> CACHE = Cache.of(HashKey.class, TomlParser.class).build();

		private String nullValue = "<NULL>";

		protected Builder() {
			consumes("application/toml");
		}

		protected Builder(Builder copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			nullValue = copyFrom.nullValue;
		}

		protected Builder(TomlParser copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			nullValue = copyFrom.nullValue;
		}

		/**
		 * String that denotes null when parsing. Must match the serializer's nullValue.
		 *
		 * @param value The null marker string.
		 * @return This object.
		 */
		public Builder nullValue(String value) {
			nullValue = value;
			return this;
		}

		@Override
		public TomlParser build() {
			return cache(CACHE).build(TomlParser.class);
		}

		@Override
		public Builder copy() {
			return new Builder(this);
		}

		@Override
		public HashKey hashKey() {
			return HashKey.of(super.hashKey(), nullValue);
		}
	}

	/** Default parser instance. */
	public static final TomlParser DEFAULT = new TomlParser(create());

	/**
	 * Creates a new builder.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	protected final String nullValue;

	/**
	 * Constructor.
	 *
	 * @param builder The builder.
	 */
	public TomlParser(Builder builder) {
		super(builder);
		nullValue = builder.nullValue != null ? builder.nullValue : "<NULL>";
	}

	/**
	 * Returns the string treated as null when parsing.
	 *
	 * @return The null marker.
	 */
	public String getNullValue() {
		return nullValue;
	}

	@Override
	public TomlParserSession.Builder createSession() {
		return TomlParserSession.create(this);
	}

	@Override
	public Builder copy() {
		return new Builder(this);
	}

	@Override
	protected FluentMap<String, Object> properties() {
		return super.properties().a(PROP_nullValue, nullValue);
	}
}
