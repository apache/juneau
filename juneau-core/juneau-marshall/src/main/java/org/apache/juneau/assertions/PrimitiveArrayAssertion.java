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
import java.util.function.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;

/**
 * Used for assertion calls against arrays.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jk>int</jk>[] <jv>array</jv> = {1};
 * 	<jsm>assertArray</jsm>(<jv>array</jv>).exists().isSize(1);
 * </p>
 *
 * <ul>
 * 	<li>Test methods:
 * 	<ul>
 * 		<li class='jm'>{@link FluentPrimitiveArrayAssertion#has(E...)}
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
 * 	<li>Transform methods:
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
 * 	<li>Configuration methods:
 * 	<ul>
 * 		<li class='jm'>{@link Assertion#msg(String, Object...)}
 * 		<li class='jm'>{@link Assertion#out(PrintStream)}
 * 		<li class='jm'>{@link Assertion#silent()}
 * 		<li class='jm'>{@link Assertion#stdout()}
 * 		<li class='jm'>{@link Assertion#throwable(Class)}
 * 	</ul>
 * </ul>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc Assertions}
 * </ul>
 *
 * @param <E> The array element type.
 * @param <T> The array type.
 */
@FluentSetters(returns="PrimitiveArrayAssertion<E,T>")
public class PrimitiveArrayAssertion<E,T> extends FluentPrimitiveArrayAssertion<E,T,PrimitiveArrayAssertion<E,T>> {

	//-----------------------------------------------------------------------------------------------------------------
	// Constructors
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new assertion object.
	 */
	public static <E,T> PrimitiveArrayAssertion<E,T> create(T value) {
		return new PrimitiveArrayAssertion<>(value);
	}

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public PrimitiveArrayAssertion(T value) {
		super(value, null);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fluent setters
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public PrimitiveArrayAssertion<E,T> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public PrimitiveArrayAssertion<E,T> out(PrintStream value) {
		super.out(value);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public PrimitiveArrayAssertion<E,T> silent() {
		super.silent();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public PrimitiveArrayAssertion<E,T> stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public PrimitiveArrayAssertion<E,T> throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	// </FluentSetters>
}
