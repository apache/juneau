/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.rest.client.classic;

import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.message.StatusLine;
import org.apache.hc.core5.http.io.*;
import org.apache.juneau.rest.client.classic.assertion.*;

/**
 * An HTTP status line wrapper with assertion methods.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestClient">juneau-rest-client Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"resource" // response is owned by the RestCall that created this status line; lifecycle managed externally
})
public class ResponseStatusLine {

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
		return new FluentResponseStatusLineAssertion<>(inner, this);
	}

	/**
	 * Gets the protocol version.
	 *
	 * @return The protocol version.
	 */
	public ProtocolVersion getProtocolVersion() { return inner.getProtocolVersion(); }

	/**
	 * Gets the reason phrase.
	 *
	 * @return The reason phrase.
	 */
	public String getReasonPhrase() { return inner.getReasonPhrase(); }

	/**
	 * Gets the status code.
	 *
	 * @return The status code.
	 */
	public int getStatusCode() { return inner.getStatusCode(); }

	/**
	 * Returns the response that created this object.
	 *
	 * @return The response that created this object.
	 */
	public RestResponse response() {
		return response;
	}

	@Override /* Overridden from Object */
	public String toString() {
		return inner.toString();
	}
}