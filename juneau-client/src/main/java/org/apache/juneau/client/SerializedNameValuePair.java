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
package org.apache.juneau.client;

import static org.apache.juneau.urlencoding.UonSerializerContext.*;

import java.io.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.urlencoding.*;

/**
 * Subclass of {@link NameValuePair} for serializing POJOs as URL-encoded form post entries
 * 	using the {@link UrlEncodingSerializer class}.
 *
 * <h6 class='topic'>Example:</h6>
 * <p class='bcode'>
 * 	NameValuePairs params = <jk>new</jk> NameValuePairs()
 * 		.append(<jk>new</jk> SerializedNameValuePair(<js>"myPojo"</js>, pojo, UrlEncodingSerializer.<jsf>DEFAULT_SIMPLE</jsf>))
 * 		.append(<jk>new</jk> BasicNameValuePair(<js>"someOtherParam"</js>, <js>"foobar"</js>));
 * 	request.setEntity(<jk>new</jk> UrlEncodedFormEntity(params));
 * </p>
 */
public final class SerializedNameValuePair implements NameValuePair {
	private String name;
	private Object value;
	private UrlEncodingSerializer serializer;

	// We must be sure to disable character encoding since it's done in the http client layer.
	private static final ObjectMap op = new ObjectMap().append(UON_encodeChars, false);

	/**
	 * Constructor.
	 *
	 * @param name The parameter name.
	 * @param value The POJO to serialize to the parameter value.
	 * @param serializer The serializer to use to convert the value to a string.
	 */
	public SerializedNameValuePair(String name, Object value, UrlEncodingSerializer serializer) {
		this.name = name;
		this.value = value;
		this.serializer = serializer;
	}

	@Override /* NameValuePair */
	public String getName() {
		if (name != null && name.length() > 0) {
			char c = name.charAt(0);
			if (c == '$' || c == '(') {
				try {
					UonSerializerSession s = serializer.createSession(new StringWriter(), op, null, null, null);
					serializer.serialize(s, name);
					return s.getWriter().toString();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
		return name;
	}

	@Override /* NameValuePair */
	public String getValue() {
		try {
			UonSerializerSession s = serializer.createSession(new StringWriter(), op, null, null, null);
			serializer.serialize(s, value);
			return s.getWriter().toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
