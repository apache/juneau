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
package org.apache.juneau.rest.server.console.test;

import java.util.*;
import java.util.function.*;
import java.util.regex.*;

/**
 * Test helper that checks every asset a rendered console page links (stylesheets, scripts, images, icons) actually
 * resolves.
 *
 * <p>
 * The console chrome links its assets at context-root URLs such as {@code /juneau-console/chrome.css}, so they only
 * load if the host mounts something at those paths.  Page tests that assert the hrefs cannot see a missing mount;
 * this helper fetches each URL through whatever client the caller supplies and reports every one that does not
 * answer {@code 200}.
 *
 * <p>
 * The fetcher is a plain {@link Function} from URL to HTTP status, so the class has no dependency on a test
 * framework or on a particular client.  Failures throw {@link AssertionError}.  Only same-origin URLs are checked:
 * absolute {@code http(s):}, protocol-relative, {@code data:} and fragment-only URLs are skipped.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	MockRestClient <jv>client</jv> = MockRestClient.<jsm>buildLax</jsm>(MyHost.<jk>class</jk>);
 * 	String <jv>html</jv> = <jv>client</jv>.get(<js>"/page"</js>).run().getContent().asString();
 * 	AssetResolutionAssert.<jsm>assertAssetsResolve</jsm>(<jv>html</jv>, <jv>url</jv> -&gt; <jv>client</jv>.get(<jv>url</jv>).run().getStatusCode());
 * </p>
 *
 * @since 10.0.0
 */
public final class AssetResolutionAssert {

	private static final Pattern TAG = Pattern.compile("<(script|img|link)\\b([^>]*)>", Pattern.CASE_INSENSITIVE);
	private static final Pattern SRC = attr("src");
	private static final Pattern HREF = attr("href");
	private static final Pattern REL = attr("rel");
	private static final Set<String> ASSET_RELS = Set.of("stylesheet", "icon", "shortcut", "apple-touch-icon", "preload", "modulepreload", "manifest");

	private AssetResolutionAssert() {}

	private static Pattern attr(String name) {
		return Pattern.compile("(?<![\\w-])" + name + "\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')", Pattern.CASE_INSENSITIVE);
	}

	private static String value(Pattern p, String attrs) {
		var m = p.matcher(attrs);
		if (! m.find())
			return null;
		return m.group(1) != null ? m.group(1) : m.group(2);
	}

	private static boolean isAssetLink(String attrs) {
		var rel = value(REL, attrs);
		if (rel == null)
			return false;
		for (var token : rel.toLowerCase(Locale.ROOT).split("\\s+"))
			if (ASSET_RELS.contains(token))
				return true;
		return false;
	}

	private static boolean isSameOrigin(String url) {
		return ! (url.isEmpty() || url.startsWith("#") || url.startsWith("//") || url.matches("(?i)^[a-z][a-z0-9+.-]*:.*"));
	}

	/**
	 * Returns the same-origin asset URLs a page links, in document order and without duplicates.
	 *
	 * <p>
	 * Covers {@code <script src>}, {@code <img src>} and {@code <link href>} whose {@code rel} is a stylesheet,
	 * icon, preload or manifest.  {@code &amp;} is decoded and any fragment is dropped.
	 *
	 * @param html The rendered page.
	 * @return The asset URLs.  Never <jk>null</jk>.
	 */
	public static List<String> assetUrls(String html) {
		var urls = new LinkedHashSet<String>();
		var m = TAG.matcher(html);
		while (m.find()) {
			var attrs = m.group(2);
			String url;
			if ("link".equalsIgnoreCase(m.group(1)))
				url = isAssetLink(attrs) ? value(HREF, attrs) : null;
			else
				url = value(SRC, attrs);
			if (url == null)
				continue;
			url = url.replace("&amp;", "&").trim();
			var hash = url.indexOf('#');
			if (hash > 0)
				url = url.substring(0, hash);
			if (isSameOrigin(url))
				urls.add(url);
		}
		return List.copyOf(urls);
	}

	/**
	 * Returns a description of every linked asset that does not answer {@code 200}.
	 *
	 * @param html The rendered page.
	 * @param statusOf Fetches a URL and returns its HTTP status code.
	 * @return One {@code "<status> <url>"} entry per failing asset, sorted; empty when every asset resolves.
	 */
	public static List<String> unresolved(String html, Function<String,Integer> statusOf) {
		var bad = new TreeSet<String>();
		for (var url : assetUrls(html)) {
			var status = statusOf.apply(url);
			if (status == null || status != 200)
				bad.add(status + " " + url);
		}
		return List.copyOf(bad);
	}

	/**
	 * Asserts that every asset the page links answers {@code 200}.
	 *
	 * @param html The rendered page.
	 * @param statusOf Fetches a URL and returns its HTTP status code.
	 * @throws AssertionError If any linked asset does not resolve; the message lists each one.
	 */
	public static void assertAssetsResolve(String html, Function<String,Integer> statusOf) {
		assertAssetsResolve(null, html, statusOf);
	}

	/**
	 * Asserts that every asset the page links answers {@code 200}, naming the page in the failure message.
	 *
	 * @param page A label for the page (typically its path); can be <jk>null</jk>.
	 * @param html The rendered page.
	 * @param statusOf Fetches a URL and returns its HTTP status code.
	 * @throws AssertionError If any linked asset does not resolve; the message lists each one.
	 */
	public static void assertAssetsResolve(String page, String html, Function<String,Integer> statusOf) {
		var bad = unresolved(html, statusOf);
		if (! bad.isEmpty())
			throw new AssertionError((page == null ? "" : "Unresolved assets linked from " + page + ":\n") + String.join("\n", bad));
	}
}
