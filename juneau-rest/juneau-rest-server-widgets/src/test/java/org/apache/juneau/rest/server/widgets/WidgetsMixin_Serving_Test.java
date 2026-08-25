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
package org.apache.juneau.rest.server.widgets;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.utils.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.util.*;
import org.junit.jupiter.api.*;

/**
 * Load + declared-API + asset-URL tests for {@link WidgetsMixin}, mirroring the shape of the views module's
 * {@code ViewsMixin_Serving_Test}.
 *
 * <p>
 * This mixin now ships the four widget runtime assets ({@code juneau-cards.js}, {@code juneau-calendar.js},
 * {@code juneau-calendar.css}, {@code juneau-chrome.js}), relocated here from the views module beside the bean
 * contracts that drive them.  Serving tests follow the file owner, so the 200 matrix and the "these bytes are on this
 * module's classpath" assertions live here now, alongside what this mixin already owned: clean composition into a
 * host, contract-version re-exports that track the bean constants they mirror, and an asset-URL composition
 * producing the same version+content-hash cache-buster shape the views mixin does.
 */
@SuppressWarnings({
	"resource" // Closeable test fixtures held in static fields; lifecycle managed by the test/framework, not a real leak.
})
class WidgetsMixin_Serving_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Hosts
	//------------------------------------------------------------------------------------------------------------------

	public static class NoMixin extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/items") public String items() { return "items"; }
	}

	@Rest(mixins=WidgetsMixin.class)
	public static class WithMixin extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/items") public String items() { return "items"; }
	}

	/** Echoes the request-aware asset-URL overload's resolved URL as plain text, for assertion below. */
	@Rest(mixins=WidgetsMixin.class)
	public static class AssetUrlHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/echo-asset-url") public String echoAssetUrl(RestRequest req) {
			return WidgetsMixin.assetUrl(req, WidgetsMixin.CARDS_JS_PATH, PROBE_RESOURCE);
		}
	}

	/**
	 * A stable, guaranteed-present classpath resource on this module's own classpath, used to exercise the asset-URL
	 * composition and the content-hash cache-buster independently of any one shipped asset's bytes.
	 */
	private static final String PROBE_RESOURCE = "/org/apache/juneau/rest/server/widgets/WidgetsMixin.class";

	/** Every asset path this mixin declares, paired with the classpath resource it must serve. */
	private static final java.util.Map<String,String> DECLARED_ASSETS = java.util.Map.of(
		WidgetsMixin.CARDS_JS_PATH, "/org/apache/juneau/widgets/juneau-cards.js",
		WidgetsMixin.CALENDAR_JS_PATH, "/org/apache/juneau/widgets/juneau-calendar.js",
		WidgetsMixin.CALENDAR_CSS_PATH, "/org/apache/juneau/widgets/juneau-calendar.css",
		WidgetsMixin.CHROME_JS_PATH, "/org/apache/juneau/widgets/juneau-chrome.js");

	/** Every asset path this mixin declares. */
	private static final String[] DECLARED_PATHS = {
		WidgetsMixin.CARDS_JS_PATH, WidgetsMixin.CALENDAR_JS_PATH, WidgetsMixin.CALENDAR_CSS_PATH,
		WidgetsMixin.CHROME_JS_PATH};

	private static final MockRestClient cNoMixin = MockRestClient.buildLax(NoMixin.class);
	private static final MockRestClient cWithMixin = MockRestClient.buildLax(WithMixin.class);

	//------------------------------------------------------------------------------------------------------------------
	// a) The mixin loads, and composing it serves every asset it ships
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_hostComposingTheMixin_bootsAndKeepsItsOwnRoutes() throws Exception {
		cWithMixin.get("/items").accept("application/json").run().assertStatus(200).assertContent().asString().isContains("items");
	}

	/**
	 * The relocated bytes are on <b>this</b> module's classpath and served at this mixin's declared paths.  Pinning
	 * the response to the classpath bytes rather than to a marker string is what makes this a byte-level ownership
	 * assertion: a truncated or stale copy could not pass it.
	 */
	@Test void a02_composingTheMixin_servesEveryDeclaredAssetFromThisModulesClasspath() throws Exception {
		for (var e : DECLARED_ASSETS.entrySet()) {
			var path = e.getKey();
			var served = cWithMixin.get(path).run()
				.assertStatus(200)
				.assertHeader("Content-Type").isContains(path.endsWith(".css") ? "text/css" : "text/javascript")
				.assertHeader("Cache-Control").isContains("max-age")
				.getContent().asBytes();
			assertArrayEquals(classpathBytes(e.getValue()), served, () -> path + ": served body is not this module's classpath bytes");
		}
	}

	@Test void a03_hostWithoutTheMixin_answers404OnThoseSamePaths() throws Exception {
		for (var path : DECLARED_PATHS)
			cNoMixin.get(path).run().assertStatus(404);
	}

	private static byte[] classpathBytes(String resource) throws Exception {
		try (var in = WidgetsMixin.class.getResourceAsStream(resource)) {
			assertNotNull(in, () -> "missing classpath resource: " + resource);
			return in.readAllBytes();
		}
	}

	@Test void a04_unknownPath_is404_withOrWithoutTheMixin() throws Exception {
		cWithMixin.get("/juneau-not-a-widget-asset.js").run().assertStatus(404);
		cNoMixin.get("/juneau-not-a-widget-asset.js").run().assertStatus(404);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) Contract-version re-exports equal the bean constants they mirror
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_cardsContractVersion_equalsCardFieldListContractVersion() {
		assertEquals(CardFieldList.CONTRACT_VERSION, WidgetsMixin.CARDS_CONTRACT_VERSION);
	}

	@Test void b02_calendarContractVersion_equalsCalendarDefContractVersion() {
		assertEquals(CalendarDef.CONTRACT_VERSION, WidgetsMixin.CALENDAR_CONTRACT_VERSION);
	}

	@Test void b03_headerContractVersion_equalsAppHeaderDefContractVersion() {
		assertEquals(AppHeaderDef.CONTRACT_VERSION, WidgetsMixin.HEADER_CONTRACT_VERSION);
	}

	@Test void b04_barContractVersion_equalsBarSlotContractVersion() {
		assertEquals(BarSlot.CONTRACT_VERSION, WidgetsMixin.BAR_CONTRACT_VERSION);
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) widgetAssetUrl(...) resolves every path this mixin ships, and fails loud for anything else
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_widgetAssetUrl_resolvesEveryDeclaredPath() {
		for (var path : DECLARED_PATHS) {
			var url = WidgetsMixin.widgetAssetUrl(path);
			assertTrue(url.startsWith("servlet:" + path + "?v="), url);
		}
	}

	/** The buster is the commons content hash of the very bytes the endpoint serves, for every declared path. */
	@Test void c02_widgetAssetUrl_busterIsTheContentHashOfTheServedBytes() throws Exception {
		for (var e : DECLARED_ASSETS.entrySet()) {
			var expected = ChecksumUtils.hash8(classpathBytes(e.getValue()));
			var url = WidgetsMixin.widgetAssetUrl(e.getKey());
			assertTrue(url.endsWith("-" + expected), () -> e.getKey() + ": expected suffix '-" + expected + "' in '" + url + "'");
		}
	}

	@Test void c03_widgetAssetUrl_rejectsAnUnknownPath() {
		assertThrows(IllegalArgumentException.class, () -> WidgetsMixin.widgetAssetUrl("/juneau-not-a-widget-asset.js"));
	}

	@Test void c04_requestAwareOverload_rejectsAnUnknownPathBeforeTouchingTheRequest() {
		assertThrows(IllegalArgumentException.class, () -> WidgetsMixin.widgetAssetUrl(null, "/juneau-not-a-widget-asset.js"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// d) Asset-URL composition + content-hash cache-buster (shared with the views mixin via ClasspathAssetCache)
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_assetUrl_carriesVersionAndContentHashCacheBuster() {
		var v = WidgetsMixin.class.getPackage().getImplementationVersion();
		var expectedPrefix = "servlet:" + WidgetsMixin.CARDS_JS_PATH + "?v=" + (v == null ? "dev" : v) + "-";
		var url = WidgetsMixin.assetUrl(WidgetsMixin.CARDS_JS_PATH, PROBE_RESOURCE);
		assertTrue(url.startsWith(expectedPrefix), url);
		assertTrue(url.substring(expectedPrefix.length()).matches("[0-9a-f]{8}"), url);
	}

	@Test void d02_assetUrl_contentHash_isStableForIdenticalContent_andTracksTheBytes() {
		var url1 = WidgetsMixin.assetUrl(WidgetsMixin.CARDS_JS_PATH, PROBE_RESOURCE);
		var url2 = WidgetsMixin.assetUrl(WidgetsMixin.CARDS_JS_PATH, PROBE_RESOURCE);
		assertEquals(url1, url2, "hash must be stable across repeated calls for identical content");

		// A different resource's bytes must produce a different hash - i.e. the buster tracks content, not the path.
		var other = WidgetsMixin.assetUrl(WidgetsMixin.CARDS_JS_PATH, "/org/apache/juneau/rest/server/widgets/CalendarDef.class");
		assertNotEquals(url1, other, "the cache-buster must change when the resource's bytes change");
	}

	@Test void d03_assetUrl_contentHash_isTheSharedCommonsHashOfTheServedBytes() {
		// The hash is not a private re-implementation: it is the commons content-hash function applied to exactly the
		// bytes this module would serve, which is what keeps the two modules' cache-buster URLs directly comparable.
		var bytes = new ClasspathAssetCache(WidgetsMixin.class).bytes(PROBE_RESOURCE);
		var url = WidgetsMixin.assetUrl(WidgetsMixin.CARDS_JS_PATH, PROBE_RESOURCE);
		assertTrue(url.endsWith("-" + ChecksumUtils.hash8(bytes)), url);
	}

	@Test void d04_requestAwareOverload_resolvesToARealUrl_notAServletPrefixedLiteral() throws Exception {
		var url = MockRestClient.buildLax(AssetUrlHost.class).get("/echo-asset-url").accept("text/plain").run()
			.assertStatus(200).getContent().asString();
		assertFalse(url.startsWith("servlet:"), url);
		var staticForm = WidgetsMixin.assetUrl(WidgetsMixin.CARDS_JS_PATH, PROBE_RESOURCE);
		assertTrue(url.endsWith(staticForm.substring(staticForm.indexOf("?v="))), url);
	}

	@Test void d05_requestAwareOverload_resolvesUnderANonRootHostMount() throws Exception {
		var c = MockRestClient.createLax(AssetUrlHost.class).servletPath("/rest/admin").build();
		var url = c.get("/echo-asset-url").accept("text/plain").run().assertStatus(200).getContent().asString();
		assertTrue(url.startsWith("/rest/admin" + WidgetsMixin.CARDS_JS_PATH), url);
	}

	@Test void d06_moduleCache_isAnchoredOnThisModule_notASharedSingleton() {
		// Each module holds its own cache instance anchored on its own class, so a module's cache-buster resolves
		// that module's implementation version rather than another module's.
		var a = new ClasspathAssetCache(WidgetsMixin.class);
		var b = new ClasspathAssetCache(WidgetsMixin.class);
		assertNotSame(a, b);
		assertEquals(a.buildVersion(), b.buildVersion());
		assertEquals(a.hash(PROBE_RESOURCE), b.hash(PROBE_RESOURCE), "independent caches must agree on content hashes");
	}
}
