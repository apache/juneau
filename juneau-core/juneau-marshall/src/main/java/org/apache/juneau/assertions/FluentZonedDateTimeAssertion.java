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
import java.time.*;
import java.time.temporal.*;

import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;

/**
 * Used for fluent assertion calls against {@link ZonedDateTime} objects.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Validates the response expiration is after the current date.</jc>
 * 	<jv>client</jv>
 * 		.get(<jsf>URL</jsf>)
 * 		.run()
 * 		.getHeader(<js>"Expires"</js>).asDateHeader().assertZonedDateTime().isAfterNow();
 * </p>
 *
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentZonedDateTimeAssertion<R>")
public class FluentZonedDateTimeAssertion<R> extends FluentComparableAssertion<ZonedDateTime,R> {

	private static final Messages MESSAGES = Messages.of(FluentZonedDateTimeAssertion.class, "Messages");
	private static final String
		MSG_unexpectedValue = MESSAGES.getString("unexpectedValue"),
		MSG_valueWasNotAfterExpected = MESSAGES.getString("valueWasNotAfterExpected"),
		MSG_valueWasNotBeforeExpected = MESSAGES.getString("valueWasNotBeforeExpected");

	//-----------------------------------------------------------------------------------------------------------------
	// Constructors
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param value The value being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentZonedDateTimeAssertion(ZonedDateTime value, R returns) {
		this(null, value, returns);
	}

	/**
	 * Constructor.
	 *
	 * @param creator The assertion that created this assertion.
	 * @param value The value being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentZonedDateTimeAssertion(Assertion creator, ZonedDateTime value, R returns) {
		super(creator, value, returns);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Asserts that the value equals the specified value at the specified precision.
	 *
	 * @param value The value to check against.
	 * @param precision The precision (e.g. {@link ChronoUnit#SECONDS}.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R is(ZonedDateTime value, ChronoUnit precision) throws AssertionError {
		assertArgNotNull("precision", precision);
		ZonedDateTime v = orElse(null);
		if (valueIsNull() && value == null)
			return returns();
		if (valueIsNotNull() && value != null) {
			Duration d = Duration.between(value(), value);
			if (d.compareTo(precision.getDuration()) <= 0)
				return returns();
		}
		throw error(MSG_unexpectedValue, value, v);
	}

	/**
	 * Asserts that the value is after the specified value.
	 *
	 * @param value The values to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isAfter(ZonedDateTime value) throws AssertionError {
		assertArgNotNull("value", value);
		if (! (value().isAfter(value)))
			throw error(MSG_valueWasNotAfterExpected, value, value());
		return returns();
	}

	/**
	 * Asserts that the value is after the current date.
	 *
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isAfterNow() throws AssertionError {
		return isAfter(ZonedDateTime.now());
	}

	/**
	 * Asserts that the value is before the specified value.
	 *
	 * @param value The values to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isBefore(ZonedDateTime value) throws AssertionError {
		assertArgNotNull("value", value);
		if (! (value().isBefore(value)))
			throw error(MSG_valueWasNotBeforeExpected, value, value());
		return returns();
	}

	/**
	 * Asserts that the value is before the current date.
	 *
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isBeforeNow() throws AssertionError {
		return isBefore(ZonedDateTime.now());
	}

	/**
	 * Asserts that the value is between (inclusive) the specified upper and lower values.
	 *
	 * @param lower The lower value to check against.
	 * @param upper The upper value to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isBetween(ZonedDateTime lower, ZonedDateTime upper) throws AssertionError {
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
	public FluentZonedDateTimeAssertion<R> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentZonedDateTimeAssertion<R> out(PrintStream value) {
		super.out(value);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentZonedDateTimeAssertion<R> silent() {
		super.silent();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentZonedDateTimeAssertion<R> stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentZonedDateTimeAssertion<R> throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	// </FluentSetters>
}
