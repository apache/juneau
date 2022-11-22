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
import java.util.regex.*;

import org.apache.juneau.assertions.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.httppart.*;
import org.apache.juneau.serializer.*;

/**
 * Used for fluent assertion calls against {@link RequestQueryParam} objects.
 *
 * <h5 class='topic'>Test Methods</h5>
 * <p>
 * <ul class='javatree'>
 * 	<li class='jc'>{@link FluentStringAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentStringAssertion#is(String) is(String)}
 * 		<li class='jm'>{@link FluentStringAssertion#isNot(String) isNot(String)}
 * 		<li class='jm'>{@link FluentStringAssertion#isLines(String...) isLines(String...)}
 * 		<li class='jm'>{@link FluentStringAssertion#isSortedLines(String...) isSortedLines(String...)}
 * 		<li class='jm'>{@link FluentStringAssertion#isIc(String) isIc(String)}
 * 		<li class='jm'>{@link FluentStringAssertion#isNotIc(String) isNotIc(String)}
 * 		<li class='jm'>{@link FluentStringAssertion#isContains(String...) isContains(String...)}
 * 		<li class='jm'>{@link FluentStringAssertion#isNotContains(String...) isNotContains(String...)}
 * 		<li class='jm'>{@link FluentStringAssertion#isEmpty() isEmpty()}
 * 		<li class='jm'>{@link FluentStringAssertion#isNotEmpty() isNotEmpty()}
 * 		<li class='jm'>{@link FluentStringAssertion#isString(Object) isString(Object)}
 * 		<li class='jm'>{@link FluentStringAssertion#isMatches(String) isMatches(String)}
 * 		<li class='jm'>{@link FluentStringAssertion#isPattern(String) isPattern(String)}
 * 		<li class='jm'>{@link FluentStringAssertion#isPattern(String,int) isPattern(String,int)}
 * 		<li class='jm'>{@link FluentStringAssertion#isPattern(Pattern) isPattern(Pattern)}
 * 		<li class='jm'>{@link FluentStringAssertion#isStartsWith(String) isStartsWith(String)}
 * 		<li class='jm'>{@link FluentStringAssertion#isEndsWith(String) isEndsWith(String)}
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
 * 	<li class='jc'>{@link FluentRequestQueryParamAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentRequestQueryParamAssertion#asBoolean() asBoolean()}
 * 		<li class='jm'>{@link FluentRequestQueryParamAssertion#asInteger() asInteger()}
 * 		<li class='jm'>{@link FluentRequestQueryParamAssertion#asLong() asLong()}
 * 		<li class='jm'>{@link FluentRequestQueryParamAssertion#asZonedDateTime() asZonedDateTime()}
 * 		<li class='jm'>{@link FluentRequestQueryParamAssertion#as(Class) as(Class)}
 * 	</ul>
 * 	<li class='jc'>{@link FluentStringAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentStringAssertion#asReplaceAll(String,String) asReplaceAll(String,String)}
 * 		<li class='jm'>{@link FluentStringAssertion#asReplace(String,String) asReplace(String,String)}
 * 		<li class='jm'>{@link FluentStringAssertion#asUrlDecode() asUrlDecode()}
 * 		<li class='jm'>{@link FluentStringAssertion#asLc() asLc()}
 * 		<li class='jm'>{@link FluentStringAssertion#asUc() asUc()}
 * 		<li class='jm'>{@link FluentStringAssertion#asLines() asLines()}
 * 		<li class='jm'>{@link FluentStringAssertion#asSplit(String) asSplit(String)}
 * 		<li class='jm'>{@link FluentStringAssertion#asLength() asLength()}
 * 		<li class='jm'>{@link FluentStringAssertion#asOneLine() asOneLine()}
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
 * 	</ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#ja.Overview">juneau-assertions</a>
 * </ul>
 *
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentRequestQueryParamAssertion<R>")
public class FluentRequestQueryParamAssertion<R> extends FluentStringAssertion<R> {

	private final RequestQueryParam value;

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
	public FluentRequestQueryParamAssertion(RequestQueryParam value, R returns) {
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
	public FluentRequestQueryParamAssertion(Assertion creator, RequestQueryParam value, R returns) {
		super(creator, value.asString().orElse(null), returns);
		this.value = value;
		setThrowable(BadRequest.class);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Transform methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Converts this object assertion into a boolean assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not a boolean.
	 */
	public FluentBooleanAssertion<R> asBoolean() {
		return new FluentBooleanAssertion<>(this, value.asBoolean().orElse(null), returns());
	}

	/**
	 * Converts this object assertion into an integer assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not an integer.
	 */
	public FluentIntegerAssertion<R> asInteger() {
		return new FluentIntegerAssertion<>(this, value.asInteger().orElse(null), returns());
	}

	/**
	 * Converts this object assertion into a long assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not a long.
	 */
	public FluentLongAssertion<R> asLong() {
		return new FluentLongAssertion<>(this, value.asLong().orElse(null), returns());
	}

	/**
	 * Converts this object assertion into a zoned-datetime assertion.
	 *
	 * @return A new assertion.
	 * @throws AssertionError If object is not a zoned-datetime.
	 */
	public FluentZonedDateTimeAssertion<R> asZonedDateTime() {
		return new FluentZonedDateTimeAssertion<>(this, value.asDatePart().asZonedDateTime().orElse(null), returns());
	}

	/**
	 * Converts the parameter value to a type using {@link RequestQueryParam#as(Class)} and then returns the value as an any-object assertion.
	 *
	 * @param <T> The object type to create.
	 * @param type The object type to create.
	 * @return A new fluent assertion object.
	 */
	public <T> FluentAnyAssertion<T,R> as(Class<T> type) {
		return new FluentAnyAssertion<>(value.as(type).orElse(null), returns());
	}

	/**
	 * Converts the parameter value to a type using {@link RequestQueryParam#as(Type,Type...)} and then returns the value as an any-object assertion.
	 *
	 * <p>
	 * See <a class="doclink" href="../../../../../index.html#jm.ComplexDataTypes">Complex Data Types</a> for information on defining complex generic types of {@link Map Maps} and {@link Collection Collections}.
	 *
	 * @param type The object type to create.
	 * @param args Optional type arguments.
	 * @return A new fluent assertion object.
	 */
	public FluentAnyAssertion<Object,R> as(Type type, Type...args) {
		return new FluentAnyAssertion<>(value.as(type, args).orElse(null), returns());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fluent setters
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentRequestQueryParamAssertion<R> setMsg(String msg, Object...args) {
		super.setMsg(msg, args);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentRequestQueryParamAssertion<R> setOut(PrintStream value) {
		super.setOut(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentRequestQueryParamAssertion<R> setSilent() {
		super.setSilent();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentRequestQueryParamAssertion<R> setStdOut() {
		super.setStdOut();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentRequestQueryParamAssertion<R> setThrowable(Class<? extends java.lang.RuntimeException> value) {
		super.setThrowable(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.FluentStringAssertion */
	public FluentRequestQueryParamAssertion<R> asJavaStrings() {
		super.asJavaStrings();
		return this;
	}

	// </FluentSetters>
}
