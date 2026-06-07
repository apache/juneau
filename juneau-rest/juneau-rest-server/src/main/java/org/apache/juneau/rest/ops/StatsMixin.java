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

import org.apache.juneau.html.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.servlet.*;
import org.apache.juneau.rest.stats.*;

/**
 * Mixin providing the residual {@code [GET /stats]} timing-statistics endpoint.
 *
 * <p>
 * Single-responsibility op-mixin carved out of the former {@code BasicRestOperations} interface. Carries
 * the per-method {@link HtmlDocConfig @HtmlDocConfig} that customizes the {@code /stats} HTML page.
 * The per-method config travels with the op, while the host-wide navlinks/JSON-schema config
 * lives at the class level on {@link BasicRestServlet}.
 *
 * @since 10.0.0
 */
@Rest
public class StatsMixin extends RestMixin {

	/**
	 * [GET /stats] - Timing statistics.
	 *
	 * <p>
	 * Timing statistics for method invocations on this resource.
	 *
	 * @param req The HTTP request.
	 * @return A collection of timing statistics for each annotated method on this resource.
	 */
	@RestGet(
		path="/stats",
		summary="Timing statistics",
		description="Timing statistics for method invocations on this resource."
	)
	@HtmlDocConfig(
		// Should override config annotations defined on class.
		rank=10,
		// Override the nav links for the swagger page.
		navlinks = { "back: servlet:/", "json: servlet:/stats?Accept=text/json&plainText=true" },
		// Never show aside contents of page inherited from class.
		aside="NONE"
	)
	public RestContextStats getStats(RestRequest req) {
		return req.getContext().getStats();
	}
}
