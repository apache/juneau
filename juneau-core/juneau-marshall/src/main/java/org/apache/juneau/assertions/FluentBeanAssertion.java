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

import org.apache.juneau.*;
import org.apache.juneau.internal.*;

/**
 * Used for fluent assertion calls against Java beans.
 *
 * @param <V> The bean type.
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentBeanAssertion<V,R>")
public class FluentBeanAssertion<V,R> extends FluentBaseAssertion<Object,R> {

	private final Object value;

	/**
	 * Constructor.
	 *
	 * @param value The object being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentBeanAssertion(V value, R returns) {
		this(null, value, returns);
	}

	/**
	 * Constructor.
	 *
	 * @param creator The assertion that created this assertion.
	 * @param value The object being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentBeanAssertion(Assertion creator, V value, R returns) {
		super(creator, value, returns);
		this.value = value;
	}

	/**
	 * Extracts the specified fields of this bean into a simple map of key/value pairs and returns it as
	 * a new {@link MapAssertion}.
	 *
	 * @param names The fields to extract.  Can also pass in comma-delimited lists.
	 * @return The response object (for method chaining).
	 */
	public FluentMapAssertion<R> fields(String...names) {
		exists();
		names = StringUtils.split(names, ',');
		return new FluentMapAssertion<>(this, BeanMap.create(value).getFields(names), returns());
	}

	/**
	 * Extracts the specified field as an {@link ObjectAssertion}.
	 *
	 * @param name The field to extract.  Can also pass in comma-delimited lists.
	 * @return The response object (for method chaining).
	 */
	public FluentObjectAssertion<Object,R> field(String name) {
		return field(Object.class, name);
	}

	/**
	 * Returns an object assertion on the value specified at the specified key.
	 *
	 * <p>
	 * If the map is <jk>null</jk> or the map doesn't contain the specified key, the returned assertion is a null assertion
	 * (meaning {@link FluentObjectAssertion#exists()} returns <jk>false</jk>).
	 *
	 * @param type The value type.
	 * @param name The bean property name.
	 * @return A new assertion.
	 */
	@SuppressWarnings("unchecked")
	public <E> FluentObjectAssertion<E,R> field(Class<E> type, String name) {
		Object v = getField(name);
		if (v == null || type.isInstance(v))
			return new FluentObjectAssertion<>(this, (E)v, returns());
		throw error("Bean property value not of expected type for property ''{0}''.\n\tExpected: {1}.\n\tActual: {2}", name, type, v.getClass());
	}

	private Object getField(String name) {
		exists();
		return BeanMap.create(value).get(name);
	}


	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public FluentBeanAssertion<V,R> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentBeanAssertion<V,R> stderr() {
		super.stderr();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentBeanAssertion<V,R> stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentBeanAssertion<V,R> throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	// </FluentSetters>
}
