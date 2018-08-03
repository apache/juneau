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

import static org.apache.juneau.rest.response.EarlyHints.*;

import org.apache.juneau.http.annotation.*;

/**
 * Represents an <code>HTTP 103 Early Hints</code> response.
 *
 * <p>
 * Used to return some response headers before final HTTP message.
 */
@Response(code=CODE, description=MESSAGE)
public class EarlyHints extends HttpResponse {

	/** HTTP status code */
	public static final int CODE = 103;

	/** Default message */
	public static final String MESSAGE = "Early Hints";

	/** Reusable instance. */
	public static final EarlyHints INSTANCE = new EarlyHints();

	/**
	 * Constructor using HTTP-standard message.
	 */
	public EarlyHints() {
		this(MESSAGE);
	}

	/**
	 * Constructor using custom message.
	 * @param message Message to send as the response.
	 */
	public EarlyHints(String message) {
		super(message);
	}
}