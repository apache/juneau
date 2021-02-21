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

import static org.apache.juneau.internal.StringUtils.*;

import java.nio.charset.*;

import org.apache.juneau.internal.*;

/**
 * Used for fluent assertion calls against byte arrays.
 *
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentByteArrayAssertion<R>")
public class FluentByteArrayAssertion<R> extends FluentArrayAssertion<R> {

	private byte[] value;

	/**
	 * Constructor.
	 *
	 * @param contents The byte array being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentByteArrayAssertion(byte[] contents, R returns) {
		this(null, contents, returns);
	}

	/**
	 * Constructor.
	 *
	 * @param creator The assertion that created this assertion.
	 * @param contents The byte array being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentByteArrayAssertion(Assertion creator, byte[] contents, R returns) {
		super(creator, contents, returns);
		this.value = contents;
	}

	/**
	 * Converts this byte array to a UTF-8 encoded string and returns it as a new assertion.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates that the specified byte array contains the string "foobar".</jc>
	 * 	<jsm>assertBytes<jsm>(myByteArray).asString().is(<js>"foobar"</js>);
	 * </p>
	 *
	 * @return A new fluent string assertion.
	 */
	@Override
	public FluentStringAssertion<R> asString() {
		return asString(IOUtils.UTF8);
	}

	/**
	 * Converts this byte array to a string and returns it as a new assertion.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates that the specified byte array contains the string "foobar" encoded in ASCII.</jc>
	 * 	<jsm>assertBytes<jsm>(myByteArray).string(<js>"iso8859-1"</js>).is(<js>"foobar"</js>);
	 * </p>
	 *
	 * @param cs The charset to use to decode the string.
	 * @return A new fluent string assertion.
	 */
	public FluentStringAssertion<R> asString(Charset cs) {
		return new FluentStringAssertion<>(this, value == null ? null : new String(value, cs), returns());
	}

	/**
	 * Converts this byte array to a base-64 encoded string and returns it as a new assertion.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates that the specified byte array contains the string "foo".</jc>
	 * 	<jsm>assertBytes<jsm>(myByteArray).base64().is(<js>"Zm9v"</js>);
	 * </p>
	 *
	 * @return A new fluent string assertion.
	 */
	public FluentStringAssertion<R> asBase64() {
		return new FluentStringAssertion<>(this, value == null ? null : base64Encode(value), returns());
	}

	/**
	 * Converts this byte array to hexadecimal and returns it as a new assertion.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates that the specified byte array contains the string "foo".</jc>
	 * 	<jsm>assertBytes<jsm>(myByteArray).asHex().is(<js>"666F6F"</js>);
	 * </p>
	 *
	 * @return A new string consisting of hexadecimal characters.
	 */
	public FluentStringAssertion<R> asHex() {
		return new FluentStringAssertion<>(this, value == null ? null : toHex(value), returns());
	}

	/**
	 * Converts this byte array to spaced hexadecimal and returns it as a new assertion.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates that the specified byte array contains the string "foo".</jc>
	 * 	<jsm>assertBytes<jsm>(myByteArray).asSpacedHex().is(<js>"66 6F 6F"</js>);
	 * </p>
	 *
	 * @return A new string consisting of hexadecimal characters.
	 */
	public FluentStringAssertion<R> asSpacedHex() {
		return new FluentStringAssertion<>(this, value == null ? null : toSpacedHex(value), returns());
	}

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public FluentByteArrayAssertion<R> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentByteArrayAssertion<R> stderr() {
		super.stderr();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentByteArrayAssertion<R> stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentByteArrayAssertion<R> throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	// </FluentSetters>
}
