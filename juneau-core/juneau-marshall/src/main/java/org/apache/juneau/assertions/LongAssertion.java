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
 * Used for assertion calls against longs.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Validates the response length isn't too long.</jc>
 * 	<jsm>assertLong</jsm>(responseLength).isLessThan(100000);
 * </p>
 */
@FluentSetters(returns="LongAssertion")
public class LongAssertion extends FluentLongAssertion<LongAssertion> {

	/**
	 * Creator.
	 *
	 * @param l The object being wrapped.
	 * @return A new {@link LongAssertion} object.
	 */
	public static LongAssertion create(Long l) {
		return new LongAssertion(l);
	}

	/**
	 * Creator.
	 *
	 * @param l The object being wrapped.
	 */
	public LongAssertion(Long l) {
		super(l, null);
	}

	@Override
	protected LongAssertion returns() {
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public LongAssertion msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public LongAssertion stderr() {
		super.stderr();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public LongAssertion stdout() {
		super.stdout();
		return this;
	}

	// </FluentSetters>
}
