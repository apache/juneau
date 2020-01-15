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

import static org.apache.juneau.rest.response.NoContent.*;

import org.apache.juneau.http.annotation.*;

/**
 * Represents an <c>HTTP 204 No Content</c> response.
 *
 * <div class='warn'>
 * 	<b>Deprecated</b> - Use {@link org.apache.juneau.http.response.NoContent}
 * </div>
 *
 * <p>
 * The server successfully processed the request and is not returning any content.
 */
@Response(code=CODE, description=MESSAGE)
@Deprecated
public class NoContent extends HttpResponse {

	/** HTTP status code */
	public static final int CODE = 204;

	/** Default message */
	public static final String MESSAGE = "No Content";

	/** Reusable instance. */
	public static final NoContent INSTANCE = new NoContent();

	/**
	 * Constructor using HTTP-standard message.
	 */
	public NoContent() {
		this(MESSAGE);
	}

	/**
	 * Constructor using custom message.
	 * @param message Message to send as the response.
	 */
	public NoContent(String message) {
		super(message);
	}
}