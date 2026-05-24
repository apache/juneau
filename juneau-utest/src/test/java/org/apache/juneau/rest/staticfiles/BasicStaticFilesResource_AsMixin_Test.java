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
package org.apache.juneau.rest.staticfiles;

import org.apache.juneau.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.apache.juneau.rest.staticfile.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link BasicStaticFilesResource} mounted as a mixin via {@code @Rest(mixins=...)} on a
 * vanilla {@link RestServlet}.
 *
 * <p>
 * Acceptance:
 * <ul>
 * 	<li>{@code GET /static/javadoc.css} returns the file (multi-mount default {@code paths}).
 * 	<li>{@code GET /htdocs/javadoc.css} returns the same file at the second default mount.
 * 	<li>{@code GET} on a missing path returns 404.
 * 	<li>The host's own endpoints are unaffected by the mixin.
 * </ul>
 *
 * <p>
 * The classpath resource {@code htdocs/javadoc.css} ships with {@code juneau-rest-server} and is
 * therefore visible to {@code juneau-utest}'s test classpath via the
 * {@link BasicStaticFiles} default constructor's recursive {@code cp(...,"htdocs",true)} walk.
 *
 * @since 9.5.0
 */
class BasicStaticFilesResource_AsMixin_Test extends TestBase {

	@Rest(mixins=BasicStaticFilesResource.class)
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

	@Test void a02_htdocsPathServesSameFile() throws Exception {
		c.get("/htdocs/javadoc.css")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("Licensed to the Apache Software Foundation");
	}

	@Test void a03_missingFileReturns404() throws Exception {
		c.get("/static/does-not-exist.css")
			.run()
			.assertStatus(404);
	}

	@Test void a04_missingFileAtHtdocsReturns404() throws Exception {
		c.get("/htdocs/does-not-exist.css")
			.run()
			.assertStatus(404);
	}

	@Test void a05_hostEndpointStillReachable() throws Exception {
		c.get("/items")
			.accept("application/json")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("items");
	}

	@Test void a06_headStaticReturnsHeadersWithEmptyBody() throws Exception {
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
		// Suppress unused-warning when neither header was set on the GET; both branches above already
		// drive the actual assertion.
		assert headContentType != null && headContentLength != null;
	}

	@Test void a07_headHtdocsReturnsHeadersWithEmptyBody() throws Exception {
		var head = c.head("/htdocs/javadoc.css").run();
		head.assertStatus(200);
		head.assertContent().is("");
	}

	@Test void a08_headMissingFileReturns404() throws Exception {
		c.head("/static/does-not-exist.css")
			.run()
			.assertStatus(404);
	}
}
