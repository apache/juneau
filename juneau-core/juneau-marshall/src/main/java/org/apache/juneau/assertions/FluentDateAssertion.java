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
import static org.apache.juneau.internal.ObjectUtils.*;

import java.io.*;
import java.time.temporal.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;

/**
 * Used for fluent assertion calls against dates.
 * {@review}
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Validates the response expiration is after the current date.</jc>
 * 	<jv>client</jv>
 * 		.get(<jsf>URL</jsf>)
 * 		.run()
 * 		.assertDateHeader(<js>"Expires"</js>).isAfterNow();
 * </p>
 *
 *
 * <h5 class='topic'>Test Methods</h5>
 * 	<ul>
 * 		<li class='jm'>{@link FluentDateAssertion#is(Date,ChronoUnit)}
 * 		<li class='jm'>{@link FluentDateAssertion#isAfter(Date)}
 * 		<li class='jm'>{@link FluentDateAssertion#isAfterNow()}
 * 		<li class='jm'>{@link FluentDateAssertion#isBefore(Date)}
 * 		<li class='jm'>{@link FluentDateAssertion#isBeforeNow()}
 * 		<li class='jm'>{@link FluentDateAssertion#isBetween(Date,Date)}
 * 		<li class='jm'>{@link FluentComparableAssertion#isGt(Comparable)}
 * 		<li class='jm'>{@link FluentComparableAssertion#isGte(Comparable)}
 * 		<li class='jm'>{@link FluentComparableAssertion#isLt(Comparable)}
 * 		<li class='jm'>{@link FluentComparableAssertion#isLte(Comparable)}
 * 		<li class='jm'>{@link FluentComparableAssertion#isBetween(Comparable,Comparable)}
 * 		<li class='jm'>{@link FluentObjectAssertion#exists()}
 * 		<li class='jm'>{@link FluentObjectAssertion#is(Object)}
 * 		<li class='jm'>{@link FluentObjectAssertion#is(Predicate)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isNot(Object)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isAny(Object...)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isNotAny(Object...)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isNull()}
 * 		<li class='jm'>{@link FluentObjectAssertion#isNotNull()}
 * 		<li class='jm'>{@link FluentObjectAssertion#isString(String)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isJson(String)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isSame(Object)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isSameJsonAs(Object)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isSameSortedJsonAs(Object)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isSameSerializedAs(Object, WriterSerializer)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isType(Class)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isExactType(Class)}
 * 	</ul>
 *
 * <h5 class='topic'>Transform Methods</h5>
 * 	<ul>
 * 		<li class='jm'>{@link FluentDateAssertion#asEpochMillis()}
 * 		<li class='jm'>{@link FluentDateAssertion#asEpochSeconds()}
 * 		<li class='jm'>{@link FluentObjectAssertion#asString()}
 * 		<li class='jm'>{@link FluentObjectAssertion#asString(WriterSerializer)}
 * 		<li class='jm'>{@link FluentObjectAssertion#asString(Function)}
 * 		<li class='jm'>{@link FluentObjectAssertion#asJson()}
 * 		<li class='jm'>{@link FluentObjectAssertion#asJsonSorted()}
 * 		<li class='jm'>{@link FluentObjectAssertion#apply(Function)}
 * 		<li class='jm'>{@link FluentObjectAssertion#asAny()}
 *	</ul>
 *
 * <h5 class='topic'>Configuration Methods</h5>
 * 	<ul>
 * 		<li class='jm'>{@link Assertion#msg(String, Object...)}
 * 		<li class='jm'>{@link Assertion#out(PrintStream)}
 * 		<li class='jm'>{@link Assertion#silent()}
 * 		<li class='jm'>{@link Assertion#stdout()}
 * 		<li class='jm'>{@link Assertion#throwable(Class)}
 * 	</ul>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jm.FluentAssertions}
 * 	<li class='extlink'>{@source}
 * </ul>
 *
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentDateAssertion<R>")
public class FluentDateAssertion<R> extends FluentComparableAssertion<Date,R> {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final Messages MESSAGES = Messages.of(FluentDateAssertion.class, "Messages");
	private static final String
		MSG_unexpectedValue = MESSAGES.getString("unexpectedValue"),
		MSG_valueWasNotAfterExpected = MESSAGES.getString("valueWasNotAfterExpected"),
		MSG_valueWasNotBeforeExpected = MESSAGES.getString("valueWasNotBeforeExpected");

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 * @param returns
	 * 	The object to return after a test method is called.
	 * 	<br>If <jk>null</jk>, the test method returns this object allowing multiple test method calls to be
	 * used on the same assertion.
	 */
	public FluentDateAssertion(Date value, R returns) {
		this(null, value, returns);
	}

	/**
	 * Chained constructor.
	 *
	 * <p>
	 * Used when transforming one assertion into another so that the assertion config can be used by the new assertion.
	 *
	 * @param creator
	 * 	The assertion that created this assertion.
	 * 	<br>Should be <jk>null</jk> if this is the top-level assertion.
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 * @param returns
	 * 	The object to return after a test method is called.
	 * 	<br>If <jk>null</jk>, the test method returns this object allowing multiple test method calls to be
	 * used on the same assertion.
	 */
	public FluentDateAssertion(Assertion creator, Date value, R returns) {
		super(creator, value, returns);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Transform methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns an long assertion on the epoch milliseconds of this date.
	 *
	 * <p>
	 * If the date is <jk>null</jk>, the returned assertion is a null assertion
	 * (meaning {@link FluentLongAssertion#exists()} returns <jk>false</jk>).
	 *
	 * @return A new assertion.
	 */
	public FluentLongAssertion<R> asEpochMillis() {
		return new FluentLongAssertion<>(this, valueIsNull() ? null : value().getTime(), returns());
	}

	/**
	 * Returns an long assertion on the epoch seconds of this date.
	 *
	 * <p>
	 * If the date is <jk>null</jk>, the returned assertion is a null assertion
	 * (meaning {@link FluentLongAssertion#exists()} returns <jk>false</jk>).
	 *
	 * @return A new assertion.
	 */
	public FluentLongAssertion<R> asEpochSeconds() {
		return new FluentLongAssertion<>(this, valueIsNull() ? null : value().getTime() / 1000, returns());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Asserts that the value equals the specified value at the specified precision.
	 *
	 * @param value The value to check against.
	 * @param precision The precision (e.g. {@link ChronoUnit#SECONDS}.
	 * @return The fluent return object.
	 * @throws AssertionError If assertion failed.
	 */
	public R is(Date value, ChronoUnit precision) throws AssertionError {
		if (ne(value(), value, (x,y)->x.toInstant().truncatedTo(precision).equals(y.toInstant().truncatedTo(precision))))
			throw error(MSG_unexpectedValue, value, value());
		return returns();
	}

	/**
	 * Asserts that the value is after the specified value.
	 *
	 * @param value The values to check against.
	 * @return The fluent return object.
	 * @throws AssertionError If assertion failed.
	 */
	public R isAfter(Date value) throws AssertionError {
		assertArgNotNull("value", value);
		if (! (value().after(value)))
			throw error(MSG_valueWasNotAfterExpected, value, value());
		return returns();
	}

	/**
	 * Asserts that the value is after the current date.
	 *
	 * @return The fluent return object.
	 * @throws AssertionError If assertion failed.
	 */
	public R isAfterNow() throws AssertionError {
		return isAfter(new Date());
	}

	/**
	 * Asserts that the value is before the specified value.
	 *
	 * @param value The values to check against.
	 * @return The fluent return object.
	 * @throws AssertionError If assertion failed.
	 */
	public R isBefore(Date value) throws AssertionError {
		assertArgNotNull("value", value);
		if (! (value().before(value)))
			throw error(MSG_valueWasNotBeforeExpected, value, value());
		return returns();
	}

	/**
	 * Asserts that the value is before the current date.
	 *
	 * @return The fluent return object.
	 * @throws AssertionError If assertion failed.
	 */
	public R isBeforeNow() throws AssertionError {
		return isBefore(new Date());
	}

	/**
	 * Asserts that the value is between (inclusive) the specified upper and lower values.
	 *
	 * @param lower The lower value to check against.
	 * @param upper The upper value to check against.
	 * @return The fluent return object.
	 * @throws AssertionError If assertion failed.
	 */
	public R isBetween(Date lower, Date upper) throws AssertionError {
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

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentDateAssertion<R> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentDateAssertion<R> out(PrintStream value) {
		super.out(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentDateAssertion<R> silent() {
		super.silent();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentDateAssertion<R> stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentDateAssertion<R> throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	// </FluentSetters>
}
