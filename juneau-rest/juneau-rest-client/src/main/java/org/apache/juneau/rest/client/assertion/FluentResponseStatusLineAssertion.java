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
package org.apache.juneau.rest.client.assertion;

import java.io.*;

import org.apache.http.*;
import org.apache.juneau.assertions.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.internal.*;

/**
 * Used for fluent assertion calls against a response {@link StatusLine} object.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Validates the response status code is 200 or 404.</jc>
 * 	<jv>client</jv>
 * 		.get(<jsf>URI</jsf>)
 * 		.run()
 * 		.assertStatus().code().isAny(200,404);
 * </p>
 *
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentResponseStatusLineAssertion<R>")
public class FluentResponseStatusLineAssertion<R> extends FluentObjectAssertion<StatusLine,R> {

	private final StatusLine statusLine;

	/**
	 * Constructor.
	 *
	 * @param statusLine The response status line.
	 * @param returns The object to return after the test.
	 */
	public FluentResponseStatusLineAssertion(StatusLine statusLine, R returns) {
		super(null, returns);
		this.statusLine = statusLine;
		throwable(BadRequest.class);
	}

	/**
	 * Returns an assertion against the status code on the response status object.
	 *
	 * @return An assertion against the status code on the response status object.
	 */
	public FluentIntegerAssertion<R> code() {
		return new FluentIntegerAssertion<>(this, statusLine.getStatusCode(), returns());
	}

	/**
	 * Returns an assertion against the reason phrase on the response status object.
	 *
	 * @return An assertion against the reason phrase on the response status object.
	 */
	public FluentStringAssertion<R> reason() {
		return new FluentStringAssertion<>(this, statusLine.getReasonPhrase(), returns());
	}

	/**
	 * Returns an assertion against the protocol on the response status object.
	 *
	 * @return An assertion against the protocol on the response status object.
	 */
	public FluentStringAssertion<R> protocol() {
		return new FluentStringAssertion<>(this, statusLine.getProtocolVersion().getProtocol(), returns());
	}

	/**
	 * Returns an assertion against the protocol major version on the response status object.
	 *
	 * @return An assertion against the protocol major version on the response status object.
	 */
	public FluentIntegerAssertion<R> major() {
		return new FluentIntegerAssertion<>(this, statusLine.getProtocolVersion().getMajor(), returns());
	}

	/**
	 * Returns an assertion against the protocol minor version on the response status object.
	 *
	 * @return An assertion against the protocol minor version on the response status object.
	 */
	public FluentIntegerAssertion<R> minor() {
		return new FluentIntegerAssertion<>(this, statusLine.getProtocolVersion().getMinor(), returns());
	}

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public FluentResponseStatusLineAssertion<R> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentResponseStatusLineAssertion<R> out(PrintStream value) {
		super.out(value);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentResponseStatusLineAssertion<R> silent() {
		super.silent();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentResponseStatusLineAssertion<R> stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentResponseStatusLineAssertion<R> throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	// </FluentSetters>
}
