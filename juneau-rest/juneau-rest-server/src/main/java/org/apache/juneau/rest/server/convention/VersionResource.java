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

import java.io.*;

import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;

/**
 * Child-resource flavor of the {@link VersionMixin} version/build-metadata capability.
 *
 * <p>
 * Mounts as a <b>routed child</b> via {@link Rest#children() @Rest(children=VersionResource.class)}
 * under a parent at the subtree {@code /version} and serves the same deployment-introspection JSON map
 * as the mixin by delegating to a shared flavor-neutral {@link VersionProvider} worker bean &mdash; so the forms
 * cannot drift.
 *
 * <p>
 * Whereas the {@link VersionMixin} mixin pins its op at {@code /version} for composition into a host at
 * {@code /}, this child declares the subtree at the class level
 * ({@link Rest#path() @Rest(path="/version")}) and pins the op at {@code /*}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link VersionMixin}
 * 	<li class='jc'>{@link VersionServlet}
 * 	<li class='jc'>{@link VersionProvider}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Composition (mixins, paths)</a>
 * </ul>
 *
 * @since 10.0.0
 */
// @formatter:off
@Rest(path="/version")
public class VersionResource extends RestResource {

	private final VersionProvider worker;

	/** No-arg constructor &mdash; uses a default {@link VersionProvider} worker. */
	public VersionResource() {
		this(new VersionProvider());
	}

	/**
	 * Worker constructor.
	 *
	 * @param worker The shared flavor-neutral version worker this child delegates to. Must not be
	 * 	{@code null}.
	 */
	protected VersionResource(VersionProvider worker) {
		this.worker = worker;
	}

	/**
	 * [GET /*] &mdash; emit the assembled metadata as a JSON map.
	 *
	 * @param res The current REST response.
	 * @throws IOException If an I/O error occurs while writing the response.
	 */
	@RestGet(
		path="/*",
		summary="Version / build metadata",
		description="Deployment-introspection metadata (name, version, git, build).",
		swagger=@OpSwagger(ignore=true)
	)
	public void getInfo(RestResponse res) throws IOException {
		worker.serve(res);
	}
}
