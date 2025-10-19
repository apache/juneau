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
package org.apache.juneau.rest.processor;

import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.internal.CollectionBuilders.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.common.utils.*;
import org.apache.juneau.cp.*;

/**
 * A list of {@link ResponseProcessor} objects.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ResponseProcessors">Response Processors</a>
 * </ul>
 */
public class ResponseProcessorList {
	/**
	 * Builder class.
	 */
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

		/**
		 * Appends the specified rest response processor classes to the list.
		 *
		 * @param values The values to add.
		 * @return This object.
		 * @throws IllegalArgumentException if any class does not extend from {@link ResponseProcessor}.
		 */
		public Builder add(Class<?>...values) {
			CollectionUtils.addAll(entries, (Object[])Utils.assertClassArrayArgIsType("values", ResponseProcessor.class, values));
			return this;
		}

		/**
		 * Appends the specified rest response processor objects to the list.
		 *
		 * @param values The values to add.
		 * @return This object.
		 */
		public Builder add(ResponseProcessor...values) {
			CollectionUtils.addAll(entries, (Object[])values);
			return this;
		}

		@Override /* Overridden from BeanBuilder */
		public Builder impl(Object value) {
			super.impl(value);
			return this;
		}

		@Override /* Overridden from BeanBuilder */
		public Builder type(Class<?> value) {
			super.type(value);
			return this;
		}

		@Override /* Overridden from BeanBuilder */
		protected ResponseProcessorList buildDefault() {
			return new ResponseProcessorList(this);
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

	private static ResponseProcessor instantiate(Object o, BeanStore bs) {
		if (o instanceof ResponseProcessor)
			return (ResponseProcessor)o;
		try {
			return bs.createBean(ResponseProcessor.class).type((Class<?>)o).run();
		} catch (ExecutableException e) {
			throw new ConfigException(e, "Could not instantiate class {0}", o);
		}
	}

	private final ResponseProcessor[] entries;

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the contents for this list.
	 */
	protected ResponseProcessorList(Builder builder) {
		BeanStore bs = builder.beanStore();
		// @formatter:off
		entries =
			builder
				.entries
				.stream()
				.map(x -> instantiate(x, bs))
				.toArray(ResponseProcessor[]::new);
		// @formatter:on
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