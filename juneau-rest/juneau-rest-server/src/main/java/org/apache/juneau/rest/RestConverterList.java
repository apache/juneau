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
 * A list of {@link RestConverter} objects.
 */
public class RestConverterList {

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
		 * Creates a new {@link RestConverterList} object using a snapshot of the settings defined in this builder.
		 *
		 * @return A new {@link RestConverterList} object.
		 */
		public RestConverterList build() {
			return new RestConverterList(this);
		}

		/**
		 * Appends the specified rest matcher classes to the list.
		 *
		 * @param values The values to add.
		 * @return This object (for method chaining).
		 */
		@SuppressWarnings("unchecked")
		public Builder append(Class<? extends RestConverter>...values) {
			entries.append((Object[])values);
			return this;
		}

		/**
		 * Appends the specified rest matcher objects to the list.
		 *
		 * @param values The values to add.
		 * @return This object (for method chaining).
		 */
		public Builder append(RestConverter...values) {
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

	private final RestConverter[] entries;

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the contents for this list.
	 */
	protected RestConverterList(Builder builder) {
		entries =
			builder
				.entries
				.stream()
				.map(x -> instantiate(x, builder.beanStore))
				.toArray(RestConverter[]::new);
	}

	private static RestConverter instantiate(Object o, BeanStore bs) {
		if (o instanceof RestConverter)
			return (RestConverter)o;
		try {
			return BeanCreator.create(RestConverter.class).type((Class<?>)o).store(bs).run();
		} catch (ExecutableException e) {
			throw new ConfigException(e, "Could not instantiate class {0}", o);
		}
	}

	/**
	 * Returns the contents of this list as a {@link RestConverter} array.
	 *
	 * @return The contents of this list as a {@link RestConverter} array.
	 */
	public RestConverter[] asArray() {
		return entries;
	}
}
