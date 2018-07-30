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
 * Represents an <code>HTTP 303 See Other</code> response.
 *
 * <p>
 * The response to the request can be found under another URI using the GET method.
 * When received in response to a POST (or PUT/DELETE), the client should presume that the server has received the data and should issue a new GET request to the given URI.
 */
@Response(code=303, example="'See Other'")
public class SeeOther {

	/** Reusable instance. */
	public static final SeeOther INSTANCE = new SeeOther();

	private final URI location;

	/**
	 * Constructor.
	 */
	public SeeOther() {
		this((URI)null);
	}

	/**
	 * Constructor.
	 *
	 * @param location <code>Location</code> header value.
	 */
	public SeeOther(String location) {
		this.location = URI.create(location);
	}

	/**
	 * Constructor.
	 *
	 * @param location <code>Location</code> header value.
	 */
	public SeeOther(URI location) {
		this.location = location;
	}

	@Override /* Object */
	public String toString() {
		return "See Other";
	}

	/**
	 * @return <code>Location</code> header value.
	 */
	@Header(name="Location")
	public URI getLocation() {
		return location;
	}
}