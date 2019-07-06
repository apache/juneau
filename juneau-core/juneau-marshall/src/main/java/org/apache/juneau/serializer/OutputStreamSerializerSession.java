// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.serializer;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;

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
 */
public abstract class OutputStreamSerializerSession extends SerializerSession {

	private final OutputStreamSerializer ctx;

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param ctx
	 * 	The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
	 * @param args
	 * 	Runtime arguments.
	 * 	These specify session-level information such as locale and URI context.
	 * 	It also include session-level properties that override the properties defined on the bean and
	 * 	serializer contexts.
	 */
	protected OutputStreamSerializerSession(OutputStreamSerializer ctx, SerializerSessionArgs args) {
		super(ctx, args);
		this.ctx = ctx;
	}

	/**
	 * Constructor for sessions that don't require context.
	 *
	 * @param args
	 * 	Runtime session arguments.
	 */
	protected OutputStreamSerializerSession(SerializerSessionArgs args) {
		this(OutputStreamSerializer.DEFAULT, args);
	}

	@Override /* SerializerSession */
	public final boolean isWriterSerializer() {
		return false;
	}

	@Override /* SerializerSession */
	protected SerializerPipe createPipe(Object output) {
		return new SerializerPipe(output);
	}

	/**
	 * Convenience method for serializing an object to a <code><jk>byte</jk></code>.
	 *
	 * @param o The object to serialize.
	 * @return The output serialized to a byte array.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	@Override /* SerializerSession */
	public final byte[] serialize(Object o) throws SerializeException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			serialize(o, baos);
		} catch (IOException e) {
			throw new SerializeException(e); // Should never happen.
		}
		return baos.toByteArray();
	}

	@Override /* SerializerSession */
	public final String serializeToString(Object o) throws SerializeException {
		byte[] b = serialize(o);
		switch(getBinaryFormat()) {
			case SPACED_HEX:  return StringUtils.toSpacedHex(b);
			case HEX:  return StringUtils.toHex(b);
			case BASE64:  return StringUtils.base64Encode(b);
			default: return null;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  Binary output format.
	 *
	 * @see OutputStreamSerializer#OSSERIALIZER_binaryFormat
	 * @return
	 * 	The format to use for the {@link #serializeToString(Object)} method on stream-based serializers when converting byte arrays to strings.
	 */
	protected final BinaryFormat getBinaryFormat() {
		return ctx.getBinaryFormat();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Session */
	public ObjectMap toMap() {
		return super.toMap()
			.append("OutputStreamSerializerSession", new DefaultFilteringObjectMap()
			);
	}
}
