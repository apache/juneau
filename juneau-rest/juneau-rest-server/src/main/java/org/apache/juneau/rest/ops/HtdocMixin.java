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
package org.apache.juneau.rest.ops;

import java.util.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.servlet.*;

/**
 * Mixin providing the residual {@code [GET /htdocs/*]} static-file endpoint.
 *
 * <p>
 * Single-responsibility op-mixin carved out of the former {@code BasicRestOperations} interface. Resolves
 * the requested path against the active {@link org.apache.juneau.rest.staticfile.StaticFiles} bean via
 * {@link RestContext#getStaticFiles()}.
 *
 * <p>
 * The greedy {@code /htdocs/*} blob handler is not API-meaningful and is excluded from the published
 * Swagger/OpenAPI specification via {@link OpSwagger#ignore() @OpSwagger(ignore=true)}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link org.apache.juneau.rest.staticfile.StaticFilesMixin}
 * </ul>
 *
 * @since 9.5.0
 */
@Rest
public class HtdocMixin extends RestMixin {

	/**
	 * [GET /htdocs/*] - Retrieve static file.
	 *
	 * @param req The HTTP request.
	 * @param path The path to retrieve.
	 * @param locale The locale of the HTTP request.
	 * @return An HTTP resource representing the static file.
	 * @throws NotFound If no resource matches the requested path.
	 */
	@RestGet(
		path="/htdocs/*",
		summary="Static files",
		description="Static file retrieval.",
		swagger=@OpSwagger(ignore=true)
	)
	public HttpResource getHtdoc(RestRequest req, @Path("/*") String path, Locale locale) throws NotFound {
		return req.getContext().getStaticFiles().resolve(path, locale).orElseThrow(NotFound::new);
	}
}
