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

import org.apache.juneau.collections.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;

/**
 * Used for fluent assertion calls against maps.
 *
 * @param <K> The key type.
 * @param <V> The value type.
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentMapAssertion<K,V,R>")
public class FluentMapAssertion<K,V,R> extends FluentObjectAssertion<Map<K,V>,R>  {

	private static final Messages MESSAGES = Messages.of(FluentMapAssertion.class, "Messages");
	private static final String
		MSG_mapWasNotEmpty = MESSAGES.getString("mapWasNotEmpty"),
		MSG_mapDidNotContainExpectedKey = MESSAGES.getString("mapDidNotContainExpectedKey"),
		MSG_mapContainedUnexpectedKey = MESSAGES.getString("mapContainedUnexpectedKey"),
		MSG_mapWasEmpty = MESSAGES.getString("mapWasEmpty"),
		MSG_mapDidNotHaveTheExpectedSize = MESSAGES.getString("mapDidNotHaveTheExpectedSize");

	//-----------------------------------------------------------------------------------------------------------------
	// Constructors
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param contents The byte array being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentMapAssertion(Map<K,V> contents, R returns) {
		this(null, contents, returns);
	}

	/**
	 * Constructor.
	 *
	 * @param creator The assertion that created this assertion.
	 * @param contents The byte array being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentMapAssertion(Assertion creator, Map<K,V> contents, R returns) {
		super(creator, contents, returns);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Transform methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* FluentObjectAssertion */
	public FluentMapAssertion<K,V,R> apply(Function<Map<K,V>,Map<K,V>> function) {
		return new FluentMapAssertion<>(this, function.apply(orElse(null)), returns());
	}

	/**
	 * Returns an object assertion on the value specified at the specified key.
	 *
	 * <p>
	 * If the map is <jk>null</jk> or the map doesn't contain the specified key, the returned assertion is a null assertion
	 * (meaning {@link FluentObjectAssertion#exists()} returns <jk>false</jk>).
	 *
	 * @param key The key of the item to retrieve from the map.
	 * @return A new assertion.
	 */
	public FluentObjectAssertion<V,R> value(K key) {
		return new FluentObjectAssertion<>(this, get(key), returns());
	}

	/**
	 * Returns a {@link FluentListAssertion} of the values of the specified keys.
	 *
	 * If the map is <jk>null</jk>, the returned assertion is a null assertion
	 * (meaning {@link FluentObjectAssertion#exists()} returns <jk>false</jk>).
	 *
	 * @param keys The keys of the values to retrieve from the map.
	 * @return A new assertion.
	 */
	public FluentListAssertion<Object,R> values(@SuppressWarnings("unchecked") K...keys) {
		return new FluentListAssertion<>(this, stream(keys).map(x -> get(x)).collect(toList()), returns());
	}

	/**
	 * Extracts a subset of this map.
	 *
	 * @param keys The entries to extract.
	 * @return The response object (for method chaining).
	 */
	@SuppressWarnings("unchecked")
	public FluentMapAssertion<K,V,R> extract(K...keys) {
		Map<K,V> m1 = orElse(null), m2 = AMap.create();
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
	 * (meaning {@link FluentIntegerAssertion#exists()} returns <jk>false</jk>).
	 *
	 * @return A new assertion.
	 */
	public FluentIntegerAssertion<R> size() {
		return new FluentIntegerAssertion<>(this, valueIsNull() ? null : value().size(), returns());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Asserts that the map exists and is empty.
	 *
	 * @return The object to return after the test.
	 * @throws AssertionError If assertion failed or value was <jk>null</jk>.
	 */
	public R isEmpty() throws AssertionError {
		if (! value().isEmpty())
			throw error(MSG_mapWasNotEmpty);
		return returns();
	}

	/**
	 * Asserts that the map contains the expected key.
	 *
	 * @param name The key name to check for.
	 * @return The object to return after the test.
	 * @throws AssertionError If assertion failed or value was <jk>null</jk>.
	 */
	public R containsKey(String name) throws AssertionError {
		if (value().containsKey(name))
			return returns();
		throw error(MSG_mapDidNotContainExpectedKey, name, value());
	}

	/**
	 * Asserts that the map contains the expected key.
	 *
	 * @param name The key name to check for.
	 * @return The object to return after the test.
	 * @throws AssertionError If assertion failed or value was <jk>null</jk>.
	 */
	public R doesNotContainKey(String name) throws AssertionError {
		if (! value().containsKey(name))
			return returns();
		throw error(MSG_mapContainedUnexpectedKey, name, value());
	}

	/**
	 * Asserts that the map exists and is not empty.
	 *
	 * @return The object to return after the test.
	 * @throws AssertionError If assertion failed or value was <jk>null</jk>.
	 */
	public R isNotEmpty() throws AssertionError {
		if (value().isEmpty())
			throw error(MSG_mapWasEmpty);
		return returns();
	}

	/**
	 * Asserts that the map exists and is the specified size.
	 *
	 * @param size The expected size.
	 * @return The object to return after the test.
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

	@Override /* GENERATED - Assertion */
	public FluentMapAssertion<K,V,R> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentMapAssertion<K,V,R> out(PrintStream value) {
		super.out(value);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentMapAssertion<K,V,R> silent() {
		super.silent();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentMapAssertion<K,V,R> stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentMapAssertion<K,V,R> throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
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
