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
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ObjectUtils.*;
import static java.util.Arrays.*;

import java.io.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.common.internal.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;

/**
 * Used for fluent assertion calls against array objects.
 *
 * <h5 class='section'>Test Methods:</h5>
 * <p>
 * <ul class='javatree'>
 * 	<li class='jc'>{@link FluentArrayAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentArrayAssertion#isHas(Object[]) isHas(Object[])}
 * 		<li class='jm'>{@link FluentArrayAssertion#is(Predicate) is(Predicate)}
 * 		<li class='jm'>{@link FluentArrayAssertion#isAny(Predicate) isAny(Predicate)}
 * 		<li class='jm'>{@link FluentArrayAssertion#isAll(Predicate) isAll(Predicate)}
 * 		<li class='jm'>{@link FluentArrayAssertion#isEmpty() isEmpty()}
 * 		<li class='jm'>{@link FluentArrayAssertion#isNotEmpty() isNotEmpty()}
 * 		<li class='jm'>{@link FluentArrayAssertion#isSize(int size) isSize(int size)}
 * 		<li class='jm'>{@link FluentArrayAssertion#isContains(Object) isContains(Object)}
 * 		<li class='jm'>{@link FluentArrayAssertion#isNotContains(Object) isNotContains(Object)}
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
 * 	<li class='jc'>{@link FluentArrayAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentArrayAssertion#asStrings() asStrings()}
 * 		<li class='jm'>{@link FluentArrayAssertion#asStrings(Function) asStrings(Function)}
 * 		<li class='jm'>{@link FluentArrayAssertion#asCdl() asCdl()}
 * 		<li class='jm'>{@link FluentArrayAssertion#asCdl(Function) asCdl(Function)}
 * 		<li class='jm'>{@link FluentArrayAssertion#asBeanList() asBeanList()}
 * 		<li class='jm'>{@link FluentArrayAssertion#asItem(int) asItem(int)}
 * 		<li class='jm'>{@link FluentArrayAssertion#asSorted() asSorted()}
 * 		<li class='jm'>{@link FluentArrayAssertion#asSorted(Comparator) asSorted(Comparator)}
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
 * @param <E> The entry type.
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentArrayAssertion<E,R>")
public class FluentArrayAssertion<E,R> extends FluentObjectAssertion<E[],R> {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

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
	public FluentArrayAssertion(E[] value, R returns) {
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
	public FluentArrayAssertion(Assertion creator, E[] value, R returns) {
		super(creator, value, returns);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Transform methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* FluentObjectAssertion */
	public FluentArrayAssertion<E,R> asTransformed(Function<E[],E[]> function) {
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
	 * Runs the stringify function against all values in this list and returns it as a fluent string list assertion.
	 *
	 * @param function The function to apply to all values in this list.
	 * @return A new fluent string list assertion.  Never <jk>null</jk>.
	 */
	public FluentStringListAssertion<R> asStrings(Function<E,String> function) {
		List<String> l = valueIsNull() ? null : stream(value()).map(x -> function.apply(x)).collect(Collectors.toList());
		return new FluentStringListAssertion<>(this, l, returns());
	}

	/**
	 * Converts the entries in this list to a simple comma-delimited list and returns the value as a fluent string assertion.
	 *
	 * @return A fluent string assertion.  Never <jk>null</jk>.
	 */
	public FluentStringAssertion<R> asCdl() {
		return new FluentStringAssertion<>(this, valueIsNull() ? null : StringUtils.join(value(), ','), returns());
	}

	/**
	 * Converts the entries to strings using the specified stringify function, combines them into a simple comma-delimited list, and returns the value as a fluent string assertion.
	 *
	 * @param function The function to apply to all values in this list.
	 * @return A fluent string assertion.  Never <jk>null</jk>.
	 */
	public FluentStringAssertion<R> asCdl(Function<E,String> function) {
		List<String> l = valueIsNull() ? null : stream(value()).map(x -> function.apply(x)).collect(Collectors.toList());
		return new FluentStringAssertion<>(this, join(l, ','), returns());
	}

	/**
	 * Converts this assertion into a {@link FluentBeanListAssertion}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Extracts the 'foo' property from an array of beans and validates their values.</jc>.
	 * 	<jsm>assertObject</jsm>(<jv>myArrayOfBeans</jv>)
	 * 		.asBeanList()
	 * 		.asProperty(<js>"foo"</js>)
	 * 		.asSorted()
	 * 		.equals(<js>"value1"</js>,<js>"value2"</js>,<js>"value3"</js>);
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
	 * (meaning {@link FluentAnyAssertion#isExists()} returns <jk>false</jk>).
	 *
	 * @param index The index of the item to retrieve from the array.
	 * @return A new assertion.
	 */
	public FluentAnyAssertion<E,R> asItem(int index) {
		return new FluentAnyAssertion<>(this, at(index), returns());
	}

	/**
	 * Sorts the entries in this list.
	 *
	 * @return A new list assertion.  The contents of the original list remain unchanged.
	 */
	public FluentListAssertion<E,R> asSorted() {
		return new FluentListAssertion<>(this, toSortedList(null), returns());
	}

	/**
	 * Sorts the entries in this list using the specified comparator.
	 *
	 * @param comparator The comparator to use to sort the list.
	 * @return A new list assertion.  The contents of the original list remain unchanged.
	 */
	public FluentListAssertion<E,R> asSorted(Comparator<E> comparator) {
		return new FluentListAssertion<>(this, toSortedList(comparator), returns());
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
	 * @return The fluent return object.
	 * @throws AssertionError If assertion failed or value was <jk>null</jk>.
	 */
	public R isAny(Predicate<E> test) throws AssertionError {
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
	 * @return The fluent return object.
	 * @throws AssertionError If assertion failed or value was <jk>null</jk>.
	 */
	public R isAll(Predicate<E> test) throws AssertionError {
		assertArgNotNull("test", test);
		for (int i = 0, j = length(); i < j; i++)
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
		if (length() != 0)
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
		if (length() == 0)
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
		if (length() != size)
			throw error(MSG_arrayUnexpectedSize, size, length());
		return returns();
	}

	/**
	 * Asserts that the array contains the expected value.
	 *
	 * @param entry The value to check for.
	 * @return The fluent return object.
	 * @throws AssertionError If assertion failed.
	 */
	public R isContains(E entry) throws AssertionError {
		for (int i = 0, j = length(); i < j; i++)
			if (eq(at(i), entry))
				return returns();
		throw error(MSG_arrayDidNotContainExpectedValue, entry, toString());
	}

	/**
	 * Asserts that the array does not contain the expected value.
	 *
	 * @param entry The value to check for.
	 * @return The fluent return object.
	 * @throws AssertionError If assertion failed.
	 */
	public R isNotContains(E entry) throws AssertionError {
		for (int i = 0, j = length(); i < j; i++)
			if (eq(at(i), entry))
				throw error(MSG_arrayContainedUnexpectedValue, entry, toString());
		return returns();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fluent setters
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentArrayAssertion<E,R> setMsg(String msg, Object...args) {
		super.setMsg(msg, args);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentArrayAssertion<E,R> setOut(PrintStream value) {
		super.setOut(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentArrayAssertion<E,R> setSilent() {
		super.setSilent();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentArrayAssertion<E,R> setStdOut() {
		super.setStdOut();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentArrayAssertion<E,R> setThrowable(Class<? extends java.lang.RuntimeException> value) {
		super.setThrowable(value);
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
		return valueIsNull() ? null : list(value());
	}

	private List<E> toSortedList(Comparator<E> comparator) {
		return valueIsNull() ? null : sortedList(comparator, value());
	}

	private E at(int index) {
		return valueIsNull() || index >= length() || index < 0 ? null : value()[index];
	}

	@Override
	public String toString() {
		return valueIsNull() ? null : Arrays.toString(value());
	}
}
