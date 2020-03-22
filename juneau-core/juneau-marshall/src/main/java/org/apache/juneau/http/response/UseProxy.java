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

import static org.apache.juneau.http.response.UseProxy.*;

import org.apache.juneau.http.annotation.*;

/**
 * Represents an <c>HTTP 305 Use Proxy</c> response.
 *
 * <p>
 * The requested resource is available only through a proxy, the address for which is provided in the response.
 * Many HTTP clients (such as Mozilla and Internet Explorer) do not correctly handle responses with this status code, primarily for security reasons.
 */
@Response(code=CODE, description=MESSAGE)
public class UseProxy extends HttpResponse {

	/** HTTP status code */
	public static final int CODE = 305;

	/** Default message */
	public static final String MESSAGE = "Use Proxy";

	/** Reusable instance. */
	public static final UseProxy INSTANCE = new UseProxy();

	/**
	 * Constructor using HTTP-standard message.
	 */
	public UseProxy() {
		this(MESSAGE);
	}

	/**
	 * Constructor using custom message.
	 * @param message Message to send as the response.
	 */
	public UseProxy(String message) {
		super(message);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Fluent setters.
	//------------------------------------------------------------------------------------------------------------------

	// <CONFIGURATION-PROPERTIES>

	@Override /* GENERATED - HttpResponse */
	public UseProxy header(String name, Object val) {
		super.header(name, val);
		return this;
	}

	// </CONFIGURATION-PROPERTIES>
}