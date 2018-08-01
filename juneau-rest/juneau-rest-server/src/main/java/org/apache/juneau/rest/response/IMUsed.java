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

import org.apache.juneau.http.annotation.*;

/**
 * Represents an <code>HTTP 226 IM Used</code> response.
 *
 * <p>
 * The server has fulfilled a request for the resource, and the response is a representation of the result of one or more instance-manipulations applied to the current instance.
 */
@Response(code=226, example="'IM Used'")
public class IMUsed extends HttpResponse {

	/** Reusable instance. */
	public static final IMUsed INSTANCE = new IMUsed();

	/**
	 * Constructor using HTTP-standard message.
	 */
	public IMUsed() {
		this("IM Used");
	}

	/**
	 * Constructor using custom message.
	 * @param message Message to send as the response.
	 */
	public IMUsed(String message) {
		super(message);
	}
}