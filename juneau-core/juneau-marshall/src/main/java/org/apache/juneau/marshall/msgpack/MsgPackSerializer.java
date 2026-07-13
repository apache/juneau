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
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.stream.*;

/**
 * Serializes POJO models to MessagePack.
 *
 * <h5 class='section'>Media types:</h5>
 *
 * Handles <c>Accept</c> types:  <bc>application/msgpack, octal/msgpack</bc>
 * <p>
 * Produces <c>Content-Type</c> types: <bc>application/msgpack</bc>
 *
 * <p>
 * The legacy media type <bc>octal/msgpack</bc> is retained in the <c>Accept</c> list as a
 * backward-compatibility alias. The <c>Content-Type</c> emitted by the serializer is always the
 * RFC-standard <bc>application/msgpack</bc> value.
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * Java-to-MessagePack type mapping:
 * <ul class='spaced-list'>
 * 	<li>Maps and beans → MessagePack map type
 * 	<li>Collections and arrays → MessagePack array type
 * 	<li>{@link String} / {@link Enum} → MessagePack string type
 * 	<li>{@link Boolean} → MessagePack boolean type
 * 	<li>{@link Number} (int, long, float, double, etc.) → MessagePack number type (variable-length int or float)
 * 	<li>{@link Date} / {@link Calendar} / {@link Instant} → ISO 8601 string
 * 	<li>{@link Duration} → ISO 8601 duration string
 * 	<li>{@code byte[]} → MessagePack binary type
 * 	<li>{@code null} → MessagePack nil type
 * 	<li>All other types → fallback to {@code toString()} as string
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
	"resource"   // Closeable resources are owned by the caller's serializer session; Eclipse JDT @Owning warning is by design.
})
public class MsgPackSerializer extends OutputStreamSerializer implements MsgPackMetaProvider, TokenWritable, ArrayRecordWritable {

	// Argument name constants for assertArgNotNull
	private static final String ARG_builder = "builder";
	private static final String ARG_copyFrom = "copyFrom";

	// Property name constants
	private static final String PROP_addBeanTypesMsgPack = "addBeanTypesMsgPack";

	/** Default serializer, BASE64 string output. */
	public static class Base64 extends MsgPackSerializer {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		public Base64(Builder builder) {
			super(assertArgNotNull(ARG_builder, builder).binaryFormat(BinaryFormat.BASE64));
		}
	}

	/**
	 * Builder class.
	 */
	public static class Builder extends OutputStreamSerializer.Builder<Builder> {

		private static final Cache<HashKey,MsgPackSerializer> CACHE = Cache.of(HashKey.class, MsgPackSerializer.class).build();

		private boolean addBeanTypesMsgPack;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			produces("application/msgpack");
			accept("application/msgpack,octal/msgpack");
			addBeanTypesMsgPack = env("MsgPackSerializer.addBeanTypesMsgPack", false);
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
		protected Builder(MsgPackSerializer copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
		}

		/**
		 * Add <js>"_type"</js> properties when needed.
		 *
		 * <p>
		 * If <jk>true</jk>, then <js>"_type"</js> properties will be added to beans if their type cannot be inferred
		 * through reflection.
		 *
		 * <p>
		 * When present, this value overrides the {@link org.apache.juneau.marshall.serializer.Serializer.Builder#addBeanTypes()} setting and is
		 * provided to customize the behavior of specific serializers in a {@link SerializerSet}.
		 *
		 * @return This object.
		 */
		public Builder addBeanTypesMsgPack() {
			return addBeanTypesMsgPack(true);
		}

		/**
		 * Same as {@link #addBeanTypesMsgPack()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public Builder addBeanTypesMsgPack(boolean value) {
			addBeanTypesMsgPack = value;
			return this;
		}

		@Override /* Overridden from Context.Builder<?> */
		public MsgPackSerializer build() {
			return cache(CACHE).build(MsgPackSerializer.class);
		}

		@Override /* Overridden from Context.Builder<?> */
		public Builder copy() {
			return new Builder(this);
		}

		@Override /* Overridden from Context.Builder<?> */
		public HashKey hashKey() {
			return HashKey.of(super.hashKey(), addBeanTypesMsgPack);
		}


	}

	/** Default serializer, spaced-hex string output. */
	public static class SpacedHex extends MsgPackSerializer {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		public SpacedHex(Builder builder) {
			super(builder.binaryFormat(BinaryFormat.SPACED_HEX));
		}
	}

	/** Default serializer, all default settings.*/
	public static final MsgPackSerializer DEFAULT = new MsgPackSerializer(create());
	/** Default serializer, all default settings, spaced-hex string output.*/
	public static final MsgPackSerializer DEFAULT_SPACED_HEX = new SpacedHex(create());

	/** Default serializer, all default settings, spaced-hex string output.*/
	public static final MsgPackSerializer DEFAULT_BASE64 = new Base64(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	protected final boolean addBeanTypesMsgPack;

	private final Map<BeanPropertyMeta,MsgPackBeanPropertyMeta> msgPackBeanPropertyMetas = new ConcurrentHashMap<>();
	private final Map<ClassMeta<?>,MsgPackClassMeta> msgPackClassMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public MsgPackSerializer(Builder builder) {
		super(builder);
		this.addBeanTypesMsgPack = builder.addBeanTypesMsgPack;
	}

	@Override /* Overridden from Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Overridden from Context */
	public MsgPackSerializerSession.Builder createSession() {
		return MsgPackSerializerSession.create(this);
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
	public MsgPackSerializerSession getSession() { return createSession().build(); }

	/**
	 * Convenience delegator that opens a {@link MsgPackTokenWriter} over the output using
	 * <b>default session arguments</b> (mirrors {@link #serialize(Object)}).
	 *
	 * <p>
	 * The real implementation lives on {@link MsgPackSerializerSession#serializeTokens(Object)}.
	 * Callers that need request-derived configuration (locale, timezone, schema, swaps) should
	 * call {@link #createSession()} and invoke
	 * {@link MsgPackSerializerSession#serializeTokens(Object)} on the built session instead.
	 *
	 * @param output The output.
	 * @return A new {@link MsgPackTokenWriter}.
	 * @throws IOException If the output type is not supported or could not be opened.
	 */
	@Override /* TokenWritable */
	public TokenWriter serializeTokens(Object output) throws IOException {
		return getSession().serializeTokens(output);
	}

	@Override
	protected final boolean isAddBeanTypes() { return addBeanTypesMsgPack || super.isAddBeanTypes(); }

	@Override /* Overridden from OutputStreamSerializer */
	protected FluentMap<String,Object> properties() {
		return super.properties()
			.a(PROP_addBeanTypesMsgPack, addBeanTypesMsgPack);
	}

	/**
	 * Convenience delegator for the buffered array-element {@link RecordWriter} (uses default
	 * session args; see {@link #serializeTokens(Object)}).  Real impl on
	 * {@link MsgPackSerializerSession#serializeArrayRecords(Object)}.
	 *
	 * @param output The output.
	 * @return A buffered {@link RecordWriter}.
	 * @throws IOException If a problem occurred opening the underlying output.
	 */
	@Override /* ArrayRecordWritable */
	public RecordWriter serializeArrayRecords(Object output) throws IOException {
		return getSession().serializeArrayRecords(output);
	}

	@Override /* ArrayRecordWritable */
	public boolean isArrayRecordStreaming() { return false; }

	/**
	 * Convenience delegator for the streaming, count-prefixed array-element {@link RecordWriter}
	 * (uses default session args; see {@link #serializeTokens(Object)}).  Real impl on
	 * {@link MsgPackSerializerSession#serializeArrayRecords(Object, int)}.
	 *
	 * @param output The output (must be an {@link OutputStream}).
	 * @param expectedCount The number of elements that will be written.
	 * @return A streaming {@link RecordWriter}.
	 * @throws IOException If a problem occurred opening the underlying output.
	 */
	@Override /* ArrayRecordWritable */
	public RecordWriter serializeArrayRecords(Object output, int expectedCount) throws IOException {
		return getSession().serializeArrayRecords(output, expectedCount);
	}
}