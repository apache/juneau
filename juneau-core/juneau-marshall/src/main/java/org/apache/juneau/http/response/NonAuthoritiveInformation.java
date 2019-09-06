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

import static org.apache.juneau.http.response.NonAuthoritiveInformation.*;

import org.apache.juneau.http.annotation.*;

/**
 * Represents an <c>HTTP 203 Non-Authoritative Information</c> response.
 *
 * <p>
 * The server is a transforming proxy (e.g. a Web accelerator) that received a 200 OK from its origin, but is returning a modified version of the origin's response.
 */
@Response(code=CODE, description=MESSAGE)
public class NonAuthoritiveInformation extends HttpResponse {

	/** HTTP status code */
	public static final int CODE = 203;

	/** Default message */
	public static final String MESSAGE = "Non-Authoritative Information";

	/** Reusable instance. */
	public static final NonAuthoritiveInformation INSTANCE = new NonAuthoritiveInformation();

	/**
	 * Constructor using HTTP-standard message.
	 */
	public NonAuthoritiveInformation() {
		this(MESSAGE);
	}

	/**
	 * Constructor using custom message.
	 * @param message Message to send as the response.
	 */
	public NonAuthoritiveInformation(String message) {
		super(message);
	}
}