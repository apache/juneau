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
package org.apache.juneau.rest.server.staticfile;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link StaticFilesMixin} mounted as a mixin via {@code @Rest(mixins=...)} on a
 * vanilla {@link RestServlet}.
 *
 * <p>
 * Acceptance:
 * <ul>
 * 	<li>{@code GET /static/javadoc.css} returns the file (single SVL-configurable default mount).
 * 	<li>{@code GET} on a missing path returns 404.
 * 	<li>{@code HEAD /static/...} mirrors GET headers with empty body.
 * 	<li>The host's own endpoints are unaffected by the mixin.
 * </ul>
 *
 * <p>
 * The classpath resource {@code htdocs/javadoc.css} ships with {@code juneau-rest-server} and is
 * therefore visible to {@code juneau-integration-tests}'s test classpath via the
 * {@link BasicStaticFiles} default constructor's recursive {@code cp(...,"htdocs",true)} walk.
 * The default {@link BasicStaticFiles} classpath search root still walks both {@code static/}
 * and {@code htdocs/} directories &mdash; only the URL-side mount is now single. Per the
 * multi-path collapse, the {@code /htdocs/*} mount alias (formerly a dual default)
 * is now reached via {@code -Djuneau.staticfiles.path=htdocs}; that behavior is covered by
 * {@code StaticFilesMixin_SvlPathOverride_Test}.
 *
 * @since 10.0.0
 */
class StaticFilesMixin_AsMixin_Test extends TestBase {

	@Rest(mixins=StaticFilesMixin.class)
	public static class A extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/items") public String items() { return "items"; }
	}

	private static final MockRestClient c = MockRestClient.buildLax(A.class);

	@Test void a01_staticPathServesClasspathFile() throws Exception {
		c.get("/static/javadoc.css")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("Licensed to the Apache Software Foundation");
	}

	@Test void a02_missingFileReturns404() throws Exception {
		c.get("/static/does-not-exist.css")
			.run()
			.assertStatus(404);
	}

	@Test void a03_hostEndpointStillReachable() throws Exception {
		c.get("/items")
			.accept("application/json")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("items");
	}

	@Test void a04_headStaticReturnsHeadersWithEmptyBody() throws Exception {
		// HEAD must mirror GET's Content-Type and Content-Length while suppressing the body.
		var get = c.get("/static/javadoc.css").run();
		get.assertStatus(200);
		var getContentType = get.getHeader("Content-Type").asString().orElse("");
		var getContentLength = get.getHeader("Content-Length").asString().orElse("");

		var head = c.head("/static/javadoc.css").run();
		head.assertStatus(200);
		head.assertContent().is("");

		// Header parity (RFC 7231 §4.3.2).
		var headContentType = head.getHeader("Content-Type").asString().orElse("");
		var headContentLength = head.getHeader("Content-Length").asString().orElse("");
		if (!getContentType.isEmpty())
			head.assertHeader("Content-Type").is(getContentType);
		if (!getContentLength.isEmpty())
			head.assertHeader("Content-Length").is(getContentLength);
		assert headContentType != null && headContentLength != null;
	}

	@Test void a05_headMissingFileReturns404() throws Exception {
		c.head("/static/does-not-exist.css")
			.run()
			.assertStatus(404);
	}
}
