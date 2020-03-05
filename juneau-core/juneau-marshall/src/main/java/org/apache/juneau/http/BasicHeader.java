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

/**
 * Superclass of all HTTP headers defined in this package.
 */
public class BasicHeader implements HttpHeader {

	private final String name;
	final Object value;

	/**
	 * Constructor.
	 *
	 * @param name The HTTP header name.
	 * @param value The HTTP header value;
	 */
	public BasicHeader(String name, Object value) {
		this.name = name;
		this.value = value;
	}

	@Override /* HttpHeader */
	public String getName() {
		return name;
	}

	@Override /* HttpHeader */
	public Object getValue() {
		return value;
	}

	/**
	 * Returns the value as a string.
	 *
	 * @return The value as a string, or an empty string if the value is <jk>null</jk>.
	 */
	public String asString() {
		return value == null ? "" : value.toString();
	}

	@Override /* Object */
	public String toString() {
		return asString();
	}
}
