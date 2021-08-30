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

import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.cp.*;

/**
 * A list of {@link RestOpArg} classes.
 */
public class RestOpArgList {

	private final Class<? extends RestOpArg>[] entries;

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
	protected RestOpArgList(Builder builder) {
		entries =
			builder
				.entries
				.stream()
				.toArray(Class[]::new);
	}

	/**
	 * Builder for {@link RestOpArgList} objects.
	 */
	public static class Builder {

		AList<Class<? extends RestOpArg>> entries;
		BeanStore beanStore;

		/**
		 * Create an empty builder.
		 */
		protected Builder() {
			this.entries = AList.create();
		}

		/**
		 * Creates a new {@link RestOpArgList} object using a snapshot of the settings defined in this builder.
		 *
		 * @return A new {@link RestOpArgList} object.
		 */
		public RestOpArgList build() {
			return new RestOpArgList(this);
		}

		/**
		 * Prepends the specified rest op arg classes to the list.
		 *
		 * @param values The values to add.
		 * @return This object (for method chaining).
		 */
		@SuppressWarnings("unchecked")
		public Builder append(Class<? extends RestOpArg>...values) {
			entries.addAll(0, Arrays.asList(values));
			return this;
		}
	}

	/**
	 * Returns the contents of this list as a {@link Class} array.
	 *
	 * @return The contents of this list as a {@link Class} array.
	 */
	public Class<? extends RestOpArg>[] asArray() {
		return entries;
	}
}
