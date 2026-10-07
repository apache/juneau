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
import static org.apache.juneau.test.bct.BctAssertions.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.petstore.console.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

/**
 * P13: the open Secure page and the bearer-guarded API under it.
 */
@SuppressWarnings({
	"resource" // MockRestClient is a no-op close.
})
class SecureRest_Test extends TestBase {

	private static final String API = "/console/dev/secure/api";

	private static MockRestClient json() {
		return MockRestClient.create(PetstoreConsoleFixture.Host.class).noTrace().ignoreErrors().header("Accept", "application/json").build();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a - page
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_pageIsOpenAndRendersTheDemo() throws Exception {
		var html = page(client(), "/console/dev/secure");
		assertPage(html).isValid().hasActiveNav("dev", "secure").hasCardOrder("about", "try");
		assertContainsAll(html,
			"data-secure-call=\"none\"", "data-secure-call=\"petstore-user\"", "data-secure-call=\"wrong-token\"",
			"id=\"secure-result\"", "/console/dev/secure/secure.js", "/console/dev/secure/api/whoami");
	}

	@Test void a02_scriptIsServed() throws Exception {
		client().get("/console/dev/secure/secure.js").run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("javascript")
			.assertContent().isContains("data-secure-call", "Authorization");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b - guarded API
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_noToken401WithChallenge() throws Exception {
		json().get(API + "/pets").run()
			.assertStatus(401)
			.assertHeader("WWW-Authenticate").isContains("Bearer", "realm=\"petstore\"");
	}

	@Test void b02_demoToken200() throws Exception {
		json().get(API + "/pets").header("Authorization", "Bearer petstore-user").run()
			.assertStatus(200).assertContent().isContains("Mr. Frisky");
	}

	@Test void b03_wrongToken401() throws Exception {
		json().get(API + "/pets").header("Authorization", "Bearer wrong-token").run().assertStatus(401);
	}

	@Test void b04_whoamiNamesThePrincipal() throws Exception {
		var r = new JsonMap(json().get(API + "/whoami").header("Authorization", "Bearer petstore-admin").run()
			.assertStatus(200).getContent().asString());
		assertBean(r, "name", "admin");
	}

	@Test void b05_petByIdAndUnknown404() throws Exception {
		var c = json();
		assertBean(new JsonMap(c.get(API + "/pets/1").header("Authorization", "Bearer petstore-user").run()
			.assertStatus(200).getContent().asString()), "id,name", "1,Mr. Frisky");
		c.get(API + "/pets/99999").header("Authorization", "Bearer petstore-user").run()
			.assertStatus(404).assertContent().isContains("Unknown pet '99999'");
	}

	@Test void b06_unguardedSiblingsStayOpen() throws Exception {
		json().get("/petstore/pets/1").run().assertStatus(200);
		json().get("/console/dev/themes").accept("text/html").run().assertStatus(200);
	}
}
