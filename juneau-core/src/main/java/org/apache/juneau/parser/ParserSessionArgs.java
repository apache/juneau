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

/**
 * Runtime arguments common to all parser sessions.
 */
public final class ParserSessionArgs extends BeanSessionArgs {

	/**
	 * Default session arguments.
	 */
	protected static final ParserSessionArgs DEFAULT = new ParserSessionArgs(ObjectMap.EMPTY_MAP, null, null, null, null, null);

	final Method javaMethod;
	final Object outer;

	/**
	 * Constructor.
	 *
	 * @param properties
	 * 	Session-level properties.
	 * 	These override context-level properties.
	 * 	Can be <jk>null</jk>.
	 * @param javaMethod
	 * 	The java method that called this serializer, usually the method in a REST servlet.
	 * 	Can be <jk>null</jk>.
	 * @param locale
	 * 	The session locale.
	 * 	If <jk>null</jk>, then the locale defined on the context is used.
	 * @param timeZone
	 * 	The session timezone.
	 * 	If <jk>null</jk>, then the timezone defined on the context is used.
	 * @param mediaType
	 * 	The session media type (e.g. <js>"application/json"</js>).
	 * 	Can be <jk>null</jk>.
	 * @param outer
	 * 	The outer object for instantiating top-level non-static inner classes.
	 */
	public ParserSessionArgs(ObjectMap properties, Method javaMethod, Locale locale, TimeZone timeZone, MediaType mediaType, Object outer) {
		super(properties, locale, timeZone, mediaType);
		this.javaMethod = javaMethod;
		this.outer = outer;
	}
}