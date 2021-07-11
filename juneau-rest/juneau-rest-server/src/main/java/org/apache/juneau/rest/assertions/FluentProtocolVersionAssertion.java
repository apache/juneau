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

import java.io.*;

import org.apache.http.*;
import org.apache.juneau.assertions.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.internal.*;

/**
 * Used for fluent assertion calls against {@link ProtocolVersion} objects.
 *
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentProtocolVersionAssertion<R>")
public class FluentProtocolVersionAssertion<R> extends FluentObjectAssertion<ProtocolVersion,R> {

	/**
	 * Constructor.
	 *
	 * @param value The object being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentProtocolVersionAssertion(ProtocolVersion value, R returns) {
		super(null, returns);
		throwable(BadRequest.class);
	}

	/**
	 * Returns the protocol string as a new assertion.
	 *
	 * @return A new assertion.
	 */
	public FluentStringAssertion<R> protocol() {
		return new FluentStringAssertion<>(value().getProtocol(), returns());
	}

	/**
	 * Returns the protocol major version as a new assertion.
	 *
	 * @return A new assertion.
	 */
	public FluentIntegerAssertion<R> major() {
		return new FluentIntegerAssertion<>(value().getMajor(), returns());
	}

	/**
	 * Returns the protocol minor version as a new assertion.
	 *
	 * @return A new assertion.
	 */
	public FluentIntegerAssertion<R> minor() {
		return new FluentIntegerAssertion<>(value().getMinor(), returns());
	}

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public FluentProtocolVersionAssertion<R> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentProtocolVersionAssertion<R> out(PrintStream value) {
		super.out(value);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentProtocolVersionAssertion<R> silent() {
		super.silent();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentProtocolVersionAssertion<R> stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentProtocolVersionAssertion<R> throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	// </FluentSetters>
}
