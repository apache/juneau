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
package org.apache.juneau.rest.assertions;

import org.apache.http.*;
import org.apache.juneau.assertions.*;
import org.apache.juneau.http.exception.*;
import org.apache.juneau.internal.*;

/**
 * Used for fluent assertion calls against {@link RequestLine} objects.
 *
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentRequestLineAssertion<R>")
public class FluentRequestLineAssertion<R> extends FluentAssertion<R> {

	private final RequestLine value;

	/**
	 * Constructor.
	 *
	 * @param value The object being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentRequestLineAssertion(RequestLine value, R returns) {
		super(null, returns);
		this.value = value;
		throwable(BadRequest.class);
	}

	/**
	 * Returns the request line method string as a new assertion.
	 *
	 * @return A new assertion.
	 */
	public FluentStringAssertion<R> method() {
		return new FluentStringAssertion<>(value.getMethod(), returns());
	}

	/**
	 * Returns the request line uri string as a new assertion.
	 *
	 * @return A new assertion.
	 */
	public FluentStringAssertion<R> uri() {
		return new FluentStringAssertion<>(value.getUri(), returns());
	}

	/**
	 * Returns the request line protocol version as a new assertion.
	 *
	 * @return A new assertion.
	 */
	public FluentProtocolVersionAssertion<R> protocolVersion() {
		return new FluentProtocolVersionAssertion<>(value.getProtocolVersion(), returns());
	}

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public FluentRequestLineAssertion<R> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentRequestLineAssertion<R> stderr() {
		super.stderr();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentRequestLineAssertion<R> stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentRequestLineAssertion<R> throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	// </FluentSetters>
}
