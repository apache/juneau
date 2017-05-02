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
package org.apache.juneau.urlencoding;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.json.*;
import org.apache.juneau.uon.*;

/**
 * Session object that lives for the duration of a single use of {@link UrlEncodingSerializer}.
 * <p>
 * This class is NOT thread safe.  It is meant to be discarded after one-time use.
 */
public class UrlEncodingSerializerSession extends UonSerializerSession {

	private final boolean expandedParams, plainTextParams;

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param ctx The context creating this session object.
	 * The context contains all the configuration settings for this object.
	 * @param encode Overrides the {@link UonSerializerContext#UON_encodeChars} setting.
	 * @param output The output object.  See {@link JsonSerializerSession#getWriter()} for valid class types.
	 * @param op The override properties.
	 * These override any context properties defined in the context.
	 * @param javaMethod The java method that called this serializer, usually the method in a REST servlet.
	 * @param locale The session locale.
	 * If <jk>null</jk>, then the locale defined on the context is used.
	 * @param timeZone The session timezone.
	 * If <jk>null</jk>, then the timezone defined on the context is used.
	 * @param mediaType The session media type (e.g. <js>"application/json"</js>).
	 */
	public UrlEncodingSerializerSession(UrlEncodingSerializerContext ctx, Boolean encode, ObjectMap op, Object output, Method javaMethod, Locale locale, TimeZone timeZone, MediaType mediaType) {
		super(ctx, encode, op, output, javaMethod, locale, timeZone, mediaType);
		if (op == null || op.isEmpty()) {
			expandedParams = ctx.expandedParams;
			plainTextParams = ctx.plainTextParams;
		} else {
			expandedParams = op.getBoolean(UrlEncodingContext.URLENC_expandedParams, false);
			plainTextParams = op.getString(UrlEncodingSerializerContext.URLENC_paramFormat, "UON").equals("PLAINTEXT");
		}
	}

	/**
	 * Returns <jk>true</jk> if the specified bean property should be expanded as multiple key-value pairs.
	 *
	 * @param pMeta The metadata on the bean property.
	 * @return <jk>true</jk> if the specified bean property should be expanded as multiple key-value pairs.
	 */
	public final boolean shouldUseExpandedParams(BeanPropertyMeta pMeta) {
		ClassMeta<?> cm = pMeta.getClassMeta();
		if (cm.isCollectionOrArray()) {
			if (expandedParams)
				return true;
			if (pMeta.getBeanMeta().getClassMeta().getExtendedMeta(UrlEncodingClassMeta.class).isExpandedParams())
				return true;
		}
		return false;
	}

	/**
	 * Returns <jk>true</jk> if the specified value should be represented as an expanded parameter list.
	 *
	 * @param value The value to check.
	 * @return <jk>true</jk> if the specified value should be represented as an expanded parameter list.
	 */
	public final boolean shouldUseExpandedParams(Object value) {
		if (value == null || ! expandedParams)
			return false;
		ClassMeta<?> cm = getClassMetaForObject(value).getSerializedClassMeta();
		if (cm.isCollectionOrArray()) {
			if (expandedParams)
				return true;
		}
		return false;
	}

	/**
	 * Returns <jk>true</jk> if the {@link UrlEncodingSerializerContext#URLENC_paramFormat} is <js>"PLAINTEXT"</js>.
	 * @return <jk>true</jk> if the {@link UrlEncodingSerializerContext#URLENC_paramFormat} is <js>"PLAINTEXT"</js>.
	 */
	protected boolean plainTextParams() {
		return plainTextParams;
	}
}
