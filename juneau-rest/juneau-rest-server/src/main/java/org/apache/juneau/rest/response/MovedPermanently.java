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

import static org.apache.juneau.rest.response.MovedPermanently.*;

import java.net.*;

import org.apache.juneau.annotation.BeanIgnore;
import org.apache.juneau.http.annotation.*;

/**
 * Represents an <code>HTTP 301 Moved Permanently</code> response.
 *
 * <p>
 * This and all future requests should be directed to the given URI.
 */
@Response(code=CODE, description=MESSAGE)
@BeanIgnore
public class MovedPermanently extends HttpResponse {

	/** HTTP status code */
	public static final int CODE = 301;

	/** Default message */
	public static final String MESSAGE = "Moved Permanently";

	/** Reusable instance. */
	public static final MovedPermanently INSTANCE = new MovedPermanently();

	private final URI location;

	/**
	 * Constructor using HTTP-standard message.
	 */
	public MovedPermanently() {
		this(MESSAGE, null);
	}

	/**
	 * Constructor using custom message.
	 * @param message Message to send as the response.
	 * @param location <code>Location</code> header value.
	 */
	public MovedPermanently(String message, URI location) {
		super(message);
		this.location = location;
	}

	/**
	 * Constructor.
	 * @param location <code>Location</code> header value.
	 */
	public MovedPermanently(URI location) {
		this(MESSAGE, location);
	}

	/**
	 * @return <code>Location</code> header value.
	 */
	@ResponseHeader(name="Location", description="New location of resource.")
	public URI getLocation() {
		return location;
	}
}