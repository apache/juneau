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
import static java.util.Arrays.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;

/**
 * Used for fluent assertion calls against primitive array objects (e.g. <c><jk>int</jk>[]</c>).
 *
 * @param <T> The array type.
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentPrimitiveArrayAssertion<T,R>")
public class FluentPrimitiveArrayAssertion<T,R> extends FluentObjectAssertion<T,R> {

	private static final Map<Class<?>,Function<Object,String>> STRINGIFIERS = new HashMap<>();
	static {
		STRINGIFIERS.put(boolean.class, (x) -> Arrays.toString((boolean[])x));
		STRINGIFIERS.put(byte.class, (x) -> Arrays.toString((byte[])x));
		STRINGIFIERS.put(char.class, (x) -> Arrays.toString((char[])x));
		STRINGIFIERS.put(double.class, (x) -> Arrays.toString((double[])x));
		STRINGIFIERS.put(float.class, (x) -> Arrays.toString((float[])x));
		STRINGIFIERS.put(int.class, (x) -> Arrays.toString((int[])x));
		STRINGIFIERS.put(long.class, (x) -> Arrays.toString((long[])x));
		STRINGIFIERS.put(short.class, (x) -> Arrays.toString((short[])x));
	}

	private static final Messages MESSAGES = Messages.of(FluentPrimitiveArrayAssertion.class, "Messages");
	static final String
		MSG_objectWasNotAnArray = MESSAGES.getString("objectWasNotAnArray"),
		MSG_arrayWasNotEmpty = MESSAGES.getString("arrayWasNotEmpty"),
		MSG_arrayWasEmpty = MESSAGES.getString("arrayWasEmpty"),
		MSG_arrayDidNotHaveExpectedSize = MESSAGES.getString("arrayDidNotHaveExpectedSize"),
		MSG_arrayDidNotContainExpectedValue = MESSAGES.getString("arrayDidNotContainExpectedValue"),
		MSG_arrayDidNotContainExpectedValueAt = MESSAGES.getString("arrayDidNotContainExpectedValueAt"),
		MSG_arrayContainedUnexpectedValue = MESSAGES.getString("arrayContainedUnexpectedValue");

	//-----------------------------------------------------------------------------------------------------------------
	// Constructors
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param contents The byte array being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentPrimitiveArrayAssertion(T contents, R returns) {
		this(null, contents, returns);
	}

	/**
	 * Constructor.
	 *
	 * @param creator The assertion that created this assertion.
	 * @param contents The byte array being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentPrimitiveArrayAssertion(Assertion creator, T contents, R returns) {
		super(creator, contents, returns);
		if (contents != null && ! contents.getClass().isArray())
			throw new BasicAssertionError(MSG_objectWasNotAnArray, contents.getClass());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Transform methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* FluentObjectAssertion */
	public FluentPrimitiveArrayAssertion<T,R> apply(Function<T,T> function) {
		return new FluentPrimitiveArrayAssertion<>(this, function.apply(orElse(null)), returns());
	}

	@Override /* FluentBaseAssertion */
	public FluentStringAssertion<R> asString() {
		return new FluentStringAssertion<>(this, toString(), returns());
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
	public FluentObjectAssertion<T,R> item(int index) {
		return new FluentObjectAssertion<>(this, at(index), returns());
	}

	/**
	 * Returns an integer assertion on the length of this array.
	 *
	 * <p>
	 * If the array is <jk>null</jk> or the index is out-of-bounds, the returned assertion is a null assertion
	 * (meaning {@link FluentIntegerAssertion#exists()} returns <jk>false</jk>).
	 *
	 * @return A new assertion.
	 */
	public FluentIntegerAssertion<R> length() {
		return new FluentIntegerAssertion<>(this, valueIsNull() ? null : Array.getLength(value()), returns());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Asserts that at least one value in the array passes the specified test.
	 *
	 * @param test The predicate test.
	 * @return The object to return after the test.
	 * @throws AssertionError If assertion failed or value was <jk>null</jk>.
	 */
	public R any(Predicate<T> test) throws AssertionError {
		for (int i = 0, j = length2(); i < j; i++)
			if (test.test(at(i)))
				return returns();
		throw error(MSG_arrayDidNotContainExpectedValue, value());
	}

	/**
	 * Asserts that all values in the array pass the specified test.
	 *
	 * @param test The predicate test.
	 * @return The object to return after the test.
	 * @throws AssertionError If assertion failed or value was <jk>null</jk>.
	 */
	public R all(Predicate<T> test) throws AssertionError {
		for (int i = 0, j = length2(); i < j; i++)
			if (! test.test(at(i)))
				throw error(MSG_arrayDidNotContainExpectedValue, value());
		return returns();
	}

	/**
	 * Asserts that the collection exists and is empty.
	 *
	 * @return The object to return after the test.
	 * @throws AssertionError If assertion failed.
	 */
	public R isEmpty() throws AssertionError {
		if (length2() != 0)
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
		if (length2() == 0)
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
		if (length2() != size)
			throw error(MSG_arrayDidNotHaveExpectedSize, size, length());
		return returns();
	}

	/**
	 * Asserts that the array contains the expected entry.
	 *
	 * @param entry The value to check for.
	 * @return The object to return after the test.
	 * @throws AssertionError If assertion failed.
	 */
	public R contains(Object entry) throws AssertionError {
		for (int i = 0, j = length2(); i < j; i++)
			if (eq(at(i), entry))
				return returns();
		throw error(MSG_arrayDidNotContainExpectedValue, entry, value());
	}

	/**
	 * Asserts that the array contains the expected entries.
	 *
	 * @param entries The values to check for after being converted to strings.
	 * @return The object to return after the test.
	 * @throws AssertionError If assertion failed.
	 */
	public R equals(String...entries) throws AssertionError {
		Predicate<T>[] p = stream(entries).map(StringUtils::stringify).map(AssertionPredicates::eq).toArray(Predicate[]::new);
		return each(p);
	}

	/**
	 * Asserts that the array contains the expected entries.
	 *
	 * <p>
	 * Equivalent to {@link #equals(String...)}.
	 *
	 * @param entries The values to check for after being converted to strings.
	 * @return The object to return after the test.
	 * @throws AssertionError If assertion failed.
	 */
	public R is(String...entries) throws AssertionError {
		return equals(entries);
	}

	/**
	 * Asserts that the array contains the expected entries.
	 *
	 * @param entries The values to check for.  Uses {@link Object#equals(Object)} for equivalency on entries.
	 * @return The object to return after the test.
	 * @throws AssertionError If assertion failed.
	 */
	@SuppressWarnings("unchecked")
	public R equals(T...entries) throws AssertionError {
		Predicate<T>[] p = stream(entries).map(AssertionPredicates::eq).toArray(Predicate[]::new);
		return each(p);
	}

	/**
	 * Asserts that the array contains the expected entries.
	 *
	 * <p>
	 * Equivalent to {@link #equals(T...)}.
	 *
	 * @param entries The values to check for.  Uses {@link Object#equals(Object)} for equivalency on entries.
	 * @return The object to return after the test.
	 * @throws AssertionError If assertion failed.
	 */
	@SuppressWarnings("unchecked")
	public R is(T...entries) throws AssertionError {
		return equals(entries);
	}

	/**
	 * Asserts that the entries in this array pass the specified tests for each entry.
	 *
	 * @param tests The tests to run.  <jk>null</jk> values are ignored.
	 * @return The object to return after the test.
	 * @throws AssertionError If assertion failed.
	 */
	@SuppressWarnings("unchecked")
	public R each(Predicate<T>...tests) throws AssertionError {
		length().is(tests.length);
		for (int i = 0, j = length2(); i < j; i++) {
			Predicate<T> t = tests[i];
			if (t != null && ! t.test(at(i)))
				throw error(MSG_arrayDidNotContainExpectedValueAt, i, getFailureMessage(t, at(i)));
		}
		return returns();
	}

	/**
	 * Asserts that the array does not contain the expected value.
	 *
	 * @param entry The value to check for.
	 * @return The object to return after the test.
	 * @throws AssertionError If assertion failed.
	 */
	public R doesNotContain(Object entry) throws AssertionError {
		for (int i = 0; i < length2(); i++)
			if (eq(at(i), entry))
				throw error(MSG_arrayContainedUnexpectedValue, entry, value());
		return returns();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fluent setters
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public FluentPrimitiveArrayAssertion<T,R> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentPrimitiveArrayAssertion<T,R> out(PrintStream value) {
		super.out(value);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentPrimitiveArrayAssertion<T,R> silent() {
		super.silent();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentPrimitiveArrayAssertion<T,R> stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentPrimitiveArrayAssertion<T,R> throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	// </FluentSetters>

	//-----------------------------------------------------------------------------------------------------------------
	// Utility methods
	//-----------------------------------------------------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	private T at(int index) {
		return valueIsNull() || index >= length2() ? null : (T)Array.get(value(), index);
	}

	private int length2() {
		return Array.getLength(value());
	}

	@Override
	public String toString() {
		if (valueIsNull())
			return null;
		return STRINGIFIERS.getOrDefault(value().getClass().getComponentType(), (x) -> x.toString()).apply(value());
	}
}
