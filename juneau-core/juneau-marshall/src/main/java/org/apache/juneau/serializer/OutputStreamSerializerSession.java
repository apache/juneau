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

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.io.*;
import org.apache.juneau.*;

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
@SuppressWarnings({
	"java:S115" // Constants use UPPER_snakeCase convention
})
public class OutputStreamSerializerSession extends SerializerSession {

	// Argument name constants for assertArgNotNull
	private static final String ARG_ctx = "ctx";

	/**
	 * Builder class.
	 */
	@SuppressWarnings({
		"java:S119" // 'SELF' (CRTP self-type) is intentional and clearer than a single-letter name.
	})
	public abstract static class Builder<SELF extends Builder<SELF>> extends SerializerSession.Builder<SELF> {

		private OutputStreamSerializer ctx;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(OutputStreamSerializer ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
			this.ctx = ctx;
		}

		@Override
		public OutputStreamSerializerSession build() {
			return new OutputStreamSerializerSession(this);
		}

	}

	/**
	 * Concrete default builder leaf for the non-subclassed {@code create()} path (CRTP terminal).
	 */
	public static final class DefaultBuilder extends Builder<DefaultBuilder> {

		DefaultBuilder(OutputStreamSerializer ctx) {
			super(ctx);
		}
	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * 	<br>Cannot be <jk>null</jk>.
	 * @return A new builder.
	 */
	@SuppressWarnings({
		"java:S1452" // Builder<?> wildcard return intentional; callers use it to construct session instances polymorphically
	})
	public static Builder<?> create(OutputStreamSerializer ctx) {
		return new DefaultBuilder(assertArgNotNull(ARG_ctx, ctx));
	}

	private final OutputStreamSerializer ctx;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected OutputStreamSerializerSession(Builder<?> builder) {
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
		// Stream-to-string is a debug / display convenience for binary serializer output (BSON / MsgPack /
		// CBOR / Proto / Parquet).  It always emits HEX irrespective of the configured BinaryFormat so that
		// downstream tooling (test fixtures, log lines, REPL output) gets a stable, copy-pasteable
		// representation of the binary frame regardless of how the surrounding context configures byte[]
		// cell encoding.
		return BinaryFormat.HEX.format(serialize(o));
	}

	@Override /* Overridden from SerializerSession */
	protected SerializerPipe createPipe(Object output) {
		return new SerializerPipe(output);
	}

	/**
	 * Binary output format.
	 *
	 * @see MarshallingContext.Builder#binaryFormat(BinaryFormat)
	 * @return
	 * 	The format to use for the {@link #serializeToString(Object)} method on stream-based serializers when converting byte arrays to strings.
	 */
	protected final BinaryFormat getBinaryFormat() { return ctx.getMarshallingContext().getBinaryFormat(); }

	/**
	 * Returns whether this binary serializer has a native byte-array wire type.
	 *
	 * <p>
	 * When {@code true}, {@code byte[]} payloads bypass the configured
	 * {@link BinaryFormat} dispatch in {@link org.apache.juneau.swaps.BinarySwap} and the variant
	 * binary swap installed by {@code MarshalledPropertyPostProcessor}, and are written as raw
	 * bytes through the serializer's native binary opcode (e.g. MsgPack {@code bin} family, CBOR
	 * byte-string major type 2, BSON binary subtype {@code 0x05}).  When {@code false}, the
	 * configured {@link BinaryFormat} fires for non-{@code NOT_SET} formats and produces a text
	 * wire form (HEX / BASE64 / etc.) that the serializer emits as a string in its native string
	 * column / literal type — used by serializers whose wire format has no native byte-array
	 * primitive (Parquet column writer, binary RDF formats).
	 *
	 * <p>
	 * Defaults to {@code true}.  Subclasses without a native byte-array wire type
	 * (e.g. {@link org.apache.juneau.parquet.ParquetSerializerSession} and the binary RDF
	 * sessions) override and return {@code false}.
	 *
	 * @return {@code true} if this serializer has a native byte-array wire type.
	 */
	public boolean hasNativeBytes() {
		return true;
	}
}