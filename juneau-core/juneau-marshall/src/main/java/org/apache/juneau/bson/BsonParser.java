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
package org.apache.juneau.bson;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.parser.*;

/**
 * Parses BSON (Binary JSON) into POJO models.
 *
 * <h5 class='section'>Media types:</h5>
 *
 * Handles <c>Content-Type</c> types:  <bc>application/bson</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * BSON-to-Java type mapping:
 * <ul class='spaced-list'>
 * 	<li>BSON document (0x03) → bean or {@link java.util.Map}
 * 	<li>BSON array (0x04) → {@link java.util.Collection} or Java array (numeric string keys ignored)
 * 	<li>BSON string (0x02) → {@link String}; coerced to types parseable from string
 * 	<li>BSON boolean (0x08) → {@link Boolean}
 * 	<li>BSON int32 (0x10) → {@code int}; coerced to {@code long}, {@code short}, {@code byte} as needed
 * 	<li>BSON int64 (0x12) → {@code long}; coerced to {@code int} if target is {@code int}
 * 	<li>BSON double (0x01) → {@code double}; coerced to {@code float}
 * 	<li>BSON datetime (0x09) → {@link java.util.Date}, {@link java.util.Calendar}, {@link java.time.Instant}, or
 * 		{@code long} (millis)
 * 	<li>BSON binary (0x05) → {@code byte[]}
 * 	<li>BSON decimal128 (0x13) → {@link java.math.BigDecimal}
 * 	<li>BSON null (0x0A) → {@code null}
 * 	<li>BSON ObjectId (0x07) → 24-character hex {@link String}
 * 	<li>Deprecated/unsupported types → silently skipped
 * </ul>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>Limitations:</h5><ul>
 * 	<li>BSON requires a top-level document. Standalone scalars are wrapped as <c>{value: x}</c>.
 * 	<li>Map keys must be strings. Non-string keys use <c>toString()</c>. Null keys use a configurable placeholder.
 * 	<li>ObjectId (0x07) is read-only; parsed as 24-character hex strings.
 * 	<li>Deprecated types (Undefined, DBPointer, Symbol, JS with Scope) are silently skipped.
 * 	<li>MongoDB-internal types (JavaScript, Timestamp, MinKey, MaxKey) are not supported.
 * 	<li>BSON is binary; <c>SpacedHex</c> and <c>Base64</c> subclasses accept text-encoded input.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/BsonBasics">BSON Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S110", // Inheritance depth acceptable for this class hierarchy
	"java:S115", // Constants use UPPER_snakeCase naming convention
})
public class BsonParser extends InputStreamParser implements BsonMetaProvider {

	// Argument name constants for assertArgNotNull
	private static final String ARG_copyFrom = "copyFrom";
	private static final String DEFAULT_NULL_KEY = "<NULL>";

	/** Default parser, string input encoded as BASE64. */
	public static class Base64 extends BsonParser {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		public Base64(Builder builder) {
			super(builder.binaryFormat(BinaryFormat.BASE64));
		}
	}

	/**
	 * Builder class.
	 */
	public static class Builder extends InputStreamParser.Builder {

		private static final Cache<HashKey,BsonParser> CACHE = Cache.of(HashKey.class, BsonParser.class).build();

		private String nullKeyString = DEFAULT_NULL_KEY;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			consumes("application/bson");
			nullKeyString = env("BsonParser.nullKeyString", DEFAULT_NULL_KEY);
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 */
		protected Builder(Builder copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			nullKeyString = copyFrom.nullKeyString;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 */
		protected Builder(BsonParser copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			nullKeyString = copyFrom.nullKeyString;
		}

		@Override /* InputStreamParser.Builder */
		public Builder binaryFormat(BinaryFormat value) {
			super.binaryFormat(value);
			return this;
		}

		/**
		 * The string used to represent <jk>null</jk> map keys in BSON documents.
		 *
		 * <p>
		 * BSON document field names cannot be <jk>null</jk>. When parsing a field with this name,
		 * it is stored with <jk>null</jk> as the key in the target map.
		 *
		 * @param value The placeholder string for null keys. Default is <js>"&lt;NULL&gt;"</js>.
		 * @return This object.
		 */
		public Builder nullKeyString(String value) {
			nullKeyString = value == null ? DEFAULT_NULL_KEY : value;
			return this;
		}

		@Override /* Parser.Builder */
		public Builder trimStrings() {
			super.trimStrings();
			return this;
		}

		@Override /* Parser.Builder */
		public Builder trimStrings(boolean value) {
			super.trimStrings(value);
			return this;
		}

		@Override /* Context.Builder */
		public BsonParser build() {
			return cache(CACHE).build(BsonParser.class);
		}

		@Override /* Context.Builder */
		public HashKey hashKey() {
			return HashKey.of(super.hashKey(), nullKeyString);
		}

		@Override /* Context.Builder */
		public Builder copy() {
			return new Builder(this);
		}
	}

	/** Default parser, string input encoded as spaced-hex. */
	public static class SpacedHex extends BsonParser {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		public SpacedHex(Builder builder) {
			super(builder.binaryFormat(BinaryFormat.SPACED_HEX));
		}
	}

	/** Default parser, all default settings. */
	public static final BsonParser DEFAULT = new BsonParser(create());
	/** Default parser, spaced-hex string input. */
	public static final BsonParser DEFAULT_SPACED_HEX = new SpacedHex(create());
	/** Default parser, BASE64 string input. */
	public static final BsonParser DEFAULT_BASE64 = new Base64(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	private final Map<ClassMeta<?>,BsonClassMeta> bsonClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,BsonBeanPropertyMeta> bsonBeanPropertyMetas = new ConcurrentHashMap<>();
	private final String nullKeyString;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public BsonParser(Builder builder) {
		super(builder);
		nullKeyString = builder.nullKeyString;
	}

	@Override /* Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Context */
	public BsonParserSession.Builder createSession() {
		return BsonParserSession.create(this);
	}

	@Override /* BsonMetaProvider */
	public BsonBeanPropertyMeta getBsonBeanPropertyMeta(BeanPropertyMeta bpm) {
		if (bpm == null)
			return BsonBeanPropertyMeta.DEFAULT;
		return bsonBeanPropertyMetas.computeIfAbsent(bpm, k -> new BsonBeanPropertyMeta(k.getDelegateFor(), this));
	}

	@Override /* BsonMetaProvider */
	public BsonClassMeta getBsonClassMeta(ClassMeta<?> cm) {
		return bsonClassMetas.computeIfAbsent(cm, k -> new BsonClassMeta(k, this));
	}

	@Override /* Context */
	public BsonParserSession getSession() {
		return createSession().build();
	}

	/**
	 * Returns the string used to represent <jk>null</jk> map keys in BSON documents.
	 *
	 * @return The null key placeholder string.
	 */
	public String getNullKeyString() {
		return nullKeyString;
	}
}
