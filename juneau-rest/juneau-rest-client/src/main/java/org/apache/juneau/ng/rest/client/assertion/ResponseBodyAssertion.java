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

import java.io.*;

import org.apache.juneau.commons.utils.StringUtils;
import org.apache.juneau.ng.rest.client.*;

/**
 * A fluent assertion class for HTTP response bodies.
 *
 * <p>
 * Obtain instances via {@link ResponseAssertion#body()}.
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
public final class ResponseBodyAssertion {

	@SuppressWarnings({
		"resource" // Eclipse resource analysis: response is borrowed; caller closes it
	})
	private final NgRestResponse response;
	private final ResponseAssertion parent;

	ResponseBodyAssertion(NgRestResponse response, ResponseAssertion parent) {
		this.response = response;
		this.parent = parent;
	}

	/**
	 * Asserts that the response body (as a UTF-8 string) equals the expected value.
	 *
	 * @param expected The expected body string. May be <jk>null</jk> to assert an empty/absent body.
	 * @return This object (for chaining).
	 * @throws AssertionError If the body does not match.
	 * @throws UncheckedIOException If reading the body throws {@link IOException}.
	 */
	public ResponseBodyAssertion equals(String expected) {
		var actual = readBodyAsString();
		if (expected == null ? actual != null : !expected.equals(actual))
			throw new AssertionError("Expected body '" + expected + "' but got '" + actual + "'");
		return this;
	}

	/**
	 * Asserts that the response body (as a UTF-8 string) contains the given substring.
	 *
	 * @param substring The substring to look for. Must not be <jk>null</jk>.
	 * @return This object (for chaining).
	 * @throws AssertionError If the body is absent or does not contain the substring.
	 * @throws UncheckedIOException If reading the body throws {@link IOException}.
	 */
	public ResponseBodyAssertion contains(String substring) {
		var actual = readBodyAsString();
		if (actual == null || !actual.contains(substring))
			throw new AssertionError("Body '" + actual + "' does not contain '" + substring + "'");
		return this;
	}

	/**
	 * Asserts that the response body is empty (null or zero-length).
	 *
	 * @return This object (for chaining).
	 * @throws AssertionError If the body is non-empty.
	 * @throws UncheckedIOException If reading the body throws {@link IOException}.
	 */
	public ResponseBodyAssertion isEmpty() {
		var actual = readBodyAsString();
		if (StringUtils.isNotEmpty(actual))
			throw new AssertionError("Expected empty body but got '" + actual + "'");
		return this;
	}

	/**
	 * Asserts that the response body is present and non-empty.
	 *
	 * @return This object (for chaining).
	 * @throws AssertionError If the body is null or empty.
	 * @throws UncheckedIOException If reading the body throws {@link IOException}.
	 */
	public ResponseBodyAssertion isNotEmpty() {
		var actual = readBodyAsString();
		if (StringUtils.isEmpty(actual))
			throw new AssertionError("Expected non-empty body but body was absent or empty");
		return this;
	}

	/**
	 * Asserts that the response body starts with the given prefix.
	 *
	 * @param prefix The expected prefix. Must not be <jk>null</jk>.
	 * @return This object (for chaining).
	 * @throws AssertionError If the body is absent or does not start with the prefix.
	 * @throws UncheckedIOException If reading the body throws {@link IOException}.
	 */
	public ResponseBodyAssertion startsWith(String prefix) {
		var actual = readBodyAsString();
		if (actual == null || !actual.startsWith(prefix))
			throw new AssertionError("Body '" + actual + "' does not start with '" + prefix + "'");
		return this;
	}

	/**
	 * Returns the parent {@link ResponseAssertion} to continue assertion chaining.
	 *
	 * @return The parent assertion. Never <jk>null</jk>.
	 */
	public ResponseAssertion and() {
		return parent;
	}

	private String readBodyAsString() {
		try {
			return response.getBodyAsString();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
