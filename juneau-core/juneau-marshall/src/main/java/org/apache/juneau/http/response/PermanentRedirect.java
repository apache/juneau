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
package org.apache.juneau.http.response;

import static org.apache.juneau.http.response.PermanentRedirect.*;

import java.net.*;

import org.apache.juneau.annotation.BeanIgnore;
import org.apache.juneau.http.annotation.*;

/**
 * Represents an <c>HTTP 308 Permanent Redirect</c> response.
 *
 * <p>
 * The request and all future requests should be repeated using another URI. 307 and 308 parallel the behaviors of 302 and 301, but do not allow the HTTP method to change.
 * So, for example, submitting a form to a permanently redirected resource may continue smoothly.
 */
@Response(code=CODE, description=MESSAGE)
@BeanIgnore
public class PermanentRedirect extends HttpResponse {

	/** HTTP status code */
	public static final int CODE = 308;

	/** Default message */
	public static final String MESSAGE = "Permanent Redirect";

	/** Reusable instance. */
	public static final PermanentRedirect INSTANCE = new PermanentRedirect();

	private final URI location;

	/**
	 * Constructor using HTTP-standard message.
	 */
	public PermanentRedirect() {
		this(MESSAGE, null);
	}

	/**
	 * Constructor with no redirect.
	 * <p>
	 * Used for end-to-end interfaces.
	 *
	 * @param message Message to send as the response.
	 */
	public PermanentRedirect(String message) {
		super(message);
		this.location = null;
	}

	/**
	 * Constructor using custom message.
	 * @param message Message to send as the response.
	 * @param location <c>Location</c> header value.
	 */
	public PermanentRedirect(String message, URI location) {
		super(message);
		this.location = location;
	}

	/**
	 * Constructor.
	 * @param location <c>Location</c> header value.
	 */
	public PermanentRedirect(URI location) {
		this(MESSAGE, location);
	}

	/**
	 * @return <c>Location</c> header value.
	 */
	@ResponseHeader(name="Location", description="New location of resource.")
	public URI getLocation() {
		return location;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Fluent setters.
	//------------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - HttpResponse */
	public PermanentRedirect header(String name, Object val) {
		super.header(name, val);
		return this;
	}

	// </FluentSetters>
}