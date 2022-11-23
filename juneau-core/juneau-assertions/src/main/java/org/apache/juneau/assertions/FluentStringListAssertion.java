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

import org.apache.juneau.common.internal.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;

/**
 * Used for fluent assertion calls against lists of strings.
 *
 * <h5 class='section'>Test Methods:</h5>
 * <p>
 * <ul class='javatree'>
 * 	<li class='jc'>{@link FluentListAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentListAssertion#isHas(Object...) isHas(Object...)}
 * 		<li class='jm'>{@link FluentListAssertion#isEach(Predicate...) isEach(Predicate...)}
 * 	</ul>
 * 	<li class='jc'>{@link FluentCollectionAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentCollectionAssertion#isEmpty() isEmpty()}
 * 		<li class='jm'>{@link FluentCollectionAssertion#isNotEmpty() isNotEmpty()}
 * 		<li class='jm'>{@link FluentCollectionAssertion#isContains(Object) isContains(Object)}
 * 		<li class='jm'>{@link FluentCollectionAssertion#isNotContains(Object) isNotContains(Object)}
 * 		<li class='jm'>{@link FluentCollectionAssertion#isAny(Predicate) isAny(Predicate)}
 * 		<li class='jm'>{@link FluentCollectionAssertion#isAll(Predicate) isAll(Predicate)}
 * 		<li class='jm'>{@link FluentCollectionAssertion#isSize(int size) isSize(int size)}
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
 * 	<li class='jc'>{@link FluentStringListAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentStringListAssertion#asJoin() asJoin()}
 * 		<li class='jm'>{@link FluentStringListAssertion#asJoin(String) asJoin(String)}
 * 		<li class='jm'>{@link FluentStringListAssertion#asJoin(String,String,String) asJoin(String,String,String)}
 * 		<li class='jm'>{@link FluentStringListAssertion#asTrimmed() asTrimmed()}
 * 	</ul>
 * 	<li class='jc'>{@link FluentListAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentListAssertion#asStrings() asStrings()}
 * 		<li class='jm'>{@link FluentListAssertion#asStrings(Function) asStrings(Function)}
 * 		<li class='jm'>{@link FluentListAssertion#asCdl() asCdl()}
 * 		<li class='jm'>{@link FluentListAssertion#asCdl(Function) asCdl(Function)}
 * 		<li class='jm'>{@link FluentListAssertion#asItem(int) asItem(int)}
 * 		<li class='jm'>{@link FluentListAssertion#asSorted() asSorted()}
 * 		<li class='jm'>{@link FluentListAssertion#asSorted(Comparator) asSorted(Comparator)}
 * 	</ul>
 * 	<li class='jc'>{@link FluentCollectionAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentCollectionAssertion#asStrings() asStrings()}
 * 		<li class='jm'>{@link FluentCollectionAssertion#asSize() asSize()}
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
@FluentSetters(returns="FluentStringListAssertion<R>")
public class FluentStringListAssertion<R> extends FluentListAssertion<String,R> {

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
	public FluentStringAssertion<R> asJoin() {
		return new FluentStringAssertion<>(this, valueIsNull() ? null : value().stream().collect(joining()), returns());
	}

	/**
	 * Concatenates this list of strings into a {@link FluentStringAssertion}.
	 *
	 * @param delimiter The delimiter to be used between each element.
	 * @return A new assertion.
	 */
	public FluentStringAssertion<R> asJoin(String delimiter) {
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
	public FluentStringAssertion<R> asJoin(String delimiter, String prefix, String suffix) {
		return new FluentStringAssertion<>(this, valueIsNull() ? null : value().stream().collect(joining(delimiter, prefix, suffix)), returns());
	}

	/**
	 * Trims all strings in this list (ignoring null) and returns it as a new string-list assertion.
	 *
	 * @return A new assertion.
	 */
	public FluentStringListAssertion<R> asTrimmed() {
		return new FluentStringListAssertion<>(this, valueIsNull() ? null : value().stream().map(x -> StringUtils.trim(x)).collect(toList()), returns());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fluent setters
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentStringListAssertion<R> setMsg(String msg, Object...args) {
		super.setMsg(msg, args);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentStringListAssertion<R> setOut(PrintStream value) {
		super.setOut(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentStringListAssertion<R> setSilent() {
		super.setSilent();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentStringListAssertion<R> setStdOut() {
		super.setStdOut();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentStringListAssertion<R> setThrowable(Class<? extends java.lang.RuntimeException> value) {
		super.setThrowable(value);
		return this;
	}

	// </FluentSetters>
}
