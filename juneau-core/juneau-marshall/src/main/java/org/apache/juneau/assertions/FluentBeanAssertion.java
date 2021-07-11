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

import static org.apache.juneau.internal.StringUtils.*;
import static java.util.stream.Collectors.*;
import static java.util.Arrays.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;

/**
 * Used for fluent assertion calls against Java beans.
 *
 * @param <T> The bean type.
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentBeanAssertion<T,R>")
public class FluentBeanAssertion<T,R> extends FluentObjectAssertion<T,R> {

	/**
	 * Constructor.
	 *
	 * @param value The object being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentBeanAssertion(T value, R returns) {
		this(null, value, returns);
	}

	/**
	 * Constructor.
	 *
	 * @param creator The assertion that created this assertion.
	 * @param value The object being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentBeanAssertion(Assertion creator, T value, R returns) {
		super(creator, value, returns);
	}

	/**
	 * Extracts the specified fields of this bean into a simple map of key/value pairs and returns it as
	 * a new {@link MapAssertion}.
	 *
	 * @param names The fields to extract.  Can also pass in comma-delimited lists.
	 * @return The response object (for method chaining).
	 */
	public FluentMapAssertion<String,Object,R> mapOf(String...names) {
		return new FluentMapAssertion<>(this, toBeanMap().getProperties(split(names, ',')), returns());
	}

	/**
	 * Extracts the specified property as an {@link FluentObjectAssertion}.
	 *
	 * @param name The property to extract.  Can also pass in comma-delimited lists.
	 * @return An assertion of the property value.
	 */
	public FluentObjectAssertion<Object,R> property(String name) {
		return new FluentObjectAssertion<>(this, toBeanMap().get(name), returns());
	}

	/**
	 * Extracts the specified property as an {@link FluentListAssertion}.
	 *
	 * @param names The names of the properties to extract.  Can also pass in comma-delimited lists.
	 * @return An assertion of the property values.
	 */
	public FluentListAssertion<Object,R> properties(String...names) {
		BeanMap<T> bm = toBeanMap();
		return new FluentListAssertion<>(stream(names).map(x -> bm.get(x)).collect(toList()), returns());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//-----------------------------------------------------------------------------------------------------------------

	private BeanMap<T> toBeanMap() {
		return BeanMap.of(value());
	}

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public FluentBeanAssertion<T,R> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentBeanAssertion<T,R> out(PrintStream value) {
		super.out(value);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentBeanAssertion<T,R> silent() {
		super.silent();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentBeanAssertion<T,R> stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentBeanAssertion<T,R> throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	// </FluentSetters>
}
