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

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;

/**
 * Subclass of {@link Serializer} for byte-based serializers.
 * {@review}
 */
@ConfigurableContext
public abstract class OutputStreamSerializer extends Serializer {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	static final String PREFIX = "OutputStreamSerializer";

	/**
	 * Configuration property:  Binary output format.
	 *
	 * <p>
	 * When using the {@link #serializeToString(Object)} method on stream-based serializers, this defines the format to use
	 * when converting the resulting byte array to a string.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.serializer.OutputStreamSerializer#OSSERIALIZER_binaryFormat OSSERIALIZER_binaryFormat}
	 * 	<li><b>Name:</b>  <js>"OutputStreamSerializer.binaryFormat.s"</js>
	 * 	<li><b>Data type:</b>  {@link org.apache.juneau.BinaryFormat}
	 * 	<li><b>System property:</b>  <c>OutputStreamSerializer.binaryFormat</c>
	 * 	<li><b>Environment variable:</b>  <c>OUTPUTSTREAMSERIALIZER_BINARYFORMAT</c>
	 * 	<li><b>Default:</b>  {@link org.apache.juneau.BinaryFormat#HEX}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.serializer.annotation.SerializerConfig#binaryFormat()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.serializer.OutputStreamSerializerBuilder#binaryFormat(BinaryFormat)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String OSSERIALIZER_binaryFormat = PREFIX + ".binaryFormat.s";

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final BinaryFormat binaryFormat;

	/**
	 * Constructor.
	 *
	 * @param builder
	 * 	The builder for this object.
	 */
	protected OutputStreamSerializer(OutputStreamSerializerBuilder builder) {
		super(builder);

		ContextProperties cp = getContextProperties();
		binaryFormat = cp.get(OSSERIALIZER_binaryFormat, BinaryFormat.class).orElse(BinaryFormat.HEX);
	}

	@Override
	public abstract OutputStreamSerializerBuilder copy();

	//-----------------------------------------------------------------------------------------------------------------
	// Abstract methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* SerializerSession */
	public abstract OutputStreamSerializerSession createSession(SerializerSessionArgs args);


	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public OutputStreamSerializerSession createSession() {
		return createSession(createDefaultSessionArgs());
	}

	@Override /* Serializer */
	public final boolean isWriterSerializer() {
		return false;
	}

	/**
	 * Convenience method for serializing an object to a <code><jk>byte</jk></code>.
	 *
	 * @param o The object to serialize.
	 * @return The output serialized to a byte array.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	@Override
	public final byte[] serialize(Object o) throws SerializeException {
		return createSession(createDefaultSessionArgs()).serialize(o);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Binary output format.
	 *
	 * @see #OSSERIALIZER_binaryFormat
	 * @return
	 * 	The format to use for the {@link #serializeToString(Object)} method on stream-based serializers when converting byte arrays to strings.
	 */
	protected final BinaryFormat getBinaryFormat() {
		return binaryFormat;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public OMap toMap() {
		return super.toMap()
			.a(
				"OutputStreamSerializer",
				OMap
					.create()
					.filtered()
					.a("binaryFormat", binaryFormat)
			);
	}
}
