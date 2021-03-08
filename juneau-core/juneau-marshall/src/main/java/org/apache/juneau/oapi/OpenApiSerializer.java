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

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.jsonschema.annotation.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.uon.*;

/**
 * Serializes POJOs to values suitable for transmission as HTTP headers, query/form-data parameters, and path variables.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc OpenApiSerializers}
 * </ul>
 */
@ConfigurableContext
public class OpenApiSerializer extends UonSerializer implements OpenApiMetaProvider, OpenApiCommon {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	static final String PREFIX = "OpenApiSerializer";

	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Reusable instance of {@link OpenApiSerializer}, all default settings. */
	public static final OpenApiSerializer DEFAULT = new OpenApiSerializer(ContextProperties.DEFAULT);


	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final Map<ClassMeta<?>,OpenApiClassMeta> openApiClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,OpenApiBeanPropertyMeta> openApiBeanPropertyMetas = new ConcurrentHashMap<>();
	private final HttpPartFormat format;
	private final HttpPartCollectionFormat collectionFormat;

	/**
	 * Constructor.
	 *
	 * @param cp
	 * 	The property store containing all the settings for this object.
	 * @param produces
	 * 	The media type that this serializer produces.
	 * @param accept
	 * 	The accept media types that the serializer can handle.
	 * 	<p>
	 * 	Can contain meta-characters per the <c>media-type</c> specification of {@doc ExtRFC2616.section14.1}
	 * 	<p>
	 * 	If empty, then assumes the only media type supported is <c>produces</c>.
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
	public OpenApiSerializer(ContextProperties cp, String produces, String accept) {
		super(
			cp.copy()
				.setDefault(UON_encoding, false)
				.build(),
			produces,
			accept
		);
		format = cp.get(OAPI_format, HttpPartFormat.class).orElse(HttpPartFormat.NO_FORMAT);
		collectionFormat = cp.get(OAPI_collectionFormat, HttpPartCollectionFormat.class).orElse(HttpPartCollectionFormat.NO_COLLECTION_FORMAT);
	}

	/**
	 * Constructor.
	 *
	 * @param cp
	 * 	The property store containing all the settings for this object.
	 */
	public OpenApiSerializer(ContextProperties cp) {
		this(cp, "text/openapi", null);
	}

	@Override /* Context */
	public OpenApiSerializerBuilder copy() {
		return new OpenApiSerializerBuilder(this);
	}

	/**
	 * Instantiates a new clean-slate {@link OpenApiSerializerBuilder} object.
	 *
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #copy()} copies
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
	public OpenApiSerializerSession createPartSession(SerializerSessionArgs args) {
		return new OpenApiSerializerSession(this, args);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Extended metadata
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* OpenApiMetaProvider */
	public OpenApiClassMeta getOpenApiClassMeta(ClassMeta<?> cm) {
		OpenApiClassMeta m = openApiClassMetas.get(cm);
		if (m == null) {
			m = new OpenApiClassMeta(cm, this);
			openApiClassMetas.put(cm, m);
		}
		return m;
	}

	@Override /* OpenApiMetaProvider */
	public OpenApiBeanPropertyMeta getOpenApiBeanPropertyMeta(BeanPropertyMeta bpm) {
		if (bpm == null)
			return OpenApiBeanPropertyMeta.DEFAULT;
		OpenApiBeanPropertyMeta m = openApiBeanPropertyMetas.get(bpm);
		if (m == null) {
			m = new OpenApiBeanPropertyMeta(bpm.getDelegateFor(), this);
			openApiBeanPropertyMetas.put(bpm, m);
		}
		return m;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the default format to use when not otherwise specified via {@link Schema#format()}
	 *
	 * @return The default format to use when not otherwise specified via {@link Schema#format()}
	 */
	protected final HttpPartFormat getFormat() {
		return format;
	}

	/**
	 * Returns the default collection format to use when not otherwise specified via {@link Schema#collectionFormat()}
	 *
	 * @return The default collection format to use when not otherwise specified via {@link Schema#collectionFormat()}
	 */
	protected final HttpPartCollectionFormat getCollectionFormat() {
		return collectionFormat;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public OMap toMap() {
		return super.toMap()
			.a(
				"OpenApiSerializer",
				OMap
					.create()
					.filtered()
			);
	}
}
