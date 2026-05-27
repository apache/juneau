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

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;

import java.util.*;

import org.apache.juneau.ConfigException;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.rest.view.*;

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
	public static class Builder {

		private final BeanStore beanStore;
		List<Object> entries;

		/**
		 * Constructor.
		 *
		 * @param beanStore The bean store to use for creating beans.
		 */
		protected Builder(BeanStore beanStore) {
			this.beanStore = beanStore;
			this.entries = list();
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
		 * Appends the specified rest response processor classes to the list.
		 *
		 * <p>
		 * Same-class deduplication is applied: if an entry of the same {@link Class} already exists
		 * in the list, the duplicate is silently skipped.
		 *
		 * @param values The values to add.
		 * @return This object.
		 * @throws IllegalArgumentException if any class does not extend from {@link ResponseProcessor}.
		 */
		public Builder add(Class<?>...values) {
			for (var v : assertClassArrayArgIsType("values", ResponseProcessor.class, values))
				if (!entryClassExists(v))
					entries.add(v);
			return this;
		}

		/**
		 * Appends the specified rest response processor objects to the list.
		 *
		 * <p>
		 * Same-class deduplication is applied: if an entry of the same {@link Class} already exists
		 * in the list, the duplicate is silently skipped.
		 *
		 * @param values The values to add.
		 * @return This object.
		 */
		public Builder add(ResponseProcessor...values) {
			for (var v : values)
				if (!entryClassExists(v.getClass()))
					entries.add(v);
			return this;
		}

		// Returns true if an entry with the given class is already present (either as a Class token
		// or as an instantiated ResponseProcessor of that class).
		private boolean entryClassExists(Class<?> cls) {
			for (var e : entries) {
				if (e instanceof Class<?> e2 && e2 == cls)
					return true;
				if (e instanceof ResponseProcessor e2 && e2.getClass() == cls)
					return true;
			}
			return false;
		}

		/**
		 * Builds the list.
		 *
		 * @return A new {@link ResponseProcessorList}.
		 */
		public ResponseProcessorList build() {
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

	@SuppressWarnings("unchecked")
	private static ResponseProcessor instantiate(Object o, BeanStore bs) {
		if (o instanceof ResponseProcessor o2)
			return o2;
		try {
			return BeanInstantiator.of(ResponseProcessor.class, bs).type((Class<? extends ResponseProcessor>) o).run();
		} catch (ExecutableException e) {
			throw new ConfigException(e, "Could not instantiate class {0}", o);
		}
	}

	private final ResponseProcessor[] entries;

	/**
	 * Constructor.
	 *
	 * <p>
	 * After instantiating all processors, a partition pass runs once at build time (O(N)):
	 * any processor implementing {@link ViewRenderer} is repositioned to run immediately before
	 * the first {@link CatchAllResponseProcessor} in the chain.  If no {@link ViewRenderer} or
	 * no {@link CatchAllResponseProcessor} is present, the chain is left unchanged.
	 *
	 * @param builder The builder containing the contents for this list.
	 */
	protected ResponseProcessorList(Builder builder) {
		var bs = builder.beanStore();
		// Instantiate all entries first.
		var list = new ArrayList<ResponseProcessor>(builder.entries.size());
		for (var x : builder.entries)
			list.add(instantiate(x, bs));

		// Partition pass: reposition ViewRenderers before the first CatchAllResponseProcessor.
		var viewRenderers = new ArrayList<ResponseProcessor>();
		for (var p : list)
			if (p instanceof ViewRenderer)
				viewRenderers.add(p);

		var hasCatchAll = list.stream().anyMatch(p -> p instanceof CatchAllResponseProcessor);

		if (!viewRenderers.isEmpty() && hasCatchAll) {
			// Build new list: non-ViewRenderer entries in original order, then insert all
			// ViewRenderers immediately before the first CatchAllResponseProcessor.
			var reordered = new ArrayList<ResponseProcessor>(list.size());
			for (var p : list)
				if (!(p instanceof ViewRenderer))
					reordered.add(p);

			var insertAt = -1;
			for (var i = 0; i < reordered.size(); i++) {
				if (reordered.get(i) instanceof CatchAllResponseProcessor) {
					insertAt = i;
					break;
				}
			}
			if (insertAt == -1)
				insertAt = reordered.size();
			reordered.addAll(insertAt, viewRenderers);
			list = reordered;
		}

		entries = list.toArray(ResponseProcessor[]::new);
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