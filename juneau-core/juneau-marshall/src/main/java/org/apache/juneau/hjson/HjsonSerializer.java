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
package org.apache.juneau.hjson;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.serializer.*;

/**
 * Serializes POJO models to Hjson (Human JSON).
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * Handles <c>Accept</c> types:  <bc>application/hjson, application/hjson+json</bc>
 * <p>
 * Produces <c>Content-Type</c> types:  <bc>application/hjson</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * Hjson is a syntax extension of JSON designed for human-friendly configuration files and hand-edited data.
 * This serializer produces output that is valid Hjson and can be parsed back by {@link HjsonParser}.
 *
 * <p>
 * The conversion is as follows:
 * <ul class='spaced-list'>
 * 	<li>
 * 		Maps (e.g. {@link java.util.HashMap HashMaps}, {@link java.util.TreeMap TreeMaps}) are converted to Hjson objects.
 * 	<li>
 * 		Collections (e.g. {@link java.util.HashSet HashSets}, {@link java.util.LinkedList LinkedLists}) and Java arrays are converted to
 * 		Hjson arrays.
 * 	<li>
 * 		{@link String Strings} are serialized as quoteless values when they contain only safe characters.
 * 		Strings that would be misinterpreted as booleans, null, or numbers are automatically double-quoted.
 * 	<li>
 * 		{@link Number Numbers} (e.g. {@link Integer}, {@link Long}, {@link Double}) are converted to Hjson numbers.
 * 	<li>
 * 		{@link Boolean Booleans} are converted to {@code true} or {@code false}.
 * 	<li>
 * 		{@code null} values are converted to Hjson {@code null}.
 * 	<li>
 * 		Beans are converted to Hjson objects.
 * </ul>
 *
 * <p>
 * The types above are considered "Hjson-primitive" object types.
 * Any non-Hjson-primitive object types are transformed into Hjson-primitive object types through
 * {@link org.apache.juneau.swap.ObjectSwap ObjectSwaps} associated through the
 * {@link org.apache.juneau.BeanContext.Builder#swaps(Class...) BeanContext.Builder.swaps(Class...)} method.
 * Several default transforms are provided for transforming Dates, Enums, Iterators, etc.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Use one of the default serializers to serialize a POJO</jc>
 * 	String <jv>hjson</jv> = HjsonSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>someObject</jv>);
 *
 * 	<jc>// Create a compact serializer (comma-separated, no newlines between members)</jc>
 * 	HjsonSerializer <jv>compact</jv> = HjsonSerializer.<jsm>create</jsm>().compact().build();
 *
 * 	<jc>// Clone an existing serializer and enable multiline strings</jc>
 * 	<jv>compact</jv> = HjsonSerializer.<jsf>DEFAULT</jsf>.copy().useMultilineStrings(<jk>true</jk>).build();
 * </p>
 *
 * <h5 class='figure'>Example output - readable mode (Map of name/age):</h5>
 * <p class='bjson'>
 * 	{
 * 	  name: Alice
 * 	  age: 30
 * 	}
 * </p>
 *
 * <h5 class='figure'>Example output - compact mode:</h5>
 * <p class='bjson'>
 * 	{name:Alice,age:30}
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>String values equal to <c>true</c>, <c>false</c>, <c>null</c>, or parseable as numbers
 * 		are automatically serialized in double quotes to prevent parse ambiguity.
 * 	<li class='note'>Empty strings are always serialized as <c>""</c>.
 * 	<li class='note'>Root-level objects always include enclosing braces <c>{}</c> by default.
 * 		Use {@link Builder#omitRootBraces(boolean)} to suppress them.
 * 	<li class='note'>In compact mode, strings containing newlines are serialized as double-quoted strings
 * 		with JSON escape sequences rather than multiline <c>'''</c> blocks.
 * 	<li class='note'>Format-specific field-level annotations ({@link Hjson @Hjson}) are supported
 * 		but intentionally minimal in the initial implementation.
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/HjsonBasics">Hjson Basics</a>
 * 	<li class='link'><a class="doclink" href="https://hjson.github.io/syntax.html">Hjson Specification</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S110", "java:S115"
})
public class HjsonSerializer extends WriterSerializer implements HjsonMetaProvider {

	private static final String ARG_copyFrom = "copyFrom";

	private final java.util.concurrent.ConcurrentHashMap<ClassMeta<?>, HjsonClassMeta> hjsonClassMetas = new java.util.concurrent.ConcurrentHashMap<>();
	private final java.util.concurrent.ConcurrentHashMap<BeanPropertyMeta, HjsonBeanPropertyMeta> hjsonBeanPropertyMetas = new java.util.concurrent.ConcurrentHashMap<>();

	/** Use multiline ''' for strings with newlines. */
	protected final boolean useMultilineStrings;

	/** Use quoteless strings for simple values. */
	protected final boolean useQuotelessStrings;

	/** Use quoteless keys when safe. */
	protected final boolean useQuotelessKeys;

	/** Omit root object braces. */
	protected final boolean omitRootBraces;

	/** Use newlines instead of commas between members. */
	protected final boolean useNewlineSeparators;

	/**
	 * Builder class.
	 */
	public static class Builder extends WriterSerializer.Builder {

		private static final Cache<HashKey, HjsonSerializer> CACHE = Cache.of(HashKey.class, HjsonSerializer.class).build();

		private boolean useMultilineStrings = true;
		private boolean useQuotelessStrings = true;
		private boolean useQuotelessKeys = true;
		private boolean omitRootBraces = false;
		private boolean useNewlineSeparators = true;

		protected Builder() {
			produces("application/hjson");
			accept("application/hjson,application/hjson+json");
		}

		protected Builder(Builder copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			useMultilineStrings = copyFrom.useMultilineStrings;
			useQuotelessStrings = copyFrom.useQuotelessStrings;
			useQuotelessKeys = copyFrom.useQuotelessKeys;
			omitRootBraces = copyFrom.omitRootBraces;
			useNewlineSeparators = copyFrom.useNewlineSeparators;
		}

		protected Builder(HjsonSerializer copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			useMultilineStrings = copyFrom.useMultilineStrings;
			useQuotelessStrings = copyFrom.useQuotelessStrings;
			useQuotelessKeys = copyFrom.useQuotelessKeys;
			omitRootBraces = copyFrom.omitRootBraces;
			useNewlineSeparators = copyFrom.useNewlineSeparators;
		}

		/**
		 * Enables multiline strings for values containing newlines.
		 *
		 * @param value <jk>true</jk> to use <c>'''</c> blocks for newline-containing strings.
		 * @return This object.
		 */
		public Builder useMultilineStrings(boolean value) {
			useMultilineStrings = value;
			return this;
		}

		/**
		 * Enables quoteless string output when values are unambiguous.
		 *
		 * @param value <jk>true</jk> to emit unquoted strings when safe.
		 * @return This object.
		 */
		public Builder useQuotelessStrings(boolean value) {
			useQuotelessStrings = value;
			return this;
		}

		/**
		 * Enables quoteless object keys when safe.
		 *
		 * @param value <jk>true</jk> to emit unquoted keys when safe.
		 * @return This object.
		 */
		public Builder useQuotelessKeys(boolean value) {
			useQuotelessKeys = value;
			return this;
		}

		/**
		 * Omits root-level object braces <c>{}</c> in output.
		 *
		 * @param value <jk>true</jk> to omit root braces.
		 * @return This object.
		 */
		public Builder omitRootBraces(boolean value) {
			omitRootBraces = value;
			return this;
		}

		/**
		 * Uses newlines instead of commas between object members.
		 *
		 * @param value <jk>true</jk> for newline-separated members.
		 * @return This object.
		 */
		public Builder useNewlineSeparators(boolean value) {
			useNewlineSeparators = value;
			return this;
		}

		/**
		 * Creates compact output (comma-separated, single line).
		 *
		 * @return This object.
		 */
		public Builder compact() {
			useNewlineSeparators = false;
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

		@Override /* Overridden from WriterSerializer.Builder */
		public Builder ws() {
			return useWhitespace();
		}

		@Override
		public HjsonSerializer build() {
			return cache(CACHE).build(HjsonSerializer.class);
		}

		@Override
		public Builder copy() {
			return new Builder(this);
		}

		@Override
		public HashKey hashKey() {
			return HashKey.of(super.hashKey(), useMultilineStrings, useQuotelessStrings, useQuotelessKeys, omitRootBraces, useNewlineSeparators);
		}
	}

	/** Default serializer (readable). */
	public static final HjsonSerializer DEFAULT = new HjsonSerializer(create().ws());

	/** Compact serializer (single line). */
	public static final HjsonSerializer DEFAULT_COMPACT = new HjsonSerializer(create().useWhitespace(false).compact());

	/**
	 * Creates a new builder.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder.
	 */
	public HjsonSerializer(Builder builder) {
		super(builder);
		useMultilineStrings = builder.useMultilineStrings;
		useQuotelessStrings = builder.useQuotelessStrings;
		useQuotelessKeys = builder.useQuotelessKeys;
		omitRootBraces = builder.omitRootBraces;
		useNewlineSeparators = builder.useNewlineSeparators;
	}

	@Override
	public HjsonSerializerSession.Builder createSession() {
		return HjsonSerializerSession.create(this);
	}

	@Override
	public Builder copy() {
		return new Builder(this);
	}

	@Override
	public HjsonBeanPropertyMeta getHjsonBeanPropertyMeta(BeanPropertyMeta bpm) {
		if (bpm == null)
			return HjsonBeanPropertyMeta.DEFAULT;
		return hjsonBeanPropertyMetas.computeIfAbsent(bpm, k -> new HjsonBeanPropertyMeta(k, this));
	}

	@Override
	public HjsonClassMeta getHjsonClassMeta(ClassMeta<?> cm) {
		return hjsonClassMetas.computeIfAbsent(cm, k -> new HjsonClassMeta(k, this));
	}

	@Override
	protected FluentMap<String, Object> properties() {
		return super.properties()
			.a("useMultilineStrings", useMultilineStrings)
			.a("useQuotelessStrings", useQuotelessStrings)
			.a("useQuotelessKeys", useQuotelessKeys)
			.a("omitRootBraces", omitRootBraces)
			.a("useNewlineSeparators", useNewlineSeparators);
	}
}
