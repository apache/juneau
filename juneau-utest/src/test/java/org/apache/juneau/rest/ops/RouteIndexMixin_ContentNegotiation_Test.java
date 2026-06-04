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
package org.apache.juneau.rest.ops;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.config.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Validates that the {@link RouteIndexMixin} route-index op is <b>content-negotiated</b> &mdash; it returns a
 * {@link org.apache.juneau.rest.beans.RouteDescriptions} POJO rather than writing a fixed format, so the host's
 * configured serializers render HTML / JSON / XML based on the request {@code Accept} header.
 *
 * <p>
 * Mirrors the {@code NavigationMixin.getChildren(...)} pattern (see {@link NavigationMixin_AsMixin_Test}): the host
 * provides serializers (here via {@link BasicUniversalConfig}), an {@code Accept: text/html} request yields a
 * browsable page with the route {@linkplain org.apache.juneau.rest.beans.RouteDescription#getPath() path} rendered as
 * a clickable link, while {@code application/json} / {@code text/xml} clients receive the same entries in their
 * requested format.
 *
 * @since 10.0.0
 */
class RouteIndexMixin_ContentNegotiation_Test extends TestBase {

	@Rest(mixins=RouteIndexMixin.class)
	public static class A extends RestServlet implements BasicUniversalConfig {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/items", summary="List items") public String items() { return "items"; }
		@RestPost(path="/items", summary="Create item") public String create() { return "created"; }
	}

	private static final MockRestClient a = MockRestClient.buildLax(A.class);

	@Test void a01_htmlYieldsBrowsablePageWithLinks() throws Exception {
		var body = a.get("/options")
			.accept("text/html")
			.run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("text/html")
			.getContent().asString();
		// Rendered as an HTML table whose path cells are clickable links.
		assertTrue(body.contains("<table"), "HTML output should render a table; got: " + body);
		assertTrue(body.contains("href=") && body.contains("/items"),
			"HTML output should render the route path as a clickable link; got: " + body);
	}

	@Test void a02_jsonYieldsStrictJson() throws Exception {
		var body = a.get("/options")
			.accept("application/json")
			.run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("application/json")
			.getContent().asString();
		// Must be strict, parseable JSON (not Json5) preserving the path/methods/summary/description/deprecated shape.
		var parsed = JsonParser.DEFAULT.parse(body, List.class);
		assertFalse(parsed.isEmpty(), "JSON route index should not be empty");
		assertTrue(body.contains("\"path\""), "JSON output should carry the 'path' property; got: " + body);
		assertTrue(body.contains("/items"), "JSON output should list /items; got: " + body);
	}

	@Test void a03_xmlYieldsXml() throws Exception {
		var body = a.get("/options")
			.accept("text/xml")
			.run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("xml")
			.getContent().asString();
		assertTrue(body.contains("/items"), "XML output should list /items; got: " + body);
	}

	@Test void a04_sameEntriesAcrossFormats() throws Exception {
		// All three negotiated formats must surface the same routes (both /items ops here).
		var json = a.get("/options").accept("application/json").run().assertStatus(200).getContent().asString();
		var html = a.get("/options").accept("text/html").run().assertStatus(200).getContent().asString();
		var xml = a.get("/options").accept("text/xml").run().assertStatus(200).getContent().asString();
		for (var format : List.of(json, html, xml))
			assertTrue(format.contains("/items"), "Every negotiated format should list /items; got: " + format);
	}
}
