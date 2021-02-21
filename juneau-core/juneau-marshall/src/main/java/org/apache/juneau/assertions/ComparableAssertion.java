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
 * Used for assertion calls against comparable objects.
 */
@FluentSetters(returns="ComparableAssertion")
@SuppressWarnings("rawtypes")
public class ComparableAssertion extends FluentComparableAssertion<Comparable,ComparableAssertion> {

	/**
	 * Creator.
	 *
	 * @param value The object being wrapped.
	 * @return A new {@link ComparableAssertion} object.
	 */
	public static ComparableAssertion create(Comparable value) {
		return new ComparableAssertion(value);
	}

	/**
	 * Creator.
	 *
	 * @param value The object being wrapped.
	 */
	public ComparableAssertion(Comparable value) {
		super(value, null);
	}

	@Override
	protected ComparableAssertion returns() {
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public ComparableAssertion msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public ComparableAssertion stderr() {
		super.stderr();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public ComparableAssertion stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public ComparableAssertion throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	// </FluentSetters>
}
