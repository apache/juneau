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
package org.apache.juneau.rest;

import org.apache.juneau.collections.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;

/**
 * Builder for {@link RestMatcherList} objects.
 */
@FluentSetters
public class RestMatcherListBuilder {

	AList<Object> matchers;
	BeanStore beanStore;

	/**
	 * Create an empty builder.
	 */
	protected RestMatcherListBuilder() {
		this.matchers = AList.create();
	}

	/**
	 * Creates a new {@link RestMatcherList} object using a snapshot of the settings defined in this builder.
	 *
	 * @return A new {@link RestMatcherList} object.
	 */
	public RestMatcherList build() {
		return new RestMatcherList(this);
	}

	/**
	 * Appends the specified rest matcher classes to the list.
	 *
	 * @param values The values to add.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings("unchecked")
	public RestMatcherListBuilder append(Class<? extends RestMatcher>...values) {
		matchers.append((Object[])values);
		return this;
	}

	/**
	 * Appends the specified rest matcher classes to the list.
	 *
	 * @param values The values to add.
	 * @return This object (for method chaining).
	 */
	public RestMatcherListBuilder append(RestMatcher...values) {
		matchers.append((Object[])values);
		return this;
	}

	/**
	 * Specifies the bean store to use for instantiating rest matcher classes.
	 *
	 * @param value The bean store to use for instantiating rest matcher classes.
	 * @return This object (for method chaining).
	 */
	public RestMatcherListBuilder beanStore(BeanStore value) {
		beanStore = value;
		return this;
	}
}
