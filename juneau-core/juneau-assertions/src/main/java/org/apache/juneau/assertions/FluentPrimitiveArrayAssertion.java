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

import static org.apache.juneau.common.internal.ArgUtils.*;
import static org.apache.juneau.internal.ObjectUtils.*;
import static java.util.Arrays.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;

/**
 * Used for fluent assertion calls against primitive array objects (e.g. <c><jk>int</jk>[]</c>).
 *
 * <h5 class='section'>Test Methods:</h5>
 * <p>
 * <ul class='javatree'>
 * 	<li class='jc'>{@link FluentPrimitiveArrayAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentPrimitiveArrayAssertion#isHas(Object...) isHas(Object...)}
 * 		<li class='jm'>{@link FluentPrimitiveArrayAssertion#is(Predicate) is(Predicate)}
 * 		<li class='jm'>{@link FluentPrimitiveArrayAssertion#isAny(Predicate) isAny(Predicate)}
 * 		<li class='jm'>{@link FluentPrimitiveArrayAssertion#isAll(Predicate) isAll(Predicate)}
 * 		<li class='jm'>{@link FluentPrimitiveArrayAssertion#isEmpty() isEmpty()}
 * 		<li class='jm'>{@link FluentPrimitiveArrayAssertion#isNotEmpty() isNotEmpty()}
 * 		<li class='jm'>{@link FluentPrimitiveArrayAssertion#isSize(int) isSize(int)}
 * 		<li class='jm'>{@link FluentPrimitiveArrayAssertion#isContains(Object) isContains(Object)}
 * 		<li class='jm'>{@link FluentPrimitiveArrayAssertion#isNotContains(Object) isNotContains(Object)}
 * 	</ul>
 * 	<li class='jc'>{@link FluentObjectAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentObjectAssertion#isExists() isExists()}
 * 		<li class='jm'>{@link FluentObjectAssertion#is(Object) is(Object)}
 * 		<li class='jm'>{@link FluentObjectAssertion#is(Predicate) is(Predicate)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isNot(Object) isNot(Object)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isAny(Object...) isAny(Object...)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isNotAny(Object...) isNotAny(Object...)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isNull() isNull()}
 * 		<li class='jm'>{@link FluentObjectAssertion#isNotNull() isNotNull()}
 * 		<li class='jm'>{@link FluentObjectAssertion#isString(String) isString(String)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isJson(String) isJson(String)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isSame(Object) isSame(Object)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isSameJsonAs(Object) isSameJsonAs(Object)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isSameSortedJsonAs(Object) isSameSortedJsonAs(Object)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isSameSerializedAs(Object, WriterSerializer) isSameSerializedAs(Object, WriterSerializer)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isType(Class) isType(Class)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isExactType(Class) isExactType(Class)}
 * 	</ul>
 * </ul>
 *
 * <h5 class='section'>Transform Methods:</h5>
 * <p>
 * <ul class='javatree'>
 * 	<li class='jc'>{@link FluentPrimitiveArrayAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentPrimitiveArrayAssertion#asItem(int) asItem(int)}
 * 		<li class='jm'>{@link FluentPrimitiveArrayAssertion#asLength() asLength()}
 * 	</ul>
 * 	<li class='jc'>{@link FluentObjectAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentObjectAssertion#asString() asString()}
 * 		<li class='jm'>{@link FluentObjectAssertion#asString(WriterSerializer) asString(WriterSerializer)}
 * 		<li class='jm'>{@link FluentObjectAssertion#asString(Function) asString(Function)}
 * 		<li class='jm'>{@link FluentObjectAssertion#asJson() asJson()}
 * 		<li class='jm'>{@link FluentObjectAssertion#asJsonSorted() asJsonSorted()}
 * 		<li class='jm'>{@link FluentObjectAssertion#asTransformed(Function) asApplied(Function)}
 * 		<li class='jm'>{@link FluentObjectAssertion#asAny() asAny()}
 *	</ul>
 * </ul>
 *
 * <h5 class='section'>Configuration Methods:</h5>
 * <p>
 * <ul class='javatree'>
 * 	<li class='jc'>{@link Assertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link Assertion#setMsg(String, Object...) setMsg(String, Object...)}
 * 		<li class='jm'>{@link Assertion#setOut(PrintStream) setOut(PrintStream)}
 * 		<li class='jm'>{@link Assertion#setSilent() setSilent()}
 * 		<li class='jm'>{@link Assertion#setStdOut() setStdOut()}
 * 		<li class='jm'>{@link Assertion#setThrowable(Class) setThrowable(Class)}
 * 	</ul>
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#ja.Overview">Overview &gt; juneau-assertions &gt; Overview</a>
 * </ul>
 *
 * @param <E> The array element type.
 * @param <T> The array type.
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentPrimitiveArrayAssertion<E,T,R>")
public class FluentPrimitiveArrayAssertion<E,T,R> extends FluentObjectAssertion<T,R> {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

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
		MSG_arrayContainedUnexpectedValue = MESSAGES.getString("arrayContainedUnexpectedValue"),
		MSG_arrayDidntContainAnyMatchingValue = MESSAGES.getString("arrayDidntContainAnyMatchingValue"),
		MSG_arrayContainedNonMatchingValueAt = MESSAGES.getString("arrayContainedNonMatchingValueAt");

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
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
	public FluentPrimitiveArrayAssertion(T value, R returns) {
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
	public FluentPrimitiveArrayAssertion(Assertion creator, T value, R returns) {
		super(creator, value, returns);
		if (value != null) {
			Class<?> c = value.getClass();
			if (! (c.isArray() && c.getComponentType().isPrimitive()))
				throw new BasicAssertionError(MSG_objectWasNotAnArray, value.getClass());
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Transform methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* FluentObjectAssertion */
	public FluentPrimitiveArrayAssertion<E,T,R> asTransformed(Function<T,T> function) {
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
	 * (meaning {@link FluentAnyAssertion#isExists()} returns <jk>false</jk>).
	 *
	 * @param index The index of the item to retrieve from the array.
	 * @return A new assertion.
	 */
	public FluentAnyAssertion<E,R> asItem(int index) {
		return new FluentAnyAssertion<>(this, at(index), returns());
	}

	/**
	 * Returns an integer assertion on the length of this array.
	 *
	 * <p>
	 * If the array is <jk>null</jk> or the index is out-of-bounds, the returned assertion is a null assertion
	 * (meaning {@link FluentIntegerAssertion#isExists()} returns <jk>false</jk>).
	 *
	 * @return A new assertion.
	 */
	public FluentIntegerAssertion<R> asLength() {
		return new FluentIntegerAssertion<>(this, valueIsNull() ? null : Array.getLength(value()), returns());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Asserts that the contents of this list contain the specified values.
	 *
	 * @param entries The expected entries in this list.
	 * @return This object.
	 * @throws AssertionError If assertion failed.
	 */
	@SuppressWarnings("unchecked")
	public R isHas(E...entries) throws AssertionError {
		assertArgNotNull("entries", entries);
		Predicate<E>[] p = stream(entries).map(AssertionPredicates::eq).toArray(Predicate[]::new);
 		return is(p);
	}

	/**
	 * Asserts that the contents of this list pass the specified tests.
	 *
	 * @param tests The tests to run.  <jk>null</jk> entries are ignored.
	 * @return This object.
	 * @throws AssertionError If assertion failed.
	 */
	@SafeVarargs
	public final R is(Predicate<E>...tests) throws AssertionError {
		isSize(tests.length);
		for (int i = 0, j = length2(); i < j; i++) {
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
	 * @return The fluent return object.
	 * @throws AssertionError If assertion failed or value was <jk>null</jk>.
	 */
	public R isAny(Predicate<E> test) throws AssertionError {
		assertArgNotNull("test", test);
		for (int i = 0, j = length2(); i < j; i++)
			if (test.test(at(i)))
				return returns();
		throw error(MSG_arrayDidntContainAnyMatchingValue, value());
	}

	/**
	 * Asserts that all values in the array pass the specified test.
	 *
	 * @param test The predicate test.
	 * @return The fluent return object.
	 * @throws AssertionError If assertion failed or value was <jk>null</jk>.
	 */
	public R isAll(Predicate<E> test) throws AssertionError {
		assertArgNotNull("test", test);
		for (int i = 0, j = length2(); i < j; i++)
			if (! test.test(at(i)))
				throw error(MSG_arrayContainedNonMatchingValueAt, i, getFailureMessage(test, at(i)));
		return returns();
	}

	/**
	 * Asserts that the collection exists and is empty.
	 *
	 * @return The fluent return object.
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
	 * @return The fluent return object.
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
	 * @return The fluent return object.
	 * @throws AssertionError If assertion failed.
	 */
	public R isSize(int size) throws AssertionError {
		if (length2() != size)
			throw error(MSG_arrayDidNotHaveExpectedSize, size, asLength());
		return returns();
	}

	/**
	 * Asserts that the array contains the expected entry.
	 *
	 * @param entry The value to check for.
	 * @return The fluent return object.
	 * @throws AssertionError If assertion failed.
	 */
	public R isContains(E entry) throws AssertionError {
		for (int i = 0, j = length2(); i < j; i++)
			if (eq(at(i), entry))
				return returns();
		throw error(MSG_arrayDidNotContainExpectedValue, entry, value());
	}

	/**
	 * Asserts that the array does not contain the expected value.
	 *
	 * @param entry The value to check for.
	 * @return The fluent return object.
	 * @throws AssertionError If assertion failed.
	 */
	public R isNotContains(E entry) throws AssertionError {
		for (int i = 0; i < length2(); i++)
			if (eq(at(i), entry))
				throw error(MSG_arrayContainedUnexpectedValue, entry, value());
		return returns();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fluent setters
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentPrimitiveArrayAssertion<E,T,R> setMsg(String msg, Object...args) {
		super.setMsg(msg, args);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentPrimitiveArrayAssertion<E,T,R> setOut(PrintStream value) {
		super.setOut(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentPrimitiveArrayAssertion<E,T,R> setSilent() {
		super.setSilent();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentPrimitiveArrayAssertion<E,T,R> setStdOut() {
		super.setStdOut();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentPrimitiveArrayAssertion<E,T,R> setThrowable(Class<? extends java.lang.RuntimeException> value) {
		super.setThrowable(value);
		return this;
	}

	// </FluentSetters>

	//-----------------------------------------------------------------------------------------------------------------
	// Utility methods
	//-----------------------------------------------------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	private E at(int index) {
		return valueIsNull() || index < 0 || index >= length2() ? null : (E)Array.get(value(), index);
	}

	private int length2() {
		return Array.getLength(value());
	}

	@Override
	public String toString() {
		if (valueIsNull())
			return null;
		return STRINGIFIERS.get(value().getClass().getComponentType()).apply(value());
	}
}
