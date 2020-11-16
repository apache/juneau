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

import java.io.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;

/**
 * Main class for creation of assertions for testing.
 */
public class Assertions {

	/**
	 * Used for assertion calls against {@link Date} objects.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the specified date is after the current date.</jc>
	 * 	<jsm>assertDate</jsm>(<jv>myDate</jv>).isAfterNow();
	 * </p>
	 *
	 * @param value The date being wrapped.
	 * @return A new {@link DateAssertion} object.  Never <jk>null</jk>.
	 */
	public static DateAssertion assertDate(Date value) {
		return new DateAssertion(value);
	}

	/**
	 * Used for assertion calls against {@link ZonedDateTime} objects.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the specified date is after the current date.</jc>
	 * 	<jsm>assertZonedDateTime</jsm>(<jv>byZdt</jv>).isAfterNow();
	 * </p>
	 *
	 * @param value The date being wrapped.
	 * @return A new {@link ZonedDateTimeAssertion} object.  Never <jk>null</jk>.
	 */
	public static ZonedDateTimeAssertion assertZonedDateTimeAssertion(ZonedDateTime value) {
		return new ZonedDateTimeAssertion(value);
	}

	/**
	 * Used for assertion calls against integers.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the response status code is 200 or 404.</jc>
	 * 	<jsm>assertInteger</jsm>(<jv>httpReponse<jv>).isAny(200,404);
	 * </p>
	 *
	 * @param value The object being wrapped.
	 * @return A new {@link IntegerAssertion} object.  Never <jk>null</jk>.
	 */
	public static IntegerAssertion assertInteger(Integer value) {
		return new IntegerAssertion(value);
	}

	/**
	 * Used for assertion calls against longs.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the response length isn't too long.</jc>
	 * 	<jsm>assertLong</jsm>(<jv>responseLength</jv>).isLessThan(100000);
	 * </p>
	 *
	 * @param value The object being wrapped.
	 * @return A new {@link LongAssertion} object.  Never <jk>null</jk>.
	 */
	public static LongAssertion assertLong(Long value) {
		return new LongAssertion(value);
	}

	/**
	 * Used for assertion calls against longs.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the response length isn't too long.</jc>
	 * 	<jsm>assertLong</jsm>(<jv>responseLength</jv>).isLessThan(100000);
	 * </p>
	 *
	 * @param value The object being wrapped.
	 * @return A new {@link LongAssertion} object.  Never <jk>null</jk>.
	 */
	public static ComparableAssertion assertComparable(Comparable<?> value) {
		return new ComparableAssertion(value);
	}

	/**
	 * Used for assertion calls against arbitrary POJOs.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the specified POJO is the specified type and serializes to the specified value.</jc>
	 * 	<jsm>assertObject</jsm>(<jv>myPojo</jv>).instanceOf(MyBean.<jk>class</jk>).json().is(<js>"{foo:'bar'}"</js>);
	 * </p>
	 *
	 * @param value The object being wrapped.
	 * @return A new {@link ObjectAssertion} object.  Never <jk>null</jk>.
	 */
	public static ObjectAssertion assertObject(Object value) {
		return new ObjectAssertion(value);
	}

	/**
	 * Used for assertion calls against string objects.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the response body of an HTTP call is the text "OK".</jc>
	 * 	<jsm>assertString</jsm>(<jv>httpBody</jv>).is(<js>"OK"</js>);
	 * </p>
	 *
	 * @param value The string being wrapped.
	 * @return A new {@link StringAssertion} object.  Never <jk>null</jk>.
	 */
	public static StringAssertion assertString(Object value) {
		return new StringAssertion(value);
	}

	/**
	 * Used for assertion calls against boolean objects.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates that the specified boolean object exists and is true.</jc>
	 * 	<jsm>assertBoolean</jsm>(<jv>myBoolean</jv>).exists().isTrue();
	 * </p>
	 *
	 * @param value The boolean being wrapped.
	 * @return A new {@link BooleanAssertion} object.  Never <jk>null</jk>.
	 */
	public static BooleanAssertion assertBoolean(Boolean value) {
		return new BooleanAssertion(value);
	}

	/**
	 * Used for assertion calls against throwable objects.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the throwable message or one of the parent messages contain 'Foobar'.</jc>
	 * 	<jsm>assertThrowable</jsm>(<jv>throwable</jv>).contains(<js>"Foobar"</js>);
	 * </p>
	 *
	 * @param value The throwable being wrapped.
	 * @return A new {@link ThrowableAssertion} object.  Never <jk>null</jk>.
	 */
	public static ThrowableAssertion assertThrowable(Throwable value) {
		return new ThrowableAssertion(value);
	}

	/**
	 * Used for assertion calls against arrays.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	String[] <jv>array</jv> = <jk>new</jk> String[]{<js>"foo"</js>};
	 * 	<jsm>assertArray</jsm>(<jv>array</jv>).isSize(1);
	 * </p>
	 *
	 * @param value The object being wrapped.
	 * @return A new {@link ArrayAssertion} object.  Never <jk>null</jk>.
	 */
	public static ArrayAssertion assertArray(Object value) {
		return new ArrayAssertion(value);
	}

	/**
	 * Used for assertion calls against {@link Collection} objects.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	List=&lt;String&gt; <jv>list</jv> = AList.<jsm>of</jsm>(<js>"foo"</js>);
	 * 	<jsm>assertCollection</jsm>(<jv>list</jv>).isNotEmpty();
	 * </p>
	 *
	 * @param value The object being wrapped.
	 * @return A new {@link CollectionAssertion} object.  Never <jk>null</jk>.
	 */
	public static CollectionAssertion assertCollection(Collection<?> value) {
		return new CollectionAssertion(value);
	}

	/**
	 * Used for assertion calls against {@link Collection} objects.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	List=&lt;String&gt; <jv>list</jv> = AList.<jsm>of</jsm>(<js>"foo"</js>);
	 * 	<jsm>assertList</jsm>(<jv>list</jv>).item(0).isEqual(<js>"foo"</js>);
	 * </p>
	 *
	 * @param value The object being wrapped.
	 * @return A new {@link ListAssertion} object.  Never <jk>null</jk>.
	 */
	public static ListAssertion assertList(List<?> value) {
		return new ListAssertion(value);
	}

	/**
	 * Executes an arbitrary snippet of code and captures anything thrown from it.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Asserts that the specified method throws a RuntimeException containing "Foobar" in the message. </jc>
	 * 	<jsm>assertThrown</jsm>(()-&gt;<jv>foo</jv>.getBar())
	 * 		.exists()
	 * 		.isType(RuntimeException.<jk>class</jk>)
	 * 		.contains(<js>"Foobar"</js>);
	 * </p>
	 *
	 * @param snippet The snippet of code to execute.
	 * @return A new assertion object.  Never <jk>null</jk>.
	 */
	public static ThrowableAssertion assertThrown(Snippet snippet) {
		try {
			snippet.run();
		} catch (Throwable e) {
			return assertThrowable(e);
		}
		return assertThrowable(null);
	}

	/**
	 * Used for assertion calls against the contents of input streams.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates that the stream contains the string "foo".</jc>
	 * 	<jsm>assertStream</jsm>(<jv>myStream</jv>).hex().is(<js>"666F6F"</js>);
	 * </p>
	 *
	 * @param is The input stream being wrapped.
	 * @return A new {@link ByteArrayAssertion} object.  Never <jk>null</jk>.
	 * @throws IOException If thrown while reading contents from stream.
	 */
	public static ByteArrayAssertion assertStream(InputStream is) throws IOException {
		return new ByteArrayAssertion(is == null ? null : IOUtils.readBytes(is));
	}

	/**
	 * Used for assertion calls against byte arrays.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates that the byte array contains the string "foo".</jc>
	 * 	<jsm>assertBytes</jsm>(<jv>myBytes</jv>).hex().is(<js>"666F6F"</js>);
	 * </p>
	 *
	 * @param bytes The byte array being wrapped.
	 * @return A new {@link ByteArrayAssertion} object.  Never <jk>null</jk>.
	 */
	public static ByteArrayAssertion assertBytes(byte[] bytes) {
		return new ByteArrayAssertion(bytes);
	}

	/**
	 * Used for assertion calls against the contents of readers.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the throwable message or one of the parent messages contain 'Foobar'.</jc>
	 * 	<jsm>assertReader</jsm>(<jv>myReader</jv>).is(<js>"foo"</js>);
	 * </p>
	 *
	 * @param r The reader being wrapped.
	 * @return A new {@link StringAssertion} object.  Never <jk>null</jk>.
	 * @throws IOException If thrown while reading contents from reader.
	 */
	public static StringAssertion assertReader(Reader r) throws IOException {
		return new StringAssertion(r == null ? null : IOUtils.read(r));
	}

	/**
	 * Throws an {@link IllegalArgumentException} if the specified argument is <jk>null</jk>.
	 *
	 * @param <T> The argument data type.
	 * @param arg The argument name.
	 * @param o The object to check.
	 * @return The same argument.
	 * @throws IllegalArgumentException Constructed exception.
	 */
	public static <T> T assertArgNotNull(String arg, T o) throws IllegalArgumentException {
		if (o == null)
			throw new BasicIllegalArgumentException("Argument ''{0}'' cannot be null", arg);
		return o;
	}
}
