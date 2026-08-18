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

package org.apache.juneau.releng.rest;

import org.apache.juneau.rest.server.filter.LoopbackBoundaryFilter;
import org.apache.juneau.rest.server.view.freemarker.FreemarkerView;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Starts every console page's view with the CSRF token {@code base.ftlh} embeds.
 *
 * <p>The token is read from the request attribute {@link LoopbackBoundaryFilter} sets on requests it allowed
 * through, rather than from an injected bean, so that a page can only carry a token if the boundary is actually
 * installed in front of it. Wiring the two together this way means "we serve tokens" and "we check tokens" cannot
 * drift apart into the worst combination — a page that hands out a token nothing validates.
 *
 * <p>Forgetting to route a new page through here is safe rather than dangerous, and loud rather than quiet:
 * {@code base.ftlh} references {@code ${csrfToken}} with no FreeMarker default, so the page fails to render at
 * once instead of silently shipping without a token. Even if it did render, its writes would be refused by the
 * filter. The security decision belongs to the filter; this class only supplies the UI half.
 */
final class ConsolePage {

	private ConsolePage() {}

	/**
	 * The named template, seeded with {@code csrfToken}.
	 *
	 * <p>When the attribute is absent — no boundary filter ran in front of this request, as in a unit test that
	 * dispatches straight at the resource — the token renders empty rather than failing the render. An empty
	 * token is never less safe than no token: the only thing that reads it is the boundary's check, which
	 * refuses an empty value like any other wrong one. If no boundary is installed, there is nothing for a token
	 * to have protected in the first place. The page-side complaint lives in {@code csrf.js}, which logs when it
	 * finds the meta tag empty, so the condition is still visible where somebody would notice it.
	 *
	 * @param template The template name, relative to the configured base path.
	 * @param req The current request, carrying the boundary's token attribute.
	 * @return A view the caller adds its own page attributes to.
	 */
	static FreemarkerView of(String template, HttpServletRequest req) {
		var token = req.getAttribute(LoopbackBoundaryFilter.TOKEN_ATTRIBUTE);
		return FreemarkerView.of(template).attr("csrfToken", token == null ? "" : token);
	}
}
