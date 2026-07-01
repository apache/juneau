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

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import java.net.http.HttpResponse.*;
import java.time.*;

import org.apache.juneau.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.testing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;

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
	"java:S8692", // warmUpServer() polls a real HTTP server against a genuine wall-clock deadline; a fixed clock would break the retry loop.
	"java:S2925" // warmUpServer() readiness loop needs a back-off between retries; without it a ConnectException would busy-spin. No event/latch to await and Awaitility isn't on the test classpath.
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
				if (HTTP.send(req, BodyHandlers.ofString()).statusCode() == 200)
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

	@Test void a01_rootRendersHtml() throws Exception {
		var resp = get("/", "text/html");
		assertEquals(200, resp.statusCode(), "body: " + resp.body());
		var body = resp.body();
		assertTrue(body.contains("petstore"), "expected petstore link on root: " + body);
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
	// d — view-engine demos (parity with springboot deployment — both inherit from core)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_mustacheView_rendersPet() throws Exception {
		var resp = get("/pet-views/mustache/pets/1/view", "text/html");
		assertEquals(200, resp.statusCode(), "body: " + resp.body());
		var ct = resp.headers().firstValue("Content-Type").orElse("");
		assertTrue(ct.startsWith("text/html"), "Content-Type was: " + ct);
		assertTrue(resp.body().contains("Mr. Frisky"), "expected pet name in mustache view: " + resp.body());
		assertTrue(resp.body().contains("Rendered via Mustache."), "expected mustache marker: " + resp.body());
	}

	@Test void d02_freemarkerView_rendersPet() throws Exception {
		var resp = get("/pet-views/freemarker/pets/1/view", "text/html");
		assertEquals(200, resp.statusCode(), "body: " + resp.body());
		var ct = resp.headers().firstValue("Content-Type").orElse("");
		assertTrue(ct.startsWith("text/html"), "Content-Type was: " + ct);
		assertTrue(resp.body().contains("Mr. Frisky"), "expected pet name in freemarker view: " + resp.body());
		assertTrue(resp.body().contains("Rendered via FreeMarker."), "expected freemarker marker: " + resp.body());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e — AuthFilterChain gating /petstore-secure/* (parity with springboot deployment)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void e01_secureEndpoint_noAuth_401() throws Exception {
		var resp = get("/petstore-secure/pets", "application/json");
		assertEquals(401, resp.statusCode(), "body: " + resp.body());
		var challenge = resp.headers().firstValue("WWW-Authenticate").orElse("");
		assertTrue(challenge.contains("Bearer"), "WWW-Authenticate should advertise Bearer scheme: " + challenge);
	}

	@Test void e02_secureEndpoint_validToken_200() throws Exception {
		var resp = getWithAuth("/petstore-secure/pets", "application/json", "Bearer petstore-user");
		assertEquals(200, resp.statusCode(), "body: " + resp.body());
		assertTrue(resp.body().contains("Mr. Frisky"), "body: " + resp.body());
	}

	@Test void e03_secureEndpoint_unknownToken_401() throws Exception {
		var resp = getWithAuth("/petstore-secure/pets", "application/json", "Bearer wrong-token");
		assertEquals(401, resp.statusCode(), "body: " + resp.body());
	}

	@Test void e04_secureEndpoint_whoami_returnsPrincipalName() throws Exception {
		var resp = getWithAuth("/petstore-secure/whoami", "application/json", "Bearer petstore-admin");
		assertEquals(200, resp.statusCode(), "body: " + resp.body());
		assertTrue(resp.body().contains("\"name\":\"admin\""), "body: " + resp.body());
	}

	@Test void e05_unsecuredEndpoint_stillOpen() throws Exception {
		// /petstore/* is NOT in the chain pattern, so anonymous access still works.
		var resp = get("/petstore/pets/1", "application/json");
		assertEquals(200, resp.statusCode(), "body: " + resp.body());
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
