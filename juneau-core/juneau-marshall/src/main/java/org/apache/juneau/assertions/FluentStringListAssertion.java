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

import static java.util.stream.Collectors.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.internal.*;

/**
 * Used for fluent assertion calls against lists of strings.
 *
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentStringListAssertion<R>")
public class FluentStringListAssertion<R> extends FluentListAssertion<String,R> {

	//-----------------------------------------------------------------------------------------------------------------
	// Constructors
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param contents The string list being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentStringListAssertion(List<String> contents, R returns) {
		this(null, contents, returns);
	}

	/**
	 * Constructor.
	 *
	 * @param creator The assertion that created this assertion.
	 * @param contents The string list being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentStringListAssertion(Assertion creator, List<String> contents, R returns) {
		super(creator, contents, returns);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Transform methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Concatenates this list of strings into a {@link FluentStringAssertion}.
	 *
	 * @return A new assertion.
	 */
	public FluentStringAssertion<R> join() {
		return new FluentStringAssertion<>(this, valueIsNull() ? null : value().stream().collect(joining()), returns());
	}

	/**
	 * Concatenates this list of strings into a {@link FluentStringAssertion}.
	 *
	 * @param delimiter The delimiter to be used between each element.
	 * @return A new assertion.
	 */
	public FluentStringAssertion<R> join(String delimiter) {
		return new FluentStringAssertion<>(this, valueIsNull() ? null : value().stream().collect(joining(delimiter)), returns());
	}

	/**
	 * Concatenates this list of strings into a {@link FluentStringAssertion}.
	 *
	 * @param delimiter The delimiter to be used between each element.
	 * @param prefix The sequence of characters to be used at the beginning of the joined result.
	 * @param suffix The sequence of characters to be used at the end of the joined result.
	 * @return A new assertion.
	 */
	public FluentStringAssertion<R> join(String delimiter, String prefix, String suffix) {
		return new FluentStringAssertion<>(this, valueIsNull() ? null : value().stream().collect(joining(delimiter, prefix, suffix)), returns());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fluent setters
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public FluentStringListAssertion<R> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentStringListAssertion<R> out(PrintStream value) {
		super.out(value);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentStringListAssertion<R> silent() {
		super.silent();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentStringListAssertion<R> stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentStringListAssertion<R> throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	// </FluentSetters>
}
