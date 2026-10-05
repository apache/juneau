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
package org.apache.juneau.rest.server.views;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * C1-D5: {@code juneau-views.js} hydrates a freshly inserted bar slot through {@code JuneauConsole.chrome}, and
 * says so loudly when the shell is not loaded. {@code ConsoleBarSlot_BrowserTest} covers the runtime behavior.
 *
 * @since 10.0.0
 */
class ViewsJs_ConsoleChromeBridge_Test extends TestBase {

	private static String views() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.VIEWS_JS_RESOURCE)) {
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	@Test void a01_enhanceUsesTheConsoleShell() throws Exception {
		var js = views();
		var start = js.indexOf("function enhanceChromeInPanel(");
		assertTrue(start > 0, "enhanceChromeInPanel not found");
		var body = js.substring(start, js.indexOf("\n\t}\n", start));
		assertTrue(body.contains("window.JuneauConsole"), body);
		assertTrue(body.contains("[juneau-views] bar slot not hydrated: juneau-console.js is not loaded"), body);
		assertFalse(js.contains("window.JuneauChrome"), "juneau-views.js must not read window.JuneauChrome any more");
	}
}
