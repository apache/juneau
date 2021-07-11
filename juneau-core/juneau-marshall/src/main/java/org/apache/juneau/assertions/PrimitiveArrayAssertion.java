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

import static org.apache.juneau.assertions.Assertions.*;

import java.io.*;

import org.apache.juneau.internal.*;

/**
 * Used for assertion calls against arrays.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jk>int</jk>[] <jv>array</jv> = {1};
 * 	<jsm>assertArray</jsm>(<jv>array</jv>).exists().isSize(1);
 * </p>
 *
 * @param <T> The array type.
 */
@FluentSetters(returns="PrimitiveArrayAssertion<T>")
public class PrimitiveArrayAssertion<T> extends FluentPrimitiveArrayAssertion<T,PrimitiveArrayAssertion<T>> {

	/**
	 * Creator.
	 *
	 * @param value The object being wrapped.
	 * @return A new {@link PrimitiveArrayAssertion} object.
	 */
	public static <T> PrimitiveArrayAssertion<T> create(T value) {
		return new PrimitiveArrayAssertion<>(value);
	}

	/**
	 * Creator.
	 *
	 * @param value The object being wrapped.
	 */
	public PrimitiveArrayAssertion(T value) {
		super(value, null);
		Class<?> c = value == null ? null : value.getClass();
		assertArg(c == null || c.isArray() && c.getComponentType().isPrimitive(), "Value wasn't a primitive array.");
	}

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public PrimitiveArrayAssertion<T> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public PrimitiveArrayAssertion<T> out(PrintStream value) {
		super.out(value);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public PrimitiveArrayAssertion<T> silent() {
		super.silent();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public PrimitiveArrayAssertion<T> stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public PrimitiveArrayAssertion<T> throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	// </FluentSetters>
}
