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
 * A fluent assertion class for a named HTTP response header.
 *
 * <p>
 * Obtain instances via {@link ResponseAssertion#header(String)}.
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
public final class ResponseHeaderAssertion {

	private final String name;

	@SuppressWarnings({
		"resource" // Eclipse resource analysis: response is borrowed; caller closes it
	})
	private final NgRestResponse response;
	private final ResponseAssertion parent;

	ResponseHeaderAssertion(String name, NgRestResponse response, ResponseAssertion parent) {
		this.name = name;
		this.response = response;
		this.parent = parent;
	}

	/**
	 * Asserts that this header is present in the response.
	 *
	 * @return This object (for chaining).
	 * @throws AssertionError If the header is absent.
	 */
	public ResponseHeaderAssertion isPresent() {
		if (response.getFirstHeader(name) == null)
			throw new AssertionError("Expected header '" + name + "' to be present but was absent");
		return this;
	}

	/**
	 * Asserts that this header is absent from the response.
	 *
	 * @return This object (for chaining).
	 * @throws AssertionError If the header is present.
	 */
	public ResponseHeaderAssertion isAbsent() {
		if (response.getFirstHeader(name) != null)
			throw new AssertionError("Expected header '" + name + "' to be absent but was present with value: " + response.getFirstHeader(name).value());
		return this;
	}

	/**
	 * Asserts that this header's value equals the expected value (case-sensitive).
	 *
	 * @param expected The expected value. Must not be <jk>null</jk>.
	 * @return This object (for chaining).
	 * @throws AssertionError If the value does not match.
	 */
	public ResponseHeaderAssertion equals(String expected) {
		var h = response.getFirstHeader(name);
		var actual = h != null ? h.value() : null;
		if (!expected.equals(actual))
			throw new AssertionError("Header '" + name + "': expected '" + expected + "' but got '" + actual + "'");
		return this;
	}

	/**
	 * Asserts that this header's value contains the given substring.
	 *
	 * @param substring The substring to look for. Must not be <jk>null</jk>.
	 * @return This object (for chaining).
	 * @throws AssertionError If the value is absent or does not contain the substring.
	 */
	public ResponseHeaderAssertion contains(String substring) {
		var h = response.getFirstHeader(name);
		var actual = h != null ? h.value() : null;
		if (actual == null || !actual.contains(substring))
			throw new AssertionError("Header '" + name + "' value '" + actual + "' does not contain '" + substring + "'");
		return this;
	}

	/**
	 * Asserts that this header's value starts with the given prefix.
	 *
	 * @param prefix The expected prefix. Must not be <jk>null</jk>.
	 * @return This object (for chaining).
	 * @throws AssertionError If the value is absent or does not start with the prefix.
	 */
	public ResponseHeaderAssertion startsWith(String prefix) {
		var h = response.getFirstHeader(name);
		var actual = h != null ? h.value() : null;
		if (actual == null || !actual.startsWith(prefix))
			throw new AssertionError("Header '" + name + "' value '" + actual + "' does not start with '" + prefix + "'");
		return this;
	}

	/**
	 * Asserts that this header's integer value equals the expected value.
	 *
	 * @param expected The expected integer value.
	 * @return This object (for chaining).
	 * @throws AssertionError If the value is absent, not a number, or does not match.
	 */
	public ResponseHeaderAssertion integerEquals(int expected) {
		var h = response.getFirstHeader(name);
		if (h == null)
			throw new AssertionError("Header '" + name + "' is absent");
		int actual;
		try {
			actual = Integer.parseInt(h.value().trim());
		} catch (NumberFormatException e) {
			throw new AssertionError("Header '" + name + "' value '" + h.value() + "' is not a valid integer");
		}
		if (actual != expected)
			throw new AssertionError("Header '" + name + "': expected " + expected + " but got " + actual);
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
}
