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
package org.apache.juneau.marshall.msgpack;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.SystemUtils.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.stream.*;

/**
 * Parses a MessagePack stream into a POJO model.
 *
 * <h5 class='topic'>Media types</h5>
 *
 * Handles <c>Content-Type</c> types:  <bc>application/msgpack, octal/msgpack</bc>
 *
 * <p>
 * The legacy media type <bc>octal/msgpack</bc> is retained as a backward-compatibility alias so
 * that existing wire values still decode. New consumers should prefer the RFC-standard
 * <bc>application/msgpack</bc> value.
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * MessagePack-to-Java type mapping:
 * <ul class='spaced-list'>
 * 	<li>MessagePack map → bean or {@link Map}
 * 	<li>MessagePack array → {@link Collection} or Java array
 * 	<li>MessagePack string → {@link String}; coerced to types parseable from string
 * 	<li>MessagePack boolean → {@link Boolean}
 * 	<li>MessagePack int → {@link Integer}; coerced to {@link Long}, {@link Short}, {@link Byte} as needed
 * 	<li>MessagePack float → {@link Float} or {@link Double}
 * 	<li>MessagePack binary → {@code byte[]}
 * 	<li>MessagePack nil → {@code null}
 * </ul>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/MessagePackSupport">MessagePack Basics</a>

 * </ul>
 */
@SuppressWarnings({
	"java:S110", // Inheritance depth acceptable for this class hierarchy
	"java:S115", // Constants use UPPER_snakeCase naming convention
	"resource"   // Closeable resources are owned by the caller's parser session; Eclipse JDT @Owning warning is by design.
})
public class MsgPackParser extends InputStreamParser implements MsgPackMetaProvider, TokenReadable, ArrayRecordReadable {

	// Argument name constants for assertArgNotNull
	private static final String ARG_copyFrom = "copyFrom";

	// Property name constants
	private static final String PROP_nativeMode = "nativeMode";

	/** Default parser, string input encoded as BASE64. */
	public static class Base64 extends MsgPackParser {

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

		private static final Cache<HashKey,MsgPackParser> CACHE = Cache.of(HashKey.class, MsgPackParser.class).build();

		private boolean nativeMode;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			consumes("application/msgpack,octal/msgpack");
			nativeMode = env("MsgPackParser.nativeMode", false);
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(Builder copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			nativeMode = copyFrom.nativeMode;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(MsgPackParser copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			nativeMode = copyFrom.nativeMode;
		}

		/**
		 * Surfaces the MsgPack {@code ext} type byte as token-level metadata on
		 * {@link TokenReader} cursors returned by {@link MsgPackParserSession#parseTokens(Object)}.
		 *
		 * <p>
		 * When enabled, ext tokens carry the signed type byte (visible via
		 * {@link TokenReader#getExtType()}) on {@link TokenType#VALUE_BINARY} tokens.  When
		 * disabled (default), the payload still surfaces as {@link TokenType#VALUE_BINARY} but
		 * the type byte is dropped.
		 *
		 * <p>
		 * This is a token-cursor-level setting and does not affect the high-level POJO databind
		 * path ({@link MsgPackParser#parse(Object, Class)}); that path always discards the type
		 * byte.
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
		public MsgPackParser build() {
			return cache(CACHE).build(MsgPackParser.class);
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
	public static class SpacedHex extends MsgPackParser {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		public SpacedHex(Builder builder) {
			super(builder.binaryFormat(BinaryFormat.SPACED_HEX));
		}
	}

	/** Default parser, all default settings.*/
	public static final MsgPackParser DEFAULT = new MsgPackParser(create());
	/** Default parser with binary-native opt-in mode enabled (MsgPack {@code ext} type byte surfaces as token-level metadata). */
	public static final MsgPackParser DEFAULT_NATIVE = new MsgPackParser(create().nativeMode());
	/** Default parser, all default settings, string input encoded as spaced-hex.*/
	public static final MsgPackParser DEFAULT_SPACED_HEX = new SpacedHex(create());

	/** Default parser, all default settings, string input encoded as BASE64.*/
	public static final MsgPackParser DEFAULT_BASE64 = new Base64(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	protected final boolean nativeMode;

	private final Map<ClassMeta<?>,MsgPackClassMeta> msgPackClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,MsgPackBeanPropertyMeta> msgPackBeanPropertyMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public MsgPackParser(Builder builder) {
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
	public MsgPackParserSession.Builder createSession() {
		return MsgPackParserSession.create(this);
	}

	@Override /* Overridden from MsgPackMetaProvider */
	public MsgPackBeanPropertyMeta getMsgPackBeanPropertyMeta(BeanPropertyMeta bpm) {
		if (bpm == null)
			return MsgPackBeanPropertyMeta.DEFAULT;
		return msgPackBeanPropertyMetas.computeIfAbsent(bpm, k -> new MsgPackBeanPropertyMeta(k.getDelegateFor(), this));
	}

	@Override /* Overridden from MsgPackMetaProvider */
	public MsgPackClassMeta getMsgPackClassMeta(ClassMeta<?> cm) {
		return msgPackClassMetas.computeIfAbsent(cm, k -> new MsgPackClassMeta(k, this));
	}

	@Override /* Overridden from Context */
	public MsgPackParserSession getSession() { return createSession().build(); }

	/**
	 * Convenience delegator that opens a {@link MsgPackTokenReader} over the input using
	 * <b>default session arguments</b> (mirrors {@link #parse(Object, Class)}).
	 *
	 * <p>
	 * The real implementation lives on {@link MsgPackParserSession#parseTokens(Object)}.  Callers
	 * that need request-derived configuration (locale, timezone, schema, swaps) should call
	 * {@link #createSession()} and invoke {@link MsgPackParserSession#parseTokens(Object)} on the
	 * built session instead.
	 *
	 * @param input The input.
	 * @return A new {@link MsgPackTokenReader}.
	 * @throws IOException If a problem occurred opening the underlying input.
	 */
	@Override /* TokenReadable */
	public TokenReader parseTokens(Object input) throws IOException {
		return getSession().parseTokens(input);
	}

	/**
	 * Convenience delegator for the array-element {@link RecordReader} (uses default session args;
	 * see {@link #parseTokens(Object)}).  Real impl on
	 * {@link MsgPackParserSession#parseArrayRecords(Object)}.
	 *
	 * @param input The input.
	 * @return A new array-element {@link RecordReader}.
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