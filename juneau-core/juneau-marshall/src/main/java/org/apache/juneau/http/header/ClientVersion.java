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
package org.apache.juneau.http.header;

import static org.apache.juneau.http.Constants.*;

import java.util.function.*;

import org.apache.juneau.http.annotation.*;
import org.apache.juneau.internal.*;

/**
 * Represents a parsed <l>Client-Version</l> HTTP request header.
 *
 * <p>
 * Specifies a client-side version number.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	Client-Version: 2.0.1
 * </p>
 *
 * <p>
 * Not part of the RFC2616 specification, but provided to allow for HTTP responses to be tailored to specified
 * known client versions.
 */
@Header("Client-Version")
public class ClientVersion extends BasicStringHeader {

	private static final long serialVersionUID = 1L;

	private static final Cache<String,ClientVersion> CACHE = new Cache<>(NOCACHE, CACHE_MAX_SIZE);

	/**
	 * Returns a parsed and cached header.
	 *
	 * @param value
	 * 	The header value.
	 * @return A cached {@link ClientVersion} object.
	 */
	public static ClientVersion of(String value) {
		if (value == null)
			return null;
		ClientVersion x = CACHE.get(value);
		if (x == null)
			x = CACHE.put(value, new ClientVersion(value));
		return x;
	}

	/**
	 * Convenience creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new {@link ClientVersion} object.
	 */
	public static ClientVersion of(Object value) {
		if (value == null)
			return null;
		return new ClientVersion(value);
	}

	/**
	 * Convenience creator using supplier.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new {@link ClientVersion} object.
	 */
	public static ClientVersion of(Supplier<?> value) {
		if (value == null)
			return null;
		return new ClientVersion(value);
	}

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 		<li>A {@link Supplier} of anything on this list.
	 * 	</ul>
	 */
	public ClientVersion(Object value) {
		super("Client-Version", value);
	}

	/**
	 * Constructor
	 *
	 * @param value
	 * 	The header value.
	 */
	public ClientVersion(String value) {
		this((Object)value);
	}
}
