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
import java.util.*;

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
	 * 	<jsm>assertDate</jsm>(myDate).isAfterNow();
	 * </p>
	 *
	 * @param date The date being wrapped.
	 * @return A new {@link DateAssertion} object.  Never <jk>null</jk>.
	 */
	public static DateAssertion assertDate(Date date) {
		return new DateAssertion(date);
	}

	/**
	 * Used for assertion calls against integers.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the response status code is 200 or 404.</jc>
	 * 	<jsm>assertInteger</jsm>(httpReponse).isAny(200,404);
	 * </p>
	 *
	 * @param integer The object being wrapped.
	 * @return A new {@link IntegerAssertion} object.  Never <jk>null</jk>.
	 */
	public static IntegerAssertion assertInteger(Integer integer) {
		return new IntegerAssertion(integer);
	}

	/**
	 * Used for assertion calls against longs.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the response length isn't too long.</jc>
	 * 	<jsm>assertLong</jsm>(responseLength).isLessThan(100000);
	 * </p>
	 *
	 * @param l The object being wrapped.
	 * @return A new {@link LongAssertion} object.  Never <jk>null</jk>.
	 */
	public static LongAssertion assertLong(Long l) {
		return new LongAssertion(l);
	}

	/**
	 * Used for assertion calls against arbitrary POJOs.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the specified POJO is the specified type and serializes to the specified value.</jc>
	 * 	<jsm>assertObject</jsm>(myPojo).instanceOf(MyBean.<jk>class</jk>).json().is(<js>"{foo:'bar'}"</js>);
	 * </p>
	 *
	 * @param object The object being wrapped.
	 * @return A new {@link ObjectAssertion} object.  Never <jk>null</jk>.
	 */
	public static ObjectAssertion assertObject(Object object) {
		return new ObjectAssertion(object);
	}

	/**
	 * Used for assertion calls against string objects.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the response body of an HTTP call is the text "OK".</jc>
	 * 	<jsm>assertString</jsm>(httpBody).is(<js>"OK"</js>);
	 * </p>
	 *
	 * @param text The string being wrapped.
	 * @return A new {@link StringAssertion} object.  Never <jk>null</jk>.
	 */
	public static StringAssertion assertString(Object text) {
		return new StringAssertion(text);
	}

	/**
	 * Used for assertion calls against throwable objects.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the throwable message or one of the parent messages contain 'Foobar'.</jc>
	 * 	<jsm>assertThrowable</jsm>(throwable).contains(<js>"Foobar"</js>);
	 * </p>
	 *
	 * @param throwable The throwable being wrapped.
	 * @return A new {@link ThrowableAssertion} object.  Never <jk>null</jk>.
	 */
	public static ThrowableAssertion assertThrowable(Throwable throwable) {
		return new ThrowableAssertion(throwable);
	}

	/**
	 * Executes an arbitrary snippet of code and captures anything thrown from it.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Asserts that the specified method throws a RuntimeException containing "Foobar" in the message. </jc>
	 * 	<jsm>assertThrown</jsm>(() -> {foo.getBar();})
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
	 * 	<jsm>assertStream</jsm>(myStream).hex().is(<js>"666F6F"</js>);
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
	 * 	<jsm>assertBytes</jsm>(myBytes).hex().is(<js>"666F6F"</js>);
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
	 * 	<jsm>assertReader</jsm>(myReader).is(<js>"foo"</js>);
	 * </p>
	 *
	 * @param r The reader being wrapped.
	 * @return A new {@link StringAssertion} object.  Never <jk>null</jk>.
	 * @throws IOException If thrown while reading contents from reader.
	 */
	public static StringAssertion assertReader(Reader r) throws IOException {
		return new StringAssertion(r == null ? null : IOUtils.read(r));
	}
}
