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
package org.apache.juneau.rest.matcher;

import static org.apache.juneau.commons.utils.CollectionUtils.*;

import java.util.*;

import org.apache.juneau.commons.inject.*;

/**
 * A list of {@link RestMatcher} objects.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestOpAnnotatedMethodBasics">@RestOp-Annotated Method Basics</a>
 * </ul>
 */
public class RestMatcherList {

	/**
	 * Builder class.
	 */
	public static class Builder {

		private final BeanStore beanStore;
		List<BeanInstantiator.Builder<RestMatcher>> entries;

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
		 * Appends the specified rest matcher classes to the list.
		 *
		 * @param values The values to add.
		 * @return This object.
		 */
		@SuppressWarnings({
			"unchecked" // Varargs with generics require unchecked cast
		})
		public Builder append(Class<? extends RestMatcher>...values) {
			for (var v : values)
				entries.add(BeanInstantiator.of(RestMatcher.class, beanStore).type(v));
			return this;
		}

		/**
		 * Appends the specified rest matcher objects to the list.
		 *
		 * @param values The values to add.
		 * @return This object.
		 */
		public Builder append(RestMatcher...values) {
			for (var v : values)
				entries.add(BeanInstantiator.of(RestMatcher.class, beanStore).impl(v));
			return this;
		}

		/**
		 * Builds the list.
		 *
		 * @return A new {@link RestMatcherList}.
		 */
		public RestMatcherList build() {
			return new RestMatcherList(this);
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

	private final RestMatcher[] optionalEntries;
	private final RestMatcher[] requiredEntries;

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the contents for this list.
	 */
	protected RestMatcherList(Builder builder) {
		List<RestMatcher> l = builder.entries.stream().map(BeanInstantiator.Builder::run).toList();
		optionalEntries = l.stream().filter(x -> ! x.required()).toArray(RestMatcher[]::new);
		requiredEntries = l.stream().filter(RestMatcher::required).toArray(RestMatcher[]::new);
	}

	/**
	 * Returns the entries in this list that are specified as optional.
	 *
	 * @return An unmodifiable list of entries in this list that are specified as optional.
	 */
	public RestMatcher[] getOptionalEntries() { return optionalEntries; }

	/**
	 * Returns the entries in this list that are specified as required.
	 *
	 * @return An unmodifiable list of entries in this list that are specified as required.
	 */
	public RestMatcher[] getRequiredEntries() { return requiredEntries; }
}