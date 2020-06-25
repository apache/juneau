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

	private static JsonSerializer JSON_BEANCOMPARE = JsonSerializer.create()
		.ssq()
		.keepNullProperties()
		.addBeanTypes().addRootType()
		.build();

	private static JsonSerializer JSON_BEANCOMPARESORTED = JsonSerializer.create()
		.ssq()
		.sortCollections()
		.sortMaps()
		.keepNullProperties()
		.addBeanTypes().addRootType()
		.build();

	private static JsonSerializer JSON = JsonSerializer.create()
		.ssq()
		.addBeanTypes().addRootType()
		.build();

	private static JsonSerializer JSON_SORTED = JsonSerializer.create()
		.ssq()
		.sortProperties()
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
	 * Asserts that the object is an instance of the specified class.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates that the specified object is an instance of MyBean.</jc>
	 * 	<jsm>assertObject<jsm>(myPojo).instanceOf(MyBean.<jk>class</jk>);
	 * </p>
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
	 * Converts this object to text using the specified serializer and returns it as a new assertion.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates that the specified object is an instance of MyBean.</jc>
	 * 	<jsm>assertObject<jsm>(myPojo).serialized(XmlSerializer.<jsf>DEFAULT</jsf>).is(<js>"&lt;object>&lt;foo>bar&lt;/foo>&lt;baz>qux&lt;/baz>&lt;/object>"</js>);
	 * </p>
	 *
	 * @param ws The serializer to use to convert the object to text.
	 * @return A new fluent string assertion.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public FluentStringAssertion<R> serialized(WriterSerializer ws) {
		try {
			String s = ws.serialize(this.o);
			return new FluentStringAssertion(s, this);
		} catch (SerializeException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Converts this object to simplified JSON and returns it as a new assertion.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates that the specified object is an instance of MyBean.</jc>
	 * 	<jsm>assertObject<jsm>(myPojo).json().is(<js>"{foo:'bar',baz:'qux'}"</js>);
	 * </p>
	 *
	 * @return A new fluent string assertion.
	 */
	public FluentStringAssertion<R> json() {
		return serialized(JSON);
	}

	/**
	 * Converts this object to sorted simplified JSON and returns it as a new assertion.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates that the specified object is an instance of MyBean.</jc>
	 * 	<jsm>assertObject<jsm>(myPojo).jsonSorted().is(<js>"{baz:'qux',foo:'bar'}"</js>);
	 * </p>
	 *
	 * @return A new fluent string assertion.
	 */
	public FluentStringAssertion<R> jsonSorted() {
		return serialized(JSON_SORTED);
	}

	/**
	 * Verifies that two objects are equivalent after converting them both to JSON.
	 *
	 * @param o The object to compare against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R sameAs(Object o) throws AssertionError {
		return sameAsSerialized(o, JSON_BEANCOMPARE);
	}

	/**
	 * Verifies that two objects are equivalent after converting them both to sorted JSON.
	 *
	 * @param o The object to compare against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R sameAsSorted(Object o) {
		return sameAsSerialized(o, JSON_BEANCOMPARESORTED);
	}

	/**
	 * Asserts that the specified object is the same as this object after converting both to strings using the specified serializer.
	 *
	 * @param o The object to compare against.
	 * @param serializer The serializer to use to serialize this object.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R sameAsSerialized(Object o, WriterSerializer serializer) {
		try {
			String s1 = serializer.serialize(this.o);
			String s2 = serializer.serialize(o);
			if (! StringUtils.isEquals(s1, s2))
				throw error("Unexpected comparison.\n\tExpected=[{0}]\n\tActual=[{1}]", s2, s1);
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
