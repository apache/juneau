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
package org.apache.juneau.uon;

import static org.apache.juneau.msgpack.MsgPackSerializerContext.*;
import static org.apache.juneau.uon.UonSerializerContext.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;

/**
 * Session object that lives for the duration of a single use of {@link UonSerializer}.
 * <p>
 * This class is NOT thread safe.  It is meant to be discarded after one-time use.
 */
public class UonSerializerSession extends SerializerSession {

	private final boolean
		encodeChars,
		addBeanTypeProperties,
		plainTextParams;

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param ctx The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
	 * @param encode Override the {@link UonSerializerContext#UON_encodeChars} setting.
	 * @param output The output object.  See {@link JsonSerializerSession#getWriter()} for valid class types.
	 * @param op The override properties.
	 * 	These override any context properties defined in the context.
	 * @param javaMethod The java method that called this serializer, usually the method in a REST servlet.
	 * @param locale The session locale.
	 * 	If <jk>null</jk>, then the locale defined on the context is used.
	 * @param timeZone The session timezone.
	 * 	If <jk>null</jk>, then the timezone defined on the context is used.
	 * @param mediaType The session media type (e.g. <js>"application/json"</js>).
	 * @param uriContext The URI context.
	 * 	Identifies the current request URI used for resolution of URIs to absolute or root-relative form.
	 */
	protected UonSerializerSession(UonSerializerContext ctx, Boolean encode, ObjectMap op, Object output, Method javaMethod, Locale locale, TimeZone timeZone, MediaType mediaType, UriContext uriContext) {
		super(ctx, op, output, javaMethod, locale, timeZone, mediaType, uriContext);
		if (op == null || op.isEmpty()) {
			encodeChars = encode == null ? ctx.encodeChars : encode;
			addBeanTypeProperties = ctx.addBeanTypeProperties;
			plainTextParams = ctx.plainTextParams;
		} else {
			encodeChars = encode == null ? op.getBoolean(UON_encodeChars, ctx.encodeChars) : encode;
			addBeanTypeProperties = op.getBoolean(MSGPACK_addBeanTypeProperties, ctx.addBeanTypeProperties);
			plainTextParams = op.getString(UonSerializerContext.UON_paramFormat, "UON").equals("PLAINTEXT");
		}
	}

	/**
	 * Returns the {@link UonSerializerContext#UON_encodeChars} setting value for this session.
	 *
	 * @return The {@link UonSerializerContext#UON_encodeChars} setting value for this session.
	 */
	public final boolean isEncodeChars() {
		return encodeChars;
	}

	/**
	 * Returns the {@link UonSerializerContext#UON_addBeanTypeProperties} setting value for this session.
	 *
	 * @return The {@link UonSerializerContext#UON_addBeanTypeProperties} setting value for this session.
	 */
	@Override /* SerializerSession */
	public final boolean isAddBeanTypeProperties() {
		return addBeanTypeProperties;
	}

	/**
	 * Returns <jk>true</jk> if the {@link UonSerializerContext#UON_paramFormat} is <js>"PLAINTEXT"</js>.
	 * @return <jk>true</jk> if the {@link UonSerializerContext#UON_paramFormat} is <js>"PLAINTEXT"</js>.
	 */
	public boolean isPlainTextParams() {
		return plainTextParams;
	}

	@Override /* SerializerSession */
	public final UonWriter getWriter() throws Exception {
		Object output = getOutput();
		if (output instanceof UonWriter)
			return (UonWriter)output;
		return new UonWriter(this, super.getWriter(), isUseWhitespace(), isEncodeChars(), isTrimStrings(), isPlainTextParams(), getUriResolver());
	}
}
