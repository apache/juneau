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
import org.apache.juneau.parser.*;
import org.apache.juneau.uon.*;

/**
 * OpenAPI part parser.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc OpenApiParsers}
 * </ul>
 */
@ConfigurableContext
public class OpenApiParser extends UonParser implements OpenApiMetaProvider, OpenApiCommon {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	static final String PREFIX = "OpenApiParser";

	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Reusable instance of {@link OpenApiParser}. */
	public static final OpenApiParser DEFAULT = new OpenApiParser(create());


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
	 * @param builder The builder for this object.
	 */
	protected OpenApiParser(OpenApiParserBuilder builder) {
		super(builder);
		ContextProperties cp = getContextProperties();
		format = cp.get(OAPI_format, HttpPartFormat.class).orElse(HttpPartFormat.NO_FORMAT);
		collectionFormat = cp.get(OAPI_collectionFormat, HttpPartCollectionFormat.class).orElse(HttpPartCollectionFormat.NO_COLLECTION_FORMAT);
	}

	@Override /* Context */
	public OpenApiParserBuilder copy() {
		return new OpenApiParserBuilder(this);
	}

	/**
	 * Instantiates a new clean-slate {@link OpenApiParserBuilder} object.
	 *
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> UonPartParserBuilder()</code>.
	 *
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #copy()} copies
	 * the settings of the object called on.
	 *
	 * @return A new {@link OpenApiParserBuilder} object.
	 */
	public static OpenApiParserBuilder create() {
		return new OpenApiParserBuilder();
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Entry point methods
	//-------------------------------------------------------------------------------------------------------------------

	@Override
	public OpenApiParserSession createSession() {
		return new OpenApiParserSession(this, ParserSessionArgs.DEFAULT);
	}

	@Override
	public OpenApiParserSession createSession(ParserSessionArgs args) {
		return new OpenApiParserSession(this, args);
	}

	@Override
	public OpenApiParserSession createPartSession(ParserSessionArgs args) {
		return new OpenApiParserSession(this, args);
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
				"OpenApiParser",
				OMap
					.create()
					.filtered()
			);
	}
}
