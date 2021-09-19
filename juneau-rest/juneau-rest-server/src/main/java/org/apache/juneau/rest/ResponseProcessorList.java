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

import static org.apache.juneau.assertions.Assertions.*;

import java.util.*;

import static java.util.Arrays.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.cp.*;

/**
 * A list of {@link ResponseProcessor} objects.
 */
public class ResponseProcessorList {

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

		List<Object> entries;
		ResponseProcessorList impl;
		BeanStore beanStore = BeanStore.INSTANCE;

		/**
		 * Constructor.
		 */
		protected Builder() {
			this.entries = AList.create();
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy.
		 */
		protected Builder(Builder copyFrom) {
			this.entries = AList.create().append(copyFrom.entries);
		}

		/**
		 * Creates a new {@link ResponseProcessorList} object using a snapshot of the settings defined in this builder.
		 *
		 * @return A new {@link ResponseProcessorList} object.
		 */
		public ResponseProcessorList build() {
			if (impl != null)
				return impl;
			return new ResponseProcessorList(this);
		}

		/**
		 * Appends the specified rest response processor classes to the list.
		 *
		 * @param values The values to add.
		 * @return This object.
		 * @throws IllegalArgumentException if any class does not extend from {@link ResponseProcessor}.
		 */
		public Builder add(Class<?>...values) {
			entries.addAll(asList(assertClassArrayArgIsType("values", ResponseProcessor.class, values)));
			return this;
		}

		/**
		 * Appends the specified rest response processor objects to the list.
		 *
		 * @param values The values to add.
		 * @return This object.
		 */
		public Builder add(ResponseProcessor...values) {
			entries.addAll(asList(values));
			return this;
		}

		/**
		 * Specifies the bean store to use for instantiating rest response processor classes.
		 *
		 * @param value The bean store to use for instantiating rest response processor classes.
		 * @return This object.
		 */
		public Builder beanStore(BeanStore value) {
			beanStore = value;
			return this;
		}

		/**
		 * Specifies a pre-instantiated bean to return from {@link #build()}.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public Builder impl(ResponseProcessorList value) {
			this.impl = value;
			return this;
		}

		/**
		 * Creates a copy of this builder.
		 *
		 * @return A copy of this builder.
		 */
		public Builder copy() {
			return new Builder(this);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final ResponseProcessor[] entries;

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the contents for this list.
	 */
	protected ResponseProcessorList(Builder builder) {
		entries =
			builder
				.entries
				.stream()
				.map(x -> instantiate(x, builder.beanStore))
				.toArray(ResponseProcessor[]::new);
	}

	private static ResponseProcessor instantiate(Object o, BeanStore bs) {
		if (o instanceof ResponseProcessor)
			return (ResponseProcessor)o;
		try {
			return bs.creator(ResponseProcessor.class).type((Class<?>)o).run();
		} catch (ExecutableException e) {
			throw new ConfigException(e, "Could not instantiate class {0}", o);
		}
	}

	/**
	 * Returns the entries in this list.
	 *
	 * @return The entries in this list.
	 */
	public ResponseProcessor[] toArray() {
		return entries;
	}
}
