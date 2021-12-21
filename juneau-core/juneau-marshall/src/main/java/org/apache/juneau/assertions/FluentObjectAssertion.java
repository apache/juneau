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

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.internal.ThrowableUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.io.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.serializer.*;

/**
 * Used for fluent assertion calls against POJOs.
 * {@review}
 *
 *
 * <h5 class='topic'>Test Methods</h5>
 * 	<ul>
 * 		<li class='jm'>{@link FluentObjectAssertion#exists()}
 * 		<li class='jm'>{@link FluentObjectAssertion#is(Object)}
 * 		<li class='jm'>{@link FluentObjectAssertion#is(Predicate)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isNot(Object)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isAny(Object...)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isNotAny(Object...)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isNull()}
 * 		<li class='jm'>{@link FluentObjectAssertion#isNotNull()}
 * 		<li class='jm'>{@link FluentObjectAssertion#isString(String)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isJson(String)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isSame(Object)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isSameJsonAs(Object)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isSameSortedJsonAs(Object)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isSameSerializedAs(Object, WriterSerializer)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isType(Class)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isExactType(Class)}
 * 	</ul>
 *
 * <h5 class='topic'>Transform Methods</h5>
 * 	<ul>
 * 		<li class='jm'>{@link FluentObjectAssertion#asString()}
 * 		<li class='jm'>{@link FluentObjectAssertion#asString(WriterSerializer)}
 * 		<li class='jm'>{@link FluentObjectAssertion#asString(Function)}
 * 		<li class='jm'>{@link FluentObjectAssertion#asJson()}
 * 		<li class='jm'>{@link FluentObjectAssertion#asJsonSorted()}
 * 		<li class='jm'>{@link FluentObjectAssertion#apply(Function)}
 * 		<li class='jm'>{@link FluentObjectAssertion#asAny()}
 *	</ul>
 *
 * <h5 class='topic'>Configuration Methods</h5>
 * 	<ul>
 * 		<li class='jm'>{@link Assertion#msg(String, Object...)}
 * 		<li class='jm'>{@link Assertion#out(PrintStream)}
 * 		<li class='jm'>{@link Assertion#silent()}
 * 		<li class='jm'>{@link Assertion#stdout()}
 * 		<li class='jm'>{@link Assertion#throwable(Class)}
 * 	</ul>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jm.FluentAssertions}
 * 	<li class='extlink'>{@source}
 * </ul>
 *
 * @param <T> The object type.
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentObjectAssertion<T,R>")
public class FluentObjectAssertion<T,R> extends FluentAssertion<R> {

	private static final Messages MESSAGES = Messages.of(FluentObjectAssertion.class, "Messages");
	private static final String
		MSG_unexpectedType = MESSAGES.getString("unexpectedType"),
		MSG_unexpectedComparison = MESSAGES.getString("unexpectedComparison"),
		MSG_unexpectedValue = MESSAGES.getString("unexpectedValue"),
		MSG_unexpectedValueDidNotExpect = MESSAGES.getString("unexpectedValueDidNotExpect"),
		MSG_notTheSameValue = MESSAGES.getString("notTheSameValue"),
		MSG_valueWasNull = MESSAGES.getString("valueWasNull"),
		MSG_valueWasNotNull = MESSAGES.getString("valueWasNotNull"),
		MSG_expectedValueNotFound = MESSAGES.getString("expectedValueNotFound"),
		MSG_unexpectedValueFound = MESSAGES.getString("unexpectedValueFound"),
		MSG_unexpectedValue2 = MESSAGES.getString("unexpectedValue2");

	private static JsonSerializer JSON = JsonSerializer.create()
		.ssq()
		.build();

	private static JsonSerializer JSON_SORTED = JsonSerializer.create()
		.ssq()
		.sortProperties()
		.sortCollections()
		.sortMaps()
		.build();

	private final T value;

	//-----------------------------------------------------------------------------------------------------------------
	// Constructors
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
	public FluentObjectAssertion(T value, R returns) {
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
	public FluentObjectAssertion(Assertion creator, T value, R returns) {
		super(creator, returns);
		this.value = value;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Transform methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Converts this object to a string using {@link Object#toString} and returns it as a new assertion.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates that the specified object is "foobar" after converting to a string.</jc>
	 * 	<jsm>assertObject<jsm>(myPojo).asString().is(<js>"foobar"</js>);
	 * </p>
	 *
	 * @return A new fluent string assertion.
	 */
	public FluentStringAssertion<R> asString() {
		return new FluentStringAssertion<>(this, valueAsString(), returns());
	}

	/**
	 * Converts this object to text using the specified serializer and returns it as a new assertion.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates that the specified object is an instance of MyBean.</jc>
	 * 	<jsm>assertObject<jsm>(myPojo).asString(XmlSerializer.<jsf>DEFAULT</jsf>).is(<js>"&lt;object>&lt;foo>bar&lt;/foo>&lt;baz>qux&lt;/baz>&lt;/object>"</js>);
	 * </p>
	 *
	 * @param ws The serializer to use to convert the object to text.
	 * @return A new fluent string assertion.
	 */
	public FluentStringAssertion<R> asString(WriterSerializer ws) {
		try {
			return new FluentStringAssertion<>(this, ws.serialize(value), returns());
		} catch (SerializeException e) {
			throw runtimeException(e);
		}
	}

	/**
	 * Converts this object to a string using the specified function and returns it as a new assertion.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates that the specified object is "foobar" after converting to a string.</jc>
	 * 	<jsm>assertObject<jsm>(myPojo).asString(<jv>x</jv>-><jv>x</jv>.toString()).is(<js>"foobar"</js>);
	 * </p>
	 *
	 * @param function The conversion function.
	 * @return A new fluent string assertion.
	 */
	public FluentStringAssertion<R> asString(Function<T,String> function) {
		return new FluentStringAssertion<>(this, function.apply(value), returns());
	}

	/**
	 * Converts this object to simplified JSON and returns it as a new assertion.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates that the specified object is an instance of MyBean.</jc>
	 * 	<jsm>assertObject<jsm>(myPojo).asJson().is(<js>"{foo:'bar',baz:'qux'}"</js>);
	 * </p>
	 *
	 * @return A new fluent string assertion.
	 */
	public FluentStringAssertion<R> asJson() {
		return asString(JSON);
	}

	/**
	 * Converts this object to sorted simplified JSON and returns it as a new assertion.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates that the specified object is an instance of MyBean.</jc>
	 * 	<jsm>assertObject<jsm>(myPojo).asJsonSorted().is(<js>"{baz:'qux',foo:'bar'}"</js>);
	 * </p>
	 *
	 * @return A new fluent string assertion.
	 */
	public FluentStringAssertion<R> asJsonSorted() {
		return asString(JSON_SORTED);
	}

	/**
	 * Applies a transform on the inner object and returns a new inner object.
	 *
	 * @param function The function to apply.
	 * @return This object.
	 */
	public FluentObjectAssertion<T,R> apply(Function<T,T> function) {
		return new FluentObjectAssertion<>(this, function.apply(orElse(null)), returns());
	}

	/**
	 * Applies a transform on the inner object and returns a new inner object.
	 *
	 * @param function The function to apply.
	 * @return This object.
	 */
	public <T2> FluentObjectAssertion<T2,R> transform(Function<T,T2> function) {
		return new FluentObjectAssertion<>(this, function.apply(orElse(null)), returns());
	}

	/**
	 * Converts this assertion into an {@link FluentAnyAssertion} so that it can be converted to other assertion types.
	 *
	 * @return This object.
	 */
	public FluentAnyAssertion<T,R> asAny() {
		return new FluentAnyAssertion<>(this, orElse(null), returns());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Asserts that the object is not null.
	 *
	 * <p>
	 * Equivalent to {@link #isNotNull()}.
	 *
	 * @return This object.
	 * @throws AssertionError If assertion failed.
	 */
	public R exists() throws AssertionError {
		return isNotNull();
	}

	/**
	 * Asserts that the object i null.
	 *
	 * <p>
	 * Equivalent to {@link #isNotNull()}.
	 *
	 * @return This object.
	 * @throws AssertionError If assertion failed.
	 */
	public R isNull() throws AssertionError {
		if (value != null)
			throw error(MSG_valueWasNotNull);
		return returns();
	}

	/**
	 * Asserts that the object is not null.
	 *
	 * <p>
	 * Equivalent to {@link #isNotNull()}.
	 *
	 * @return This object.
	 * @throws AssertionError If assertion failed.
	 */
	public R isNotNull() throws AssertionError {
		if (value == null)
			throw error(MSG_valueWasNull);
		return returns();
	}

	/**
	 * Asserts that the value equals the specified value.
	 *
	 * @param value The value to check against.
	 * @return This object.
	 * @throws AssertionError If assertion failed.
	 */
	public R is(T value) throws AssertionError {
		if (this.value == value)
			return returns();
		if (! equals(orElse(null), value))
			throw error(MSG_unexpectedValue, value, this.value);
		return returns();
	}

	/**
	 * Asserts that the value converted to a string equals the specified value.
	 *
	 * @param value The value to check against.
	 * @return This object.
	 * @throws AssertionError If assertion failed.
	 */
	public R isString(String value) {
		return asString().is(value);
	}

	/**
	 * Asserts that the value does not equal the specified value.
	 *
	 * @param value The value to check against.
	 * @return This object.
	 * @throws AssertionError If assertion failed.
	 */
	public R isNot(T value) throws AssertionError {
		if (equals(orElse(null), value))
			throw error(MSG_unexpectedValueDidNotExpect, value, orElse(null));
		return returns();
	}

	/**
	 * Asserts that the value is one of the specified values.
	 *
	 * @param values The values to check against.
	 * @return This object.
	 * @throws AssertionError If assertion failed.
	 */
	@SuppressWarnings("unchecked")
	public R isAny(T...values) throws AssertionError {
		for (T v : values)
			if (equals(orElse(null), v))
				return returns();
		throw error(MSG_expectedValueNotFound, values, value);
	}

	/**
	 * Asserts that the value is not one of the specified values.
	 *
	 * @param values The values to check against.
	 * @return This object.
	 * @throws AssertionError If assertion failed.
	 */
	@SuppressWarnings("unchecked")
	public R isNotAny(T...values) throws AssertionError {
		for (T v : values)
			if (equals(orElse(null), v))
				throw error(MSG_unexpectedValueFound, v, value);
		return returns();
	}

	/**
	 * Asserts that the specified object is the same object as this object.
	 *
	 * @param value The value to check against.
	 * @return This object.
	 * @throws AssertionError If assertion failed.
	 */
	public R isSame(T value) throws AssertionError {
		if (this.value == value)
			return returns();
		throw error(MSG_notTheSameValue, value, ObjectUtils.identity(value), this.value, ObjectUtils.identity(this.value));
	}

	/**
	 * Verifies that two objects are equivalent after converting them both to JSON.
	 *
	 * @param o The object to compare against.
	 * @return This object.
	 * @throws AssertionError If assertion failed.
	 */
	public R isSameJsonAs(Object o) throws AssertionError {
		return isSameSerializedAs(o, JSON);
	}

	/**
	 * Verifies that two objects are equivalent after converting them both to sorted JSON.
	 *
	 * <p>
	 * Properties, maps, and collections are all sorted on both objects before comparison.
	 *
	 * @param o The object to compare against.
	 * @return This object.
	 * @throws AssertionError If assertion failed.
	 */
	public R isSameSortedJsonAs(Object o) {
		return isSameSerializedAs(o, JSON_SORTED);
	}

	/**
	 * Asserts that the specified object is the same as this object after converting both to strings using the specified serializer.
	 *
	 * @param o The object to compare against.
	 * @param serializer The serializer to use to serialize this object.
	 * @return This object.
	 * @throws AssertionError If assertion failed.
	 */
	public R isSameSerializedAs(Object o, WriterSerializer serializer) {
		String s1 = serializer.toString(value);
		String s2 = serializer.toString(o);
		if (ne(s1, s2))
			throw error(MSG_unexpectedComparison, s2, s1);
		return returns();
	}

	/**
	 * Asserts that the object is an instance of the specified class.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates that the specified object is an instance of MyBean.</jc>
	 * 	<jsm>assertObject<jsm>(myPojo).isType(MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * @param parent The value to check against.
	 * @return This object.
	 * @throws AssertionError If assertion failed.
	 */
	public R isType(Class<?> parent) throws AssertionError {
		assertArgNotNull("parent", parent);
		if (! ClassInfo.of(value()).isChildOf(parent))
			throw error(MSG_unexpectedType, className(parent), className(value));
		return returns();
	}

	/**
	 * Asserts that the object is an instance of the specified class.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates that the specified object is an instance of MyBean.</jc>
	 * 	<jsm>assertObject<jsm>(myPojo).isExactType(MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * @param type The value to check against.
	 * @return This object.
	 * @throws AssertionError If assertion failed.
	 */
	public R isExactType(Class<?> type) throws AssertionError {
		assertArgNotNull("parent", type);
		if (value().getClass() != type)
			throw error(MSG_unexpectedType, className(type), className(value));
		return returns();
	}

	/**
	 * Asserts that the value passes the specified predicate test.
	 *
	 * @param test The predicate to use to test the value.
	 * @return This object.
	 * @throws AssertionError If assertion failed.
	 */
	public R is(Predicate<T> test) throws AssertionError {
		if (test != null && ! test.test(value))
			throw error(getFailureMessage(test, value));
		return returns();
	}

	/**
	 * Converts this object to simplified JSON and runs the {@link FluentStringAssertion#is(String)} on the result.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates that the specified object is an instance of MyBean.</jc>
	 * 	<jsm>assertObject<jsm>(myPojo).asJson().is(<js>"{foo:'bar',baz:'qux'}"</js>);
	 * </p>
	 *
	 * @param value The expected string value.
	 * @return This object.
	 */
	public R isJson(String value) {
		return asJson().is(value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fluent setters
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentObjectAssertion<T,R> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentObjectAssertion<T,R> out(PrintStream value) {
		super.out(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentObjectAssertion<T,R> silent() {
		super.silent();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentObjectAssertion<T,R> stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentObjectAssertion<T,R> throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	// </FluentSetters>

	//-----------------------------------------------------------------------------------------------------------------
	// Utility methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the inner value after asserting it is not <jk>null</jk>.
	 *
	 * @return The inner value.
	 * @throws AssertionError If inner value was <jk>null</jk>.
	 */
	protected T value() throws AssertionError {
		exists();
		return value;
	}

	/**
	 * Returns the inner value as a string.
	 *
	 * @return The inner value as a string, or <jk>null</jk> if the value was null.
	 */
	protected String valueAsString() {
		return stringify(value);
	}

	/**
	 * Returns the inner value or the other value if the value is <jk>null</jk>.
	 *
	 * @param other The other value.
	 * @return The inner value.
	 */
	protected T orElse(T other) {
		return value == null ? other : value;
	}

	/**
	 * Returns <jk>true</jk> if the inner value is null.
	 *
	 * @return <jk>true</jk> if the inner value is null.
	 */
	protected boolean valueIsNull() {
		return value == null;
	}

	/**
	 * Returns <jk>true</jk> if the inner value is not null.
	 *
	 * @return <jk>true</jk> if the inner value is not null.
	 */
	protected boolean valueIsNotNull() {
		return value != null;
	}

	/**
	 * Returns the value wrapped in an {@link Optional}.
	 *
	 * @return The value wrapped in an {@link Optional}.
	 */
	protected Optional<T> opt() {
		return Optional.ofNullable(value);
	}

	/**
	 * Returns the result of running the specified function against the value and returns the result.
	 *
	 * @param mapper The function to run against the value.
	 * @return The result, never <jk>null</jk>.
	 */
	protected <T2> Optional<T2> map(Function<? super T, ? extends T2> mapper) {
		return opt().map(mapper);
	}

	/**
	 * Returns the predicate failure message.
	 *
	 * <p>
	 * If the predicate extends from {@link AssertionPredicate}, then the message comes from {@link AssertionPredicate#getFailureMessage()}.
	 * Otherwise, returns a generic <js>"Unexpected value: x"</js> message.
	 *
	 * @param p The function to run against the value.
	 * @param value The value that failed the test.
	 * @return The result, never <jk>null</jk>.
	 */
	protected String getFailureMessage(Predicate<?> p, Object value) {
		if (p instanceof AssertionPredicate)
			return ((AssertionPredicate<?>)p).getFailureMessage();
		return format(MSG_unexpectedValue2, value);
	}

	/**
	 * Checks two objects for equality.
	 *
	 * @param o1 The first object.
	 * @param o2 The second object.
	 * @return <jk>true</jk> if the objects are equal.
	 */
	protected boolean equals(Object o1, Object o2) {
		if (o1 == o2)
			return true;
		if (o1 == null || o2 == null)
			return false;
		if (o1.equals(o2))
			return true;
		if (o1.getClass().isArray())
			return stringifyDeep(o1).equals(stringifyDeep(o2));
		return false;
	}

	/**
	 * Returns the string form of the inner object.
	 * Subclasses can override this method to affect the {@link #asString()} method (and related).
	 */
	@Override /* Object */
	public String toString() {
		return valueAsString();
	}
}
