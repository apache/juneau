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

import static org.apache.juneau.http.response.SeeOther.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.net.*;
import java.text.*;

import org.apache.juneau.annotation.BeanIgnore;
import org.apache.juneau.http.annotation.*;

/**
 * Represents an <c>HTTP 303 See Other</c> response.
 *
 * <p>
 * The response to the request can be found under another URI using the GET method.
 * When received in response to a POST (or PUT/DELETE), the client should presume that the server has received the data and should issue a new GET request to the given URI.
 */
@Response(code=CODE, description=MESSAGE)
@BeanIgnore
public class SeeOther extends HttpResponse {

	/** HTTP status code */
	public static final int CODE = 303;

	/** Default message */
	public static final String MESSAGE = "See Other";

	/** Reusable instance. */
	public static final SeeOther INSTANCE = new SeeOther();

	private final URI location;

	/**
	 * Constructor using HTTP-standard message.
	 */
	public SeeOther() {
		this(MESSAGE, null);
	}

	/**
	 * Constructor with no redirect.
	 * <p>
	 * Used for end-to-end interfaces.
	 *
	 * @param message Message to send as the response.
	 */
	public SeeOther(String message) {
		super(message);
		this.location = null;
	}

	/**
	 * Constructor using custom message.
	 *
	 * @param message Message to send as the response.
	 * @param location <c>Location</c> header value.
	 */
	public SeeOther(String message, URI location) {
		super(message);
		this.location = location;
	}

	/**
	 * Constructor.
	 *
	 * @param message Message to send as the response.
	 * @param uri URI containing {@link MessageFormat}-style arguments.
	 * @param uriArgs {@link MessageFormat}-style arguments.
	 */
	public SeeOther(String message, String uri, Object uriArgs) {
		this(message, toURI(format(uri.toString(), uriArgs)));
	}

	/**
	 * Constructor.
	 *
	 * @param location <c>Location</c> header value.
	 */
	public SeeOther(URI location) {
		this(MESSAGE, location);
	}

	/**
	 * Constructor.
	 *
	 * @param uri URI containing {@link MessageFormat}-style arguments.
	 * @param uriArgs {@link MessageFormat}-style arguments.
	 */
	public SeeOther(String uri, Object uriArgs) {
		this(toURI(format(uri.toString(), uriArgs)));
	}

	/**
	 * @return <c>Location</c> header value.
	 */
	@ResponseHeader(name="Location", description="Other location.")
	public URI getLocation() {
		return location;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Fluent setters.
	//------------------------------------------------------------------------------------------------------------------

	// <CONFIGURATION-PROPERTIES>

	@Override /* GENERATED - HttpResponse */
	public SeeOther header(String name, Object val) {
		super.header(name, val);
		return this;
	}

	// </CONFIGURATION-PROPERTIES>
}