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
package org.apache.juneau.rest;

import static org.apache.juneau.common.utils.Utils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.common.utils.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.util.*;

import jakarta.servlet.*;

/**
 * Implements the child resources of a {@link Rest}-annotated class.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestAnnotatedClassBasics">@Rest-Annotated Class Basics</a>
 * </ul>
 */
public class RestChildren {
	/**
	 * Builder class.
	 */
	public static class Builder extends BeanBuilder<RestChildren> {

		final List<RestContext> list;

		/**
		 * Constructor.
		 *
		 * @param beanStore The bean store to use for creating beans.
		 */
		protected Builder(BeanStore beanStore) {
			super(RestChildren.class, beanStore);
			list = Utils.list();
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
		protected RestChildren buildDefault() {
			return new RestChildren(this);
		}
	}

	/**
	 * Represents a null value for the {@link Rest#restChildrenClass()} annotation.
	 */
	@SuppressWarnings("javadoc")
	public final class Void extends RestChildren {
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

	private final Map<String,RestContext> children = CollectionUtils.synced(CollectionUtils.map());

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
	 * Returns the children in this object as a map.
	 *
	 * <p>
	 * The keys are the {@link RestContext#getPath() paths} of the child contexts.
	 *
	 * @return The children as an unmodifiable map.
	 */
	public Map<String,RestContext> asMap() {
		return u(children);
	}

	/**
	 * Called during servlet destruction on all children to invoke all {@link RestDestroy} and {@link Servlet#destroy()} methods.
	 */
	public void destroy() {
		for (RestContext r : children.values()) {
			r.destroy();
			if (r.getResource() instanceof Servlet)
				((Servlet)r.getResource()).destroy();
		}
	}

	/**
	 * Looks through the registered children of this object and returns the best match.
	 *
	 * @param builder The HTTP call builder.
	 * @return The child that best matches the call, or an empty {@link Optional} if a match could not be made.
	 */
	public Optional<RestChildMatch> findMatch(RestSession.Builder builder) {
		var pi = builder.getPathInfoUndecoded();
		if ((! children.isEmpty()) && nn(pi) && ! pi.equals("/")) {
			for (RestContext rc : children.values()) {
				UrlPathMatcher upp = rc.getPathMatcher();
				UrlPathMatch uppm = upp.match(builder.getUrlPath());
				if (nn(uppm)) {
					return Utils.opt(RestChildMatch.create(uppm, rc));
				}
			}
		}
		return opte();
	}

	/**
	 * Called during servlet initialization on all children to invoke all {@link RestPostInit} child-last methods.
	 *
	 * @throws ServletException Error occurred.
	 */
	public void postInit() throws ServletException {
		for (RestContext childContext : children.values())
			childContext.postInit();
	}

	/**
	 * Called during servlet initialization on all children to invoke all {@link RestPostInit} child-first methods.
	 *
	 * @throws ServletException Error occurred.
	 */
	public void postInitChildFirst() throws ServletException {
		for (RestContext childContext : children.values())
			childContext.postInitChildFirst();
	}
}