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
package org.apache.juneau.rest.server.view.jsp;

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.*;

import java.io.*;

import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.view.*;

/**
 * Flavor-neutral worker bean carrying the JSP raw-template-dispatch logic shared by the
 * {@link JspMixin} mixin, the {@link JspServlet} servlet, and the {@link JspResource} child flavors.
 *
 * <p>
 * This is the §2.3.1 <i>flavor-neutral worker</i> for the JSP capability: it is <b>not</b> one of the
 * three {@code @Rest} flavor classes &mdash; it is a plain bean that implements
 * {@link RawTemplateDispatcher} and holds the capability state ({@code basePath}) plus the raw-serve
 * logic. Each flavor independently holds an instance of this worker and delegates to it, so no flavor
 * is another flavor's worker.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link JspMixin}
 * 	<li class='jc'>{@link JspServlet}
 * 	<li class='jc'>{@link JspResource}
 * 	<li class='jic'>{@link RawTemplateDispatcher}
 * </ul>
 *
 * @since 10.0.0
 */
public class JspDispatcher implements RawTemplateDispatcher {

	/** Default base path applied when no {@link Builder#basePath(String)} call has been made. */
	public static final String DEFAULT_BASE_PATH = "/";

	private final String basePath;

	/**
	 * Creates a new builder.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Builder constructor.
	 *
	 * @param builder The builder. Must not be {@code null}.
	 */
	protected JspDispatcher(Builder builder) {
		basePath = builder.basePath;
	}

	/**
	 * Returns the base path under which {@code .jsp} resources are resolved.
	 *
	 * @return The base path. Never {@code null}.
	 */
	public String getBasePath() {
		return basePath;
	}

	/**
	 * Forwards the request to the JSP engine for the raw {@code .jsp} resource at the trailing path.
	 *
	 * @param path The trailing path segment after the mount prefix (the JSP file name relative to
	 * 	the configured {@link #getBasePath() base path}).
	 * @param req The current REST request.
	 * @param res The current REST response.
	 * @throws IOException If the underlying servlet writer fails.
	 * @throws NotFound If the JSP resource cannot be resolved.
	 */
	@Override /* RawTemplateDispatcher */
	public void render(String path, RestRequest req, RestResponse res) throws IOException, NotFound {
		// joinPath delegates to FileUtils.resolveVirtualPathSafely, which throws IAE on any
		// resolved target that escapes basePath (../, %2e%2e treated as literal, absolute-path
		// injection, etc.). Map IAE → 403 (Forbidden); attackers can't distinguish from the
		// container-layer rejection of malformed paths.
		String target;
		try {
			target = JspViewRenderer.joinPath(basePath, path);
		} catch (IllegalArgumentException ex) {
			throw new Forbidden("Path escapes configured base path.");
		}
		try {
			var ctx = req.getServletContext();
			var rd = ctx.getRequestDispatcher(target);
			if (rd == null)
				throw new InternalServerError("Could not resolve RequestDispatcher for '%s'. %s",
					target, JspViewRenderer.NO_ENGINE_DIAGNOSTIC);
			rd.forward(req.getHttpServletRequest(), res.getHttpServletResponse());
		} catch (NoClassDefFoundError ex) {
			throw new InternalServerError(ex, JspViewRenderer.NO_ENGINE_DIAGNOSTIC);
		} catch (IOException | NotFound ex) {
			throw ex;
		} catch (Exception ex) {
			throw new InternalServerError(ex, "JSP render failed for '%s'", target);
		}
	}

	/**
	 * Builder for {@link JspDispatcher}.
	 */
	public static class Builder {

		String basePath = DEFAULT_BASE_PATH;

		/** Constructor &mdash; package access for {@link JspDispatcher#create()}. */
		protected Builder() {}

		/**
		 * Sets the classpath / webapp base path under which {@code .jsp} resources are resolved.
		 *
		 * @param value The base path. {@code null} or blank values reset to the default
		 * 	{@link JspDispatcher#DEFAULT_BASE_PATH}.
		 * @return This object.
		 */
		public Builder basePath(String value) {
			basePath = isBlank(value) ? DEFAULT_BASE_PATH : value;
			return this;
		}

		/**
		 * Reads the current base path setting (test/inspection helper).
		 *
		 * @return The base path. Never {@code null}.
		 */
		public String getBasePath() {
			return basePath;
		}

		/**
		 * Builds the {@link JspDispatcher}.
		 *
		 * @return A new {@link JspDispatcher} instance.
		 */
		public JspDispatcher build() {
			if (basePath == null)
				throw iaex("basePath must not be null");
			return new JspDispatcher(this);
		}
	}
}
