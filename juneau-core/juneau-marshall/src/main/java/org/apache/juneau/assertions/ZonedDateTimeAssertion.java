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
import java.time.*;

import org.apache.juneau.internal.*;

/**
 * Used for assertion calls against {@link ZonedDateTime} objects.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Validates the specified date is after the current date.</jc>
 * 	<jsm>assertZonedDateTime</jsm>(<jv>myZdt</jv>).isAfterNow();
 * </p>
 */
@FluentSetters(returns="ZonedDateTimeAssertion")
public class ZonedDateTimeAssertion extends FluentZonedDateTimeAssertion<ZonedDateTimeAssertion> {

	//-----------------------------------------------------------------------------------------------------------------
	// Constructors
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Creator.
	 *
	 * @param value The date being wrapped.
	 * @return A new {@link ZonedDateTimeAssertion} object.
	 */
	public static ZonedDateTimeAssertion create(ZonedDateTime value) {
		return new ZonedDateTimeAssertion(value);
	}

	/**
	 * Creator.
	 *
	 * @param value The date being wrapped.
	 */
	public ZonedDateTimeAssertion(ZonedDateTime value) {
		super(value, null);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fluent setters
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public ZonedDateTimeAssertion msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public ZonedDateTimeAssertion out(PrintStream value) {
		super.out(value);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public ZonedDateTimeAssertion silent() {
		super.silent();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public ZonedDateTimeAssertion stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public ZonedDateTimeAssertion throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	// </FluentSetters>
}
