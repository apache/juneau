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
package org.apache.juneau.releng;

import java.io.IOException;

import org.apache.juneau.commons.inject.Bean;
import org.apache.juneau.rest.server.Rest;
import org.apache.juneau.rest.server.console.ConsoleChromeMixin;
import org.apache.juneau.rest.server.servlet.BasicRestServlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Serves the shared console-ui chrome stylesheet and its two themeable assets (logo, page-background image) at
 * the server root, independent of the app's own {@code /rest/*} mount, so the {@code <link>} reference in every
 * tab's {@code base.ftlh} resolves the same way regardless of which tab rendered the page.
 *
 * <p>
 * Registered directly (not composed onto {@code HomeRest}/{@code ReleaseRest}/etc.) via a dedicated
 * {@code ServletRegistrationBean} in {@link AppConfiguration} at {@code /juneau-console/*} &mdash; see there for
 * why.
 *
 * <h5 class='section'>Servlet-path double-consumption fix:</h5>
 * <p>
 * {@code ConsoleChromeMixin}'s {@code @RestGet} paths are absolute-looking literals baked into the shipped jar
 * ({@code ConsoleChromeMixin#CHROME_CSS_PATH} et al., e.g. {@code "/juneau-console/chrome.css"}). Juneau's
 * request dispatch resolves an operation's path against {@code getContextPath() + getServletPath()} subtracted
 * from the request URI &mdash; <b>not</b> against the servlet container's {@code getPathInfo()}. Mounting this
 * servlet at the container url-pattern {@code "/juneau-console/*"} (mirroring the app's other
 * {@code ServletRegistrationBean} mounts, e.g. {@code NexusMockRest} at {@code /mock/nexus/*}) makes the
 * container report {@code servletPath="/juneau-console"}, leaving only {@code "/chrome.css"} once that prefix is
 * subtracted &mdash; one copy of the {@code /juneau-console} segment short of what the mixin's hardcoded path
 * expects, so the real endpoint would only resolve at the doubled
 * {@code /juneau-console/juneau-console/chrome.css}. {@link #service} corrects this at the servlet boundary by
 * wrapping every request so {@code getServletPath()} reports an empty string, which makes Juneau compute the
 * same path it would if this servlet were mounted at the site root &mdash; without actually claiming the site
 * root (and disrupting the app's other routes: Spring MVC static resources, {@code /rest/*},
 * {@code /mock/nexus/*}, {@code /events/*}), since the container's own {@code /juneau-console/*} url-pattern
 * still gates which requests even reach this servlet.
 */
@Rest(mixins=ConsoleChromeMixin.class)
public class ConsoleAssetsRest extends BasicRestServlet {
	private static final long serialVersionUID = 1L;

	@Bean
	public ConsoleChromeMixin consoleChrome() {
		return ConsoleChromeMixin.create()
			.theme(ReleaseManagerTheme.INSTANCE)
			.logo("/static/img/oakleaf.svg")
			.pageBackgroundImage("/static/img/topo-bg.png")
			.build();
	}

	@Override /* Overridden from HttpServlet */
	public void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		super.service(new HttpServletRequestWrapper(req) {
			@Override public String getServletPath() { return ""; }
		}, res);
	}
}
