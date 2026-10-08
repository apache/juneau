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

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Proves {@code juneau-console-output.js} never writes line data through an HTML sink (spec §7): no
 * {@code innerHTML}, {@code outerHTML}, {@code insertAdjacentHTML}, {@code document.write} or jQuery
 * {@code .html(}.  Icons arrive as nodes from {@code JuneauViews.helpers.icon}, so this file needs no allowlist entry.
 */
class ViewsJs_ConsoleOutputSinks_Test extends TestBase {

	private static String source() throws Exception {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.CONSOLE_OUTPUT_JS_RESOURCE)) {
			assertNotNull(in, ViewsMixin.CONSOLE_OUTPUT_JS_RESOURCE);
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	@Test void a01_moduleHasNoHtmlSinks() throws Exception {
		var r = RawContentSinkScanner.scanJsHtmlSinks("juneau-console-output.js", source());
		assertTrue(r.sinks().isEmpty(), () -> "unexpected HTML sinks: " + r.sinks());
		assertTrue(r.violations().isEmpty(), () -> String.join("\n", r.violations()));
	}

}
