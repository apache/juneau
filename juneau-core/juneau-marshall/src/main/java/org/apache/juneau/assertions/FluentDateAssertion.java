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


import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;

/**
 * Used for fluent assertion calls.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Validates the response expiration is after the current date.</jc>
 * 	client
 * 		.get(<jsf>URL</jsf>)
 * 		.run()
 * 		.assertDateHeader(<js>"Expires"</js>).isAfter(<jk>new</jk> Date());
 * </p>
 * @param <R> The return type.
 */
public class FluentDateAssertion<R> {

	private final Date value;
	private final R returns;

	/**
	 * Constructor.
	 *
	 * @param value The value being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentDateAssertion(Date value, R returns) {
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
	public R equals(Date value) throws AssertionError {
		if (! this.value.equals(value))
			throw new BasicAssertionError("Unexpected value.\n\tExpected=[{0}]\n\tActual=[{1}]", value, this.value);
		return returns;
	}

	/**
	 * Asserts that the value equals the specified value.
	 *
	 * <p>
	 * Equivalent to {@link #equals(Date)}.
	 *
	 * @param value The value to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R is(Date value) throws AssertionError {
		return equals(value);
	}

	/**
	 * Asserts that the value equals the specified value.
	 *
	 * @param value The value to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R doesNotEqual(Date value) throws AssertionError {
		if (this.value.equals(value))
			throw new BasicAssertionError("Unexpected value.\n\tExpected not=[{0}]\n\tActual=[{1}]", value, this.value);
		return returns;
	}

	/**
	 * Asserts that the value equals the specified value.
	 *
	 * <p>
	 * Equivalent to {@link #doesNotEqual(Date)}.
	 *
	 * @param value The value to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isNot(Date value) throws AssertionError {
		return doesNotEqual(value);
	}

	/**
	 * Asserts that the date is not null.
	 *
	 * <p>
	 * Equivalent to {@link #isNotNull()}.
	 *
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R exists() throws AssertionError {
		return isNotNull();
	}

	/**
	 * Asserts that the date is not null.
	 *
	 * <p>
	 * Equivalent to {@link #isNull()}.
	 *
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R doesNotExist() throws AssertionError {
		return isNull();
	}

	/**
	 * Asserts that the date is not null.
	 *
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isNull() throws AssertionError {
		if (value != null)
			throw new BasicAssertionError("Value was not null.");
		return returns;
	}

	/**
	 * Asserts that the date is not null.
	 *
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isNotNull() throws AssertionError {
		if (value == null)
			throw new BasicAssertionError("Value was null.");
		return returns;
	}

	/**
	 * Asserts that the value is greater than the specified value.
	 *
	 * @param value The values to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isAfter(Date value) throws AssertionError {
		if (! (this.value.after(value)))
				throw new BasicAssertionError("Value was not greater than expected.\n\tExpected=[{0}]\n\tActual=[{1}]", value, this.value);
		return returns;
	}

	/**
	 * Asserts that the value is less than the specified value.
	 *
	 * @param value The values to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isBefore(Date value) throws AssertionError {
		if (! (this.value.before(value)))
				throw new BasicAssertionError("Value was not less than expected.\n\tExpected=[{0}]\n\tActual=[{1}]", value, this.value);
		return returns;
	}

	/**
	 * Asserts that the value is between (exclusive) the specified upper and lower values.
	 *
	 * @param lower The lower value to check against.
	 * @param upper The upper value to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isBetween(Date lower, Date upper) throws AssertionError {
		isBefore(upper);
		isAfter(lower);
		return returns;
	}

	/**
	 * Asserts that the value passes the specified predicate test.
	 *
	 * @param test The predicate to use to test the value.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R passes(Predicate<Date> test) throws AssertionError {
		if (! test.test(value))
			throw new BasicAssertionError("Value did not pass predicate test.\n\tValue=[{0}]", value);
		return returns;
	}
}
