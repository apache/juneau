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
package org.apache.juneau.rest.ops;

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.beans.*;
import org.apache.juneau.rest.servlet.*;

/**
 * Mixin providing the group-navigation endpoint ({@code [GET /]}) that lists the host resource's child
 * resources.
 *
 * <p>
 * Single-responsibility op-mixin carved out of the former {@code BasicGroupOperations} interface (and the
 * concrete {@code getChildren(...)} method previously kept on the {@code Basic*Group} base classes). The
 * navigation page renders one entry per {@link Rest#children() @Rest(children)} resource declared on the
 * host.
 *
 * <h5 class='section'>Reaching the host's children:</h5>
 *
 * <p>
 * A mixin's {@code @RestOp} methods are bound to a per-mixin {@link RestContext} sub-context that has no
 * children of its own, so this op resolves the host's children via {@link RestMixin#getHostContext()
 * getHostContext()}{@code .getRestChildren()} rather than against the mixin's own (empty) context.  Because
 * {@code getHostContext()} is backed by the sub-context's already-populated parent linkage, this works
 * whether the mixin is composed onto a servlet host or a child-resource host.
 *
 * <h5 class='section'>Page decoration:</h5>
 *
 * <p>
 * Per the mixin sub-context inheritance model, the navigation page inherits the host's class-level
 * {@link org.apache.juneau.html.annotation.HtmlDocConfig @HtmlDocConfig} page decoration (navigation links,
 * etc.), so the rendered HTML matches the host's other endpoints.
 *
 * <h5 class='figure'>Composition example:</h5>
 *
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(mixins=NavigationMixin.<jk>class</jk>, children={Foo.<jk>class</jk>, Bar.<jk>class</jk>})
 * 	<jk>public class</jk> RootResource <jk>extends</jk> RestServlet { ... }
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link ChildResourceDescriptions}
 * 	<li class='jm'>{@link RestMixin#getHostContext()}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Composition (mixins, paths)</a>
 * </ul>
 *
 * @since 9.5.0
 */
@Rest
public class NavigationMixin extends RestMixin {

	/** No-arg constructor &mdash; navigation has no configurable state. */
	public NavigationMixin() {}

	/**
	 * [GET /] - Get child resources.
	 *
	 * <p>
	 * Returns a bean that lists and allows navigation to the host resource's child resources, resolved
	 * against the {@linkplain RestMixin#getHostContext() host context} so the listing reflects the host's
	 * children rather than the mixin's own (empty) sub-context.
	 *
	 * @param req The HTTP request.
	 * @return The bean containing links to the child resources.
	 */
	@RestGet(path="/", summary="Navigation page")
	public ChildResourceDescriptions getChildren(RestRequest req) {
		return new ChildResourceDescriptions(getHostContext(), req);
	}
}
