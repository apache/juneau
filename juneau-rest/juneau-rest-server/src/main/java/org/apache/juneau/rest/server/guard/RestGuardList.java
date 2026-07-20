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
package org.apache.juneau.rest.server.guard;

import static org.apache.juneau.commons.utils.CollectionUtils.*;

import java.util.*;

import org.apache.juneau.commons.inject.*;

/**
 * A list of {@link RestGuard} objects.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Guards">Guards</a>
 * </ul>
 */
public class RestGuardList {

	/**
	 * Builder class.
	 */
	public static class Builder {

		private final BeanStore beanStore;
		List<BeanInstantiator.Builder<RestGuard>> entries;

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
		 * Appends the specified rest guard classes to the list.
		 *
		 * @param values The values to add.
		 * 	<br>Must not be <jk>null</jk>.
		 * @return This object.
		 */
		@SuppressWarnings({
			"unchecked" // Varargs with generics require unchecked cast
		})
		public Builder append(Class<? extends RestGuard>...values) {
			for (var v : values)
				entries.add(BeanInstantiator.of(RestGuard.class, beanStore).type(v));
			return this;
		}

		/**
		 * Appends the specified rest guard objects to the list.
		 *
		 * @param values The values to add.
		 * 	<br>Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder append(RestGuard...values) {
			for (var v : values)
				entries.add(BeanInstantiator.of(RestGuard.class, beanStore).impl(v));
			return this;
		}

		/**
		 * Builds the list.
		 *
		 * @return A new {@link RestGuardList}.
		 */
		public RestGuardList build() {
			return new RestGuardList(this);
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

	private RestGuard[] entries;

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the contents for this list.
	 * 	<br>Must not be <jk>null</jk>.
	 */
	protected RestGuardList(Builder builder) {
		entries = builder.entries.stream().map(BeanInstantiator.Builder::run).toArray(RestGuard[]::new);
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