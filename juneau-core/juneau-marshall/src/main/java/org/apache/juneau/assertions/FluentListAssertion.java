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

import java.util.*;

import org.apache.juneau.internal.*;

/**
 * Used for fluent assertion calls against lists.
 *
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentListAssertion<R>")
@SuppressWarnings("rawtypes")
public class FluentListAssertion<R> extends FluentCollectionAssertion<R> {

	private List value;

	/**
	 * Constructor.
	 *
	 * @param contents The byte array being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentListAssertion(List contents, R returns) {
		this(null, contents, returns);
	}

	/**
	 * Constructor.
	 *
	 * @param creator The assertion that created this assertion.
	 * @param contents The byte array being tested.
	 * @param returns The object to return after the test.
	 */
	public FluentListAssertion(Assertion creator, List contents, R returns) {
		super(creator, contents, returns);
		this.value = contents;
	}

	/**
	 * Returns an object assertion on the item specified at the specified index.
	 *
	 * <p>
	 * If the list is <jk>null</jk> or the index is out-of-bounds, the returned assertion is a null assertion
	 * (meaning {@link FluentObjectAssertion#exists()} returns <jk>false</jk>).
	 *
	 * @param index The index of the item to retrieve from the list.
	 * @return A new assertion.
	 */
	public FluentObjectAssertion<R> item(int index) {
		return new FluentObjectAssertion<>(this, getItem(index), returns());
	}

	private Object getItem(int index) {
		if (value != null && value.size() > index)
			return value.get(index);
		return null;
	}

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public FluentListAssertion<R> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentListAssertion<R> stderr() {
		super.stderr();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentListAssertion<R> stdout() {
		super.stdout();
		return this;
	}

	// </FluentSetters>
}
