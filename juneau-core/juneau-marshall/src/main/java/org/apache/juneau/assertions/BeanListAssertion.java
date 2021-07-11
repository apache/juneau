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

import org.apache.juneau.internal.*;

/**
 * Used for assertion calls against Java beans.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Validates the specified list contains 3 beans with the specified values for the 'foo' property.</jc>
 * 	<jsm>assertBeanList</jsm>(<jv>myBeanList</jv>)
 * 		.property(<js>"foo"</js>)
 * 		.is(<js>"bar"</js>,<js>"baz"</js>,<js>"qux"</js>);
 * </p>
 *
 * @param <E> The bean type.
 */
@FluentSetters(returns="BeanListAssertion<E>")
public class BeanListAssertion<E> extends FluentBeanListAssertion<E,BeanListAssertion<E>> {

	/**
	 * Creator.
	 *
	 * @param value The object being wrapped.
	 * @return A new {@link BeanListAssertion} object.
	 */
	public static <E> BeanListAssertion<E> create(List<E> value) {
		return new BeanListAssertion<>(value);
	}

	/**
	 * Creator.
	 *
	 * @param value The object being wrapped.
	 */
	public BeanListAssertion(List<E> value) {
		super(value, null);
	}

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public BeanListAssertion<E> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public BeanListAssertion<E> out(PrintStream value) {
		super.out(value);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public BeanListAssertion<E> silent() {
		super.silent();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public BeanListAssertion<E> stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public BeanListAssertion<E> throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	// </FluentSetters>
}
