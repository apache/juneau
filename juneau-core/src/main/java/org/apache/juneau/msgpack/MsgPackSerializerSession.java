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
package org.apache.juneau.msgpack;

import static org.apache.juneau.msgpack.MsgPackSerializerContext.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;

/**
 * Session object that lives for the duration of a single use of {@link MsgPackSerializer}.
 * <p>
 * This class is NOT thread safe.  It is meant to be discarded after one-time use.
 */
public final class MsgPackSerializerSession extends SerializerSession {

	private final boolean
		addBeanTypeProperties;

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param ctx The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
	 * @param output The output object.  See {@link JsonSerializerSession#getOutputStream()} for valid class types.
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
	protected MsgPackSerializerSession(MsgPackSerializerContext ctx, ObjectMap op, Object output, Method javaMethod, Locale locale, TimeZone timeZone, MediaType mediaType, UriContext uriContext) {
		super(ctx, op, output, javaMethod, locale, timeZone, mediaType, uriContext);
		if (op == null || op.isEmpty()) {
			addBeanTypeProperties = ctx.addBeanTypeProperties;
		} else {
			addBeanTypeProperties = op.getBoolean(MSGPACK_addBeanTypeProperties, ctx.addBeanTypeProperties);
		}
	}

	/**
	 * Returns the {@link MsgPackSerializerContext#MSGPACK_addBeanTypeProperties} setting value for this session.
	 *
	 * @return The {@link MsgPackSerializerContext#MSGPACK_addBeanTypeProperties} setting value for this session.
	 */
	@Override /* SerializerSession */
	public final boolean isAddBeanTypeProperties() {
		return addBeanTypeProperties;
	}

	@Override /*SerializerSession */
	public MsgPackOutputStream getOutputStream() throws Exception {
		Object output = getOutput();
		if (output instanceof MsgPackOutputStream)
			return (MsgPackOutputStream)output;
		return new MsgPackOutputStream(super.getOutputStream());
	}
}
