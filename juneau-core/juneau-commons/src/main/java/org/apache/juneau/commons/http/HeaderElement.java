/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.commons.http;

/**
 * Represents a parsed element from an HTTP header value.
 *
 * <p>
 * An element consists of a name and zero or more parameters, as defined by RFC 2616.
 * For example, in the header value <js>"text/html;charset=UTF-8;q=0.9"</js>, the element
 * name is <js>"text/html"</js> and the parameters are <js>"charset=UTF-8"</js> and <js>"q=0.9"</js>.
 */
public class HeaderElement {

	private final String name;
	private final NameValuePair[] parameters;

	/**
	 * Constructor.
	 *
	 * @param name The element name.
	 * @param parameters The element parameters.
	 */
	public HeaderElement(String name, NameValuePair... parameters) {
		this.name = name;
		this.parameters = parameters;
	}

	/**
	 * Returns the element name.
	 *
	 * @return The element name, never <jk>null</jk>.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the element parameters.
	 *
	 * @return The parameters array, never <jk>null</jk>.
	 */
	public NameValuePair[] getParameters() {
		return parameters;
	}
}
