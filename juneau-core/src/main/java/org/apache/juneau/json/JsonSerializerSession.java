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
package org.apache.juneau.json;

import static org.apache.juneau.json.JsonSerializerContext.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.serializer.*;

/**
 * Session object that lives for the duration of a single use of {@link JsonSerializer}.
 * <p>
 * This class is NOT thread safe.  It is meant to be discarded after one-time use.
 */
public final class JsonSerializerSession extends SerializerSession {

	private final boolean
		simpleMode,
		escapeSolidus,
		addBeanTypeProperties;

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param ctx The context creating this session object.
	 * The context contains all the configuration settings for this object.
	 * @param output The output object.  See {@link JsonSerializerSession#getWriter()} for valid class types.
	 * @param op The override properties.
	 * These override any context properties defined in the context.
	 * @param javaMethod The java method that called this parser, usually the method in a REST servlet.
	 * @param locale The session locale.
	 * If <jk>null</jk>, then the locale defined on the context is used.
	 * @param timeZone The session timezone.
	 * If <jk>null</jk>, then the timezone defined on the context is used.
	 * @param mediaType The session media type (e.g. <js>"application/json"</js>).
	 */
	protected JsonSerializerSession(JsonSerializerContext ctx, ObjectMap op, Object output, Method javaMethod, Locale locale, TimeZone timeZone, MediaType mediaType) {
		super(ctx, op, output, javaMethod, locale, timeZone, mediaType);
		if (op == null || op.isEmpty()) {
			simpleMode = ctx.simpleMode;
			escapeSolidus = ctx.escapeSolidus;
			addBeanTypeProperties = ctx.addBeanTypeProperties;
		} else {
			simpleMode = op.getBoolean(JSON_simpleMode, ctx.simpleMode);
			escapeSolidus = op.getBoolean(JSON_escapeSolidus, ctx.escapeSolidus);
			addBeanTypeProperties = op.getBoolean(JSON_addBeanTypeProperties, ctx.addBeanTypeProperties);
		}
	}

	/**
	 * Returns the {@link JsonSerializerContext#JSON_simpleMode} setting value for this session.
	 *
	 * @return The {@link JsonSerializerContext#JSON_simpleMode} setting value for this session.
	 */
	public final boolean isSimpleMode() {
		return simpleMode;
	}

	/**
	 * Returns the {@link JsonSerializerContext#JSON_escapeSolidus} setting value for this session.
	 *
	 * @return The {@link JsonSerializerContext#JSON_escapeSolidus} setting value for this session.
	 */
	public final boolean isEscapeSolidus() {
		return escapeSolidus;
	}

	/**
	 * Returns the {@link JsonSerializerContext#JSON_addBeanTypeProperties} setting value for this session.
	 *
	 * @return The {@link JsonSerializerContext#JSON_addBeanTypeProperties} setting value for this session.
	 */
	@Override /* SerializerSession */
	public final boolean isAddBeanTypeProperties() {
		return addBeanTypeProperties;
	}

	@Override /* ParserSession */
	public JsonWriter getWriter() throws Exception {
		Object output = getOutput();
		if (output instanceof JsonWriter)
			return (JsonWriter)output;
		return new JsonWriter(super.getWriter(), isUseWhitespace(), isEscapeSolidus(), getQuoteChar(), isSimpleMode(), isTrimStrings(), getRelativeUriBase(), getAbsolutePathUriBase());
	}
}
