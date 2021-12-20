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

import static java.util.stream.Collectors.*;

import java.io.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;

/**
 * Used for fluent assertion calls against lists of strings.
 * {@review}
 *
 *
 * <h5 class='topic'>Test Methods</h5>
 * 	<ul>
 * 		<li class='jm'>{@link FluentListAssertion#has(Object...)}
 * 		<li class='jm'>{@link FluentListAssertion#each(Predicate...)}
 * 		<li class='jm'>{@link FluentCollectionAssertion#isEmpty()}
 * 		<li class='jm'>{@link FluentCollectionAssertion#isNotEmpty()}
 * 		<li class='jm'>{@link FluentCollectionAssertion#contains(Object)}
 * 		<li class='jm'>{@link FluentCollectionAssertion#doesNotContain(Object)}
 * 		<li class='jm'>{@link FluentCollectionAssertion#any(Predicate)}
 * 		<li class='jm'>{@link FluentCollectionAssertion#all(Predicate)}
 * 		<li class='jm'>{@link FluentCollectionAssertion#isSize(int size)}
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
 * 		<li class='jm'>{@link FluentStringListAssertion#join()}
 * 		<li class='jm'>{@link FluentStringListAssertion#join(String)}
 * 		<li class='jm'>{@link FluentStringListAssertion#join(String,String,String)}
 * 		<li class='jm'>{@link FluentStringListAssertion#trim()}
 * 		<li class='jm'>{@link FluentListAssertion#item(int)}
 * 		<li class='jm'>{@link FluentListAssertion#sorted()}
 * 		<li class='jm'>{@link FluentListAssertion#sorted(Comparator)}
 * 		<li class='jm'>{@link FluentCollectionAssertion#asStrings()}
 * 		<li class='jm'>{@link FluentCollectionAssertion#size()}
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
 * </ul>
 *
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentStringListAssertion<R>")
public class FluentStringListAssertion<R> extends FluentListAssertion<String,R> {

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
	public FluentStringListAssertion(List<String> value, R returns) {
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
	public FluentStringListAssertion(Assertion creator, List<String> value, R returns) {
		super(creator, value, returns);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Transform methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Concatenates this list of strings into a {@link FluentStringAssertion}.
	 *
	 * @return A new assertion.
	 */
	public FluentStringAssertion<R> join() {
		return new FluentStringAssertion<>(this, valueIsNull() ? null : value().stream().collect(joining()), returns());
	}

	/**
	 * Concatenates this list of strings into a {@link FluentStringAssertion}.
	 *
	 * @param delimiter The delimiter to be used between each element.
	 * @return A new assertion.
	 */
	public FluentStringAssertion<R> join(String delimiter) {
		return new FluentStringAssertion<>(this, valueIsNull() ? null : value().stream().collect(joining(delimiter)), returns());
	}

	/**
	 * Concatenates this list of strings into a {@link FluentStringAssertion}.
	 *
	 * @param delimiter The delimiter to be used between each element.
	 * @param prefix The sequence of characters to be used at the beginning of the joined result.
	 * @param suffix The sequence of characters to be used at the end of the joined result.
	 * @return A new assertion.
	 */
	public FluentStringAssertion<R> join(String delimiter, String prefix, String suffix) {
		return new FluentStringAssertion<>(this, valueIsNull() ? null : value().stream().collect(joining(delimiter, prefix, suffix)), returns());
	}

	/**
	 * Trims all strings in this list (ignoring null) and returns it as a new string-list assertion.
	 *
	 * @return A new assertion.
	 */
	public FluentStringListAssertion<R> trim() {
		return new FluentStringListAssertion<>(this, valueIsNull() ? null : value().stream().map(x -> StringUtils.trim(x)).collect(toList()), returns());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fluent setters
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentStringListAssertion<R> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentStringListAssertion<R> out(PrintStream value) {
		super.out(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentStringListAssertion<R> silent() {
		super.silent();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentStringListAssertion<R> stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentStringListAssertion<R> throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	// </FluentSetters>
}
