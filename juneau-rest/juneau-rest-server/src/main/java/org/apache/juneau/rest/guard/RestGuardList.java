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

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;

/**
 * A list of {@link RestGuard} objects.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jrs.Guards}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public class RestGuardList {

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
	@FluentSetters
	public static class Builder extends BeanBuilder<RestGuardList> {

		AList<BeanCreator<RestGuard>> entries;

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(RestGuardList.class);
			entries = AList.create();
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder being copied.
		 */
		protected Builder(Builder copyFrom) {
			super(copyFrom);
			entries = AList.of(copyFrom.entries);
		}

		@Override /* BeanBuilder */
		protected RestGuardList buildDefault() {
			return new RestGuardList(this);
		}

		@Override /* BeanBuilder */
		public Builder copy() {
			return new Builder(this);
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
				entries.append(BeanCreator.of(RestGuard.class).type(v));
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
				entries.append(BeanCreator.of(RestGuard.class).impl(v));
			return this;
		}

		// <FluentSetters>

		@Override /* GENERATED - org.apache.juneau.BeanBuilder */
		public Builder beanStore(BeanStore value) {
			super.beanStore(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanBuilder */
		public Builder impl(Object value) {
			super.impl(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanBuilder */
		public Builder outer(Object value) {
			super.outer(value);
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
		BeanStore bs = builder.beanStore().orElse(BeanStore.INSTANCE);
		entries =
			builder
				.entries
				.stream()
				.map(x -> x.store(bs).run())
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
