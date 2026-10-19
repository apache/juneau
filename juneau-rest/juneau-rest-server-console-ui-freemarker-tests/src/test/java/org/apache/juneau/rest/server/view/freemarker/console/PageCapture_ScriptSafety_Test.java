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
package org.apache.juneau.rest.server.view.freemarker.console;

import static org.apache.juneau.rest.server.console.test.PageContractAssert.*;
import static org.apache.juneau.rest.server.view.freemarker.console.C1Fixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Contract text cannot break out of the {@code #juneau-page} island: every {@code <} is written as the JSON unicode
 * escape, and the parsed value is the author's literal string.
 *
 * @since 10.0.0
 */
class PageCapture_ScriptSafety_Test extends TestBase {

	@Test void a01_titleWithScriptCloseIsEscapedAndRoundTrips() {
		var html = render("page-script-break");
		var start = html.indexOf("<script type=\"application/json\" id=\"juneau-page\">");
		var end = html.indexOf("</script>", start);
		var island = html.substring(start, end);
		assertFalse(island.contains("<b>"), island);
		assertTrue(island.contains("\\u003c/script>"), island);
		var c = PageCapture_Segments_Test.card(assertPage(html).isValid(), "x");
		assertEquals("</script><b>t</b>", c.getString("title"));
	}

	@Test void a02_noSidecarsAnywhere() {
		for (var name : new String[] {"page-segments", "page-card-title", "page-card-src", "console-full"})
			assertFalse(render(name).contains("juneau-card-sidecar"), name);
	}
}
