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

/**
 * Subclass of {@link Serializer} for byte-based serializers.
 */
public abstract class OutputStreamSerializer extends Serializer {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	private static final String PREFIX = "OutputStreamSerializer.";

	/**
	 * Configuration property:  Binary output format.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"OutputStreamSerializer.binaryFormat.s"</js>
	 * 	<li><b>Data type:</b>  {@link BinaryFormat}
	 * 	<li><b>Default:</b>  {@link BinaryFormat#HEX}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link OutputStreamSerializerBuilder#binaryFormat(BinaryFormat)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * When using the {@link #serializeToString(Object)} method on stream-based serializers, this defines the format to use
	 * when converting the resulting byte array to a string.
	 *
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that serializes to BASE64.</jc>
	 * 	OutputStreamSerializer s = MsgPackSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.binaryFormat(<jsf>BASE64</jsf>)
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	OutputStreamSerializer s = MsgPackSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>SERIALIZER_binaryOutputFormat</jsf>, <js>"BASE64"</js>)
	 * 		.build();
	 *
	 * 	<jc>// The bean we want to serialize.</jc>
	 * 	<jk>public class</jk> MyBean {...}
	 *
	 * 	<jc>// MessagePack will generate BASE64-encoded string.</jc>
	 * 	String msgPack = s.serializeToString(<jk>new</jk> MyBean());
	 * </p>
	 */
	public static final String OSSERIALIZER_binaryFormat = PREFIX + "binaryFormat.s";

	static final OutputStreamSerializer DEFAULT = new OutputStreamSerializer(PropertyStore.create().build(), "", "") {
		@Override
		public OutputStreamSerializerSession createSession(SerializerSessionArgs args) {
			throw new NoSuchMethodError();
		}
	};

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final BinaryFormat binaryFormat;

	/**
	 * Constructor.
	 *
	 * @param ps
	 * 	The property store containing all the settings for this object.
	 * @param produces
	 * 	The media type that this serializer produces.
	 * @param accept
	 * 	The accept media types that the serializer can handle.
	 * 	<p>
	 * 	Can contain meta-characters per the <code>media-type</code> specification of {@doc RFC2616.section14.1}
	 * 	<p>
	 * 	If empty, then assumes the only media type supported is <code>produces</code>.
	 * 	<p>
	 * 	For example, if this serializer produces <js>"application/json"</js> but should handle media types of
	 * 	<js>"application/json"</js> and <js>"text/json"</js>, then the arguments should be:
	 * 	<p class='bcode w800'>
	 * 	<jk>super</jk>(ps, <js>"application/json"</js>, <js>"application/json,text/json"</js>);
	 * 	</p>
	 * 	<br>...or...
	 * 	<p class='bcode w800'>
	 * 	<jk>super</jk>(ps, <js>"application/json"</js>, <js>"*&#8203;/json"</js>);
	 * 	</p>
	 * <p>
	 * The accept value can also contain q-values.
	 */
	protected OutputStreamSerializer(PropertyStore ps, String produces, String accept) {
		super(ps, produces, accept);

		binaryFormat = getProperty(OSSERIALIZER_binaryFormat, BinaryFormat.class, BinaryFormat.HEX);
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Abstract methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* SerializerSession */
	public abstract OutputStreamSerializerSession createSession(SerializerSessionArgs args);


	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

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
	 * Configuration property:  Binary output format.
	 *
	 * @see #OSSERIALIZER_binaryFormat
	 * @return
	 * 	The format to use for the {@link #serializeToString(Object)} method on stream-based serializers when converting byte arrays to strings.
	 */
	protected final BinaryFormat getBinaryFormat() {
		return binaryFormat;
	}

	@Override /* Context */
	public ObjectMap asMap() {
		return super.asMap()
			.append("OutputStreamSerializer", new ObjectMap()
				.append("binaryFormat", binaryFormat)
			);
	}

	/**
	 * @deprecated No replacement.
	 */
	@SuppressWarnings({ "javadoc", "unused" })
	@Deprecated
	public final String serializeToHex(Object o) throws SerializeException {
		return null;
	}
}
