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
package org.apache.juneau.rest.server.staticfile;

import java.util.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;

/**
 * Child-resource flavor of the static-files capability.
 *
 * <p>
 * Mounts as a <b>routed child</b> via {@link Rest#children() @Rest(children=StaticFilesResource.class)}
 * under a parent at the subtree {@code /static} and serves static files by delegating to the
 * flavor-neutral {@link StaticFiles} worker bean resolved from the request context
 * ({@link RestContext#getStaticFiles()}) &mdash; the same worker the {@link StaticFilesMixin mixin} and
 * {@link StaticFilesServlet servlet} flavors use, so the three forms cannot drift.
 *
 * <p>
 * Whereas the {@link StaticFilesMixin} mixin pins its op at {@code /static/*} to merge into a host's
 * namespace, this child declares the subtree at the class level ({@link Rest#path() @Rest(path="/static")})
 * and pins the ops at {@code /*} &mdash; the op must <b>not</b> re-pin {@code /static/*} or it would
 * double-path to {@code /static/static/*} under the routed-subtree wiring.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link StaticFilesMixin}
 * 	<li class='jc'>{@link StaticFilesServlet}
 * 	<li class='jc'>{@link StaticFiles}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/StaticFiles">Static files</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Mixins and Multi-Mount Paths</a>
 * </ul>
 *
 * @since 10.0.0
 */
// @formatter:off
@Rest(path="/static")
public class StaticFilesResource extends RestResource {

	/**
	 * [GET /*] &mdash; serve a static file from the active {@link StaticFiles} worker bean.
	 *
	 * @param req The current REST request &mdash; supplies {@link RestContext#getStaticFiles()}.
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
		return req.getContext().getStaticFiles().resolve(path, locale).orElseThrow(NotFound::new);
	}

	/**
	 * [HEAD /*] &mdash; return GET headers for a static file without the body.
	 *
	 * @param req The current REST request &mdash; supplies {@link RestContext#getStaticFiles()}.
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
		return getStaticFile(req, path, locale);
	}
}
