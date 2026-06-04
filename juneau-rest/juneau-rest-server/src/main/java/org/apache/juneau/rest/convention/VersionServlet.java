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
package org.apache.juneau.rest.convention;

import java.io.*;

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.servlet.*;

/**
 * Standalone servlet companion of the {@link VersionMixin} mixin.
 *
 * <p>
 * Mounts as a <b>sibling top-level servlet</b> at {@code /version/*} and serves the same
 * deployment-introspection JSON map as the mixin by delegating to a shared flavor-neutral
 * {@link VersionProvider} worker bean &mdash; so the two forms cannot drift.
 *
 * <p>
 * Whereas the {@link VersionMixin} mixin pins its op at {@code /version} for composition
 * into a host at {@code /}, this companion declares the mount at the class level
 * ({@link Rest#paths() @Rest(paths="/version/*")}) and pins the op at {@code /*}.
 *
 * <h5 class='figure'>Microservice registration:</h5>
 *
 * <p class='bjava'>
 * 	<ja>@Bean</ja> Servlet version() {
 * 		<jk>return new</jk> VersionServlet();  <jc>// auto-mounted at /version/* by JettyServerComponent</jc>
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link VersionMixin}
 * 	<li class='jc'>{@link VersionProvider}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Composition (mixins, paths)</a>
 * </ul>
 *
 * @serial exclude
 * @since 10.0.0
 */
// @formatter:off
@Rest(paths="/version/*")
public class VersionServlet extends RestServlet {

	private static final long serialVersionUID = 1L;

	private final transient VersionProvider worker;

	/** No-arg constructor &mdash; uses a default {@link VersionProvider} worker. */
	public VersionServlet() {
		this(new VersionProvider());
	}

	/**
	 * Worker constructor.
	 *
	 * @param worker The shared flavor-neutral version worker this servlet delegates to. Must not be
	 * 	{@code null}.
	 */
	protected VersionServlet(VersionProvider worker) {
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
