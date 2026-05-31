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
package org.apache.juneau.rest.staticfile;

import java.util.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.servlet.*;

/**
 * Standalone servlet companion of the {@link BasicStaticFilesResource} mixin.
 *
 * <p>
 * Mounts as a <b>sibling top-level servlet</b> at {@code /static/*} and serves static files from the
 * active {@link StaticFiles} bean by delegating to a shared {@link BasicStaticFilesResource}
 * instance &mdash; the same logic the mixin uses, so the two forms cannot drift.
 *
 * <p>
 * Whereas the {@link BasicStaticFilesResource} mixin pins its op at {@code /static/*} for composition
 * into a host at {@code /}, this companion declares the mount at the class level
 * ({@link Rest#paths() @Rest(paths="/static/*")}) and pins the ops at {@code /*} &mdash; eliminating
 * the path-doubling that a mixin-shaped op would produce when the servlet itself is mounted at
 * {@code /static/*}.
 *
 * <h5 class='figure'>Microservice registration:</h5>
 *
 * <p class='bjava'>
 * 	<ja>@Bean</ja> Servlet staticFiles() {
 * 		<jk>return new</jk> BasicStaticFilesServlet();  <jc>// auto-mounted at /static/* by JettyServerComponent</jc>
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link BasicStaticFilesResource}
 * 	<li class='jc'>{@link StaticFiles}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/StaticFiles">Static files</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Composition (mixins, paths)</a>
 * </ul>
 *
 * @serial exclude
 * @since 9.5.0
 */
// @formatter:off
@Rest(paths="/static/*")
public class BasicStaticFilesServlet extends RestServlet {

	private static final long serialVersionUID = 1L;

	private final transient BasicStaticFilesResource delegate;

	/** No-arg constructor &mdash; uses a default {@link BasicStaticFilesResource} delegate. */
	public BasicStaticFilesServlet() {
		this(new BasicStaticFilesResource());
	}

	/**
	 * Delegate constructor.
	 *
	 * @param delegate The shared static-files mixin this servlet delegates to. Must not be
	 * 	{@code null}.
	 */
	protected BasicStaticFilesServlet(BasicStaticFilesResource delegate) {
		this.delegate = delegate;
	}

	/**
	 * [GET /*] &mdash; serve a static file from the active {@link StaticFiles} bean.
	 *
	 * @param req The current REST request.
	 * @param path The trailing remainder after the mount prefix.
	 * @param locale The request locale (used for localized resource lookups).
	 * @return The matching {@link HttpResource} (with content type + cache headers).
	 * @throws NotFound If no resource matches the requested path.
	 */
	@RestGet(
		path="/*",
		summary="Static files",
		description="Static file retrieval.",
		swagger=@OpSwagger(ignore=true)
	)
	public HttpResource getStaticFile(RestRequest req, @Path("/*") String path, Locale locale) {
		return delegate.getStaticFile(req, path, locale);
	}

	/**
	 * [HEAD /*] &mdash; return GET headers for a static file without the body.
	 *
	 * @param req The current REST request.
	 * @param path The trailing remainder after the mount prefix.
	 * @param locale The request locale (used for localized resource lookups).
	 * @return The matching {@link HttpResource} (with headers; body suppressed by the processor).
	 * @throws NotFound If no resource matches the requested path.
	 */
	@RestOp(
		method="HEAD",
		path="/*",
		summary="Static files (HEAD)",
		description="Static file metadata retrieval.",
		swagger=@OpSwagger(ignore=true)
	)
	public HttpResource headStaticFile(RestRequest req, @Path("/*") String path, Locale locale) {
		return delegate.headStaticFile(req, path, locale);
	}
}
