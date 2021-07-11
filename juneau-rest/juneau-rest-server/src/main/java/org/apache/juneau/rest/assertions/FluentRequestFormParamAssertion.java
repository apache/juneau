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
 * Used for fluent assertion calls against {@link RequestFormParam} objects.
 *
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentRequestFormParamAssertion<R>")
public class FluentRequestFormParamAssertion<R> extends FluentObjectAssertion<RequestFormParam,R> {

	/**
	 * Constructor.
	 *
	 * @param value The object being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentRequestFormParamAssertion(RequestFormParam value, R returns) {
		this(null, value, returns);
		throwable(BadRequest.class);
	}

	/**
	 * Constructor.
	 *
	 * @param creator The assertion that created this assertion.
	 * @param value The object being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentRequestFormParamAssertion(Assertion creator, RequestFormParam value, R returns) {
		super(creator, value, returns);
	}

	/**
	 * Converts this object assertion into a string assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not a string.
	 */
	@Override
	public FluentStringAssertion<R> asString() {
		return new FluentStringAssertion<>(this, value().asString().orElse(null), returns());
	}

	/**
	 * Converts this object assertion into a boolean assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not a boolean.
	 */
	@Override
	public FluentBooleanAssertion<R> asBoolean() {
		return new FluentBooleanAssertion<>(this, value().asBoolean().orElse(null), returns());
	}

	/**
	 * Converts this object assertion into a date assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not a date.
	 */
	@Override
	public FluentDateAssertion<R> asDate() {
		return new FluentDateAssertion<>(this, value().asDatePart().asDate().orElse(null), returns());
	}

	/**
	 * Converts this object assertion into an integer assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not an integer.
	 */
	@Override
	public FluentIntegerAssertion<R> asInteger() {
		return new FluentIntegerAssertion<>(this, value().asInteger().orElse(null), returns());
	}

	/**
	 * Converts this object assertion into a long assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not a long.
	 */
	@Override
	public FluentLongAssertion<R> asLong() {
		return new FluentLongAssertion<>(this, value().asLong().orElse(null), returns());
	}

	/**
	 * Converts this object assertion into a zoned-datetime assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not a zoned-datetime.
	 */
	@Override
	public FluentZonedDateTimeAssertion<R> asZonedDateTime() {
		return new FluentZonedDateTimeAssertion<>(this, value().asDatePart().asZonedDateTime().orElse(null), returns());
	}

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public FluentRequestFormParamAssertion<R> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentRequestFormParamAssertion<R> out(PrintStream value) {
		super.out(value);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentRequestFormParamAssertion<R> silent() {
		super.silent();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentRequestFormParamAssertion<R> stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentRequestFormParamAssertion<R> throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	// </FluentSetters>
}
