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
package org.apache.juneau.assertions;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;

/**
 * Used for fluent assertion calls against {@link Version} objects.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Validates the response expiration is after the current date.</jc>
 * 	<jv>client</jv>
 * 		.get(<jsf>URL</jsf>)
 * 		.run()
 * 		.getHeader(ClientVersion.<jk>class</jk>).assertVersion().major().isGreaterThanOrEqual(2);
 * </p>
 *
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentVersionAssertion<R>")
public class FluentVersionAssertion<R> extends FluentComparableAssertion<Version,R> {

	//-----------------------------------------------------------------------------------------------------------------
	// Constructors
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param value The value being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentVersionAssertion(Version value, R returns) {
		this(null, value, returns);
	}

	/**
	 * Constructor.
	 *
	 * @param creator The assertion that created this assertion.
	 * @param value The value being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentVersionAssertion(Assertion creator, Version value, R returns) {
		super(creator, value, returns);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Transform methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Extracts the specified version part (zero-indexed position).
	 *
	 * @param index The index of the version part to extract.
	 * @return The response object (for method chaining).
	 */
	public FluentIntegerAssertion<R> part(int index) {
		return new FluentIntegerAssertion<>(this, value().getPart(index).orElse(null), returns());
	}

	/**
	 * Extracts the major part of the version string (index position 0).
	 *
	 * @return The response object (for method chaining).
	 */
	public FluentIntegerAssertion<R> major() {
		return part(0);
	}

	/**
	 * Extracts the minor part of the version string (index position 1).
	 *
	 * @return The response object (for method chaining).
	 */
	public FluentIntegerAssertion<R> minor() {
		return part(1);
	}

	/**
	 * Extracts the maintenance part of the version string (index position 2).
	 *
	 * @return The response object (for method chaining).
	 */
	public FluentIntegerAssertion<R> maintenance() {
		return part(2);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fluent setters
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public FluentVersionAssertion<R> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentVersionAssertion<R> out(PrintStream value) {
		super.out(value);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentVersionAssertion<R> silent() {
		super.silent();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentVersionAssertion<R> stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentVersionAssertion<R> throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	// </FluentSetters>
}
