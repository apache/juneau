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
package org.apache.juneau.microservice.examples;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.config.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link ConfigResource} covering the GET and PUT endpoints via {@link MockRestClient}.
 */
@SuppressWarnings({
	"resource" // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
})
class ConfigResource_Test extends TestBase {

	// Tests intentionally leave resources open; try-with-resources would obscure the test intent.
	private static MockRestClient buildClient() {
		var cfg = Config.create().memStore().build();
		cfg.set("Section1/key1", "val1");
		cfg.set("Section1/key2", "123");
		cfg.set("Section2/keyA", "valA");
		var overlay = new BasicBeanStore().addBean(Config.class, cfg);
		return MockRestClient.create(ConfigResource.class)
			.overridingBeanStore(overlay)
			.disableRedirectHandling()
			.ignoreErrors()
			.build();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// GET endpoints
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_getConfig_returnsFullMap() throws Exception {
		try (var c = buildClient()) {
			var resp = c.get("/").run().assertStatus(200);
			var body = resp.getContent().asString();
			assertTrue(body.contains("Section1"), "Body should contain Section1");
			assertTrue(body.contains("Section2"), "Body should contain Section2");
			assertTrue(body.contains("val1"), "Body should contain val1");
		}
	}

	@Test void a02_getConfigSection_existing() throws Exception {
		try (var c = buildClient()) {
			var resp = c.get("/Section1").run().assertStatus(200);
			var body = resp.getContent().asString();
			assertTrue(body.contains("key1"), "Body should contain key1");
			assertTrue(body.contains("val1"), "Body should contain val1");
		}
	}
	@Test void a03_getConfigSection_missingReturns404() throws Exception {
		try (var c = buildClient()) {
			c.get("/NoSuchSection").run().assertStatus(404);
		}
	}
	@Test void a04_getConfigEntry_existing() throws Exception {
		try (var c = buildClient()) {
			c.get("/Section1/key1").run()
				.assertStatus(200)
				.assertContent().isContains("val1");
		}
	}
	@Test void a05_getConfigEntry_missingSectionReturns404() throws Exception {
		try (var c = buildClient()) {
			c.get("/NoSuchSection/key1").run().assertStatus(404);
		}
	}

	@Test void a06_editForm_isGone() throws Exception {
		// "/edit" is now just a section name, and no such section exists.
		try (var c = buildClient()) {
			var resp = c.get("/edit").run().assertStatus(404);
			assertFalse(resp.getContent().asString().contains("<form"), "No edit form may be served");
		}
	}

	@Test void a07_listing_hasNoEditLink() throws Exception {
		try (var c = buildClient()) {
			var body = c.get("/").accept("text/html").run().assertStatus(200).getContent().asString();
			assertFalse(body.contains("servlet:/edit") || body.contains("/edit\""), () -> "Page must not link to an edit form: " + body);
			assertFalse(body.contains("<form"), () -> "Page must not carry a form: " + body);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// PUT endpoints
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_setConfigValue_putEntry() throws Exception {
		try (var c = buildClient()) {
			c.put("/Section1/key1", "newVal").run()
				.assertStatus(200)
				.assertContent().isContains("newVal");
		}
	}

	@Test void b02_setConfigSection_putSection() throws Exception {
		try (var c = buildClient()) {
			var resp = c.put("/Section3", "{newKey:'newValue'}").run().assertStatus(200);
			var body = resp.getContent().asString();
			assertTrue(body.contains("newKey"), "Body should contain newKey");
			assertTrue(body.contains("newValue"), "Body should contain newValue");
		}
	}

	@Test void b03_setConfigContents_putRawIni() throws Exception {
		try (var c = buildClient()) {
			var ini = "[NewSection]\nfoo = bar\n";
			var resp = c.put("/", ini).run().assertStatus(200);
			var body = resp.getContent().asString();
			assertTrue(body.contains("NewSection"), "Body should contain NewSection after PUT");
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Writes are PUT only
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_formPost_isNotAllowed() throws Exception {
		try (var c = buildClient()) {
			var ini = "[FormSection]\nbaz = qux\n";
			c.formPostPairs("/", "contents", ini).run().assertStatus(405);
			assertFalse(c.get("/").run().getContent().asString().contains("FormSection"), "A form POST must not change the configuration");
		}
	}

	@Test void c02_putOperations_areMutating() {
		for (var m : ConfigResource.class.getMethods())
			if (m.getAnnotation(RestPut.class) != null)
				assertNotNull(m.getAnnotation(Mutating.class), () -> m.getName() + " must be declared @Mutating");
	}
}
