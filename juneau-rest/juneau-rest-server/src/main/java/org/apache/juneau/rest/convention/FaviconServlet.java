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

import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.servlet.*;

/**
 * Standalone servlet companion of the {@link FaviconMixin} mixin.
 *
 * <p>
 * Mounts as a <b>sibling top-level servlet</b> at {@code /favicon.ico} and serves the configured favicon
 * bytes by delegating to a shared {@link FaviconMixin} instance &mdash; the same logic the mixin uses,
 * so the two forms cannot drift. Browsers fetch {@code /favicon.ico} from the site root, so a standalone
 * servlet at that fixed path is the natural deployment for sites that do not compose the mixin into an
 * existing host resource.
 *
 * <h5 class='figure'>Microservice registration:</h5>
 *
 * <p class='bjava'>
 * 	<ja>@Bean</ja> Servlet favicon() {
 * 		<jk>return new</jk> FaviconServlet();  <jc>// auto-mounted at /favicon.ico</jc>
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link FaviconMixin}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Composition (mixins, paths)</a>
 * </ul>
 *
 * @serial exclude
 * @since 9.5.0
 */
// @formatter:off
@Rest(paths="/favicon.ico")
public class FaviconServlet extends RestServlet {

	private static final long serialVersionUID = 1L;

	private final transient FaviconMixin delegate;

	/** No-arg constructor &mdash; uses a default {@link FaviconMixin} delegate. */
	public FaviconServlet() {
		this(new FaviconMixin());
	}

	/**
	 * Delegate constructor.
	 *
	 * @param delegate The shared favicon mixin this servlet delegates to. Must not be {@code null}.
	 */
	protected FaviconServlet(FaviconMixin delegate) {
		this.delegate = delegate;
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
		return delegate.getFavicon();
	}
}
