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

import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.serializer.*;

/**
 * Used for fluent assertion calls.
 *
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentObjectAssertion<R>")
public class FluentObjectAssertion<R> extends FluentAssertion<R> {

	private final Object o;

	private static JsonSerializer JSON = JsonSerializer.create()
		.ssq()
		.keepNullProperties()
		.addBeanTypes().addRootType()
		.build();

	private static JsonSerializer SORTEDJSON = JsonSerializer.create()
		.ssq()
		.sortCollections()
		.sortMaps()
		.keepNullProperties()
		.addBeanTypes().addRootType()
		.build();


	/**
	 * Constructor.
	 *
	 * @param o The object being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentObjectAssertion(Object o, R returns) {
		super(returns);
		this.o = o;
	}

	/**
	 * Asserts that the value equals the specified value.
	 *
	 * @param parent The value to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R instanceOf(Class<?> parent) throws AssertionError {
		if (o == null && parent == null)
			return returns();
		if (o == null && parent != null || o != null && parent == null || ! ClassInfo.of(o).isChildOf(parent))
			throw error("Unexpected class.\n\tExpected=[{0}]\n\tActual=[{1}]", StringUtils.stringify(parent), o == null ? null : o.getClass());
		return returns();
	}

	/**
	 * Verifies that two objects are equivalent after converting them both to JSON.
	 *
	 * @param o The object to compare against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R jsonSameAs(Object o) throws AssertionError {
		try {
			String s1 = JSON.serialize(this.o);
			String s2 = JSON.serialize(o);
			if (! StringUtils.isEquals(s1, s2))
				throw error("Unexpected JSON comparison.\n\tExpected=[{0}]\n\tActual=[{1}]", s2, s1);
		} catch (SerializeException e) {
			throw new RuntimeException(e);
		}
		return returns();
	}

	/**
	 * Verifies that two objects are equivalent after converting them both to sorted JSON.
	 *
	 * @param o The object to compare against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R jsonSameAsSorted(Object o) {
		try {
			String s1 = SORTEDJSON.serialize(this.o);
			String s2 = SORTEDJSON.serialize(o);
			if (! StringUtils.isEquals(s1, s2))
				throw error("Unexpected JSON comparison.\n\tExpected=[{0}]\n\tActual=[{1}]", s2, s1);
		} catch (SerializeException e) {
			throw new RuntimeException(e);
		}
		return returns();
	}

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public FluentObjectAssertion<R> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentObjectAssertion<R> stderr() {
		super.stderr();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentObjectAssertion<R> stdout() {
		super.stdout();
		return this;
	}

	// </FluentSetters>
}
