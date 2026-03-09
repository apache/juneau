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
package org.apache.juneau.cbor;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.serializer.*;

/**
 * Serializes POJO models to CBOR (RFC 8949).
 *
 * <h5 class='section'>Media types:</h5>
 *
 * Handles <c>Accept</c> types:  <bc>application/cbor</bc>
 * <p>
 * Produces <c>Content-Type</c> types: <bc>application/cbor</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * Java-to-CBOR type mapping:
 * <ul class='spaced-list'>
 * 	<li>Maps and beans → CBOR map (major type 5)
 * 	<li>Collections and arrays → CBOR array (major type 4)
 * 	<li>{@link String} / {@link Enum} → CBOR text string (major type 3, UTF-8)
 * 	<li>{@link Boolean} → CBOR simple values 20/21 (<c>0xF4</c>/<c>0xF5</c>)
 * 	<li>{@link Number} (int, long, float, double) → CBOR compact integers (major types 0/1) or IEEE 754 floats (major type 7)
 * 	<li>{@link java.util.Date} / {@link java.util.Calendar} / {@link java.time.Instant} → ISO 8601 string
 * 	<li>{@link java.time.Duration} → ISO 8601 duration string
 * 	<li>{@code byte[]} → CBOR byte string (major type 2)
 * 	<li>{@code null} → CBOR simple value 22 (<c>0xF6</c>)
 * 	<li>All other types → fallback to {@code toString()} as string
 * </ul>
 *
 * <h5 class='section'>Behavior-specific subclasses:</h5>
 * <ul class='spaced-list'>
 * 	<li>{@link SpacedHex} — Spaced-hex text output for debugging
 * 	<li>{@link Base64} — Base64 text output
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Serialize a bean to CBOR bytes using the default serializer</jc>
 * 	byte[] <jv>cbor</jv> = CborSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>myBean</jv>);
 *
 * 	<jc>// Custom serializer with bean type properties enabled</jc>
 * 	CborSerializer <jv>s</jv> = CborSerializer.<jsm>create</jsm>().addBeanTypes().build();
 * 	byte[] <jv>cbor</jv> = <jv>s</jv>.serialize(<jv>myBean</jv>);
 * </p>
 *
 * <h5 class='section'>Limitations:</h5>
 * <ul class='spaced-list'>
 * 	<li>{@link java.math.BigInteger} / {@link java.math.BigDecimal} — Cast to long/double; precision loss for values exceeding range. Use {@link org.apache.juneau.swap.ObjectSwap} to serialize as string if precision required.
 * 	<li>Indefinite-length encoding not supported in output.
 * 	<li>Half-precision float (0xF9) not produced; always uses float32 (0xFA) or float64 (0xFB).
 * 	<li>Semantic tags (major type 6) not emitted unless {@link Builder#useTags(boolean) useTags(true)}.
 * </ul>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/CborBasics">CBOR Basics</a>
 * 	<li class='link'><a class="doclink" href="https://www.rfc-editor.org/rfc/rfc8949.html">RFC 8949</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S110", // Inheritance depth acceptable for this class hierarchy
	"java:S115", // Constants use UPPER_snakeCase naming convention
})
public class CborSerializer extends OutputStreamSerializer implements CborMetaProvider {

	// Argument name constants for assertArgNotNull
	private static final String ARG_builder = "builder";
	private static final String ARG_copyFrom = "copyFrom";

	// Property name constants
	private static final String PROP_addBeanTypesCbor = "addBeanTypesCbor";
	private static final String PROP_useTags = "useTags";

	/** Default serializer, BASE64 string output. */
	public static class Base64 extends CborSerializer {

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

		private static final Cache<HashKey,CborSerializer> CACHE = Cache.of(HashKey.class, CborSerializer.class).build();

		private boolean addBeanTypesCbor;
		private boolean useTags;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			produces("application/cbor");
			addBeanTypesCbor = env("CborSerializer.addBeanTypesCbor", false);
			useTags = env("CborSerializer.useTags", false);
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 */
		protected Builder(Builder copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			addBeanTypesCbor = copyFrom.addBeanTypesCbor;
			useTags = copyFrom.useTags;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The serializer to copy from.
		 */
		protected Builder(CborSerializer copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			addBeanTypesCbor = copyFrom.addBeanTypesCbor;
			useTags = copyFrom.useTags;
		}

		/**
		 * Add <js>"_type"</js> properties when needed.
		 *
		 * @return This object.
		 */
		public Builder addBeanTypesCbor() {
			return addBeanTypesCbor(true);
		}

		/**
		 * Same as {@link #addBeanTypesCbor()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public Builder addBeanTypesCbor(boolean value) {
			addBeanTypesCbor = value;
			return this;
		}

		/**
		 * Use CBOR semantic tags for dates and other typed values.
		 *
		 * @return This object.
		 */
		public Builder useTags() {
			return useTags(true);
		}

		/**
		 * Same as {@link #useTags()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public Builder useTags(boolean value) {
			useTags = value;
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder binaryFormat(BinaryFormat value) {
			super.binaryFormat(value);
			return this;
		}

		@Override /* Overridden from Context.Builder */
		public Builder copy() {
			return new Builder(this);
		}

		@Override /* Overridden from Context.Builder */
		public CborSerializer build() {
			return cache(CACHE).build(CborSerializer.class);
		}

		@Override /* Overridden from Context.Builder */
		public HashKey hashKey() {
			return HashKey.of(super.hashKey(), addBeanTypesCbor, useTags);
		}
	}

	/** Default serializer, spaced-hex string output. */
	public static class SpacedHex extends CborSerializer {

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
	public static final CborSerializer DEFAULT = new CborSerializer(create());
	/** Default serializer, spaced-hex string output. */
	public static final CborSerializer DEFAULT_SPACED_HEX = new SpacedHex(create());
	/** Default serializer, BASE64 string output. */
	public static final CborSerializer DEFAULT_BASE64 = new Base64(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	protected final boolean addBeanTypesCbor;
	protected final boolean useTags;

	private final Map<BeanPropertyMeta,CborBeanPropertyMeta> cborBeanPropertyMetas = new ConcurrentHashMap<>();
	private final Map<ClassMeta<?>,CborClassMeta> cborClassMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public CborSerializer(Builder builder) {
		super(builder);
		this.addBeanTypesCbor = builder.addBeanTypesCbor;
		this.useTags = builder.useTags;
	}

	@Override /* Overridden from Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Overridden from Context */
	public CborSerializerSession.Builder createSession() {
		return CborSerializerSession.create(this);
	}

	@Override /* Overridden from CborMetaProvider */
	public CborBeanPropertyMeta getCborBeanPropertyMeta(BeanPropertyMeta bpm) {
		if (bpm == null)
			return CborBeanPropertyMeta.DEFAULT;
		return cborBeanPropertyMetas.computeIfAbsent(bpm, k -> new CborBeanPropertyMeta(k.getDelegateFor(), this));
	}

	@Override /* Overridden from CborMetaProvider */
	public CborClassMeta getCborClassMeta(ClassMeta<?> cm) {
		return cborClassMetas.computeIfAbsent(cm, k -> new CborClassMeta(k, this));
	}

	@Override /* Overridden from Context */
	public CborSerializerSession getSession() {
		return createSession().build();
	}

	@Override
	protected final boolean isAddBeanTypes() {
		return addBeanTypesCbor || super.isAddBeanTypes();
	}

	@Override /* Overridden from OutputStreamSerializer */
	protected FluentMap<String,Object> properties() {
		return super.properties()
			.a(PROP_addBeanTypesCbor, addBeanTypesCbor)
			.a(PROP_useTags, useTags);
	}
}
