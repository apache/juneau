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
package org.apache.juneau.rest.server.view;

import java.io.*;

import org.apache.juneau.http.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;

/**
 * Abstract base for the child-resource flavors of the view mixin resources.
 *
 * <p>
 * Mirror of {@link ViewServlet} for routed-child deployment: each per-engine bridge module ships a thin
 * {@code <Engine>Resource} subclass that declares its own subtree mount via
 * {@link Rest#path() @Rest(path)} (e.g. {@code "/jsp"}) and supplies the per-engine
 * {@link RawTemplateDispatcher} (the matching flavor-neutral {@code <Engine>Dispatcher} worker) via
 * {@link #dispatcher()}.
 *
 * <p>
 * The single greedy op declared here ({@code @RestGet(path="/*")}) captures the trailing remainder under
 * the child's subtree mount and delegates straight to the shared {@link RawTemplateDispatcher}, so the
 * mixin, servlet, and child forms cannot drift.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jic'>{@link RawTemplateDispatcher}
 * 	<li class='jc'>{@link ViewServlet}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerCompositionMixinsAndPaths">REST Server &mdash; Composition (mixins, paths)</a>
 * </ul>
 *
 * @since 10.0.0
 */
@Rest
public abstract class ViewResource extends RestResource {

	/**
	 * Returns the per-engine {@link RawTemplateDispatcher} this child delegates raw template dispatch
	 * to.
	 *
	 * <p>
	 * Typically the matching flavor-neutral {@code <Engine>Dispatcher} worker (which implements
	 * {@link RawTemplateDispatcher}).  Never {@code null}.
	 *
	 * @return The raw-template dispatcher. Never {@code null}.
	 */
	protected abstract RawTemplateDispatcher dispatcher();

	/**
	 * [GET /*] &mdash; render the raw template identified by the trailing path under this child's
	 * subtree mount.
	 *
	 * @param path The trailing path segment after the mount prefix.
	 * @param req The current REST request.
	 * @param res The current REST response.
	 * @throws IOException If the underlying response writer fails.
	 */
	@RestGet(
		path="/*",
		summary="View",
		description="Render a raw template under this child's subtree mount.",
		swagger=@OpSwagger(ignore=true)
	)
	public void render(@Path("/*") String path, RestRequest req, RestResponse res) throws IOException {
		dispatcher().render(path, req, res);
	}
}
