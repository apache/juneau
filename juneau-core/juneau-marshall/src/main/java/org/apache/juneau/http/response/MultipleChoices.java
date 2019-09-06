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

import static org.apache.juneau.http.response.MultipleChoices.*;

import org.apache.juneau.http.annotation.*;

/**
 * Represents an <c>HTTP 300 Multiple Choices</c> response.
 *
 * <p>
 * Indicates multiple options for the resource from which the client may choose (via agent-driven content negotiation).
 * For example, this code could be used to present multiple video format options, to list files with different filename extensions, or to suggest word-sense disambiguation.
 */
@Response(code=CODE, description=MESSAGE)
public class MultipleChoices extends HttpResponse {

	/** HTTP status code */
	public static final int CODE = 300;

	/** Default message */
	public static final String MESSAGE = "Multiple Choices";

	/** Reusable instance. */
	public static final MultipleChoices INSTANCE = new MultipleChoices();

	/**
	 * Constructor using HTTP-standard message.
	 */
	public MultipleChoices() {
		this(MESSAGE);
	}

	/**
	 * Constructor using custom message.
	 * @param message Message to send as the response.
	 */
	public MultipleChoices(String message) {
		super(message);
	}
}