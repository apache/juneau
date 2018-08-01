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
 * Represents an <code>HTTP 307 Temporary Redirect</code> response.
 *
 * <p>
 * In this case, the request should be repeated with another URI; however, future requests should still use the original URI.
 * In contrast to how 302 was historically implemented, the request method is not allowed to be changed when reissuing the original request.
 * For example, a POST request should be repeated using another POST request.
 */
@Response(code=307, example="'Temporary Redirect'")
public class TemporaryRedirect extends HttpResponse {

	/** Reusable instance. */
	public static final TemporaryRedirect INSTANCE = new TemporaryRedirect();

	private final URI location;

	/**
	 * Constructor using HTTP-standard message.
	 */
	public TemporaryRedirect() {
		this("Temporary Redirect", null);
	}

	/**
	 * Constructor using custom message.
	 * @param message Message to send as the response.
	 * @param location <code>Location</code> header value.
	 */
	public TemporaryRedirect(String message, URI location) {
		super(message);
		this.location = location;
	}

	/**
	 * Constructor.
	 * @param location <code>Location</code> header value.
	 */
	public TemporaryRedirect(URI location) {
		this("Temporary Redirect", location);
	}

	/**
	 * @return <code>Location</code> header value.
	 */
	@ResponseHeader(name="Location")
	public URI getLocation() {
		return location;
	}
}