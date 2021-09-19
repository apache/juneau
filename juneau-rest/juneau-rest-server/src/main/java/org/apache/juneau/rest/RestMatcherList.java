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

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.cp.*;

/**
 * A list of {@link RestMatcher} objects.
 */
public class RestMatcherList {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Static creator.
	 *
	 * @return An empty list.
	 */
	public static Builder create() {
		return new Builder();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
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
		 * Appends the specified rest matcher objects to the list.
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

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final RestMatcher[] optionalEntries;
	private final RestMatcher[] requiredEntries;

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the contents for this list.
	 */
	protected RestMatcherList(Builder builder) {
		List<RestMatcher> l =
			builder
				.entries
				.stream()
				.map(x -> instantiate(x, builder.beanStore))
				.collect(toList());
		optionalEntries = l.stream().filter(x -> ! x.required()).toArray(RestMatcher[]::new);
		requiredEntries = l.stream().filter(x -> x.required()).toArray(RestMatcher[]::new);
	}

	private static RestMatcher instantiate(Object o, BeanStore bs) {
		if (o instanceof RestMatcher)
			return (RestMatcher)o;
		try {
			return BeanCreator.create(RestMatcher.class).type((Class<?>)o).store(bs).run();
		} catch (ExecutableException e) {
			throw new ConfigException(e, "Could not instantiate class {0}", o);
		}
	}

	/**
	 * Returns the entries in this list that are specified as optional.
	 *
	 * @return An unmodifiable list of entries in this list that are specified as optional.
	 */
	public RestMatcher[] getOptionalEntries() {
		return optionalEntries;
	}

	/**
	 * Returns the entries in this list that are specified as required.
	 *
	 * @return An unmodifiable list of entries in this list that are specified as required.
	 */
	public RestMatcher[] getRequiredEntries() {
		return requiredEntries;
	}
}
