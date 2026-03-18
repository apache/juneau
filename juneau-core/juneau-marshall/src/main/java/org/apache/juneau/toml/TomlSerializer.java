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
import org.apache.juneau.serializer.*;

/**
 * Serializes POJOs to TOML v1.0.0 format.
 *
 * <p>
 * TOML (Tom's Obvious Minimal Language) maps unambiguously to hash tables with native typing.
 * See <a href="https://toml.io/en/v1.0.0">TOML v1.0.0</a> specification.
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * Produces: <bc>application/toml</bc>
 * <br>Accepts: <bc>application/toml</bc>
 *
 * <h5 class='topic'>Bean-to-TOML mapping</h5>
 * <ul class='spaced-list'>
 * 	<li>Simple properties → key-value pairs
 * 	<li>Nested beans → <c>[tableName]</c> sections
 * 	<li>Collections of beans → <c>[[tableName]]</c> array of tables
 * 	<li>Collections of simple values → <c>key = [a, b, c]</c>
 * 	<li>Maps with string keys → <c>[mapName]</c> table
 * 	<li>Null properties are omitted (TOML has no null type)
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Serialize a bean to TOML</jc>
 * 	String <jv>toml</jv> = TomlSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>myBean</jv>);
 *
 * 	<jc>// Serialize a map (e.g. config with nested sections)</jc>
 * 	Map&lt;String, Object&gt; <jv>config</jv> = Map.of(<js>"name"</js>, <js>"myapp"</js>, <js>"port"</js>, 8080);
 * 	<jv>toml</jv> = TomlSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>config</jv>);
 *
 * 	<jc>// Use the marshaller for convenience</jc>
 * 	<jv>toml</jv> = Toml.<jsm>of</jsm>(<jv>config</jv>);
 *
 * 	<jc>// Custom serializer with whitespace for readability</jc>
 * 	TomlSerializer <jv>s</jv> = TomlSerializer.<jsm>create</jsm>().ws().build();
 * 	<jv>toml</jv> = <jv>s</jv>.serialize(<jv>config</jv>);
 * </p>
 *
 * <h5 class='figure'>Example output (Map of name/age):</h5>
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
 * <h5 class='topic'>Limitations compared to JSON</h5>
 * <p>
 * TOML is a configuration format with a more restricted type system than JSON.
 * The following features supported by {@link org.apache.juneau.json.JsonSerializer JsonSerializer}
 * are not available or limited in TOML:
 * </p>
 * <ul class='spaced-list'>
 * 	<li><b>Null values</b> — TOML has no null type. By default, null properties are omitted.
 * 		Use <c>keepNullProperties()</c> to write the configured <c>nullValue</c> string (e.g. <c>&lt;NULL&gt;</c>) instead.
 * 	<li><b>Map keys</b> — TOML tables only support string keys. Maps with non-string keys
 * 		(e.g. <c>Map&lt;Integer,String&gt;</c>) have keys converted via <c>toString()</c>; ensure results are valid TOML keys.
 * 		Null keys are serialized as the string <c>null</c>.
 * 	<li><b>Polymorphic types</b> — <c>addBeanTypes()</c> and <c>addRootType()</c> add <c>_type</c>
 * 		discriminators for polymorphic parsing; use with care as TOML structure differs from JSON.
 * 	<li><b>Root-level collections</b> — TOML expects a root table. Root arrays use array-of-tables
 * 		(<c>[[item]]</c>) for beans/maps, or a <c>_value</c> wrapper for primitives and simple arrays.
 * 	<li><b>Duplicate keys</b> — TOML forbids duplicate keys; structures that would produce duplicate
 * 		keys may yield invalid TOML.
 * </ul>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 */
@SuppressWarnings({
	"java:S110", "java:S115"
})
public class TomlSerializer extends WriterSerializer {

	private static final String PROP_inlineTableThreshold = "inlineTableThreshold";
	private static final String PROP_useInlineTables = "useInlineTables";
	private static final String PROP_sortKeys = "sortKeys";
	private static final String PROP_nullValue = "nullValue";
	private static final String ARG_copyFrom = "copyFrom";

	/**
	 * Builder class.
	 */
	public static class Builder extends WriterSerializer.Builder {

		private static final Cache<HashKey,TomlSerializer> CACHE = Cache.of(HashKey.class, TomlSerializer.class).build();

		private int inlineTableThreshold = 3;
		private boolean useInlineTables = true;
		private boolean sortKeys = false;
		private String nullValue = "<NULL>";

		protected Builder() {
			produces("application/toml");
			accept("application/toml");
		}

		protected Builder(Builder copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			inlineTableThreshold = copyFrom.inlineTableThreshold;
			useInlineTables = copyFrom.useInlineTables;
			sortKeys = copyFrom.sortKeys;
			nullValue = copyFrom.nullValue;
		}

		protected Builder(TomlSerializer copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			inlineTableThreshold = copyFrom.inlineTableThreshold;
			useInlineTables = copyFrom.useInlineTables;
			sortKeys = copyFrom.sortKeys;
			nullValue = copyFrom.nullValue;
		}

		/**
		 * String to write for null values. Parser treats values matching this as null.
		 *
		 * <p>
		 * Default is {@code <NULL>} to avoid confusion with the literal string {@code "null"}.
		 *
		 * @param value The null marker string.
		 * @return This object.
		 */
		public Builder nullValue(String value) {
			nullValue = value;
			return this;
		}

		/**
		 * Maximum properties for inline table format.
		 *
		 * @param value The threshold.
		 * @return This object.
		 */
		public Builder inlineTableThreshold(int value) {
			inlineTableThreshold = value;
			return this;
		}

		/**
		 * Use inline tables when possible.
		 *
		 * @param value The flag.
		 * @return This object.
		 */
		public Builder useInlineTables(boolean value) {
			useInlineTables = value;
			return this;
		}

		/**
		 * Sort keys alphabetically in output.
		 *
		 * @param value The flag.
		 * @return This object.
		 */
		public Builder sortKeys(boolean value) {
			sortKeys = value;
			return this;
		}

		@Override
		public Builder useWhitespace() {
			super.useWhitespace();
			return this;
		}

		@Override
		public Builder useWhitespace(boolean value) {
			super.useWhitespace(value);
			return this;
		}

		@Override
		public Builder ws() {
			return useWhitespace();
		}

		@Override
		public TomlSerializer build() {
			return cache(CACHE).build(TomlSerializer.class);
		}

		@Override
		public Builder copy() {
			return new Builder(this);
		}

		@Override
		public HashKey hashKey() {
			return HashKey.of(super.hashKey(), inlineTableThreshold, useInlineTables, sortKeys, nullValue);
		}
	}

	/** Default serializer. */
	public static final TomlSerializer DEFAULT = new TomlSerializer(create());

	/** Default serializer with blank lines between sections. */
	public static final TomlSerializer DEFAULT_READABLE = new TomlSerializer(create().ws());

	public static Builder create() {
		return new Builder();
	}

	protected final int inlineTableThreshold;
	protected final boolean useInlineTables;
	protected final boolean sortKeys;
	/** String written for null values. */
	protected final String nullValue;

	/**
	 * Constructor.
	 *
	 * @param builder The builder.
	 */
	public TomlSerializer(Builder builder) {
		super(builder);
		inlineTableThreshold = builder.inlineTableThreshold;
		useInlineTables = builder.useInlineTables;
		sortKeys = builder.sortKeys;
		nullValue = builder.nullValue != null ? builder.nullValue : "<NULL>";
	}

	/**
	 * Returns the string written for null values.
	 *
	 * @return The null marker.
	 */
	public String getNullValue() {
		return nullValue;
	}

	@Override
	public TomlSerializerSession.Builder createSession() {
		return TomlSerializerSession.create(this);
	}

	@Override
	public Builder copy() {
		return new Builder(this);
	}

	@Override
	protected FluentMap<String,Object> properties() {
		return super.properties()
			.a(PROP_inlineTableThreshold, inlineTableThreshold)
			.a(PROP_useInlineTables, useInlineTables)
			.a(PROP_sortKeys, sortKeys)
			.a(PROP_nullValue, nullValue);
	}
}
