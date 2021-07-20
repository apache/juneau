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

import org.apache.juneau.assertions.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.*;

/**
 * Used for fluent assertion calls against {@link RequestBody} objects.
 *
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentRequestBodyAssertion<R>")
public class FluentRequestBodyAssertion<R> extends FluentObjectAssertion<RequestBody,R> {

	/**
	 * Constructor.
	 *
	 * @param value The object being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentRequestBodyAssertion(RequestBody value, R returns) {
		super(null, returns);
		throwable(BadRequest.class);
	}

	/**
	 * Provides the ability to perform fluent-style assertions on this request body.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the request body equals the text "OK".</jc>
	 * 	<jv>request</jv>
	 * 		.assertBody().is(<js>"OK"</js>);
	 * </p>
	 *
	 * <p>
	 * The assertion test returns the original request object allowing you to chain multiple requests like so:
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the request body matches a regular expression.</jc>
	 * 	MyBean <jv>bean</jv> = <jv>request</jv>
	 * 		.assertBody().asString().matches(<js>".*OK.*"</js>);
	 * 		.assertBody().asString().doesNotMatch(<js>".*ERROR.*"</js>)
	 * 		.getBody().asType(MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If no charset was found on the <code>Content-Type</code> request header, <js>"UTF-8"</js> is assumed.
	 *  <li>
	 *		When using this method, the body is automatically cached by calling the {@link RequestBody#cache()}.
	 * 	<li>
	 * 		The input stream is automatically closed after this call.
	 * </ul>
	 *
	 * @return A new fluent assertion object.
	 */
	@Override
	public FluentStringAssertion<R> asString() {
		return new FluentStringAssertion<>(valueAsString(), returns());
	}

	/**
	 * Provides the ability to perform fluent-style assertions on the bytes of the request body.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the request body equals the text "foo".</jc>
	 * 	<jv>request</jv>
	 * 		.assertBody().asBytes().hex().is(<js>"666F6F"</js>);
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If no charset was found on the <code>Content-Type</code> request header, <js>"UTF-8"</js> is assumed.
	 *  <li>
	 *		When using this method, the body is automatically cached by calling the {@link RequestBody#cache()}.
	 * 	<li>
	 * 		The input stream is automatically closed after this call.
	 * </ul>
	 *
	 * @return A new fluent assertion object.
	 */
	public FluentByteArrayAssertion<R> asBytes() {
		return new FluentByteArrayAssertion<>(valueAsBytes(), returns());
	}

	/**
	 * Provides the ability to perform fluent-style assertions on this request body.
	 *
	 * <p>
	 * Converts the body to a type using {@link RequestBody#asType(Class)} and then returns the value as an object assertion.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the request body bean is the expected value.</jc>
	 * 	<jv>request</jv>
	 * 		.assertBody().asType(MyBean.<jk>class</jk>).json().is(<js>"{foo:'bar'}"</js>);
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If no charset was found on the <code>Content-Type</code> request header, <js>"UTF-8"</js> is assumed.
	 *  <li>
	 *		When using this method, the body is automatically cached by calling the {@link RequestBody#cache()}.
	 * 	<li>
	 * 		The input stream is automatically closed after this call.
	 * </ul>
	 *
	 * @param type The object type to create.
	 * @return A new fluent assertion object.
	 */
	public <V> FluentObjectAssertion<V,R> asType(Class<V> type) {
		return new FluentObjectAssertion<>(valueAsType(type), returns());
	}

	/**
	 * Asserts that the body contains the specified value.
	 *
	 * @param values The value to check against.
	 * @return The request object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R is(String values) throws AssertionError {
		return asString().is(values);
	}

	/**
	 * Asserts that the text contains all of the specified substrings.
	 *
	 * @param values The values to check against.
	 * @return The request object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R contains(String...values) throws AssertionError {
		return asString().contains(values);
	}

	/**
	 * Asserts that the body doesn't contain any of the specified substrings.
	 *
	 * @param values The values to check against.
	 * @return The request object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R doesNotContain(String...values) throws AssertionError {
		return asString().doesNotContain(values);
	}

	/**
	 * Asserts that the body is empty.
	 *
	 * @return The request object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isEmpty() {
		return asString().isEmpty();
	}

	/**
	 * Asserts that the body is not empty.
	 *
	 * @return The request object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isNotEmpty() {
		return asString().isNotEmpty();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//-----------------------------------------------------------------------------------------------------------------

	private String valueAsString() throws AssertionError {
		try {
			return value().cache().asString();
		} catch (IOException e) {
			throw error(e, "Exception occurred during call.");
		}
	}

	private byte[] valueAsBytes() throws AssertionError {
		try {
			return value().cache().asBytes();
		} catch (IOException e) {
			throw error(e, "Exception occurred during call.");
		}
	}

	private <T> T valueAsType(Class<T> c) throws AssertionError {
		try {
			return value().cache().asType(c);
		} catch (IOException e) {
			throw error(e, "Exception occurred during call.");
		}
	}

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public FluentRequestBodyAssertion<R> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentRequestBodyAssertion<R> out(PrintStream value) {
		super.out(value);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentRequestBodyAssertion<R> silent() {
		super.silent();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentRequestBodyAssertion<R> stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentRequestBodyAssertion<R> throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	// </FluentSetters>
}
