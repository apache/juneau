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

import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static java.util.Arrays.*;

import java.io.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;

/**
 * Used for fluent assertion calls against maps.
 *
 * <h5 class='section'>Test Methods:</h5>
 * <p>
 * <ul class='javatree'>
 * 	<li class='jc'>{@link FluentMapAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentMapAssertion#isEmpty() isEmpty()}
 * 		<li class='jm'>{@link FluentMapAssertion#isNotEmpty() isNotEmpty()}
 * 		<li class='jm'>{@link FluentMapAssertion#isContainsKey(String) isContainsKey(String)}
 * 		<li class='jm'>{@link FluentMapAssertion#isNotContainsKey(String) isNotContainsKey(String)}
 * 		<li class='jm'>{@link FluentMapAssertion#isSize(int) isSize(int)}
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
 * 	<li class='jc'>{@link FluentMapAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentMapAssertion#asValue(Object) asValue(Object)}
 * 		<li class='jm'>{@link FluentMapAssertion#asValues(Object...) asValues(Object...)}
 * 		<li class='jm'>{@link FluentMapAssertion#asValueMap(Object...) asValueMap(Object...)}
 * 		<li class='jm'>{@link FluentMapAssertion#asSize() asSize()}
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
 * @param <K> The key type.
 * @param <V> The value type.
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentMapAssertion<K,V,R>")
public class FluentMapAssertion<K,V,R> extends FluentObjectAssertion<Map<K,V>,R>  {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final Messages MESSAGES = Messages.of(FluentMapAssertion.class, "Messages");
	private static final String
		MSG_mapWasNotEmpty = MESSAGES.getString("mapWasNotEmpty"),
		MSG_mapDidNotContainExpectedKey = MESSAGES.getString("mapDidNotContainExpectedKey"),
		MSG_mapContainedUnexpectedKey = MESSAGES.getString("mapContainedUnexpectedKey"),
		MSG_mapWasEmpty = MESSAGES.getString("mapWasEmpty"),
		MSG_mapDidNotHaveTheExpectedSize = MESSAGES.getString("mapDidNotHaveTheExpectedSize");

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
	public FluentMapAssertion(Map<K,V> value, R returns) {
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
	public FluentMapAssertion(Assertion creator, Map<K,V> value, R returns) {
		super(creator, value, returns);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Transform methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* FluentObjectAssertion */
	public FluentMapAssertion<K,V,R> asTransformed(Function<Map<K,V>,Map<K,V>> function) {
		return new FluentMapAssertion<>(this, function.apply(orElse(null)), returns());
	}

	/**
	 * Returns an object assertion on the value specified at the specified key.
	 *
	 * <p>
	 * If the map is <jk>null</jk> or the map doesn't contain the specified key, the returned assertion is a null assertion
	 * (meaning {@link FluentAnyAssertion#isExists()} returns <jk>false</jk>).
	 *
	 * @param key The key of the item to retrieve from the map.
	 * @return A new assertion.
	 */
	public FluentAnyAssertion<V,R> asValue(K key) {
		return new FluentAnyAssertion<>(this, get(key), returns());
	}

	/**
	 * Returns a {@link FluentListAssertion} of the values of the specified keys.
	 *
	 * If the map is <jk>null</jk>, the returned assertion is a null assertion
	 * (meaning {@link FluentObjectAssertion#isExists()} returns <jk>false</jk>).
	 *
	 * @param keys The keys of the values to retrieve from the map.
	 * @return A new assertion.
	 */
	public FluentListAssertion<Object,R> asValues(@SuppressWarnings("unchecked") K...keys) {
		return new FluentListAssertion<>(this, valueIsNull() ? null : stream(keys).map(x -> get(x)).collect(toList()), returns());
	}

	/**
	 * Extracts a subset of this map.
	 *
	 * @param keys The entries to extract.
	 * @return This object.
	 */
	@SuppressWarnings("unchecked")
	public FluentMapAssertion<K,V,R> asValueMap(K...keys) {
		if (valueIsNull())
			return new FluentMapAssertion<>(this, null, returns());
		Map<K,V> m1 = value(), m2 = CollectionUtils.map();
		if (m1 != null)
			for (K k : keys)
				m2.put(k, m1.get(k));
		return new FluentMapAssertion<>(this, m2, returns());
	}

	/**
	 * Returns an integer assertion on the size of this map.
	 *
	 * <p>
	 * If the map is <jk>null</jk>, the returned assertion is a null assertion
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
	 * Asserts that the map exists and is empty.
	 *
	 * @return The fluent return object.
	 * @throws AssertionError If assertion failed or value was <jk>null</jk>.
	 */
	public R isEmpty() throws AssertionError {
		if (! value().isEmpty())
			throw error(MSG_mapWasNotEmpty);
		return returns();
	}

	/**
	 * Asserts that the map exists and is not empty.
	 *
	 * @return The fluent return object.
	 * @throws AssertionError If assertion failed or value was <jk>null</jk>.
	 */
	public R isNotEmpty() throws AssertionError {
		if (value().isEmpty())
			throw error(MSG_mapWasEmpty);
		return returns();
	}

	/**
	 * Asserts that the map contains the expected key.
	 *
	 * @param name The key name to check for.
	 * @return The fluent return object.
	 * @throws AssertionError If assertion failed or value was <jk>null</jk>.
	 */
	public R isContainsKey(String name) throws AssertionError {
		if (value().containsKey(name))
			return returns();
		throw error(MSG_mapDidNotContainExpectedKey, name, value());
	}

	/**
	 * Asserts that the map contains the expected key.
	 *
	 * @param name The key name to check for.
	 * @return The fluent return object.
	 * @throws AssertionError If assertion failed or value was <jk>null</jk>.
	 */
	public R isNotContainsKey(String name) throws AssertionError {
		if (! value().containsKey(name))
			return returns();
		throw error(MSG_mapContainedUnexpectedKey, name, value());
	}

	/**
	 * Asserts that the map exists and is the specified size.
	 *
	 * @param size The expected size.
	 * @return The fluent return object.
	 * @throws AssertionError If assertion failed or value was <jk>null</jk>.
	 */
	public R isSize(int size) throws AssertionError {
		if (size2() != size)
			throw error(MSG_mapDidNotHaveTheExpectedSize, size, size2());
		return returns();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fluent setters
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentMapAssertion<K,V,R> setMsg(String msg, Object...args) {
		super.setMsg(msg, args);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentMapAssertion<K,V,R> setOut(PrintStream value) {
		super.setOut(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentMapAssertion<K,V,R> setSilent() {
		super.setSilent();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentMapAssertion<K,V,R> setStdOut() {
		super.setStdOut();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentMapAssertion<K,V,R> setThrowable(Class<? extends java.lang.RuntimeException> value) {
		super.setThrowable(value);
		return this;
	}

	// </FluentSetters>

	//-----------------------------------------------------------------------------------------------------------------
	// Utility methods
	//-----------------------------------------------------------------------------------------------------------------

	private V get(K key) {
		return orElse(emptyMap()).get(key);
	}

	private int size2() {
		return value().size();
	}
}
