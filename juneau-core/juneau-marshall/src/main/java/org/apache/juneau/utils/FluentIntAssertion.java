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
package org.apache.juneau.utils;


import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.*;

/**
 * Used for fluent assertion calls.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Validates the response status code is 200 or 404.</jc>
 * 	client
 * 		.get(<jsf>URL</jsf>)
 * 		.run()
 * 		.assertStatus().isOneOf(200,404);
 * </p>
 * @param <R> The return type.
 */
public class FluentIntAssertion<R> {

	private final int value;
	private final R returns;

	/**
	 * Constructor.
	 *
	 * @param value The value being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentIntAssertion(int value, R returns) {
		this.value = value;
		this.returns = returns;
	}

	/**
	 * Asserts that the value equals the specified value.
	 *
	 * @param value The value to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R equals(int value) throws AssertionError {
		if (this.value != value)
			throw new BasicAssertionError("Unexpected value.\n\tExpected=[{0}]\n\tActual=[{1}]", value, this.value);
		return returns;
	}

	/**
	 * Asserts that the value equals the specified value.
	 *
	 * @param value The value to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R notEquals(int value) throws AssertionError {
		if (this.value == value)
			throw new BasicAssertionError("Unexpected value.\n\tExpected not=[{0}]\n\tActual=[{1}]", value, this.value);
		return returns;
	}

	/**
	 * Asserts that the value is one of the specified values.
	 *
	 * @param values The values to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isOneOf(int...values) throws AssertionError {
		for (int v : values)
			if (this.value == v)
				return returns;
		throw new BasicAssertionError("Expected value not found.\n\tExpected=[{0}]\n\tActual=[{1}]", SimpleJson.DEFAULT.toString(values), value);
	}

	/**
	 * Asserts that the value is one of the specified values.
	 *
	 * @param values The values to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isNotOneOf(int...values) throws AssertionError {
		for (int v : values)
			if (this.value == v)
				throw new BasicAssertionError("Unexpected value found.\n\tUnexpected=[{0}]\n\tActual=[{1}]", v, value);
		return returns;
	}

	/**
	 * Asserts that the value passes the specified predicate test.
	 *
	 * @param test The predicate to use to test the value.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R passes(Predicate<Integer> test) throws AssertionError {
		if (! test.test(value))
			throw new BasicAssertionError("Value did not pass predicate test.\n\tValue=[{0}]", value);
		return returns;
	}
}
