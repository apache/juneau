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

import static org.apache.juneau.http.response.NotModified.*;

import org.apache.juneau.http.annotation.*;

/**
 * Represents an <c>HTTP 304 Not Modified</c> response.
 *
 * <p>
 * Indicates that the resource has not been modified since the version specified by the request headers If-Modified-Since or If-None-Match.
 * In such case, there is no need to retransmit the resource since the client still has a previously-downloaded copy.
 */
@Response(code=CODE, description=MESSAGE)
public class NotModified extends HttpResponse {

	/** HTTP status code */
	public static final int CODE = 304;

	/** Default message */
	public static final String MESSAGE = "Not Modified";

	/** Reusable instance. */
	public static final NotModified INSTANCE = new NotModified();

	/**
	 * Constructor using HTTP-standard message.
	 */
	public NotModified() {
		this(MESSAGE);
	}

	/**
	 * Constructor using custom message.
	 * @param message Message to send as the response.
	 */
	public NotModified(String message) {
		super(message);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Fluent setters.
	//------------------------------------------------------------------------------------------------------------------

	// <CONFIGURATION-PROPERTIES>

	@Override /* GENERATED - HttpResponse */
	public NotModified header(String name, Object val) {
		super.header(name, val);
		return this;
	}

	// </CONFIGURATION-PROPERTIES>
}