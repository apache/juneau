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
package org.apache.juneau.http.part;

import static org.apache.juneau.internal.StringUtils.*;
import static java.util.Optional.*;

import java.net.*;
import java.util.*;
import java.util.function.*;

import org.apache.http.*;

/**
 * A {@link NameValuePair} that consists of a single URL value.
 */
public class BasicNamedUri extends BasicPart {

	/**
	 * Convenience creator.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> then parsed.
	 * 	</ul>
	 * @return A new {@link BasicNamedUri} object, or <jk>null</jk> if the name or value is <jk>null</jk>.
	 */
	public static BasicNamedUri of(String name, Object value) {
		if (isEmpty(name) || value == null)
			return null;
		return new BasicNamedUri(name, value);
	}

	/**
	 * Convenience creator using supplier.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> then parsed.
	 * 	</ul>
	 * @return A new {@link BasicNamedUri} object, or <jk>null</jk> if the name or value is <jk>null</jk>.
	 */
	public static BasicNamedUri of(String name, Supplier<?> value) {
		if (isEmpty(name) || value == null)
			return null;
		return new BasicNamedUri(name, value);
	}

	private URI parsed;

	/**
	 * Constructor
	 *
	 * @param name The header name.
	 * @param value
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> then parsed.
	 * 		<li>A {@link Supplier} of anything on this list.
	 * 	</ul>
	 */
	public BasicNamedUri(String name, Object value) {
		super(name, value);
		if (! isSupplier(value))
			parsed = getParsedValue();
	}

	/**
	 * Returns this header as a {@link URI}.
	 *
	 * @return This header as a {@link URI}, or {@link Optional#empty()} if the value is <jk>null</jk>
	 */
	public Optional<URI> asURI() {
		return ofNullable(getParsedValue());
	}

	private URI getParsedValue() {
		if (parsed != null)
			return parsed;
		Object o = getRawValue();
		if (o == null)
			return null;
		String s = o.toString();
		if (isEmpty(s))
			return null;
		return URI.create(s);
	}
}
