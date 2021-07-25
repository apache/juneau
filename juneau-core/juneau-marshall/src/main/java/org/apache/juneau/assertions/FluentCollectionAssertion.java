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

import static org.apache.juneau.internal.ObjectUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.io.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;

/**
 * Used for fluent assertion calls against collections objects.
 *
 *
 * <h5 class='topic'>Test Methods</h5>
 * 	<ul>
 * 		<li class='jm'>{@link FluentCollectionAssertion#isEmpty()}
 * 		<li class='jm'>{@link FluentCollectionAssertion#isNotEmpty()}
 * 		<li class='jm'>{@link FluentCollectionAssertion#contains(Object)}
 * 		<li class='jm'>{@link FluentCollectionAssertion#doesNotContain(Object)}
 * 		<li class='jm'>{@link FluentCollectionAssertion#any(Predicate)}
 * 		<li class='jm'>{@link FluentCollectionAssertion#all(Predicate)}
 * 		<li class='jm'>{@link FluentCollectionAssertion#isSize(int size)}
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
 * 		<li class='jm'>{@link FluentCollectionAssertion#asStrings()}
 * 		<li class='jm'>{@link FluentCollectionAssertion#size()}
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
 * 	<li class='link'>{@doc Assertions}
 * </ul>
 *
 * @param <E> The element type.
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentCollectionAssertion<E,R>")
public class FluentCollectionAssertion<E,R> extends FluentObjectAssertion<Collection<E>,R> {

	private static final Messages MESSAGES = Messages.of(FluentCollectionAssertion.class, "Messages");
	private static final String
		MSG_collectionWasNotEmpty = MESSAGES.getString("collectionWasNotEmpty"),
		MSG_collectionDidNotContainExpectedValue = MESSAGES.getString("collectionDidNotContainExpectedValue"),
		MSG_collectionDidNotContainTestedValue = MESSAGES.getString("collectionDidNotContainTestedValue"),
		MSG_collectionContainedUnexpectedValue = MESSAGES.getString("collectionContainedUnexpectedValue"),
		MSG_collectionWasEmpty = MESSAGES.getString("collectionWasEmpty"),
		MSG_collectionDidNotHaveExpectedSize = MESSAGES.getString("collectionDidNotHaveExpectedSize");

	//-----------------------------------------------------------------------------------------------------------------
	// Constructors
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
	public FluentCollectionAssertion(Collection<E> value, R returns) {
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
	public FluentCollectionAssertion(Assertion creator, Collection<E> value, R returns) {
		super(creator, value, returns);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Transform methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* FluentObjectAssertion */
	public FluentCollectionAssertion<E,R> apply(Function<Collection<E>,Collection<E>> function) {
		return new FluentCollectionAssertion<>(this, function.apply(orElse(null)), returns());
	}

	/**
	 * Converts this assertion into a {@link FluentListAssertion} of strings.
	 *
	 * @return A new fluent string assertion.
	 */
	public FluentStringListAssertion<R> asStrings() {
		return new FluentStringListAssertion<>(this, valueIsNull() ? null : value().stream().map(x -> stringify(x)).collect(Collectors.toList()), returns());
	}


	/**
	 * Returns an integer assertion on the size of this collection.
	 *
	 * <p>
	 * If the collection is <jk>null</jk>, the returned assertion is a null assertion
	 * (meaning {@link FluentIntegerAssertion#exists()} returns <jk>false</jk>).
	 *
	 * @return A new assertion.
	 */
	public FluentIntegerAssertion<R> size() {
		return new FluentIntegerAssertion<>(this, valueIsNull() ? null : value().size(), returns());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Asserts that the collection exists and is empty.
	 *
	 * @return The object to return after the test.
	 * @throws AssertionError If assertion failed or value was <jk>null</jk>.
	 */
	public R isEmpty() throws AssertionError {
		if (! value().isEmpty())
			throw error(MSG_collectionWasNotEmpty);
		return returns();
	}

	/**
	 * Asserts that the collection exists and is not empty.
	 *
	 * @return The object to return after the test.
	 * @throws AssertionError If assertion failed or value was <jk>null</jk>.
	 */
	public R isNotEmpty() throws AssertionError {
		if (value().isEmpty())
			throw error(MSG_collectionWasEmpty);
		return returns();
	}

	/**
	 * Asserts that the collection contains the expected value.
	 *
	 * @param entry The value to check for.
	 * @return The object to return after the test.
	 * @throws AssertionError If assertion failed or value was <jk>null</jk>.
	 */
	public R contains(E entry) throws AssertionError {
		for (Object v : value())
			if (eq(v, entry))
				return returns();
		throw error(MSG_collectionDidNotContainExpectedValue, entry, value());
	}

	/**
	 * Asserts that the collection contains the expected value.
	 *
	 * @param entry The value to check for.
	 * @return The object to return after the test.
	 * @throws AssertionError If assertion failed or value was <jk>null</jk>.
	 */
	public R doesNotContain(E entry) throws AssertionError {
		for (Object v : value())
			if (eq(v, entry))
				throw error(MSG_collectionContainedUnexpectedValue, entry, value());
		return returns();
	}

	/**
	 * Asserts that at least one value in the collection passes the specified test.
	 *
	 * @param test The predicate test.
	 * @return The object to return after the test.
	 * @throws AssertionError If assertion failed or value was <jk>null</jk>.
	 */
	public R any(Predicate<E> test) throws AssertionError {
		if (test == null)
			return returns();
		for (E v : value())
			if (test.test(v))
				return returns();
		throw error(MSG_collectionDidNotContainTestedValue, value());
	}

	/**
	 * Asserts that all values in the collection pass the specified test.
	 *
	 * @param test The predicate test.
	 * @return The object to return after the test.
	 * @throws AssertionError If assertion failed or value was <jk>null</jk>.
	 */
	public R all(Predicate<E> test) throws AssertionError {
		if (test == null)
			return returns();
		for (E v : value())
			if (! test.test(v))
				throw error(MSG_collectionDidNotContainTestedValue, value());
		return returns();
	}

	/**
	 * Asserts that the collection exists and is the specified size.
	 *
	 * @param size The expected size.
	 * @return The object to return after the test.
	 * @throws AssertionError If assertion failed or value was <jk>null</jk>.
	 */
	public R isSize(int size) throws AssertionError {
		if (getSize() != size)
			throw error(MSG_collectionDidNotHaveExpectedSize, size, getSize());
		return returns();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fluent setters
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public FluentCollectionAssertion<E,R> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentCollectionAssertion<E,R> out(PrintStream value) {
		super.out(value);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentCollectionAssertion<E,R> silent() {
		super.silent();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentCollectionAssertion<E,R> stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentCollectionAssertion<E,R> throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	// </FluentSetters>

	//-----------------------------------------------------------------------------------------------------------------
	// Utility methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the size of this collection if it is not <jk>null</jk>.
	 *
	 * @return the size of this collection if it is not <jk>null</jk>.
	 * @throws AssertionError If value was <jk>null</jk>.
	 */
	protected int getSize() throws AssertionError {
		return value().size();
	}
}
