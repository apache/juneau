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
package org.apache.juneau.petstore.rest;

import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.staticfile.*;

/**
 * Petstore React SPA demo — serves the no-build single-page React UI from the core classpath.
 *
 * <p>
 * Composes {@link StaticFilesMixin} to serve {@code /static/*} from the importer's classpath: the file
 * {@code static/petstore-ui.html} is reachable at {@code GET /petstore-ui/static/petstore-ui.html}.  The HTML
 * loads React + Babel and uses {@code fetch()} against {@code /petstore/pets} to demonstrate the headless-JSON +
 * decoupled-SPA pattern alongside the server-rendered HTML-doc UI.
 *
 * <p>
 * Lives in {@code juneau-petstore-core} so both the Jetty and Spring Boot deployments inherit it for free.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauPetstore">juneau-petstore</a>
 * </ul>
 */
@Rest(
	path="/petstore-ui",
	title="Petstore — React SPA demo",
	description="No-build single-page React UI loaded as static classpath resource; calls /petstore/* over fetch().",
	mixins=StaticFilesMixin.class
)
@SuppressWarnings({
	"java:S110" // Inheritance depth acceptable for example/demo code
})
public class PetstoreUiResource extends BasicRestServlet {

	private static final long serialVersionUID = 1L;
}
