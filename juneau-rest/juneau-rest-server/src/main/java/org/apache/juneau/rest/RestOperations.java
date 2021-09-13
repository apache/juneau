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
import static org.apache.juneau.internal.ObjectUtils.*;
import static org.apache.juneau.rest.HttpRuntimeException.*;

import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Encapsulates the set of {@link RestOp}-annotated methods within a single {@link Rest}-annotated object.
 */
public class RestOperations {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Represents a null value for the {@link Rest#restOperationsClass()} annotation.
	 */
	@SuppressWarnings("javadoc")
	public final class Null extends RestOperations {
		public Null(Builder builder) throws Exception {
			super(builder);
		}
	}

	/**
	 * Creates a new builder.
	 *
	 * @return A new builder.
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
	public static class Builder  {

		TreeMap<String,TreeSet<RestOpContext>> map = new TreeMap<>();
		Set<RestOpContext> set = ASet.of();

		private BeanStore beanStore;
		private Class<? extends RestOperations> type;
		private RestOperations impl;

		/**
		 * Instantiates a {@link RestOperations} object based on the contents of this builder.
		 *
		 * @return A new {@link RestOperations} object.
		 */
		public RestOperations build() {
			try {
				if (impl != null)
					return impl;
				Class<? extends RestOperations> ic = firstNonNull(type, getDefaultImplClass());
				return BeanStore.of(beanStore).addBeans(Builder.class, this).createBean(ic);
			} catch (Exception e) {
				throw toHttpException(e, InternalServerError.class);
			}
		}

		/**
		 * Specifies the default implementation class if not specified via {@link #type(Class)}.
		 *
		 * @return The default implementation class if not specified via {@link #type(Class)}.
		 */
		protected Class<? extends RestOperations> getDefaultImplClass() {
			return RestOperations.class;
		}

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

		/**
		 * Specifies a {@link RestOperations} implementation subclass to use.
		 *
		 * <p>
		 * When specified, the {@link #build()} method will create an instance of that class instead of the default {@link RestOperations}.
		 *
		 * <p>
		 * The subclass must have a public constructor that takes in any of the following arguments:
		 * <ul>
		 * 	<li>{@link Builder} - This object.
		 * 	<li>Any beans found in the specified {@link #beanStore(BeanStore) bean store}.
		 * 	<li>Any {@link Optional} beans that may or may not be found in the specified {@link #beanStore(BeanStore) bean store}.
		 * </ul>
		 *
		 * @param value The implementation class to build.
		 * @return This object.
		 */
		public Builder type(Class<? extends RestOperations> value) {
			type = value;
			return this;
		}

		/**
		 * Specifies a {@link BeanStore} to use when resolving constructor arguments.
		 *
		 * @param value The bean store to use for resolving constructor arguments.
		 * @return This object.
		 */
		public Builder beanStore(BeanStore value) {
			beanStore = value;
			return this;
		}

		/**
		 * Specifies an already-instantiated bean for the {@link #build()} method to return.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public Builder impl(RestOperations value) {
			impl = value;
			return this;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final Map<String,List<RestOpContext>> map;
	private List<RestOpContext> list;

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the settings for this object.
	 */
	public RestOperations(Builder builder) {
		AMap<String,List<RestOpContext>> m = AMap.create();
		for (Map.Entry<String,TreeSet<RestOpContext>> e : builder.map.entrySet())
			m.put(e.getKey(), AList.of(e.getValue()));
		this.map = m;
		this.list = AList.of(builder.set);
	}

	/**
	 * Finds the method that should handle the specified call.
	 *
	 * @param call The HTTP call.
	 * @return The method that should handle the specified call.
	 * @throws MethodNotAllowed If no methods implement the requested HTTP method.
	 * @throws PreconditionFailed At least one method was found but it didn't match one or more matchers.
	 * @throws NotFound HTTP method match was found but matching path was not.
	 */
	public RestOpContext findOperation(RestCall call) throws MethodNotAllowed, PreconditionFailed, NotFound {
		String m = call.getMethod();

		int rc = 0;
		if (map.containsKey(m)) {
			for (RestOpContext oc : map.get(m)) {
				int mrc = oc.match(call);
				if (mrc == 2)
					return oc;
				rc = Math.max(rc, mrc);
			}
		}

		if (map.containsKey("*")) {
			for (RestOpContext oc : map.get("*")) {
				int mrc = oc.match(call);
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
					int orc = oc.match(call);
					if (orc == 2)
						throw new MethodNotAllowed();
				}
			}
		}

		if (rc == 1)
			throw new PreconditionFailed("Method ''{0}'' not found on resource on path ''{1}'' with matching matcher.", m, call.getPathInfo());

		throw new NotFound("Java method matching path ''{0}'' not found on resource ''{1}''.", call.getPathInfo(), className(call.getResource()));
	}


	/**
	 * Returns the list of method contexts in this object.
	 *
	 * @return An unmodifiable list of method contexts in this object.
	 */
	public List<RestOpContext> getOpContexts() {
		return Collections.unmodifiableList(list);
	}
}
