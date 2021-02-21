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

import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;

/**
 * Used for fluent assertion calls against POJOs.
 *
 * @param <V> The object type.
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentObjectAssertion<V,R>")
public class FluentObjectAssertion<V,R> extends FluentBaseAssertion<V,R> {

	private final Object value;

	/**
	 * Constructor.
	 *
	 * @param value The object being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentObjectAssertion(V value, R returns) {
		this(null, value, returns);
	}

	/**
	 * Constructor.
	 *
	 * @param creator The assertion that created this assertion.
	 * @param value The object being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentObjectAssertion(Assertion creator, V value, R returns) {
		super(creator, value, returns);
		this.value = value;
	}

	@SuppressWarnings("unchecked")
	private <T> T cast(Class<T> c) throws AssertionError {
		Object o = value;
		if (value == null || c.isInstance(value))
			return (T)o;
		throw new BasicAssertionError("Object was not type ''{0}''.  Actual=''{1}''", c, o.getClass());
	}

	/**
	 * Converts this object assertion into an array assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not an array.
	 */
	public FluentArrayAssertion<R> asArray() throws AssertionError {
		return new FluentArrayAssertion<>(this, value, returns());
	}

	/**
	 * Converts this object assertion into a boolean assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not a boolean.
	 */
	public FluentBooleanAssertion<R> asBoolean() {
		return new FluentBooleanAssertion<>(this, cast(Boolean.class), returns());
	}

	/**
	 * Converts this object assertion into a byte array assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not a byte array.
	 */
	public FluentByteArrayAssertion<R> asByteArray() {
		return new FluentByteArrayAssertion<>(this, cast(byte[].class), returns());
	}

	/**
	 * Converts this object assertion into a collection assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not a collection.
	 */
	public FluentCollectionAssertion<R> asCollection() {
		return new FluentCollectionAssertion<>(this, cast(Collection.class), returns());
	}

	/**
	 * Converts this object assertion into a comparable object assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not an instance of {@link Comparable}.
	 */
	public FluentComparableAssertion<Comparable<?>,R> asComparable() {
		return new FluentComparableAssertion<>(this, cast(Comparable.class), returns());
	}

	/**
	 * Converts this object assertion into a date assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not a date.
	 */
	public FluentDateAssertion<R> asDate() {
		return new FluentDateAssertion<>(this, cast(Date.class), returns());
	}

	/**
	 * Converts this object assertion into an integer assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not an integer.
	 */
	public FluentIntegerAssertion<R> asInteger() {
		return new FluentIntegerAssertion<>(this, cast(Integer.class), returns());
	}

	/**
	 * Converts this object assertion into a list assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not a list.
	 */
	public FluentListAssertion<R> asList() {
		return new FluentListAssertion<>(this, cast(List.class), returns());
	}

	/**
	 * Converts this object assertion into a long assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not a long.
	 */
	public FluentLongAssertion<R> asLong() {
		return new FluentLongAssertion<>(this, cast(Long.class), returns());
	}

	/**
	 * Converts this object assertion into a map assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not a map.
	 */
	public FluentMapAssertion<R> asMap() {
		return new FluentMapAssertion<>(this, cast(Map.class), returns());
	}

	/**
	 * Converts this object assertion into a zoned-datetime assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not a zoned-datetime.
	 */
	public FluentZonedDateTimeAssertion<R> asZonedDateTime() {
		return new FluentZonedDateTimeAssertion<>(this, cast(ZonedDateTime.class), returns());
	}

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public FluentObjectAssertion<V,R> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentObjectAssertion<V,R> stderr() {
		super.stderr();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentObjectAssertion<V,R> stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentObjectAssertion<V,R> throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	// </FluentSetters>
}
