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

import static org.apache.juneau.assertions.Assertions.*;

import java.io.*;
import java.util.function.*;

import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;

/**
 * Used for fluent assertion calls against comparable objects.
 *
 * @param <T> The value type
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentComparableAssertion<T,R>")
@SuppressWarnings("rawtypes")
public class FluentComparableAssertion<T extends Comparable,R> extends FluentObjectAssertion<T,R> {

	private static final Messages MESSAGES = Messages.of(FluentComparableAssertion.class, "Messages");
	private static final String
		MSG_valueWasNotGreaterThanExpected = MESSAGES.getString("valueWasNotGreaterThanExpected"),
		MSG_valueWasNotGreaterOrEqualsToExpected = MESSAGES.getString("valueWasNotGreaterOrEqualsToExpected"),
		MSG_valueWasNotLessThanExpected = MESSAGES.getString("valueWasNotLessThanExpected"),
		MSG_valueWasNotLessOrEqualsToExpected = MESSAGES.getString("valueWasNotLessOrEqualsToExpected");

	//-----------------------------------------------------------------------------------------------------------------
	// Constructors
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param value The value being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentComparableAssertion(T value, R returns) {
		this(null, value, returns);
	}

	/**
	 * Constructor.
	 *
	 * @param creator The assertion that created this assertion.
	 * @param value The value being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentComparableAssertion(Assertion creator, T value, R returns) {
		super(creator, value, returns);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Transform methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* FluentObjectAssertion */
	public FluentComparableAssertion<T,R> apply(Function<T,T> function) {
		return new FluentComparableAssertion<>(this, function.apply(orElse(null)), returns());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Asserts that the value is greater than the specified value.
	 *
	 * @param value The values to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isGt(Comparable value) throws AssertionError {
		assertArgNotNull("value", value);
		if (value().compareTo(value) <= 0)
			throw error(MSG_valueWasNotGreaterThanExpected, value, value());
		return returns();
	}

	/**
	 * Asserts that the value is greater than or equal to the specified value.
	 *
	 * @param value The values to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isGte(Comparable value) throws AssertionError {
		assertArgNotNull("value", value);
		if (value().compareTo(value) < 0)
				throw error(MSG_valueWasNotGreaterOrEqualsToExpected, value, value());
		return returns();
	}

	/**
	 * Asserts that the value is less than the specified value.
	 *
	 * @param value The values to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isLt(Comparable value) throws AssertionError {
		assertArgNotNull("value", value);
		if (value().compareTo(value) >= 0)
				throw error(MSG_valueWasNotLessThanExpected, value, value());
		return returns();
	}

	/**
	 * Asserts that the value is less than or equals to the specified value.
	 *
	 * @param value The values to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isLte(Comparable value) throws AssertionError {
		assertArgNotNull("value", value);
		if (value().compareTo(value) > 0)
				throw error(MSG_valueWasNotLessOrEqualsToExpected, value, value());
		return returns();
	}

	/**
	 * Asserts that the value is between (inclusive) the specified upper and lower values.
	 *
	 * @param lower The lower value to check against.
	 * @param upper The upper value to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isBetween(Comparable lower, Comparable upper) throws AssertionError {
		exists();
		assertArgNotNull("lower", lower);
		assertArgNotNull("upper", upper);
		isLte(upper);
		isGte(lower);
		return returns();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fluent setters
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public FluentComparableAssertion<T,R> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentComparableAssertion<T,R> out(PrintStream value) {
		super.out(value);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentComparableAssertion<T,R> silent() {
		super.silent();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentComparableAssertion<T,R> stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentComparableAssertion<T,R> throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	// </FluentSetters>
}
