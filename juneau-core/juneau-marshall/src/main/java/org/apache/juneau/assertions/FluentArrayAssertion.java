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
import static java.util.Arrays.*;

import java.io.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;

/**
 * Used for fluent assertion calls against array objects.
 *
 * @param <E> The entry type.
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentArrayAssertion<E,R>")
public class FluentArrayAssertion<E,R> extends FluentObjectAssertion<E[],R> {

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

	/**
	 * Asserts that the collection exists and is empty.
	 *
	 * @return The object to return after the test.
	 * @throws AssertionError If assertion failed.
	 */
	public R isEmpty() throws AssertionError {
		if (length() != 0)
			throw error("Array was not empty.");
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
			throw error("Array was empty.");
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
			throw error("Array did not have the expected size.  Expect={0}, Actual={1}.", size, length());
		return returns();
	}

	/**
	 * Asserts that the array contains the expected value.
	 *
	 * @param entry The value to check for.
	 * @return The object to return after the test.
	 * @throws AssertionError If assertion failed.
	 */
	public R contains(Object entry) throws AssertionError {
		for (int i = 0, j = length(); i < j; i++)
			if (eq(at(i), entry))
				return returns();
		throw error("Array did not contain expected value.\n\tContents: {0}\n\tExpected: {1}", toString(), entry);
	}

	/**
	 * Asserts that the array contains the expected value when value is converted to a string.
	 *
	 * @param entry The value to check for.
	 * @return The object to return after the test.
	 * @throws AssertionError If assertion failed.
	 */
	public R contains(String entry) throws AssertionError {
		for (int i = 0, j = length(); i < j; i++)
			if (eq(stringify(at(i)), entry))
				return returns();
		throw error("Array did not contain expected value.\n\tContents: {0}\n\tExpected: {1}", toString(), entry);
	}

	/**
	 * Asserts that the array does not contain the expected value.
	 *
	 * @param entry The value to check for.
	 * @return The object to return after the test.
	 * @throws AssertionError If assertion failed.
	 */
	public R doesNotContain(Object entry) throws AssertionError {
		for (int i = 0, j = length(); i < j; i++)
			if (eq(at(i), entry))
				throw error("Array contained unexpected value.\n\tContents: {0}\n\tUnexpected: {1}", toString(), entry);
		return returns();
	}

	/**
	 * Returns an object assertion on the item specified at the specified index.
	 *
	 * <p>
	 * If the array is <jk>null</jk> or the index is out-of-bounds, the returned assertion is a null assertion
	 * (meaning {@link FluentObjectAssertion#exists()} returns <jk>false</jk>).
	 *
	 * @param index The index of the item to retrieve from the array.
	 * @return A new assertion.
	 */
	public FluentObjectAssertion<E,R> item(int index) {
		return new FluentObjectAssertion<>(this, at(index), returns());
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

	/**
	 * Asserts that the contents of this list contain the specified values when each entry is converted to a string.
	 *
	 * @param entries The expected entries in this list.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R equals(String...entries) throws AssertionError {
		Predicate<E>[] p = stream(entries).map(AssertionPredicates::eq).toArray(Predicate[]::new);
 		return passes(p);
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
 		return passes(p);
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
	 * @param tests The tests to run.  <jk>null</jk> entries are ignored.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	@SafeVarargs
	public final R passes(Predicate<E>...tests) throws AssertionError {
		isSize(tests.length);
		for (int i = 0, j = length(); i < j; i++) {
			Predicate<E> t = tests[i];
			if (t != null && ! t.test(at(i)))
				throw error("Array did not contain expected value at index {0}.\n\t{1}", i, getFailureMessage(t, at(i)));
		}
		return returns();
	}

	@Override /* FluentBaseAssertion */
	public FluentStringAssertion<R> asString() {
		return new FluentStringAssertion<>(this, toString(), returns());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods.
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
		return valueIsNull() || index >= length() ? null : value()[index];
	}

	@Override
	public String toString() {
		return valueIsNull() ? null : Arrays.toString(value());
	}

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
}
