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
package org.apache.juneau.rest;

import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Encapsulates the set of {@link RestOp}-annotated methods within a single {@link Rest}-annotated object.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class RestOperations {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Represents a null value for the {@link Rest#restOperationsClass()} annotation.
	 */
	@SuppressWarnings("javadoc")
	public final class Void extends RestOperations {
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

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends BeanBuilder<RestOperations> {

		TreeMap<String,TreeSet<RestOpContext>> map;
		Set<RestOpContext> set;

		/**
		 * Constructor.
		 *
		 * @param beanStore The bean store to use for creating beans.
		 */
		protected Builder(BeanStore beanStore) {
			super(RestOperations.class, beanStore);
			map = new TreeMap<>();
			set = set();
		}

		@Override /* BeanBuilder */
		protected RestOperations buildDefault() {
			return new RestOperations(this);
		}

		//-------------------------------------------------------------------------------------------------------------
		// Properties
		//-------------------------------------------------------------------------------------------------------------

		/**
		 * Adds a method context to this builder.
		 *
		 * @param value The REST method context to add.
		 * @return Adds a method context to this builder.
		 */
		public Builder add(RestOpContext value) {
			return add(value.getHttpMethod(), value);
		}

		/**
		 * Adds a method context to this builder.
		 *
		 * @param httpMethodName The HTTP method name.
		 * @param value The REST method context to add.
		 * @return Adds a method context to this builder.
		 */
		public Builder add(String httpMethodName, RestOpContext value) {
			httpMethodName = httpMethodName.toUpperCase();
			if (! map.containsKey(httpMethodName))
				map.put(httpMethodName, new TreeSet<>());
			map.get(httpMethodName).add(value);
			set.add(value);
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

	private final Map<String,List<RestOpContext>> map;
	private RestOpContext[] list;

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the settings for this object.
	 */
	public RestOperations(Builder builder) {
		Map<String,List<RestOpContext>> m = map();
		for (Map.Entry<String,TreeSet<RestOpContext>> e : builder.map.entrySet())
			m.put(e.getKey(), listFrom(e.getValue()));
		this.map = m;
		this.list = array(builder.set, RestOpContext.class);
	}

	/**
	 * Finds the method that should handle the specified call.
	 *
	 * @param session The HTTP call.
	 * @return The method that should handle the specified call.
	 * @throws MethodNotAllowed If no methods implement the requested HTTP method.
	 * @throws PreconditionFailed At least one method was found but it didn't match one or more matchers.
	 * @throws NotFound HTTP method match was found but matching path was not.
	 */
	public RestOpContext findOperation(RestSession session) throws MethodNotAllowed, PreconditionFailed, NotFound {
		String m = session.getMethod();

		int rc = 0;
		if (map.containsKey(m)) {
			for (RestOpContext oc : map.get(m)) {
				int mrc = oc.match(session);
				if (mrc == 2)
					return oc;
				rc = Math.max(rc, mrc);
			}
		}

		if (map.containsKey("*")) {
			for (RestOpContext oc : map.get("*")) {
				int mrc = oc.match(session);
				if (mrc == 2)
					return oc;
				rc = Math.max(rc, mrc);
			}
		}

		// If no paths matched, see if the path matches any other methods.
		// Note that we don't want to match against "/*" patterns such as getOptions().
		if (rc == 0) {
			for (RestOpContext oc : list) {
				if (! oc.getPathPattern().endsWith("/*")) {
					int orc = oc.match(session);
					if (orc == 2)
						throw new MethodNotAllowed();
				}
			}
		}

		if (rc == 1)
			throw new PreconditionFailed("Method ''{0}'' not found on resource on path ''{1}'' with matching matcher.", m, session.getPathInfo());

		throw new NotFound("Java method matching path ''{0}'' not found on resource ''{1}''.", session.getPathInfo(), className(session.getResource()));
	}


	/**
	 * Returns the list of method contexts in this object.
	 *
	 * @return An unmodifiable list of method contexts in this object.
	 */
	public List<RestOpContext> getOpContexts() {
		return ulist(list);
	}
}
