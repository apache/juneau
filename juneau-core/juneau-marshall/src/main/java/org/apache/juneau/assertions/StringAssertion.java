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

import static org.apache.juneau.internal.StringUtils.*;

import org.apache.juneau.internal.*;

/**
 * Used for assertion calls against string objects.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Validates the response body of an HTTP call is the text "OK".</jc>
 * 	<jsm>assertString</jsm>(<jv>httpBody</jv>).is(<js>"OK"</js>);
 * </p>
 */
@FluentSetters(returns="StringAssertion")
public class StringAssertion extends FluentStringAssertion<StringAssertion> {

	/**
	 * Creator.
	 *
	 * @param value The string being wrapped.
	 * @return A new {@link StringAssertion} object.
	 */
	public static StringAssertion create(Object value) {
		return new StringAssertion(value);
	}

	/**
	 * Creator.
	 *
	 * @param text The string being wrapped.
	 */
	public StringAssertion(Object text) {
		super(stringify(text), null);
	}

	@Override
	protected StringAssertion returns() {
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public StringAssertion msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public StringAssertion stderr() {
		super.stderr();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public StringAssertion stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public StringAssertion throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	@Override /* GENERATED - FluentStringAssertion */
	public StringAssertion javaStrings() {
		super.javaStrings();
		return this;
	}

	// </FluentSetters>
}
