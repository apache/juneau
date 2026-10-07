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
package org.apache.juneau.petstore.console.dev;

import static org.apache.juneau.petstore.console.PetstoreConsoleFixture.*;
import static org.apache.juneau.rest.server.console.test.PageContractAssert.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * The Developer and About nav nodes exist on every page, and their routes resolve.
 */
@SuppressWarnings({
	"resource" // MockRestClient is a no-op close.
})
class DevNav_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// a - nav tree
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_developerNodeAndChildren() throws Exception {
		assertPage(page(client(), "/console/store"))
			.hasNavPath("dev")
			.hasNavChildren("dev", "cards", "themes", "flavors", "secure")
			.hasNavChildren("dev/flavors", "html", "freemarker", "mustache", "react")
			.hasNavHref("dev", "/console/dev/cards")
			.hasNavHref("dev/flavors", "/console/dev/flavors/html")
			.hasNavHref("dev/flavors/react", "/console/dev/flavors/react");
	}

	@Test void a02_aboutNodeExists() throws Exception {
		assertPage(page(client(), "/console/store"))
			.hasNavPath("about")
			.hasNavHref("about", "/console/about");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b - routes
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_devRootRedirectsToFirstChild() throws Exception {
		rawClient().get("/console/dev").run().assertStatus(303).assertHeader("Location").isMatches("*/console/dev/cards");
	}

	@Test void b02_levelThreePageActivatesFullPath() throws Exception {
		assertPage(page(client(), "/console/dev/flavors/mustache")).isValid().hasActiveNav("dev", "flavors", "mustache");
	}

	@Test void b03_unknownFlavor404() throws Exception {
		client().get("/console/dev/flavors/velocity").accept("text/html").run().assertStatus(404);
	}
}
