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
 * Parent class of all fluent assertion calls.
 *
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentAssertion<R>")
public abstract class FluentAssertion<R> extends Assertion {

	private final R returns;

	/**
	 * Constructor.
	 *
	 * @param creator The assertion that created this assertion.
	 * @param returns The object to return after the test.
	 */
	protected FluentAssertion(Assertion creator, R returns) {
		super(creator);
		this.returns = returns;
	}

	/**
	 * Returns the object that the fluent methods on this class should return.
	 *
	 * @return The response object.
	 */
	protected R returns() {
		return returns;
	}

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public FluentAssertion<R> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentAssertion<R> stderr() {
		super.stderr();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentAssertion<R> stdout() {
		super.stdout();
		return this;
	}

	// </FluentSetters>
}
