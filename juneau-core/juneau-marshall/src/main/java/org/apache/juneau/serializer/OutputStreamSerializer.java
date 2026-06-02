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

import org.apache.juneau.commons.http.MediaType;
import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.lang.annotation.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.commons.function.*;
import org.apache.juneau.commons.reflect.*;

/**
 * Subclass of {@link Serializer} for byte-based serializers.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SerializersAndParsers">Serializers and Parsers</a>

 * </ul>
 */
public class OutputStreamSerializer extends Serializer {

	private static final String ARG_copyFrom = "copyFrom";

	/**
	 * Builder class.
	 */
	public abstract static class Builder<SELF extends Builder<SELF>> extends Serializer.Builder<SELF> {

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(Builder<?> copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(OutputStreamSerializer copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
		}

		@Override /* Overridden from Context.Builder */
		public OutputStreamSerializer build() {
			return build(OutputStreamSerializer.class);
		}

		@Override /* Overridden from Context.Builder */
		public abstract SELF copy();


	}

	/**
	 * Concrete default builder leaf for the non-subclassed {@link OutputStreamSerializer#create()} / {@link OutputStreamSerializer#copy()} path.
	 */
	public static final class DefaultBuilder extends Builder<DefaultBuilder> {

		DefaultBuilder() {}

		DefaultBuilder(OutputStreamSerializer copyFrom) {
			super(copyFrom);
		}

		DefaultBuilder(Builder<?> copyFrom) {
			super(copyFrom);
		}

		@Override /* Overridden from Context.Builder */
		public DefaultBuilder copy() {
			return new DefaultBuilder(this);
		}
	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder<?> create() {
		return new DefaultBuilder();
	}

	/**
	 * Constructor.
	 *
	 * @param builder
	 * 	The builder for this object.
	 */
	protected OutputStreamSerializer(Builder<?> builder) {
		super(builder);
	}

	@Override /* Overridden from Context */
	public OutputStreamSerializerSession.Builder<?> createSession() {
		return OutputStreamSerializerSession.create(this);
	}

	@Override /* Overridden from Context */
	public OutputStreamSerializerSession getSession() { return createSession().build(); }

	@Override /* Overridden from Serializer */
	public final boolean isWriterSerializer() { return false; }

	/**
	 * Convenience method for serializing an object to a <code><jk>byte</jk></code>.
	 *
	 * @param o The object to serialize.
	 * @return The output serialized to a byte array.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	@Override
	public final byte[] serialize(Object o) throws SerializeException {
		return getSession().serialize(o);
	}

	/**
	 * Binary output format.
	 *
	 * <p>
	 * Resolves from the configured {@link MarshallingContext#getBinaryFormat()}.
	 *
	 * @return The binary wire format used by this serializer.
	 */
	protected final BinaryFormat getBinaryFormat() { return getMarshallingContext().getBinaryFormat(); }
}
