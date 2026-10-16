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
package org.apache.juneau.petstore.jetty;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import java.net.http.HttpResponse.*;
import java.time.*;
import java.util.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.petstore.console.*;
import org.apache.juneau.testing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import jakarta.servlet.DispatcherType;

/**
 * Integration test that boots the petstore Jetty deployment on an ephemeral port and exercises the CRUD surface
 * + content-negotiation through a real HTTP client.
 *
 * <p>
 * Tagged {@code @JettyMicroserviceTest} (which composes {@code @Tag("container")} + {@code @Tag("jetty")}) so it
 * skips under {@code scripts/test.py --no-container}.
 */
@JettyMicroserviceTest
@SuppressWarnings({
	"java:S2925", // warmUpServer() readiness loop needs a back-off between retries; without it a ConnectException would busy-spin. No event/latch to await and Awaitility isn't on the test classpath.
	"java:S8692" // warmUpServer() polls a real HTTP server against a genuine wall-clock deadline; a fixed clock would break the retry loop.
})
class PetstoreJetty_Test extends TestBase {

	@RegisterExtension
	static MicroserviceTestFixture fixture = MicroserviceTestFixture.create()
		.configurations(App.AppConfig.class);

	private static final HttpClient HTTP = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(5))
		.followRedirects(HttpClient.Redirect.NEVER)
		.build();

	/**
	 * Primes the root endpoint before the timed test methods run.
	 *
	 * <p>
	 * The fixture's {@code beforeAll} only waits for the Jetty connector to bind — not for the REST servlet to
	 * initialize. The first request to a Juneau REST resource triggers one-time {@code RestContext} setup
	 * (serializer/parser metadata, {@code HtmlDocSerializer} construction) which, under a loaded CI agent, can
	 * exceed the tight 10s per-request timeout the test methods use. Absorbing that cold-start here (with a
	 * generous budget + retry) removes the startup race from {@code a01} while keeping the per-test timeouts tight.
	 */
	@BeforeAll
	static void warmUpServer() throws Exception {
		var deadline = Instant.now().plusSeconds(30);
		Exception last = null;
		while (Instant.now().isBefore(deadline)) {
			try {
				var req = HttpRequest.newBuilder()
					.uri(URI.create(fixture.getRootUrl() + "/"))
					.timeout(Duration.ofSeconds(20))
					.header("Accept", "text/html")
					.GET()
					.build();
				// GET / is a 303 to /console/store (see a01); redirects aren't followed.
				if (HTTP.send(req, BodyHandlers.ofString()).statusCode() == 303)
					return;
			} catch (HttpTimeoutException | ConnectException e) {
				last = e;
			}
			Thread.sleep(250);
		}
		throw new IllegalStateException("Petstore Jetty server did not become ready within 30s", last);
	}

	private static HttpResponse<String> get(String path, String accept) throws Exception {
		var req = HttpRequest.newBuilder()
			.uri(URI.create(fixture.getRootUrl() + path))
			.timeout(Duration.ofSeconds(30))
			.header("Accept", accept)
			.GET()
			.build();
		return HTTP.send(req, BodyHandlers.ofString());
	}

	private static HttpResponse<String> getWithAuth(String path, String accept, String authValue) throws Exception {
		var req = HttpRequest.newBuilder()
			.uri(URI.create(fixture.getRootUrl() + path))
			.timeout(Duration.ofSeconds(30))
			.header("Accept", accept)
			.header("Authorization", authValue)
			.GET()
			.build();
		return HTTP.send(req, BodyHandlers.ofString());
	}

	private static HttpResponse<String> post(String path, String contentType, String body) throws Exception {
		var req = HttpRequest.newBuilder()
			.uri(URI.create(fixture.getRootUrl() + path))
			.timeout(Duration.ofSeconds(30))
			.header("Content-Type", contentType)
			.header("Accept", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(body))
			.build();
		return HTTP.send(req, BodyHandlers.ofString());
	}

	private static HttpResponse<String> delete(String path) throws Exception {
		var req = HttpRequest.newBuilder()
			.uri(URI.create(fixture.getRootUrl() + path))
			.timeout(Duration.ofSeconds(30))
			.DELETE()
			.build();
		return HTTP.send(req, BodyHandlers.ofString());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a — root router page
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_rootRedirectsToConsole() throws Exception {
		var resp = get("/", "text/html");
		assertEquals(303, resp.statusCode(), "body: " + resp.body());
		var location = resp.headers().firstValue("Location").orElse("");
		assertTrue(location.endsWith("/console/store"), "Location was: " + location);
	}

	/**
	 * Confirms the console store page renders in the console chrome.  (The old router page's
	 * {@code $C{Petstore/appName}} header check left with that page; the console brand is static.)
	 */
	@Test void a02_consoleStoreRenders() throws Exception {
		var resp = get("/console/store", "text/html");
		assertEquals(200, resp.statusCode(), "body: " + resp.body());
		var body = resp.body();
		assertTrue(body.contains("Juneau Petstore"), "expected console brand: " + body);
		assertTrue(body.contains("id=\"juneau-page\""), "expected the page contract element: " + body);
	}

	/**
	 * The console pages link their chrome assets at context-root {@code /juneau-console/*} URLs and their views
	 * toolkit assets page-relative; both must be served for the console to style and start in a browser.
	 */
	@ParameterizedTest(name="{0}")
	@ValueSource(strings={
		"/juneau-console/chrome.css",
		"/juneau-console/themes/juneau-theme-open.css",
		"/juneau-console/juneau-console.js",
		"/console/store/juneau-views.js",
		"/console/ops/audit/juneau-datatables.js",
		"/console/ops/audit/webjars/jquery/3.7.1/jquery.min.js",
		"/console/ops/audit/webjars/datatables.net/2.3.8/js/dataTables.min.js"
	})
	void a03_consoleAssetIsServed(String path) throws Exception {
		assertEquals(200, get(path, "*/*").statusCode(), path);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b — petstore CRUD over real HTTP, JSON
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_listPets_jsonContainsSeed() throws Exception {
		var resp = get("/petstore/pets", "application/json");
		assertEquals(200, resp.statusCode(), "body: " + resp.body());
		var ct = resp.headers().firstValue("Content-Type").orElse("");
		assertTrue(ct.startsWith("application/json"), "Content-Type was: " + ct);
		assertTrue(resp.body().contains("Mr. Frisky"), "expected seeded pet name in response: " + resp.body());
	}

	@Test void b02_getPet_byId_json() throws Exception {
		var resp = get("/petstore/pets/1", "application/json");
		assertEquals(200, resp.statusCode(), "body: " + resp.body());
		assertTrue(resp.body().contains("\"name\":\"Mr. Frisky\""), resp.body());
	}

	@Test void b03_getPet_unknown_404() throws Exception {
		var resp = get("/petstore/pets/99999", "application/json");
		assertEquals(404, resp.statusCode(), "body: " + resp.body());
	}

	@Test void b04_createPet_thenGet_roundTrip() throws Exception {
		var body = "{\"name\":\"Wanda\",\"species\":\"RABBIT\",\"price\":12.50,\"status\":\"AVAILABLE\"}";
		var post = post("/petstore/pets", "application/json", body);
		assertEquals(200, post.statusCode(), "post body: " + post.body());
		// Extract id from posted response (assigned server-side).
		var id = extractLong(post.body(), "\"id\":");
		assertNotEquals(0L, id, "server-assigned id missing in: " + post.body());

		var get = get("/petstore/pets/" + id, "application/json");
		assertEquals(200, get.statusCode(), "get body: " + get.body());
		assertTrue(get.body().contains("\"name\":\"Wanda\""), get.body());
	}

	@Test void b05_deletePet_removes() throws Exception {
		// Seed has pet id=2; delete then verify 404.
		var del = delete("/petstore/pets/2");
		assertEquals(200, del.statusCode(), "delete body: " + del.body());
		var get = get("/petstore/pets/2", "application/json");
		assertEquals(404, get.statusCode());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c — content negotiation
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_listPets_xml() throws Exception {
		var resp = get("/petstore/pets", "text/xml");
		assertEquals(200, resp.statusCode(), "body: " + resp.body());
		var ct = resp.headers().firstValue("Content-Type").orElse("");
		assertTrue(ct.startsWith("text/xml"), "Content-Type was: " + ct);
		assertTrue(resp.body().contains("Mr. Frisky"), resp.body());
	}

	@Test void c02_listPets_html() throws Exception {
		var resp = get("/petstore/pets", "text/html");
		assertEquals(200, resp.statusCode(), "body: " + resp.body());
		var ct = resp.headers().firstValue("Content-Type").orElse("");
		assertTrue(ct.startsWith("text/html"), "Content-Type was: " + ct);
		assertTrue(resp.body().contains("Mr. Frisky"), resp.body());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d — rendering flavors (parity with springboot deployment — both inherit from core)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_mustacheFlavorFragment() throws Exception {
		var resp = get("/console/dev/flavors/mustache/fragment", "text/html");
		assertEquals(200, resp.statusCode(), "body: " + resp.body());
		assertContainsAll(resp.body(), "data-flavor=\"mustache\"", "Mr. Frisky");
	}

	@Test void d02_freemarkerFlavorFragment() throws Exception {
		var resp = get("/console/dev/flavors/freemarker/fragment", "text/html");
		assertEquals(200, resp.statusCode(), "body: " + resp.body());
		assertContainsAll(resp.body(), "data-flavor=\"freemarker\"", "Mr. Frisky");
	}

	@Test void d03_flavorPageRendersInConsole() throws Exception {
		var resp = get("/console/dev/flavors/html", "text/html");
		assertEquals(200, resp.statusCode(), "body: " + resp.body());
		assertContainsAll(resp.body(), "/console/dev/flavors/html/fragment", "View source");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e — BearerTokenGuard gating /console/dev/secure/api/* (parity with springboot deployment)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void e01_secureEndpoint_noAuth_401() throws Exception {
		var resp = get("/console/dev/secure/api/pets", "application/json");
		assertEquals(401, resp.statusCode(), "body: " + resp.body());
		var challenge = resp.headers().firstValue("WWW-Authenticate").orElse("");
		assertTrue(challenge.contains("Bearer"), "WWW-Authenticate should advertise Bearer scheme: " + challenge);
		assertContains("realm=\"petstore\"", challenge);
	}

	@Test void e02_secureEndpoint_validToken_200() throws Exception {
		var resp = getWithAuth("/console/dev/secure/api/pets", "application/json", "Bearer petstore-user");
		assertEquals(200, resp.statusCode(), "body: " + resp.body());
		assertTrue(resp.body().contains("Mr. Frisky"), "body: " + resp.body());
	}

	@Test void e03_secureEndpoint_unknownToken_401() throws Exception {
		var resp = getWithAuth("/console/dev/secure/api/pets", "application/json", "Bearer wrong-token");
		assertEquals(401, resp.statusCode(), "body: " + resp.body());
	}

	@Test void e04_secureEndpoint_whoami_returnsPrincipalName() throws Exception {
		var resp = getWithAuth("/console/dev/secure/api/whoami", "application/json", "Bearer petstore-admin");
		assertEquals(200, resp.statusCode(), "body: " + resp.body());
		assertTrue(resp.body().contains("\"name\":\"admin\""), "body: " + resp.body());
	}

	@Test void e05_unsecuredEndpoint_stillOpen() throws Exception {
		// /petstore/* has no guard, so anonymous access still works.
		var resp = get("/petstore/pets/1", "application/json");
		assertEquals(200, resp.statusCode(), "body: " + resp.body());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// f — retired one-off resources (folded into the console; parity with springboot g)
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest(name="{0}")
	@ValueSource(strings={
		"/pet-views/mustache/pets/1/view",
		"/pet-views/freemarker/pets/1/view",
		"/petstore-html/card/1",
		"/petstore-secure/pets",
		"/petstore-info/BeanDescription"
	})
	void f01_retiredPathIs404(String path) throws Exception {
		assertEquals(404, get(path, "text/html").statusCode(), path);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// g — the console CSRF boundary is wired in (parity with springboot h)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void g01_consolePageCarriesTheCsrfToken() throws Exception {
		var resp = get("/console/store", "text/html");
		assertEquals(200, resp.statusCode(), "body: " + resp.body());
		assertTrue(resp.body().matches("(?s).*<body[^>]*\\sdata-juneau-csrf=\"[0-9a-f]{64}\".*"), "no data-juneau-csrf on <body>");
	}

	@Test void g02_consoleWriteWithoutTheTokenIsRefused() throws Exception {
		var req = HttpRequest.newBuilder()
			.uri(URI.create(fixture.getRootUrl() + "/console/pets/1/sell"))
			.timeout(Duration.ofSeconds(30))
			.header("Origin", fixture.getRootUrl().toString())
			.header("Content-Type", "application/json")
			.header("Accept", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString("{}"))
			.build();
		var resp = HTTP.send(req, BodyHandlers.ofString());
		assertEquals(403, resp.statusCode(), "body: " + resp.body());
		assertEquals("CSRF_TOKEN_MISSING", resp.headers().firstValue("X-Request-Boundary").orElse(null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// g — the API's CSRF boundary guards /petstore/*
	//-----------------------------------------------------------------------------------------------------------------

	private static final Pattern API_TOKEN = Pattern.compile("data-petstore-csrf=\"([0-9a-f]{128})\"");
	private static final Pattern CONSOLE_TOKEN = Pattern.compile("<body[^>]*\\sdata-juneau-csrf=\"([0-9a-f]{64})\"");
	private static final String PET_JSON = "{\"name\":\"Gus\",\"species\":\"RABBIT\",\"price\":12.50,\"status\":\"AVAILABLE\"}";

	/** The API token the admin Photo tab stamps on its upload form. */
	private static String apiToken() throws Exception {
		var resp = getWithAuth("/console/pets/1/photo", "text/html", "Bearer petstore-admin");
		assertEquals(200, resp.statusCode(), "body: " + resp.body());
		var m = API_TOKEN.matcher(resp.body());
		assertTrue(m.find(), "no data-petstore-csrf on the admin photo page");
		return m.group(1);
	}

	private static String consoleToken() throws Exception {
		var m = CONSOLE_TOKEN.matcher(get("/console/store", "text/html").body());
		assertTrue(m.find(), "no data-juneau-csrf on the console page");
		return m.group(1);
	}

	private static String boundaryReason(HttpResponse<?> resp) {
		return resp.headers().firstValue("X-Request-Boundary").orElse(null);
	}

	private static HttpResponse<String> send(String method, String path, String contentType, String origin, String token, String body) throws Exception {
		var b = HttpRequest.newBuilder().uri(URI.create(fixture.getRootUrl() + path)).timeout(Duration.ofSeconds(30))
			.header("Accept", "application/json").method(method, HttpRequest.BodyPublishers.ofString(body));
		if (contentType != null)
			b.header("Content-Type", contentType);
		if (origin != null)
			b.header("Origin", origin);
		if (token != null)
			b.header("X-Csrf-Token", token);
		return HTTP.send(b.build(), BodyHandlers.ofString());
	}

	private static String self() {
		return fixture.getRootUrl().toString();
	}

	@Test void g03_apiWriteWithSameOriginAndNoTokenIsRefused() throws Exception {
		var resp = send("POST", "/petstore/pets", "application/json", self(), null, PET_JSON);
		assertEquals(403, resp.statusCode(), "body: " + resp.body());
		assertEquals("CSRF_TOKEN_MISSING", boundaryReason(resp));
	}

	// Shared-instance evidence, not presence evidence: the token the console boundary published is the one the API filter
	// verifies.  Stays green with the API boundary deleted.
	@Test void g04_apiWriteWithTheTokenPasses() throws Exception {
		var resp = send("POST", "/petstore/pets", "application/json", self(), apiToken(), PET_JSON);
		assertEquals(200, resp.statusCode(), "body: " + resp.body());
		assertNull(boundaryReason(resp));
	}

	@Test void g05_apiCrossOriginFormPostIsRefused() throws Exception {
		var resp = send("POST", "/petstore/pets", "application/x-www-form-urlencoded", "http://evil.example", null, "name=Evil&species=CAT&price=1");
		assertEquals(403, resp.statusCode(), "body: " + resp.body());
		assertEquals("ORIGIN_MISMATCH", boundaryReason(resp));
	}

	@Test void g06_apiConsoleTokenIsRefused() throws Exception {
		var resp = send("PUT", "/petstore/pets/1/photo", "image/png", self(), consoleToken(), "x");
		assertEquals(403, resp.statusCode(), "body: " + resp.body());
		assertEquals("CSRF_TOKEN_MISMATCH", boundaryReason(resp));
	}

	// Pins that the API filter does not cover /console (the console boundary alone judges it).  Stays green with the API
	// boundary deleted.
	@Test void g07_consoleIsUnaffected() throws Exception {
		var token = consoleToken();
		var sell = HttpRequest.newBuilder().uri(URI.create(self() + "/console/pets/3/sell")).timeout(Duration.ofSeconds(30))
			.header("Origin", self()).header("X-Csrf-Token", token).header("Authorization", "Bearer petstore-admin")
			.header("Content-Type", "application/json").header("Accept", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString("{}")).build();
		var ok = HTTP.send(sell, BodyHandlers.ofString());
		assertEquals(200, ok.statusCode(), "body: " + ok.body());
		assertNull(boundaryReason(ok));

		// The API's token is not the console's.
		var swapped = HttpRequest.newBuilder().uri(URI.create(self() + "/console/pets/3/sell")).timeout(Duration.ofSeconds(30))
			.header("Origin", self()).header("X-Csrf-Token", apiToken()).header("Authorization", "Bearer petstore-admin")
			.header("Content-Type", "application/json").header("Accept", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString("{}")).build();
		var refused = HTTP.send(swapped, BodyHandlers.ofString());
		assertEquals(403, refused.statusCode(), "body: " + refused.body());
		assertEquals("CSRF_TOKEN_MISMATCH", boundaryReason(refused));
	}

	// Premise pin, not CSRF evidence: stays green with the API boundary deleted.
	@Test void g08_petstoreUiAndAssetsAreOutsideTheApiBoundary() throws Exception {
		var ui = get("/petstore-ui", "text/html");
		assertEquals(200, ui.statusCode(), "body: " + ui.body());
		assertNull(boundaryReason(ui));
		var post = send("POST", "/petstore-ui/x", "application/x-www-form-urlencoded", "http://evil.example", null, "a=b");
		assertNull(boundaryReason(post), "status " + post.statusCode());
	}

	@Test void g09_apiBoundaryIsMappedForRequestOnly() throws Exception {
		var handler = fixture.getJettyServerComponent().getServletContextHandler().getServletHandler();
		var mappings = handler.getFilterMappings();
		var api = -1;
		var console = -1;
		for (var i = 0; i < mappings.length; i++) {
			var filter = handler.getFilter(mappings[i].getFilterName()).getFilter();
			if (filter instanceof PetstoreApiBoundary) {
				api = i;
				assertArrayEquals(new String[]{PetstoreApiBoundary.URL_PATTERN}, mappings[i].getPathSpecs());
				assertEquals(EnumSet.of(DispatcherType.REQUEST), mappings[i].getDispatcherTypes());
			} else if (filter instanceof PetstoreConsoleBoundary) {
				console = i;
			}
		}
		assertTrue(api >= 0, "no PetstoreApiBoundary mapped");
		assertTrue(console >= 0 && console < api, "console boundary must be ahead of the API boundary");
		var registrations = ServletFilterRegistration.collect(fixture.getMicroservice().getBeanStore()).stream()
			.filter(r -> r.getFilter() instanceof PetstoreApiBoundary).toList();
		assertEquals(1, registrations.size());
		assertEquals(ServletFilterRegistration.HIGHEST_PRECEDENCE + 10, registrations.get(0).getOrder());
	}

	// Premise pin, not CSRF evidence: stays green with the API boundary deleted.
	@ParameterizedTest(name="{0}")
	@CsvSource({"/petstore,404", "/petstore/pets/1,200", "/petstore/pets,200", "/petstore/api,200", "/petstore/stats,200"})
	void g10_apiHtmlPagesRenderNoPostForm(String path, int status) throws Exception {
		// The bare /petstore path has no HTML page (404 for text/html), so its premise is that the error body has no POST form either.
		var resp = get(path, "text/html");
		assertEquals(status, resp.statusCode(), "body: " + resp.body());
		assertFalse(resp.body().toLowerCase().contains("method=\"post\""), path + " renders a POST form");
	}

	@Test void g13_apiSimpleFormPostWithoutOriginIsRefused() throws Exception {
		var resp = send("POST", "/petstore/pets", "application/x-www-form-urlencoded", null, null, "name=Legacy&species=CAT&price=1");
		assertEquals(403, resp.statusCode(), "body: " + resp.body());
		assertEquals("SIMPLE_REQUEST_WITHOUT_ORIGIN", boundaryReason(resp));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// helpers
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Cheap brittle JSON int extractor — finds the first occurrence of {@code key} (e.g. {@code "id":}) and reads
	 * the integer that follows. Sufficient for the simple CRUD round-trip assertions; we don't pull in a full JSON
	 * parser just for this test class.
	 */
	private static long extractLong(String body, String key) {
		var i = body.indexOf(key);
		if (i < 0)
			return 0L;
		i += key.length();
		var end = i;
		while (end < body.length() && (Character.isDigit(body.charAt(end)) || body.charAt(end) == '-'))
			end++;
		if (end == i)
			return 0L;
		return Long.parseLong(body.substring(i, end));
	}
}
