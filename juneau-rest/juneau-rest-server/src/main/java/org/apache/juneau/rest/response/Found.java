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

import static org.apache.juneau.rest.response.Found.*;

import org.apache.juneau.annotation.BeanIgnore;
import org.apache.juneau.http.annotation.*;

/**
 * Represents an <c>HTTP 302 Found</c> response.
 *
 * <p>
 * Tells the client to look at (browse to) another url. 302 has been superseded by 303 and 307.
 * This is an example of industry practice contradicting the standard.
 * The HTTP/1.0 specification (RFC 1945) required the client to perform a temporary redirect (the original describing phrase was "Moved Temporarily"), but popular browsers implemented 302 with the functionality of a 303 See Other.
 * Therefore, HTTP/1.1 added status codes 303 and 307 to distinguish between the two behaviours.
 * However, some Web applications and frameworks use the 302 status code as if it were the 303.
 *
 * @deprecated Use {@link org.apache.juneau.http.response.Found}
 */
@Response(code=CODE, description=MESSAGE)
@BeanIgnore
@Deprecated
public class Found extends HttpResponse {

	/** HTTP status code */
	public static final int CODE = 302;

	/** Default message */
	public static final String MESSAGE = "Found";

	/** Reusable instance. */
	public static final Found INSTANCE = new Found();

	private final URI location;

	/**
	 * Constructor using HTTP-standard message.
	 */
	public Found() {
		this(MESSAGE, null);
	}

	/**
	 * Constructor with no redirect.
	 * <p>
	 * Used for end-to-end interfaces.
	 *
	 * @param message Message to send as the response.
	 */
	public Found(String message) {
		super(message);
		this.location = null;
	}

	/**
	 * Constructor using custom message.
	 * @param message Message to send as the response.
	 * @param location <c>Location</c> header value.
	 */
	public Found(String message, URI location) {
		super(message);
		this.location = location;
	}

	/**
	 * Constructor.
	 * @param location <c>Location</c> header value.
	 */
	public Found(URI location) {
		this(MESSAGE, location);
	}

	/**
	 * @return <c>Location</c> header value.
	 */
	@ResponseHeader(name="Location", description="Location of resource.")
	public URI getLocation() {
		return location;
	}
}