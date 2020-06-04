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

import static org.apache.juneau.http.response.IMUsed.*;

import org.apache.juneau.http.annotation.*;

/**
 * Represents an <c>HTTP 226 IM Used</c> response.
 *
 * <p>
 * The server has fulfilled a request for the resource, and the response is a representation of the result of one or more instance-manipulations applied to the current instance.
 */
@Response(code=CODE, description=MESSAGE)
public class IMUsed extends HttpResponse {

	/** HTTP status code */
	public static final int CODE = 226;

	/** Default message */
	public static final String MESSAGE = "IM Used";

	/** Reusable instance. */
	public static final IMUsed INSTANCE = new IMUsed();

	/**
	 * Constructor using HTTP-standard message.
	 */
	public IMUsed() {
		this(MESSAGE);
	}

	/**
	 * Constructor using custom message.
	 * @param message Message to send as the response.
	 */
	public IMUsed(String message) {
		super(message);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Fluent setters.
	//------------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - HttpResponse */
	public IMUsed header(String name, Object val) {
		super.header(name, val);
		return this;
	}

	// </FluentSetters>
}