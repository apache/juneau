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

import java.io.*;
import java.util.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.marshall.*;

/**
 * Used for fluent assertion calls against collections objects.
 *
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentCollectionAssertion<R>")
@SuppressWarnings("rawtypes")
public class FluentCollectionAssertion<R> extends FluentBaseAssertion<Collection,R> {

	private Collection value;

	/**
	 * Constructor.
	 *
	 * @param contents The byte array being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentCollectionAssertion(Collection contents, R returns) {
		this(null, contents, returns);
	}

	/**
	 * Constructor.
	 *
	 * @param creator The assertion that created this assertion.
	 * @param contents The byte array being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentCollectionAssertion(Assertion creator, Collection contents, R returns) {
		super(creator, contents, returns);
		this.value = contents;
	}

	/**
	 * Asserts that the collection exists and is empty.
	 *
	 * @return The object to return after the test.
	 * @throws AssertionError If assertion failed.
	 */
	public R isEmpty() throws AssertionError {
		exists();
		if (! value.isEmpty())
			throw error("Collection was not empty.");
		return returns();
	}

	/**
	 * Asserts that the collection contains the expected value.
	 *
	 * @param value The value to check for.
	 * @return The object to return after the test.
	 * @throws AssertionError If assertion failed.
	 */
	public R contains(Object value) throws AssertionError {
		exists();
		for (Object o : this.value)
			if (eq(o, value))
				return returns();
		throw error("Collection did not contain expected value.\n\tContents: {0}\n\tExpected: {1}", SimpleJson.DEFAULT.toString(this.value), value);
	}

	/**
	 * Asserts that the collection contains the expected value.
	 *
	 * @param value The value to check for.
	 * @return The object to return after the test.
	 * @throws AssertionError If assertion failed.
	 */
	public R doesNotContain(Object value) throws AssertionError {
		exists();
		for (Object o : this.value)
			if (eq(o, value))
				throw error("Collection contained unexpected value.\n\tContents: {0}\n\tUnexpected: {1}", SimpleJson.DEFAULT.toString(this.value), value);
		return returns();
	}

	/**
	 * Asserts that the collection exists and is not empty.
	 *
	 * @return The object to return after the test.
	 * @throws AssertionError If assertion failed.
	 */
	public R isNotEmpty() throws AssertionError {
		exists();
		if (value.isEmpty())
			throw error("Collection was empty.");
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
		exists();
		if (value.size() != size)
			throw error("Collection did not have the expected size.  Expect={0}, Actual={1}.", size, value.size());
		return returns();
	}

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public FluentCollectionAssertion<R> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentCollectionAssertion<R> out(PrintStream value) {
		super.out(value);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentCollectionAssertion<R> silent() {
		super.silent();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentCollectionAssertion<R> stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentCollectionAssertion<R> throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	// </FluentSetters>
}
