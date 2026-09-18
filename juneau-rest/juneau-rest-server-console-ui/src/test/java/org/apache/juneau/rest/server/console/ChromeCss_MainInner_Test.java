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

import static java.nio.charset.StandardCharsets.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Pins the inner-main well, card fill-width, and outer table-chrome contracts in {@code chrome.css}.
 */
class ChromeCss_MainInner_Test extends TestBase {

	private static String readChromeCss() throws IOException {
		try (var in = ChromeCss_MainInner_Test.class.getResourceAsStream("/org/apache/juneau/console/chrome.css")) {
			assertNotNull(in, "missing classpath resource: /org/apache/juneau/console/chrome.css");
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static String flat() throws IOException {
		return readChromeCss().replaceAll("\\s+", " ");
	}

	@Test void a01_jcMain_paintsMainBg_gutter_radius_noBorder() throws Exception {
		var c = flat();
		assertTrue(c.contains(".jc-main {"), ".jc-main rule missing");
		var start = c.indexOf(".jc-main {");
		var end = c.indexOf('}', start);
		var body = c.substring(start, end);
		assertTrue(body.contains("background-color: var(--jc-main-bg)"), body);
		assertTrue(body.contains("margin: var(--jc-space-3)"), body);
		assertTrue(body.contains("border-radius: var(--jc-radius)"), body);
		assertFalse(body.contains("max-width:"), "Q4: no max-width on .jc-main: " + body);
		assertFalse(body.contains("border:"), "Q4: no extra border on .jc-main: " + body);
		assertTrue(body.contains("padding: 20px 24px 40px"), "keep today's inner padding: " + body);
	}

	@Test void a02_cardAndPageHeader_haveNo1180MaxWidth() throws Exception {
		var css = readChromeCss();
		assertFalse(css.contains("max-width: 1180px"), "Q5: drop max-width 1180px on .jc-card / .jc-page-header");
	}

	@Test void a03_tableChrome_transparentUnpadded_outerOnly() throws Exception {
		var c = flat();
		var sel = ".jc-card:has([data-juneau-layout=\"wide\"]):not(.jc-card .jc-card)";
		assertTrue(c.contains(sel), "missing table-chrome selector");
		var start = c.indexOf(sel);
		var brace = c.indexOf('{', start);
		var end = c.indexOf('}', brace);
		var body = c.substring(brace, end);
		assertTrue(body.contains("background-color: transparent"), body);
		assertTrue(body.contains("padding: 0"), body);
	}
}
