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

import static org.apache.juneau.common.internal.IOUtils.*;

import java.io.*;
import java.time.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.common.utils.*;

/**
 * Main class for creation of assertions for stand-alone testing.
 *
 * <p>
 * Provides assertions for various common POJO types.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>import static</jk> org.apache.juneau.assertions.Assertions.*;
 *
 *	<jc>// Assert string is greater than 100 characters and contains "foo".</jc>
 * 	<jsm>assertString</jsm>(<jv>myString</jv>)
 * 		.length().isGt(100)
 * 		.contains(<js>"foo"</js>);
 * </p>
 *
 * <p>
 * Provides simple testing that {@link Throwable Throwables} are being thrown correctly.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>import static</jk> org.apache.juneau.assertions.Assertions.*;
 *
 *	<jc>// Assert that calling doBadCall() causes a RuntimeException.</jc>
 * 	<jsm>assertThrown</jsm>(() -&gt; <jv>myPojo</jv>.doBadCall())
 * 		.isType(RuntimeException.<jk>class</jk>)
 * 		.message().contains(<js>"Bad thing happened."</js>);
 * </p>
 *
 * <p>
 * Provides other assertion convenience methods such as asserting non-null method arguments.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>import static</jk> org.apache.juneau.assertions.Assertions.*;
 *
 *	<jk>public</jk> String getFoo(String <jv>bar</jv>) {
 *		<jsm>assertArgNotNull</jsm>(<js>"bar"</js>, <jv>bar</jv>);
 *		...
 *	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#ja.Overview">Fluent Assertions</a>
 * </ul>
 */
public class Assertions {

	//-----------------------------------------------------------------------------------------------------------------
	// Fluent assertions
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Performs an assertion on an arbitrary POJO.
	 *
	 * <p>
	 * The distinction between {@link ObjectAssertion} and {@link AnyAssertion} is that the latter supports all
	 * the operations of the former, but adds various transform methods for conversion to specific assertion types.
	 *
	 * <p>
	 * Various transform methods such as {@link FluentListAssertion#asItem(int)} and {@link FluentBeanAssertion#asProperty(String)}
	 * return generic any-assertions so that they can be easily transformed into other assertion types.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.assertions.Assertions.*;
	 *
	 * 	<jc>// Asserts that the property 'foo' of a bean is 'bar'.</jc>
	 * 	<jsm>assertAny</jsm>(<jv>myPojo</jv>)  <jc>// Start with AnyAssertion.</jc>
	 * 		.asBean(MyBean.<jk>class</jk>)  <jc>// Transform to BeanAssertion.</jc>
	 * 			.property(<js>"foo"</js>).is(<js>"bar"</js>);
	 * </p>
	 *
	 * <p>
	 * See <a class="doclink" href="../../../../index.html#ja.Overview">Fluent Assertions</a> for general assertion usage and {@link AnyAssertion} for supported operations on this type.
	 *
	 * @param <T> The value type.
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 * @return
	 * 	A new assertion object.
	 * 	<br>Never <jk>null</jk>.
	 */
	public static final <T> AnyAssertion<T> assertAny(T value) {
		return AnyAssertion.create(value);
	}

	/**
	 * Performs an assertion on an array of POJOs.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.assertions.Assertions.*;
	 *
	 *	<jc>// Asserts that an Integer array contains [1,2,3].</jc>
	 * 	Integer[] <jv>array</jv> = {...};
	 * 	<jsm>assertArray</jsm>(<jv>array</jv>)
	 * 		.asJson().is(<js>"[1,2,3]"</js>);
	 * </p>
	 *
	 * <p>
	 * See <a class="doclink" href="../../../../index.html#ja.Overview">Fluent Assertions</a> for general assertion usage and {@link ArrayAssertion} for supported operations on this type.
	 *
	 * @param <E> The value element type.
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 * @return
	 * 	A new assertion object.
	 * 	<br>Never <jk>null</jk>.
	 */
	public static final <E> ArrayAssertion<E> assertArray(E[] value) {
		return ArrayAssertion.create(value);
	}

	/**
	 * Performs an assertion on a Java bean.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.assertions.Assertions.*;
	 *
	 *	<jc>// Asserts that the 'foo' and 'bar' properties of a bean are 1 and 2 respectively.</jc>
	 * 	<jsm>assertBean</jsm>(<jv>myBean</jv>)
	 * 		.isType(MyBean.<jk>class</jk>)
	 * 		.extract(<js>"foo,bar"</js>)
	 * 			.asJson().is(<js>"{foo:1,bar:2}"</js>);
	 * </p>
	 *
	 * <p>
	 * See <a class="doclink" href="../../../../index.html#ja.Overview">Fluent Assertions</a> for general assertion usage and {@link BeanAssertion} for supported operations on this type.
	 *
	 * @param <T> The value type.
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 * @return
	 * 	A new assertion object.
	 * 	<br>Never <jk>null</jk>.
	 */
	public static final <T> BeanAssertion<T> assertBean(T value) {
		return BeanAssertion.create(value);
	}

	/**
	 * Performs an assertion on a list of Java beans.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.assertions.Assertions.*;
	 *
	 *	<jc>// Asserts that a bean list has 3 entries with 'foo' property values of 'bar','baz','qux'.</jc>
	 * 	<jsm>assertBeanList</jsm>(<jv>myListOfBeans</jv>)
	 * 		.isSize(3)
	 * 		.property(<js>"foo"</js>)
	 * 			.is(<js>"bar"</js>,<js>"baz"</js>,<js>"qux"</js>);
	 * </p>
	 *
	 * <p>
	 * See <a class="doclink" href="../../../../index.html#ja.Overview">Fluent Assertions</a> for general assertion usage and {@link BeanListAssertion} for supported operations on this type.
	 *
	 * @param <E> The element type.
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 * @return
	 * 	A new assertion object.
	 * 	<br>Never <jk>null</jk>.
	 */
	public static final <E> BeanListAssertion<E> assertBeanList(List<E> value) {
		return BeanListAssertion.create(value);
	}

	/**
	 * Performs an assertion on a Boolean.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.assertions.Assertions.*;
	 *
	 *	<jc>// Asserts that a Boolean is not null and TRUE.</jc>
	 * 	<jsm>assertBoolean</jsm>(<jv>myBoolean</jv>)
	 * 		.isTrue();
	 * </p>
	 *
	 * <p>
	 * See <a class="doclink" href="../../../../index.html#ja.Overview">Fluent Assertions</a> for general assertion usage and {@link BooleanAssertion} for supported operations on this type.
	 *
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 * @return
	 * 	A new assertion object.
	 * 	<br>Never <jk>null</jk>.
	 */
	public static final BooleanAssertion assertBoolean(Boolean value) {
		return BooleanAssertion.create(value);
	}

	/**
	 * Performs an assertion on a boolean array.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.assertions.Assertions.*;
	 *
	 *	<jc>// Asserts that a Boolean array has size of 3 and all entries are TRUE.</jc>
	 * 	<jsm>assertBooleanArray</jsm>(<jv>myBooleanArray</jv>)
	 * 		.isSize(3)
	 * 		.all(<jv>x</jv> -&gt; <jv>x</jv> == <jk>true</jk>);
	 * </p>
	 *
	 * <p>
	 * See <a class="doclink" href="../../../../index.html#ja.Overview">Fluent Assertions</a> for general assertion usage and {@link PrimitiveArrayAssertion} for supported operations on this type.
	 *
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 * @return
	 * 	A new assertion object.
	 * 	<br>Never <jk>null</jk>.
	 */
	public static final PrimitiveArrayAssertion<Boolean,boolean[]> assertBooleanArray(boolean[] value) {
		return PrimitiveArrayAssertion.create(value);
	}

	/**
	 * Performs an assertion on a byte array.
	 *
	 * <p>
	 * The distinction between {@link #assertByteArray} and {@link #assertBytes} is that the former returns an assertion
	 * more tied to general byte arrays and the latter returns an assertion more tied to dealing with binary streams
	 * that can be decoded or transformed into a string.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.assertions.Assertions.*;
	 *
	 *	<jc>// Asserts that a byte array has size of 3 and all bytes are larger than 10.</jc>
	 * 	<jsm>assertByteArray</jsm>(<jv>myByteArray</jv>)
	 * 		.isSize(3)
	 * 		.all(<jv>x</jv> -&gt; <jv>x</jv> &gt; 10);
	 * </p>
	 *
	 * <p>
	 * See <a class="doclink" href="../../../../index.html#ja.Overview">Fluent Assertions</a> for general assertion usage and {@link PrimitiveArrayAssertion} for supported operations on this type.
	 *
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 * @return
	 * 	A new assertion object.
	 * 	<br>Never <jk>null</jk>.
	 */
	public static final PrimitiveArrayAssertion<Byte,byte[]> assertByteArray(byte[] value) {
		return PrimitiveArrayAssertion.create(value);
	}

	/**
	 * Performs an assertion on a byte array.
	 *
	 * <p>
	 * The distinction between {@link #assertByteArray} and {@link #assertBytes} is that the former returns an assertion
	 * more tied to general byte arrays and the latter returns an assertion more tied to dealing with binary streams
	 * that can be decoded or transformed into a string.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.assertions.Assertions.*;
	 *
	 * 	<jc>// Asserts that the byte array contains the string "foo".</jc>
	 * 	<jsm>assertBytes</jsm>(<jv>myBytes</jv>)
	 * 		.asHex().is(<js>"666F6F"</js>);
	 * </p>
	 *
	 * <p>
	 * See <a class="doclink" href="../../../../index.html#ja.Overview">Fluent Assertions</a> for general assertion usage and {@link ByteArrayAssertion} for supported operations on this type.
	 *
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 * @return
	 * 	A new assertion object.
	 * 	<br>Never <jk>null</jk>.
	 */
	public static final ByteArrayAssertion assertBytes(byte[] value) {
		return ByteArrayAssertion.create(value);
	}

	/**
	 * Performs an assertion on the contents of an input stream.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.assertions.Assertions.*;
	 *
	 * 	<jc>// Asserts that the stream contains the string "foo".</jc>
	 * 	<jsm>assertBytes</jsm>(<jv>myStream</jv>)
	 * 		.asHex().is(<js>"666F6F"</js>);
	 * </p>
	 *
	 * <p>
	 * See <a class="doclink" href="../../../../index.html#ja.Overview">Fluent Assertions</a> for general assertion usage and {@link ByteArrayAssertion} for supported operations on this type.
	 *
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br>Stream is automatically closed.
	 * @return
	 * 	A new assertion object.
	 * 	<br>Never <jk>null</jk>.
	 * @throws IOException If thrown while reading contents from stream.
	 */
	public static final ByteArrayAssertion assertBytes(InputStream value) throws IOException {
		return assertBytes(value == null ? null : readBytes(value));
	}

	/**
	 * Performs an assertion on a char array.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.assertions.Assertions.*;
	 *
	 * 	<jc>// Asserts that the char array contains the string "foo".</jc>
	 * 	<jsm>assertCharArray</jsm>(<jv>myCharArray</jv>)
	 * 		.asString().is(<js>"foo"</js>);
	 * </p>
	 *
	 * <p>
	 * See <a class="doclink" href="../../../../index.html#ja.Overview">Fluent Assertions</a> for general assertion usage and {@link PrimitiveArrayAssertion} for supported operations on this type.
	 *
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 * @return
	 * 	A new assertion object.
	 * 	<br>Never <jk>null</jk>.
	 */
	public static final PrimitiveArrayAssertion<Character,char[]> assertCharArray(char[] value) {
		return PrimitiveArrayAssertion.create(value);
	}

	/**
	 * Performs an assertion on a collection of POJOs.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.assertions.Assertions.*;
	 *
	 * 	<jc>// Asserts that a collection of strings has only one entry of 'foo'.</jc>
	 * 	<jsm>assertCollection</jsm>(<jv>myCollectionOfStrings</jv>)
	 * 		.isSize(1)
	 * 		.contains(<js>"foo"</js>);
	 * </p>
	 *
	 * <p>
	 * In general, use {@link #assertList(List)} if you're performing an assertion on a list since {@link ListAssertion}
	 * provides more functionality than {@link CollectionAssertion}.
	 *
	 * <p>
	 * See <a class="doclink" href="../../../../index.html#ja.Overview">Fluent Assertions</a> for general assertion usage and {@link CollectionAssertion} for supported operations on this type.
	 *
	 * @param <E> The element type.
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 * @return
	 * 	A new assertion object.
	 * 	<br>Never <jk>null</jk>.
	 */
	public static final <E> CollectionAssertion<E> assertCollection(Collection<E> value) {
		return CollectionAssertion.create(value);
	}

	/**
	 * Performs an assertion on a Comparable.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.assertions.Assertions.*;
	 *
	 * 	<jc>// Asserts a comparable is less than another comparable.</jc>
	 * 	<jsm>assertComparable</jsm>(<jv>myComparable</jv>)
	 * 		.isLt(<jv>anotherComparable</jv>);
	 * </p>
	 *
	 * <p>
	 * See <a class="doclink" href="../../../../index.html#ja.Overview">Fluent Assertions</a> for general assertion usage and {@link ComparableAssertion} for supported operations on this type.
	 *
	 * @param <T> The value type.
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 * @return
	 * 	A new assertion object.
	 * 	<br>Never <jk>null</jk>.
	 */
	public static final <T extends Comparable<T>> ComparableAssertion<T> assertComparable(T value) {
		return ComparableAssertion.create(value);
	}

	/**
	 * Performs an assertion on a Date.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.assertions.Assertions.*;
	 *
	 * 	<jc>// Asserts the specified date is after the current date.</jc>
	 * 	<jsm>assertDate</jsm>(<jv>myDate</jv>)
	 * 		.isAfterNow();
	 * </p>
	 *
	 * <p>
	 * See <a class="doclink" href="../../../../index.html#ja.Overview">Fluent Assertions</a> for general assertion usage and {@link DateAssertion} for supported operations on this type.
	 *
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 * @return
	 * 	A new assertion object.
	 * 	<br>Never <jk>null</jk>.
	 */
	public static final DateAssertion assertDate(Date value) {
		return DateAssertion.create(value);
	}

	/**
	 * Performs an assertion on a double array.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.assertions.Assertions.*;
	 *
	 * 	<jc>// Asserts that a double array is at least size 100 and all values are greater than 1000.</jc>
	 * 	<jsm>assertDoubleArray</jsm>(<jv>myDoubleArray</jv>)
	 * 		.size().isGte(100f)
	 * 		.all(<jv>x</jv> -&gt; <jv>x</jv> &gt; 1000f);
	 * </p>
	 *
	 * <p>
	 * See <a class="doclink" href="../../../../index.html#ja.Overview">Fluent Assertions</a> for general assertion usage and {@link PrimitiveArrayAssertion} for supported operations on this type.
	 *
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 * @return
	 * 	A new assertion object.
	 * 	<br>Never <jk>null</jk>.
	 */
	public static final PrimitiveArrayAssertion<Double,double[]> assertDoubleArray(double[] value) {
		return PrimitiveArrayAssertion.create(value);
	}

	/**
	 * Performs an assertion on a float array.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.assertions.Assertions.*;
	 *
	 * 	<jc>// Asserts that a float array is at least size 100 and all values are greater than 1000.</jc>
	 * 	<jsm>assertFloatArray</jsm>(<jv>myFloatArray</jv>)
	 * 		.size().isGte(100f)
	 * 		.all(<jv>x</jv> -&gt; <jv>x</jv> &gt; 1000f);
	 * </p>
	 *
	 * <p>
	 * See <a class="doclink" href="../../../../index.html#ja.Overview">Fluent Assertions</a> for general assertion usage and {@link PrimitiveArrayAssertion} for supported operations on this type.
	 *
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 * @return
	 * 	A new assertion object.
	 * 	<br>Never <jk>null</jk>.
	 */
	public static final PrimitiveArrayAssertion<Float,float[]> assertFloatArray(float[] value) {
		return PrimitiveArrayAssertion.create(value);
	}

	/**
	 * Performs an assertion on an int array.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.assertions.Assertions.*;
	 *
	 * 	<jc>// Asserts that a double array is at least size 100 and all values are greater than 1000.</jc>
	 * 	<jsm>assertIntArray</jsm>(<jv>myIntArray</jv>)
	 * 		.size().isGte(100)
	 * 		.all(<jv>x</jv> -&gt; <jv>x</jv> &gt; 1000);
	 * </p>
	 *
	 * <p>
	 * See <a class="doclink" href="../../../../index.html#ja.Overview">Fluent Assertions</a> for general assertion usage and {@link PrimitiveArrayAssertion} for supported operations on this type.
	 *
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 * @return
	 * 	A new assertion object.
	 * 	<br>Never <jk>null</jk>.
	 */
	public static final PrimitiveArrayAssertion<Integer,int[]> assertIntArray(int[] value) {
		return PrimitiveArrayAssertion.create(value);
	}

	/**
	 * Performs an assertion on an Integer.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.assertions.Assertions.*;
	 *
	 * 	<jc>// Assert that an HTTP response status code is 200 or 404.</jc>
	 * 	<jsm>assertInteger</jsm>(<jv>httpReponse</jv>)
	 * 		.isAny(200,404);
	 * </p>
	 *
	 * <p>
	 * See <a class="doclink" href="../../../../index.html#ja.Overview">Fluent Assertions</a> for general assertion usage and {@link IntegerAssertion} for supported operations on this type.
	 *
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 * @return
	 * 	A new assertion object.
	 * 	<br>Never <jk>null</jk>.
	 */
	public static final IntegerAssertion assertInteger(Integer value) {
		return IntegerAssertion.create(value);
	}

	/**
	 * Performs an assertion on a list of POJOs.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.assertions.Assertions.*;
	 *
	 * 	<jc>// Assert that the first entry in a list is "{foo:'bar'}" when serialized to simplified JSON.</jc>
	 * 	<jsm>assertList</jsm>(<jv>myList</jv>)
	 * 		.item(0)
	 * 			.asJson().is(<js>"{foo:'bar'}"</js>);
	 * </p>
	 *
	 * <p>
	 * See <a class="doclink" href="../../../../index.html#ja.Overview">Fluent Assertions</a> for general assertion usage and {@link ListAssertion} for supported operations on this type.
	 *
	 * @param <E> The element type.
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 * @return
	 * 	A new assertion object.
	 * 	<br>Never <jk>null</jk>.
	 */
	public static final <E> ListAssertion<E> assertList(List<E> value) {
		return ListAssertion.create(value);
	}

	/**
	 * Performs an assertion on a stream of POJOs.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.assertions.Assertions.*;
	 *
	 * 	<jc>// Assert that the first entry in a list is "{foo:'bar'}" when serialized to simplified JSON.</jc>
	 * 	<jsm>assertList</jsm>(<jv>myStream</jv>)
	 * 		.item(0)
	 * 			.asJson().is(<js>"{foo:'bar'}"</js>);
	 * </p>
	 *
	 * <p>
	 * See <a class="doclink" href="../../../../index.html#ja.Overview">Fluent Assertions</a> for general assertion usage and {@link ListAssertion} for supported operations on this type.
	 *
	 * @param <E> The element type.
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 * @return
	 * 	A new assertion object.
	 * 	<br>Never <jk>null</jk>.
	 */
	public static final <E> ListAssertion<E> assertList(Stream<E> value) {
		return ListAssertion.create(value);
	}

	/**
	 * Performs an assertion on a Long.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.assertions.Assertions.*;
	 *
	 * 	<jc>// Throw a BadReqest if an HTTP response length is greater than 100k.</jc>
	 * 	<jsm>assertLong</jsm>(<jv>responseLength</jv>)
	 * 		.throwable(BadRequest.<jk>class</jk>)
	 * 		.msg(<js>"Request is too large"</js>)
	 * 		.isLt(100000);
	 * </p>
	 *
	 * <p>
	 * See <a class="doclink" href="../../../../index.html#ja.Overview">Fluent Assertions</a> for general assertion usage and {@link LongAssertion} for supported operations on this type.
	 *
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 * @return
	 * 	A new assertion object.
	 * 	<br>Never <jk>null</jk>.
	 */
	public static final LongAssertion assertLong(Long value) {
		return LongAssertion.create(value);
	}

	/**
	 * Performs an assertion on a long array.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.assertions.Assertions.*;
	 *
	 * 	<jc>// Asserts that a long array is at least size 100 and all values are greater than 1000.</jc>
	 * 	<jsm>assertLongArray</jsm>(<jv>myLongArray</jv>)
	 * 		.size().isGte(100)
	 * 		.all(<jv>x</jv> -&gt; <jv>x</jv> &gt; 1000);
	 * </p>
	 *
	 * <p>
	 * See <a class="doclink" href="../../../../index.html#ja.Overview">Fluent Assertions</a> for general assertion usage and {@link PrimitiveArrayAssertion} for supported operations on this type.
	 *
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 * @return
	 * 	A new assertion object.
	 * 	<br>Never <jk>null</jk>.
	 */
	public static final PrimitiveArrayAssertion<Long,long[]> assertLongArray(long[] value) {
		return PrimitiveArrayAssertion.create(value);
	}

	/**
	 * Performs an assertion on a map.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.assertions.Assertions.*;
	 *
	 * 	<jc>// Assert the specified map is a HashMap and contains the key "foo".</jc>
	 * 	<jsm>assertMap</jsm>(<jv>myMap</jv>)
	 * 		.isType(HashMap.<jk>class</jk>)
	 * 		.containsKey(<js>"foo"</js>);
	 * </p>
	 *
	 * <p>
	 * See <a class="doclink" href="../../../../index.html#ja.Overview">Fluent Assertions</a> for general assertion usage and {@link MapAssertion} for supported operations on this type.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 * @return
	 * 	A new assertion object.
	 * 	<br>Never <jk>null</jk>.
	 */
	public static final <K,V> MapAssertion<K,V> assertMap(Map<K,V> value) {
		return MapAssertion.create(value);
	}

	/**
	 * Performs an assertion on a Java Object.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.assertions.Assertions.*;
	 *
	 * 	<jc>// Asserts the specified POJO is of type MyBean and is "{foo:'bar'}" </jc>
	 * 	<jc>// when serialized to Simplified JSON.</jc>
	 * 	<jsm>assertObject</jsm>(<jv>myPojo</jv>)
	 * 		.isType(MyBean.<jk>class</jk>)
	 * 		.asJson().is(<js>"{foo:'bar'}"</js>);
	 * </p>
	 *
	 * <p>
	 * See <a class="doclink" href="../../../../index.html#ja.Overview">Fluent Assertions</a> for general assertion usage and {@link ObjectAssertion} for supported operations on this type.
	 *
	 * @param <T> The value type.
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 * @return
	 * 	A new assertion object.
	 * 	<br>Never <jk>null</jk>.
	 */
	public static final <T> ObjectAssertion<T> assertObject(T value) {
		return ObjectAssertion.create(value);
	}

	/**
	 * Performs an assertion on a Java Object wrapped in an Optional.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.assertions.Assertions.*;
	 *
	 * 	<jc>// Asserts the specified POJO is of type MyBean and is "{foo:'bar'}" </jc>
	 * 	<jc>// when serialized to Simplified JSON.</jc>
	 * 	<jsm>assertOptional</jsm>(<jv>opt</jv>)
	 * 		.isType(MyBean.<jk>class</jk>)
	 * 		.asJson().is(<js>"{foo:'bar'}"</js>);
	 * </p>
	 *
	 * <p>
	 * See <a class="doclink" href="../../../../index.html#ja.Overview">Fluent Assertions</a> for general assertion usage and {@link AnyAssertion} for supported operations on this type.
	 *
	 * @param <T> The value type.
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 * @return
	 * 	A new assertion object.
	 * 	<br>Never <jk>null</jk>.
	 */
	public static final <T> AnyAssertion<T> assertOptional(Optional<T> value) {
		return AnyAssertion.create(value.orElse(null));
	}

	/**
	 * Performs an assertion on the contents of a Reader.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.assertions.Assertions.*;
	 *
	 * 	<jc>// Asserts the contents of the Reader contains "foo".</jc>
	 * 	<jsm>assertReader</jsm>(<jv>myReader</jv>)
	 * 		.contains(<js>"foo"</js>);
	 * </p>
	 *
	 * <p>
	 * See <a class="doclink" href="../../../../index.html#ja.Overview">Fluent Assertions</a> for general assertion usage and {@link StringAssertion} for supported operations on this type.
	 *
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br>Reader is automatically closed.
	 * @return
	 * 	A new assertion object.
	 * 	<br>Never <jk>null</jk>.
	 * @throws IOException If thrown while reading contents from reader.
	 */
	public static final StringAssertion assertReader(Reader value) throws IOException {
		return assertString(read(value));
	}

	/**
	 * Performs an assertion on a short array.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.assertions.Assertions.*;
	 *
	 * 	<jc>// Asserts that a float array is at least size 10 and all values are greater than 100.</jc>
	 * 	<jsm>assertShortArray</jsm>(<jv>myShortArray</jv>)
	 * 		.size().isGte(10)
	 * 		.all(<jv>x</jv> -&gt; <jv>x</jv> &gt; 100);
	 * </p>
	 *
	 * <p>
	 * See <a class="doclink" href="../../../../index.html#ja.Overview">Fluent Assertions</a> for general assertion usage and {@link PrimitiveArrayAssertion} for supported operations on this type.
	 *
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 * @return
	 * 	A new assertion object.
	 * 	<br>Never <jk>null</jk>.
	 */
	public static final PrimitiveArrayAssertion<Short,short[]> assertShortArray(short[] value) {
		return PrimitiveArrayAssertion.create(value);
	}

	/**
	 * Performs an assertion on a String.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.assertions.Assertions.*;
	 *
	 *	<jc>// Asserts a string is at least 100 characters long and contains "foo".</jc>
	 * 	<jsm>assertString</jsm>(<jv>myString</jv>)
	 * 		.size().isGte(100)
	 * 		.contains(<js>"foo"</js>);
	 * </p>
	 *
	 * <p>
	 * See <a class="doclink" href="../../../../index.html#ja.Overview">Fluent Assertions</a> for general assertion usage and {@link StringAssertion} for supported operations on this type.
	 *
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 * @return
	 * 	A new assertion object.
	 * 	<br>Never <jk>null</jk>.
	 */
	public static final StringAssertion assertString(Object value) {
		if (value instanceof Optional)
			value = ((Optional<?>)value).orElse(null);
		return StringAssertion.create(value);
	}

	/**
	 * Performs an assertion on a list of Strings.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.assertions.Assertions.*;
	 *
	 *	<jc>// Asserts a list of strings contain "foo,bar,baz" after trimming all and joining.</jc>
	 * 	<jsm>assertStringList</jsm>(<jv>myListOfStrings</jv>)
	 * 		.isSize(3)
	 * 		.trim()
	 * 		.join(<js>","</js>)
	 * 		.is(<js>"foo,bar,baz"</js>);
	 * </p>
	 *
	 * <p>
	 * See <a class="doclink" href="../../../../index.html#ja.Overview">Fluent Assertions</a> for general assertion usage and {@link StringListAssertion} for supported operations on this type.
	 *
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 * @return
	 * 	A new assertion object.
	 * 	<br>Never <jk>null</jk>.
	 */
	public static final StringListAssertion assertStringList(List<String> value) {
		return StringListAssertion.create(value);
	}

	/**
	 * Performs an assertion on a Throwable.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.assertions.Assertions.*;
	 *
	 * 	<jc>// Asserts a throwable is a RuntimeException containing 'foobar' in the message.</jc>
	 * 	<jsm>assertThrowable</jsm>(<jv>throwable</jv>)
	 * 		.isExactType(RuntimeException.<jk>class</jk>)
	 * 		.message().contains(<js>"foobar"</js>);
	 * </p>
	 *
	 * <p>
	 * See <a class="doclink" href="../../../../index.html#ja.Overview">Fluent Assertions</a> for general assertion usage and {@link ThrowableAssertion} for supported operations on this type.
	 *
	 * @param <T> The value type.
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 * @return
	 * 	A new assertion object.
	 * 	<br>Never <jk>null</jk>.
	 */
	public static final <T extends Throwable> ThrowableAssertion<T> assertThrowable(T value) {
		return ThrowableAssertion.create(value);
	}

	/**
	 * Performs an assertion on a Version.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.assertions.Assertions.*;
	 *
	 * 	<jc>// Asserts the specified major version is at least 2.</jc>
	 * 	<jsm>assertVersion</jsm>(<jv>version</jv>)
	 * 		.major().isGte(2);
	 * </p>
	 *
	 * <p>
	 * See <a class="doclink" href="../../../../index.html#ja.Overview">Fluent Assertions</a> for general assertion usage and {@link VersionAssertion} for supported operations on this type.
	 *
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 * @return
	 * 	A new assertion object.
	 * 	<br>Never <jk>null</jk>.
	 */
	public static final VersionAssertion assertVersion(Version value) {
		return VersionAssertion.create(value);
	}

	/**
	 * Performs an assertion on a ZonedDateTime.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.assertions.Assertions.*;
	 *
	 * 	<jc>// Asserts the specified date is after the current date.</jc>
	 * 	<jsm>assertZonedDateTime</jsm>(<jv>myZonedDateTime</jv>)
	 * 		.isAfterNow();
	 * </p>
	 *
	 * <p>
	 * See <a class="doclink" href="../../../../index.html#ja.Overview">Fluent Assertions</a> for general assertion usage and {@link ZonedDateTimeAssertion} for supported operations on this type.
	 *
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 * @return
	 * 	A new assertion object.
	 * 	<br>Never <jk>null</jk>.
	 */
	public static final ZonedDateTimeAssertion assertZonedDateTime(ZonedDateTime value) {
		return ZonedDateTimeAssertion.create(value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Snippet assertions
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Executes an arbitrary snippet of code and captures anything thrown from it as a Throwable assertion.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.assertions.Assertions.*;
	 *
	 * 	<jc>// Asserts that the specified method throws a RuntimeException containing "foobar" in the message. </jc>
	 * 	<jsm>assertThrown</jsm>(()-&gt;<jv>foo</jv>.getBar())
	 * 		.isType(RuntimeException.<jk>class</jk>)
	 * 		.message().contains(<js>"foobar"</js>);
	 * </p>
	 *
	 * @param snippet The snippet of code to execute.
	 * @return A new assertion object.  Never <jk>null</jk>.
	 */
	public static final ThrowableAssertion<Throwable> assertThrown(Snippet snippet) {
		try {
			snippet.run();
		} catch (Throwable e) {
			return assertThrowable(e);
		}
		return assertThrowable(null);
	}
}
