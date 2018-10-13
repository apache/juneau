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
package org.apache.juneau.parser;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.httppart.*;

/**
 * Runtime arguments common to all parser sessions.
 */
public final class ParserSessionArgs extends BeanSessionArgs {

	Method javaMethod;
	Object outer;

	/**
	 * Default parser session args.
	 */
	public static final ParserSessionArgs DEFAULT = new ParserSessionArgs();

	/**
	 * Constructor
	 */
	public ParserSessionArgs() {}

	/**
	 * Constructor.
	 *
	 * @param properties
	 * 	Session-level properties.
	 * 	<br>These override context-level properties.
	 * 	<br>Can be <jk>null</jk>.
	 * @param javaMethod
	 * 	The java method that called this serializer, usually the method in a REST servlet.
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
	 * @param outer
	 * 	The outer object for instantiating top-level non-static inner classes.
	 */
	public ParserSessionArgs(ObjectMap properties, Method javaMethod, Locale locale, TimeZone timeZone, MediaType mediaType, HttpPartSchema schema, Boolean debug, Object outer) {
		super(properties, locale, timeZone, mediaType, schema, debug);
		this.javaMethod = javaMethod;
		this.outer = outer;
	}


	/**
	 * The java method that called this serializer, usually the method in a REST servlet.
	 *
	 * @param javaMethod
	 * 	The java method that called this serializer, usually the method in a REST servlet.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public ParserSessionArgs javaMethod(Method javaMethod) {
		this.javaMethod = javaMethod;
		return this;
	}

	/**
	 * 	The outer object for instantiating top-level non-static inner classes.
	 *
	 * @param outer
	 * 	The outer object for instantiating top-level non-static inner classes.
	 * @return This object (for method chaining).
	 */
	public ParserSessionArgs outer(Object outer) {
		this.outer = outer;
		return this;
	}

	@Override /* BeanSessionArgs */
	public ParserSessionArgs locale(Locale locale) {
		super.locale(locale);
		return this;
	}

	@Override /* BeanSessionArgs */
	public ParserSessionArgs timeZone(TimeZone timeZone) {
		super.timeZone(timeZone);
		return this;
	}

	@Override /* BeanSessionArgs */
	public ParserSessionArgs mediaType(MediaType mediaType) {
		super.mediaType(mediaType);
		return this;
	}

	@Override /* SessionArgs */
	public ParserSessionArgs properties(ObjectMap properties) {
		super.properties(properties);
		return this;
	}

	/**
	 * @deprecated Use {@link #ParserSessionArgs(ObjectMap, Method, Locale, TimeZone, MediaType, HttpPartSchema, Boolean, Object)}
	 */
	@SuppressWarnings("javadoc")
	@Deprecated
	public ParserSessionArgs(ObjectMap properties, Method javaMethod, Locale locale, TimeZone timeZone, MediaType mediaType, Object outer) {
		this(properties, javaMethod, locale, timeZone, mediaType, null, null, outer);
	}
}