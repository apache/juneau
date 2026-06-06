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
package org.apache.juneau.microservice.resources;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.config.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link ConfigResource} covering the GET/PUT/POST endpoints via {@link MockRestClient}.
 */
class ConfigResource_Test extends TestBase {

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
			.noTrace()
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

	@Test void a06_getConfigEditForm_returnsForm() throws Exception {
		try (var c = buildClient()) {
			var resp = c.get("/edit").run().assertStatus(200);
			var body = resp.getContent().asString();
			assertTrue(body.contains("form"), "Edit form should contain a form element");
			assertTrue(body.contains("textarea"), "Edit form should contain a textarea");
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
	// POST endpoint
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_setConfigContentsFormPost_postFormData() throws Exception {
		try (var c = buildClient()) {
			var ini = "[FormSection]\nbaz = qux\n";
			var resp = c.formPostPairs("/", "contents", ini).run().assertStatus(200);
			var body = resp.getContent().asString();
			assertTrue(body.contains("FormSection"), "Body should contain FormSection after FORM POST");
		}
	}
}
