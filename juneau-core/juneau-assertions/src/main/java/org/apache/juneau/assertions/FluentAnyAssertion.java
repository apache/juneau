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

import static org.apache.juneau.common.internal.ArgUtils.*;

import java.io.*;
import java.time.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.serializer.*;

/**
 * Used for assertion calls against generic POJOs.
 *
 * <p>
 * Extends from {@link FluentObjectAssertion} allowing you to perform basic assertions, but adds several transform
 * methods to convert to more-specific assertion types.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>import static</jk> org.apache.juneau.assertions.Assertions.*;
 *
 * 	List&lt;MyBean&gt; <jv>listOfBeans</jv> = ...;
 * 	<jsm>assertList</jsm>(<jv>listOfBeans</jv>)
 * 		.asItem(1)  <jc>// Returns an AnyAssertion.</jc>
 * 		.asBean()  <jc>// Transforms to BeanAssertion.</jc>
 * 			.asProperty(<js>"foo"</js>)  <jc>// Returns an AnyAssertion.</jc>
 * 			.asString()  <jc>// Transforms to StringAssertion.</jc>
 * 				.is(<js>"bar"</js>);  <jc>// Performs test.</jc>
 * </p>
 *
 * <h5 class='section'>Test Methods:</h5>
 * <p>
 * <ul class='javatree'>
 * 	<li class='jc'>{@link FluentObjectAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentObjectAssertion#isExists() isExists()}
 * 		<li class='jm'>{@link FluentObjectAssertion#is(Object) is(Object)}
 * 		<li class='jm'>{@link FluentObjectAssertion#is(Predicate) is(Predicate)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isNot(Object) isNot(Object)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isAny(Object...) isAny(Object...)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isNotAny(Object...) isNotAny(Object...)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isNull() isNull()}
 * 		<li class='jm'>{@link FluentObjectAssertion#isNotNull() isNotNull()}
 * 		<li class='jm'>{@link FluentObjectAssertion#isString(String) isString(String)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isJson(String) isJson(String)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isSame(Object) isSame(Object)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isSameJsonAs(Object) isSameJsonAs(Object)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isSameSortedJsonAs(Object) isSameSortedJsonAs(Object)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isSameSerializedAs(Object, WriterSerializer) isSameSerializedAs(Object, WriterSerializer)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isType(Class) isType(Class)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isExactType(Class) isExactType(Class)}
 * 	</ul>
 * </ul>
 *
 * <h5 class='section'>Transform Methods:</h5>
 * <p>
 * <ul class='javatree'>
 * 	<li class='jc'>{@link FluentAnyAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentAnyAssertion#asArray(Class) asArray(Class)}
 * 		<li class='jm'>{@link FluentAnyAssertion#asIntArray() asIntArray()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asLongArray() asLongArray()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asShortArray() asShortArray()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asFloatArray() asFloatArray()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asDoubleArray() asDoubleArray()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asCharArray() asCharArray()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asByteArray() asByteArray()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asBooleanArray() asBooleanArray()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asBoolean() asBoolean()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asBytes() asBytes()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asCollection() asCollection()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asCollection(Class) asCollection(Class)}
 * 		<li class='jm'>{@link FluentAnyAssertion#asStringList() asStringList()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asComparable() asComparable()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asDate() asDate()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asInteger() asInteger()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asLong() asLong()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asList() asList()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asList(Class) asList(Class)}
 * 		<li class='jm'>{@link FluentAnyAssertion#asMap() asMap()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asMap(Class,Class) asMap(Class,Class)}
 * 		<li class='jm'>{@link FluentAnyAssertion#asBean() asBean()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asBean(Class) asBean(Class)}
 * 		<li class='jm'>{@link FluentAnyAssertion#asBeanList(Class) asBeanList(Class)}
 * 		<li class='jm'>{@link FluentAnyAssertion#asZonedDateTime() asZonedDateTime()}
 * 	</ul>
 * 	<li class='jc'>{@link FluentObjectAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentObjectAssertion#asString() asString()}
 * 		<li class='jm'>{@link FluentObjectAssertion#asString(WriterSerializer) asString(WriterSerializer)}
 * 		<li class='jm'>{@link FluentObjectAssertion#asString(Function) asString(Function)}
 * 		<li class='jm'>{@link FluentObjectAssertion#asJson() asJson()}
 * 		<li class='jm'>{@link FluentObjectAssertion#asJsonSorted() asJsonSorted()}
 * 		<li class='jm'>{@link FluentObjectAssertion#asTransformed(Function) asApplied(Function)}
 * 		<li class='jm'>{@link FluentObjectAssertion#asAny() asAny()}
 *	</ul>
 * </ul>
 *
 * <h5 class='section'>Configuration Methods:</h5>
 * <p>
 * <ul class='javatree'>
 * 	<li class='jc'>{@link Assertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link Assertion#setMsg(String, Object...) setMsg(String, Object...)}
 * 		<li class='jm'>{@link Assertion#setOut(PrintStream) setOut(PrintStream)}
 * 		<li class='jm'>{@link Assertion#setSilent() setSilent()}
 * 		<li class='jm'>{@link Assertion#setStdOut() setStdOut()}
 * 		<li class='jm'>{@link Assertion#setThrowable(Class) setThrowable(Class)}
 * 	</ul>
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#ja.Overview">Overview &gt; juneau-assertions &gt; Overview</a>
 * </ul>
 *
 * @param <T> The object type.
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentAnyAssertion<T,R>")
public class FluentAnyAssertion<T,R> extends FluentObjectAssertion<T,R> {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final Messages MESSAGES = Messages.of(FluentAnyAssertion.class, "Messages");
	private static final String
		MSG_objectWasNotType = MESSAGES.getString("objectWasNotType");

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
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
	public FluentAnyAssertion(T value, R returns) {
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
	public FluentAnyAssertion(Assertion creator, T value, R returns) {
		super(creator, value, returns);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Transform methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Converts this object assertion into an array assertion.
	 *
	 * @param <E> The element type of the array.
	 * @param elementType The element type of the array.
	 * @return A new assertion.
	 * @throws AssertionError If object is not an array.
	 */
	public <E> FluentArrayAssertion<E,R> asArray(Class<E> elementType) throws AssertionError {
		assertArgNotNull("elementType", elementType);
		return new FluentArrayAssertion<>(this, cast(arrayClass(elementType)), returns());
	}

	/**
	 * Converts this object assertion into a primitive int array assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not an int array.
	 */
	public FluentPrimitiveArrayAssertion<Integer,int[],R> asIntArray() throws AssertionError {
		return new FluentPrimitiveArrayAssertion<>(this, cast(int[].class), returns());
	}

	/**
	 * Converts this object assertion into a primitive long array assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not an long array.
	 */
	public FluentPrimitiveArrayAssertion<Long,long[],R> asLongArray() throws AssertionError {
		return new FluentPrimitiveArrayAssertion<>(this, cast(long[].class), returns());
	}

	/**
	 * Converts this object assertion into a primitive short array assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not an short array.
	 */
	public FluentPrimitiveArrayAssertion<Short,short[],R> asShortArray() throws AssertionError {
		return new FluentPrimitiveArrayAssertion<>(this, cast(short[].class), returns());
	}

	/**
	 * Converts this object assertion into a primitive float array assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not an float array.
	 */
	public FluentPrimitiveArrayAssertion<Float,float[],R> asFloatArray() throws AssertionError {
		return new FluentPrimitiveArrayAssertion<>(this, cast(float[].class), returns());
	}

	/**
	 * Converts this object assertion into a primitive double array assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not an double array.
	 */
	public FluentPrimitiveArrayAssertion<Double,double[],R> asDoubleArray() throws AssertionError {
		return new FluentPrimitiveArrayAssertion<>(this, cast(double[].class), returns());
	}

	/**
	 * Converts this object assertion into a primitive char array assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not an char array.
	 */
	public FluentPrimitiveArrayAssertion<Character,char[],R> asCharArray() throws AssertionError {
		return new FluentPrimitiveArrayAssertion<>(this, cast(char[].class), returns());
	}

	/**
	 * Converts this object assertion into a primitive byte array assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not an byte array.
	 */
	public FluentPrimitiveArrayAssertion<Byte,byte[],R> asByteArray() throws AssertionError {
		return new FluentPrimitiveArrayAssertion<>(this, cast(byte[].class), returns());
	}

	/**
	 * Converts this object assertion into a primitive boolean array assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not an boolean array.
	 */
	public FluentPrimitiveArrayAssertion<Boolean,boolean[],R> asBooleanArray() throws AssertionError {
		return new FluentPrimitiveArrayAssertion<>(this, cast(boolean[].class), returns());
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
	public FluentByteArrayAssertion<R> asBytes() {
		return new FluentByteArrayAssertion<>(this, cast(byte[].class), returns());
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
	 * @param <E> The element type of the collection.
	 * @param elementType The element type of the collection.
	 * @return A new assertion.
	 * @throws AssertionError If object is not a collection.
	 */
	@SuppressWarnings("unchecked")
	public <E> FluentCollectionAssertion<E,R> asCollection(Class<E> elementType) {
		assertArgNotNull("elementType", elementType);
		return new FluentCollectionAssertion<>(this, cast(Collection.class), returns());
	}

	/**
	 * Converts this object assertion into a collection assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not a collection.
	 */
	@SuppressWarnings("unchecked")
	public FluentStringListAssertion<R> asStringList() {
		return new FluentStringListAssertion<>(this, cast(List.class), returns());
	}

	/**
	 * Converts this object assertion into a comparable object assertion.
	 *
	 * @param <T2> The comparable type.
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
	 * Converts this object assertion into a long assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not a long.
	 */
	public FluentLongAssertion<R> asLong() {
		return new FluentLongAssertion<>(this, cast(Long.class), returns());
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
	 * @param <E> The element type.
	 * @param elementType The element type.
	 * @return A new assertion.
	 * @throws AssertionError If object is not a list.
	 */
	@SuppressWarnings("unchecked")
	public <E> FluentListAssertion<E,R> asList(Class<E> elementType) {
		assertArgNotNull("elementType", elementType);
		return new FluentListAssertion<>(this, cast(List.class), returns());
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
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param keyType The key type.
	 * @param valueType The value type.
	 * @return A new assertion.
	 * @throws AssertionError If object is not a map.
	 */
	@SuppressWarnings("unchecked")
	public <K,V> FluentMapAssertion<K,V,R> asMap(Class<K> keyType, Class<V> valueType) {
		assertArgNotNull("keyType", keyType);
		assertArgNotNull("valueType", valueType);
		return new FluentMapAssertion<>(this, cast(Map.class), returns());
	}

	/**
	 * Converts this object assertion into a bean assertion.
	 *
	 * @param <T2> The bean type.
	 * @param beanType The bean type.
	 * @return A new assertion.
	 * @throws AssertionError If object is not a bean.
	 */
	public <T2> FluentBeanAssertion<T2,R> asBean(Class<T2> beanType) {
		assertArgNotNull("beanType", beanType);
		return new FluentBeanAssertion<>(this, cast(beanType), returns());
	}

	/**
	 * Converts this object assertion into a bean assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not a bean.
	 */
	public FluentBeanAssertion<T,R> asBean() {
		return new FluentBeanAssertion<>(this, orElse(null), returns());
	}

	/**
	 * Converts this object assertion into a list-of-beans assertion.
	 *
	 * @param <T2> The bean type.
	 * @param beanType The bean type.
	 * @return A new assertion.
	 * @throws AssertionError If object is not a bean.
	 */
	@SuppressWarnings("unchecked")
	public <T2> FluentBeanListAssertion<T2,R> asBeanList(Class<T2> beanType) {
		assertArgNotNull("beanType", beanType);
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
	// Fluent setters
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentAnyAssertion<T,R> setMsg(String msg, Object...args) {
		super.setMsg(msg, args);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentAnyAssertion<T,R> setOut(PrintStream value) {
		super.setOut(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentAnyAssertion<T,R> setSilent() {
		super.setSilent();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentAnyAssertion<T,R> setStdOut() {
		super.setStdOut();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentAnyAssertion<T,R> setThrowable(Class<? extends java.lang.RuntimeException> value) {
		super.setThrowable(value);
		return this;
	}

	// </FluentSetters>

	//-----------------------------------------------------------------------------------------------------------------
	// Utility methods
	//-----------------------------------------------------------------------------------------------------------------

	private <T2> T2 cast(Class<T2> c) throws AssertionError {
		Object o = orElse(null);
		if (o == null || c.isInstance(o))
			return c.cast(o);
		throw new BasicAssertionError(MSG_objectWasNotType, ClassInfo.of(c).getFullName(), o.getClass());
	}
}
