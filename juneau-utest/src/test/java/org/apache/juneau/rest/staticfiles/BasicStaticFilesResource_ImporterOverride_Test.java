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
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.apache.juneau.rest.staticfile.*;
import org.junit.jupiter.api.*;

/**
 * Validates that an importer's {@code @Bean StaticFiles} factory method overrides the default
 * {@link BasicStaticFiles} configuration when mounted alongside {@link BasicStaticFilesResource}.
 *
 * <p>
 * The importer below declares a custom {@code StaticFiles} bean with a non-default
 * {@code Cache-Control} header ({@code "no-store"} instead of the
 * {@code "max-age=86400, public"} baked into {@link BasicStaticFiles}). The mixin reads
 * {@code RestRequest.getStaticFiles()} at request time, which delegates to
 * {@code BeanStore.getBean(StaticFiles.class)}, so the importer's bean wins.
 *
 * @since 9.5.0
 */
class BasicStaticFilesResource_ImporterOverride_Test extends TestBase {

	@Rest(mixins=BasicStaticFilesResource.class)
	public static class A extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@Bean public StaticFiles staticFiles(BeanStore bs) {
			return BasicStaticFiles
				.create(bs)
				.cp(A.class, "/htdocs", true)
				.headers(CacheControl.of("no-store"))
				.build();
		}
	}

	private static final MockRestClient c = MockRestClient.buildLax(A.class);

	@Test void a01_overrideTakesEffect() throws Exception {
		c.get("/static/javadoc.css")
			.run()
			.assertStatus(200)
			.assertHeader("Cache-Control").is("no-store");
	}

	@Test void a02_overrideAppliesAtLegacyHtdocPath() throws Exception {
		// FINISHED-101: /htdocs/* is no longer a multi-path default on the mixin, but
		// BasicRestServlet still owns the legacy /htdocs/* via BasicRestOperations#getHtdoc.
		// That handler also reads from BeanStore.getBean(StaticFiles.class), so the importer's
		// override still applies.
		c.get("/htdocs/javadoc.css")
			.run()
			.assertStatus(200)
			.assertHeader("Cache-Control").is("no-store");
	}
}
