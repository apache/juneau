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


import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.*;

/**
 * Used for fluent assertion calls.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Validates the response length isn't too long.</jc>
 * 	client
 * 		.get(<jsf>URL</jsf>)
 * 		.run()
 * 		.assertLongHeader(<js>"Length"</js>).isLessThan(100000);
 * </p>
 * @param <R> The return type.
 */
public class FluentLongAssertion<R> {

	private final Long value;
	private final R returns;

	/**
	 * Constructor.
	 *
	 * @param value The value being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentLongAssertion(Long value, R returns) {
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
	public R equals(Long value) throws AssertionError {
		if (this.value == value)
			return returns;
		exists();
		if (! this.value.equals(value))
			throw new BasicAssertionError("Unexpected value.\n\tExpected=[{0}]\n\tActual=[{1}]", value, this.value);
		return returns;
	}

	/**
	 * Asserts that the value equals the specified value.
	 *
	 * <p>
	 * Equivalent to {@link #equals(Long)}.
	 *
	 * @param value The value to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R is(Long value) throws AssertionError {
		return equals(value);
	}

	/**
	 * Asserts that the value equals the specified value.
	 *
	 * @param value The value to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R doesNotEqual(Long value) throws AssertionError {
		if (this.value != value)
			return returns;
		exists();
		if (this.value.equals(value))
			throw new BasicAssertionError("Unexpected value.\n\tExpected not=[{0}]\n\tActual=[{1}]", value, this.value);
		return returns;
	}

	/**
	 * Asserts that the value equals the specified value.
	 *
	 * <p>
	 * Equivalent to {@link #doesNotEqual(Long)}.
	 *
	 * @param value The value to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isNot(Long value) throws AssertionError {
		return doesNotEqual(value);
	}

	/**
	 * Asserts that the long is not null.
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
	 * Asserts that the long is not null.
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
	 * Asserts that the long is not null.
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
	 * Asserts that the long is not null.
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
	 * Asserts that the value is one of the specified values.
	 *
	 * @param values The values to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isAny(Long...values) throws AssertionError {
		exists();
		for (Long v : values)
			if (this.value.equals(v))
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
	public R isNotAny(Long...values) throws AssertionError {
		exists();
		for (Long v : values)
			if (this.value.equals(v))
				throw new BasicAssertionError("Unexpected value found.\n\tUnexpected=[{0}]\n\tActual=[{1}]", v, value);
		return returns;
	}

	/**
	 * Asserts that the value is greater than the specified value.
	 *
	 * @param value The values to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isGreaterThan(Long value) throws AssertionError {
		exists();
		if (! (this.value > value))
				throw new BasicAssertionError("Value was not greater than expected.\n\tExpected=[{0}]\n\tActual=[{1}]", value, this.value);
		return returns;
	}

	/**
	 * Asserts that the value is greater than the specified value.
	 *
	 * <p>
	 * Equivalent to {@link #isGreaterThan(Long)}
	 *
	 * @param value The values to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isGt(Long value) throws AssertionError {
		return isGreaterThan(value);
	}

	/**
	 * Asserts that the value is greater than or equal to the specified value.
	 *
	 * @param value The values to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isGreaterThanOrEquals(Long value) throws AssertionError {
		exists();
		if (! (this.value >= value))
				throw new BasicAssertionError("Value was not greater than or equals to expected.\n\tExpected=[{0}]\n\tActual=[{1}]", value, this.value);
		return returns;
	}

	/**
	 * Asserts that the value is greater than or equal to the specified value.
	 *
	 * <p>
	 * Equivalent to {@link #isGreaterThanOrEquals(Long)}
	 *
	 * @param value The values to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isGte(Long value) throws AssertionError {
		return isGreaterThanOrEquals(value);
	}

	/**
	 * Asserts that the value is less than the specified value.
	 *
	 * @param value The values to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isLessThan(Long value) throws AssertionError {
		exists();
		if (! (this.value < value))
				throw new BasicAssertionError("Value was not less than expected.\n\tExpected=[{0}]\n\tActual=[{1}]", value, this.value);
		return returns;
	}

	/**
	 * Asserts that the value is less than the specified value.
	 *
	 * <p>
	 * Equivalent to {@link #isLessThan(Long)}
	 *
	 * @param value The values to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isLt(Long value) throws AssertionError {
		return isLessThan(value);
	}

	/**
	 * Asserts that the value is less than or equals to the specified value.
	 *
	 * @param value The values to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isLessThanOrEquals(Long value) throws AssertionError {
		exists();
		if (! (this.value <= value))
				throw new BasicAssertionError("Value was not less than or equals to expected.\n\tExpected=[{0}]\n\tActual=[{1}]", value, this.value);
		return returns;
	}

	/**
	 * Asserts that the value is less than or equals to the specified value.
	 *
	 * <p>
	 * Equivalent to {@link #isLessThanOrEquals(Long)}
	 *
	 * @param value The values to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isLte(Long value) throws AssertionError {
		return isLessThanOrEquals(value);
	}

	/**
	 * Asserts that the value is between (inclusive) the specified upper and lower values.
	 *
	 * @param lower The lower value to check against.
	 * @param upper The upper value to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isBetween(Long lower, Long upper) throws AssertionError {
		isLessThanOrEquals(upper);
		isGreaterThanOrEquals(lower);
		return returns;
	}

	/**
	 * Asserts that the value passes the specified predicate test.
	 *
	 * @param test The predicate to use to test the value.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R passes(Predicate<Long> test) throws AssertionError {
		if (! test.test(value))
			throw new BasicAssertionError("Value did not pass predicate test.\n\tValue=[{0}]", value);
		return returns;
	}
}
