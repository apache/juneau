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
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.apache.juneau.rest.staticfile.*;
import org.junit.jupiter.api.*;

/**
 * Validates the {@link StaticFilesResource} child-resource flavor mounted via
 * {@code @Rest(children=...)} under a host.
 *
 * <p>
 * The child mounts at the subtree {@code /static} (op pinned at {@code /*}) and delegates to a shared
 * {@link StaticFilesMixin} worker. The classpath resource {@code htdocs/javadoc.css} ships with
 * {@code juneau-rest-server} and is visible via the default {@link BasicStaticFiles} recursive
 * classpath walk. Cases:
 * <ul>
 * 	<li>{@code GET /static/javadoc.css} serves the classpath file.
 * 	<li>A missing path returns {@code 404}.
 * 	<li>The host's own endpoints are unaffected by the mounted child.
 * </ul>
 *
 * @since 9.5.0
 */
class StaticFilesResource_Test extends TestBase {

	@Rest(children={StaticFilesResource.class})
	public static class A extends RestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/items") public String items() { return "items"; }
	}

	private static final MockRestClient c = MockRestClient.buildLax(A.class);

	@Test void a01_servesClasspathFileAsChild() throws Exception {
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
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("items");
	}
}
