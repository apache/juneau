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

package org.apache.juneau.releng.rest;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;

import org.apache.juneau.commons.inject.StackOverlay;
import org.apache.juneau.commons.secret.*;
import org.apache.juneau.releng.credential.*;
import org.apache.juneau.rest.mock.MockRestClient;
import org.apache.juneau.rest.server.filter.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;

/**
 * The credential-overwrite vector, reproduced and then closed.
 *
 * <p>
 * The finding this pins down (recorded as F1 in {@code .work/specs/2026-08-18-console-credentials-surface.md}) was
 * established by reading framework defaults rather than by issuing a request: {@code CredentialRest} extends
 * {@code BasicRestResource}, whose default parser list includes {@code UrlEncodingParser} and
 * {@code PlainTextParser}, and {@code disableContentParam} defaults to {@code false}. If that reading is right,
 * then a plain cross-origin {@code <form method="POST">} in any page the operator has open can replace a stored
 * credential &mdash; no JavaScript, and no CORS preflight, because form-encoded is one of the three content types a
 * browser may send cross-origin without one.
 *
 * <p>
 * Group a reproduces it. The store is an {@link InMemorySecretStore} and never the Keychain, so running these tests
 * cannot touch a real credential &mdash; but everything between the request and the store is the real thing: the
 * real resource class, its real default parser list, real content negotiation and real {@code @Content} binding.
 * The vector is the framework's defaults doing exactly what they are configured to do, which is why it holds for
 * any application on {@code BasicUniversalConfig} and not only this one.
 *
 * <p>
 * Group b closes it, at the boundary rather than at the resource. {@link MockRestClient} dispatches into a
 * {@code RestContext} directly and so does not run the servlet filter chain &mdash; which is the point: group a's
 * requests reach the handler precisely because nothing stands in front of it, and the fix is to put something
 * there. Group b therefore asserts the refusal at {@link LoopbackBoundary}, where the decision is actually made.
 *
 * @see LoopbackBoundary
 */
class CredentialWriteVectorTest {

	private static final String KEYCHAIN_FREE_ACCOUNT = "jdoe";

	private InMemorySecretStore apacheStore;
	private CredentialService service;

	@BeforeEach
	void setUp(@TempDir Path stateDir) {
		apacheStore = new InMemorySecretStore();
		var stores = new EnumMap<CredentialSpec,SecretStore>(CredentialSpec.class);
		for (var spec : CredentialSpec.values())
			stores.put(spec, spec == CredentialSpec.APACHE_LDAP ? apacheStore : new InMemorySecretStore());
		service = new CredentialService(stores, new EnumMap<>(CredentialSpec.class), new AccountStore(stateDir));
		service.store("apache", KEYCHAIN_FREE_ACCOUNT, "the-real-password");
	}

	@SuppressWarnings("resource") // Caller closes via try-with-resources; MockRestClient caches RestContext per class, so opt out with a fresh StackOverlay.
	private MockRestClient client() {
		return MockRestClient.builder(new CredentialRest(service)).overridingBeanStore(new StackOverlay()).build();
	}

	private String storedSecret() {
		return apacheStore.find(KEYCHAIN_FREE_ACCOUNT).map(String::new).orElse(null);
	}

	// -----------------------------------------------------------------------------------------------------------
	// a - the vector, reproduced through the real resource
	// -----------------------------------------------------------------------------------------------------------

	@Test
	void formEncodedPostOverwritesAStoredCredential() throws Exception {
		// The whole finding in one assertion. This is the body a cross-origin <form> submits, with the content
		// type a browser sets for it, and it lands in the Keychain slot in a real deployment.
		try (var c = client();
			var res = c.post("/apache").header("Content-Type", "application/x-www-form-urlencoded")
				.bodyString("account=" + KEYCHAIN_FREE_ACCOUNT + "&secret=hijacked").run()) {
			assertEquals(200, res.getStatusCode());
		}
		assertEquals("hijacked", storedSecret(), "form-encoded POST did not reach the store");
	}

	@Test
	void plainTextPostOverwritesAStoredCredential() throws Exception {
		// text/plain is the second of the three no-preflight content types, and PlainTextParser is in the same
		// default list. Included because closing only the urlencoded shape would leave the vector open.
		try (var c = client();
			var res = c.post("/apache").header("Content-Type", "text/plain")
				.bodyString("{\"account\":\"" + KEYCHAIN_FREE_ACCOUNT + "\",\"secret\":\"hijacked-plain\"}").run()) {
			assertEquals(200, res.getStatusCode());
		}
		assertEquals("hijacked-plain", storedSecret(), "text/plain POST did not reach the store");
	}

	@Test
	void contentQueryParameterIsRefusedByTheResource() throws Exception {
		// The second half of F1, asserted closed rather than reproduced. disableContentParam defaults to false, so
		// the body can travel in the URL instead; CredentialRest now sets it to "true", and this fails if that is
		// removed.
		//
		// Worth a test of its own rather than folding into the form vector, for two reasons. The attacker sends no
		// body and therefore no content type at all, so a check that only inspected the content type of a present
		// body would wave it through. And a URL-borne secret has a second life the form shape does not: browser
		// history, access logs, and the Referer of whatever the page loads next.
		//
		// The payload is UON, not JSON, because that is what the server would actually parse -- RestRequest's
		// constructor rewrites the content type to UON's own and hands the parameter to UonParser. JSON here would
		// be refused as malformed and this test would pass while proving nothing about disableContentParam. It is
		// asserted on the store, not only the status, for the same reason.
		//
		// This is defence in depth and not the primary control: the boundary refuses this shape from a hostile
		// page anyway (group b). What it buys is that a developer or a copied URL cannot use it either.
		try (var c = client();
			var res = c.post("/apache?content=" + java.net.URLEncoder.encode(
				"(account=" + KEYCHAIN_FREE_ACCOUNT + ",secret=hijacked-via-url)",
				java.nio.charset.StandardCharsets.UTF_8)).run()) {
			assertNotEquals(200, res.getStatusCode(), "content= parameter was still honoured");
		}
		assertEquals("the-real-password", storedSecret(), "content= parameter still reached the store");
	}

	// -----------------------------------------------------------------------------------------------------------
	// b - closed by the boundary
	// -----------------------------------------------------------------------------------------------------------

	private static LoopbackBoundary boundary() {
		return LoopbackBoundary.create().authority("127.0.0.1:8790").token(SynchronizerToken.of("t0ken")).build();
	}

	/**
	 * The content type is stubbed on {@code getContentType()} and not merely as a header, because that is what the
	 * boundary reads. Stubbing only the header leaves it {@code null}, and every write then fails the content-type
	 * check for the wrong reason -- which would make the refusals below pass vacuously.
	 */
	private static jakarta.servlet.http.HttpServletRequest req(String method, String contentType, Map<String,String> headers) {
		var r = org.mockito.Mockito.mock(jakarta.servlet.http.HttpServletRequest.class);
		org.mockito.Mockito.when(r.getMethod()).thenReturn(method);
		org.mockito.Mockito.when(r.getContentType()).thenReturn(contentType);
		headers.forEach((k, v) -> org.mockito.Mockito.when(r.getHeader(k)).thenReturn(v));
		return r;
	}

	@Test
	void theBoundaryRefusesTheFormEncodedPost() {
		// The exact request from group a, as it would arrive from a hostile page: refused on content type, so it
		// never reaches CredentialRest and the store is never opened.
		var res = boundary().check(req("POST", "application/x-www-form-urlencoded", Map.of(
			"Host", "127.0.0.1:8790",
			"Origin", "http://evil.example")));
		assertFalse(res.isAllowed());
		assertEquals(403, res.status(), "foreign Origin is caught before the content type");
	}

	@Test
	void theBoundaryRefusesTheFormEncodedPostEvenFromOurOwnOrigin() {
		// With the origin corrected, the content-type check is what stops it. This is the assertion that the
		// no-preflight form shape is unreachable, rather than merely that this particular attacker got the Origin
		// wrong.
		var res = boundary().check(req("POST", "application/x-www-form-urlencoded", Map.of(
			"Host", "127.0.0.1:8790",
			"Origin", "http://127.0.0.1:8790",
			"Sec-Fetch-Site", "same-origin",
			"X-Csrf-Token", "t0ken")));
		assertFalse(res.isAllowed());
		assertEquals(415, res.status());
	}

	@Test
	void theBoundaryRefusesTheContentQueryParameterPost() {
		// The content= variant carries no body and therefore no content type, which is itself a no-preflight
		// shape. It has to be refused on that absence rather than exempted for it -- and note the server would
		// otherwise rewrite the content type to UON itself, so there is nothing downstream to catch it either.
		var res = boundary().check(req("POST", null, Map.of(
			"Host", "127.0.0.1:8790",
			"Origin", "http://127.0.0.1:8790",
			"Sec-Fetch-Site", "same-origin",
			"X-Csrf-Token", "t0ken")));
		assertFalse(res.isAllowed());
		assertEquals(415, res.status());
	}

	@Test
	void theBoundaryAllowsTheLegitimateJsonPost() {
		// The other direction, so this class cannot pass by refusing everything.
		var res = boundary().check(req("POST", "application/json", Map.of(
			"Host", "127.0.0.1:8790",
			"Origin", "http://127.0.0.1:8790",
			"Sec-Fetch-Site", "same-origin",
			"X-Csrf-Token", "t0ken")));
		assertTrue(res.isAllowed());
	}
}
