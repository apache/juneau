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
import static java.util.stream.Collectors.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.io.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.common.internal.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;

/**
 * Used for fluent assertion calls against lists.
 *
 * <h5 class='section'>Test Methods:</h5>
 * <p>
 * <ul class='javatree'>
 * 	<li class='jc'>{@link FluentListAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentListAssertion#isHas(Object...) isHas(Object...)}
 * 		<li class='jm'>{@link FluentListAssertion#isEach(Predicate...) isEach(Predicate...)}
 * 	</ul>
 * 	<li class='jc'>{@link FluentCollectionAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentCollectionAssertion#isEmpty() isEmpty()}
 * 		<li class='jm'>{@link FluentCollectionAssertion#isNotEmpty() isNotEmpty()}
 * 		<li class='jm'>{@link FluentCollectionAssertion#isContains(Object) isContains(Object)}
 * 		<li class='jm'>{@link FluentCollectionAssertion#isNotContains(Object) isNotContains(Object)}
 * 		<li class='jm'>{@link FluentCollectionAssertion#isAny(Predicate) isAny(Predicate)}
 * 		<li class='jm'>{@link FluentCollectionAssertion#isAll(Predicate) isAll(Predicate)}
 * 		<li class='jm'>{@link FluentCollectionAssertion#isSize(int size) isSize(int size)}
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
 * 	<li class='jc'>{@link FluentListAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentListAssertion#asStrings() asStrings()}
 * 		<li class='jm'>{@link FluentListAssertion#asStrings(Function) asStrings(Function)}
 * 		<li class='jm'>{@link FluentListAssertion#asCdl() asCdl()}
 * 		<li class='jm'>{@link FluentListAssertion#asCdl(Function) asCdl(Function)}
 * 		<li class='jm'>{@link FluentListAssertion#asItem(int) asItem(int)}
 * 		<li class='jm'>{@link FluentListAssertion#asSorted() asSorted()}
 * 		<li class='jm'>{@link FluentListAssertion#asSorted(Comparator) asSorted(Comparator)}
 * 	</ul>
 * 	<li class='jc'>{@link FluentCollectionAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentCollectionAssertion#asStrings() asStrings()}
 * 		<li class='jm'>{@link FluentCollectionAssertion#asSize() asSize()}
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
 * @param <E> The element type.
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentListAssertion<E,R>")
public class FluentListAssertion<E,R> extends FluentCollectionAssertion<E,R> {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final Messages MESSAGES = Messages.of(FluentListAssertion.class, "Messages");
	private static final String
		MSG_listDidNotContainExpectedValueAt = MESSAGES.getString("listDidNotContainExpectedValueAt");

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
	public FluentListAssertion<E,R> asApplied2(Function<List<E>,List<E>> function) {
		return new FluentListAssertion<>(this, function.apply((List<E>)orElse(null)), returns());
	}

	/**
	 * Returns an object assertion on the item specified at the specified index.
	 *
	 * <p>
	 * If the list is <jk>null</jk> or the index is out-of-bounds, the returned assertion is a null assertion
	 * (meaning {@link FluentAnyAssertion#isExists()} returns <jk>false</jk>).
	 *
	 * @param index The index of the item to retrieve from the list.
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

	/**
	 * Returns the first entry from this list.
	 *
	 * @return A new list assertion.
	 */
	public FluentAnyAssertion<E,R> asFirst() {
		return asItem(0);
	}

	/**
	 * Returns the last entry from this list.
	 *
	 * @return A new list assertion.
	 */
	public FluentAnyAssertion<E,R> asLast() {
		return asItem(getSize()-1);
	}

	/**
	 * Returns the first X number of entries from this list.
	 *
	 * @param count The number of entries in the list to retrieve.
	 * @return A new list assertion.  The contents of the original list remain unchanged.
	 */
	public FluentListAssertion<E,R> asFirst(int count) {
		return new FluentListAssertion<>(this, valueIsNull() ? null : value().subList(0, count), returns());
	}

	/**
	 * Returns the first X number of entries from this list.
	 *
	 * @param count The number of entries in the list to retrieve.
	 * @return A new list assertion.  The contents of the original list remain unchanged.
	 */
	public FluentListAssertion<E,R> asLast(int count) {
		return new FluentListAssertion<>(this, valueIsNull() ? null : value().subList(getSize()-count, getSize()), returns());
	}

	/**
	 * Returns a sublist of the entries in this list.
	 *
	 * @param start The start index (inclusive).
	 * @param end The end index (exclusive).
	 * @return A new list assertion.  The contents of the original list remain unchanged.
	 */
	public FluentListAssertion<E,R> asSublist(int start, int end) {
		return new FluentListAssertion<>(this, valueIsNull() ? null : value().subList(start, end), returns());
	}

	/**
	 * Runs the stringify function against all values in this list and returns it as a fluent string list assertion.
	 *
	 * @param function The function to apply to all values in this list.
	 * @return A new fluent string list assertion.  Never <jk>null</jk>.
	 */
	public FluentStringListAssertion<R> asStrings(Function<E,String> function) {
		List<String> l = valueIsNull() ? null : value().stream().map(x -> function.apply(x)).collect(toList());
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
		List<String> l = valueIsNull() ? null : value().stream().map(x -> function.apply(x)).collect(toList());
		return new FluentStringAssertion<>(this, join(l, ','), returns());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Asserts that the contents of this list contain the specified values.
	 *
	 * @param entries The expected entries in this list.
	 * @return The fluent return object.
	 * @throws AssertionError If assertion failed.
	 */
	@SuppressWarnings("unchecked")
	public R isHas(E...entries) throws AssertionError {
		Predicate<E>[] p = stream(entries).map(AssertionPredicates::eq).toArray(Predicate[]::new);
 		return isEach(p);
	}

	/**
	 * Asserts that the contents of this list pass the specified tests.
	 *
	 * @param tests
	 * 	The tests to run.
	 * <jk>null</jk> predicates are ignored.
	 * @return The fluent return object.
	 * @throws AssertionError If assertion failed.
	 */
	@SafeVarargs
	public final R isEach(Predicate<E>...tests) throws AssertionError {
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
	public FluentListAssertion<E,R> setMsg(String msg, Object...args) {
		super.setMsg(msg, args);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentListAssertion<E,R> setOut(PrintStream value) {
		super.setOut(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentListAssertion<E,R> setSilent() {
		super.setSilent();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentListAssertion<E,R> setStdOut() {
		super.setStdOut();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentListAssertion<E,R> setThrowable(Class<? extends java.lang.RuntimeException> value) {
		super.setThrowable(value);
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
		return valueIsNull() ? null : sortedList(comparator, value());
	}
}
