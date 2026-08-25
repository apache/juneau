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
package org.apache.juneau.rest.server.ops;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.config.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Validates that {@link HtdocMixin} still resolves its legacy, fixed {@code [GET /htdocs/*]} mount after
 * being refactored to delegate to
 * {@link org.apache.juneau.rest.server.staticfile.StaticFilesMixin#resolveStaticFile(RestRequest,String,java.util.Locale)}
 * &mdash; the same resolution chokepoint used by the newer configurable-mount
 * {@link org.apache.juneau.rest.server.staticfile.StaticFilesMixin}. Also pins {@code HtdocMixin}'s
 * externally-observable contract: fixed mount, {@code GET}-only (no {@code HEAD}).
 *
 * @since 10.0.0
 */
class HtdocMixin_Test extends TestBase {

	@Rest(mixins=HtdocMixin.class)
	public static class A extends RestServlet implements BasicUniversalConfig {
		private static final long serialVersionUID = 1L;
	}

	private static final MockRestClient c = MockRestClient.buildLax(A.class);

	@Test void a01_existingFileResolvesViaLegacyMount() throws Exception {
		c.get("/htdocs/themes/devops.css")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("Licensed to the Apache Software Foundation");
	}

	@Test void a02_missingFileReturns404() throws Exception {
		c.get("/htdocs/does-not-exist.css")
			.run()
			.assertStatus(404);
	}

	@Test void a03_headNotSupported() throws Exception {
		// HtdocMixin keeps its legacy GET-only contract (unlike StaticFilesMixin, which also serves HEAD).
		c.head("/htdocs/themes/devops.css")
			.run()
			.assertStatus(404);
	}
}
