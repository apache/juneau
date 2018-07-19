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

import static org.apache.juneau.internal.StringUtils.*;

import java.util.*;

import org.apache.http.*;
import org.apache.http.client.entity.*;
import org.apache.http.message.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.urlencoding.*;

/**
 * Convenience class for constructing instances of <code>List&lt;NameValuePair&gt;</code> for the
 * {@link UrlEncodedFormEntity} class.
 *
 * <p>
 * Instances of this method can be passed directly to the {@link RestClient#doPost(Object, Object)} method or
 * {@link RestCall#body(Object)} methods to perform URL-encoded form posts.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	NameValuePairs params = <jk>new</jk> NameValuePairs()
 * 		.append(<js>"j_username"</js>, user)
 * 		.append(<js>"j_password"</js>, pw);
 * 	restClient.doPost(url, params).run();
 * </p>
 */
public final class NameValuePairs extends LinkedList<NameValuePair> {

	private static final long serialVersionUID = 1L;

	/**
	 * Appends the specified pair to the end of this list.
	 *
	 * @param pair The pair to append to this list.
	 * @return This object (for method chaining).
	 */
	public NameValuePairs append(NameValuePair pair) {
		super.add(pair);
		return this;
	}

	/**
	 * Appends the specified name/value pair to the end of this list.
	 *
	 * <p>
	 * The value is simply converted to a string using <code>toString()</code>, or <js>"null"</js> if <jk>null</jk>.
	 *
	 * @param name The pair name.
	 * @param value The pair value.
	 * @return This object (for method chaining).
	 */
	public NameValuePairs append(String name, Object value) {
		super.add(new BasicNameValuePair(name, asString(value)));
		return this;
	}

	/**
	 * Appends the specified name/value pair to the end of this list.
	 *
	 * <p>
	 * The value is converted to UON notation using the {@link UrlEncodingSerializer} defined on the client.
	 *
	 * @param name The pair name.
	 * @param value The pair value.
	 * @param serializer
	 * 	The serializer to use for serializing the value to a string value.
	 * @param schema
	 * 	The schema object that defines the format of the output.
	 * 	<br>If <jk>null</jk>, defaults to the schema defined on the parser.
	 * 	<br>If that's also <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 	<br>Ignored if the serializer parser is not a subclass of {@link OpenApiPartSerializer}.
	 * @return This object (for method chaining).
	 */
	public NameValuePairs append(String name, Object value, HttpPartSerializer serializer, HttpPartSchema schema) {
		super.add(new SerializedNameValuePair(name, value, serializer, schema));
		return this;
	}
}
