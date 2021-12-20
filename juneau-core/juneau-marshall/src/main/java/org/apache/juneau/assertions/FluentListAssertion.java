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
import org.apache.juneau.serializer.*;

/**
 * Used for fluent assertion calls against lists.
 * {@review}
 *
 *
 * <h5 class='topic'>Test Methods</h5>
 * 	<ul>
 * 		<li class='jm'>{@link FluentListAssertion#has(Object...)}
 * 		<li class='jm'>{@link FluentListAssertion#each(Predicate...)}
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
 * 		<li class='jm'>{@link FluentListAssertion#item(int)}
 * 		<li class='jm'>{@link FluentListAssertion#sorted()}
 * 		<li class='jm'>{@link FluentListAssertion#sorted(Comparator)}
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
 * 	<li class='link'>{@doc jm.FluentAssertions}
 * </ul>
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
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 * @param returns
	 * 	The object to return after a test method is called.
	 * 	<br>If <jk>null</jk>, the test method returns this object allowing multiple test method calls to be
	 * used on the same assertion.
	 */
	public FluentListAssertion(List<E> value, R returns) {
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
	public FluentListAssertion(Assertion creator, List<E> value, R returns) {
		super(creator, value, returns);
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
	 * (meaning {@link FluentAnyAssertion#exists()} returns <jk>false</jk>).
	 *
	 * @param index The index of the item to retrieve from the list.
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

	/**
	 * Returns the first entry from this list.
	 *
	 * @return A new list assertion.
	 */
	public FluentAnyAssertion<E,R> first() {
		return item(0);
	}

	/**
	 * Returns the last entry from this list.
	 *
	 * @return A new list assertion.
	 */
	public FluentAnyAssertion<E,R> last() {
		return item(getSize()-1);
	}

	/**
	 * Returns the first X number of entries from this list.
	 *
	 * @param count The number of entries in the list to retrieve.
	 * @return A new list assertion.  The contents of the original list remain unchanged.
	 */
	public FluentListAssertion<E,R> first(int count) {
		return new FluentListAssertion<>(this, valueIsNull() ? null : value().subList(0, count), returns());
	}

	/**
	 * Returns the first X number of entries from this list.
	 *
	 * @param count The number of entries in the list to retrieve.
	 * @return A new list assertion.  The contents of the original list remain unchanged.
	 */
	public FluentListAssertion<E,R> last(int count) {
		return new FluentListAssertion<>(this, valueIsNull() ? null : value().subList(getSize()-count, getSize()), returns());
	}

	/**
	 * Returns a sublist of the entries in this list.
	 *
	 * @param start The start index (inclusive).
	 * @param end The end index (exclusive).
	 * @return A new list assertion.  The contents of the original list remain unchanged.
	 */
	public FluentListAssertion<E,R> sublist(int start, int end) {
		return new FluentListAssertion<>(this, valueIsNull() ? null : value().subList(start, end), returns());
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
	public R has(E...entries) throws AssertionError {
		Predicate<E>[] p = stream(entries).map(AssertionPredicates::eq).toArray(Predicate[]::new);
 		return each(p);
	}

	/**
	 * Asserts that the contents of this list pass the specified tests.
	 *
	 * @param tests
	 * 	The tests to run.
	 * <jk>null</jk> predicates are ignored.
	 * @return This object.
	 * @throws AssertionError If assertion failed.
	 */
	@SafeVarargs
	public final R each(Predicate<E>...tests) throws AssertionError {
		isSize(tests.length);
		for (int i = 0, j = getSize(); i < j; i++) {
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

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentListAssertion<E,R> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentListAssertion<E,R> out(PrintStream value) {
		super.out(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentListAssertion<E,R> silent() {
		super.silent();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentListAssertion<E,R> stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
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
		return valueIsNull() || index < 0 || index >= getSize() ? null : value().get(index);
	}

	private List<E> toSortedList(Comparator<E> comparator) {
		return valueIsNull() ? null : AList.of(value()).sortWith(comparator);
	}
}
