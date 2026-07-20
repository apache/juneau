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
package org.apache.juneau.rest.server;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.http.response.*;

/**
 * Encapsulates the set of {@link RestOp}-annotated methods within a single {@link Rest}-annotated object.
 */
public class RestOperations {

	/**
	 * Builder class.
	 */
	public static class Builder {

		private final BeanStore beanStore;
		TreeMap<String,TreeSet<RestOpContext>> map;
		Set<RestOpContext> set;

		/**
		 * Constructor.
		 *
		 * @param beanStore The bean store to use for creating beans.
		 */
		protected Builder(BeanStore beanStore) {
			this.beanStore = beanStore;
			map = new TreeMap<>();
			set = set();
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
		 * Adds a method context to this builder.
		 *
		 * @param value The REST method context to add. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder add(RestOpContext value) {
			return add(value.getHttpMethod(), value);
		}

		/**
		 * Adds a method context to this builder.
		 *
		 * @param httpMethodName The HTTP method name. Must not be <jk>null</jk>.
		 * @param value The REST method context to add. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder add(String httpMethodName, RestOpContext value) {
			httpMethodName = ucr(httpMethodName);
			map.computeIfAbsent(httpMethodName, k -> new TreeSet<>()).add(value);
			set.add(value);
			return this;
		}

		/**
		 * Builds the operations.
		 *
		 * @return A new {@link RestOperations}.
		 */
		public RestOperations build() {
			return new RestOperations(this);
		}
	}

	/**
	 * Represents a null value for the RestOperations class.
	 * 
	 * <p>
	 * This is used internally when no custom RestOperations implementation is specified.
	 * The {@code Void} class is used as a placeholder when the {@link Rest} annotation
	 * does not specify a custom {@code RestOperations} class.
	 */
	@SuppressWarnings({
		"java:S112" // throws Exception intentional - callback/lifecycle method
	})
	public final class Void extends RestOperations {

		/**
		 * Constructor.
		 *
		 * @param builder The builder. Must not be <jk>null</jk>.
		 * @throws Exception If initialization fails.
		 */
		public Void(Builder builder) throws Exception {
			super(builder);
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

	private final Map<String,List<RestOpContext>> map;
	private RestOpContext[] list;

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the settings for this object. Must not be <jk>null</jk>.
	 */
	public RestOperations(Builder builder) {
		Map<String,List<RestOpContext>> m = map();
		for (var e : builder.map.entrySet())
			m.put(e.getKey(), toList(e.getValue()));
		this.map = m;
		this.list = array(builder.set, RestOpContext.class);
	}

	/**
	 * Finds the method that should handle the specified call.
	 *
	 * @param session The HTTP call. Must not be <jk>null</jk>.
	 * @return The method that should handle the specified call.
	 * @throws MethodNotAllowed If no methods implement the requested HTTP method.
	 * @throws PreconditionFailed At least one method was found but it didn't match one or more matchers.
	 * @throws NotFound HTTP method match was found but matching path was not.
	 */
	@SuppressWarnings({
		"java:S3776" // Cognitive complexity acceptable for REST operation lookup/matching logic
	})
	public RestOpContext findOperation(RestSession session) throws MethodNotAllowed, PreconditionFailed, NotFound {
		String m = session.getMethod();

		int rc = 0;
		if (map.containsKey(m)) {
			for (var oc : map.get(m)) {
				int mrc = oc.match(session);
				if (mrc == 2)
					return oc;
				rc = Math.max(rc, mrc);
			}
		}

		if (map.containsKey("*")) {
			for (var oc : map.get("*")) {
				int mrc = oc.match(session);
				if (mrc == 2)
					return oc;
				rc = Math.max(rc, mrc);
			}
		}

		// If no paths matched, see if the path matches any other methods.
		// Note that we don't want to match against "/*" patterns such as getOptions().
		if (rc == 0) {
			for (var oc : list) {
				if (! oc.getPathPattern().endsWith("/*")) {
					int orc = oc.match(session);
					if (orc == 2)
						throw new MethodNotAllowed();
				}
			}
		}

		if (rc == 1)
			throw new PreconditionFailed("Method '%s' not found on resource on path '%s' with matching matcher.", m, session.getPathInfo());

		throw new NotFound("Java method matching path '%s' not found on resource '%s'.", session.getPathInfo(), cn(session.getResource()));
	}

	/**
	 * Returns the list of method contexts in this object.
	 *
	 * @return An unmodifiable list of method contexts in this object.
	 */
	public List<RestOpContext> getOpContexts() { return u(l(list)); }
}
