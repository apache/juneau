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
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentObjectAssertion<R>")
public class FluentBeanAssertion<R> extends FluentBaseAssertion<Object,R> {

	private final Object value;

	/**
	 * Constructor.
	 *
	 * @param value The object being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentBeanAssertion(Object value, R returns) {
		this(null, value, returns);
	}

	/**
	 * Constructor.
	 *
	 * @param creator The assertion that created this assertion.
	 * @param value The object being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentBeanAssertion(Assertion creator, Object value, R returns) {
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
	public FluentObjectAssertion<R> field(String name) {
		exists();
		return new FluentObjectAssertion<>(this, BeanMap.create(value).get(name), returns());
	}

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public FluentBeanAssertion<R> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentBeanAssertion<R> stderr() {
		super.stderr();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentBeanAssertion<R> stdout() {
		super.stdout();
		return this;
	}

	// </FluentSetters>
}
