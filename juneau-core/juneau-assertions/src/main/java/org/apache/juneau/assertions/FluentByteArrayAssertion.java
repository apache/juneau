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

import static org.apache.juneau.common.internal.StringUtils.*;

import java.io.*;
import java.nio.charset.*;
import java.util.function.*;

import org.apache.juneau.common.internal.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;

/**
 * Used for fluent assertion calls against byte arrays.
 *
 * <h5 class='section'>Test Methods:</h5>
 * <p>
 * <ul class='javatree'>
 * 	<li class='jc'>{@link FluentPrimitiveArrayAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentPrimitiveArrayAssertion#isHas(Object...) isHas(Object...)}
 * 		<li class='jm'>{@link FluentPrimitiveArrayAssertion#is(Predicate) is(Predicate)}
 * 		<li class='jm'>{@link FluentPrimitiveArrayAssertion#isAny(Predicate) isAny(Predicate)}
 * 		<li class='jm'>{@link FluentPrimitiveArrayAssertion#isAll(Predicate) isAll(Predicate)}
 * 		<li class='jm'>{@link FluentPrimitiveArrayAssertion#isEmpty() isEmpty()}
 * 		<li class='jm'>{@link FluentPrimitiveArrayAssertion#isNotEmpty() isNotEmpty()}
 * 		<li class='jm'>{@link FluentPrimitiveArrayAssertion#isSize(int) isSize(int)}
 * 		<li class='jm'>{@link FluentPrimitiveArrayAssertion#isContains(Object) isContains(Object)}
 * 		<li class='jm'>{@link FluentPrimitiveArrayAssertion#isNotContains(Object) isNotContains(Object)}
 * 	</ul>
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
 * 	<li class='jc'>{@link FluentByteArrayAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentByteArrayAssertion#asString() asString()}
 * 		<li class='jm'>{@link FluentByteArrayAssertion#asString(Charset) asString(Charset)}
 * 		<li class='jm'>{@link FluentByteArrayAssertion#asBase64() asBase64()}
 * 		<li class='jm'>{@link FluentByteArrayAssertion#asHex() asHex()}
 * 		<li class='jm'>{@link FluentByteArrayAssertion#asSpacedHex() asSpacedHex()}
 * 	</ul>
 * 	<li class='jc'>{@link FluentPrimitiveArrayAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentPrimitiveArrayAssertion#asItem(int) asItem(int)}
 * 		<li class='jm'>{@link FluentPrimitiveArrayAssertion#asLength() asLength()}
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
	 * <p class='bjava'>
	 * 	<jc>// Validates that the specified byte array contains the string "foobar".</jc>
	 * 	<jsm>assertBytes</jsm>(<jv>myByteArray</jv>).asString().is(<js>"foobar"</js>);
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
	 * <p class='bjava'>
	 * 	<jc>// Validates that the specified byte array contains the string "foobar" encoded in ASCII.</jc>
	 * 	<jsm>assertBytes</jsm>(<jv>myByteArray</jv>).asString(<js>"iso8859-1"</js>).is(<js>"foobar"</js>);
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
	 * <p class='bjava'>
	 * 	<jc>// Validates that the specified byte array contains the string "foo".</jc>
	 * 	<jsm>assertBytes</jsm>(<jv>myByteArray</jv>).asBase64().is(<js>"Zm9v"</js>);
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
	 * <p class='bjava'>
	 * 	<jc>// Validates that the specified byte array contains the string "foo".</jc>
	 * 	<jsm>assertBytes</jsm>(<jv>myByteArray</jv>).asHex().is(<js>"666F6F"</js>);
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
	 * <p class='bjava'>
	 * 	<jc>// Validates that the specified byte array contains the string "foo".</jc>
	 * 	<jsm>assertBytes</jsm>(<jv>myByteArray</jv>).asSpacedHex().is(<js>"66 6F 6F"</js>);
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
	public FluentByteArrayAssertion<R> setMsg(String msg, Object...args) {
		super.setMsg(msg, args);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentByteArrayAssertion<R> setOut(PrintStream value) {
		super.setOut(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentByteArrayAssertion<R> setSilent() {
		super.setSilent();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentByteArrayAssertion<R> setStdOut() {
		super.setStdOut();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentByteArrayAssertion<R> setThrowable(Class<? extends java.lang.RuntimeException> value) {
		super.setThrowable(value);
		return this;
	}

	// </FluentSetters>
}
