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
import org.junit.jupiter.api.*;

/**
 * Validates the {@link StaticFilesServlet} servlet flavor mounted directly as a top-level servlet.
 *
 * <p>
 * The servlet pins its ops at {@code /*} and delegates to a shared {@link StaticFilesMixin} worker
 * (which reads the active {@link StaticFiles} bean at request time), so this mirrors
 * {@link StaticFilesMixin_AsMixin_Test} but exercises the standalone-servlet deployment. The
 * classpath resource {@code htdocs/javadoc.css} ships with {@code juneau-rest-server} and is visible
 * via the default {@link BasicStaticFiles} recursive classpath walk.
 * Cases:
 * <ul>
 * 	<li>A served classpath file with the default {@code Cache-Control} header.
 * 	<li>A missing path returns {@code 404}.
 * 	<li>{@code HEAD} mirrors GET headers with an empty body.
 * </ul>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // Static MockRestClient fields are a common test pattern; resources are managed by the mock framework.
})
class StaticFilesServlet_Test extends TestBase {

	private static final MockRestClient c = MockRestClient.buildLax(StaticFilesServlet.class);

	@Test void a01_servesClasspathFile() throws Exception {
		c.get("/javadoc.css")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("Licensed to the Apache Software Foundation");
	}

	@Test void a02_missingFileReturns404() throws Exception {
		c.get("/does-not-exist.css")
			.run()
			.assertStatus(404);
	}

	@Test void a03_cacheControl() throws Exception {
		c.get("/javadoc.css")
			.run()
			.assertStatus(200)
			.assertHeader("Cache-Control").is("max-age=86400, public");
	}

	@Test void a04_headReturnsHeadersWithEmptyBody() throws Exception {
		c.head("/javadoc.css")
			.run()
			.assertStatus(200)
			.assertContent().is("");
	}

	@Test void a05_headMissingFileReturns404() throws Exception {
		c.head("/does-not-exist.css")
			.run()
			.assertStatus(404);
	}
}
