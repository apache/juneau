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

import static org.apache.juneau.http.response.MultiStatus.*;

import org.apache.juneau.http.annotation.*;

/**
 * Represents an <c>HTTP 207 Multi-Status</c> response.
 *
 * <p>
 * The message body that follows is by default an XML message and can contain a number of separate response codes, depending on how many sub-requests were made.
 */
@Response(code=CODE, description=MESSAGE)
public class MultiStatus extends HttpResponse {

	/** HTTP status code */
	public static final int CODE = 207;

	/** Default message */
	public static final String MESSAGE = "Multi-Status";

	/** Reusable instance. */
	public static final MultiStatus INSTANCE = new MultiStatus();

	/**
	 * Constructor using HTTP-standard message.
	 */
	public MultiStatus() {
		this(MESSAGE);
	}

	/**
	 * Constructor using custom message.
	 * @param message Message to send as the response.
	 */
	public MultiStatus(String message) {
		super(message);
	}
}