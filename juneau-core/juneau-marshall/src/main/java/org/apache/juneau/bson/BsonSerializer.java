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
import org.apache.juneau.serializer.*;

/**
 * Serializes POJO models to BSON (Binary JSON).
 *
 * <h5 class='section'>Media types:</h5>
 *
 * Handles <c>Accept</c> types:  <bc>application/bson</bc>
 * <p>
 * Produces <c>Content-Type</c> types: <bc>application/bson</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * Java-to-BSON type mapping:
 * <ul class='spaced-list'>
 * 	<li>Maps and beans → BSON document (type 0x03)
 * 	<li>Collections and arrays → BSON array (type 0x04, document with numeric string keys)
 * 	<li>{@link String} / {@link Enum} → BSON string (type 0x02)
 * 	<li>{@link Boolean} → BSON boolean (type 0x08)
 * 	<li>{@code int} / {@code short} / {@code byte} → BSON int32 (type 0x10)
 * 	<li>{@code long} → BSON int64 (type 0x12)
 * 	<li>{@code float} / {@code double} → BSON double (type 0x01)
 * 	<li>{@link java.util.Date} / {@link java.util.Calendar} / {@link java.time.Instant} → BSON datetime (type 0x09) when
 * 		{@code writeDatesAsDatetime=true}; otherwise ISO 8601 string
 * 	<li>{@link java.time.Duration} → ISO 8601 duration string
 * 	<li>{@code byte[]} → BSON binary (type 0x05, subtype 0x00)
 * 	<li>{@link java.math.BigDecimal} / {@link java.math.BigInteger} → BSON decimal128 (type 0x13)
 * 	<li>{@code null} → BSON null (type 0x0A)
 * 	<li>All other types → fallback to {@code toString()} as string
 * </ul>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>Limitations:</h5><ul>
 * 	<li>BSON requires a top-level document. Standalone scalars, collections, and arrays are wrapped as <c>{value: x}</c>.
 * 	<li>Map keys must be strings. Non-string keys use <c>toString()</c>. Null keys use a configurable placeholder.
 * 	<li>ObjectId (0x07) is not generated; parsing returns 24-character hex strings.
 * 	<li>Deprecated types (Undefined, DBPointer, Symbol, JS with Scope) are skipped during parsing.
 * 	<li>MongoDB-internal types (JavaScript, Timestamp, MinKey, MaxKey) are not supported.
 * 	<li>BSON is binary; <c>SpacedHex</c> and <c>Base64</c> subclasses provide text-encodable output.
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Serialize a POJO to BSON</jc>
 * 	byte[] <jv>bson</jv> = BsonSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>someObject</jv>);
 *
 * 	<jc>// Create a serializer that writes dates as ISO 8601 strings instead of BSON datetime</jc>
 * 	BsonSerializer <jv>s</jv> = BsonSerializer.<jsm>create</jsm>().writeDatesAsDatetime(<jk>false</jk>).build();
 *
 * 	<jc>// Or use the marshaller for convenience</jc>
 * 	<jv>bson</jv> = Bson.<jsm>of</jsm>(<jv>someObject</jv>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/BsonBasics">BSON Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S110", // Inheritance depth acceptable for this class hierarchy
	"java:S115", // Constants use UPPER_snakeCase naming convention
})
public class BsonSerializer extends OutputStreamSerializer implements BsonMetaProvider {

	// Argument name constants for assertArgNotNull
	private static final String ARG_builder = "builder";
	private static final String ARG_copyFrom = "copyFrom";

	// Property name constants
	private static final String PROP_addBeanTypesBson = "addBeanTypesBson";
	private static final String PROP_nullKeyString = "nullKeyString";
	private static final String PROP_writeDatesAsDatetime = "writeDatesAsDatetime";

	/** Default serializer, BASE64 string output. */
	public static class Base64 extends BsonSerializer {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		public Base64(Builder builder) {
			super(assertArgNotNull(ARG_builder, builder).binaryFormat(BinaryFormat.BASE64));
		}
	}

	/**
	 * Builder class.
	 */
	public static class Builder extends OutputStreamSerializer.Builder {

		private static final Cache<HashKey,BsonSerializer> CACHE = Cache.of(HashKey.class, BsonSerializer.class).build();

		private boolean addBeanTypesBson;
		private String nullKeyString = "<NULL>";
		private boolean writeDatesAsDatetime = true;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			produces("application/bson");
			accept("application/bson");
			addBeanTypesBson = env("BsonSerializer.addBeanTypesBson", false);
			nullKeyString = env("BsonSerializer.nullKeyString", "<NULL>");
			writeDatesAsDatetime = env("BsonSerializer.writeDatesAsDatetime", true);
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 */
		protected Builder(Builder copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			addBeanTypesBson = copyFrom.addBeanTypesBson;
			nullKeyString = copyFrom.nullKeyString;
			writeDatesAsDatetime = copyFrom.writeDatesAsDatetime;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 */
		protected Builder(BsonSerializer copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			addBeanTypesBson = copyFrom.addBeanTypesBson;
			nullKeyString = copyFrom.nullKeyString;
			writeDatesAsDatetime = copyFrom.writeDatesAsDatetime;
		}

		/**
		 * Add <js>"_type"</js> properties when needed.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public Builder addBeanTypesBson(boolean value) {
			addBeanTypesBson = value;
			return this;
		}

		/**
		 * The string used to represent <jk>null</jk> map keys in BSON documents.
		 *
		 * <p>
		 * BSON document field names cannot be <jk>null</jk>. When serializing a {@link Map} with a <jk>null</jk> key,
		 * this string is used as the field name. When parsing, a field with this name is stored with <jk>null</jk>
		 * as the key.
		 *
		 * @param value The placeholder string for null keys. Default is <js>"&lt;NULL&gt;"</js>.
		 * @return This object.
		 */
		public Builder nullKeyString(String value) {
			nullKeyString = value == null ? "<NULL>" : value;
			return this;
		}

		/**
		 * Use BSON datetime type for dates. When <jk>false</jk>, dates are serialized as ISO-8601 strings.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public Builder writeDatesAsDatetime(boolean value) {
			writeDatesAsDatetime = value;
			return this;
		}

		@Override /* OutputStreamSerializer.Builder */
		public Builder binaryFormat(BinaryFormat value) {
			super.binaryFormat(value);
			return this;
		}

		@Override /* Serializer.Builder */
		public Builder keepNullProperties() {
			super.keepNullProperties();
			return this;
		}

		@Override /* Serializer.Builder */
		public Builder keepNullProperties(boolean value) {
			super.keepNullProperties(value);
			return this;
		}

		@Override /* Serializer.Builder */
		public Builder addRootType() {
			super.addRootType();
			return this;
		}

		@Override /* Serializer.Builder */
		public Builder addRootType(boolean value) {
			super.addRootType(value);
			return this;
		}

		@Override /* Context.Builder */
		public BsonSerializer build() {
			return cache(CACHE).build(BsonSerializer.class);
		}

		@Override /* Context.Builder */
		public HashKey hashKey() {
			return HashKey.of(super.hashKey(), addBeanTypesBson, nullKeyString, writeDatesAsDatetime);
		}

		@Override /* Context.Builder */
		public Builder copy() {
			return new Builder(this);
		}
	}

	/** Default serializer, spaced-hex string output. */
	public static class SpacedHex extends BsonSerializer {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		public SpacedHex(Builder builder) {
			super(builder.binaryFormat(BinaryFormat.SPACED_HEX));
		}
	}

	/** Default serializer, all default settings. */
	public static final BsonSerializer DEFAULT = new BsonSerializer(create());
	/** Default serializer, spaced-hex string output. */
	public static final BsonSerializer DEFAULT_SPACED_HEX = new SpacedHex(create());
	/** Default serializer, BASE64 string output. */
	public static final BsonSerializer DEFAULT_BASE64 = new Base64(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	protected final boolean addBeanTypesBson;
	protected final String nullKeyString;
	protected final boolean writeDatesAsDatetime;

	private final Map<BeanPropertyMeta,BsonBeanPropertyMeta> bsonBeanPropertyMetas = new ConcurrentHashMap<>();
	private final Map<ClassMeta<?>,BsonClassMeta> bsonClassMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public BsonSerializer(Builder builder) {
		super(builder);
		addBeanTypesBson = builder.addBeanTypesBson;
		nullKeyString = builder.nullKeyString;
		writeDatesAsDatetime = builder.writeDatesAsDatetime;
	}

	@Override /* Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Context */
	public BsonSerializerSession.Builder createSession() {
		return BsonSerializerSession.create(this);
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
	public BsonSerializerSession getSession() {
		return createSession().build();
	}

	@Override
	protected final boolean isAddBeanTypes() {
		return addBeanTypesBson || super.isAddBeanTypes();
	}

	@Override /* OutputStreamSerializer */
	protected FluentMap<String,Object> properties() {
		return super.properties()
			.a(PROP_addBeanTypesBson, addBeanTypesBson)
			.a(PROP_nullKeyString, nullKeyString)
			.a(PROP_writeDatesAsDatetime, writeDatesAsDatetime);
	}
}
