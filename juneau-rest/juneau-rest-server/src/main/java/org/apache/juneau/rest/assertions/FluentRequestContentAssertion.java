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
package org.apache.juneau.rest.assertions;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.assertions.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.httppart.*;
import org.apache.juneau.serializer.*;

/**
 * Used for fluent assertion calls against {@link RequestContent} objects.
 *
 * <h5 class='topic'>Test Methods</h5>
 * <p>
 * <ul class='javatree'>
 * 	<li class='jc'>{@link FluentRequestContentAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentRequestContentAssertion#is(String) is(String)}
 * 		<li class='jm'>{@link FluentRequestContentAssertion#isContains(String...) isContains(String...)}
 * 		<li class='jm'>{@link FluentRequestContentAssertion#isNotContains(String...) isNotContains(String...)}
 * 		<li class='jm'>{@link FluentRequestContentAssertion#isEmpty() isEmpty()}
 * 		<li class='jm'>{@link FluentRequestContentAssertion#isNotEmpty() isNotEmpty()}
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
 * <h5 class='topic'>Transform Methods</h5>
 * <p>
 * <ul class='javatree'>
 * 	<li class='jc'>{@link FluentRequestContentAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentRequestContentAssertion#asBytes() asBytes()}
 * 		<li class='jm'>{@link FluentRequestContentAssertion#as(Class) as(Class)}
 * 		<li class='jm'>{@link FluentRequestContentAssertion#as(Type,Type...) as(Type,Type...)}
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
 * <h5 class='topic'>Configuration Methods</h5>
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
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#ja.Overview">juneau-assertions</a>
 * </ul>
 *
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentRequestContentAssertion<R>")
public class FluentRequestContentAssertion<R> extends FluentObjectAssertion<RequestContent,R> {

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
	public FluentRequestContentAssertion(RequestContent value, R returns) {
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
	public FluentRequestContentAssertion(Assertion creator, RequestContent value, R returns) {
		super(creator, value, returns);
		setThrowable(BadRequest.class);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Transform methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Provides the ability to perform fluent-style assertions on the bytes of the request content.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Validates the request content equals the text "foo".</jc>
	 * 	<jv>request</jv>
	 * 		.assertContent().asBytes().asHex().is(<js>"666F6F"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		If no charset was found on the <code>Content-Type</code> request header, <js>"UTF-8"</js> is assumed.
	 *  <li class='note'>
	 *		When using this method, the content is automatically cached by calling the {@link RequestContent#cache()}.
	 * 	<li class='note'>
	 * 		The input stream is automatically closed after this call.
	 * </ul>
	 *
	 * @return A new fluent assertion object.
	 */
	public FluentByteArrayAssertion<R> asBytes() {
		return new FluentByteArrayAssertion<>(valueAsBytes(), returns());
	}

	/**
	 * Converts the content to a type using {@link RequestContent#as(Class)} and then returns the value as an object assertion.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Validates the request content bean is the expected value.</jc>
	 * 	<jv>request</jv>
	 * 		.assertContent()
	 * 		.as(MyBean.<jk>class</jk>)
	 * 			.asJson().is(<js>"{foo:'bar'}"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		If no charset was found on the <code>Content-Type</code> request header, <js>"UTF-8"</js> is assumed.
	 *  <li class='note'>
	 *		When using this method, the content is automatically cached by calling the {@link RequestContent#cache()}.
	 * 	<li class='note'>
	 * 		The input stream is automatically closed after this call.
	 * </ul>
	 *
	 * <p>
	 * See <a class="doclink" href="../../../../../index.html#jm.ComplexDataTypes">Complex Data Types</a> for information on defining complex generic types of {@link Map Maps} and {@link Collection Collections}.
	 *
	 * @param <T> The object type to create.
	 * @param type The object type to create.
	 * @return A new fluent assertion object.
	 */
	public <T> FluentObjectAssertion<T,R> as(Class<T> type) {
		return new FluentObjectAssertion<>(valueAsType(type), returns());
	}

	/**
	 * Converts the content to a type using {@link RequestContent#as(Type,Type...)} and then returns the value as an object assertion.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Validates the request content bean is the expected value.</jc>
	 * 	<jv>request</jv>
	 * 		.assertContent()
	 * 		.as(Map.<jk>class</jk>,String.<jk>class</jk>,Integer.<jk>class</jk>)
	 * 			.asJson().is(<js>"{foo:123}"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		If no charset was found on the <code>Content-Type</code> request header, <js>"UTF-8"</js> is assumed.
	 *  <li class='note'>
	 *		When using this method, the content is automatically cached by calling the {@link RequestContent#cache()}.
	 * 	<li class='note'>
	 * 		The input stream is automatically closed after this call.
	 * </ul>
	 *
	 * <p>
	 * See <a class="doclink" href="../../../../../index.html#jm.ComplexDataTypes">Complex Data Types</a> for information on defining complex generic types of {@link Map Maps} and {@link Collection Collections}.
	 *
	 * @param <T> The type to create.
	 * @param type The object type to create.
	 * @param args Optional type arguments.
	 * @return A new fluent assertion object.
	 */
	public <T> FluentObjectAssertion<T,R> as(Type type, Type...args) {
		return new FluentObjectAssertion<>(valueAsType(type, args), returns());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Asserts that the content contains the specified value.
	 *
	 * @param values The value to check against.
	 * @return This object.
	 * @throws AssertionError If assertion failed.
	 */
	public R is(String values) throws AssertionError {
		return asString().is(values);
	}

	/**
	 * Asserts that the text contains all of the specified substrings.
	 *
	 * @param values The values to check against.
	 * @return This object.
	 * @throws AssertionError If assertion failed.
	 */
	public R isContains(String...values) throws AssertionError {
		return asString().isContains(values);
	}

	/**
	 * Asserts that the content doesn't contain any of the specified substrings.
	 *
	 * @param values The values to check against.
	 * @return This object.
	 * @throws AssertionError If assertion failed.
	 */
	public R isNotContains(String...values) throws AssertionError {
		return asString().isNotContains(values);
	}

	/**
	 * Asserts that the content is empty.
	 *
	 * @return This object.
	 * @throws AssertionError If assertion failed.
	 */
	public R isEmpty() {
		return asString().isEmpty();
	}

	/**
	 * Asserts that the content is not empty.
	 *
	 * @return This object.
	 * @throws AssertionError If assertion failed.
	 */
	public R isNotEmpty() {
		return asString().isNotEmpty();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//-----------------------------------------------------------------------------------------------------------------

	@Override
	protected String valueAsString() throws AssertionError {
		try {
			return value().cache().asString();
		} catch (IOException e) {
			throw error(e, "Exception occurred during call.");
		}
	}

	private byte[] valueAsBytes() throws AssertionError {
		try {
			return value().cache().asBytes();
		} catch (IOException e) {
			throw error(e, "Exception occurred during call.");
		}
	}

	private <T> T valueAsType(Class<T> c) throws AssertionError {
		try {
			return value().cache().as(c);
		} catch (IOException e) {
			throw error(e, "Exception occurred during call.");
		}
	}

	private <T> T valueAsType(Type c, Type...args) throws AssertionError {
		try {
			return value().cache().as(c, args);
		} catch (IOException e) {
			throw error(e, "Exception occurred during call.");
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fluent setters
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentRequestContentAssertion<R> setMsg(String msg, Object...args) {
		super.setMsg(msg, args);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentRequestContentAssertion<R> setOut(PrintStream value) {
		super.setOut(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentRequestContentAssertion<R> setSilent() {
		super.setSilent();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentRequestContentAssertion<R> setStdOut() {
		super.setStdOut();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentRequestContentAssertion<R> setThrowable(Class<? extends java.lang.RuntimeException> value) {
		super.setThrowable(value);
		return this;
	}

	// </FluentSetters>
}
