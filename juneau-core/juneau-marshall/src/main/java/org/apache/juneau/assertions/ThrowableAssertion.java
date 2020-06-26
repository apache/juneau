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

import org.apache.juneau.internal.*;

/**
 * Used for assertion calls against throwable objects.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Validates the throwable message or one of the parent messages contain 'Foobar'.</jc>
 * 	<jsm>assertThrowable</jsm>(throwable).contains(<js>"Foobar"</js>);
 * </p>
 */
@FluentSetters(returns="ThrowableAssertion")
public class ThrowableAssertion extends FluentThrowableAssertion<ThrowableAssertion> {

	/**
	 * Creator.
	 *
	 * @param throwable The throwable being wrapped.
	 * @return A new {@link ThrowableAssertion} object.
	 */
	public static ThrowableAssertion assertThrowable(Throwable throwable) {
		return new ThrowableAssertion(throwable);
	}

	/**
	 * Executes an arbitrary snippet of code and captures anything thrown from it.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Asserts that the specified method throws a RuntimeException containing "Foobar" in the message. </jc>
	 * 	<jsm>assertThrown</jsm>(() -> {foo.getBar();})
	 * 		.exists()
	 * 		.isType(RuntimeException.<jk>class</jk>)
	 * 		.contains(<js>"Foobar"</js>);
	 * </p>
	 *
	 * @param snippet The snippet of code to execute.
	 * @return A new assertion object.
	 */
	public static ThrowableAssertion assertThrown(Snippet snippet) {
		try {
			snippet.run();
		} catch (Throwable e) {
			return assertThrowable(e);
		}
		return assertThrowable(null);
	}

	/**
	 * Creator.
	 *
	 * @param throwable The throwable being wrapped.
	 * @return A new {@link ThrowableAssertion} object.
	 */
	public static ThrowableAssertion create(Throwable throwable) {
		return new ThrowableAssertion(throwable);
	}

	/**
	 * Creator.
	 *
	 * @param throwable The throwable being wrapped.
	 */
	protected ThrowableAssertion(Throwable throwable) {
		super(throwable, null);
	}

	@Override
	protected ThrowableAssertion returns() {
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public ThrowableAssertion msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public ThrowableAssertion stderr() {
		super.stderr();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public ThrowableAssertion stdout() {
		super.stdout();
		return this;
	}

	// </FluentSetters>
}
