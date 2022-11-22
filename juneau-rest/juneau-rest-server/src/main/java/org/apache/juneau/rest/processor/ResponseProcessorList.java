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
package org.apache.juneau.rest.processor;

import static org.apache.juneau.common.internal.ArgUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;

/**
 * A list of {@link ResponseProcessor} objects.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.ResponseProcessors">Response Processors</a>
 * </ul>
 */
public class ResponseProcessorList {

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
	public static class Builder extends BeanBuilder<ResponseProcessorList> {

		List<Object> entries;

		/**
		 * Constructor.
		 *
		 * @param beanStore The bean store to use for creating beans.
		 */
		protected Builder(BeanStore beanStore) {
			super(ResponseProcessorList.class, beanStore);
			this.entries = list();
		}

		@Override /* BeanBuilder */
		protected ResponseProcessorList buildDefault() {
			return new ResponseProcessorList(this);
		}

		//-------------------------------------------------------------------------------------------------------------
		// Properties
		//-------------------------------------------------------------------------------------------------------------

		/**
		 * Appends the specified rest response processor classes to the list.
		 *
		 * @param values The values to add.
		 * @return This object.
		 * @throws IllegalArgumentException if any class does not extend from {@link ResponseProcessor}.
		 */
		public Builder add(Class<?>...values) {
			addAll(entries, (Object[])assertClassArrayArgIsType("values", ResponseProcessor.class, values));
			return this;
		}

		/**
		 * Appends the specified rest response processor objects to the list.
		 *
		 * @param values The values to add.
		 * @return This object.
		 */
		public Builder add(ResponseProcessor...values) {
			addAll(entries, (Object[])values);
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

	private final ResponseProcessor[] entries;

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the contents for this list.
	 */
	protected ResponseProcessorList(Builder builder) {
		BeanStore bs = builder.beanStore();
		entries =
			builder
				.entries
				.stream()
				.map(x -> instantiate(x, bs))
				.toArray(ResponseProcessor[]::new);
	}

	private static ResponseProcessor instantiate(Object o, BeanStore bs) {
		if (o instanceof ResponseProcessor)
			return (ResponseProcessor)o;
		try {
			return bs.createBean(ResponseProcessor.class).type((Class<?>)o).run();
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
