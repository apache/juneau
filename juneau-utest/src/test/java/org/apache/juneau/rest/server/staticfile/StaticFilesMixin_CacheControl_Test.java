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
 * Validates that {@link StaticFilesMixin} preserves the default
 * {@code Cache-Control: max-age=86400, public} header end-to-end (the default supplied by
 * {@link BasicStaticFiles}).
 *
 * <p>
 * Header flow:
 * <ol>
 * 	<li>{@link BasicStaticFiles} attaches {@code Cache-Control: max-age=86400, public} to every
 * 		resolved {@link org.apache.juneau.http.HttpResource} as a default header.
 * 	<li>{@link org.apache.juneau.rest.server.server.processor.HttpResourceProcessor} forwards every header from
 * 		the {@code HttpResource} to the response.
 * 	<li>The {@code MockRest} client receives the header verbatim.
 * </ol>
 *
 * @since 10.0.0
 */
class StaticFilesMixin_CacheControl_Test extends TestBase {

	@Rest(mixins=StaticFilesMixin.class)
	public static class A extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	private static final MockRestClient c = MockRestClient.buildLax(A.class);

	@Test void a01_cacheControlOnStaticMount() throws Exception {
		c.get("/static/javadoc.css")
			.run()
			.assertStatus(200)
			.assertHeader("Cache-Control").is("max-age=86400, public");
	}

	@Test void a02_cacheControlOnLegacyHtdocPath() throws Exception {
		// FINISHED-101: /htdocs/* is no longer a multi-path default on the mixin, but
		// BasicRestServlet still owns the legacy /htdocs/* via BasicRestOperations#getHtdoc.
		// That handler also reads from BasicStaticFiles, so the Cache-Control default flows
		// through unchanged.
		c.get("/htdocs/javadoc.css")
			.run()
			.assertStatus(200)
			.assertHeader("Cache-Control").is("max-age=86400, public");
	}

	@Test void a03_cacheControlOnHeadRequest() throws Exception {
		// HEAD must mirror the GET's headers verbatim — Cache-Control included.
		c.head("/static/javadoc.css")
			.run()
			.assertStatus(200)
			.assertContent().is("")
			.assertHeader("Cache-Control").is("max-age=86400, public");
	}
}
