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
package org.apache.juneau.http;

import static org.apache.juneau.internal.StringUtils.*;

import java.util.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.urlencoding.*;

/**
 * Convenience class for constructing instances of <c>List&lt;NameValuePair&gt;</c>.
 *
 * <p>
 * Instances of this method can be passed directly to the <c>RestClient.post(Object, Object)</c> method or
 * <c>RestRequest.body(Object)</c> methods to perform URL-encoded form posts.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	NameValuePairs params = <jk>new</jk> NameValuePairs(<js>"j_username"</js>, user, <js>"j_password"</js>, pw);
 * 	client.post(<jsf>URL</jsf>, params).execute();
 * </p>
 */
public class NameValuePairs extends LinkedList<NameValuePair> {

	private static final long serialVersionUID = 1L;

	//------------------------------------------------------------------------------------------------------------------
	// Constructors
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Empty constructor.
	 */
	public NameValuePairs() {}

	/**
	 * Constructor.
	 *
	 * @param parameters Initial list of parameters.
	 */
	public NameValuePairs(NameValuePair...parameters) {
		Collections.addAll(this, parameters);
	}

	/**
	 * Constructor.
	 *
	 * @param parameters Initial list of parameters.
	 */
	public NameValuePairs(Collection<? extends NameValuePair> parameters) {
		addAll(parameters);
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * Constructs a set of {@link BasicNameValuePair} objects from a list of key/value pairs.
	 *
	 * @param parameters
	 * 	Initial list of parameters.
	 * 	<br>Must be an even number of parameters representing key/value pairs.
	 * @throws RuntimeException If odd number of parameters were specified.
	 */
	public NameValuePairs(Object...parameters) {
		if (parameters.length % 2 != 0)
			throw new BasicRuntimeException("Odd number of parameters passed into NameValuePairs(Object...)");
		for (int i = 0; i < parameters.length; i+=2)
			add(new BasicNameValuePair(stringify(parameters[i]), parameters[i+1]));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Creators
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Creates an empty instance.
	 *
	 * @return A new empty instance.
	 */
	public static NameValuePairs of() {
		return new NameValuePairs();
	}

	/**
	 * Creates an instance initialized with the specified pairs.
	 *
	 * @param pairs The pairs to add to this list.
	 * @return A new instance.
	 */
	public static NameValuePairs of(NameValuePair...pairs) {
		return new NameValuePairs(pairs);
	}

	/**
	 * Creates an instance initialized with the specified pairs.
	 *
	 * @param pairs The pairs to add to this list.
	 * @return A new instance.
	 */
	public static NameValuePairs of(Collection<? extends NameValuePair> pairs) {
		return new NameValuePairs(pairs);
	}

	/**
	 * Creates an instance initialized with the specified pairs.
	 *
	 * @param parameters
	 * 	Initial list of parameters.
	 * 	<br>Must be an even number of parameters representing key/value pairs.
	 * @throws RuntimeException If odd number of parameters were specified.
	 * @return A new instance.
	 */
	public static NameValuePairs of(Object...parameters) {
		return new NameValuePairs(parameters);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Appenders
	//------------------------------------------------------------------------------------------------------------------

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
	 * The pair is added as a {@link BasicNameValuePair}.
	 *
	 * @param name The pair name.
	 * @param value The pair value.
	 * @return This object (for method chaining).
	 */
	public NameValuePairs append(String name, Object value) {
		super.add(new BasicNameValuePair(name, value));
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
	 * @param partType The HTTP part type.
	 * @param serializer
	 * 	The serializer to use for serializing the value to a string value.
	 * @param schema
	 * 	The schema object that defines the format of the output.
	 * 	<br>If <jk>null</jk>, defaults to the schema defined on the parser.
	 * 	<br>If that's also <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 	<br>Only used if serializer is schema-aware (e.g. {@link OpenApiSerializer}).
	 * @param skipIfEmpty If value is a blank string, the value should return as <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public NameValuePairs append(String name, Object value, HttpPartType partType, HttpPartSerializerSession serializer, HttpPartSchema schema, boolean skipIfEmpty) {
		super.add(new SerializedNameValuePair(name, value, partType, serializer, schema, skipIfEmpty));
		return this;
	}

	/**
	 * Returns this list as a URL-encoded custom query.
	 */
	@Override /* Object */
	public String toString() {
		if (isEmpty())
			return null;
		StringBuilder sb = new StringBuilder();
		for (NameValuePair p : this) {
			String v = p.getValue();
			if (v != null) {
				if (sb.length() > 0)
					sb.append("&");
				sb.append(urlEncode(p.getName())).append('=').append(urlEncode(p.getValue()));
			}
		}
		return sb.toString();
	}
}
