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

import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.util.*;

/**
 * Implements the child resources of a {@link Rest}-annotated class.
 */
public class RestChildren {

	/**
	 * Represents a null value for the {@link Rest#restChildrenClass()} annotation.
	 */
	@SuppressWarnings("javadoc")
	public final class Null extends RestChildren {
		public Null(RestChildrenBuilder builder) throws Exception {
			super(builder);
		}
	}

	private final Map<String,RestContext> children = Collections.synchronizedMap(new LinkedHashMap<String,RestContext>());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder for this object.
	 */
	public static RestChildrenBuilder create() {
		return new RestChildrenBuilder();
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the settings for this object.
	 */
	public RestChildren(RestChildrenBuilder builder) {
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
