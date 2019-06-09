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
package org.apache.juneau;

import static org.apache.juneau.BeanContext.*;

import java.util.*;

import org.apache.juneau.http.*;
import org.apache.juneau.httppart.*;

/**
 * Runtime arguments common to all bean, serializer, and parser sessions.
 */
public class BeanSessionArgs extends SessionArgs {

	/**
	 * Default empty session arguments.
	 */
	public static final BeanSessionArgs DEFAULT = new BeanSessionArgs();

	HttpPartSchema schema;

	/**
	 * Constructor
	 */
	public BeanSessionArgs() {}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Debug mode.
	 *
	 * <p>
	 * Enables the following additional information during parsing:
	 * <ul>
	 * 	<li> When bean setters throws exceptions, the exception includes the object stack information in order to determine how that method was invoked.
	 * </ul>
	 *
	 * <p>
	 * If not specified, defaults to {@link BeanContext#BEAN_debug}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public BeanSessionArgs debug(Boolean value) {
		property(BEAN_debug, value);
		return this;
	}

	/**
	 * The session locale.
	 *
	 * <p>
	 * Specifies the default locale for serializer and parser sessions.
	 *
	 * <p>
	 * If not specified, defaults to {@link BeanContext#BEAN_locale}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>If <jk>null</jk>, then the locale defined on the context is used.
	 * @return This object (for method chaining).
	 */
	public BeanSessionArgs locale(Locale value) {
		property(BEAN_locale, value);
		return this;
	}

	/**
	 * The session media type.
	 *
	 * <p>
	 * Specifies the default media type value for serializer and parser sessions.
	 *
	 * <p>
	 * If not specified, defaults to {@link BeanContext#BEAN_mediaType}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public BeanSessionArgs mediaType(MediaType value) {
		property(BEAN_mediaType, value);
		return this;
	}

	/**
	 * HTTP-part schema.
	 *
	 * <p>
	 * Used for schema-based serializers and parsers to define additional formatting.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public BeanSessionArgs schema(HttpPartSchema value) {
		this.schema = value;
		return this;
	}

	/**
	 * The session timezone.
	 *
	 * <p>
	 * Specifies the default timezone for serializer and parser sessions.
	 *
	 * <p>
	 * If not specified, defaults to {@link BeanContext#BEAN_timeZone}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public BeanSessionArgs timeZone(TimeZone value) {
		property(BEAN_timeZone, value);
		return this;
	}

	@Override /* SessionArgs */
	public BeanSessionArgs properties(ObjectMap value) {
		super.properties(value);
		return this;
	}

	@Override /* SessionArgs */
	public BeanSessionArgs property(String key, Object value) {
		super.property(key, value);
		return this;
	}

	@Override /* SessionArgs */
	public ObjectMap asMap() {
		return super.asMap()
			.append("BeanSessionArgs", new ObjectMap()
				.appendSkipNull("schema", schema)
			);
	}
}
