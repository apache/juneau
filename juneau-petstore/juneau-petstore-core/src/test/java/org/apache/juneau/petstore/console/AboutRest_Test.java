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

import static org.apache.juneau.petstore.console.PetstoreConsoleFixture.*;
import static org.apache.juneau.rest.server.console.test.PageContractAssert.*;
import static org.apache.juneau.test.bct.BctAssertions.*;

import org.apache.juneau.*;
import org.apache.juneau.petstore.dto.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

/**
 * The About page and the utility-bean demos that used to be a standalone resource.
 */
@SuppressWarnings({
	"resource" // MockRestClient is a no-op close.
})
class AboutRest_Test extends TestBase {

	private static MockRestClient json() {
		return MockRestClient.create(PetstoreConsoleFixture.Host.class).noTrace().ignoreErrors().header("Accept", "application/json").build();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a - page
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_pageRendersProseCardsAndLinks() throws Exception {
		var html = page(client(), "/console/about");
		assertPage(html).isValid().hasActiveNav("about").hasCardOrder("overview", "links", "beans");
		assertContainsAll(html, "jc-prose", "href=\"/petstore/api\"", "href=\"/petstore-ui\"",
			"href=\"https://juneau.apache.org/docs/topics/JuneauPetstore\"",
			"href=\"/console/about/beans/BeanDescription\"", "href=\"/console/about/beans/Hyperlink\"", "href=\"/console/about/beans/SeeOtherRoot\"");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b - utility beans
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_beansListsAllThree() throws Exception {
		assertContainsAll(json().get("/console/about/beans").run().assertStatus(200).getContent().asString(),
			"BeanDescription", "Hyperlink", "SeeOtherRoot");
		assertContainsAll(client().get("/console/about/beans").accept("text/html").run().assertStatus(200).getContent().asString(),
			"/console/about/beans/BeanDescription", "/console/about/beans/Hyperlink", "/console/about/beans/SeeOtherRoot");
	}

	@Test void b02_beanDescriptionDescribesPet() throws Exception {
		assertContainsAll(json().get("/console/about/beans/BeanDescription").run().assertStatus(200).getContent().asString(),
			Pet.class.getName(), "species", "photo");
	}

	@Test void b03_hyperlinkPointsBackToAbout() throws Exception {
		assertContains("/console/about", json().get("/console/about/beans/Hyperlink").run().assertStatus(200).getContent().asString());
	}

	@Test void b04_seeOtherRootRedirects() throws Exception {
		rawClient().get("/console/about/beans/SeeOtherRoot").run().assertStatus(303);
	}

	@Test void b05_apiLinkIsTheSwaggerPage() throws Exception {
		client().get("/petstore/api").accept("text/html").run().assertStatus(200);
	}
}
