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
import static org.apache.juneau.internal.StringUtils.*;
import static java.util.Arrays.*;

import java.io.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;

/**
 * Used for fluent assertion calls against array objects.
 *
 * @param <E> The entry type.
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentArrayAssertion<E,R>")
public class FluentArrayAssertion<E,R> extends FluentObjectAssertion<E[],R> {

	private static final Messages MESSAGES = Messages.of(FluentArrayAssertion.class, "Messages");
	private static final String
		MSG_arrayWasNotEmpty = MESSAGES.getString("arrayWasNotEmpty"),
		MSG_arrayWasEmpty = MESSAGES.getString("arrayWasEmpty"),
		MSG_arrayUnexpectedSize = MESSAGES.getString("arrayUnexpectedSize"),
		MSG_arrayDidNotContainExpectedValue = MESSAGES.getString("arrayDidNotContainExpectedValue"),
		MSG_arrayContainedUnexpectedValue = MESSAGES.getString("arrayContainedUnexpectedValue"),
		MSG_arrayDidNotContainExpectedValueAt = MESSAGES.getString("arrayDidNotContainExpectedValueAt"),
		MSG_arrayDidntContainAnyMatchingValue = MESSAGES.getString("arrayDidntContainAnyMatchingValue"),
		MSG_arrayContainedNonMatchingValueAt = MESSAGES.getString("arrayContainedNonMatchingValueAt");

	//-----------------------------------------------------------------------------------------------------------------
	// Constructors
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param contents The byte array being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentArrayAssertion(E[] contents, R returns) {
		this(null, contents, returns);
	}

	/**
	 * Constructor.
	 *
	 * @param creator The assertion that created this assertion.
	 * @param contents The byte array being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentArrayAssertion(Assertion creator, E[] contents, R returns) {
		super(creator, contents, returns);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Transform methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* FluentObjectAssertion */
	public FluentArrayAssertion<E,R> apply(Function<E[],E[]> function) {
		return new FluentArrayAssertion<>(this, function.apply(orElse(null)), returns());
	}

	@Override /* FluentBaseAssertion */
	public FluentStringAssertion<R> asString() {
		return new FluentStringAssertion<>(this, toString(), returns());
	}

	/**
	 * Converts this assertion into a {@link FluentListAssertion} of strings.
	 *
	 * @return A new fluent string assertion.
	 */
	public FluentStringListAssertion<R> asStrings() {
		return new FluentStringListAssertion<>(this, valueIsNull() ? null : stream(value()).map(x -> stringify(x)).collect(Collectors.toList()), returns());
	}

	/**
	 * Converts this assertion into a {@link FluentBeanListAssertion}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Extracts the 'foo' property from an array of beans and validates their values.</jc>.
	 * 	<jsm>assertObject<jsm>(myArrayOfBeans).asBeanList().property(<js>"foo"</js>).sorted().equals(<js>"value1"</js>,<js>"value2"</js>,<js>"value3"</js>);
	 * </p>
	 *
	 * @return A new fluent string assertion.
	 */
	public FluentBeanListAssertion<E,R> asBeanList() {
		return new FluentBeanListAssertion<>(this, toList(), returns());
	}

	/**
	 * Returns an object assertion on the item specified at the specified index.
	 *
	 * <p>
	 * If the array is <jk>null</jk> or the index is out-of-bounds, the returned assertion is a null assertion
	 * (meaning {@link FluentAnyAssertion#exists()} returns <jk>false</jk>).
	 *
	 * @param index The index of the item to retrieve from the array.
	 * @return A new assertion.
	 */
	public FluentAnyAssertion<E,R> item(int index) {
		return new FluentAnyAssertion<>(this, at(index), returns());
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
	 * Asserts that the contents of this list contain the specified values.
	 *
	 * @param entries The expected entries in this list.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	@SuppressWarnings("unchecked")
	public R has(E...entries) throws AssertionError {
		Predicate<E>[] p = stream(entries).map(AssertionPredicates::eq).toArray(Predicate[]::new);
 		return is(p);
	}

	/**
	 * Asserts that the contents of this list pass the specified tests.
	 *
	 * @param tests The tests to run.  <jk>null</jk> entries are ignored.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	@SafeVarargs
	public final R is(Predicate<E>...tests) throws AssertionError {
		isSize(tests.length);
		for (int i = 0, j = length(); i < j; i++) {
			Predicate<E> t = tests[i];
			if (t != null)
				if (! t.test(at(i)))
					throw error(MSG_arrayDidNotContainExpectedValueAt, i, getFailureMessage(t, at(i)));
		}
		return returns();
	}

	/**
	 * Asserts that at least one value in the array passes the specified test.
	 *
	 * @param test The predicate test.
	 * @return The object to return after the test.
	 * @throws AssertionError If assertion failed or value was <jk>null</jk>.
	 */
	public R any(Predicate<E> test) throws AssertionError {
		assertArgNotNull("test", test);
		for (E v : value())
			if (test.test(v))
				return returns();
		throw error(MSG_arrayDidntContainAnyMatchingValue, (Object)value());
	}

	/**
	 * Asserts that all values in the array passes the specified test.
	 *
	 * @param test The predicate test.
	 * @return The object to return after the test.
	 * @throws AssertionError If assertion failed or value was <jk>null</jk>.
	 */
	public R all(Predicate<E> test) throws AssertionError {
		assertArgNotNull("test", test);
		for (int i = 0, j = length(); i < j; i++)
			if (! test.test(at(i)))
				throw error(MSG_arrayContainedNonMatchingValueAt, i, getFailureMessage(test, at(i)));
		return returns();
	}

	/**
	 * Asserts that the collection exists and is empty.
	 *
	 * @return The object to return after the test.
	 * @throws AssertionError If assertion failed.
	 */
	public R isEmpty() throws AssertionError {
		if (length() != 0)
			throw error(MSG_arrayWasNotEmpty);
		return returns();
	}

	/**
	 * Asserts that the collection exists and is not empty.
	 *
	 * @return The object to return after the test.
	 * @throws AssertionError If assertion failed.
	 */
	public R isNotEmpty() throws AssertionError {
		if (length() == 0)
			throw error(MSG_arrayWasEmpty);
		return returns();
	}

	/**
	 * Asserts that the collection exists and is the specified size.
	 *
	 * @param size The expected size.
	 * @return The object to return after the test.
	 * @throws AssertionError If assertion failed.
	 */
	public R isSize(int size) throws AssertionError {
		if (length() != size)
			throw error(MSG_arrayUnexpectedSize, size, length());
		return returns();
	}

	/**
	 * Asserts that the array contains the expected value.
	 *
	 * @param entry The value to check for.
	 * @return The object to return after the test.
	 * @throws AssertionError If assertion failed.
	 */
	public R contains(E entry) throws AssertionError {
		for (int i = 0, j = length(); i < j; i++)
			if (eq(at(i), entry))
				return returns();
		throw error(MSG_arrayDidNotContainExpectedValue, entry, toString());
	}

	/**
	 * Asserts that the array does not contain the expected value.
	 *
	 * @param entry The value to check for.
	 * @return The object to return after the test.
	 * @throws AssertionError If assertion failed.
	 */
	public R doesNotContain(E entry) throws AssertionError {
		for (int i = 0, j = length(); i < j; i++)
			if (eq(at(i), entry))
				throw error(MSG_arrayContainedUnexpectedValue, entry, toString());
		return returns();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fluent setters
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public FluentArrayAssertion<E,R> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentArrayAssertion<E,R> out(PrintStream value) {
		super.out(value);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentArrayAssertion<E,R> silent() {
		super.silent();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentArrayAssertion<E,R> stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentArrayAssertion<E,R> throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	// </FluentSetters>

	//-----------------------------------------------------------------------------------------------------------------
	// Utility methods
	//-----------------------------------------------------------------------------------------------------------------

	private int length() {
		return value().length;
	}

	private List<E> toList() {
		return valueIsNull() ? null : AList.of(value());
	}

	private List<E> toSortedList(Comparator<E> comparator) {
		return valueIsNull() ? null : AList.of(value()).sortWith(comparator);
	}

	private E at(int index) {
		return valueIsNull() || index >= length() || index < 0 ? null : value()[index];
	}

	@Override
	public String toString() {
		return valueIsNull() ? null : Arrays.toString(value());
	}
}
