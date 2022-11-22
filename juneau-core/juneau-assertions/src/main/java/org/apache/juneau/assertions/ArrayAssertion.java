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

import java.io.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;

/**
 * Used for assertion calls against arrays.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>import static</jk> org.apache.juneau.assertions.Assertions.*;
 *
 * 	String[] <jv>array</jv> = {<js>"foo"</js>};
 * 	<jsm>assertArray</jsm>(<jv>array</jv>).exists().isSize(1);
 * </p>
 *
 * <h5 class='topic'>Test Methods</h5>
 * <ul class='javatree'>
 * 	<li class='jc'>{@link FluentArrayAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentArrayAssertion#isHas(Object[]) isHas(Object[])}
 * 		<li class='jm'>{@link FluentArrayAssertion#is(Predicate) is(Predicate)}
 * 		<li class='jm'>{@link FluentArrayAssertion#isAny(Predicate) isAny(Predicate)}
 * 		<li class='jm'>{@link FluentArrayAssertion#isAll(Predicate) isAll(Predicate)}
 * 		<li class='jm'>{@link FluentArrayAssertion#isEmpty() isEmpty()}
 * 		<li class='jm'>{@link FluentArrayAssertion#isNotEmpty() isNotEmpty()}
 * 		<li class='jm'>{@link FluentArrayAssertion#isSize(int) isSize(int)}
 * 		<li class='jm'>{@link FluentArrayAssertion#isContains(Object) isContains(Object)}
 * 		<li class='jm'>{@link FluentArrayAssertion#isNotContains(Object) isNotContains(Object)}
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
 * 	<li class='jc'>{@link FluentArrayAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentArrayAssertion#asStrings() asStrings()}
 * 		<li class='jm'>{@link FluentArrayAssertion#asStrings(Function) asStrings(Function)}
 * 		<li class='jm'>{@link FluentArrayAssertion#asCdl() asCdl()}
 * 		<li class='jm'>{@link FluentArrayAssertion#asCdl(Function) asCdl(Function)}
 * 		<li class='jm'>{@link FluentArrayAssertion#asBeanList() asBeanList()}
 * 		<li class='jm'>{@link FluentArrayAssertion#asItem(int) asItem(int)}
 * 		<li class='jm'>{@link FluentArrayAssertion#asSorted() asSorted()}
 * 		<li class='jm'>{@link FluentArrayAssertion#asSorted(Comparator) asSorted(Comparator)}
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
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#ja.Overview">Overview &gt; juneau-assertions &gt; Overview</a>
 * </ul>
 *
 * @param <E> The element type.
 */
@FluentSetters(returns="ArrayAssertion<E>")
public class ArrayAssertion<E> extends FluentArrayAssertion<E,ArrayAssertion<E>> {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Static creator.
	 *
	 * @param <E> The element type.
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new assertion object.
	 */
	public static <E> ArrayAssertion<E> create(E[] value) {
		return new ArrayAssertion<>(value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public ArrayAssertion(E[] value) {
		super(value, null);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fluent setters
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public ArrayAssertion<E> setMsg(String msg, Object...args) {
		super.setMsg(msg, args);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public ArrayAssertion<E> setOut(PrintStream value) {
		super.setOut(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public ArrayAssertion<E> setSilent() {
		super.setSilent();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public ArrayAssertion<E> setStdOut() {
		super.setStdOut();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public ArrayAssertion<E> setThrowable(Class<? extends java.lang.RuntimeException> value) {
		super.setThrowable(value);
		return this;
	}

	// </FluentSetters>
}
