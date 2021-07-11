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
 * Used for assertion calls against booleans.
 */
@FluentSetters(returns="BooleanAssertion")
public class BooleanAssertion extends FluentBooleanAssertion<BooleanAssertion> {

	/**
	 * Creator.
	 *
	 * @param value The object being wrapped.
	 * @return A new {@link BooleanAssertion} object.
	 */
	public static BooleanAssertion create(Boolean value) {
		return new BooleanAssertion(value);
	}

	/**
	 * Creator.
	 *
	 * @param value The object being wrapped.
	 */
	public BooleanAssertion(Boolean value) {
		super(value, null);
	}

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public BooleanAssertion msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public BooleanAssertion out(PrintStream value) {
		super.out(value);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public BooleanAssertion silent() {
		super.silent();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public BooleanAssertion stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public BooleanAssertion throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	// </FluentSetters>
}
