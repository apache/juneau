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
package org.apache.juneau.rest.guard;

import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;

/**
 * A list of {@link RestGuard} objects.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.Guards">Guards</a>
 * </ul>
 */
public class RestGuardList {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Static creator.
	 *
	 * @param beanStore The bean store to use for creating beans.
	 * @return A new builder for this object.
	 */
	public static Builder create(BeanStore beanStore) {
		return new Builder(beanStore);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends BeanBuilder<RestGuardList> {

		List<BeanCreator<RestGuard>> entries;

		/**
		 * Constructor.
		 *
		 * @param beanStore The bean store to use for creating beans.
		 */
		protected Builder(BeanStore beanStore) {
			super(RestGuardList.class, beanStore);
			entries = list();
		}

		@Override /* BeanBuilder */
		protected RestGuardList buildDefault() {
			return new RestGuardList(this);
		}

		//-------------------------------------------------------------------------------------------------------------
		// Properties
		//-------------------------------------------------------------------------------------------------------------

		/**
		 * Appends the specified rest matcher classes to the list.
		 *
		 * @param values The values to add.
		 * @return This object.
		 */
		@SuppressWarnings("unchecked")
		public Builder append(Class<? extends RestGuard>...values) {
			for (Class<? extends RestGuard> v : values)
				entries.add(beanStore().createBean(RestGuard.class).type(v));
			return this;
		}

		/**
		 * Appends the specified rest matcher objects to the list.
		 *
		 * @param values The values to add.
		 * @return This object.
		 */
		public Builder append(RestGuard...values) {
			for (RestGuard v : values)
				entries.add(beanStore().createBean(RestGuard.class).impl(v));
			return this;
		}

		// <FluentSetters>

		@Override /* GENERATED - org.apache.juneau.BeanBuilder */
		public Builder impl(Object value) {
			super.impl(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanBuilder */
		public Builder type(Class<?> value) {
			super.type(value);
			return this;
		}

		// </FluentSetters>
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private RestGuard[] entries;

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
				.map(x -> x.run())
				.toArray(RestGuard[]::new);
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
