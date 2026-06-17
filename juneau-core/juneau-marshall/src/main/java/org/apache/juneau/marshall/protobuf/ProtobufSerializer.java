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
import static org.apache.juneau.commons.utils.Utils.*;

import java.math.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.serializer.*;

/**
 * Serializes POJO models to the Protocol Buffers <b>binary</b> wire format.
 *
 * <h5 class='section'>Media types:</h5>
 *
 * Handles <c>Accept</c> types:  <bc>application/protobuf</bc>
 * <p>
 * Produces <c>Content-Type</c> types: <bc>application/protobuf</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * Unlike the text-format {@link org.apache.juneau.marshall.proto.ProtoSerializer} (<c>text/protobuf</c>), this
 * serializer emits the compact, non-self-describing protobuf binary wire format.  Because the wire carries neither
 * field names nor specific scalar types, a per-bean field-number/scalar-type table (cached on
 * {@link ProtobufClassMeta}) is consulted during serialization.  The codec is lossless for Juneau&hArr;Juneau
 * round-trips, and upgrades field-by-field to external <c>protoc</c> interop via {@link Protobuf @Protobuf}.
 *
 * <h5 class='section'>Supported types / scalar mapping:</h5>
 * <ul class='spaced-list'>
 * 	<li>{@code boolean}/{@code Boolean} &rarr; bool (varint)
 * 	<li>{@code byte}/{@code short}/{@code int}/{@code Integer} &rarr; int32 (varint)
 * 	<li>{@code long}/{@code Long} &rarr; int64 (varint)
 * 	<li>{@code float}/{@code Float} &rarr; float (I32); {@code double}/{@code Double} &rarr; double (I64)
 * 	<li>{@link String} &rarr; string; {@code byte[]} &rarr; bytes (length-delimited)
 * 	<li>{@link Enum} &rarr; int32 ordinal (varint) by default; {@code name()} via {@link Protobuf#type()}={@link ProtobufScalarType#ENUM_STRING}
 * 	<li>{@link BigInteger}/{@link BigDecimal}/{@code char} and date/time types &rarr; lossless string forms
 * 	<li>Nested bean &rarr; embedded message; {@link Map} &rarr; repeated <c>entry{key=1,value=2}</c>
 * 	<li>Repeated scalars &rarr; packed; repeated string/bytes/message &rarr; tagged entries
 * </ul>
 *
 * <h5 class='section'>Field-number assignment:</h5>
 * <ul class='spaced-list'>
 * 	<li>Explicit {@link Protobuf#fieldNumber()} overrides are reserved first.
 * 	<li>Remaining properties are auto-numbered from 1 by alphabetical property name, skipping the reserved 19000&ndash;19999 band.
 * </ul>
 *
 * <h5 class='section'>Limitations / round-trip guarantees:</h5>
 * <ul class='spaced-list'>
 * 	<li>Lossless for nullable Juneau&hArr;Juneau round-trips.
 * 	<li>proto3-inherent:  a primitive set to its zero value is wire-indistinguishable from unset.
 * 	<li>Unknown fields are skipped (by wire type) but not preserved.
 * 	<li><c>oneof</c>, full proto2 presence, unknown-field preservation, and well-known types are not supported.
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Serialize a bean to protobuf binary bytes using the default serializer</jc>
 * 	<jk>byte</jk>[] <jv>bytes</jv> = ProtobufSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>myBean</jv>);
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ProtobufBinaryBasics">Protobuf Binary Format Basics</a>
 * 	<li class='link'><a class="doclink" href="https://protobuf.dev/programming-guides/encoding/">Protocol Buffers Encoding</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S110" // Inheritance depth acceptable for this class hierarchy
})
public class ProtobufSerializer extends OutputStreamSerializer implements ProtobufMetaProvider {

	// Argument name constants for assertArgNotNull
	private static final String ARG_copyFrom = "copyFrom";

	// Property name constants
	private static final String PROP_addBeanTypesProtobuf = "addBeanTypesProtobuf";
	private static final String PROP_nativeTypes = "nativeTypes";

	/**
	 * Builder class.
	 */
	public static class Builder extends OutputStreamSerializer.Builder<Builder> {

		private static final Cache<HashKey,ProtobufSerializer> CACHE = Cache.of(HashKey.class, ProtobufSerializer.class).build();

		private boolean addBeanTypesProtobuf;
		private boolean nativeTypes;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			produces("application/protobuf");
			addBeanTypesProtobuf = env("ProtobufSerializer.addBeanTypesProtobuf", false);
			nativeTypes = env("ProtobufSerializer.nativeTypes", false);
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 */
		protected Builder(Builder copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			addBeanTypesProtobuf = copyFrom.addBeanTypesProtobuf;
			nativeTypes = copyFrom.nativeTypes;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The serializer to copy from.
		 */
		protected Builder(ProtobufSerializer copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			addBeanTypesProtobuf = copyFrom.addBeanTypesProtobuf;
			nativeTypes = copyFrom.nativeTypes;
		}

		/**
		 * Add <js>"_type"</js> properties when needed.
		 *
		 * @return This object.
		 */
		public Builder addBeanTypesProtobuf() {
			return addBeanTypesProtobuf(true);
		}

		/**
		 * Same as {@link #addBeanTypesProtobuf()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public Builder addBeanTypesProtobuf(boolean value) {
			addBeanTypesProtobuf = value;
			return this;
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
		public Builder copy() {
			return new Builder(this);
		}

		@Override /* Overridden from Context.Builder<?> */
		public ProtobufSerializer build() {
			return cache(CACHE).build(ProtobufSerializer.class);
		}

		@Override /* Overridden from Context.Builder<?> */
		public HashKey hashKey() {
			return HashKey.of(super.hashKey(), addBeanTypesProtobuf, nativeTypes);
		}
	}

	/** Default serializer, all default settings. */
	public static final ProtobufSerializer DEFAULT = new ProtobufSerializer(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	final boolean addBeanTypesProtobuf;
	final boolean nativeTypes;

	private final Map<BeanPropertyMeta,ProtobufBeanPropertyMeta> protobufBeanPropertyMetas = new ConcurrentHashMap<>();
	private final Map<ClassMeta<?>,ProtobufClassMeta> protobufClassMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public ProtobufSerializer(Builder builder) {
		super(builder);
		this.addBeanTypesProtobuf = builder.addBeanTypesProtobuf;
		this.nativeTypes = builder.nativeTypes;
	}

	@Override /* Overridden from Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Overridden from Context */
	public ProtobufSerializerSession.Builder createSession() {
		return ProtobufSerializerSession.create(this);
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
	public ProtobufSerializerSession getSession() {
		return createSession().build();
	}

	@Override
	protected final boolean isAddBeanTypes() {
		return addBeanTypesProtobuf || super.isAddBeanTypes();
	}

	@Override /* Overridden from OutputStreamSerializer */
	protected FluentMap<String,Object> properties() {
		return super.properties()
			.a(PROP_addBeanTypesProtobuf, addBeanTypesProtobuf)
			.a(PROP_nativeTypes, nativeTypes);
	}
}
