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
package org.apache.juneau.rest.server.convention;

import org.apache.juneau.http.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;

/**
 * Child-resource flavor of the {@link FaviconMixin} mixin.
 *
 * <p>
 * Mounts as a <b>routed child</b> via {@link Rest#children() @Rest(children=FaviconResource.class)}
 * under a parent at the subtree {@code /favicon.ico} and serves the configured favicon bytes by
 * delegating to a shared flavor-neutral {@link FaviconProvider} worker bean &mdash; so the forms cannot drift.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link FaviconMixin}
 * 	<li class='jc'>{@link FaviconServlet}
 * 	<li class='jc'>{@link FaviconProvider}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Composition (mixins, paths)</a>
 * </ul>
 *
 * @since 10.0.0
 */
// @formatter:off
@Rest(path="/favicon.ico")
public class FaviconResource extends RestResource {

	private final FaviconProvider worker;

	/** No-arg constructor &mdash; uses a default {@link FaviconProvider} worker. */
	public FaviconResource() {
		this(new FaviconProvider());
	}

	/**
	 * Worker constructor.
	 *
	 * @param worker The shared flavor-neutral favicon worker this child delegates to. Must not be
	 * 	{@code null}.
	 */
	protected FaviconResource(FaviconProvider worker) {
		this.worker = worker;
	}

	/**
	 * [GET /*] &mdash; serve the configured favicon bytes.
	 *
	 * @return The favicon as an {@link HttpResource} with proper headers.
	 */
	@RestGet(
		path="/*",
		summary="Favorites icon",
		description="Browser favorites icon (favicon.ico).",
		swagger=@OpSwagger(ignore=true)
	)
	public HttpResource getFavicon() {
		return worker.serve();
	}
}
