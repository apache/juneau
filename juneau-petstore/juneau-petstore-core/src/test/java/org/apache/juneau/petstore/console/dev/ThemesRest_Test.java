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
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.console.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * P9: the themes page, each stock theme via {@code ?theme=}, and the custom token theme.
 */
@SuppressWarnings({
	"resource" // MockRestClient is a no-op close.
})
class ThemesRest_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// a - stock themes
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_defaultIsOpen() throws Exception {
		var html = page(client(), "/console/dev/themes");
		assertPage(html).isValid().hasActiveNav("dev", "themes").hasCardOrder("swatches", "picker", "authoring").hasTheme("open");
	}

	@ParameterizedTest
	@ValueSource(strings={"open", "light-red", "light-brown", "red", "gray"})
	void a02_eachStockTheme(String name) throws Exception {
		var html = page(client(), "/console/dev/themes?theme=" + name);
		assertPage(html).isValid().hasTheme(name);
		assertContains("juneau-theme-" + name + ".css", html);
	}

	@Test void a03_pickerListsEveryThemeAndCustom() throws Exception {
		var html = page(client(), "/console/dev/themes");
		for (var n : ConsoleChromeMixin.BUILTIN_THEME_NAMES)
			assertContains("href=\"/console/dev/themes?theme=" + n + "\"", html);
		assertContains("href=\"/console/dev/themes?theme=custom\"", html);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b - custom token theme
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_customThemeEmitsTheOverrideBlock() throws Exception {
		var html = page(client(), "/console/dev/themes?theme=custom");
		assertPage(html).isValid().hasTheme("light-brown");
		assertContainsAll(html, "juneau-theme-light-brown.css", "--jc-pill-red-bg:#fdeceb", "--jc-tab-bar-bg:var(--jc-card-bg)");
	}

	@Test void b02_stockThemeHasNoOverrideBlock() throws Exception {
		var html = page(client(), "/console/dev/themes?theme=light-brown");
		assertFalse(html.contains("--jc-pill-red-bg:#fdeceb"), () -> "stock theme should not carry the custom overrides");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c - validation and scope
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_unknownThemeIs400() throws Exception {
		client().get("/console/dev/themes?theme=neon").accept("application/json").run()
			.assertStatus(400)
			.assertContent().isContains("Unknown theme 'neon'", "'open, light-red, light-brown, red, gray, custom'");
	}

	@Test void c02_otherPagesKeepTheDefault() throws Exception {
		assertPage(page(client(), "/console/store")).hasTheme("open");
	}
}
