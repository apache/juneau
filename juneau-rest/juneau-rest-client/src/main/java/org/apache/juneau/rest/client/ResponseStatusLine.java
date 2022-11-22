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
package org.apache.juneau.rest.client;

import org.apache.http.*;
import org.apache.juneau.rest.client.assertion.*;

/**
 * An implementation of {@link StatusLine} that adds assertions methods.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-client">juneau-rest-client</a>
 * </ul>
 */
public class ResponseStatusLine implements StatusLine {

	private final RestResponse response;
	private final StatusLine inner;

	/**
	 * Constructor.
	 *
	 * @param response The response that created this object.
	 * @param inner The status line returned by the underlying client.
	 */
	protected ResponseStatusLine(RestResponse response, StatusLine inner) {
		this.response = response;
		this.inner = inner;
	}

	@Override /* StatusLine */
	public ProtocolVersion getProtocolVersion() {
		return inner.getProtocolVersion();
	}

	@Override /* StatusLine */
	public int getStatusCode() {
		return inner.getStatusCode();
	}

	@Override /* StatusLine */
	public String getReasonPhrase() {
		return inner.getReasonPhrase();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Assertions
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Provides the ability to perform fluent-style assertions on this response status line.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Validates the content type header is provided.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.getStatusLine().assertValue().asCode().is(200);
	 * </p>
	 *
	 * @return A new fluent assertion object.
	 */
	public FluentResponseStatusLineAssertion<ResponseStatusLine> assertValue() {
		return new FluentResponseStatusLineAssertion<>(this, this);
	}

	/**
	 * Returns the response that created this object.
	 *
	 * @return The response that created this object.
	 */
	public RestResponse response() {
		return response;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Other methods
	//------------------------------------------------------------------------------------------------------------------

	@Override /* Object */
	public String toString() {
		return inner.toString();
	}
}
