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
package org.apache.juneau.rest.docs;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.function.*;

/**
 * Shared fixtures + per-URL response-shape assertions for the api-docs mixin pack.
 *
 * <p>
 * Used by the three-way deployment-parity test suite ({@code MockRest} baseline via
 * {@code BasicApiDocs_TransitiveDedupe_Test}, real-Jetty via
 * {@code BasicApiDocs_JettyMicroservice_Test}, and real Spring Boot via
 * {@code BasicApiDocs_Springboot_Test}) so every deployment runs the same response-shape checks
 * against the six canonical api-docs URLs:
 *
 * <ul>
 * 	<li>{@code /api} &mdash; Swagger v2 spec (JSON when {@code Accept: application/json}).
 * 	<li>{@code /swagger} &mdash; Swagger UI HTML view (bare browser GET).
 * 	<li>{@code /openapi} &mdash; OpenAPI 3.1 spec (JSON when {@code Accept: application/json}).
 * 	<li>{@code /openapi.json} &mdash; OpenAPI 3.1 spec as JSON (format-pinned).
 * 	<li>{@code /openapi.yaml} &mdash; OpenAPI 3.1 spec as YAML (format-pinned).
 * 	<li>{@code /redoc} &mdash; Redoc UI HTML view (bare browser GET).
 * </ul>
 *
 * <p>
 * <b>Why a fixture rather than byte-identity?</b> The OpenAPI / Swagger documents include
 * {@code host} / {@code servers} elements that are derived from the request URL, so a real HTTP
 * call against a random-port Jetty or Tomcat container will differ on those fields from a
 * {@code MockRest} response. Asserting content-shape (status, content-type prefix, key markers)
 * captures the same deployment-parity intent as byte-identity without the noise.
 *
 * <p>
 * Cross-references:
 * <ul>
 * 	<li>{@link BasicApiDocs_TransitiveDedupe_Test} &mdash; the {@code MockRest} baseline.
 * 	<li>{@code BasicApiDocs_JettyMicroservice_Test} &mdash; the real-Jetty parity assertion.
 * 	<li>{@code BasicApiDocs_Springboot_Test} &mdash; the Spring Boot parity assertion.
 * </ul>
 */
final class BasicApiDocsTestFixtures {

	private BasicApiDocsTestFixtures() {}

	/**
	 * One per-URL expectation: relative path, optional {@code Accept} header, expected
	 * {@code Content-Type} prefix on the response, and a substring that MUST appear in the response
	 * body.
	 *
	 * <p>
	 * Six entries &mdash; one per canonical api-docs URL. Tests iterate the list and assert each
	 * entry against their deployment's HTTP client.
	 */
	record UrlAssertion(String path, String accept, String expectedContentTypePrefix, String mustContain) {}

	/**
	 * Returns the canonical six-URL assertion list.
	 *
	 * <p>
	 * Entries match {@link BasicApiDocs_TransitiveDedupe_Test}'s six tests verbatim, modulo being
	 * encoded as data rather than test methods so a single iteration covers all three deployment
	 * paths.
	 */
	static List<UrlAssertion> sixUrlAssertions() {
		// @formatter:off
		return List.of(
			// /api — Swagger v2 spec; JSON when Accept: application/json.
			new UrlAssertion("/api", "application/json", "application/json", "\"swagger\":\"2.0\""),
			// /swagger — Swagger UI HTML (bare GET; defaultAccept="text/html" on the UI mixin).
			new UrlAssertion("/swagger", null, "text/html", "<html"),
			// /openapi — OpenAPI 3.1 spec; JSON when Accept: application/json.
			new UrlAssertion("/openapi", "application/json", "application/json", "\"openapi\""),
			// /openapi.json — format-pinned JSON regardless of Accept.
			new UrlAssertion("/openapi.json", null, "application/json", "\"openapi\""),
			// /openapi.yaml — format-pinned YAML regardless of Accept.
			new UrlAssertion("/openapi.yaml", null, "application/yaml", "openapi:"),
			// /redoc — Redoc HTML view (bare GET; defaultAccept="text/html" on the UI mixin).
			new UrlAssertion("/redoc", null, "text/html", "<html")
		);
		// @formatter:on
	}

	/**
	 * Runs the canonical six-URL assertion list against a deployment-specific HTTP caller.
	 *
	 * <p>
	 * The caller is a functional interface that, given a path + Accept header, returns the
	 * deployment's HTTP response captured as a {@link Response} record. This indirection keeps the
	 * fixture deployment-agnostic &mdash; {@code MockRest}, Jetty, and Spring Boot tests each
	 * provide their own implementation.
	 *
	 * @param caller The deployment-specific HTTP caller.
	 */
	static void assertAllSixUrls(BiFunction<String,String,Response> caller) {
		for (var ua : sixUrlAssertions()) {
			var resp = caller.apply(ua.path(), ua.accept());
			assertEquals(200, resp.status(),
				"Expected 200 on " + ua.path() + " (Accept=" + ua.accept() + "), got " + resp.status()
					+ "; body=" + truncate(resp.body()));
			assertNotNull(resp.contentType(),
				"Expected Content-Type on " + ua.path() + ", got null");
			assertTrue(resp.contentType().toLowerCase(Locale.ROOT).startsWith(ua.expectedContentTypePrefix()),
				"Expected Content-Type starting with '" + ua.expectedContentTypePrefix()
					+ "' on " + ua.path() + ", got '" + resp.contentType() + "'");
			assertNotNull(resp.body(), "Expected non-null body on " + ua.path());
			assertTrue(resp.body().contains(ua.mustContain()),
				"Expected body on " + ua.path() + " to contain '" + ua.mustContain()
					+ "'; got: " + truncate(resp.body()));
		}
	}

	/**
	 * Deployment-agnostic HTTP response capture.
	 *
	 * <p>
	 * Holds just the three things the per-URL assertions need: HTTP status, response
	 * {@code Content-Type} header (verbatim, before parameter stripping), and response body as
	 * UTF-8 text.
	 *
	 * @param status      HTTP status code.
	 * @param contentType The full {@code Content-Type} header value (may include charset / boundary
	 *                    parameters &mdash; assertions normalize via {@code startsWith}).
	 * @param body        Response body as UTF-8 text.
	 */
	record Response(int status, String contentType, String body) {}

	private static String truncate(String s) {
		if (s == null)
			return "<null>";
		var max = 256;
		return s.length() <= max ? s : s.substring(0, max) + "…(" + (s.length() - max) + " more)";
	}
}
