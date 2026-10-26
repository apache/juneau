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
package org.apache.juneau.petstore.console;

import static org.apache.juneau.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.net.http.HttpRequest.*;
import java.net.http.HttpResponse.*;
import java.time.*;
import java.util.regex.*;

import javax.imageio.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.petstore.console.browser.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;

/**
 * The console's CSRF boundary on a real server: console pages carry the token, console writes without it (or from
 * another origin) are refused before they reach the resource, and the {@code /petstore} API stays outside this
 * boundary.
 */
class PetstoreConsoleBoundary_Test extends TestBase {

	@RegisterExtension
	static MicroserviceTestFixture server = PetstoreTestServer.fixture();

	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
	private static final Pattern TOKEN = Pattern.compile("<body[^>]*\\sdata-juneau-csrf=\"([^\"]+)\"");
	private static final String EVIL = "http://evil.example";

	private static String base, token;

	@BeforeAll
	static void start() throws Exception {
		base = PetstoreTestServer.awaitReady(server);
		var m = TOKEN.matcher(page("/console/store").body());
		token = m.find() ? m.group(1) : null;
	}

	private static HttpResponse<String> page(String path) throws Exception {
		return HTTP.send(HttpRequest.newBuilder(URI.create(base + path)).header("Accept", "text/html").GET().build(), BodyHandlers.ofString());
	}

	private static HttpResponse<String> send(String method, String path, String origin, String csrf, String contentType, String body) throws Exception {
		var b = HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(20)).header("Accept", "application/json");
		if (origin != null)
			b.header("Origin", origin);
		if (csrf != null)
			b.header("X-Csrf-Token", csrf);
		if (contentType != null)
			b.header("Content-Type", contentType);
		b.method(method, body == null ? BodyPublishers.noBody() : BodyPublishers.ofString(body));
		return HTTP.send(b.build(), BodyHandlers.ofString());
	}

	private static HttpResponse<String> json(String method, String path, String origin, String csrf) throws Exception {
		return send(method, path, origin, csrf, "application/json", "{}");
	}

	private static JsonMap api(String path) throws Exception {
		return new JsonMap(HTTP.send(HttpRequest.newBuilder(URI.create(base + path)).header("Accept", "application/json").GET().build(), BodyHandlers.ofString()).body());
	}

	private static JsonList apiList(String path) throws Exception {
		return new JsonList(HTTP.send(HttpRequest.newBuilder(URI.create(base + path)).header("Accept", "application/json").GET().build(), BodyHandlers.ofString()).body());
	}

	private static long petWithStatus(String status) throws Exception {
		for (var o : apiList("/petstore/pets"))
			if (status.equals(((JsonMap)o).getString("status")))
				return ((JsonMap)o).getLong("id");
		throw new IllegalStateException("No seeded pet with status " + status);
	}

	private static long orderWithStatus(String status) throws Exception {
		for (var o : apiList("/petstore/orders"))
			if (status.equals(((JsonMap)o).getString("status")))
				return ((JsonMap)o).getLong("id");
		throw new IllegalStateException("No seeded order with status " + status);
	}

	private static void assertRefused(HttpResponse<String> r, int status, String reason) {
		assertEquals(status, r.statusCode(), r.body());
		assertString(reason, r.headers().firstValue("X-Request-Boundary").orElse(null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a - console pages carry the server's token
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_consolePagesCarryTheToken() {
		assertNotNull(token, "console page body carries no data-juneau-csrf token");
		assertTrue(token.matches("[0-9a-f]{64}"), token);
	}

	@Test void a02_theTokenIsTheSameOnEveryPage() throws Exception {
		assertContains("data-juneau-csrf=\"" + token + "\"", page("/console/pets").body());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b - a console write without the token is refused, and nothing changes
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_writeWithoutTokenIsRefused() throws Exception {
		var id = petWithStatus("AVAILABLE");
		assertRefused(json("POST", "/console/pets/" + id + "/sell", base, null), 403, "CSRF_TOKEN_MISSING");
		assertString("AVAILABLE", api("/petstore/pets/" + id).getString("status"));
	}

	@Test void b02_writeWithWrongTokenIsRefused() throws Exception {
		var id = petWithStatus("AVAILABLE");
		assertRefused(json("POST", "/console/pets/" + id + "/sell", base, "0".repeat(64)), 403, "CSRF_TOKEN_MISMATCH");
		assertString("AVAILABLE", api("/petstore/pets/" + id).getString("status"));
	}

	@Test void b03_formPostIsRefused() throws Exception {
		// A cross-origin <form> needs no preflight; the boundary accepts only JSON on writes.
		var id = petWithStatus("AVAILABLE");
		assertRefused(send("POST", "/console/pets/" + id + "/sell", base, token, "application/x-www-form-urlencoded", "a=b"), 415, "CONTENT_TYPE_NOT_JSON");
		assertString("AVAILABLE", api("/petstore/pets/" + id).getString("status"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c - a cross-origin unsafe request to an action endpoint is refused, even carrying the token
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_crossOriginDeleteUserIsRefused() throws Exception {
		var username = ((JsonMap)apiList("/petstore/users").get(0)).getString("username");
		assertRefused(json("DELETE", "/console/users/" + username, EVIL, token), 403, "ORIGIN_MISMATCH");
		assertString(username, api("/petstore/users/" + username).getString("username"));
	}

	@Test void c02_crossOriginMarkSoldIsRefused() throws Exception {
		var id = petWithStatus("AVAILABLE");
		assertRefused(json("POST", "/console/pets/" + id + "/sell", EVIL, token), 403, "ORIGIN_MISMATCH");
		assertString("AVAILABLE", api("/petstore/pets/" + id).getString("status"));
	}

	@Test void c03_crossOriginApproveOrderIsRefused() throws Exception {
		var id = orderWithStatus("PLACED");
		assertRefused(json("POST", "/console/orders/" + id + "/approve", EVIL, token), 403, "ORIGIN_MISMATCH");
		assertString("PLACED", api("/petstore/orders/" + id).getString("status"));
	}

	@Test void c04_crossOriginDeletePetIsRefused() throws Exception {
		var id = petWithStatus("AVAILABLE");
		assertRefused(json("DELETE", "/console/pets/" + id, EVIL, token), 403, "ORIGIN_MISMATCH");
		assertString("AVAILABLE", api("/petstore/pets/" + id).getString("status"));
	}

	@Test void c05_missingOriginIsRefused() throws Exception {
		var id = petWithStatus("AVAILABLE");
		assertRefused(json("POST", "/console/pets/" + id + "/sell", null, token), 403, "ORIGIN_MISSING");
		assertString("AVAILABLE", api("/petstore/pets/" + id).getString("status"));
	}

	@Test void c06_crossSiteFetchIsRefused() throws Exception {
		var id = petWithStatus("AVAILABLE");
		var r = HTTP.send(HttpRequest.newBuilder(URI.create(base + "/console/pets/" + id + "/sell"))
			.header("Origin", base).header("X-Csrf-Token", token).header("Content-Type", "application/json").header("Sec-Fetch-Site", "cross-site")
			.POST(BodyPublishers.ofString("{}")).build(), BodyHandlers.ofString());
		assertRefused(r, 403, "FETCH_SITE_NOT_SAME_ORIGIN");
		assertString("AVAILABLE", api("/petstore/pets/" + id).getString("status"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d - a same-origin write carrying the token goes through
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_sameOriginWriteWithTokenSucceeds() throws Exception {
		var id = petWithStatus("AVAILABLE");
		var r = json("POST", "/console/pets/" + id + "/sell", base, token);
		assertEquals(200, r.statusCode(), r.body());
		assertString("SOLD", api("/petstore/pets/" + id).getString("status"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e - the console answers one Host spelling; the /petstore API is outside the console boundary (it has its own, PetstoreApiBoundary)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void e01_otherHostSpellingIsRefusedOnConsolePages() throws Exception {
		var r = HTTP.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.getPort() + "/console/store")).header("Accept", "text/html").GET().build(), BodyHandlers.ofString());
		assertRefused(r, 421, "HOST_MISMATCH");
	}

	@Test void e02_apiIsOutsideTheConsoleBoundary() throws Exception {
		var r = HTTP.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.getPort() + "/petstore/pets")).header("Accept", "application/json").GET().build(), BodyHandlers.ofString());
		assertEquals(200, r.statusCode());
		assertTrue(r.headers().firstValue("X-Request-Boundary").isEmpty());
	}

	// An ADMIT_NON_SIMPLE pin: the API boundary admits a non-browser, non-simple write.  Not a presence pin; stays green
	// with the API boundary deleted.
	@Test void e03_nonBrowserPhotoPutNeedsNoToken() throws Exception {
		// A non-browser client's PUT (no Origin, non-simple); the console's own upload carries the API token.
		var png = new ByteArrayOutputStream();
		ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", png);
		var r = HTTP.send(HttpRequest.newBuilder(URI.create(base + "/petstore/pets/1/photo"))
			.header("Content-Type", "image/png").header("Accept", "text/plain").PUT(BodyPublishers.ofByteArray(png.toByteArray())).build(), BodyHandlers.ofString());
		assertTrue(r.statusCode() < 300, r.statusCode() + ": " + r.body());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// f - the authority
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f01_authorityDefaultsToLocalhostAndTheBoundPort() {
		assertString("localhost:8080", PetstoreConsoleBoundary.authority(null, 8080));
		assertString("localhost:8080", PetstoreConsoleBoundary.authority("  ", 8080));
	}

	@Test void f02_authoritySettingOverrides() {
		assertString("pets.example:8080", PetstoreConsoleBoundary.authority(" pets.example:8080 ", 8080));
	}

	@Test void f03_onlyConsolePathsAreCovered() {
		assertTrue(PetstoreConsoleBoundary.covers("", "/console"));
		assertTrue(PetstoreConsoleBoundary.covers("", "/console/"));
		assertTrue(PetstoreConsoleBoundary.covers(null, "/console"));
		assertTrue(PetstoreConsoleBoundary.covers("", "/console/pets/1/sell"));
		assertTrue(PetstoreConsoleBoundary.covers("/console", "/pets"));
		assertFalse(PetstoreConsoleBoundary.covers("", "/consoles"));
		assertFalse(PetstoreConsoleBoundary.covers("", "/petstore/pets/1/photo"));
		assertFalse(PetstoreConsoleBoundary.covers("", "/petstore-ui"));
		assertFalse(PetstoreConsoleBoundary.covers("", null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// g - the boundary judges the effective method, not only the request line
	//-----------------------------------------------------------------------------------------------------------------

	private static HttpResponse<String> get(String path, String origin, String csrf, String headerName, String headerValue) throws Exception {
		var b = HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(20)).header("Accept", "application/json");
		if (origin != null)
			b.header("Origin", origin);
		if (csrf != null)
			b.header("X-Csrf-Token", csrf);
		if (headerName != null)
			b.header(headerName, headerValue);
		return HTTP.send(b.GET().build(), BodyHandlers.ofString());
	}

	@Test void g01_methodQueryOverrideWithoutTokenIsRefused() throws Exception {
		var id = petWithStatus("AVAILABLE");
		assertRefused(get("/console/pets/" + id + "?method=DELETE", null, null, null, null), 403, "ORIGIN_MISSING");
		assertString("AVAILABLE", api("/petstore/pets/" + id).getString("status"));
	}

	@Test void g02_encodedMethodQueryOverrideIsRefused() throws Exception {
		var id = petWithStatus("AVAILABLE");
		assertRefused(get("/console/pets/" + id + "?x=1&%6Dethod=delete", EVIL, token, null, null), 403, "ORIGIN_MISMATCH");
	}

	@Test void g03_xMethodHeaderOverrideIsRefusedCrossOrigin() throws Exception {
		var id = petWithStatus("AVAILABLE");
		assertRefused(get("/console/pets/" + id, EVIL, token, "X-Method", "DELETE"), 403, "ORIGIN_MISMATCH");
		assertString("AVAILABLE", api("/petstore/pets/" + id).getString("status"));
	}

	@Test void g04_xHttpMethodOverrideHeaderIsRefusedCrossOrigin() throws Exception {
		var id = petWithStatus("AVAILABLE");
		assertRefused(get("/console/pets/" + id, EVIL, token, "X-HTTP-Method-Override", "DELETE"), 403, "ORIGIN_MISMATCH");
		assertString("AVAILABLE", api("/petstore/pets/" + id).getString("status"));
	}

	@Test void g05_unknownOverrideIsTreatedAsUnsafe() throws Exception {
		assertRefused(get("/console/store", null, null, "X-Method", "FROB"), 403, "ORIGIN_MISSING");
	}

	@Test void g06_safeOverrideStaysSafe() throws Exception {
		var r = get("/console/store?method=HEAD", null, null, "X-Method", "get");
		assertTrue(r.headers().firstValue("X-Request-Boundary").isEmpty(), r.statusCode() + ": " + r.body());
	}

	@Test void g07_sameOriginOverrideWithTokenStillWorks() throws Exception {
		var r = send("GET", "/console/store?method=DELETE", base, token, "application/json", null);
		assertEquals(200, r.statusCode(), r.body());
		assertTrue(r.headers().firstValue("X-Request-Boundary").isEmpty());
	}

	@Test void g08_effectiveMethod() {
		assertString("POST", org.apache.juneau.rest.server.MethodSafety.effectiveMethod("POST", "method=GET", h -> null));
		assertString("GET", org.apache.juneau.rest.server.MethodSafety.effectiveMethod("GET", null, h -> null));
		assertString("GET", org.apache.juneau.rest.server.MethodSafety.effectiveMethod("GET", "method=&methods=DELETE", h -> " "));
		assertString("DELETE", org.apache.juneau.rest.server.MethodSafety.effectiveMethod("GET", "method=options&method=delete", h -> null));
		assertString("PUT", org.apache.juneau.rest.server.MethodSafety.effectiveMethod("GET", "method=get", h -> h.equals("X-HTTP-Method") ? "put" : null));
		assertString("%ZZ", org.apache.juneau.rest.server.MethodSafety.effectiveMethod("GET", "method=%ZZ", h -> null));  // undecodable, so unknown, so unsafe
		assertString("PATCH", org.apache.juneau.rest.server.MethodSafety.effectiveMethod("HEAD", null, h -> h.equals("X-Method-Override") ? "patch" : null));
		assertString("POST", org.apache.juneau.rest.server.MethodSafety.effectiveMethod("post", null, h -> null));  // the shared helper upper-cases the request line too
	}

	//-----------------------------------------------------------------------------------------------------------------
	// h - path spellings and CORS
	//-----------------------------------------------------------------------------------------------------------------

	@Test void h01_doubleSlashConsolePathIsNeverAnUnguardedWrite() throws Exception {
		var id = petWithStatus("AVAILABLE");
		var r = json("POST", "//console/pets/" + id + "/sell", base, null);
		assertNotEquals(200, r.statusCode(), r.body());
		assertString("AVAILABLE", api("/petstore/pets/" + id).getString("status"));
	}

	@Test void h02_pathParameterConsolePathIsNeverAnUnguardedWrite() throws Exception {
		var id = petWithStatus("AVAILABLE");
		var r = json("POST", "/console;x/pets/" + id + "/sell", base, null);
		assertNotEquals(200, r.statusCode(), r.body());
		assertString("AVAILABLE", api("/petstore/pets/" + id).getString("status"));
	}

	@Test void h03_photoPreflightGrantsNothing() throws Exception {
		var r = HTTP.send(HttpRequest.newBuilder(URI.create(base + "/petstore/pets/1/photo"))
			.header("Origin", EVIL).header("Access-Control-Request-Method", "PUT").header("Access-Control-Request-Headers", "content-type")
			.method("OPTIONS", BodyPublishers.noBody()).build(), BodyHandlers.ofString());
		assertTrue(r.headers().firstValue("Access-Control-Allow-Origin").isEmpty(), r.headers().map().toString());
		assertTrue(r.headers().firstValue("Access-Control-Allow-Methods").isEmpty(), r.headers().map().toString());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// i - row-job key endpoints are console writes
	//-----------------------------------------------------------------------------------------------------------------

	@Test void i01_keyEndpointsNeedTheToken() throws Exception {
		assertRefused(json("POST", "/console/ops/jobs/species/DOG/key", base, null), 403, "CSRF_TOKEN_MISSING");
		assertRefused(json("POST", "/console/ops/jobs/species/DOG/key", EVIL, token), 403, "ORIGIN_MISMATCH");
		var r = json("POST", "/console/ops/jobs/species/DOG/key", base, token);
		assertEquals(200, r.statusCode(), r.body());
		assertBean(new JsonMap(r.body()), "action,targetId", "restock-species,DOG");
	}

	@Test void i02_keyEndpointsAreNotServedOnGet() throws Exception {
		var r = send("GET", "/console/ops/jobs/species/DOG/key", null, null, null, null);
		assertTrue(r.statusCode() == 404 || r.statusCode() == 405, () -> r.statusCode() + " " + r.body());
		assertNotContains("idempotencyKey", r.body());
	}

	@Test void i03_rowSubmitWithoutTheTokenIsRefused() throws Exception {
		var key = new JsonMap(json("POST", "/console/ops/jobs/species/DOG/key", base, token).body()).getString("idempotencyKey");
		var body = "{\"action\":\"restock-species\",\"targetId\":\"DOG\",\"idempotencyKey\":\"" + key + "\",\"fields\":{\"count\":1}}";
		assertRefused(send("POST", "/console/ops/jobs/species/DOG/restock", base, null, "application/json", body), 403, "CSRF_TOKEN_MISSING");
	}
}
