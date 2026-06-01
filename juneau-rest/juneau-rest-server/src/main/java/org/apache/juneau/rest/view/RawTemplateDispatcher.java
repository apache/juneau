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

import org.apache.juneau.rest.*;

/**
 * Engine-agnostic raw-template-dispatch contract shared by the view mixin resources and their
 * standalone servlet companions.
 *
 * <p>
 * Not to be confused with {@link ViewRenderer} &mdash; that is the response-processor marker that
 * renders {@link View}-typed {@code @RestOp} return values. This contract instead covers the
 * <i>raw</i> file-serving path: serving a {@code .jsp} / {@code .ftlh} / {@code .mustache} /
 * {@code .html} template directly by its trailing request path.
 *
 * <p>
 * The per-engine mixin resources ({@code JspMixin}, {@code FreemarkerMixin},
 * {@code MustacheMixin}, {@code ThymeleafMixin}) implement this interface so the
 * raw dispatch they already perform can be reused verbatim by their {@code Basic*Servlet} standalone
 * companions through {@link ViewServlet}. Sharing a single implementation means the standalone
 * and mixin forms cannot drift in their file-serving behavior.
 *
 * <p>
 * The single {@link #render(String, RestRequest, RestResponse) render(...)} method receives the
 * trailing request path (the multi-segment remainder captured by the host's {@code @Path("/*")}
 * binding), resolves it against the implementation's configured base path, and writes the rendered
 * output directly onto the response.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link ViewServlet}
 * 	<li class='jic'>{@link ViewRenderer}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Composition (mixins, paths)</a>
 * </ul>
 *
 * @since 9.5.0
 */
public interface RawTemplateDispatcher {

	/**
	 * Renders the raw template identified by the trailing request path onto the response.
	 *
	 * <p>
	 * Implementations resolve {@code path} against their configured base path (rejecting any
	 * {@code ..} traversal), ask the underlying templating engine for the resource, and write it
	 * to the response writer.  Boundary violations and engine errors surface as
	 * {@link org.apache.juneau.http.response.BasicHttpException BasicHttpException} subtypes
	 * (unchecked), so this method only declares the checked {@link IOException}.
	 *
	 * @param path The trailing path segment after the mount prefix (the template / file name
	 * 	relative to the configured base path). May be {@code null} (treated as empty).
	 * @param req The current REST request.
	 * @param res The current REST response.
	 * @throws IOException If the underlying response writer fails.
	 */
	void render(String path, RestRequest req, RestResponse res) throws IOException;
}
