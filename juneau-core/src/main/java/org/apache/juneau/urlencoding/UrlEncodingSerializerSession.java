/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.urlencoding;

import static org.apache.juneau.urlencoding.UrlEncodingParserContext.*;

import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;

/**
 * Session object that lives for the duration of a single use of {@link UrlEncodingSerializer}.
 * <p>
 * This class is NOT thread safe.  It is meant to be discarded after one-time use.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public class UrlEncodingSerializerSession extends UonSerializerSession {

	private final boolean expandedParams;

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param ctx The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
	 * @param beanContext The bean context being used.
	 * @param output The output object.  See {@link JsonSerializerSession#getWriter()} for valid class types.
	 * @param op The override properties.
	 * 	These override any context properties defined in the context.
	 * @param javaMethod The java method that called this parser, usually the method in a REST servlet.
	 */
	public UrlEncodingSerializerSession(UrlEncodingSerializerContext ctx, BeanContext beanContext, Object output, ObjectMap op, Method javaMethod) {
		super(ctx, beanContext, output, op, javaMethod);
		if (op == null || op.isEmpty()) {
			expandedParams = ctx.expandedParams;
		} else {
			expandedParams = op.getBoolean(URLENC_expandedParams, false);
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
		if (cm.isArray() || cm.isCollection()) {
			if (expandedParams)
				return true;
			if (pMeta.getBeanMeta().getClassMeta().getUrlEncodingMeta().isExpandedParams())
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
		ClassMeta<?> cm = getBeanContext().getClassMetaForObject(value).getTransformedClassMeta();
		if (cm.isArray() || cm.isCollection()) {
			if (expandedParams)
				return true;
		}
		return false;
	}
}
