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
 * Used for assertion calls against arbitrary POJOs.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Validates the specified POJO is the specified type.</jc>
 * 	<jsm>assertObject</jsm>(<jv>myPojo</jv>).isType(MyBean.<jk>class</jk>);
 * </p>
 *
 * @param <T> The object type.
 */
@FluentSetters(returns="ObjectAssertion<T>")
public class ObjectAssertion<T> extends FluentObjectAssertion<T,ObjectAssertion<T>> {

	/**
	 * Creator.
	 *
	 * @param value The object being wrapped.
	 * @return A new {@link ObjectAssertion} object.
	 */
	public static <T> ObjectAssertion<T> create(T value) {
		return new ObjectAssertion<>(value);
	}

	/**
	 * Creator.
	 *
	 * @param value The object being wrapped.
	 */
	public ObjectAssertion(T value) {
		super(value, null);
	}

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public ObjectAssertion<T> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public ObjectAssertion<T> out(PrintStream value) {
		super.out(value);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public ObjectAssertion<T> silent() {
		super.silent();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public ObjectAssertion<T> stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public ObjectAssertion<T> throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	// </FluentSetters>
}
