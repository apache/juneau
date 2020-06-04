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

import static org.apache.juneau.http.response.Ok.*;

import org.apache.juneau.http.annotation.*;

/**
 * Represents an <c>HTTP 200 OK</c> response.
 *
 * <p>
 * Standard response for successful HTTP requests. The actual response will depend on the request method used.
 * In a GET request, the response will contain an entity corresponding to the requested resource.
 * In a POST request, the response will contain an entity describing or containing the result of the action.
 */
@Response(code=CODE, description=MESSAGE)
public class Ok extends HttpResponse {

	/** HTTP status code */
	public static final int CODE = 200;

	/** Default message */
	public static final String MESSAGE = "OK";

	/** Reusable instance. */
	public static final Ok INSTANCE = new Ok();
	/** Reusable instance. */
	public static final Ok OK = new Ok();

	/**
	 * Constructor using HTTP-standard message.
	 */
	public Ok() {
		this(MESSAGE);
	}

	/**
	 * Constructor using custom message.
	 * @param message Message to send as the response.
	 */
	public Ok(String message) {
		super(message);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Fluent setters.
	//------------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - HttpResponse */
	public Ok header(String name, Object val) {
		super.header(name, val);
		return this;
	}

	// </FluentSetters>
}