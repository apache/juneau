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

import org.apache.juneau.internal.*;

/**
 * Used for assertion calls against throwable objects.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Validates the throwable message or one of the parent messages contain 'Foobar'.</jc>
 * 	<jsm>assertThrowable</jsm>(<jv>throwable</jv>).contains(<js>"Foobar"</js>);
 * </p>
 *
 * @param <T> The throwable type.
 */
@FluentSetters(returns="ThrowableAssertion<T>")
public class ThrowableAssertion<T extends Throwable> extends FluentThrowableAssertion<T,ThrowableAssertion<T>> {

	//-----------------------------------------------------------------------------------------------------------------
	// Constructors
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Creator.
	 *
	 * @param value The throwable being wrapped.
	 * @return A new {@link ThrowableAssertion} object.
	 */
	public static <V extends Throwable> ThrowableAssertion<V> create(V value) {
		return new ThrowableAssertion<>(value);
	}

	/**
	 * Creator.
	 *
	 * @param value The throwable being wrapped.
	 */
	public ThrowableAssertion(T value) {
		super(value, null);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fluent setters
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public ThrowableAssertion<T> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public ThrowableAssertion<T> out(PrintStream value) {
		super.out(value);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public ThrowableAssertion<T> silent() {
		super.silent();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public ThrowableAssertion<T> stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public ThrowableAssertion<T> throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	// </FluentSetters>
}
