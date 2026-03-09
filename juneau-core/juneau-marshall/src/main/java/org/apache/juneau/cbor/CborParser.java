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

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.parser.*;

/**
 * Parses CBOR streams (RFC 8949) into POJO models.
 *
 * <h5 class='topic'>Media types</h5>
 *
 * Handles <c>Content-Type</c> types:  <bc>application/cbor</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * CBOR-to-Java type mapping:
 * <ul class='spaced-list'>
 * 	<li>CBOR map (major type 5) → bean or {@link java.util.Map}
 * 	<li>CBOR array (major type 4) → {@link java.util.Collection} or Java array
 * 	<li>CBOR text string (major type 3) → {@link String}; coerced to types parseable from string
 * 	<li>CBOR unsigned/negative integer (major types 0/1) → {@link Integer} or {@link Long}
 * 	<li>CBOR float (major type 7, 0xFA/0xFB) → {@link Float} or {@link Double}
 * 	<li>CBOR half-precision float (major type 7, 0xF9) → {@link Float} (converted)
 * 	<li>CBOR boolean (major type 7, 0xF4/0xF5) → {@link Boolean}
 * 	<li>CBOR null (major type 7, 0xF6) → {@code null}
 * 	<li>CBOR byte string (major type 2) → {@code byte[]}
 * 	<li>CBOR semantic tag (major type 6) → tag ignored; tagged value parsed normally
 * </ul>
 *
 * <h5 class='section'>Limitations:</h5>
 * <ul class='spaced-list'>
 * 	<li>{@link java.math.BigInteger} / {@link java.math.BigDecimal} — Cast to long/double; precision loss.
 * 	<li>Indefinite-length CBOR (break code 0xFF) not supported.
 * 	<li>Semantic tags are read and discarded; tag number does not affect parsed type.
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
	"java:S115" // Constants use UPPER_snakeCase naming convention
})
public class CborParser extends InputStreamParser implements CborMetaProvider {

	// Argument name constants for assertArgNotNull
	private static final String ARG_copyFrom = "copyFrom";

	/** Default parser, string input encoded as BASE64. */
	public static class Base64 extends CborParser {

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

		private static final Cache<HashKey,CborParser> CACHE = Cache.of(HashKey.class, CborParser.class).build();

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			consumes("application/cbor");
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 */
		protected Builder(Builder copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The parser to copy from.
		 */
		protected Builder(CborParser copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
		}

		@Override /* Overridden from Context.Builder */
		public CborParser build() {
			return cache(CACHE).build(CborParser.class);
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
	}

	/** Default parser, string input encoded as spaced-hex. */
	public static class SpacedHex extends CborParser {

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
	public static final CborParser DEFAULT = new CborParser(create());
	/** Default parser, string input encoded as spaced-hex. */
	public static final CborParser DEFAULT_SPACED_HEX = new SpacedHex(create());
	/** Default parser, string input encoded as BASE64. */
	public static final CborParser DEFAULT_BASE64 = new Base64(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	private final Map<ClassMeta<?>,CborClassMeta> cborClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,CborBeanPropertyMeta> cborBeanPropertyMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public CborParser(Builder builder) {
		super(builder);
	}

	@Override /* Overridden from Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Overridden from Context */
	public CborParserSession.Builder createSession() {
		return CborParserSession.create(this);
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
	public CborParserSession getSession() {
		return createSession().build();
	}
}
