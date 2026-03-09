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
package org.apache.juneau.hocon;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.serializer.*;

/**
 * Serializes POJO models to HOCON (Human-Optimized Config Object Notation).
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * Accepts <c>Accept</c> types:  <bc>application/hocon</bc>
 * <p>
 * Produces <c>Content-Type</c> types:  <bc>application/hocon</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * HOCON is a superset of JSON used extensively in the JVM ecosystem for configuration files.
 * This serializer produces output that is valid HOCON and can be parsed back by {@link HoconParser}.
 *
 * <p>
 * The conversion is as follows:
 * <ul class='spaced-list'>
 * 	<li>
 * 		{@link java.util.HashMap HashMap} / bean → HOCON object with unquoted keys and <c>=</c> separator (<c>{ key = value }</c>).
 * 	<li>
 * 		{@link java.util.Collection Collection} / array → HOCON array (<c>[a, b, c]</c>).
 * 	<li>
 * 		{@link String String} (simple, no special chars) → unquoted string (<c>name = myapp</c>).
 * 	<li>
 * 		{@link String String} (contains special chars or is ambiguous like <c>"true"</c>, <c>"42"</c>, <c>"null"</c>) → quoted string (<c>path = "/usr/local/bin"</c>).
 * 	<li>
 * 		{@link String String} (multi-line) → triple-quoted string (<c>desc = """..."""</c>).
 * 	<li>
 * 		{@link Number Number} → number literal (<c>port = 8080</c>).
 * 	<li>
 * 		{@link Boolean Boolean} → <c>true</c> or <c>false</c>.
 * 	<li>
 * 		<c>null</c> → <c>null</c> keyword.
 * 	<li>
 * 		Non-primitive types (e.g. {@link java.util.Date Date}, {@link java.net.URI URI}) → converted via {@link org.apache.juneau.swap.ObjectSwap} before serialization.
 * </ul>
 *
 * <h5 class='topic'>Behavior-specific subclasses</h5>
 * <p>
 * <ul class='spaced-list'>
 * 	<li>
 * 		{@link #DEFAULT} — readable, unquoted keys/values, <c>=</c> separator, root braces omitted (config-file style).
 * 	<li>
 * 		{@link #DEFAULT_BRACES} — same as {@link #DEFAULT} but includes root <c>{ }</c> braces.
 * 	<li>
 * 		{@link #DEFAULT_COMPACT} — single-line compact output, JSON-compatible.
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Serialize a bean</jc>
 * 	String <jv>hocon</jv> = HoconSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>myBean</jv>);
 *
 * 	<jc>// Create a custom serializer instance</jc>
 * 	HoconSerializer <jv>s</jv> = HoconSerializer.<jsm>create</jsm>().omitRootBraces(<jk>false</jk>).build();
 * 	String <jv>hocon2</jv> = <jv>s</jv>.serialize(<jv>myBean</jv>);
 * </p>
 *
 * <h5 class='figure'>Example output (bean, root braces omitted):</h5>
 * <p class='bjson'>
 * name = myapp
 * port = 8080
 * debug = true
 * database {
 *   host = localhost
 *   port = 5432
 * }
 * tags = [web, api, rest]
 * </p>
 *
 * <h5 class='figure'>Example output (with root braces — DEFAULT_BRACES):</h5>
 * <p class='bjson'>
 * {
 *   name = myapp
 *   port = 8080
 *   debug = true
 * }
 * </p>
 *
 * <h5 class='section'>Limitations:</h5><ul>
 * 	<li class='note'>Substitutions (<c>${var}</c>) are never emitted — the serializer always writes resolved (concrete) values.
 * 	<li class='note'>Value concatenation is never emitted — the serializer always writes complete strings.
 * 	<li class='note'>Path expressions (<c>a.b.c = value</c>) are never emitted — the serializer always uses nested-brace form.
 * 	<li class='note'><c>include</c> directives are never emitted — these require file-system access outside the scope of a serializer.
 * </ul>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/HoconBasics">hocon-basics</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S110", "java:S115"
})
public class HoconSerializer extends WriterSerializer implements HoconMetaProvider {

	private static final String ARG_copyFrom = "copyFrom";

	private final java.util.concurrent.ConcurrentHashMap<ClassMeta<?>, HoconClassMeta> hoconClassMetas = new java.util.concurrent.ConcurrentHashMap<>();
	private final java.util.concurrent.ConcurrentHashMap<BeanPropertyMeta, HoconBeanPropertyMeta> hoconBeanPropertyMetas = new java.util.concurrent.ConcurrentHashMap<>();

	/** Use = (not :) for key-value separator. */
	protected final boolean useEqualsSign;

	/** Use unquoted strings for simple values. */
	protected final boolean useUnquotedStrings;

	/** Use unquoted keys when safe. */
	protected final boolean useUnquotedKeys;

	/** Omit root object braces. */
	protected final boolean omitRootBraces;

	/** Use triple-quoted """ for strings with newlines. */
	protected final boolean useMultilineStrings;

	/** Use newlines instead of commas between members. */
	protected final boolean useNewlineSeparators;

	/**
	 * Builder class.
	 */
	public static class Builder extends WriterSerializer.Builder {

		private static final Cache<HashKey, HoconSerializer> CACHE = Cache.of(HashKey.class, HoconSerializer.class).build();

		private boolean useEqualsSign = true;
		private boolean useUnquotedStrings = true;
		private boolean useUnquotedKeys = true;
		private boolean omitRootBraces = true;
		private boolean useMultilineStrings = true;
		private boolean useNewlineSeparators = true;

		protected Builder() {
			produces("application/hocon");
			accept("application/hocon");
		}

		protected Builder(Builder copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			useEqualsSign = copyFrom.useEqualsSign;
			useUnquotedStrings = copyFrom.useUnquotedStrings;
			useUnquotedKeys = copyFrom.useUnquotedKeys;
			omitRootBraces = copyFrom.omitRootBraces;
			useMultilineStrings = copyFrom.useMultilineStrings;
			useNewlineSeparators = copyFrom.useNewlineSeparators;
		}

		protected Builder(HoconSerializer copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			useEqualsSign = copyFrom.useEqualsSign;
			useUnquotedStrings = copyFrom.useUnquotedStrings;
			useUnquotedKeys = copyFrom.useUnquotedKeys;
			omitRootBraces = copyFrom.omitRootBraces;
			useMultilineStrings = copyFrom.useMultilineStrings;
			useNewlineSeparators = copyFrom.useNewlineSeparators;
		}

		/**
		 * Uses = for key-value separator (HOCON convention).
		 *
		 * @param value <jk>true</jk> for <c>=</c>, <jk>false</jk> for <c>:</c>.
		 * @return This object.
		 */
		public Builder useEqualsSign(boolean value) {
			useEqualsSign = value;
			return this;
		}

		/**
		 * Enables unquoted string output when values are unambiguous.
		 *
		 * @param value <jk>true</jk> to emit unquoted strings when safe.
		 * @return This object.
		 */
		public Builder useUnquotedStrings(boolean value) {
			useUnquotedStrings = value;
			return this;
		}

		/**
		 * Enables unquoted object keys when safe.
		 *
		 * @param value <jk>true</jk> to emit unquoted keys when safe.
		 * @return This object.
		 */
		public Builder useUnquotedKeys(boolean value) {
			useUnquotedKeys = value;
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
		 * Enables triple-quoted strings for values containing newlines.
		 *
		 * @param value <jk>true</jk> to use <c>"""</c> blocks for newline-containing strings.
		 * @return This object.
		 */
		public Builder useMultilineStrings(boolean value) {
			useMultilineStrings = value;
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
		public HoconSerializer build() {
			return cache(CACHE).build(HoconSerializer.class);
		}

		@Override
		public Builder copy() {
			return new Builder(this);
		}

		@Override
		public HashKey hashKey() {
			return HashKey.of(super.hashKey(), useEqualsSign, useUnquotedStrings, useUnquotedKeys,
				omitRootBraces, useMultilineStrings, useNewlineSeparators);
		}
	}

	/** Default serializer (readable, root braces omitted). */
	public static final HoconSerializer DEFAULT = new HoconSerializer(create().ws());

	/** Default serializer with root braces. */
	public static final HoconSerializer DEFAULT_BRACES = new HoconSerializer(create().ws().omitRootBraces(false));

	/** Compact serializer (single line). */
	public static final HoconSerializer DEFAULT_COMPACT = new HoconSerializer(create().useWhitespace(false).compact());

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
	public HoconSerializer(Builder builder) {
		super(builder);
		useEqualsSign = builder.useEqualsSign;
		useUnquotedStrings = builder.useUnquotedStrings;
		useUnquotedKeys = builder.useUnquotedKeys;
		omitRootBraces = builder.omitRootBraces;
		useMultilineStrings = builder.useMultilineStrings;
		useNewlineSeparators = builder.useNewlineSeparators;
	}

	@Override
	public HoconSerializerSession.Builder createSession() {
		return HoconSerializerSession.create(this);
	}

	@Override
	public Builder copy() {
		return new Builder(this);
	}

	@Override
	public HoconBeanPropertyMeta getHoconBeanPropertyMeta(BeanPropertyMeta bpm) {
		if (bpm == null)
			return HoconBeanPropertyMeta.DEFAULT;
		return hoconBeanPropertyMetas.computeIfAbsent(bpm, k -> new HoconBeanPropertyMeta(k, this));
	}

	@Override
	public HoconClassMeta getHoconClassMeta(ClassMeta<?> cm) {
		return hoconClassMetas.computeIfAbsent(cm, k -> new HoconClassMeta(k, this));
	}

	@Override
	protected FluentMap<String, Object> properties() {
		return super.properties()
			.a("useEqualsSign", useEqualsSign)
			.a("useUnquotedStrings", useUnquotedStrings)
			.a("useUnquotedKeys", useUnquotedKeys)
			.a("omitRootBraces", omitRootBraces)
			.a("useMultilineStrings", useMultilineStrings)
			.a("useNewlineSeparators", useNewlineSeparators);
	}
}
