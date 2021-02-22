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
package org.apache.juneau.rest.client.assertion;

import org.apache.juneau.assertions.*;
import org.apache.juneau.http.exception.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.client.*;

/**
 * Used for fluent assertion calls against {@link ResponseHeader} objects.
 *
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentResponseHeaderAssertion<R>")
public class FluentResponseHeaderAssertion<R> extends FluentBaseAssertion<String,R> {

	private final ResponseHeader value;

	/**
	 * Constructor.
	 *
	 * @param value The object being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentResponseHeaderAssertion(ResponseHeader value, R returns) {
		super(null, value.getValue(), returns);
		this.value = value;
		throwable(BadRequest.class);
	}

	/**
	 * Converts this object assertion into a boolean assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not a boolean.
	 */
	public FluentBooleanAssertion<R> asBoolean() {
		return new FluentBooleanAssertion<>(this, value.asBoolean().orElse(null), returns());
	}

	/**
	 * Converts this object assertion into a date assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not a date.
	 */
	public FluentDateAssertion<R> asDate() {
		return new FluentDateAssertion<>(this, value.asDateHeader().asDate().orElse(null), returns());
	}

	/**
	 * Converts this object assertion into an integer assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not an integer.
	 */
	public FluentIntegerAssertion<R> asInteger() {
		return new FluentIntegerAssertion<>(this, value.asInteger().orElse(null), returns());
	}

	/**
	 * Converts this object assertion into a long assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not a long.
	 */
	public FluentLongAssertion<R> asLong() {
		return new FluentLongAssertion<>(this, value.asLong().orElse(null), returns());
	}

	/**
	 * Converts this object assertion into a zoned-datetime assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not a zoned-datetime.
	 */
	public FluentZonedDateTimeAssertion<R> asZonedDateTime() {
		return new FluentZonedDateTimeAssertion<>(this, value.asDateHeader().asZonedDateTime().orElse(null), returns());
	}

	/**
	 * Provides the ability to perform fluent-style assertions on this response body.
	 *
	 * <p>
	 * Converts the body to a type using {@link ResponseBody#asType(Class)} and then returns the value as an object assertion.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the response body bean is the expected value.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<js>"/myBean"</js>)
	 * 		.run()
	 * 		.assertBody().asType(MyBean.<jk>class</jk>).json().is(<js>"{foo:'bar'}"</js>);
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If no charset was found on the <code>Content-Type</code> response header, <js>"UTF-8"</js> is assumed.
	 *  <li>
	 *		When using this method, the body is automatically cached by calling the {@link ResponseBody#cache()}.
	 * 	<li>
	 * 		The input stream is automatically closed after this call.
	 * </ul>
	 *
	 * @param type The object type to create.
	 * @return A new fluent assertion object.
	 * @throws RestCallException If REST call failed.
	 */
	public <V> FluentObjectAssertion<V,R> asType(Class<V> type) throws RestCallException {
		return new FluentObjectAssertion<>(value.asType(type).orElse(null), returns());
	}

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public FluentResponseHeaderAssertion<R> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentResponseHeaderAssertion<R> stderr() {
		super.stderr();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentResponseHeaderAssertion<R> stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentResponseHeaderAssertion<R> throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	// </FluentSetters>
}
