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

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.cp.*;

/**
 * A list of {@link RestGuard} objects.
 */
public class RestGuardList {

	private RestGuard[] entries;

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
	protected RestGuardList(Builder builder) {
		entries =
			builder
				.entries
				.stream()
				.map(x -> instantiate(x, builder.beanStore))
				.toArray(RestGuard[]::new);
	}

	/**
	 * Builder for {@link RestGuardList} objects.
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
		 * Creates a new {@link RestGuardList} object using a snapshot of the settings defined in this builder.
		 *
		 * @return A new {@link RestGuardList} object.
		 */
		public RestGuardList build() {
			return new RestGuardList(this);
		}

		/**
		 * Appends the specified rest matcher classes to the list.
		 *
		 * @param values The values to add.
		 * @return This object (for method chaining).
		 */
		@SuppressWarnings("unchecked")
		public Builder append(Class<? extends RestGuard>...values) {
			entries.append((Object[])values);
			return this;
		}

		/**
		 * Appends the specified rest matcher objects to the list.
		 *
		 * @param values The values to add.
		 * @return This object (for method chaining).
		 */
		public Builder append(RestGuard...values) {
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

	private static RestGuard instantiate(Object o, BeanStore bs) {
		if (o instanceof RestGuard)
			return (RestGuard)o;
		try {
			return (RestGuard)bs.createBean((Class<?>)o);
		} catch (ExecutableException e) {
			throw new ConfigException(e, "Could not instantiate class {0}", o);
		}
	}

	/**
	 * Returns the entries in this list.
	 *
	 * @return The entries in this list.
	 */
	public RestGuard[] asArray() {
		return entries;
	}
}
