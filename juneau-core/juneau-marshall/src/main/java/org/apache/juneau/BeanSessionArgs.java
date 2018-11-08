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

	Locale locale;
	TimeZone timeZone;
	MediaType mediaType;
	Boolean debug;
	HttpPartSchema schema;

	/**
	 * Constructor
	 */
	public BeanSessionArgs() {}

	/**
	 * Constructor.
	 *
	 * @param properties
	 * 	Session-level properties.
	 * 	<br>These override context-level properties.
	 * 	<br>Can be <jk>null</jk>.
	 * @param locale
	 * 	The session locale.
	 * 	<br>If <jk>null</jk>, then the locale defined on the context is used.
	 * @param timeZone
	 * 	The session timezone.
	 * 	<br>If <jk>null</jk>, then the timezone defined on the context is used.
	 * @param mediaType
	 * 	The session media type (e.g. <js>"application/json"</js>).
	 * 	<br>Can be <jk>null</jk>.
	 * @param schema
	 * 	The part schema for the serialized part.
	 * 	<br>Can be <jk>null</jk>.
	 * @param debug
	 * 	Enable debug mode for this session.
	 * 	<br>Can be <jk>null</jk> to use the debug setting on the bean context..
	 */
	public BeanSessionArgs(ObjectMap properties, Locale locale, TimeZone timeZone, MediaType mediaType, HttpPartSchema schema, Boolean debug) {
		super(properties);
		this.locale = locale;
		this.timeZone = timeZone;
		this.mediaType = mediaType;
		this.schema = schema;
		this.debug = debug;
	}

	/**
	 * The session locale.
	 *
	 * @param locale
	 * 	The session locale.
	 * 	<br>If <jk>null</jk>, then the locale defined on the context is used.
	 * @return This object (for method chaining).
	 */
	public BeanSessionArgs locale(Locale locale) {
		this.locale = locale;
		return this;
	}

	/**
	 * The session timezone.
	 *
	 * @param timeZone
	 * 	The session timezone.
	 * 	<br>If <jk>null</jk>, then the timezone defined on the context is used.
	 * @return This object (for method chaining).
	 */
	public BeanSessionArgs timeZone(TimeZone timeZone) {
		this.timeZone = timeZone;
		return this;
	}

	/**
	 * The session media type.
	 *
	 * @param mediaType
	 * 	The session media type (e.g. <js>"application/json"</js>).
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public BeanSessionArgs mediaType(MediaType mediaType) {
		this.mediaType = mediaType;
		return this;
	}

	/**
	 * Debug mode.
	 *
	 * @param debug
	 * 	Debug mode flag.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public BeanSessionArgs debug(Boolean debug) {
		this.debug = debug;
		return this;
	}

	@Override /* SessionArgs */
	public BeanSessionArgs properties(ObjectMap properties) {
		super.properties(properties);
		return this;
	}
}
