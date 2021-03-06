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
 * Used for assertion calls against byte arrays.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Validates the byte array contains the string "foo".</jc>
 * 	<jsm>assertBytes</jsm>(<jv>myByteArray</jv>).asHex().is(<js>"666F6F"</js>);
 * </p>
 */
@FluentSetters(returns="ByteArrayAssertion")
public class ByteArrayAssertion extends FluentByteArrayAssertion<ByteArrayAssertion> {

	//-----------------------------------------------------------------------------------------------------------------
	// Constructors
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Creator.
	 *
	 * @param value The contents being wrapped.
	 * @return A new {@link ByteArrayAssertion} object.
	 */
	public static ByteArrayAssertion create(byte[] value) {
		return new ByteArrayAssertion(value);
	}

	/**
	 * Creator.
	 *
	 * @param value The contents being wrapped.
	 */
	public ByteArrayAssertion(byte[] value) {
		super(value, null);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fluent setters
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public ByteArrayAssertion msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public ByteArrayAssertion out(PrintStream value) {
		super.out(value);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public ByteArrayAssertion silent() {
		super.silent();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public ByteArrayAssertion stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public ByteArrayAssertion throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	// </FluentSetters>
}
