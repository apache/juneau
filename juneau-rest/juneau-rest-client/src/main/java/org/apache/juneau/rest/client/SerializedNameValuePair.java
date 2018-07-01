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
package org.apache.juneau.rest.client;

import org.apache.http.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.httppart.oapi.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.urlencoding.*;

/**
 * Subclass of {@link NameValuePair} for serializing POJOs as URL-encoded form post entries using the
 * {@link UrlEncodingSerializer class}.
 * 
 * <h5 class='section'>Example:</h5>
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
	private HttpPartSerializer serializer;
	private HttpPartSchema schema;

	/**
	 * Constructor.
	 * 
	 * @param name The parameter name.
	 * @param value The POJO to serialize to the parameter value.
	 * @param serializer
	 * 	The serializer to use for serializing the value to a string value.
	 * @param schema 
	 * 	The schema object that defines the format of the output.
	 * 	<br>If <jk>null</jk>, defaults to the schema defined on the serializer.
	 * 	<br>If that's also <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.  
	 * 	<br>Ignored if the part serializer is not a subclass of {@link OapiPartSerializer}.
	 */
	public SerializedNameValuePair(String name, Object value, HttpPartSerializer serializer, HttpPartSchema schema) {
		this.name = name;
		this.value = value;
		this.serializer = serializer;
		this.schema = schema;
	}

	@Override /* NameValuePair */
	public String getName() {
		return name;
	}

	@Override /* NameValuePair */
	public String getValue() {
		try {
			return serializer.serialize(HttpPartType.FORMDATA, schema, value);
		} catch (SerializeException e) {
			throw new RuntimeException(e);
		}
	}
}
