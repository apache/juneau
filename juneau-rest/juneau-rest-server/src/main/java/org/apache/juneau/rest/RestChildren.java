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
import static org.apache.juneau.rest.HttpRuntimeException.*;

import java.util.*;

import javax.servlet.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.util.*;

/**
 * Implements the child resources of a {@link Rest}-annotated class.
 */
public class RestChildren {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Represents a null value for the {@link Rest#restChildrenClass()} annotation.
	 */
	@SuppressWarnings("javadoc")
	public final class Null extends RestChildren {
		public Null(Builder builder) throws Exception {
			super(builder);
		}
	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder for this object.
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
	public static class Builder {

		final List<RestContext> list = AList.create();

		private BeanStore beanStore;
		private Class<? extends RestChildren> type;
		private RestChildren impl;

		/**
		 * Instantiates a {@link RestChildren} object based on the contents of this builder.
		 *
		 * @return A new {@link RestChildren} object.
		 */
		public RestChildren build() {
			try {
				if (impl != null)
					return impl;
				return BeanCreator.create(RestChildren.class).type(isConcrete(type) ? type : getDefaultImplClass()).store(beanStore).builder(this).run();
			} catch (Exception e) {
				throw toHttpException(e, InternalServerError.class);
			}
		}

		/**
		 * Specifies the default implementation class if not specified via {@link #type(Class)}.
		 *
		 * @return The default implementation class if not specified via {@link #type(Class)}.
		 */
		protected Class<? extends RestChildren> getDefaultImplClass() {
			return RestChildren.class;
		}

		/**
		 * Adds a child resource to this builder.
		 *
		 * @param value The REST context of the child resource.
		 * @return This object.
		 */
		public Builder add(RestContext value) {
			list.add(value);
			return this;
		}

		/**
		 * Specifies a {@link RestChildren} implementation subclass to use.
		 *
		 * <p>
		 * When specified, the {@link #build()} method will create an instance of that class instead of the default {@link RestChildren}.
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
		public Builder type(Class<? extends RestChildren> value) {
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
		public Builder impl(RestChildren value) {
			impl = value;
			return this;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final Map<String,RestContext> children = Collections.synchronizedMap(new LinkedHashMap<String,RestContext>());

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the settings for this object.
	 */
	public RestChildren(Builder builder) {
		for (RestContext rc : builder.list)
			children.put(rc.getPath(), rc);
	}

	/**
	 * Looks through the registered children of this object and returns the best match.
	 *
	 * @param call The HTTP call.
	 * @return The child that best matches the call, or an empty {@link Optional} if a match could not be made.
	 */
	public Optional<RestChildMatch> findMatch(RestCall call) {
		String pi = call.getPathInfoUndecoded();
		if ((! children.isEmpty()) && pi != null && ! pi.equals("/")) {
			for (RestContext rc : children.values()) {
				UrlPathMatcher upp = rc.getPathMatcher();
				UrlPathMatch uppm = upp.match(call.getUrlPath());
				if (uppm != null) {
					return Optional.of(RestChildMatch.create(uppm, rc));
				}
			}
		}
		return Optional.empty();
	}

	/**
	 * Returns the children in this object as a map.
	 *
	 * <p>
	 * The keys are the {@link RestContext#getPath() paths} of the child contexts.
	 *
	 * @return The children as an unmodifiable map.
	 */
	public Map<String,RestContext> asMap() {
		return Collections.unmodifiableMap(children);
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Lifecycle methods.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Called during servlet initialization on all children to invoke all {@link HookEvent#POST_INIT} methods.
	 *
	 * @throws ServletException Error occurred.
	 */
	public void postInit() throws ServletException {
		for (RestContext childContext : children.values())
			childContext.postInit();
	}

	/**
	 * Called during servlet initialization on all children to invoke all {@link HookEvent#POST_INIT_CHILD_FIRST} methods.
	 *
	 * @throws ServletException Error occurred.
	 */
	public void postInitChildFirst() throws ServletException {
		for (RestContext childContext : children.values())
			childContext.postInitChildFirst();
	}

	/**
	 * Called during servlet destruction on all children to invoke all {@link HookEvent#DESTROY} and {@link Servlet#destroy()} methods.
	 */
	public void destroy() {
		for (RestContext r : children.values()) {
			r.destroy();
			if (r.getResource() instanceof Servlet)
				((Servlet)r.getResource()).destroy();
		}
	}
}
