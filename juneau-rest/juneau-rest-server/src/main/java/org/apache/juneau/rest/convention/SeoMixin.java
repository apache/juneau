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

import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.servlet.*;

/**
 * Mixin that serves SEO-conventional endpoints {@code /robots.txt} and {@code /sitemap.xml}.
 *
 * <p>
 * Sibling of {@link FaviconMixin} ({@code /favicon.ico}),
 * {@link VersionMixin} ({@code /version} / {@code /info} / {@code /about}), and
 * {@link WellKnownMixin} ({@code /.well-known/*}). All four classes live in the
 * {@code org.apache.juneau.rest.convention} convention-endpoints mixin pack.
 *
 * <p>
 * Compose into a host resource via {@link Rest#mixins() @Rest(mixins=SeoMixin.class)};
 * the {@code /robots.txt} and {@code /sitemap.xml} URLs become available alongside the host's own
 * endpoints with no further wiring.
 *
 * <h5 class='section'>Hardcoded mount paths:</h5>
 *
 * <p>
 * Unlike the sibling api-docs and ops mixins (see {@link org.apache.juneau.rest.docs.SwaggerMixin},
 * {@link VersionMixin}, etc.), the two mount paths here are <b>not</b> SVL-configurable.
 * {@code /robots.txt} is fixed by the Robots Exclusion Protocol
 * (<a href="https://www.rfc-editor.org/rfc/rfc9309">RFC 9309</a>) which prescribes that
 * crawlers fetch the policy from {@code /robots.txt} at the site root; {@code /sitemap.xml} is
 * fixed by the <a href="https://www.sitemaps.org/protocol.html">sitemaps.org protocol</a>
 * (and consumed by search engines configured to look for the file at that exact path). A
 * runtime mount-path override here would have no practical effect.
 *
 * <h5 class='section'>Mixin-only deployment:</h5>
 *
 * <p>
 * This resource is designed for composition via {@code @Rest(mixins=...)}. The two mount paths
 * ({@code /robots.txt}, {@code /sitemap.xml}) are pinned at the op level by
 * {@link RestGet @RestGet(path=...)} on the handler methods; a class-level
 * {@code @Rest(paths=...)} declaration would be silently ignored under the mixin pattern (see
 * {@link Rest#paths() @Rest(paths)} Javadoc).
 *
 * <h5 class='figure'>Composition example:</h5>
 *
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(path=<js>"/api"</js>, mixins=SeoMixin.<jk>class</jk>)
 * 	<jk>public class</jk> ApiResource <jk>extends</jk> RestServlet { }
 *
 * 	<jc>// Override defaults via a @Bean factory:</jc>
 * 	<ja>@Bean</ja> SeoMixin seo() {
 * 		<jk>return</jk> SeoMixin.<jsm>create</jsm>()
 * 			.robotsAllow(<js>"*"</js>, <js>"/"</js>)
 * 			.sitemapEntry(<js>"/api/items"</js>)
 * 			.build();
 * 	}
 * </p>
 *
 * <h5 class='section'>Defaults &amp; behavior:</h5>
 *
 * <ul class='spaced-list'>
 * 	<li><b>Robots policy &mdash; deny-all by default.</b> {@code GET /robots.txt} returns
 * 		{@code "User-agent: *\nDisallow: /\n"} unless the builder specifies a different policy via
 * 		{@link Builder#robotsAllow(String,String...) robotsAllow(...)} or
 * 		{@link Builder#robotsDisallow(String,String...) robotsDisallow(...)}. The deny-all default
 * 		is intentional &mdash; mounting the mixin without thinking should not auto-opt the service
 * 		into search-engine indexing.
 * 	<li><b>Sitemap empty by default.</b> {@code GET /sitemap.xml} returns an empty
 * 		{@code <urlset>} when no entries are configured. Add entries via
 * 		{@link Builder#sitemapEntry(String) sitemapEntry(String)} or
 * 		{@link Builder#sitemapEntry(String,ZonedDateTime,String,Double) sitemapEntry(String,ZonedDateTime,String,Double)}.
 * 	<li><b>Content types.</b> {@code /robots.txt} returns {@code text/plain; charset=UTF-8};
 * 		{@code /sitemap.xml} returns {@code application/xml; charset=UTF-8}.
 * 	<li>Both endpoints are excluded from generated Swagger / OpenAPI specs via
 * 		{@link OpSwagger#ignore() @OpSwagger(ignore=true)}.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link FaviconMixin}
 * 	<li class='jc'>{@link VersionMixin}
 * 	<li class='jc'>{@link WellKnownMixin}
 * 	<li class='link'><a class="doclink" href="https://www.rfc-editor.org/rfc/rfc9309">RFC 9309 &mdash; Robots Exclusion Protocol</a>
 * 	<li class='link'><a class="doclink" href="https://www.sitemaps.org/protocol.html">sitemaps.org &mdash; Sitemap protocol</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Composition (mixins, paths)</a>
 * </ul>
 *
 * @since 10.0.0
 */
// @formatter:off
@Rest
public class SeoMixin extends RestMixin {

	/** Default robots policy: deny everything. */
	public static final String DEFAULT_ROBOTS = "User-agent: *\nDisallow: /\n";

	/**
	 * Creates a new builder.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	private final String robotsTxt;
	private final List<SitemapEntry> sitemapEntries;

	/** No-arg constructor used when a host registers the mixin without builder configuration. */
	public SeoMixin() {
		this(create());
	}

	/**
	 * Builder constructor.
	 *
	 * <p>
	 * Stashes the programmatic {@link RestBuilder}(the builder itself, carrying any {@code @Rest}-level
	 * overrides such as {@code path}) via {@code super(builder)} so those values take precedence over this
	 * mixin's own {@link Rest @Rest} annotation.
	 *
	 * @param builder The builder.
	 */
	protected SeoMixin(Builder builder) {
		super(builder);
		robotsTxt = builder.buildRobots();
		sitemapEntries = List.copyOf(builder.sitemapEntries);
	}

	/**
	 * [GET /robots.txt] &mdash; emit the configured robots policy as {@code text/plain}.
	 *
	 * @param res The current REST response.
	 * @throws IOException If an I/O error occurs while writing the response.
	 */
	@RestGet(
		path="/robots.txt",
		summary="Robots policy",
		description="Robots Exclusion Protocol policy file (RFC 9309).",
		swagger=@OpSwagger(ignore=true)
	)
	public void getRobotsTxt(RestResponse res) throws IOException {
		try (var w = res.getDirectWriter("text/plain; charset=UTF-8")) {
			w.write(robotsTxt);
		}
	}

	/**
	 * [GET /sitemap.xml] &mdash; emit the configured sitemap as {@code application/xml}.
	 *
	 * @param res The current REST response.
	 * @throws IOException If an I/O error occurs while writing the response.
	 */
	@RestGet(
		path="/sitemap.xml",
		summary="Sitemap",
		description="XML sitemap of indexable URLs (sitemaps.org protocol).",
		swagger=@OpSwagger(ignore=true)
	)
	public void getSitemap(RestResponse res) throws IOException {
		try (var w = res.getDirectWriter("application/xml; charset=UTF-8")) {
			w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			w.write("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
			for (var e : sitemapEntries)
				e.write(w);
			w.write("</urlset>\n");
		}
	}

	/**
	 * Single sitemap entry (immutable). Composed of a required {@code loc} URL and optional
	 * {@code lastmod}, {@code changefreq}, and {@code priority} elements per the
	 * <a href="https://www.sitemaps.org/protocol.html">sitemaps.org protocol</a>.
	 */
	public static final class SitemapEntry {
		private final String loc;
		private final ZonedDateTime lastmod;
		private final String changefreq;
		private final Double priority;

		SitemapEntry(String loc, ZonedDateTime lastmod, String changefreq, Double priority) {
			this.loc = loc;
			this.lastmod = lastmod;
			this.changefreq = changefreq;
			this.priority = priority;
		}

		void write(java.io.Writer w) throws IOException {
			w.write("\t<url>\n");
			w.write("\t\t<loc>" + xmlEscape(loc) + "</loc>\n");
			if (lastmod != null)
				w.write("\t\t<lastmod>" + lastmod.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) + "</lastmod>\n");
			if (changefreq != null)
				w.write("\t\t<changefreq>" + xmlEscape(changefreq) + "</changefreq>\n");
			if (priority != null)
				w.write("\t\t<priority>" + priority + "</priority>\n");
			w.write("\t</url>\n");
		}

		private static String xmlEscape(String s) {
			return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
		}
	}

	/**
	 * Builder for {@link SeoMixin} instances.
	 *
	 * <p>
	 * Extends {@link org.apache.juneau.rest.servlet.RestMixin.Builder} so the mixin's bespoke robots/sitemap setters chain
	 * with true covariant returns alongside the inherited {@link RestBuilder}surface (e.g. {@code path},
	 * {@code roleGuard}).
	 */
	public static class Builder extends RestMixin.Builder<SeoMixin, Builder> {

		private final List<RobotsRule> robotsRules = new ArrayList<>();
		private final List<SitemapEntry> sitemapEntries = new ArrayList<>();
		private String customRobotsTxt;

		/** Constructor &mdash; package access for {@link SeoMixin#create()}. */
		protected Builder() {
			super(SeoMixin.class);
		}

		/**
		 * Adds an {@code Allow} rule to the robots policy.
		 *
		 * <p>
		 * Multiple invocations append rules in order. The first call to either
		 * {@link #robotsAllow(String,String...) robotsAllow(...)} or
		 * {@link #robotsDisallow(String,String...) robotsDisallow(...)} replaces the deny-all
		 * default; subsequent calls continue to append.
		 *
		 * @param userAgent The user agent (e.g. {@code "*"} or {@code "Googlebot"}).
		 * 	Must not be <jk>null</jk> or blank.
		 * @param paths Allowed path prefixes (e.g. {@code "/"}). At least one path is required.
		 * @return This object.
		 */
		public Builder robotsAllow(String userAgent, String...paths) {
			robotsRules.add(new RobotsRule(userAgent, true, paths));
			return this;
		}

		/**
		 * Adds a {@code Disallow} rule to the robots policy.
		 *
		 * @param userAgent The user agent (e.g. {@code "*"}). Must not be <jk>null</jk> or blank.
		 * @param paths Disallowed path prefixes. At least one path is required.
		 * @return This object.
		 */
		public Builder robotsDisallow(String userAgent, String...paths) {
			robotsRules.add(new RobotsRule(userAgent, false, paths));
			return this;
		}

		/**
		 * Sets a fully-formed {@code robots.txt} body, overriding any rule-builder calls.
		 *
		 * <p>
		 * Useful when the desired policy doesn't fit the simple
		 * {@code allow}/{@code disallow}/per-user-agent shape of
		 * {@link #robotsAllow(String,String...) robotsAllow(...)} /
		 * {@link #robotsDisallow(String,String...) robotsDisallow(...)} (e.g. when including
		 * {@code Sitemap:} or {@code Crawl-delay:} directives).
		 *
		 * @param value The full {@code robots.txt} body. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder robotsTxt(String value) {
			customRobotsTxt = value;
			return this;
		}

		/**
		 * Adds a sitemap entry with only the {@code loc} URL.
		 *
		 * @param url The fully-qualified URL.
		 * @return This object.
		 */
		public Builder sitemapEntry(String url) {
			return sitemapEntry(url, null, null, null);
		}

		/**
		 * Adds a sitemap entry with optional {@code lastmod}, {@code changefreq}, and
		 * {@code priority} components.
		 *
		 * @param url The fully-qualified URL.
		 * @param lastmod The last-modified instant (or <jk>null</jk> to omit).
		 * @param changefreq Change-frequency hint (e.g. {@code "weekly"}, {@code "daily"}; or
		 * 	<jk>null</jk> to omit).
		 * @param priority Priority value 0.0..1.0 (or <jk>null</jk> to omit).
		 * @return This object.
		 */
		public Builder sitemapEntry(String url, ZonedDateTime lastmod, String changefreq, Double priority) {
			sitemapEntries.add(new SitemapEntry(url, lastmod, changefreq, priority));
			return this;
		}

		/**
		 * Builds a {@link SeoMixin} instance.
		 *
		 * @return A configured instance.
		 */
		@Override /* AbstractRestBuilder */
		public SeoMixin build() {
			return new SeoMixin(this);
		}

		String buildRobots() {
			if (customRobotsTxt != null)
				return customRobotsTxt;
			if (robotsRules.isEmpty())
				return DEFAULT_ROBOTS;
			var sb = new StringBuilder();
			for (var r : robotsRules) {
				sb.append("User-agent: ").append(r.userAgent).append('\n');
				for (var p : r.paths)
					sb.append(r.allow ? "Allow: " : "Disallow: ").append(p).append('\n');
			}
			return sb.toString();
		}
	}

	/** Internal representation of a single user-agent + allow/disallow + paths rule. */
	@SuppressWarnings({
		"java:S125" // The constructor comment is explanatory prose referencing robotsAllow/robotsDisallow, not commented-out code.
	})
	private static final class RobotsRule {
		final String userAgent;
		final boolean allow;
		final String[] paths;

		RobotsRule(String userAgent, boolean allow, String[] paths) {
			this.userAgent = userAgent;
			this.allow = allow;
			// paths arrives via varargs from robotsAllow/robotsDisallow → never null in practice;
			// clone to insulate downstream mutation.
			this.paths = paths.clone();
		}
	}
}
