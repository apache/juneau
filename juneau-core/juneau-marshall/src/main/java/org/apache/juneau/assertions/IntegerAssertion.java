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
 * Used for assertion calls against integers.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Validates the response status code is 200 or 404.</jc>
 * 	<jsm>assertInteger</jsm>(httpReponse).isAny(200,404);
 * </p>
 */
@FluentSetters(returns="IntegerAssertion")
public class IntegerAssertion extends FluentIntegerAssertion<IntegerAssertion> {

	/**
	 * Creator.
	 *
	 * @param integer The object being wrapped.
	 * @return A new {@link IntegerAssertion} object.
	 */
	public static IntegerAssertion create(Integer integer) {
		return new IntegerAssertion(integer);
	}

	/**
	 * Creator.
	 *
	 * @param integer The object being wrapped.
	 */
	public IntegerAssertion(Integer integer) {
		super(integer, null);
	}

	@Override
	protected IntegerAssertion returns() {
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public IntegerAssertion msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public IntegerAssertion stderr() {
		super.stderr();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public IntegerAssertion stdout() {
		super.stdout();
		return this;
	}

	// </FluentSetters>
}
