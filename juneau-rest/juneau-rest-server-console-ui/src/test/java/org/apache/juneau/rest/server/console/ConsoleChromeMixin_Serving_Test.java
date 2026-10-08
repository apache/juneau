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
package org.apache.juneau.rest.server.console;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

@SuppressWarnings({
	"resource" // The static in-process MockRestClient C (and its responses) live for the whole test class and hold no external resources
})
class ConsoleChromeMixin_Serving_Test extends TestBase {

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class WithMixin extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	private static final MockRestClient C = MockRestClient.buildLax(WithMixin.class);

	@ParameterizedTest
	@ValueSource(strings = {"/juneau-console/juneau-console.js", "/juneau-console.js"})
	void a01_shellJs(String path) throws Exception {
		var body = C.get(path).run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("javascript")
			.assertHeader("Cache-Control").isContains("max-age")
			.getContent().asString();
		assertTrue(body.contains("window.JuneauConsole"), "shell body");
	}

	@ParameterizedTest
	@ValueSource(strings = {"/juneau-console/juneau-page.schema.json", "/juneau-page.schema.json"})
	void a02_schema(String path) throws Exception {
		var body = C.get(path).run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("json")
			.getContent().asString();
		assertEquals(PageContractSchema.get().schemaJson(), body);
	}

	@ParameterizedTest
	@ValueSource(strings = {"/juneau-console/juneau-badges.js", "/juneau-badges.js"})
	void a04_badgesJs(String path) throws Exception {
		var body = C.get(path).run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("javascript")
			.assertHeader("Cache-Control").isContains("max-age")
			.getContent().asString();
		assertTrue(body.contains("window.JuneauConsoleBadges"), "badges body");
	}

	@Test void a03_constants() {
		assertEquals("/juneau-console/juneau-console.js", ConsoleChromeMixin.CONSOLE_JS_PATH);
		assertEquals("/juneau-console/juneau-page.schema.json", ConsoleChromeMixin.SCHEMA_PATH);
	}
}
