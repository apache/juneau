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
import org.apache.juneau.rest.staticfile.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link BasicStaticFilesResource} deployed as a <em>standalone</em> resource (Path B
 * in TODO-75) &mdash; the user extends {@code BasicStaticFilesResource} directly and the inherited
 * {@code @Rest(paths={"/static/*","/htdocs/*"})} declares both mount points without any additional
 * configuration.
 *
 * <p>
 * Two layers of {@code paths} interact here:
 * <ul>
 * 	<li><b>Servlet-mount paths</b> (from {@link Rest#paths() @Rest(paths)} on the class) &mdash;
 * 		container-level deployment seam, surfaced by real Jetty/Spring Boot deployments and
 * 		exercised by {@code BasicStaticFilesResource_JettyMicroservice_Test} (real-container test).
 * 	<li><b>Inner {@code @RestGet(path=...)}</b> &mdash; the URL-path matcher used by Juneau's
 * 		{@code UrlPathMatcher} once a request lands on the servlet. {@link BasicStaticFilesResource}
 * 		declares {@code path={"/static/*","/htdocs/*"}} so a single Java method binds to both
 * 		default URL prefixes.
 * </ul>
 *
 * <p>
 * {@code MockRest} dispatches directly to the inner matcher and does NOT model the container-level
 * servlet-mapping layer, so this test focuses on the standalone-extends-mixin shape and the inner
 * handler's behavior. The container-level multi-path mount is validated in the real-Jetty parity
 * test.
 *
 * @since 9.5.0
 */
class BasicStaticFilesResource_Standalone_Test extends TestBase {

	public static class CdnResource extends BasicStaticFilesResource { }

	private static final MockRestClient c = MockRestClient.buildLax(CdnResource.class);

	@Test void a01_standaloneServesFromStaticMount() throws Exception {
		c.get("/static/javadoc.css")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("Licensed to the Apache Software Foundation");
	}

	@Test void a02_standaloneServesFromHtdocsMount() throws Exception {
		c.get("/htdocs/javadoc.css")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("Licensed to the Apache Software Foundation");
	}

	@Test void a03_standaloneMissingFileReturns404() throws Exception {
		c.get("/static/does-not-exist.css")
			.run()
			.assertStatus(404);
	}

	@Test void a04_standaloneHeadProbe() throws Exception {
		c.head("/static/javadoc.css")
			.run()
			.assertStatus(200)
			.assertContent().is("");
	}

	@Test void a05_standaloneCacheControl() throws Exception {
		c.get("/static/javadoc.css")
			.run()
			.assertStatus(200)
			.assertHeader("Cache-Control").is("max-age=86400, public");
	}
}
