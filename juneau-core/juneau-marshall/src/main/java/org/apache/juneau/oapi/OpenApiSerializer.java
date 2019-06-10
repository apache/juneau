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
package org.apache.juneau.oapi;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.uon.*;

/**
 * Serializes POJOs to values suitable for transmission as HTTP headers, query/form-data parameters, and path variables.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'>{@doc juneau-marshall.OpenApiDetails.Serializers}
 * </ul>
 */
@ConfigurableContext
public class OpenApiSerializer extends UonSerializer {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	static final String PREFIX = "OpenApiSerializer";

	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Reusable instance of {@link OpenApiSerializer}, all default settings. */
	public static final OpenApiSerializer DEFAULT = new OpenApiSerializer(PropertyStore.DEFAULT);


	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

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
	public OpenApiSerializer(PropertyStore ps, String produces, String accept) {
		super(
			ps.builder()
				.set(UON_encoding, false)
				.build(),
			produces,
			accept
		);
	}

	/**
	 * Constructor.
	 *
	 * @param ps
	 * 	The property store containing all the settings for this object.
	 */
	public OpenApiSerializer(PropertyStore ps) {
		this(ps, "text/openapi", null);
	}

	@Override /* Context */
	public OpenApiSerializerBuilder builder() {
		return new OpenApiSerializerBuilder(getPropertyStore());
	}

	/**
	 * Instantiates a new clean-slate {@link OpenApiSerializerBuilder} object.
	 *
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #builder()} copies
	 * the settings of the object called on.
	 *
	 * @return A new {@link OpenApiSerializerBuilder} object.
	 */
	public static OpenApiSerializerBuilder create() {
		return new OpenApiSerializerBuilder();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Entry point methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public OpenApiSerializerSession createSession() {
		return createSession(createDefaultSessionArgs());
	}

	@Override /* Serializer */
	public OpenApiSerializerSession createSession(SerializerSessionArgs args) {
		return new OpenApiSerializerSession(this, args);
	}

	@Override /* HttpPartSerializer */
	public OpenApiSerializerSession createPartSession() {
		return createPartSession(null);
	}

	@Override /* HttpPartSerializer */
	public OpenApiSerializerSession createPartSession(SerializerSessionArgs args) {
		return new OpenApiSerializerSession(this, args);
	}

	@Override /* HttpPartSerializer */
	public String serialize(HttpPartType partType, HttpPartSchema schema, Object value) throws SchemaValidationException, SerializeException {
		return createPartSession().serialize(partType, schema, value);
	}

	@Override /* HttpPartSerializer */
	public String serialize(HttpPartSchema schema, Object value) throws SchemaValidationException, SerializeException {
		return createPartSession().serialize(null, schema, value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public ObjectMap asMap() {
		return super.asMap()
			.append("OpenApiSerializer", new DefaultFilteringObjectMap()
			);
	}
}
