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

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;

/**
 * Runtime arguments common to all serializer sessions.
 *
 * <p>
 * This object specifies information such as session locale or URI context.
 */
public final class SerializerSessionArgs extends BeanSessionArgs {

	/**
	 * Default serializer session args.
	 */
	public static final SerializerSessionArgs DEFAULT = new SerializerSessionArgs();

	Method javaMethod;
	UriContext uriContext;
	Boolean useWhitespace;

	/**
	 * Constructor
	 */
	public SerializerSessionArgs() {}

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
	 * @param debug
	 * 	Enable debug mode for this session.
	 * 	<br>Can be <jk>null</jk> to use the debug setting on the bean context..
	 * @param uriContext
	 * 	The URI context.
	 * 	<br>Identifies the current request URI used for resolution of URIs to absolute or root-relative form.
	 * @param useWhitespace
	 * 	Override the use-whitespace flag on the serializer.
	 */
	public SerializerSessionArgs(ObjectMap properties, Method javaMethod, Locale locale, TimeZone timeZone, MediaType mediaType, Boolean debug, UriContext uriContext, Boolean 	useWhitespace) {
		super(properties, locale, timeZone, mediaType, debug);
		this.javaMethod = javaMethod;
		this.uriContext = uriContext;
		this.useWhitespace = useWhitespace;
	}

	/**
	 * The java method that called this serializer, usually the method in a REST servlet.
	 *
	 * @param javaMethod
	 * 	The java method that called this serializer, usually the method in a REST servlet.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public SerializerSessionArgs javaMethod(Method javaMethod) {
		this.javaMethod = javaMethod;
		return this;
	}

	/**
	 * The URI context.
	 *
	 * @param uriContext
	 * 	The URI context.
	 * 	<br>Identifies the current request URI used for resolution of URIs to absolute or root-relative form.
	 * @return This object (for method chaining).
	 */
	public SerializerSessionArgs uriContext(UriContext uriContext) {
		this.uriContext = uriContext;
		return this;
	}

	/**
	 * Use-whitespace flag
	 *
	 * @param useWhitespace
	 * 	The use-whitespace flag.
	 * 	<br>Overrides the use-whitespace flag on the serializer.
	 * @return This object (for method chaining).
	 */
	public SerializerSessionArgs useWhitespace(Boolean useWhitespace) {
		this.useWhitespace = useWhitespace;
		return this;
	}

	@Override /* BeanSessionArgs */
	public SerializerSessionArgs locale(Locale locale) {
		super.locale(locale);
		return this;
	}

	@Override /* BeanSessionArgs */
	public SerializerSessionArgs timeZone(TimeZone timeZone) {
		super.timeZone(timeZone);
		return this;
	}

	@Override /* BeanSessionArgs */
	public SerializerSessionArgs mediaType(MediaType mediaType) {
		super.mediaType(mediaType);
		return this;
	}

	@Override /* BeanSessionArgs */
	public SerializerSessionArgs debug(Boolean debug) {
		super.debug(debug);
		return this;
	}

	@Override /* SessionArgs */
	public SerializerSessionArgs properties(ObjectMap properties) {
		super.properties(properties);
		return this;
	}
}
