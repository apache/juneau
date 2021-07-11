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
		return new FluentListAssertion<>(stream(keys).map(x -> get(x)).collect(toList()), returns());
	}

	/**
	 * Asserts that the map exists and is empty.
	 *
	 * @return The object to return after the test.
	 * @throws AssertionError If assertion failed or value was <jk>null</jk>.
	 */
	public R isEmpty() throws AssertionError {
		if (! value().isEmpty())
			throw error("Map was not empty.");
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
		throw error("Map did not contain expected key.\n\tContents: {0}\n\tExpected key: {1}", value(), name);
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
		throw error("Map contained unexpected key.\n\tContents: {0}\n\tUnexpected key: {1}", value(), name);
	}

	/**
	 * Asserts that the map exists and is not empty.
	 *
	 * @return The object to return after the test.
	 * @throws AssertionError If assertion failed or value was <jk>null</jk>.
	 */
	public R isNotEmpty() throws AssertionError {
		if (value().isEmpty())
			throw error("Map was empty.");
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
		if (size() != size)
			throw error("Map did not have the expected size.  Expect={0}, Actual={1}.", size, size());
		return returns();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//-----------------------------------------------------------------------------------------------------------------

	private V get(K key) {
		return orElse(emptyMap()).get(key);
	}

	private int size() {
		return value().size();
	}

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
}
