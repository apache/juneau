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
package org.apache.juneau.proto;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.serializer.*;

/**
 * Serializes POJO models to Protobuf Text Format.
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * Handles <c>Accept</c> types:  <bc>text/protobuf, text/x-protobuf</bc>
 * <p>
 * Produces <c>Content-Type</c> types:  <bc>text/protobuf</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * Implements the <a class="doclink" href="https://protobuf.dev/reference/protobuf/textformat-spec">Protobuf Text Format</a>
 * serializer.  This is the human-readable text format used for protobuf configuration files and debugging,
 * NOT the binary wire format.
 *
 * <p>
 * The conversion is as follows:
 * <ul class='spaced-list'>
 * 	<li>
 * 		Maps (e.g. {@link java.util.HashMap}, {@link java.util.TreeMap}) and beans are converted to protobuf message fields.
 * 	<li>
 * 		Collections (e.g. {@link java.util.HashSet}, {@link java.util.LinkedList}) and Java arrays of simple values are converted to
 * 		protobuf list syntax {@code [v1, v2, ...]}.
 * 	<li>
 * 		Collections of beans are converted to repeated field names.
 * 	<li>
 * 		{@link String Strings} are converted to quoted strings with C-style escaping.
 * 	<li>
 * 		{@link Number Numbers} (e.g. {@link Integer}, {@link Long}, {@link Double}) are converted to numeric literals.
 * 	<li>
 * 		{@link Boolean Booleans} are converted to {@code true}/{@code false}.
 * 	<li>
 * 		Enum values are converted to unquoted identifiers.
 * 	<li>
 * 		{@code null} properties are omitted (protobuf text format has no null representation).
 * 	<li>
 * 		{@code byte[]} is converted to a quoted string using C-style hex escaping.
 * </ul>
 *
 * <p>
 * Date/time types ({@code Calendar}, {@code Date}, {@code Temporal} subtypes, {@code Duration}) are serialized as
 * ISO 8601 strings.  Swaps can be used to convert non-serializable POJOs into serializable forms or to override
 * the default format for types that already have built-in support.
 *
 * <h5 class='topic'>Behavior-specific subclasses</h5>
 * <p>
 * The following direct subclasses are provided for convenience:
 * <ul class='spaced-list'>
 * 	<li>
 * 		{@link ProtoSerializer.Readable} - Default serializer with additional indentation spacing.
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Use one of the default serializers to serialize a POJO</jc>
 * 	String <jv>proto</jv> = ProtoSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>someObject</jv>);
 *
 * 	<jc>// Create a custom serializer</jc>
 * 	ProtoSerializer <jv>serializer</jv> = ProtoSerializer.<jsm>create</jsm>().addBeanTypes().build();
 *
 * 	<jc>// Clone an existing serializer and modify it</jc>
 * 	<jv>serializer</jv> = ProtoSerializer.<jsf>DEFAULT</jsf>.copy().useColonForMessages().build();
 *
 * 	<jc>// Serialize a POJO to Protobuf Text Format</jc>
 * 	<jv>proto</jv> = <jv>serializer</jv>.serialize(<jv>someObject</jv>);
 * </p>
 *
 * <h5 class='figure'>Example output (Map of name/age):</h5>
 * <p class='bcode'>
 * 	name: "Alice"
 * 	age: 30
 * </p>
 *
 * <h5 class='figure'>Complex (nested object + array):</h5>
 * <p class='bcode'>
 * 	name: "Alice"
 * 	age: 30
 * 	address {
 * 	  street: "123 Main St"
 * 	  city: "Boston"
 * 	  state: "MA"
 * 	}
 * 	tags: ["a", "b", "c"]
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ProtobufBasics">Protobuf Text Format Basics</a>
 * 	<li class='link'><a class="doclink" href="https://protobuf.dev/reference/protobuf/textformat-spec">Protobuf Text Format Specification</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S110", // Builder pattern requires many parameters
	"java:S115"   // PROP_/ARG_ prefix follows framework convention
})
public class ProtoSerializer extends WriterSerializer implements ProtoMetaProvider {

	private static final String PROP_useListSyntaxForBeans = "useListSyntaxForBeans";
	private static final String PROP_useColonForMessages = "useColonForMessages";
	private static final String ARG_copyFrom = "copyFrom";

	/**
	 * Builder class.
	 */
	public static class Builder extends WriterSerializer.Builder {

		private static final Cache<HashKey, ProtoSerializer> CACHE =
			Cache.of(HashKey.class, ProtoSerializer.class).build();

		private boolean useListSyntaxForBeans = false;
		private boolean useColonForMessages = false;

		protected Builder() {
			produces("text/protobuf");
			accept("text/protobuf,text/x-protobuf");
		}

		protected Builder(Builder copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			useListSyntaxForBeans = copyFrom.useListSyntaxForBeans;
			useColonForMessages = copyFrom.useColonForMessages;
		}

		protected Builder(ProtoSerializer copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			useListSyntaxForBeans = copyFrom.useListSyntaxForBeans;
			useColonForMessages = copyFrom.useColonForMessages;
		}

		/**
		 * Use list syntax for bean collections.
		 *
		 * <p>
		 * When <jk>true</jk>, collections of beans use <c>[{...}, {...}]</c> instead of repeated field names.
		 *
		 * @param value The flag.
		 * @return This object.
		 */
		public Builder useListSyntaxForBeans(boolean value) {
			useListSyntaxForBeans = value;
			return this;
		}

		/**
		 * Use colon before message values.
		 *
		 * <p>
		 * When <jk>true</jk>, message fields use <c>field: { ... }</c> instead of <c>field { ... }</c>.
		 *
		 * @param value The flag.
		 * @return This object.
		 */
		public Builder useColonForMessages(boolean value) {
			useColonForMessages = value;
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

		@Override
		public ProtoSerializer build() {
			return cache(CACHE).build(ProtoSerializer.class);
		}

		@Override
		public Builder copy() {
			return new Builder(this);
		}

		@Override
		public HashKey hashKey() {
			return HashKey.of(
				super.hashKey(),
				useListSyntaxForBeans,
				useColonForMessages
			);
		}
	}

	/** Default serializer. */
	public static final ProtoSerializer DEFAULT = new ProtoSerializer(create());

	/** Default serializer with additional indentation. */
	public static final ProtoSerializer DEFAULT_READABLE = new ProtoSerializer(create().useWhitespace());

	/** Default serializer subclass with readable output. */
	public static class Readable extends ProtoSerializer {
		/**
		 * Constructor.
		 */
		public Readable() {
			super(create().useWhitespace());
		}
	}

	public static Builder create() {
		return new Builder();
	}

	protected final boolean useListSyntaxForBeans;
	protected final boolean useColonForMessages;

	private final Map<ClassMeta<?>, ProtoClassMeta> protoClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta, ProtoBeanPropertyMeta> protoBeanPropertyMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param builder The builder.
	 */
	public ProtoSerializer(Builder builder) {
		super(builder);
		useListSyntaxForBeans = builder.useListSyntaxForBeans;
		useColonForMessages = builder.useColonForMessages;
	}

	@Override
	public ProtoSerializerSession.Builder createSession() {
		return ProtoSerializerSession.create(this);
	}

	@Override
	public Builder copy() {
		return new Builder(this);
	}

	@Override
	protected FluentMap<String, Object> properties() {
		return super.properties()
			.a(PROP_useListSyntaxForBeans, useListSyntaxForBeans)
			.a(PROP_useColonForMessages, useColonForMessages);
	}

	@Override /* ProtoMetaProvider */
	public ProtoBeanPropertyMeta getProtoBeanPropertyMeta(BeanPropertyMeta bpm) {
		if (bpm == null)
			return ProtoBeanPropertyMeta.DEFAULT;
		return protoBeanPropertyMetas.computeIfAbsent(bpm, k -> new ProtoBeanPropertyMeta(k.getDelegateFor(), this));
	}

	@Override /* ProtoMetaProvider */
	public ProtoClassMeta getProtoClassMeta(ClassMeta<?> cm) {
		return protoClassMetas.computeIfAbsent(cm, k -> new ProtoClassMeta(k, this));
	}
}
