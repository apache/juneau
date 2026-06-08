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
package org.apache.juneau.rest.server.convention;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link SeoMixin} mounted as a mixin via {@code @Rest(mixins=...)} on a vanilla
 * {@link RestServlet}.
 *
 * <p>
 * Cases:
 * <ul>
 * 	<li>Default {@code /robots.txt} returns deny-all when no override is registered.
 * 	<li>Default {@code /sitemap.xml} returns an empty {@code <urlset>}.
 * 	<li>Importer's {@code @Bean SeoMixin} factory drives custom robots policy and sitemap
 * 		entries.
 * 	<li>Custom {@code robotsTxt(...)} body overrides any builder rules.
 * 	<li>Content-Type pinning works for both endpoints.
 * </ul>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // Static MockRestClient fields are a common test pattern; resources are managed by the mock framework.
})
class SeoMixin_AsMixin_Test extends TestBase {

	/** Default-host mounting the mixin with no @Bean override. */
	@Rest(mixins=SeoMixin.class)
	public static class A extends RestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/items") public String items() { return "items"; }
	}

	private static final MockRestClient ca = MockRestClient.buildLax(A.class);

	@Test void a01_defaultRobotsTxtIsDenyAll() throws Exception {
		ca.get("/robots.txt")
			.run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("text/plain")
			.assertContent().asString().is("User-agent: *\nDisallow: /\n");
	}

	@Test void a02_defaultSitemapIsEmptyUrlset() throws Exception {
		var body = ca.get("/sitemap.xml")
			.run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("application/xml")
			.getContent().asString();
		Assertions.assertTrue(body.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"), "XML prolog");
		Assertions.assertTrue(body.contains("<urlset"), "urlset opening");
		Assertions.assertTrue(body.contains("</urlset>"), "urlset closing");
		Assertions.assertFalse(body.contains("<url>"), "no entries by default");
	}

	@Test void a03_hostEndpointStillReachable() throws Exception {
		ca.get("/items").run().assertStatus(200).assertContent().asString().isContains("items");
	}

	/** Host with builder-driven robots policy via @Bean factory. */
	@Rest(mixins=SeoMixin.class)
	public static class B extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public SeoMixin seo() {
			return SeoMixin.create()
				.robotsAllow("*", "/")
				.robotsDisallow("BadBot", "/private")
				.sitemapEntry("https://example.com/")
				.sitemapEntry("https://example.com/items")
				.build();
		}
	}

	private static final MockRestClient cb = MockRestClient.buildLax(B.class);

	@Test void b01_overrideRobotsRules() throws Exception {
		var body = cb.get("/robots.txt").run().assertStatus(200).getContent().asString();
		Assertions.assertTrue(body.contains("User-agent: *"), "wildcard ua");
		Assertions.assertTrue(body.contains("Allow: /"), "allow root");
		Assertions.assertTrue(body.contains("User-agent: BadBot"), "bad bot ua");
		Assertions.assertTrue(body.contains("Disallow: /private"), "disallow private");
	}

	@Test void b02_overrideSitemapEntries() throws Exception {
		var body = cb.get("/sitemap.xml").run().assertStatus(200).getContent().asString();
		Assertions.assertTrue(body.contains("<loc>https://example.com/</loc>"), "root entry");
		Assertions.assertTrue(body.contains("<loc>https://example.com/items</loc>"), "items entry");
	}

	/** Host with a custom robots body via robotsTxt(...). */
	@Rest(mixins=SeoMixin.class)
	public static class C extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public SeoMixin seo() {
			return SeoMixin.create()
				.robotsTxt("User-agent: *\nCrawl-delay: 10\nSitemap: https://example.com/sitemap.xml\n")
				.build();
		}
	}

	private static final MockRestClient cc = MockRestClient.buildLax(C.class);

	@Test void c01_customRobotsBodyWinsOverRules() throws Exception {
		var body = cc.get("/robots.txt").run().assertStatus(200).getContent().asString();
		Assertions.assertTrue(body.contains("Crawl-delay: 10"), "crawl-delay directive");
		Assertions.assertTrue(body.contains("Sitemap: https://example.com/sitemap.xml"), "sitemap directive");
		Assertions.assertFalse(body.contains("Disallow: /"), "default deny-all body must be replaced");
	}
}
