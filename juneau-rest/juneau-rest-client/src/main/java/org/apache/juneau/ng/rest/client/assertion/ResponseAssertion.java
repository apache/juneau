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
package org.apache.juneau.ng.rest.client.assertion;

import org.apache.juneau.ng.rest.client.*;

/**
 * A fluent assertion class for validating {@link NgRestResponse} instances.
 *
 * <p>
 * Provides assertion methods for HTTP status code, response headers, and body content.
 * All assertion methods return {@code this} for chaining, and throw {@link AssertionError}
 * on failure.
 *
 * <p>
 * Obtain instances via {@link NgRestResponse#assertThat()}.
 *
 * <p class='bjava'>
 * 	<jk>try</jk> (NgRestResponse <jv>resp</jv> = client.get(<js>"/api/users"</js>).run()) {
 * 		<jv>resp</jv>.assertThat()
 * 			.statusCode(200)
 * 			.header(<js>"Content-Type"</js>).contains(<js>"application/json"</js>)
 * 			.body().contains(<js>"alice"</js>);
 * 	}
 * </p>
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/juneau-ng-rest-client">juneau-ng REST client</a>
 * </ul>
 *
 * @since 9.2.1
 */
public final class ResponseAssertion {

	@SuppressWarnings({
		"resource" // Eclipse resource analysis: response is borrowed for assertions; caller closes it
	})
	private final NgRestResponse response;

	/**
	 * Constructor.
	 *
	 * @param response The response to assert on. Must not be <jk>null</jk>.
	 */
	public ResponseAssertion(NgRestResponse response) {
		this.response = response;
	}

	/**
	 * Asserts that the response status code equals the expected value.
	 *
	 * @param expected The expected status code.
	 * @return This object (for chaining).
	 * @throws AssertionError If the status code does not match.
	 */
	public ResponseAssertion statusCode(int expected) {
		var actual = response.getStatusCode();
		if (actual != expected)
			throw new AssertionError("Expected status code " + expected + " but got " + actual);
		return this;
	}

	/**
	 * Asserts that the response status code is in the 2xx (success) range.
	 *
	 * @return This object (for chaining).
	 * @throws AssertionError If the status code is not in 200–299.
	 */
	public ResponseAssertion isOk() {
		var sc = response.getStatusCode();
		if (sc < 200 || sc > 299)
			throw new AssertionError("Expected 2xx status but got " + sc);
		return this;
	}

	/**
	 * Asserts that the response status code is in the 4xx (client error) range.
	 *
	 * @return This object (for chaining).
	 * @throws AssertionError If the status code is not in 400–499.
	 */
	public ResponseAssertion isClientError() {
		var sc = response.getStatusCode();
		if (sc < 400 || sc > 499)
			throw new AssertionError("Expected 4xx status but got " + sc);
		return this;
	}

	/**
	 * Asserts that the response status code is in the 5xx (server error) range.
	 *
	 * @return This object (for chaining).
	 * @throws AssertionError If the status code is not in 500–599.
	 */
	public ResponseAssertion isServerError() {
		var sc = response.getStatusCode();
		if (sc < 500 || sc > 599)
			throw new AssertionError("Expected 5xx status but got " + sc);
		return this;
	}

	/**
	 * Returns a fluent header assertion for the named response header.
	 *
	 * @param name The header name (case-insensitive). Must not be <jk>null</jk>.
	 * @return A new header assertion. Never <jk>null</jk>.
	 */
	public ResponseHeaderAssertion header(String name) {
		return new ResponseHeaderAssertion(name, response, this);
	}

	/**
	 * Returns a fluent body assertion.
	 *
	 * @return A new body assertion. Never <jk>null</jk>.
	 */
	public ResponseBodyAssertion body() {
		return new ResponseBodyAssertion(response, this);
	}

	/**
	 * Returns the underlying response (for further access after assertions).
	 *
	 * @return The response. Never <jk>null</jk>.
	 */
	public NgRestResponse and() {
		return response;
	}
}
