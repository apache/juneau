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

import static org.apache.juneau.internal.StringUtils.*;

/**
 * Used for assertion calls against string objects.
 */
public class StringAssertion extends FluentStringAssertion<StringAssertion> {

	/**
	 * Creator.
	 *
	 * @param text The string being wrapped.
	 * @return A new {@link StringAssertion} object.
	 */
	public static StringAssertion assertString(Object text) {
		return new StringAssertion(text);
	}

	/**
	 * Creator.
	 *
	 * @param text The string being wrapped.
	 * @return A new {@link StringAssertion} object.
	 */
	public static StringAssertion create(Object text) {
		return new StringAssertion(text);
	}

	/**
	 * Creator.
	 *
	 * @param text The string being wrapped.
	 */
	protected StringAssertion(Object text) {
		super(stringify(text), null);
	}

	@Override
	protected StringAssertion returns() {
		return this;
	}

	// <FluentSetters>

	// </FluentSetters>
}
