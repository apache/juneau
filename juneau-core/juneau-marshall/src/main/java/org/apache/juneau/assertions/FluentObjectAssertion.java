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
import static org.apache.juneau.internal.ExceptionUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.io.*;
import java.time.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.serializer.*;

/**
 * Used for fluent assertion calls against POJOs.
 *
 * @param <T> The object type.
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentObjectAssertion<T,R>")
public class FluentObjectAssertion<T,R> extends FluentAssertion<R> {

	private static final Messages MESSAGES = Messages.of(FluentObjectAssertion.class, "Messages");
	static final String
		MSG_unexpectedType = MESSAGES.getString("unexpectedType"),
		MSG_unexpectedComparison = MESSAGES.getString("unexpectedComparison"),
		MSG_unexpectedValue = MESSAGES.getString("unexpectedValue"),
		MSG_unexpectedValueDidNotExpect = MESSAGES.getString("unexpectedValueDidNotExpect"),
		MSG_notTheSameValue = MESSAGES.getString("notTheSameValue"),
		MSG_valueWasNull = MESSAGES.getString("valueWasNull"),
		MSG_valueWasNotNull = MESSAGES.getString("valueWasNotNull"),
		MSG_expectedValueNotFound = MESSAGES.getString("expectedValueNotFound"),
		MSG_unexpectedValueFound = MESSAGES.getString("unexpectedValueFound"),
		MSG_objectWasNotType = MESSAGES.getString("objectWasNotType"),
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

	/**
	 * Constructor.
	 *
	 * @param value The object being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentObjectAssertion(T value, R returns) {
		this(null, value, returns);
	}

	/**
	 * Constructor.
	 *
	 * @param creator The assertion that created this assertion.
	 * @param value The object being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentObjectAssertion(Assertion creator, T value, R returns) {
		super(creator, returns);
		this.value = value;
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
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isType(Class<?> parent) throws AssertionError {
		assertArgNotNull("parent", parent);
		if (! ClassInfo.of(value()).isChildOf(parent))
			throw error(MSG_unexpectedType, className(parent), className(value));
		return returns();
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
		return new FluentStringAssertion<>(this, stringify(value), returns());
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
	public FluentStringAssertion<R> asString(Function<Object,String> function) {
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
	 * Verifies that two objects are equivalent after converting them both to JSON.
	 *
	 * @param o The object to compare against.
	 * @return The response object (for method chaining).
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
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isSameSortedAs(Object o) {
		return isSameSerializedAs(o, JSON_SORTED);
	}

	/**
	 * Asserts that the specified object is the same as this object after converting both to strings using the specified serializer.
	 *
	 * @param o The object to compare against.
	 * @param serializer The serializer to use to serialize this object.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isSameSerializedAs(Object o, WriterSerializer serializer) {
		try {
			String s1 = serializer.serialize(value);
			String s2 = serializer.serialize(o);
			if (ne(s1, s2))
				throw error(MSG_unexpectedComparison, s2, s1);
		} catch (SerializeException e) {
			throw runtimeException(e);
		}
		return returns();
	}

	/**
	 * Asserts that the value equals the specified value.
	 *
	 * @param value The value to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isEqual(Object value) throws AssertionError {
		if (this.value == value)
			return returns();
		if (! value().equals(equivalent(value)))
			throw error(MSG_unexpectedValue, value, this.value);
		return returns();
	}

	/**
	 * Asserts that the value equals the specified value.
	 *
	 * <p>
	 * Equivalent to {@link #isEqual(Object)}.
	 *
	 * @param value The value to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R is(Object value) throws AssertionError {
		return isEqual(equivalent(value));
	}

	/**
	 * Asserts that the value equals the specified value.
	 *
	 * @param value The value to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R doesNotEqual(Object value) throws AssertionError {
		if (this.value == null && value != null || this.value != null && value == null)
			return returns();
		if (this.value == null || this.value.equals(equivalent(value)))
			throw error(MSG_unexpectedValueDidNotExpect, value, orElse(null));
		return returns();
	}

	/**
	 * Asserts that the specified object is the same object as this object.
	 *
	 * @param value The value to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isSameObjectAs(Object value) throws AssertionError {
		if (this.value == value)
			return returns();
		throw error(MSG_notTheSameValue, value, this.value);
	}

	/**
	 * Asserts that the value passes the specified predicate test.
	 *
	 * @param test The predicate to use to test the value.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R passes(Predicate<T> test) throws AssertionError {
		if (test != null && ! test.test(value))
			throw error(getFailureMessage(test, value));
		return returns();
	}

	/**
	 * Asserts that the object is not null.
	 *
	 * <p>
	 * Equivalent to {@link #isNotNull()}.
	 *
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R exists() throws AssertionError {
		return isNotNull();
	}

	/**
	 * Asserts that the object is null.
	 *
	 * <p>
	 * Equivalent to {@link #isNotNull()}.
	 *
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R doesNotExist() throws AssertionError {
		return isNull();
	}

	/**
	 * Asserts that the object is not null.
	 *
	 * <p>
	 * Equivalent to {@link #isNotNull()}.
	 *
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isNotNull() throws AssertionError {
		if (value == null)
			throw error(MSG_valueWasNull);
		return returns();
	}

	/**
	 * Asserts that the object i null.
	 *
	 * <p>
	 * Equivalent to {@link #isNotNull()}.
	 *
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isNull() throws AssertionError {
		if (value != null)
			throw error(MSG_valueWasNotNull);
		return returns();
	}

	/**
	 * Asserts that the value equals the specified value.
	 *
	 * <p>
	 * Equivalent to {@link #doesNotEqual(Object)}.
	 *
	 * @param value The value to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isNot(Object value) throws AssertionError {
		return doesNotEqual(equivalent(value));
	}

	/**
	 * Asserts that the value is one of the specified values.
	 *
	 * @param values The values to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isAny(Object...values) throws AssertionError {
		for (Object v : values)
			if (value().equals(equivalent(v)))
				return returns();
		throw error(MSG_expectedValueNotFound, values, value);
	}

	/**
	 * Asserts that the value is one of the specified values.
	 *
	 * @param values The values to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isNotAny(Object...values) throws AssertionError {
		for (Object v : values)
			if (value().equals(equivalent(v)))
				throw error(MSG_unexpectedValueFound, v, value);
		return returns();
	}

	/**
	 * Subclasses can override this method to provide special conversions on objects being compared.
	 *
	 * @param o The object to cast.
	 * @return The cast object.
	 */
	protected Object equivalent(Object o) {
		return o;
	}

	/**
	 * Converts this object to a string using {@link Object#toString} and runs the {@link FluentStringAssertion#is(String)} on the result.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates that the specified object is "foobar" after converting to a string.</jc>
	 * 	<jsm>assertObject<jsm>(myPojo).is(<js>"foobar"</js>);
	 * </p>
	 *
	 * @param value The expected string value.
	 * @return This object (for method chaining).
	 */
	public R isString(String value) {
		return asString().is(value);
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
	 * @return This object (for method chaining).
	 */
	public R isJson(String value) {
		return asJson().is(value);
	}

	@SuppressWarnings("unchecked")
	private <T2> T2 cast(Class<T2> c) throws AssertionError {
		Object o = value;
		if (o == null || c.isInstance(o))
			return (T2)o;
		throw new BasicAssertionError(MSG_objectWasNotType, ClassInfo.of(c).getFullName(), o.getClass());
	}

	/**
	 * Converts this object assertion into an array assertion.
	 *
	 * @param elementType The element type of the array.
	 * @return A new assertion.
	 * @throws AssertionError If object is not an array.
	 */
	public <E> FluentArrayAssertion<E,R> asArray(Class<E> elementType) throws AssertionError {
		return new FluentArrayAssertion<>(this, cast(arrayClass(elementType)), returns());
	}

	/**
	 * Converts this object assertion into a primitive array assertion.
	 *
	 * @param arrayType The array type.
	 * @return A new assertion.
	 * @throws AssertionError If object is not an array.
	 */
	public <E> FluentPrimitiveArrayAssertion<E,R> asPrimitiveArray(Class<E> arrayType) throws AssertionError {
		return new FluentPrimitiveArrayAssertion<>(this, cast(arrayType), returns());
	}

	/**
	 * Converts this object assertion into a boolean assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not a boolean.
	 */
	public FluentBooleanAssertion<R> asBoolean() {
		return new FluentBooleanAssertion<>(this, cast(Boolean.class), returns());
	}

	/**
	 * Converts this object assertion into a byte array assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not a byte array.
	 */
	public FluentByteArrayAssertion<R> asByteArray() {
		return new FluentByteArrayAssertion<>(this, cast(byte[].class), returns());
	}

	/**
	 * Converts this object assertion into a primitive short array assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not a primitive short array.
	 */
	public FluentPrimitiveArrayAssertion<short[],R> asShortArray() {
		return asPrimitiveArray(short[].class);
	}

	/**
	 * Converts this object assertion into a primitive int array assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not a primitive int array.
	 */
	public FluentPrimitiveArrayAssertion<int[],R> asIntArray() {
		return asPrimitiveArray(int[].class);
	}

	/**
	 * Converts this object assertion into a primitive long array assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not a primitive long array.
	 */
	public FluentPrimitiveArrayAssertion<long[],R> asLongArray() {
		return asPrimitiveArray(long[].class);
	}

	/**
	 * Converts this object assertion into a primitive float array assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not a primitive float array.
	 */
	public FluentPrimitiveArrayAssertion<float[],R> asFloatArray() {
		return asPrimitiveArray(float[].class);
	}

	/**
	 * Converts this object assertion into a primitive double array assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not a primitive double array.
	 */
	public FluentPrimitiveArrayAssertion<double[],R> asDoubleArray() {
		return asPrimitiveArray(double[].class);
	}

	/**
	 * Converts this object assertion into a primitive boolean array assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not a primitive boolean array.
	 */
	public FluentPrimitiveArrayAssertion<boolean[],R> asBooleanArray() {
		return asPrimitiveArray(boolean[].class);
	}

	/**
	 * Converts this object assertion into a primitive char array assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not a primitive char array.
	 */
	public FluentPrimitiveArrayAssertion<char[],R> asCharArray() {
		return asPrimitiveArray(char[].class);
	}

	/**
	 * Converts this object assertion into a collection assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not a collection.
	 */
	public FluentCollectionAssertion<Object,R> asCollection() {
		return asCollection(Object.class);
	}

	/**
	 * Converts this object assertion into a collection assertion.
	 *
	 * @param elementType The element type of the collection.
	 * @return A new assertion.
	 * @throws AssertionError If object is not a collection.
	 */
	@SuppressWarnings("unchecked")
	public <E> FluentCollectionAssertion<E,R> asCollection(Class<E> elementType) {
		return new FluentCollectionAssertion<>(this, cast(Collection.class), returns());
	}

	/**
	 * Converts this object assertion into a comparable object assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not an instance of {@link Comparable}.
	 */
	@SuppressWarnings("unchecked")
	public <T2 extends Comparable<T2>> FluentComparableAssertion<T2,R> asComparable() {
		return new FluentComparableAssertion<>(this, (T2)cast(Comparable.class), returns());
	}

	/**
	 * Converts this object assertion into a date assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not a date.
	 */
	public FluentDateAssertion<R> asDate() {
		return new FluentDateAssertion<>(this, cast(Date.class), returns());
	}

	/**
	 * Converts this object assertion into an integer assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not an integer.
	 */
	public FluentIntegerAssertion<R> asInteger() {
		return new FluentIntegerAssertion<>(this, cast(Integer.class), returns());
	}

	/**
	 * Converts this object assertion into a list assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not a list.
	 */
	public FluentListAssertion<Object,R> asList() {
		return asList(Object.class);
	}

	/**
	 * Converts this object assertion into a list assertion.
	 *
	 * @param elementType The element type.
	 * @return A new assertion.
	 * @throws AssertionError If object is not a list.
	 */
	@SuppressWarnings("unchecked")
	public <E> FluentListAssertion<E,R> asList(Class<E> elementType) {
		return new FluentListAssertion<>(this, cast(List.class), returns());
	}

	/**
	 * Converts this object assertion into a long assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not a long.
	 */
	public FluentLongAssertion<R> asLong() {
		return new FluentLongAssertion<>(this, cast(Long.class), returns());
	}

	/**
	 * Converts this object assertion into a map assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not a map.
	 */
	public FluentMapAssertion<String,Object,R> asMap() {
		return asMap(String.class,Object.class);
	}

	/**
	 * Converts this object assertion into a map assertion with the specified key and value types.
	 *
	 * @param keyType The key type.
	 * @param valueType The value type.
	 * @return A new assertion.
	 * @throws AssertionError If object is not a map.
	 */
	@SuppressWarnings("unchecked")
	public <K,V> FluentMapAssertion<K,V,R> asMap(Class<K> keyType, Class<V> valueType) {
		return new FluentMapAssertion<>(this, cast(Map.class), returns());
	}

	/**
	 * Converts this object assertion into a bean assertion.
	 *
	 * @param beanType The bean type.
	 * @return A new assertion.
	 * @throws AssertionError If object is not a bean.
	 */
	public <T2> FluentBeanAssertion<T2,R> asBean(Class<T2> beanType) {
		return new FluentBeanAssertion<>(this, cast(beanType), returns());
	}

	/**
	 * Converts this object assertion into a list-of-beans assertion.
	 *
	 * @param beanType The bean type.
	 * @return A new assertion.
	 * @throws AssertionError If object is not a bean.
	 */
	@SuppressWarnings("unchecked")
	public <T2> FluentBeanListAssertion<T2,R> asBeanList(Class<T2> beanType) {
		return new FluentBeanListAssertion<>(this, cast(List.class), returns());
	}

	/**
	 * Converts this object assertion into a zoned-datetime assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not a zoned-datetime.
	 */
	public FluentZonedDateTimeAssertion<R> asZonedDateTime() {
		return new FluentZonedDateTimeAssertion<>(this, cast(ZonedDateTime.class), returns());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods.
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
	 * Returns the string form of the inner object.
	 * Subclasses can override this method to affect the {@link #asString()} method (and related).
	 */
	@Override /* Object */
	public String toString() {
		return value == null ? null : value.toString();
	}

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public FluentObjectAssertion<T,R> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentObjectAssertion<T,R> out(PrintStream value) {
		super.out(value);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentObjectAssertion<T,R> silent() {
		super.silent();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentObjectAssertion<T,R> stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentObjectAssertion<T,R> throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	// </FluentSetters>
}
