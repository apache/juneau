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
package org.apache.juneau.marshall.cbor;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.SystemUtils.*;

import java.io.*;
import java.math.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.stream.*;

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
 * 	<li>CBOR map (major type 5) → bean or {@link Map}
 * 	<li>CBOR array (major type 4) → {@link Collection} or Java array
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
 * <h5 class='section'>Round-trip notes:</h5>
 * <ul class='spaced-list'>
 * 	<li>Integers (major types 0/1) surface by target type: a {@link BigInteger} field carries the full
 * 		unsigned-64-bit magnitude; any other numeric type keeps the raw 64-bit bits as a {@code long}.
 * 	<li>{@link BigInteger} / {@link BigDecimal} round-trip losslessly (big integers decode from native
 * 		CBOR integers or, beyond {@code ±2^64}, from their decimal string; {@link BigDecimal} decodes
 * 		from its decimal string).
 * 	<li>Indefinite-length CBOR (RFC 8949 §3.2) is supported: arrays, maps, and chunked text/byte
 * 		strings all decode.
 * 	<li>Semantic tags are read and discarded on the databind path; tag number does not affect parsed
 * 		type.  Enable {@link Builder#nativeMode() nativeMode} to surface tags/simple values on the
 * 		{@link TokenReader} cursor.
 * </ul>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Cbor">CBOR Basics</a>
 * 	<li class='link'><a class="doclink" href="https://www.rfc-editor.org/rfc/rfc8949.html">RFC 8949</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S110", // Inheritance depth acceptable for this class hierarchy
	"java:S115", // Constants use UPPER_snakeCase naming convention
	"resource"   // Closeable resources are owned by the caller's parser session; Eclipse JDT @Owning warning is by design.
})
public class CborParser extends InputStreamParser implements CborMetaProvider, TokenReadable, ArrayRecordReadable {

	// Argument name constants for assertArgNotNull
	private static final String ARG_copyFrom = "copyFrom";

	// Property name constants
	private static final String PROP_nativeMode = "nativeMode";

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
	public static class Builder extends InputStreamParser.Builder<Builder> {

		private static final Cache<HashKey,CborParser> CACHE = Cache.of(HashKey.class, CborParser.class).build();

		private boolean nativeMode;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			consumes("application/cbor");
			nativeMode = env("CborParser.nativeMode", false);
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 */
		protected Builder(Builder copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			nativeMode = copyFrom.nativeMode;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The parser to copy from.
		 */
		protected Builder(CborParser copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			nativeMode = copyFrom.nativeMode;
		}

		/**
		 * Surfaces CBOR semantic tags and simple values as token-level metadata on
		 * {@link TokenReader} cursors returned by {@link CborParserSession#parseTokens(Object)}.
		 *
		 * <p>
		 * When enabled, tags accumulate on the cursor's tag stack (visible via
		 * {@link TokenReader#getTagCount()} / {@link TokenReader#getTag(int)}) and simple values
		 * surface their int via {@link TokenReader#getSimpleValue()} on
		 * {@link TokenType#VALUE_NULL} tokens.  When disabled (default), tags are silently
		 * unwrapped and simple values collapse to {@link TokenType#VALUE_NULL} with no metadata.
		 *
		 * <p>
		 * This is a token-cursor-level setting and does not affect the high-level POJO databind
		 * path ({@link CborParser#parse(Object, Class)}); that path always discards tags.
		 *
		 * @return This object.
		 */
		public Builder nativeMode() {
			return nativeMode(true);
		}

		/**
		 * Same as {@link #nativeMode()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public Builder nativeMode(boolean value) {
			nativeMode = value;
			return this;
		}

		@Override /* Overridden from Context.Builder<?> */
		public CborParser build() {
			return cache(CACHE).build(CborParser.class);
		}

		@Override /* Overridden from Context.Builder<?> */
		public Builder copy() {
			return new Builder(this);
		}

		@Override /* Overridden from Context.Builder<?> */
		public HashKey hashKey() {
			return HashKey.of(super.hashKey(), nativeMode);
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
	/** Default parser with binary-native opt-in mode enabled (CBOR tags / simple values surface as token-level metadata). */
	public static final CborParser DEFAULT_NATIVE = new CborParser(create().nativeMode());
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

	protected final boolean nativeMode;

	private final Map<ClassMeta<?>,CborClassMeta> cborClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,CborBeanPropertyMeta> cborBeanPropertyMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public CborParser(Builder builder) {
		super(builder);
		this.nativeMode = builder.nativeMode;
	}

	/**
	 * Returns <jk>true</jk> if token-cursor native mode is enabled.
	 *
	 * @return <jk>true</jk> if native mode is enabled, else <jk>false</jk>.
	 */
	public boolean isNativeMode() {
		return nativeMode;
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

	/**
	 * Convenience delegator that opens a {@link CborTokenReader} over the input using
	 * <b>default session arguments</b> (mirrors {@link #parse(Object, Class)}).
	 *
	 * <p>
	 * The real implementation lives on {@link CborParserSession#parseTokens(Object)}.  Callers
	 * that need request-derived configuration (locale, timezone, schema, swaps) should call
	 * {@link #createSession()} and invoke {@link CborParserSession#parseTokens(Object)} on the
	 * built session instead.
	 *
	 * @param input The input.
	 * @return A new {@link CborTokenReader}.
	 * @throws IOException If a problem occurred opening the underlying input.
	 */
	@Override /* TokenReadable */
	public TokenReader parseTokens(Object input) throws IOException {
		return getSession().parseTokens(input);
	}

	/**
	 * Convenience delegator for the streaming array-element {@link RecordReader} (uses default
	 * session args; see {@link #parseTokens(Object)}).  Real impl on
	 * {@link CborParserSession#parseArrayRecords(Object)}.
	 *
	 * @param input The input.
	 * @return A new element-streamed {@link RecordReader}.
	 * @throws IOException If a problem occurred opening the underlying input.
	 */
	@Override /* ArrayRecordReadable */
	public RecordReader parseArrayRecords(Object input) throws IOException {
		return getSession().parseArrayRecords(input);
	}

	@Override /* Overridden from InputStreamParser */
	protected FluentMap<String,Object> properties() {
		return super.properties()
			.a(PROP_nativeMode, nativeMode);
	}
}
