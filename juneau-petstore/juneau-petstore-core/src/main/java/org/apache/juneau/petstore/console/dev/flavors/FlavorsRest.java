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
package org.apache.juneau.petstore.console.dev.flavors;

import org.apache.juneau.http.response.*;
import org.apache.juneau.petstore.console.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.view.*;

/**
 * Rendering flavors: the same pet list rendered by each of the petstore's view technologies, inside the console.
 *
 * <p>
 * Html, Freemarker and Mustache are child resources, each serving its page at {@code /} and a chrome-less fragment
 * at {@code /fragment} that the page's {@code html} card loads with {@code src=}.  React is the standalone app at
 * {@code /petstore-ui}, embedded in an iframe.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	&lt;@card id="flavor" title="Pets" src="/console/dev/flavors/mustache/fragment"/&gt;
 * </p>
 */
@Rest(
	path="/flavors",
	title="Rendering flavors",
	children={HtmlFlavorRest.class, FreemarkerFlavorRest.class, MustacheFlavorRest.class}
)
@SuppressWarnings({
	"java:S110" // Inheritance depth comes from the BasicRestServlet hierarchy, not this page.
})
public class FlavorsRest extends PetstoreConsolePage {

	private static final long serialVersionUID = 1L;

	/**
	 * Redirects to the first flavor.
	 *
	 * @param req The request.
	 * @return A 303 to {@code html}.
	 */
	@RestGet(path="/")
	public SeeOther index(RestRequest req) {
		return new SeeOther().setLocation(req.getUriResolver().resolve("servlet:/html"));
	}

	/** @return The React flavor page. */
	@RestGet(path="/react")
	public View react() {
		return Flavor.REACT.page();
	}
}
