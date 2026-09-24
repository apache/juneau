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

import org.apache.juneau.commons.inject.Bean;
import org.apache.juneau.rest.server.Rest;
import org.apache.juneau.rest.server.console.ConsoleChromeMixin;
import org.apache.juneau.rest.server.servlet.BasicRestServlet;

/**
 * Serves the shared console-ui chrome stylesheet and its stock theme packs at the server root, independent of
 * the app's own {@code /rest/*} mount, so the {@code chrome.css} and theme-pack {@code <link>}s that
 * {@code <@console>} emits into every tab's document head resolve the same way regardless of which tab rendered
 * the page.
 *
 * <p>
 * Registered directly (not composed onto {@code HomeRest}/{@code ReleaseRest}/etc.) via a dedicated
 * {@code ServletRegistrationBean} in {@link AppConfiguration} at {@code /juneau-console/*} &mdash; see there for
 * why.
 *
 * <p>
 * {@code ConsoleChromeMixin} supports this standalone-mount arrangement directly &mdash; each of its endpoints
 * matches both its {@code /juneau-console}-prefixed path and the unprefixed remainder the container leaves
 * behind once it has consumed {@code /juneau-console} as the servlet path &mdash; so no servlet-path
 * rewriting is needed here.
 *
 * <p>
 * The mixin is stock: the app's chrome (theme selection, the oakleaf mark, the danger-pill token overrides, and
 * the footer copy) is authored declaratively in {@code base.ftlh}'s {@code <@console>} shell rather than
 * configured on the builder here.
 */
@Rest(mixins=ConsoleChromeMixin.class)
public class ConsoleAssetsRest extends BasicRestServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * The stock {@link ConsoleChromeMixin}; the app's chrome is authored declaratively in {@code base.ftlh} instead.
	 */
	@Bean
	public ConsoleChromeMixin consoleChrome() {
		return ConsoleChromeMixin.create().build();
	}
}
