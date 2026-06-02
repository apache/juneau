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
package org.apache.juneau.msgpack;

import org.apache.juneau.commons.http.MediaType;
import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.lang.annotation.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.commons.function.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.commons.bean.BeanPropertyMeta;

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
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/MessagePackBasics">MessagePack Basics</a>

 * </ul>
 */
@SuppressWarnings({
	"java:S110", // Inheritance depth acceptable for this class hierarchy
	"java:S115" // Constants use UPPER_snakeCase naming convention
})
public class MsgPackParser extends InputStreamParser implements MsgPackMetaProvider {

	// Argument name constants for assertArgNotNull
	private static final String ARG_copyFrom = "copyFrom";

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

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			consumes("application/msgpack,octal/msgpack");
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(Builder copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(MsgPackParser copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
		}

		@Override /* Overridden from Context.Builder */
		public MsgPackParser build() {
			return cache(CACHE).build(MsgPackParser.class);
		}

		@Override /* Overridden from Context.Builder */
		public Builder copy() {
			return new Builder(this);
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

	private final Map<ClassMeta<?>,MsgPackClassMeta> msgPackClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,MsgPackBeanPropertyMeta> msgPackBeanPropertyMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public MsgPackParser(Builder builder) {
		super(builder);
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
}