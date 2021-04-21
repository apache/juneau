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

import static org.apache.juneau.internal.ExceptionUtils.*;
import static org.apache.juneau.internal.IOUtils.*;

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
	public static final DateAssertion assertDate(Date value) {
		return DateAssertion.create(value);
	}

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
	public static final DateAssertion assertDate(Optional<Date> value) {
		assertArgNotNull("value", value);
		return assertDate(value.orElse(null));
	}

	/**
	 * Used for assertion calls against {@link Version} objects.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the specified major version is greater than 2.</jc>
	 * 	<jsm>assertVersion</jsm>(<jv>version</jv>).major().isGreaterThan(2);
	 * </p>
	 *
	 * @param value The version object being wrapped.
	 * @return A new {@link VersionAssertion} object.  Never <jk>null</jk>.
	 */
	public static final VersionAssertion assertVersion(Version value) {
		return VersionAssertion.create(value);
	}

	/**
	 * Used for assertion calls against {@link Version} objects.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the specified major version is greater than 2.</jc>
	 * 	<jsm>assertVersion</jsm>(<jv>version</jv>).major().isGreaterThan(2);
	 * </p>
	 *
	 * @param value The version object being wrapped.
	 * @return A new {@link VersionAssertion} object.  Never <jk>null</jk>.
	 */
	public static final VersionAssertion assertVersion(Optional<Version> value) {
		assertArgNotNull("value", value);
		return assertVersion(value.orElse(null));
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
	public static final ZonedDateTimeAssertion assertZonedDateTime(ZonedDateTime value) {
		return ZonedDateTimeAssertion.create(value);
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
	public static final ZonedDateTimeAssertion assertZonedDateTime(Optional<ZonedDateTime> value) {
		assertArgNotNull("value", value);
		return assertZonedDateTime(value.orElse(null));
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
	public static final IntegerAssertion assertInteger(Integer value) {
		return IntegerAssertion.create(value);
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
	public static final IntegerAssertion assertInteger(Optional<Integer> value) {
		assertArgNotNull("value", value);
		return assertInteger(value.orElse(null));
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
	public static final LongAssertion assertLong(Long value) {
		return LongAssertion.create(value);
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
	public static final LongAssertion assertLong(Optional<Long> value) {
		assertArgNotNull("value", value);
		return assertLong(value.orElse(null));
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
	public static final ComparableAssertion assertComparable(Comparable<?> value) {
		return ComparableAssertion.create(value);
	}

	/**
	 * Used for assertion calls against arbitrary POJOs.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the specified POJO is the specified type and serializes to the specified value.</jc>
	 * 	<jsm>assertObject</jsm>(<jv>myPojo</jv>).isType(MyBean.<jk>class</jk>).asJson().is(<js>"{foo:'bar'}"</js>);
	 * </p>
	 *
	 * @param value The object being wrapped.
	 * @return A new {@link ObjectAssertion} object.  Never <jk>null</jk>.
	 */
	public static final <V> ObjectAssertion<V> assertObject(V value) {
		return ObjectAssertion.create(value);
	}

	/**
	 * Used for assertion calls against arbitrary POJOs.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the specified POJO is the specified type and serializes to the specified value.</jc>
	 * 	<jsm>assertObject</jsm>(<jv>myPojo</jv>).isType(MyBean.<jk>class</jk>).asJson().is(<js>"{foo:'bar'}"</js>);
	 * </p>
	 *
	 * @param value The object being wrapped.
	 * @return A new {@link ObjectAssertion} object.  Never <jk>null</jk>.
	 */
	public static final <V> ObjectAssertion<V> assertObject(Optional<V> value) {
		assertArgNotNull("value", value);
		return assertObject(value.orElse(null));
	}

	/**
	 * Used for assertion calls against Java beans.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the specified POJO is the specified type and serializes to the specified value.</jc>
	 * 	<jsm>assertBean</jsm>(<jv>myBean</jv>).isType(MyBean.<jk>class</jk>).fields(<js>"foo"</js>).asJson().is(<js>"{foo:'bar'}"</js>);
	 * </p>
	 *
	 * @param value The object being wrapped.
	 * @return A new {@link BeanAssertion} object.  Never <jk>null</jk>.
	 */
	public static final <V> BeanAssertion<V> assertBean(V value) {
		return BeanAssertion.create(value);
	}

	/**
	 * Used for assertion calls against Java beans.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the specified POJO is the specified type and serializes to the specified value.</jc>
	 * 	<jsm>assertBean</jsm>(<jv>myBean</jv>).isType(MyBean.<jk>class</jk>).fields(<js>"foo"</js>).asJson().is(<js>"{foo:'bar'}"</js>);
	 * </p>
	 *
	 * @param value The object being wrapped.
	 * @return A new {@link BeanAssertion} object.  Never <jk>null</jk>.
	 */
	public static final <V> BeanAssertion<V> assertBean(Optional<V> value) {
		assertArgNotNull("value", value);
		return assertBean(value.orElse(null));
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
	public static final StringAssertion assertString(Object value) {
		return StringAssertion.create(value);
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
	public static final StringAssertion assertString(Optional<?> value) {
		assertArgNotNull("value", value);
		return assertString(value.orElse(null));
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
	public static final BooleanAssertion assertBoolean(Boolean value) {
		return BooleanAssertion.create(value);
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
	public static final BooleanAssertion assertBoolean(Optional<Boolean> value) {
		assertArgNotNull("value", value);
		return assertBoolean(value.orElse(null));
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
	public static final <V extends Throwable> ThrowableAssertion<V> assertThrowable(V value) {
		return ThrowableAssertion.create(value);
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
	public static final ArrayAssertion assertArray(Object value) {
		return ArrayAssertion.create(value);
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
	public static final CollectionAssertion assertCollection(Collection<?> value) {
		return CollectionAssertion.create(value);
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
	public static final ListAssertion assertList(List<?> value) {
		return ListAssertion.create(value);
	}

	/**
	 * Used for assertion calls against maps.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the specified POJO is the specified type and contains the specified key.</jc>
	 * 	<jsm>assertMap</jsm>(<jv>myMap</jv>).isType(HashMap.<jk>class</jk>).containsKey(<js>"foo"</js>);
	 * </p>
	 *
	 * @param value The object being wrapped.
	 * @return A new {@link MapAssertion} object.  Never <jk>null</jk>.
	 */
	@SuppressWarnings("rawtypes")
	public static final MapAssertion assertMap(Map value) {
		return MapAssertion.create(value);
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
	public static final ThrowableAssertion<Throwable> assertThrown(Snippet snippet) {
		return assertThrown(Throwable.class, snippet);
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
	 * @param type The expected exception type.
	 * @param snippet The snippet of code to execute.
	 * @return A new assertion object.  Never <jk>null</jk>.
	 */
	@SuppressWarnings("unchecked")
	public static final <T extends Throwable> ThrowableAssertion<Throwable> assertThrown(Class<T> type, Snippet snippet) {
		try {
			snippet.run();
		} catch (Throwable e) {
			if (type.isInstance(e))
				return assertThrowable((T)e);
			throw new BasicAssertionError("Exception not of expected type.\n\tExpected: {1}.\n\tActual: {2}", type, e.getClass());
		}
		return assertThrowable(null);
	}

	/**
	 * Used for assertion calls against the contents of input streams.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates that the stream contains the string "foo".</jc>
	 * 	<jsm>assertStream</jsm>(<jv>myStream</jv>).asHex().is(<js>"666F6F"</js>);
	 * </p>
	 *
	 * @param value
	 * 	The input stream being wrapped.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br>Stream is automatically closed.
	 * @return A new {@link ByteArrayAssertion} object.  Never <jk>null</jk>.
	 * @throws IOException If thrown while reading contents from stream.
	 */
	public static final ByteArrayAssertion assertStream(InputStream value) throws IOException {
		return assertBytes(value == null ? null : readBytes(value));
	}

	/**
	 * Used for assertion calls against the contents of input streams.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates that the stream contains the string "foo".</jc>
	 * 	<jsm>assertStream</jsm>(<jv>myStream</jv>).asHex().is(<js>"666F6F"</js>);
	 * </p>
	 *
	 * @param value The input stream being wrapped.
	 * @return A new {@link ByteArrayAssertion} object.  Never <jk>null</jk>.
	 * @throws IOException If thrown while reading contents from stream.
	 */
	public static final ByteArrayAssertion assertStream(Optional<InputStream> value) throws IOException {
		assertArgNotNull("value", value);
		return assertStream(value.orElse(null));
	}

	/**
	 * Used for assertion calls against byte arrays.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates that the byte array contains the string "foo".</jc>
	 * 	<jsm>assertBytes</jsm>(<jv>myBytes</jv>).asHex().is(<js>"666F6F"</js>);
	 * </p>
	 *
	 * @param value The byte array being wrapped.
	 * @return A new {@link ByteArrayAssertion} object.  Never <jk>null</jk>.
	 */
	public static final ByteArrayAssertion assertBytes(byte[] value) {
		return ByteArrayAssertion.create(value);
	}

	/**
	 * Used for assertion calls against byte arrays.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates that the byte array contains the string "foo".</jc>
	 * 	<jsm>assertBytes</jsm>(<jv>myBytes</jv>).asHex().is(<js>"666F6F"</js>);
	 * </p>
	 *
	 * @param value The byte array being wrapped.
	 * @return A new {@link ByteArrayAssertion} object.  Never <jk>null</jk>.
	 */
	public static final ByteArrayAssertion assertBytes(Optional<byte[]> value) {
		assertArgNotNull("value", value);
		return assertBytes(value.orElse(null));
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
	 * @param value The reader being wrapped.
	 * @return A new {@link StringAssertion} object.  Never <jk>null</jk>.
	 * @throws IOException If thrown while reading contents from reader.
	 */
	public static final StringAssertion assertReader(Reader value) throws IOException {
		return assertString(read(value));
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
	 * @param value The reader being wrapped.
	 * @return A new {@link StringAssertion} object.  Never <jk>null</jk>.
	 * @throws IOException If thrown while reading contents from reader.
	 */
	public static final StringAssertion assertReader(Optional<Reader> value) throws IOException {
		assertArgNotNull("value", value);
		return assertReader(value.orElse(null));
	}

	/**
	 * Throws an {@link IllegalArgumentException} if the specified argument is <jk>null</jk>.
	 *
	 * @param <T> The argument data type.
	 * @param name The argument name.
	 * @param o The object to check.
	 * @return The same argument.
	 * @throws IllegalArgumentException Constructed exception.
	 */
	public static final <T> T assertArgNotNull(String name, T o) throws IllegalArgumentException {
		assertArg(o != null, "Argument ''{0}'' cannot be null", name);
		return o;
	}

	/**
	 * Throws an {@link IllegalArgumentException} if the specified expression is <jk>false</jk>.
	 *
	 * @param expression The boolean expression to check.
	 * @param msg The exception message.
	 * @param args The exception message args.
	 * @throws IllegalArgumentException Constructed exception.
	 */
	public static final void assertArg(boolean expression, String msg, Object...args) throws IllegalArgumentException {
		if (! expression)
			throw illegalArgumentException(msg, args);
	}
}
