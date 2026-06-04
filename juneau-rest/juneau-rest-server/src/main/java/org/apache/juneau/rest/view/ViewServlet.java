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
package org.apache.juneau.rest.view;

import java.io.*;

import org.apache.juneau.http.annotation.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.servlet.*;

/**
 * Abstract base for the standalone view-servlet companions of the view mixin resources.
 *
 * <p>
 * Each per-engine bridge module ({@code juneau-rest-server-view-jsp},
 * {@code juneau-rest-server-view-freemarker}, {@code juneau-rest-server-view-mustache},
 * {@code juneau-rest-server-view-thymeleaf}) ships a thin {@code Basic*Servlet} subclass that:
 *
 * <ol class='spaced-list'>
 * 	<li>Declares its own sibling top-level mount via {@link Rest#paths() @Rest(paths)} (e.g.
 * 		{@code "/jsp/*"}), so the servlet is registered as a sibling of the host application's other
 * 		servlets ({@code /rest/*}, {@code /jsp/*}, ...) rather than composed under a host root.
 * 	<li>Supplies the per-engine {@link RawTemplateDispatcher} (the matching flavor-neutral
 * 		{@code <Engine>Dispatcher} worker) via {@link #dispatcher()}.
 * </ol>
 *
 * <p>
 * The single greedy op declared here ({@code @RestGet(path="/*")}) captures the trailing remainder
 * under the servlet's mount and delegates straight to the shared {@link RawTemplateDispatcher}.
 * Because the mixin and standalone forms both route through the same {@code RawTemplateDispatcher}
 * implementation, their raw file-serving behavior cannot drift.
 *
 * <h5 class='section'>Standalone vs mixin:</h5>
 *
 * <p>
 * The mixin {@code Basic*Resource} keeps its op pinned at {@code /<token>/*} for composition into a
 * host at {@code /}; the standalone {@code Basic*Servlet} moves the mount to the class level
 * ({@code @Rest(paths="/<token>/*")}) and pins the op at {@code /*}, eliminating the path-doubling
 * that a mixin-shaped op would otherwise produce when the servlet itself is mounted at
 * {@code /<token>/*}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jic'>{@link RawTemplateDispatcher}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Composition (mixins, paths)</a>
 * </ul>
 *
 * @serial exclude
 * @since 10.0.0
 */
@Rest
public abstract class ViewServlet extends RestServlet {

	private static final long serialVersionUID = 1L;

	/**
	 * Returns the per-engine {@link RawTemplateDispatcher} this servlet delegates raw template
	 * dispatch to.
	 *
	 * <p>
	 * Typically the matching flavor-neutral {@code <Engine>Dispatcher} worker (which implements
	 * {@link RawTemplateDispatcher}).  Never {@code null}.
	 *
	 * @return The raw-template dispatcher. Never {@code null}.
	 */
	protected abstract RawTemplateDispatcher dispatcher();

	/**
	 * [GET /*] &mdash; render the raw template identified by the trailing path under this servlet's
	 * sibling top-level mount.
	 *
	 * <p>
	 * The {@code @Path("/*") String path} captures the multi-segment remainder after the mount
	 * prefix (e.g. a request for {@code /jsp/admin/dashboard.jsp} on a servlet mounted at
	 * {@code /jsp/*} matches with {@code path = "admin/dashboard.jsp"}).  Delegates verbatim to the
	 * shared {@link RawTemplateDispatcher}.
	 *
	 * @param path The trailing path segment after the mount prefix.
	 * @param req The current REST request.
	 * @param res The current REST response.
	 * @throws IOException If the underlying response writer fails.
	 */
	@RestGet(
		path="/*",
		summary="View",
		description="Render a raw template under this servlet's sibling top-level mount.",
		swagger=@OpSwagger(ignore=true)
	)
	public void render(@Path("/*") String path, RestRequest req, RestResponse res) throws IOException {
		dispatcher().render(path, req, res);
	}
}
