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

import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.http.header.*;

/**
 * Subclass of {@link NameValuePair} for serializing POJOs as URL-encoded form post entries.
 * 
 * <p>
 * The value is serialized using {@link Object#toString()} at the point of reading.  This allows the value to be modified
 * periodically by overriding the method to return different values.
 */
public class BasicNameValuePair implements NameValuePair, Headerable {
	private String name;
	private Object value;

	/**
	 * Convenience creator.
	 *
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * @return A new {@link BasicNameValuePair} object.
	 */
	public static BasicNameValuePair of(String name, Object value) {
		return new BasicNameValuePair(name, value);
	}

	/**
	 * Convenience creator using supplier.
	 *
	 * <p>
	 * Value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param name The parameter name.
	 * @param value The parameter value supplier.
	 * @return A new {@link BasicNameValuePair} object.
	 */
	public static BasicNameValuePair of(String name, Supplier<?> value) {
		return new BasicNameValuePair(name, value);
	}

	/**
	 * Constructor.
	 *
	 * @param name The parameter name.
	 * @param value The POJO to serialize to the parameter value.
	 */
	public BasicNameValuePair(String name, Object value) {
		this.name = name;
		this.value = value;
	}

	@Override /* Headerable */
	public BasicHeader asHeader() {
		return BasicHeader.of(name, value);
	}

	@Override /* NameValuePair */
	public String getName() {
		return name;
	}

	@Override /* NameValuePair */
	public String getValue() {
		return stringify(unwrap(value));
	}

	private Object unwrap(Object o) {
		if (o instanceof Supplier)
			return ((Supplier<?>)o).get();
		return o;
	}
}
