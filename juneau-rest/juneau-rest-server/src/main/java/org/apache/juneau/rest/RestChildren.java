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

import java.util.*;

import javax.servlet.*;

import org.apache.juneau.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.util.*;

/**
 * Implements the child resources of a {@link Rest}-annotated class.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jrs.AnnotatedClasses}
 * 	<li class='extlink'>{@source}
 * </ul>
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
	public static class Builder extends BeanBuilder<RestChildren> {

		final List<RestContext> list;

		/**
		 * Constructor.
		 *
		 * @param beanStore The bean store to use for creating beans.
		 */
		protected Builder(BeanStore beanStore) {
			super(RestChildren.class, beanStore);
			list = AList.create();
		}

		@Override /* BeanBuilder */
		protected RestChildren buildDefault() {
			return new RestChildren(this);
		}

		//-------------------------------------------------------------------------------------------------------------
		// Properties
		//-------------------------------------------------------------------------------------------------------------

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
	 * @param builder The HTTP call builder.
	 * @return The child that best matches the call, or an empty {@link Optional} if a match could not be made.
	 */
	public Optional<RestChildMatch> findMatch(RestSession.Builder builder) {
		String pi = builder.getPathInfoUndecoded();
		if ((! children.isEmpty()) && pi != null && ! pi.equals("/")) {
			for (RestContext rc : children.values()) {
				UrlPathMatcher upp = rc.getPathMatcher();
				UrlPathMatch uppm = upp.match(builder.getUrlPath());
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
