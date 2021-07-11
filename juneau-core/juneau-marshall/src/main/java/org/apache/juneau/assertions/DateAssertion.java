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
 * Used for assertion calls against {@link Date} objects.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Validates the specified date is after the current date.</jc>
 * 	<jsm>assertDate</jsm>(<jv>myDate</jv>).isAfterNow();
 * </p>
 */
@FluentSetters(returns="DateAssertion")
public class DateAssertion extends FluentDateAssertion<DateAssertion> {

	/**
	 * Creator.
	 *
	 * @param value The date being wrapped.
	 * @return A new {@link DateAssertion} object.
	 */
	public static DateAssertion create(Date value) {
		return new DateAssertion(value);
	}

	/**
	 * Creator.
	 *
	 * @param value The date being wrapped.
	 */
	public DateAssertion(Date value) {
		super(value, null);
	}

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public DateAssertion msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public DateAssertion out(PrintStream value) {
		super.out(value);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public DateAssertion silent() {
		super.silent();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public DateAssertion stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public DateAssertion throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	// </FluentSetters>
}
