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
 * Used for fluent assertion calls against {@link RequestBody} objects.
 *
 * <h5 class='topic'>Test Methods</h5>
 * <p>
 * <ul class='javatree'>
 * 	<li class='jc'>{@link FluentRequestBodyAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentRequestBodyAssertion#is(String) is(String)}
 * 		<li class='jm'>{@link FluentRequestBodyAssertion#isContains(String...) isContains(String...)}
 * 		<li class='jm'>{@link FluentRequestBodyAssertion#isNotContains(String...) isNotContains(String...)}
 * 		<li class='jm'>{@link FluentRequestBodyAssertion#isEmpty() isEmpty()}
 * 		<li class='jm'>{@link FluentRequestBodyAssertion#isNotEmpty() isNotEmpty()}
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
 * 	<li class='jc'>{@link FluentRequestBodyAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentRequestBodyAssertion#asBytes() asBytes()}
 * 		<li class='jm'>{@link FluentRequestBodyAssertion#as(Class) as(Class)}
 * 		<li class='jm'>{@link FluentRequestBodyAssertion#as(Type,Type...) as(Type,Type...)}
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
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jm.FluentAssertions}
 * 	<li class='extlink'>{@source}
 * </ul>
 *
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentRequestBodyAssertion<R>")
public class FluentRequestBodyAssertion<R> extends FluentObjectAssertion<RequestBody,R> {

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
	public FluentRequestBodyAssertion(RequestBody value, R returns) {
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
	public FluentRequestBodyAssertion(Assertion creator, RequestBody value, R returns) {
		super(creator, value, returns);
		setThrowable(BadRequest.class);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Transform methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Provides the ability to perform fluent-style assertions on the bytes of the request body.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Validates the request body equals the text "foo".</jc>
	 * 	<jv>request</jv>
	 * 		.assertBody().asBytes().asHex().is(<js>"666F6F"</js>);
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li class='note'>
	 * 		If no charset was found on the <code>Content-Type</code> request header, <js>"UTF-8"</js> is assumed.
	 *  <li class='note'>
	 *		When using this method, the body is automatically cached by calling the {@link RequestBody#cache()}.
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
	 * Converts the body to a type using {@link RequestBody#as(Class)} and then returns the value as an object assertion.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Validates the request body bean is the expected value.</jc>
	 * 	<jv>request</jv>
	 * 		.assertBody()
	 * 		.as(MyBean.<jk>class</jk>)
	 * 			.asJson().is(<js>"{foo:'bar'}"</js>);
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li class='note'>
	 * 		If no charset was found on the <code>Content-Type</code> request header, <js>"UTF-8"</js> is assumed.
	 *  <li class='note'>
	 *		When using this method, the body is automatically cached by calling the {@link RequestBody#cache()}.
	 * 	<li class='note'>
	 * 		The input stream is automatically closed after this call.
	 * </ul>
	 *
	 * <p>
	 * See {@doc jm.ComplexDataTypes Complex Data Types} for information on defining complex generic types of {@link Map Maps} and {@link Collection Collections}.
	 *
	 * @param <T> The object type to create.
	 * @param type The object type to create.
	 * @return A new fluent assertion object.
	 */
	public <T> FluentObjectAssertion<T,R> as(Class<T> type) {
		return new FluentObjectAssertion<>(valueAsType(type), returns());
	}

	/**
	 * Converts the body to a type using {@link RequestBody#as(Type,Type...)} and then returns the value as an object assertion.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Validates the request body bean is the expected value.</jc>
	 * 	<jv>request</jv>
	 * 		.assertBody()
	 * 		.as(Map.<jk>class</jk>,String.<jk>class</jk>,Integer.<jk>class</jk>)
	 * 			.asJson().is(<js>"{foo:123}"</js>);
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li class='note'>
	 * 		If no charset was found on the <code>Content-Type</code> request header, <js>"UTF-8"</js> is assumed.
	 *  <li class='note'>
	 *		When using this method, the body is automatically cached by calling the {@link RequestBody#cache()}.
	 * 	<li class='note'>
	 * 		The input stream is automatically closed after this call.
	 * </ul>
	 *
	 * <p>
	 * See {@doc jm.ComplexDataTypes Complex Data Types} for information on defining complex generic types of {@link Map Maps} and {@link Collection Collections}.
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
	 * Asserts that the body contains the specified value.
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
	 * Asserts that the body doesn't contain any of the specified substrings.
	 *
	 * @param values The values to check against.
	 * @return This object.
	 * @throws AssertionError If assertion failed.
	 */
	public R isNotContains(String...values) throws AssertionError {
		return asString().isNotContains(values);
	}

	/**
	 * Asserts that the body is empty.
	 *
	 * @return This object.
	 * @throws AssertionError If assertion failed.
	 */
	public R isEmpty() {
		return asString().isEmpty();
	}

	/**
	 * Asserts that the body is not empty.
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
	public FluentRequestBodyAssertion<R> setMsg(String msg, Object...args) {
		super.setMsg(msg, args);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentRequestBodyAssertion<R> setOut(PrintStream value) {
		super.setOut(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentRequestBodyAssertion<R> setSilent() {
		super.setSilent();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentRequestBodyAssertion<R> setStdOut() {
		super.setStdOut();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentRequestBodyAssertion<R> setThrowable(Class<? extends java.lang.RuntimeException> value) {
		super.setThrowable(value);
		return this;
	}

	// </FluentSetters>
}
