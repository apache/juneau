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
package org.apache.juneau.rest.matcher;

import static java.util.stream.Collectors.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;

/**
 * A list of {@link RestMatcher} objects.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.RestOpAnnotatedMethods">@RestOp-Annotated Methods</a>
 * </ul>
 */
public class RestMatcherList {

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
	public static class Builder extends BeanBuilder<RestMatcherList> {

		List<BeanCreator<RestMatcher>> entries;

		/**
		 * Constructor.
		 *
		 * @param beanStore The bean store to use for creating beans.
		 */
		protected Builder(BeanStore beanStore) {
			super(RestMatcherList.class, beanStore);
			entries = list();
		}

		@Override /* BeanBuilder */
		protected RestMatcherList buildDefault() {
			return new RestMatcherList(this);
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
		public Builder append(Class<? extends RestMatcher>...values) {
			for (Class<? extends RestMatcher> v : values)
				entries.add(beanStore().createBean(RestMatcher.class).type(v));
			return this;
		}

		/**
		 * Appends the specified rest matcher objects to the list.
		 *
		 * @param values The values to add.
		 * @return This object.
		 */
		public Builder append(RestMatcher...values) {
			for (RestMatcher v : values)
				entries.add(beanStore().createBean(RestMatcher.class).impl(v));
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
				.map(x -> x.run())
				.collect(toList());
		optionalEntries = l.stream().filter(x -> ! x.required()).toArray(RestMatcher[]::new);
		requiredEntries = l.stream().filter(x -> x.required()).toArray(RestMatcher[]::new);
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
