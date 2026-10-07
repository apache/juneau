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
package org.apache.juneau.petstore.console;

import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.console.*;
import org.apache.juneau.rest.server.servlet.*;

/**
 * Serves the console chrome assets (the structural {@code chrome.css}, the stock theme packs and
 * {@code juneau-console.js}) at the context-root {@code /juneau-console/*} URLs that every console page links.
 *
 * <p>
 * The console directive resolves those asset URLs against the context root, not against the page's own mount, so
 * a host that mounts {@link PetstoreConsoleResource} must also mount this resource as a sibling child for the pages
 * to be styled and for the console shell to start in a browser.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(children={PetstoreConsoleResource.<jk>class</jk>, ConsoleAssetsRest.<jk>class</jk>})
 * 	<jk>public class</jk> RootResources <jk>extends</jk> BasicRestServletGroup {}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link ConsoleChromeMixin}
 * </ul>
 *
 * @serial exclude
 */
@Rest(path="/juneau-console", mixins=ConsoleChromeMixin.class)
public class ConsoleAssetsRest extends BasicRestServlet {

	private static final long serialVersionUID = 1L;
}
