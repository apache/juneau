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

import static java.util.stream.Collectors.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;

/**
 * Used for fluent assertion calls against lists of beans.
 *
 * @param <E> The bean type.
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentBeanListAssertion<E,R>")
public class FluentBeanListAssertion<E,R> extends FluentListAssertion<E,R> {

	//-----------------------------------------------------------------------------------------------------------------
	// Constructors
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param contents The byte array being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentBeanListAssertion(List<E> contents, R returns) {
		this(null, contents, returns);
	}

	/**
	 * Constructor.
	 *
	 * @param creator The assertion that created this assertion.
	 * @param contents The byte array being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentBeanListAssertion(Assertion creator, List<E> contents, R returns) {
		super(creator, contents, returns);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Transform methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Extracts the specified fields of this bean into a simple map of key/value pairs and returns it as
	 * a new {@link FluentListAssertion} containing maps.
	 *
	 * @param names The fields to extract.  Can also pass in comma-delimited lists.
	 * @return The response object (for method chaining).
	 */
	public FluentListAssertion<Map<String,Object>,R> extract(String...names) {
		String[] n = StringUtils.split(names, ',');
		return new FluentListAssertion<>(this, value().stream().map(x -> beanMap(x).getProperties(n)).collect(toList()), returns());
	}

	/**
	 * Extracts the specified property from each entry in this list and returns it as a {@link FluentListAssertion}.
	 *
	 * @param name The field to extract.
	 * @return The response object (for method chaining).
	 */
	public FluentListAssertion<Object,R> property(String name) {
		return new FluentListAssertion<>(this, value().stream().map(x -> beanMap(x).get(name)).collect(toList()), returns());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fluent setters
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public FluentBeanListAssertion<E,R> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentBeanListAssertion<E,R> out(PrintStream value) {
		super.out(value);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentBeanListAssertion<E,R> silent() {
		super.silent();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentBeanListAssertion<E,R> stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentBeanListAssertion<E,R> throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	// </FluentSetters>

	//-----------------------------------------------------------------------------------------------------------------
	// Utility methods
	//-----------------------------------------------------------------------------------------------------------------

	private static BeanMap<?> beanMap(Object o) {
		return BeanMap.of(o);
	}
}
