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
import java.time.*;
import java.time.temporal.*;
import java.util.function.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;

/**
 * Used for assertion calls against {@link ZonedDateTime} objects.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Validates the specified date is after the current date.</jc>
 * 	<jsm>assertZonedDateTime</jsm>(<jv>myZdt</jv>).isAfterNow();
 * </p>
 *
 * <ul>
 * 	<li>Test methods:
 * 	<ul>
 * 		<li class='jm'>{@link FluentZonedDateTimeAssertion#is(ZonedDateTime,ChronoUnit)}
 * 		<li class='jm'>{@link FluentZonedDateTimeAssertion#isAfter(ZonedDateTime)}
 * 		<li class='jm'>{@link FluentZonedDateTimeAssertion#isAfterNow()}
 * 		<li class='jm'>{@link FluentZonedDateTimeAssertion#isBefore(ZonedDateTime)}
 * 		<li class='jm'>{@link FluentZonedDateTimeAssertion#isBeforeNow()}
 * 		<li class='jm'>{@link FluentZonedDateTimeAssertion#isBetween(ZonedDateTime,ZonedDateTime)}
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
 * 	<li>Transform methods:
 * 		<li class='jm'>{@link FluentObjectAssertion#asString()}
 * 		<li class='jm'>{@link FluentObjectAssertion#asString(WriterSerializer)}
 * 		<li class='jm'>{@link FluentObjectAssertion#asString(Function)}
 * 		<li class='jm'>{@link FluentObjectAssertion#asJson()}
 * 		<li class='jm'>{@link FluentObjectAssertion#asJsonSorted()}
 * 		<li class='jm'>{@link FluentObjectAssertion#apply(Function)}
 * 		<li class='jm'>{@link FluentObjectAssertion#asAny()}
 *	</ul>
 * 	<li>Configuration methods:
 * 	<ul>
 * 		<li class='jm'>{@link Assertion#msg(String, Object...)}
 * 		<li class='jm'>{@link Assertion#out(PrintStream)}
 * 		<li class='jm'>{@link Assertion#silent()}
 * 		<li class='jm'>{@link Assertion#stdout()}
 * 		<li class='jm'>{@link Assertion#throwable(Class)}
 * 	</ul>
 * </ul>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc Assertions}
 * </ul>
 */
@FluentSetters(returns="ZonedDateTimeAssertion")
public class ZonedDateTimeAssertion extends FluentZonedDateTimeAssertion<ZonedDateTimeAssertion> {

	//-----------------------------------------------------------------------------------------------------------------
	// Constructors
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Creator.
	 *
	 * @param value The date being wrapped.
	 * @return A new {@link ZonedDateTimeAssertion} object.
	 */
	public static ZonedDateTimeAssertion create(ZonedDateTime value) {
		return new ZonedDateTimeAssertion(value);
	}

	/**
	 * Creator.
	 *
	 * @param value The date being wrapped.
	 */
	public ZonedDateTimeAssertion(ZonedDateTime value) {
		super(value, null);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fluent setters
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public ZonedDateTimeAssertion msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public ZonedDateTimeAssertion out(PrintStream value) {
		super.out(value);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public ZonedDateTimeAssertion silent() {
		super.silent();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public ZonedDateTimeAssertion stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public ZonedDateTimeAssertion throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	// </FluentSetters>
}
