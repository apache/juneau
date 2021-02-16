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
 * Used for fluent assertion calls against integers.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Validates the response status code is 200 or 404.</jc>
 * 	<jv>client</jv>
 * 		.get(<jsf>URL</jsf>)
 * 		.run()
 * 		.assertStatus().isAny(200,404);
 * </p>
 *
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentIntegerAssertion<R>")
public class FluentIntegerAssertion<R> extends FluentComparableAssertion<Integer,R> {

	private final Integer value;

	/**
	 * Constructor.
	 *
	 * @param value The value being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentIntegerAssertion(Integer value, R returns) {
		this(null, value, returns);
	}

	/**
	 * Constructor.
	 *
	 * @param creator The assertion that created this assertion.
	 * @param value The value being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentIntegerAssertion(Assertion creator, Integer value, R returns) {
		super(creator, value, returns);
		this.value = value;
	}

	@Override
	protected int compareTo(Object value) {
		return this.value.compareTo(((Number)value).intValue());
	}

	@Override
	protected Object equivalent(Object o) {
		if (o instanceof Number)
			return ((Number)o).intValue();
		return o;
	}

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public FluentIntegerAssertion<R> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentIntegerAssertion<R> stderr() {
		super.stderr();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentIntegerAssertion<R> stdout() {
		super.stdout();
		return this;
	}

	// </FluentSetters>
}
