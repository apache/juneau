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

import java.io.*;
import java.nio.charset.*;
import java.util.function.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;

/**
 * Used for fluent assertion calls against byte arrays.
 * {@review}
 *
 *
 * <h5 class='topic'>Test Methods</h5>
 * 	<ul>
 * 		<li class='jm'>{@link FluentPrimitiveArrayAssertion#has(Object...)}
 * 		<li class='jm'>{@link FluentPrimitiveArrayAssertion#is(Predicate)}
 * 		<li class='jm'>{@link FluentPrimitiveArrayAssertion#any(Predicate)}
 * 		<li class='jm'>{@link FluentPrimitiveArrayAssertion#all(Predicate)}
 * 		<li class='jm'>{@link FluentPrimitiveArrayAssertion#isEmpty()}
 * 		<li class='jm'>{@link FluentPrimitiveArrayAssertion#isNotEmpty()}
 * 		<li class='jm'>{@link FluentPrimitiveArrayAssertion#isSize(int)}
 * 		<li class='jm'>{@link FluentPrimitiveArrayAssertion#contains(Object)}
 * 		<li class='jm'>{@link FluentPrimitiveArrayAssertion#doesNotContain(Object)}
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
 * 		<li class='jm'>{@link FluentByteArrayAssertion#asString()}
 * 		<li class='jm'>{@link FluentByteArrayAssertion#asString(Charset)}
 * 		<li class='jm'>{@link FluentByteArrayAssertion#asBase64()}
 * 		<li class='jm'>{@link FluentByteArrayAssertion#asHex()}
 * 		<li class='jm'>{@link FluentByteArrayAssertion#asSpacedHex()}
 * 		<li class='jm'>{@link FluentPrimitiveArrayAssertion#item(int)}
 * 		<li class='jm'>{@link FluentPrimitiveArrayAssertion#length()}
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
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentByteArrayAssertion<R>")
public class FluentByteArrayAssertion<R> extends FluentPrimitiveArrayAssertion<Byte,byte[],R> {

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
	public FluentByteArrayAssertion(byte[] value, R returns) {
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
	public FluentByteArrayAssertion(Assertion creator, byte[] value, R returns) {
		super(creator, value, returns);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Transform methods
	//-----------------------------------------------------------------------------------------------------------------

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
		return new FluentStringAssertion<>(this, valueIsNull() ? null : new String(value(), cs), returns());
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
		return new FluentStringAssertion<>(this, valueIsNull() ? null : base64Encode(value()), returns());
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
		return new FluentStringAssertion<>(this, valueIsNull() ? null : toHex(value()), returns());
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
		return new FluentStringAssertion<>(this, valueIsNull() ? null : toSpacedHex(value()), returns());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fluent setters
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentByteArrayAssertion<R> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentByteArrayAssertion<R> out(PrintStream value) {
		super.out(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentByteArrayAssertion<R> silent() {
		super.silent();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentByteArrayAssertion<R> stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentByteArrayAssertion<R> throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	// </FluentSetters>
}
