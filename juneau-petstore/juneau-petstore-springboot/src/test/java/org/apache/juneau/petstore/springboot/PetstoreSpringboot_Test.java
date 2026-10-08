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
package org.apache.juneau.petstore.springboot;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.net.http.HttpResponse.*;
import java.time.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.context.SpringBootTest.*;
import org.springframework.boot.test.web.server.*;

/**
 * Integration test that boots the petstore Spring Boot deployment on a random port and exercises the CRUD
 * surface plus the Spring {@code @Autowired} injection demo.
 *
 * <p>
 * Tagged {@code @SpringbootTest} ({@code @Tag("container")} + {@code @Tag("springboot")}) so it skips under
 * {@code scripts/test.py --no-container}.
 *
 * <p>
 * Asserts the parity story: the same CRUD surface from {@code juneau-petstore-core} that the Jetty deployment
 * mounts is reachable identically here, plus the Spring-injected {@link HelloResource} demonstrates the
 * deployment-specific {@code @Autowired} pattern.
 */
@org.apache.juneau.testing.SpringbootTest
@SpringBootTest(classes = App.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@SuppressWarnings({
	"java:S2925", // warmUpServer() readiness loop needs a back-off between retries; without it a ConnectException would busy-spin. No event/latch to await and Awaitility isn't on the test classpath.
	"java:S8692" // warmUpServer() polls a real HTTP server against a genuine wall-clock deadline; a fixed clock would break the retry loop.
})
class PetstoreSpringboot_Test {

	@LocalServerPort
	int port;

	private static final HttpClient HTTP = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(5))
		.followRedirects(HttpClient.Redirect.NEVER)
		.build();

	private static volatile boolean warmedUp;

	private static final int READY_TIMEOUT_SECONDS = Integer.parseInt(
		System.getProperty("juneau.petstore.springboot.readyTimeoutSeconds", "180"));

	/**
	 * Primes the root endpoint before the timed test methods run.
	 *
	 * <p>
	 * Spring Boot's {@code RANDOM_PORT} environment only waits for the Tomcat connector to bind — not for the Juneau
	 * REST servlet to initialize. The first request to the group resource ({@code /}) forces one-time
	 * {@code RestContext} setup of the root <i>and all of its child resources</i> (serializer/parser metadata,
	 * {@code HtmlDocSerializer} construction) in a single request. Under a loaded CI agent this cold start can exceed
	 * the per-request timeout the test methods use (it has timed out at exactly 10s under a loaded CI run before). Absorbing that
	 * startup cost here once (with a generous budget + retry) removes the race from {@code a01} while keeping the
	 * per-test timeouts tight. Mirrors the same guard in {@code PetstoreJetty_Test}.
	 *
	 * <p>
	 * The overall deadline is deliberately generous (default {@value #READY_TIMEOUT_SECONDS}s, configurable via the
	 * {@code juneau.petstore.springboot.readyTimeoutSeconds} system property) because under a full-reactor build the
	 * whole JVM fleet is CPU-starved and Spring Boot startup itself can take far longer than it does in isolation.
	 * Each probe still uses a short per-attempt request timeout so a slow-to-bind server fails fast and gets retried
	 * instead of burning the whole budget on one stuck request.
	 */
	@BeforeEach
	void warmUpServer() throws Exception {
		if (warmedUp)
			return;
		var deadline = Instant.now().plusSeconds(READY_TIMEOUT_SECONDS);
		Exception last = null;
		while (Instant.now().isBefore(deadline)) {
			try {
				var req = HttpRequest.newBuilder()
					.uri(URI.create("http://localhost:" + port + "/"))
					.timeout(Duration.ofSeconds(5))
					.header("Accept", "text/html")
					.GET()
					.build();
				// GET / is a 303 to /console/store (see a01); redirects aren't followed.
				if (HTTP.send(req, BodyHandlers.ofString()).statusCode() == 303) {
					warmedUp = true;
					return;
				}
			} catch (IOException e) {
				last = e;
			}
			Thread.sleep(500);
		}
		throw new IllegalStateException(
			"Petstore Spring Boot server did not become ready within " + READY_TIMEOUT_SECONDS + "s", last);
	}

	private HttpResponse<String> get(String path, String accept) throws Exception {
		var req = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port + path))
			.timeout(Duration.ofSeconds(30))
			.header("Accept", accept)
			.GET()
			.build();
		return HTTP.send(req, BodyHandlers.ofString());
	}

	private HttpResponse<String> getWithAuth(String path, String accept, String authValue) throws Exception {
		var req = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port + path))
			.timeout(Duration.ofSeconds(30))
			.header("Accept", accept)
			.header("Authorization", authValue)
			.GET()
			.build();
		return HTTP.send(req, BodyHandlers.ofString());
	}

	private HttpResponse<String> post(String path, String contentType, String body) throws Exception {
		var req = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port + path))
			.timeout(Duration.ofSeconds(30))
			.header("Content-Type", contentType)
			.header("Accept", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(body))
			.build();
		return HTTP.send(req, BodyHandlers.ofString());
	}

	private HttpResponse<String> delete(String path) throws Exception {
		var req = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port + path))
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

	@Test void a02_consoleStoreRenders() throws Exception {
		var resp = get("/console/store", "text/html");
		assertEquals(200, resp.statusCode(), "body: " + resp.body());
		// Fail fast if the page rendered an inline server error inside a 200 body.
		assertFalse(resp.body().contains("HTTP 500"), "console page rendered an inline server error: " + resp.body());
		assertTrue(resp.body().contains("Juneau Petstore"), "expected console brand: " + resp.body());
		assertTrue(resp.body().contains("id=\"juneau-page\""), "expected the page contract element: " + resp.body());
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
	// b — petstore CRUD over real HTTP
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_listPets_jsonContainsSeed() throws Exception {
		var resp = get("/petstore/pets", "application/json");
		assertEquals(200, resp.statusCode(), "body: " + resp.body());
		assertTrue(resp.body().contains("Mr. Frisky"), "expected seeded pet name in response: " + resp.body());
	}

	@Test void b01b_seededStoreReachedTheChild() throws Exception {
		// Pet 500 exists only in the seeded store, so a 200 proves the Spring PetStore bean reached the child resource.
		var resp = get("/petstore/pets/500", "application/json");
		assertEquals(200, resp.statusCode(), "body: " + resp.body());
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
		var id = extractLong(post.body(), "\"id\":");
		assertNotEquals(0L, id, "server-assigned id missing in: " + post.body());

		var get = get("/petstore/pets/" + id, "application/json");
		assertEquals(200, get.statusCode(), "get body: " + get.body());
		assertTrue(get.body().contains("\"name\":\"Wanda\""), get.body());
	}

	@Test void b05_deletePet_removes() throws Exception {
		var del = delete("/petstore/pets/2");
		assertEquals(200, del.statusCode(), "delete body: " + del.body());
		var get = get("/petstore/pets/2", "application/json");
		assertEquals(404, get.statusCode());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c — content negotiation parity with the Jetty deployment
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_listPets_xml() throws Exception {
		var resp = get("/petstore/pets", "text/xml");
		assertEquals(200, resp.statusCode(), "body: " + resp.body());
		assertTrue(resp.body().contains("Mr. Frisky"), resp.body());
	}

	@Test void c02_listPets_html() throws Exception {
		var resp = get("/petstore/pets", "text/html");
		assertEquals(200, resp.statusCode(), "body: " + resp.body());
		assertTrue(resp.body().contains("Mr. Frisky"), resp.body());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d — Spring @Autowired injection demo
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_helloResource_returnsInjectedMessage() throws Exception {
		var resp = get("/hello", "text/plain");
		assertEquals(200, resp.statusCode(), "body: " + resp.body());
		assertTrue(resp.body().contains("Hello from Spring-injected bean!"),
			"expected injected message in response: " + resp.body());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e — rendering flavors (parity with jetty deployment — both inherit from core)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void e01_mustacheFlavorFragment() throws Exception {
		var resp = get("/console/dev/flavors/mustache/fragment", "text/html");
		assertEquals(200, resp.statusCode(), "body: " + resp.body());
		assertContainsAll(resp.body(), "data-flavor=\"mustache\"", "Mr. Frisky");
	}

	@Test void e02_freemarkerFlavorFragment() throws Exception {
		var resp = get("/console/dev/flavors/freemarker/fragment", "text/html");
		assertEquals(200, resp.statusCode(), "body: " + resp.body());
		assertContainsAll(resp.body(), "data-flavor=\"freemarker\"", "Mr. Frisky");
	}

	@Test void e03_flavorPageRendersInConsole() throws Exception {
		var resp = get("/console/dev/flavors/html", "text/html");
		assertEquals(200, resp.statusCode(), "body: " + resp.body());
		assertContainsAll(resp.body(), "/console/dev/flavors/html/fragment", "View source");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// f — BearerTokenGuard gating /console/dev/secure/api/* (parity with jetty deployment)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f01_secureEndpoint_noAuth_401() throws Exception {
		var resp = get("/console/dev/secure/api/pets", "application/json");
		assertEquals(401, resp.statusCode(), "body: " + resp.body());
		var challenge = resp.headers().firstValue("WWW-Authenticate").orElse("");
		assertTrue(challenge.contains("Bearer"), "WWW-Authenticate should advertise Bearer scheme: " + challenge);
		assertContains("realm=\"petstore\"", challenge);
	}

	@Test void f02_secureEndpoint_validToken_200() throws Exception {
		var resp = getWithAuth("/console/dev/secure/api/pets", "application/json", "Bearer petstore-user");
		assertEquals(200, resp.statusCode(), "body: " + resp.body());
		assertTrue(resp.body().contains("Mr. Frisky"), "body: " + resp.body());
	}

	@Test void f03_secureEndpoint_unknownToken_401() throws Exception {
		var resp = getWithAuth("/console/dev/secure/api/pets", "application/json", "Bearer wrong-token");
		assertEquals(401, resp.statusCode(), "body: " + resp.body());
	}

	@Test void f04_secureEndpoint_whoami_returnsPrincipalName() throws Exception {
		var resp = getWithAuth("/console/dev/secure/api/whoami", "application/json", "Bearer petstore-admin");
		assertEquals(200, resp.statusCode(), "body: " + resp.body());
		assertTrue(resp.body().contains("\"name\":\"admin\""), "body: " + resp.body());
	}

	@Test void f05_unsecuredEndpoint_stillOpen() throws Exception {
		var resp = get("/petstore/pets/1", "application/json");
		assertEquals(200, resp.statusCode(), "body: " + resp.body());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// g — retired one-off resources (parity with jetty f)
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest(name="{0}")
	@ValueSource(strings={
		"/pet-views/mustache/pets/1/view",
		"/pet-views/freemarker/pets/1/view",
		"/petstore-html/card/1",
		"/petstore-secure/pets",
		"/petstore-info/BeanDescription"
	})
	void g01_retiredPathIs404(String path) throws Exception {
		assertEquals(404, get(path, "text/html").statusCode(), path);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// helpers
	//-----------------------------------------------------------------------------------------------------------------

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
