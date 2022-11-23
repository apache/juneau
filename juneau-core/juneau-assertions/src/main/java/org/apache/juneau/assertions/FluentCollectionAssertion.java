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

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.ObjectUtils.*;

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
 * <h5 class='section'>Test Methods:</h5>
 * <p>
 * <ul class='javatree'>
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
@FluentSetters(returns="FluentCollectionAssertion<E,R>")
public class FluentCollectionAssertion<E,R> extends FluentObjectAssertion<Collection<E>,R> {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final Messages MESSAGES = Messages.of(FluentCollectionAssertion.class, "Messages");
	private static final String
		MSG_collectionWasNotEmpty = MESSAGES.getString("collectionWasNotEmpty"),
		MSG_collectionDidNotContainExpectedValue = MESSAGES.getString("collectionDidNotContainExpectedValue"),
		MSG_collectionDidNotContainTestedValue = MESSAGES.getString("collectionDidNotContainTestedValue"),
		MSG_collectionContainedUnexpectedValue = MESSAGES.getString("collectionContainedUnexpectedValue"),
		MSG_collectionWasEmpty = MESSAGES.getString("collectionWasEmpty"),
		MSG_collectionDidNotHaveExpectedSize = MESSAGES.getString("collectionDidNotHaveExpectedSize");

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
	public FluentCollectionAssertion<E,R> asTransformed(Function<Collection<E>,Collection<E>> function) {
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
	 * (meaning {@link FluentIntegerAssertion#isExists()} returns <jk>false</jk>).
	 *
	 * @return A new assertion.
	 */
	public FluentIntegerAssertion<R> asSize() {
		return new FluentIntegerAssertion<>(this, valueIsNull() ? null : value().size(), returns());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Asserts that the collection exists and is empty.
	 *
	 * @return The fluent return object.
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
	 * @return The fluent return object.
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
	 * @return The fluent return object.
	 * @throws AssertionError If assertion failed or value was <jk>null</jk>.
	 */
	public R isContains(E entry) throws AssertionError {
		for (Object v : value())
			if (eq(v, entry))
				return returns();
		throw error(MSG_collectionDidNotContainExpectedValue, entry, value());
	}

	/**
	 * Asserts that the collection contains the expected value.
	 *
	 * @param entry The value to check for.
	 * @return The fluent return object.
	 * @throws AssertionError If assertion failed or value was <jk>null</jk>.
	 */
	public R isNotContains(E entry) throws AssertionError {
		value().forEach(x -> {
			if (eq(x, entry))
				throw error(MSG_collectionContainedUnexpectedValue, entry, value());
		});
		return returns();
	}

	/**
	 * Asserts that at least one value in the collection passes the specified test.
	 *
	 * @param test The predicate test.
	 * @return The fluent return object.
	 * @throws AssertionError If assertion failed or value was <jk>null</jk>.
	 */
	public R isAny(Predicate<E> test) throws AssertionError {
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
	 * @return The fluent return object.
	 * @throws AssertionError If assertion failed or value was <jk>null</jk>.
	 */
	public R isAll(Predicate<E> test) throws AssertionError {
		if (test == null)
			return returns();
		value().forEach(x -> {
			if (! test.test(x))
				throw error(MSG_collectionDidNotContainTestedValue, value());
		});
		return returns();
	}

	/**
	 * Asserts that the collection exists and is the specified size.
	 *
	 * @param size The expected size.
	 * @return The fluent return object.
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

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentCollectionAssertion<E,R> setMsg(String msg, Object...args) {
		super.setMsg(msg, args);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentCollectionAssertion<E,R> setOut(PrintStream value) {
		super.setOut(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentCollectionAssertion<E,R> setSilent() {
		super.setSilent();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentCollectionAssertion<E,R> setStdOut() {
		super.setStdOut();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentCollectionAssertion<E,R> setThrowable(Class<? extends java.lang.RuntimeException> value) {
		super.setThrowable(value);
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
