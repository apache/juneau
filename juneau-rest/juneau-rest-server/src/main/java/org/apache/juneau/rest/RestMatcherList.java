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

import static java.util.stream.Collectors.*;
import static java.util.Collections.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.cp.*;

/**
 * A list of {@link RestMatcher} objects.
 */
public class RestMatcherList {

	private final List<RestMatcher> entries;

	/**
	 * Static creator.
	 *
	 * @return An empty list.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the contents for this list.
	 */
	protected RestMatcherList(Builder builder) {
		entries = unmodifiableList(
			builder
				.entries
				.stream()
				.map(x -> instantiate(x, builder.beanStore))
				.collect(toList())
		);
	}

	/**
	 * Builder for {@link RestMatcherList} objects.
	 */
	public static class Builder {

		AList<Object> entries;
		BeanStore beanStore;

		/**
		 * Create an empty builder.
		 */
		protected Builder() {
			this.entries = AList.create();
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
		public Builder append(Class<? extends RestMatcher>...values) {
			entries.append((Object[])values);
			return this;
		}

		/**
		 * Appends the specified rest matcher classes to the list.
		 *
		 * @param values The values to add.
		 * @return This object (for method chaining).
		 */
		public Builder append(RestMatcher...values) {
			entries.append((Object[])values);
			return this;
		}

		/**
		 * Specifies the bean store to use for instantiating rest matcher classes.
		 *
		 * @param value The bean store to use for instantiating rest matcher classes.
		 * @return This object (for method chaining).
		 */
		public Builder beanStore(BeanStore value) {
			beanStore = value;
			return this;
		}
	}

	private static RestMatcher instantiate(Object o, BeanStore bs) {
		if (o instanceof RestMatcher)
			return (RestMatcher)o;
		try {
			return (RestMatcher)bs.createBean((Class<?>)o);
		} catch (ExecutableException e) {
			throw new ConfigException(e, "Could not instantiate class {0}", o);
		}
	}

	/**
	 * Returns the entries in this list.
	 *
	 * @return The entries in this list.
	 */
	public List<RestMatcher> getEntries() {
		return entries;
	}
}
