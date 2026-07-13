/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.rest.server.arg;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;

import java.util.*;

import org.apache.juneau.commons.inject.*;

/**
 * A list of {@link RestOpArg} classes.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestOpAnnotatedMethods">@RestOp-Annotated Method Basics</a>
 * </ul>
 */
public class RestOpArgList {

	/**
	 * Builder class.
	 */
	public static class Builder {

		private final BeanStore beanStore;
		List<Class<? extends RestOpArg>> entries;

		/**
		 * Constructor.
		 *
		 * @param beanStore The bean store to use for creating beans.
		 */
		protected Builder(BeanStore beanStore) {
			this.beanStore = beanStore;
			entries = list();
		}

		/**
		 * Returns the bean store used by this builder.
		 *
		 * @return The bean store used by this builder.
		 */
		public BeanStore beanStore() {
			return beanStore;
		}

		/**
		 * Prepends the specified rest op arg classes to the list.
		 *
		 * @param values The values to add.
		 * @return This object.
		 * @throws IllegalArgumentException if any class does not extend from {@link RestOpArg}.
		 */
		public Builder add(Class<?>...values) {
			prependAll(entries, assertClassArrayArgIsType("values", RestOpArg.class, values));
			return this;
		}

		/**
		 * Builds the list.
		 *
		 * @return A new {@link RestOpArgList}.
		 */
		public RestOpArgList build() {
			return new RestOpArgList(this);
		}
	}

	/**
	 * Static creator.
	 *
	 * @param beanStore The bean store to use for creating beans.
	 * @return A new builder for this object.
	 */
	public static Builder create(BeanStore beanStore) {
		return new Builder(beanStore);
	}

	private final Class<? extends RestOpArg>[] entries;

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the contents for this list.
	 */
	@SuppressWarnings({
		"unchecked" // Type erasure requires cast for builder list
	})
	protected RestOpArgList(Builder builder) {
		entries = builder.entries.stream().toArray(Class[]::new);
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