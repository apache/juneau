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

import static org.apache.juneau.Context.*;

import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.http.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;

/**
 * Runtime arguments common to all bean, serializer, and parser sessions.
 */
public class SessionArgs {

	SessionProperties properties = SessionProperties.create();

	/**
	 * Constructor.
	 */
	public SessionArgs() {}

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
	 * If not specified, defaults to {@link Context#CONTEXT_debug}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public SessionArgs debug(Boolean value) {
		property(CONTEXT_debug, value);
		return this;
	}

	/**
	 * The session locale.
	 *
	 * <p>
	 * Specifies the default locale for serializer and parser sessions.
	 *
	 * <p>
	 * If not specified, defaults to {@link Context#CONTEXT_locale}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>If <jk>null</jk>, then the locale defined on the context is used.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public SessionArgs locale(Locale value) {
		property(CONTEXT_locale, value);
		return this;
	}

	/**
	 * The session media type.
	 *
	 * <p>
	 * Specifies the default media type value for serializer and parser sessions.
	 *
	 * <p>
	 * If not specified, defaults to {@link Context#CONTEXT_mediaType}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public SessionArgs mediaType(MediaType value) {
		property(CONTEXT_mediaType, value);
		return this;
	}

	/**
	 * The session timezone.
	 *
	 * <p>
	 * Specifies the default timezone for serializer and parser sessions.
	 *
	 * <p>
	 * If not specified, defaults to {@link BeanContext#CONTEXT_timeZone}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public SessionArgs timeZone(TimeZone value) {
		property(CONTEXT_timeZone, value);
		return this;
	}

	/**
	 * Session-level properties.
	 *
	 * <p>
	 * Overrides context-level properties.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public SessionArgs properties(Map<String,Object> value) {
		this.properties = SessionProperties.create(value);
		return this;
	}

	/**
	 * Adds a property to this session.
	 *
	 * @param key The property key.
	 * @param value The property value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public SessionArgs property(String key, Object value) {
		if (value == null) {
			properties.remove(key);
		} else {
			properties.put(key, value);
		}
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the properties defined on this bean as a simple map for debugging purposes.
	 *
	 * <p>
	 * Use <c>SimpleJson.<jsf>DEFAULT</jsf>.println(<jv>thisBean</jv>)</c> to dump the contents of this bean to the console.
	 *
	 * @return A new map containing this bean's properties.
	 */
	public OMap toMap() {
		return OMap
			.create()
			.filtered()
			.append("SessionArgs", OMap.create().filtered()
				.append("properties", properties.asMap())
			);
	}

	@Override /* Object */
	public String toString() {
		return SimpleJsonSerializer.DEFAULT_READABLE.toString(toMap());
	}

	// <FluentSetters>

	// </FluentSetters>
}
