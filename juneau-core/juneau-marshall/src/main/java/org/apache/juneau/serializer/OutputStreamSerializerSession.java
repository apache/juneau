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
package org.apache.juneau.serializer;

import static org.apache.juneau.common.utils.StringUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.svl.*;

/**
 * Subclass of {@link SerializerSession} for stream-based serializers.
 *
 * <h5 class='topic'>Description</h5>
 *
 * This class is the parent class of all byte-based serializers.
 * <br>It has 1 abstract method to implement...
 * <ul>
 * 	<li>{@link #doSerialize(SerializerPipe, Object)}
 * </ul>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SerializersAndParsers">Serializers and Parsers</a>
 * </ul>
 */
public class OutputStreamSerializerSession extends SerializerSession {
	/**
	 * Builder class.
	 */
	public static class Builder extends SerializerSession.Builder {

		OutputStreamSerializer ctx;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 */
		protected Builder(OutputStreamSerializer ctx) {
			super(ctx);
			this.ctx = ctx;
		}

		@Override /* Overridden from Builder */
		public <T> Builder apply(Class<T> type, Consumer<T> apply) {
			super.apply(type, apply);
			return this;
		}

		@Override
		public OutputStreamSerializerSession build() {
			return new OutputStreamSerializerSession(this);
		}

		@Override /* Overridden from Builder */
		public Builder debug(Boolean value) {
			super.debug(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder javaMethod(Method value) {
			super.javaMethod(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder locale(Locale value) {
			super.locale(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder localeDefault(Locale value) {
			super.localeDefault(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder mediaType(MediaType value) {
			super.mediaType(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder mediaTypeDefault(MediaType value) {
			super.mediaTypeDefault(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder properties(Map<String,Object> value) {
			super.properties(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder property(String key, Object value) {
			super.property(key, value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder resolver(VarResolverSession value) {
			super.resolver(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder schema(HttpPartSchema value) {
			super.schema(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder schemaDefault(HttpPartSchema value) {
			super.schemaDefault(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder timeZone(TimeZone value) {
			super.timeZone(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder timeZoneDefault(TimeZone value) {
			super.timeZoneDefault(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder unmodifiable() {
			super.unmodifiable();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder uriContext(UriContext value) {
			super.uriContext(value);
			return this;
		}
	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * @return A new builder.
	 */
	public static Builder create(OutputStreamSerializer ctx) {
		return new Builder(ctx);
	}

	private final OutputStreamSerializer ctx;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected OutputStreamSerializerSession(Builder builder) {
		super(builder);
		ctx = builder.ctx;
	}

	@Override /* Overridden from SerializerSession */
	public final boolean isWriterSerializer() { return false; }

	/**
	 * Convenience method for serializing an object to a <code><jk>byte</jk></code>.
	 *
	 * @param o The object to serialize.
	 * @return The output serialized to a byte array.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	@Override /* Overridden from SerializerSession */
	public final byte[] serialize(Object o) throws SerializeException {
		var baos = new ByteArrayOutputStream();
		try {
			serialize(o, baos);
			baos.flush();
		} catch (IOException e) {
			throw new SerializeException(e); // Should never happen.
		}
		return baos.toByteArray();
	}

	@Override /* Overridden from SerializerSession */
	public final String serializeToString(Object o) throws SerializeException {
		byte[] b = serialize(o);
		return switch (getBinaryFormat()) {
			case SPACED_HEX -> toSpacedHex(b);
			case HEX -> toHex(b);
			case BASE64 -> base64Encode(b);
			default -> null;
		};
	}

	@Override /* Overridden from SerializerSession */
	protected SerializerPipe createPipe(Object output) {
		return new SerializerPipe(output);
	}

	/**
	 * Binary output format.
	 *
	 * @see OutputStreamSerializer.Builder#binaryFormat(BinaryFormat)
	 * @return
	 * 	The format to use for the {@link #serializeToString(Object)} method on stream-based serializers when converting byte arrays to strings.
	 */
	protected final BinaryFormat getBinaryFormat() { return ctx.getBinaryFormat(); }
}