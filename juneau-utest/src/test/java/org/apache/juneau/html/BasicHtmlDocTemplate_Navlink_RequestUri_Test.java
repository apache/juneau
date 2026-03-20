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
package org.apache.juneau.html;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.config.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Verifies {@link BasicHtmlDocTemplate} navlink parsing for bare {@code request:/?query} and {@code request:?query}.
 */
@SuppressWarnings("serial")
class BasicHtmlDocTemplate_Navlink_RequestUri_Test extends TestBase {

	@Rest
	@HtmlDocConfig(navlinks = {
		"q: request:?Accept=text/json&plainText=true",
		"request:?Accept=text/plain&plainText=true",
		"request:/?Accept=text/html&plainText=true"
	})
	public static class R extends BasicRestServlet implements BasicJsonHtmlConfig {
		@RestGet
		public String page() {
			return "OK";
		}
	}

	@Test
	void a01_bareRequestQueryNavlinksResolveWithoutRequestPathSegment() throws Exception {
		var content = MockRestClient.buildLax(R.class).get("/page").accept("text/html").run().getContent().asString();
		assertFalse(content.contains("href=\"request:"),
			"href must not be browser-relative request:?... (creates .../request:?...)");
		assertTrue(content.contains("text/json") || content.contains("text%2Fjson"), content);
		assertTrue(content.contains("text/plain") || content.contains("text%2Fplain"), content);
		assertTrue(content.contains("text/html") || content.contains("text%2Fhtml"), content);
	}
}
