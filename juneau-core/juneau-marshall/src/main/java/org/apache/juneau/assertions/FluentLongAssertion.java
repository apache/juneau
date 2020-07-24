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
 * Used for fluent assertion calls against longs.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Validates the response length isn't too long.</jc>
 * 	<jv>client</jv>
 * 		.get(<jsf>URL</jsf>)
 * 		.run()
 * 		.assertLongHeader(<js>"Length"</js>).isLessThan(100000);
 * </p>
 *
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentLongAssertion<R>")
public class FluentLongAssertion<R> extends FluentComparableAssertion<R> {

	private final Long value;

	/**
	 * Constructor.
	 *
	 * @param value The value being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentLongAssertion(Long value, R returns) {
		this(null, value, returns);
	}

	/**
	 * Constructor.
	 *
	 * @param creator The assertion that created this assertion.
	 * @param value The value being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentLongAssertion(Assertion creator, Long value, R returns) {
		super(creator, value, returns);
		this.value = value;
	}

	/**
	 * Converts this long into an integer and then returns it as an integer assertion.
	 *
	 * @return A new assertion.
	 */
	public FluentIntegerAssertion<R> integer() {
		return new FluentIntegerAssertion<>(this, value == null ? null : value.intValue(), returns());
	}

	@Override
	protected int compareTo(Object value) {
		return this.value.compareTo(((Number)value).longValue());
	}

	@Override
	protected Object equivalent(Object o) {
		if (o instanceof Number)
			return ((Number)o).longValue();
		return o;
	}

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public FluentLongAssertion<R> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentLongAssertion<R> stderr() {
		super.stderr();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentLongAssertion<R> stdout() {
		super.stdout();
		return this;
	}

	// </FluentSetters>
}
