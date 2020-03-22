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

import static org.apache.juneau.http.response.Continue.*;

import org.apache.juneau.http.annotation.*;

/**
 * Represents an <c>HTTP 100 Continue</c> response.
 *
 * <p>
 * The server has received the request headers and the client should proceed to send the request body (in the case of a request for which a body needs to be sent; for example, a POST request).
 * Sending a large request body to a server after a request has been rejected for inappropriate headers would be inefficient.
 * To have a server check the request's headers, a client must send Expect: 100-continue as a header in its initial request and receive a 100 Continue status code in response before sending the body.
 * If the client receives an error code such as 403 (Forbidden) or 405 (Method Not Allowed) then it shouldn't send the request's body.
 * The response 417 Expectation Failed indicates that the request should be repeated without the Expect header as it indicates that the server doesn't support expectations (this is the case, for example, of HTTP/1.0 servers).
 */
@Response(code=CODE, description=MESSAGE)
public class Continue extends HttpResponse {

	/** HTTP status code */
	public static final int CODE = 100;

	/** Default message */
	public static final String MESSAGE = "Continue";

	/** Reusable instance.*/
	public static final Continue INSTANCE = new Continue();

	/**
	 * Constructor using HTTP-standard message.
	 */
	public Continue() {
		this(MESSAGE);
	}

	/**
	 * Constructor using custom message.
	 * @param message Message to send as the response.
	 */
	public Continue(String message) {
		super(message);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Fluent setters.
	//------------------------------------------------------------------------------------------------------------------

	// <CONFIGURATION-PROPERTIES>

	@Override /* GENERATED - HttpResponse */
	public Continue header(String name, Object val) {
		super.header(name, val);
		return this;
	}

	// </CONFIGURATION-PROPERTIES>
}