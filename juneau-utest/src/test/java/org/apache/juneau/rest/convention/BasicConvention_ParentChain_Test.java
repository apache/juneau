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
package org.apache.juneau.rest.convention;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Verifies that all four convention-pack mixins compose cleanly on a single host with no path
 * collisions and that each mixin's {@code RestContext} is registered.
 *
 * <p>
 * Setup: a single {@link RestServlet} host mounts all four convention mixins
 * (favicon, SEO, version, well-known) and registers builder-configured {@code @Bean}
 * factories for those that need explicit configuration.
 *
 * <p>
 * Acceptance:
 * <ul>
 * 	<li>All four mixins appear in {@code RestContext.getMixinContexts()}.
 * 	<li>Every convention path resolves (favicon.ico, robots.txt, sitemap.xml, version, info,
 * 		about, .well-known/security.txt).
 * 	<li>Host's own {@code /items} endpoint is unaffected.
 * </ul>
 *
 * @since 9.5.0
 */
class BasicConvention_ParentChain_Test extends TestBase {

	@Rest(mixins={
		FaviconMixin.class,
		SeoMixin.class,
		VersionMixin.class,
		WellKnownMixin.class
	})
	public static class A extends RestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet(path="/items") public String items() { return "items"; }

		@Bean public SeoMixin seo() {
			return SeoMixin.create()
				.robotsAllow("*", "/")
				.sitemapEntry("https://example.com/items")
				.build();
		}

		@Bean public VersionMixin version() {
			return VersionMixin.create()
				.entry("name", "convention-pack")
				.entry("version", "9.5.0")
				.fromJavaVersion()
				.build();
		}

		@Bean public WellKnownMixin wellKnown() {
			return WellKnownMixin.create()
				.securityTxt("Contact: security@example.com\nExpires: 2027-01-01T00:00:00Z\n")
				.build();
		}
	}

	private static final MockRestClient c = MockRestClient.buildLax(A.class);

	@Test void a01_allFourMixinContextsRegistered() throws Exception {
		MockRestClient.buildLax(A.class);
		var hostCtx = RestContext.getGlobalRegistry().get(A.class);
		var ctxs = hostCtx.getMixinContexts();

		assertNotNull(ctxs.get(FaviconMixin.class), "Favicon mixin context registered");
		assertNotNull(ctxs.get(SeoMixin.class), "SEO mixin context registered");
		assertNotNull(ctxs.get(VersionMixin.class), "Version mixin context registered");
		assertNotNull(ctxs.get(WellKnownMixin.class), "Well-known mixin context registered");
		assertEquals(4, ctxs.size(),
			"Expected exactly four mixin contexts; got: " + ctxs.keySet());
	}

	@Test void a02_faviconResolves() throws Exception {
		c.get("/favicon.ico").run().assertStatus(200)
			.assertHeader("Content-Type").is("image/x-icon");
	}

	@Test void a03_robotsTxtResolves() throws Exception {
		c.get("/robots.txt").run().assertStatus(200)
			.assertContent().asString().isContains("Allow: /");
	}

	@Test void a04_sitemapXmlResolves() throws Exception {
		c.get("/sitemap.xml").run().assertStatus(200)
			.assertContent().asString().isContains("<loc>https://example.com/items</loc>");
	}

	@Test void a05_versionEndpointResolves() throws Exception {
		c.get("/version").run().assertStatus(200)
			.assertContent().asString().isContains("\"name\": \"convention-pack\"");
	}

	@Test void a06_infoLegacyAliasNotMountedByDefault() throws Exception {
		// FINISHED-101: /info is no longer a multi-path default. Migration to that secondary
		// alias is covered by VersionMixin_SvlPathOverride_Test#a02.
		c.get("/info").run().assertStatus(404);
	}

	@Test void a07_aboutLegacyAliasNotMountedByDefault() throws Exception {
		// FINISHED-101: /about is no longer a multi-path default.
		c.get("/about").run().assertStatus(404);
	}

	@Test void a08_securityTxtResolves() throws Exception {
		c.get("/.well-known/security.txt").run().assertStatus(200)
			.assertContent().asString().isContains("Contact: security@example.com");
	}

	@Test void a09_hostEndpointStillReachable() throws Exception {
		c.get("/items").run().assertStatus(200)
			.assertContent().asString().isContains("items");
	}
}
