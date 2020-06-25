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
 * Used for assertion calls against throwable objects.
 */
public class ThrowableAssertion extends Assertion {

	private final Throwable t;

	/**
	 * Creator.
	 *
	 * @param t The throwable being wrapped.
	 * @return A new {@link ThrowableAssertion} object.
	 */
	public static ThrowableAssertion assertThrowable(Throwable t) {
		return new ThrowableAssertion(t);
	}

	/**
	 * Creator.
	 *
	 * @param t The throwable being wrapped.
	 * @return A new {@link ThrowableAssertion} object.
	 */
	public static ThrowableAssertion create(Throwable t) {
		return new ThrowableAssertion(t);
	}

	/**
	 * Creator.
	 *
	 * @param t The throwable being wrapped.
	 */
	protected ThrowableAssertion(Throwable t) {
		this.t = t;
	}

	/**
	 * Asserts that this throwable or any parent throwables contains all of the specified substrings.
	 *
	 * @param substrings The substrings to check for.
	 * @return This object (for method chaining).
	 */
	public ThrowableAssertion contains(String...substrings) {
		for (String substring : substrings) {
			Throwable e2 = t;
			boolean found = false;
			while (e2 != null && ! found) {
				found |= StringUtils.contains(e2.getMessage(), substring);
				e2 = e2.getCause();
			}
			if (! found) {
				throw error("Exception message did not contain expected substring.\n\tSubstring=[{0}]\n\tText=[{1}]", substring, t.getMessage());
			}
		}
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public ThrowableAssertion msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public ThrowableAssertion stderr() {
		super.stderr();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public ThrowableAssertion stdout() {
		super.stdout();
		return this;
	}

	// </FluentSetters>
}
