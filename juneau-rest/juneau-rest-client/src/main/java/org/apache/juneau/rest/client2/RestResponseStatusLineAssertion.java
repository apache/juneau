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
package org.apache.juneau.rest.client2;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.assertions.*;

/**
 * Used for fluent assertion calls against a response {@link StatusLine} object.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Validates the response status code is 200 or 404.</jc>
 * 	client
 * 		.get(<jsf>URL</jsf>)
 * 		.run()
 * 		.assertStatus().isAny(200,404);
 * </p>
 */
public class RestResponseStatusLineAssertion extends FluentIntegerAssertion<RestResponse> {

	private final StatusLine statusLine;

	/**
	 * Constructor.
	 *
	 * @param statusLine The response status line.
	 * @param returns The object to return after the test.
	 */
	public RestResponseStatusLineAssertion(StatusLine statusLine, RestResponse returns) {
		super(statusLine.getStatusCode(), returns);
		this.statusLine = statusLine;
	}

	/**
	 * Asserts that the protocol version equals the specified value.
	 *
	 * @param value The value to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public RestResponse isProtocolVersion(ProtocolVersion value) throws AssertionError {
		if (! statusLine.getProtocolVersion().equals(value))
			throw new BasicAssertionError("Unexpected protocol version.\n\tExpected=[{0}]\n\tActual=[{1}]", value, this.statusLine.getProtocolVersion());
		return returns();
	}
}
