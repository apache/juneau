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

import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Mixin that serves static files from the active {@link StaticFiles} implementation under
 * {@code /static/*} (configurable).
 *
 * <p>
 * Wraps the existing {@link BasicStaticFiles} plumbing (a {@link StaticFiles} implementation, not a
 * servlet) in a servlet-level mixin so any Juneau resource can opt into static-file serving via
 * {@link Rest#mixins() @Rest(mixins=StaticFilesMixin.class)}. The mixin reads the active
 * {@link StaticFiles} from the bean store at request time &mdash; importer's
 * {@code @Bean StaticFiles} declarations + classpath defaults are picked up automatically.
 *
 * <h5 class='section'>Configurable mount path:</h5>
 *
 * <p>
 * The default mount {@code /static/*} can be overridden via the SVL variable
 * {@code ${juneau.staticfiles.path:static}} &mdash; set via system property
 * ({@code -Djuneau.staticfiles.path=assets}), environment variable
 * ({@code JUNEAU_STATICFILES_PATH=assets}), or {@code Config} key
 * ({@code juneau.staticfiles.path = assets}) to change the runtime mount without subclassing.
 * Resolution happens once at {@link RestContext} construction time (SVL resolution in {@code @RestOp(path)}).
 *
 * <p>
 * Override accepts bare token ({@code static}), leading slash ({@code /static}), trailing slash
 * ({@code static/}), or wildcard suffix ({@code /static/*}) &mdash; all resolve to the same mount.
 *
 * <p>
 * <b>Migration note (9.5.0):</b> Earlier development snapshots of this mixin mounted at both
 * {@code /static/*} <i>and</i> {@code /htdocs/*} as historical convenience aliases &mdash; that
 * dual default has been collapsed to a single SVL-configurable mount as part of the
 * "single path per op" principle. Deployers who relied on the {@code /htdocs/*}
 * alias must now either set {@code -Djuneau.staticfiles.path=htdocs} or compose a second
 * resource mounted with the {@code htdocs} override. The default {@link StaticFiles} classpath
 * search root still walks both {@code static/} and {@code htdocs/} directories &mdash; only the
 * URL-side mount is now single.
 *
 * <h5 class='section'>Mixin-only deployment:</h5>
 *
 * <p>
 * This resource is designed for composition via {@code @Rest(mixins=...)}. The mount path is
 * pinned at the op level by
 * {@link RestGet @RestGet(path="/&#123;juneau.staticfiles.path:static&#125;/*")} on {@link #getStaticFile}
 * (and the matching {@link RestOp} for {@code HEAD}); a class-level {@code @Rest(paths=...)}
 * declaration would be silently ignored under the mixin pattern (see
 * {@link Rest#paths() @Rest(paths)} Javadoc).
 *
 * <h5 class='figure'>Composition example:</h5>
 *
 * <p class='bjava'>
 * 	<jc>// Vanilla resource gains static-file serving at /static/*.</jc>
 * 	<ja>@Rest</ja>(path=<js>"/api"</js>, mixins=StaticFilesMixin.<jk>class</jk>)
 * 	<jk>public class</jk> ApiResource <jk>extends</jk> RestServlet {
 * 		<ja>@RestGet</ja>(<js>"/items"</js>) <jk>public</jk> List&lt;Item&gt; items() { ... }
 * 	}
 * </p>
 *
 * <h5 class='section'>Behavior:</h5>
 *
 * <ul class='spaced-list'>
 * 	<li>GET hits the active {@link StaticFiles} bean (resolved via
 * 		{@link RestContext#getStaticFiles()}); missing paths surface as
 * 		{@link NotFound} thrown from the handler.
 * 	<li>{@code HEAD} requests are accepted via the standard servlet
 * 		{@code HEAD}-via-{@code GET} contract: the response carries identical headers to the
 * 		corresponding {@code GET} (including {@code Cache-Control} and {@code Content-Type}) with
 * 		an empty body.
 * 	<li>{@code Cache-Control} headers come from the {@link BasicStaticFiles} default
 * 		({@code max-age=86400, public}); importers can override via
 * 		{@code BasicStaticFiles.create(beanStore).headers(...)} when registering their own
 * 		{@code @Bean StaticFiles}.
 * 	<li>Default classpath base searches both {@code static/} and {@code htdocs/} directories on
 * 		the importer's classpath (recursive walk via {@link BasicStaticFiles}'s built-in
 * 		{@link org.apache.juneau.cp.ResourceSupplier} hook).
 * </ul>
 *
 * <h5 class='section'>Path matching:</h5>
 *
 * <p>
 * The handler declares {@code @RestGet(path="/${juneau.staticfiles.path:static}/*")} so a single
 * Java method binds to the configured prefix (default {@code /static/*}). The trailing
 * {@code /*} captures the multi-segment remainder via {@code @Path("/*") String path}. Juneau's
 * {@code UrlPathMatcher} does not support the Spring/JAX-RS {@code {var:regex}} syntax &mdash;
 * each {@code {var}} matches a single segment only, and multi-segment matching is only available
 * via the trailing-{@code *} pattern shown here (same idiom as the legacy
 * {@code BasicRestServlet.getHtdoc(...)} accessor).
 * </p>
 *
 * <h5 class='section'>OpenAPI surface:</h5>
 *
 * <p>
 * The greedy {@code /*} handler is not API-meaningful and is excluded from generated Swagger /
 * OpenAPI specs via {@link OpSwagger#ignore() @OpSwagger(ignore=true)}.
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link BasicStaticFiles}
 * 	<li class='jc'>{@link StaticFiles}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/StaticFiles">Static files</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Composition (mixins, paths)</a>
 * </ul>
 *
 * @since 9.5.0
 */
// @formatter:off
@Rest
public class StaticFilesMixin {

	/**
	 * [GET /static/*] &mdash; serve a static file from the active {@link StaticFiles} bean.
	 *
	 * <p>
	 * The {@code @Path("/*") String path} captures the multi-segment trailing remainder. The
	 * {@code Locale} parameter is auto-injected from the request and forwarded to
	 * {@link StaticFiles#resolve(String,Locale) StaticFiles.resolve(...)} so localized lookups
	 * (e.g. {@code styles_fr.css}) work out of the box.
	 *
	 * @param req The current REST request &mdash; supplies {@link RestContext#getStaticFiles()}.
	 * @param path The trailing remainder after the mount prefix (the file path within the
	 * 	{@code static/} or {@code htdocs/} classpath / filesystem search roots).
	 * @param locale The request locale (used for localized resource lookups).
	 * @return The matching {@link HttpResource} (with content type + cache headers).
	 * @throws NotFound If no resource matches the requested path.
	 */
	@RestGet(
		path="/#{pathToken(${juneau.staticfiles.path:static})}/*",
		summary="Static files",
		description="Static file retrieval.",
		swagger=@OpSwagger(ignore=true)
	)
	public HttpResource getStaticFile(RestRequest req, @Path("/*") String path, Locale locale) {
		return req.getContext().getStaticFiles().resolve(path, locale).orElseThrow(NotFound::new);
	}

	/**
	 * [HEAD /static/*] &mdash; return GET headers for a static file without the body.
	 *
	 * <p>
	 * Per RFC 7231 §4.3.2, {@code HEAD} mirrors the equivalent {@code GET}'s headers (Content-Type,
	 * Content-Length, Cache-Control) but emits an empty response body. The shared
	 * {@link org.apache.juneau.rest.processor.HttpResourceProcessor} performs the body suppression
	 * at the response-processor layer based on {@link RestRequest#getMethod()}, so this handler can
	 * delegate to {@link #getStaticFile} verbatim.
	 *
	 * @param req The current REST request &mdash; supplies {@link RestContext#getStaticFiles()}.
	 * @param path The trailing remainder after the mount prefix.
	 * @param locale The request locale (used for localized resource lookups).
	 * @return The matching {@link HttpResource} (with headers; body suppressed by the processor).
	 * @throws NotFound If no resource matches the requested path.
	 */
	@RestOp(
		method="HEAD",
		path="/#{pathToken(${juneau.staticfiles.path:static})}/*",
		summary="Static files (HEAD)",
		description="Static file metadata retrieval.",
		swagger=@OpSwagger(ignore=true)
	)
	public HttpResource headStaticFile(RestRequest req, @Path("/*") String path, Locale locale) {
		return getStaticFile(req, path, locale);
	}
}
