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

import java.net.*;

import org.apache.juneau.internal.*;

/**
 * Category of headers that consist of a single URL value.
 *
 * <p>
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	Location: http://www.w3.org/pub/WWW/People.html
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@doc RFC2616}
 * </ul>
 */
public class BasicUriHeader extends BasicHeader {

	private static final long serialVersionUID = 1L;

	final String value;

	/**
	 * Constructor.
	 *
	 * @param name The HTTP header name.
	 * @param value The raw header value.
	 */
	public BasicUriHeader(String name, String value) {
		super(name, value);
		this.value = StringUtils.trim(value);
	}

	/**
	 * Returns this header as a {@link URI}.
	 *
	 * @return This header as a {@link URI}.
	 */
	public URI asURI() {
		return value == null ? null : URI.create(toString());
	}

	/**
	 * Returns this header as a simple string value.
	 *
	 * <p>
	 * Functionally equivalent to calling {@link #toString()}.
	 *
	 * @return This header as a simple string.
	 */
	@Override
	public String asString() {
		return value;
	}

	@Override /* Object */
	public String toString() {
		return value == null ? "" : value;
	}
}
