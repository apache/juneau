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

package org.apache.juneau.releng;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.commons.inject.StackOverlay;
import org.apache.juneau.rest.mock.MockRestClient;
import org.apache.juneau.rest.server.console.ConsoleChromeMixin;
import org.junit.jupiter.api.Test;

class ConsoleAssetsRestTest {

	@SuppressWarnings({
		"resource" // Caller owns and closes the returned MockRestClient (via try-with-resources).
	})
	private static MockRestClient client() {
		return MockRestClient.builder(new ConsoleAssetsRest()).overridingBeanStore(new StackOverlay()).build();
	}

	@Test
	void a01_chromeCssBakesNoFooterPseudoElement() throws Exception {
		try (var client = client()) {
			try (var resp = client.request("GET", ConsoleChromeMixin.CHROME_CSS_PATH).run()) {
				assertEquals(200, resp.getStatusCode());
				var body = resp.getBodyAsString();
				// The bare mixin no longer bakes the footer copy into the stylesheet via a body::after rule; the
				// footer is authored declaratively as real HTML in base.ftlh's <@footer> slot instead.
				assertFalse(body.contains("body::after{content:"),
					"bare mixin must not inject a footer pseudo-element: " + body);
				assertFalse(body.contains("loopback tool for cutting Apache Juneau releases"),
					"footer copy must live in the <@footer> HTML, not chrome.css: " + body);
			}
		}
	}

	@Test
	void a02_chromeCssInheritsJuneauCardPaddingShadowAndControlBorder() throws Exception {
		try (var client = client()) {
			try (var resp = client.request("GET", ConsoleChromeMixin.CHROME_CSS_PATH).run()) {
				assertEquals(200, resp.getStatusCode());
				var body = resp.getBodyAsString();
				assertTrue(body.contains("--jc-card-padding:16px 16px 8px") || body.contains("--jc-card-padding: 16px 16px 8px"),
					"J0544 card padding must be on the SNAPSHOT chrome: " + body);
				assertTrue(body.contains("--jc-card-shadow:0 2px 2px rgba(0, 0, 0, 0.05)")
						|| body.contains("--jc-card-shadow: 0 2px 2px rgba(0, 0, 0, 0.05)"),
					"J0545 card shadow must be on the SNAPSHOT chrome: " + body);
				assertTrue(body.contains("padding: var(--jc-card-padding)"), body);
				assertTrue(body.contains("box-shadow: var(--jc-card-shadow)"), body);
				assertTrue(body.contains("--jc-control-border:var(--jc-border-2)")
						|| body.contains("--jc-control-border: var(--jc-border-2)"),
					"J0546 control-border alias must be on the SNAPSHOT chrome: " + body);
				assertTrue(body.contains(".jc-card:has([data-juneau-layout=\"wide\"]):not(.jc-card .jc-card)"),
					"table-chrome must unsadow the outer card: " + body);
			}
		}
	}
}
