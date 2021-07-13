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

import static java.util.Arrays.*;

import java.io.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;

/**
 * Used for fluent assertion calls against lists.
 *
 * @param <E> The element type.
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentListAssertion<E,R>")
public class FluentListAssertion<E,R> extends FluentCollectionAssertion<E,R> {

	private static final Messages MESSAGES = Messages.of(FluentListAssertion.class, "Messages");
	private static final String
		MSG_listDidNotContainExpectedValueAt = MESSAGES.getString("listDidNotContainExpectedValueAt");

	//-----------------------------------------------------------------------------------------------------------------
	// Constructors
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param contents The byte array being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentListAssertion(List<E> contents, R returns) {
		this(null, contents, returns);
	}

	/**
	 * Constructor.
	 *
	 * @param creator The assertion that created this assertion.
	 * @param contents The byte array being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentListAssertion(Assertion creator, List<E> contents, R returns) {
		super(creator, contents, returns);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Transform methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Applies a transform on the inner object and returns a new inner object.
	 *
	 * @param function The transform to apply.
	 * @return A new assertion.
	 */
	public FluentListAssertion<E,R> apply2(Function<List<E>,List<E>> function) {
		return new FluentListAssertion<>(this, function.apply((List<E>)orElse(null)), returns());
	}

	/**
	 * Returns an object assertion on the item specified at the specified index.
	 *
	 * <p>
	 * If the list is <jk>null</jk> or the index is out-of-bounds, the returned assertion is a null assertion
	 * (meaning {@link FluentObjectAssertion#exists()} returns <jk>false</jk>).
	 *
	 * @param index The index of the item to retrieve from the list.
	 * @return A new assertion.
	 */
	public FluentObjectAssertion<E,R> item(int index) {
		return new FluentObjectAssertion<>(this, at(index), returns());
	}

	/**
	 * Sorts the entries in this list.
	 *
	 * @return A new list assertion.  The contents of the original list remain unchanged.
	 */
	public FluentListAssertion<E,R> sorted() {
		return new FluentListAssertion<>(this, toSortedList(null), returns());
	}

	/**
	 * Sorts the entries in this list using the specified comparator.
	 *
	 * @param comparator The comparator to use to sort the list.
	 * @return A new list assertion.  The contents of the original list remain unchanged.
	 */
	public FluentListAssertion<E,R> sorted(Comparator<E> comparator) {
		return new FluentListAssertion<>(this, toSortedList(comparator), returns());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Asserts that the contents of this list contain the specified values when each entry is converted to a string.
	 *
	 * @param entries The expected entries in this list.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R equals(String...entries) throws AssertionError {
		Predicate<E>[] p = stream(entries).map(AssertionPredicates::eq).toArray(Predicate[]::new);
 		return each(p);
	}

	/**
	 * Asserts that the contents of this list contain the specified values when each entry is converted to a string.
	 *
	 * <p>
	 * Equivalent to {@link #equals(String...)}
	 *
	 * @param entries The expected entries in this list.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R is(String...entries) throws AssertionError {
		return equals(entries);
	}

	/**
	 * Asserts that the contents of this list contain the specified values when each entry is converted to a string.
	 *
	 * @param entries The expected entries in this list.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	@SuppressWarnings("unchecked")
	public R equals(E...entries) throws AssertionError {
		Predicate<E>[] p = stream(entries).map(AssertionPredicates::eq).toArray(Predicate[]::new);
 		return each(p);
	}

	/**
	 * Asserts that the contents of this list contain the specified values.
	 *
	 * <p>
	 * Equivalent to {@link #equals(String...)}
	 *
	 * @param entries The expected entries in this list.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R is(@SuppressWarnings("unchecked") E...entries) throws AssertionError {
		return equals(entries);
	}

	/**
	 * Asserts that the contents of this list pass the specified tests.
	 *
	 * <p>
	 * Equivalent to {@link #equals(String...)}
	 *
	 * @param tests
	 * 	The tests to run.
	 * <jk>null</jk> predicates are ignored.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	@SafeVarargs
	public final R each(Predicate<E>...tests) throws AssertionError {
		isSize(tests.length);
		for (int i = 0, j = size(); i < j; i++) {
			Predicate<E> t = tests[i];
			if (t != null && ! t.test(at(i)))
				throw error(MSG_listDidNotContainExpectedValueAt, i, getFailureMessage(t, at(i)));
		}
		return returns();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fluent setters
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public FluentListAssertion<E,R> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentListAssertion<E,R> out(PrintStream value) {
		super.out(value);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentListAssertion<E,R> silent() {
		super.silent();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentListAssertion<E,R> stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentListAssertion<E,R> throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	// </FluentSetters>

	//-----------------------------------------------------------------------------------------------------------------
	// Utility methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override
	protected List<E> value() throws AssertionError {
		return (List<E>)super.value();
	}

	private E at(int index) throws AssertionError {
		return valueIsNull() || index >= size() ? null : value().get(index);
	}

	private List<E> toSortedList(Comparator<E> comparator) {
		return valueIsNull() ? null : AList.of(value()).sortWith(comparator);
	}
}
