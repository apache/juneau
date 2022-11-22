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
 * Used for fluent assertion calls against {@link ResponseContent} objects.
 *
 * <h5 class='topic'>Test Methods</h5>
 * <p>
 * <ul class='javatree'>
 * 	<li class='jc'>{@link FluentResponseBodyAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentResponseBodyAssertion#is(String) is(String)}
 * 		<li class='jm'>{@link FluentResponseBodyAssertion#isContains(String...) isContains(String...)}
 * 		<li class='jm'>{@link FluentResponseBodyAssertion#isNotContains(String...) isNotContains(String...)}
 * 		<li class='jm'>{@link FluentResponseBodyAssertion#isEmpty() isEmpty()}
 * 		<li class='jm'>{@link FluentResponseBodyAssertion#isNotEmpty() isNotEmpty()}
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
 * 	<li class='jc'>{@link FluentResponseBodyAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentResponseBodyAssertion#asBytes() asBytes()}
 * 		<li class='jm'>{@link FluentResponseBodyAssertion#as(Class) as(Class)}
 * 		<li class='jm'>{@link FluentResponseBodyAssertion#as(Type,Type...) as(Type,Type...)}
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
 * 	<li class='link'><a class="doclink" href="../../../../../../index.html#ja.Overview">juneau-assertions</a>
 * 	<li class='link'><a class="doclink" href="../../../../../../index.html#juneau-rest-client">juneau-rest-client</a>
 * </ul>
 *
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentResponseBodyAssertion<R>")
public class FluentResponseBodyAssertion<R> extends FluentObjectAssertion<ResponseContent,R> {

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
	public FluentResponseBodyAssertion(ResponseContent value, R returns) {
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
	public FluentResponseBodyAssertion(Assertion creator, ResponseContent value, R returns) {
		super(creator, value, returns);
		setThrowable(BadRequest.class);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Transform methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Provides the ability to perform fluent-style assertions on this response body.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Validates the response body equals the text "OK".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertContent().is(<js>"OK"</js>);
	 *
	 * 	<jc>// Validates the response body contains the text "OK".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertContent().isContains(<js>"OK"</js>);
	 *
	 * 	<jc>// Validates the response body passes a predicate test.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertContent().is(<jv>x</jv> -&gt; <jv>x</jv>.contains(<js>"OK"</js>));
	 *
	 * 	<jc>// Validates the response body matches a regular expression.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertContent().isPattern(<js>".*OK.*"</js>);
	 *
	 * 	<jc>// Validates the response body matches a regular expression using regex flags.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertContent().isPattern(<js>".*OK.*"</js>, <jsf>MULTILINE</jsf> &amp; <jsf>CASE_INSENSITIVE</jsf>);
	 *
	 * 	<jc>// Validates the response body matches a regular expression in the form of an existing Pattern.</jc>
	 * 	Pattern <jv>pattern</jv> = Pattern.<jsm>compile</jsm>(<js>".*OK.*"</js>);
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertContent().isPattern(<jv>pattern</jv>);
	 * </p>
	 *
	 * <p>
	 * The assertion test returns the original response object allowing you to chain multiple requests like so:
	 * <p class='bjava'>
	 * 	<jc>// Validates the response body matches a regular expression.</jc>
	 * 	MyBean <jv>bean</jv> = <jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertContent().isPattern(<js>".*OK.*"</js>);
	 * 		.assertContent().isNotPattern(<js>".*ERROR.*"</js>)
	 * 		.getContent().as(MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		If no charset was found on the <code>Content-Type</code> response header, <js>"UTF-8"</js> is assumed.
	 *  <li class='note'>
	 *		When using this method, the body is automatically cached by calling the {@link ResponseContent#cache()}.
	 * 	<li class='note'>
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
	 * <p class='bjava'>
	 * 	<jc>// Validates the response body equals the text "foo".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertContent().asBytes().asHex().is(<js>"666F6F"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		If no charset was found on the <code>Content-Type</code> response header, <js>"UTF-8"</js> is assumed.
	 *  <li class='note'>
	 *		When using this method, the body is automatically cached by calling the {@link ResponseContent#cache()}.
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
	 * Converts the body to a type using {@link ResponseContent#as(Class)} and then returns the value as an object assertion.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Validates the response body as a list of strings and validates the length.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<js>"/myBean"</js>)
	 * 		.run()
	 * 		.assertContent().as(List.<jk>class</jk>, String.<jk>class</jk>).is(<jv>x</jv> -&gt; <jv>x</jv>.size() &gt; 0);
	 * </p>
	 *
	 * @param <T> The object type to create.
	 * @param type The object type to create.
	 * @return A new fluent assertion object.
	 */
	public <T> FluentAnyAssertion<T,R> as(Class<T> type) {
		return new FluentAnyAssertion<>(valueAsType(type), returns());
	}

	/**
	 * Converts the body to a type using {@link ResponseContent#as(Type,Type...)} and then returns the value as an object assertion.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Validates the response body as a list of strings and validates the length.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<js>"/myBean"</js>)
	 * 		.run()
	 * 		.assertContent().as(List.<jk>class</jk>, String.<jk>class</jk>).is(<jv>x</jv> -&gt; <jv>x</jv>.size() &gt; 0);
	 * </p>
	 *
	 * <p>
	 * See <a class="doclink" href="../../../../../../index.html#jm.ComplexDataTypes">Complex Data Types</a> for information on defining complex generic types of {@link Map Maps} and {@link Collection Collections}.
	 *
	 * @param type The object type to create.
	 * @param args Optional type arguments.
	 * @return A new fluent assertion object.
	 */
	public FluentAnyAssertion<Object,R> as(Type type, Type...args) {
		return new FluentAnyAssertion<>(valueAsType(type, args), returns());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Asserts that the body contains the specified value.
	 *
	 * @param value The value to check against.
	 * @return This object.
	 * @throws AssertionError If assertion failed.
	 */
	public R is(String value) throws AssertionError {
		return asString().is(value);
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
			return value().cache().as(type, args);
		} catch (RestCallException e) {
			throw error(e, "Exception occurred during call.");
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fluent setters
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentResponseBodyAssertion<R> setMsg(String msg, Object...args) {
		super.setMsg(msg, args);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentResponseBodyAssertion<R> setOut(PrintStream value) {
		super.setOut(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentResponseBodyAssertion<R> setSilent() {
		super.setSilent();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentResponseBodyAssertion<R> setStdOut() {
		super.setStdOut();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentResponseBodyAssertion<R> setThrowable(Class<? extends java.lang.RuntimeException> value) {
		super.setThrowable(value);
		return this;
	}

	// </FluentSetters>
}
