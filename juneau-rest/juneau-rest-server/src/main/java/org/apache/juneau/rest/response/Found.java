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
package org.apache.juneau.rest.response;

import java.net.*;

import org.apache.juneau.http.annotation.*;

/**
 * Represents an <code>HTTP 302 Found</code> response.
 *
 * <p>
 * Tells the client to look at (browse to) another url. 302 has been superseded by 303 and 307.
 * This is an example of industry practice contradicting the standard.
 * The HTTP/1.0 specification (RFC 1945) required the client to perform a temporary redirect (the original describing phrase was "Moved Temporarily"), but popular browsers implemented 302 with the functionality of a 303 See Other.
 * Therefore, HTTP/1.1 added status codes 303 and 307 to distinguish between the two behaviours.
 * However, some Web applications and frameworks use the 302 status code as if it were the 303.
 */
@Response(code=302, example="'Found'")
public class Found {

	/** Reusable instance. */
	public static final Found INSTANCE = new Found();

	private final URI location;

	/**
	 * Constructor.
	 */
	public Found() {
		this(null);
	}

	/**
	 * Constructor.
	 *
	 * @param location <code>Location</code> header value.
	 */
	public Found(URI location) {
		this.location = location;
	}

	@Override /* Object */
	public String toString() {
		return "Found";
	}

	/**
	 * @return <code>Location</code> header value.
	 */
	@ResponseHeader(name="Location")
	public URI getLocation() {
		return location;
	}
}