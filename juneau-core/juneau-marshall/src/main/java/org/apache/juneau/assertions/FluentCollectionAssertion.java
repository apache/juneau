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

import java.io.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;

/**
 * Used for fluent assertion calls against collections objects.
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
	 * @param contents The byte array being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentCollectionAssertion(Collection<E> contents, R returns) {
		this(null, contents, returns);
	}

	/**
	 * Constructor.
	 *
	 * @param creator The assertion that created this assertion.
	 * @param contents The byte array being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentCollectionAssertion(Assertion creator, Collection<E> contents, R returns) {
		super(creator, contents, returns);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Transform methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* FluentObjectAssertion */
	public FluentCollectionAssertion<E,R> apply(Function<Collection<E>,Collection<E>> function) {
		return new FluentCollectionAssertion<>(this, function.apply(orElse(null)), returns());
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
	 * Asserts that at least one value in the collection passes the specified test.
	 *
	 * @param test The predicate test.
	 * @return The object to return after the test.
	 * @throws AssertionError If assertion failed or value was <jk>null</jk>.
	 */
	public R any(Predicate<E> test) throws AssertionError {
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
		for (E v : value())
			if (! test.test(v))
				throw error(MSG_collectionDidNotContainTestedValue, value());
		return returns();
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
	 * Asserts that the collection exists and is the specified size.
	 *
	 * @param size The expected size.
	 * @return The object to return after the test.
	 * @throws AssertionError If assertion failed or value was <jk>null</jk>.
	 */
	public R isSize(int size) throws AssertionError {
		if (size() != size)
			throw error(MSG_collectionDidNotHaveExpectedSize, size, size());
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
	protected int size() throws AssertionError {
		return value().size();
	}
}
