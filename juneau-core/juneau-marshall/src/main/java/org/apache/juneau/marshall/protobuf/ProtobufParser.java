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
package org.apache.juneau.marshall.protobuf;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.SystemUtils.*;

import java.math.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.parser.*;

/**
 * Parses the Protocol Buffers <b>binary</b> wire format into POJO models.
 *
 * <h5 class='topic'>Media types</h5>
 *
 * Handles <c>Content-Type</c> types:  <bc>application/protobuf</bc>, <bc>application/x-protobuf</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * Parsing requires the target type (<c>parse(bytes, MyBean.<jk>class</jk>)</c>) since the protobuf binary wire is
 * not self-describing.  The target type's field-number/scalar-type table (cached on {@link ProtobufClassMeta}) drives
 * decoding.  Unknown field numbers are skipped correctly by wire type (parsing never breaks) but are not preserved.
 *
 * <h5 class='section'>Limitations / round-trip guarantees:</h5>
 * <ul class='spaced-list'>
 * 	<li>Lossless for nullable Juneau&hArr;Juneau round-trips.
 * 	<li>proto3-inherent:  a primitive set to its zero value is wire-indistinguishable from unset.
 * 	<li>Unknown fields are skipped (by wire type) but not preserved.
 * 	<li>Both packed and unpacked repeated scalars are accepted on parse (interop tolerance).
 * 	<li>{@code uint64} values are surfaced by Java type:  a {@code long} field carries the raw bits; a {@link BigInteger}
 * 		field carries the full unsigned magnitude.
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Parse protobuf binary bytes into a bean</jc>
 * 	MyBean <jv>bean</jv> = ProtobufParser.<jsf>DEFAULT</jsf>.parse(<jv>bytes</jv>, MyBean.<jk>class</jk>);
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Protobuf">Protobuf Binary Format Basics</a>
 * 	<li class='link'><a class="doclink" href="https://protobuf.dev/programming-guides/encoding/">Protocol Buffers Encoding</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S110", // Inheritance depth acceptable for this class hierarchy
	"java:S115" // Constants use UPPER_camelCase convention (e.g., ARG_copyFrom, PROP_nativeTypes)
})
public class ProtobufParser extends InputStreamParser implements ProtobufMetaProvider {

	// Argument name constants for assertArgNotNull
	private static final String ARG_copyFrom = "copyFrom";

	// Property name constants
	private static final String PROP_nativeTypes = "nativeTypes";

	/**
	 * Builder class.
	 */
	public static class Builder extends InputStreamParser.Builder<Builder> {

		private static final Cache<HashKey,ProtobufParser> CACHE = Cache.of(HashKey.class, ProtobufParser.class).build();

		private boolean nativeTypes;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			consumes("application/protobuf,application/x-protobuf");
			nativeTypes = env("ProtobufParser.nativeTypes", false);
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 */
		protected Builder(Builder copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			nativeTypes = copyFrom.nativeTypes;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The parser to copy from.
		 */
		protected Builder(ProtobufParser copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			nativeTypes = copyFrom.nativeTypes;
		}

		/**
		 * Opt in to native-type encodings.
		 *
		 * <p>
		 * Reserved opt-in axis for future native-type behaviors.  Currently a wired-but-documented placeholder.
		 *
		 * @return This object.
		 */
		public Builder nativeTypes() {
			return nativeTypes(true);
		}

		/**
		 * Same as {@link #nativeTypes()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public Builder nativeTypes(boolean value) {
			nativeTypes = value;
			return this;
		}

		@Override /* Overridden from Context.Builder<?> */
		public ProtobufParser build() {
			return cache(CACHE).build(ProtobufParser.class);
		}

		@Override /* Overridden from Context.Builder<?> */
		public Builder copy() {
			return new Builder(this);
		}

		@Override /* Overridden from Context.Builder<?> */
		public HashKey hashKey() {
			return HashKey.of(super.hashKey(), nativeTypes);
		}
	}

	/** Default parser, all default settings. */
	public static final ProtobufParser DEFAULT = new ProtobufParser(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	final boolean nativeTypes;

	private final Map<ClassMeta<?>,ProtobufClassMeta> protobufClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,ProtobufBeanPropertyMeta> protobufBeanPropertyMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public ProtobufParser(Builder builder) {
		super(builder);
		this.nativeTypes = builder.nativeTypes;
	}

	@Override /* Overridden from Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Overridden from Context */
	public ProtobufParserSession.Builder createSession() {
		return ProtobufParserSession.create(this);
	}

	@Override /* Overridden from ProtobufMetaProvider */
	public ProtobufBeanPropertyMeta getProtobufBeanPropertyMeta(BeanPropertyMeta bpm) {
		if (bpm == null)
			return ProtobufBeanPropertyMeta.DEFAULT;
		return protobufBeanPropertyMetas.computeIfAbsent(bpm, k -> new ProtobufBeanPropertyMeta(k.getDelegateFor(), this));
	}

	@Override /* Overridden from ProtobufMetaProvider */
	public ProtobufClassMeta getProtobufClassMeta(ClassMeta<?> cm) {
		return protobufClassMetas.computeIfAbsent(cm, k -> new ProtobufClassMeta(k, this));
	}

	@Override /* Overridden from Context */
	public ProtobufParserSession getSession() {
		return createSession().build();
	}

	@Override /* Overridden from InputStreamParser */
	protected FluentMap<String,Object> properties() {
		return super.properties()
			.a(PROP_nativeTypes, nativeTypes);
	}
}
