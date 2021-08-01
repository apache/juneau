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
package org.apache.juneau.rest.client.assertion;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.assertions.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.serializer.*;

/**
 * Used for fluent assertion calls against {@link ResponseBody} objects.
 * {@review}
 *
 *
 * <h5 class='topic'>Test Methods</h5>
 * 	<ul>
 * 		<li class='jm'>{@link FluentResponseBodyAssertion#is(String)}
 * 		<li class='jm'>{@link FluentResponseBodyAssertion#contains(String...)}
 * 		<li class='jm'>{@link FluentResponseBodyAssertion#doesNotContain(String...)}
 * 		<li class='jm'>{@link FluentResponseBodyAssertion#isEmpty()}
 * 		<li class='jm'>{@link FluentResponseBodyAssertion#isNotEmpty()}
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
 * 		<li class='jm'>{@link FluentResponseBodyAssertion#asBytes()}
 * 		<li class='jm'>{@link FluentResponseBodyAssertion#asType(Class)}
 * 		<li class='jm'>{@link FluentResponseBodyAssertion#asType(Type,Type...)}
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
 * 	<li class='link'>{@doc FluentAssertions}
 * </ul>
 *
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentResponseBodyAssertion<R>")
public class FluentResponseBodyAssertion<R> extends FluentObjectAssertion<ResponseBody,R> {

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
	public FluentResponseBodyAssertion(ResponseBody value, R returns) {
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
	public FluentResponseBodyAssertion(Assertion creator, ResponseBody value, R returns) {
		super(creator, value, returns);
		throwable(BadRequest.class);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Transform methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Provides the ability to perform fluent-style assertions on this response body.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the response body equals the text "OK".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertBody().is(<js>"OK"</js>);
	 *
	 * 	<jc>// Validates the response body contains the text "OK".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertBody().contains(<js>"OK"</js>);
	 *
	 * 	<jc>// Validates the response body passes a predicate test.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertBody().passes(<jv>x</jv> -&gt; <jv>x</jv>.contains(<js>"OK"</js>));
	 *
	 * 	<jc>// Validates the response body matches a regular expression.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertBody().matches(<js>".*OK.*"</js>);
	 *
	 * 	<jc>// Validates the response body matches a regular expression using regex flags.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertBody().matches(<js>".*OK.*"</js>, <jsf>MULTILINE</jsf> &amp; <jsf>CASE_INSENSITIVE</jsf>);
	 *
	 * 	<jc>// Validates the response body matches a regular expression in the form of an existing Pattern.</jc>
	 * 	Pattern <jv>p</jv> = Pattern.<jsm>compile</jsm>(<js>".*OK.*"</js>);
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertBody().matches(<jv>p</jv>);
	 * </p>
	 *
	 * <p>
	 * The assertion test returns the original response object allowing you to chain multiple requests like so:
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the response body matches a regular expression.</jc>
	 * 	MyBean <jv>bean</jv> = <jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertBody().matches(<js>".*OK.*"</js>);
	 * 		.assertBody().doesNotMatch(<js>".*ERROR.*"</js>)
	 * 		.getBody().as(MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If no charset was found on the <code>Content-Type</code> response header, <js>"UTF-8"</js> is assumed.
	 *  <li>
	 *		When using this method, the body is automatically cached by calling the {@link ResponseBody#cache()}.
	 * 	<li>
	 * 		The input stream is automatically closed after this call.
	 * </ul>
	 *
	 * @return A new fluent assertion object.
	 */
	@Override
	public FluentStringAssertion<R> asString() {
		return new FluentStringAssertion<>(valueAsString(), returns());
	}

	/**
	 * Provides the ability to perform fluent-style assertions on the bytes of the response body.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the response body equals the text "foo".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertBodyBytes().hex().is(<js>"666F6F"</js>);
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If no charset was found on the <code>Content-Type</code> response header, <js>"UTF-8"</js> is assumed.
	 *  <li>
	 *		When using this method, the body is automatically cached by calling the {@link ResponseBody#cache()}.
	 * 	<li>
	 * 		The input stream is automatically closed after this call.
	 * </ul>
	 *
	 * @return A new fluent assertion object.
	 * @throws RestCallException If REST call failed.
	 */
	public FluentByteArrayAssertion<R> asBytes() throws RestCallException {
		return new FluentByteArrayAssertion<>(valueAsBytes(), returns());
	}

	/**
	 * Converts the body to a type using {@link ResponseBody#asType(Class)} and then returns the value as an object assertion.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the response body as a list of strings and validates the length.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<js>"/myBean"</js>)
	 * 		.run()
	 * 		.assertBody().asObject(List.<jk>class</jk>, String.<jk>class</jk>).passes(<jv>x</jv> -&gt; <jv>x</jv>.size() > 0);
	 * </p>
	 *
	 * @param type The object type to create.
	 * @return A new fluent assertion object.
	 */
	public <V> FluentAnyAssertion<V,R> asType(Class<V> type) {
		return new FluentAnyAssertion<>(valueAsType(type), returns());
	}

	/**
	 * Converts the body to a type using {@link ResponseBody#asType(Type,Type...)} and then returns the value as an object assertion.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the response body as a list of strings and validates the length.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<js>"/myBean"</js>)
	 * 		.run()
	 * 		.assertBody().asObject(List.<jk>class</jk>, String.<jk>class</jk>).passes(<jv>x</jv> -&gt; <jv>x</jv>.size() > 0);
	 * </p>
	 *
	 * <p>
	 * See {@doc Generics Generics} for information on defining complex generic types of {@link Map Maps} and {@link Collection Collections}.
	 *
	 * @param type The object type to create.
	 * @param args Optional type arguments.
	 * @return A new fluent assertion object.
	 */
	public FluentAnyAssertion<Object,R> asType(Type type, Type...args) {
		return new FluentAnyAssertion<>(valueAsType(type, args), returns());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Asserts that the body contains the specified value.
	 *
	 * @param value The value to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R is(String value) throws AssertionError {
		return asString().is(value);
	}

	/**
	 * Asserts that the text contains all of the specified substrings.
	 *
	 * @param values The values to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R contains(String...values) throws AssertionError {
		return asString().contains(values);
	}

	/**
	 * Asserts that the body doesn't contain any of the specified substrings.
	 *
	 * @param values The values to check against.
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R doesNotContain(String...values) throws AssertionError {
		return asString().doesNotContain(values);
	}

	/**
	 * Asserts that the body is empty.
	 *
	 * @return The response object (for method chaining).
	 * @throws AssertionError If assertion failed.
	 */
	public R isEmpty() {
		return asString().isEmpty();
	}

	/**
	 * Asserts that the body is not empty.
	 *
	 * @return The response object (for method chaining).
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
		} catch (RestCallException e) {
			throw error(e, "Exception occurred during call.");
		}
	}

	private byte[] valueAsBytes() throws AssertionError {
		try {
			return value().cache().asBytes();
		} catch (RestCallException e) {
			throw error(e, "Exception occurred during call.");
		}
	}

	private <T> T valueAsType(Type type, Type...args) throws AssertionError {
		try {
			return value().cache().asType(type, args);
		} catch (RestCallException e) {
			throw error(e, "Exception occurred during call.");
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fluent setters
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public FluentResponseBodyAssertion<R> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentResponseBodyAssertion<R> out(PrintStream value) {
		super.out(value);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentResponseBodyAssertion<R> silent() {
		super.silent();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentResponseBodyAssertion<R> stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentResponseBodyAssertion<R> throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	// </FluentSetters>
}
