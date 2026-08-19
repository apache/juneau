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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.*;
import java.util.*;

import org.apache.juneau.commons.inject.StackOverlay;
import org.apache.juneau.commons.secret.*;
import org.apache.juneau.http.Content;
import org.apache.juneau.releng.credential.*;
import org.apache.juneau.rest.mock.MockRestClient;
import org.apache.juneau.rest.server.Mutating;
import org.apache.juneau.rest.server.Rest;
import org.apache.juneau.rest.server.RestPost;
import org.apache.juneau.rest.server.filter.*;
import org.apache.juneau.rest.server.servlet.BasicRestResource;
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
 * Group a reproduces it, and both of its routes are demonstrated as an <i>actual overwrite</i> &mdash; the claim is
 * integrity loss, so a {@code 200} with no write would falsify it just as much as a {@code 415} would. The store is
 * an {@link InMemorySecretStore} and never the Keychain, so running these tests cannot touch a real credential
 * &mdash; but everything between the request and the store is the real thing: a real {@code BasicRestResource}, its
 * real default parser list, real content negotiation and real {@code @Content} binding. The vector is the
 * framework's defaults doing exactly what they are configured to do, which is why it holds for any application on
 * {@code BasicUniversalConfig} and not only this one.
 *
 * <p>
 * The two routes need different subjects, and the reason is the trap this test exists to avoid. The form-encodable
 * body route is gated by nothing at the resource, so it is reproduced against the real {@code CredentialRest}. The
 * {@code &content=} query route is gated at the resource by {@code disableContentParam}, and {@code CredentialRest}
 * already sets that to {@code "true"} &mdash; so reproducing the {@code content=} route against {@code CredentialRest}
 * would demonstrate the <i>fix</i>, not the finding, and could never fail against unfixed code. That route is
 * therefore reproduced against {@link DefaultConfigCredentialResource}, a resource carrying {@code BasicRestResource}'s
 * defaults unchanged (in particular {@code disableContentParam} at its default {@code false}) &mdash; the
 * configuration every other Juneau consumer has.
 *
 * <p>
 * The route (b) remedy is asserted twice. {@link #contentQueryParameterIsRefusedByTheResource()} asserts it against
 * the real {@code CredentialRest}, which is correct and kept, but {@code CredentialRest} differs from
 * {@link DefaultConfigCredentialResource} in more than {@code disableContentParam} &mdash; its full parser list,
 * filters and view config are also live &mdash; so a pass there does not by itself prove {@code disableContentParam}
 * is the control doing the work. {@link #contentQueryParameterIsRefusedByTheTwinWithDisableContentParamTrue()} closes
 * that gap: it asserts the same remedy against {@link HardenedConfigCredentialResource}, a twin of
 * {@link DefaultConfigCredentialResource} with only {@code disableContentParam} flipped to {@code "true"}. Because
 * reproduction and remedy subjects then differ by exactly the one control under test, that pairing is airtight.
 *
 * <p>
 * Group b closes it. {@link MockRestClient} dispatches into a {@code RestContext} directly and so does not run the
 * servlet filter chain &mdash; which is the point: group a's requests reach the handler precisely because nothing
 * stands in front of it, and the fix is to put something there. The two controls are asserted against the routes
 * they close, kept separate so that the record shows <i>which</i> control closes <i>which</i> route and neither is
 * later dropped as redundant: {@link LoopbackBoundary} refuses the form-encodable shape (route a), and
 * {@code disableContentParam="true"} on {@code CredentialRest} refuses the {@code content=} route (route b). The
 * absent-{@code Content-Type} case is pinned last as a standing regression guard.
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

	@SuppressWarnings("resource") // As client(): caller closes; fresh StackOverlay opts out of the per-class RestContext cache.
	private MockRestClient defaultConfigClient() {
		return MockRestClient.builder(new DefaultConfigCredentialResource(service)).overridingBeanStore(new StackOverlay()).build();
	}

	@SuppressWarnings("resource") // As client(): caller closes; fresh StackOverlay opts out of the per-class RestContext cache.
	private MockRestClient hardenedConfigClient() {
		return MockRestClient.builder(new HardenedConfigCredentialResource(service)).overridingBeanStore(new StackOverlay()).build();
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
	void contentQueryParameterOverwritesAStoredCredentialWhenDisableContentParamIsDefault() throws Exception {
		// Route (b) of F1, reproduced as an actual overwrite -- the half that contentQueryParameterIsRefusedByThe
		// Resource (below) cannot demonstrate. That test runs against the real CredentialRest, which sets
		// disableContentParam="true", so it can only ever show the fix working; a &content= reproduction there
		// could not fail against unfixed code, which would make it a test of the remedy rather than a confirmation
		// of the finding. This runs against DefaultConfigCredentialResource instead -- BasicRestResource's defaults
		// unchanged, disableContentParam at its default false -- the configuration every BasicUniversalConfig
		// consumer ships. That is the only difference from CredentialRest that matters for this route, so the pair
		// (this and the remedy test below) records that disableContentParam="true" is the control closing it.
		//
		// The payload is UON, not JSON, for the same reason the remedy test gives: RestRequest rewrites the content
		// type to UON's own and hands the parameter to UonParser, so JSON here would be refused as malformed and
		// the test would pass while proving nothing. Asserted on the store, not only the status, because the claim
		// is integrity loss -- a 200 with no write would falsify it.
		try (var c = defaultConfigClient();
			var res = c.post("/apache?content=" + java.net.URLEncoder.encode(
				"(account=" + KEYCHAIN_FREE_ACCOUNT + ",secret=hijacked-via-url)",
				java.nio.charset.StandardCharsets.UTF_8)).run()) {
			assertEquals(200, res.getStatusCode(), "content= parameter was not honoured on default config");
		}
		assertEquals("hijacked-via-url", storedSecret(), "content= parameter did not reach the store on default config");
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

	@Test
	void contentQueryParameterIsRefusedByTheTwinWithDisableContentParamTrue() throws Exception {
		// The airtight version of contentQueryParameterIsRefusedByTheResource (above), which stays because it is
		// also correct. That test's subject is the real CredentialRest, which differs from the reproduction
		// subject (DefaultConfigCredentialResource) in more than disableContentParam -- so a pass there does not,
		// on its own, prove disableContentParam is the operative control rather than some other difference
		// between the two resources. HardenedConfigCredentialResource is DefaultConfigCredentialResource with
		// only that one flag flipped to "true", so reproduction and remedy subjects here differ by exactly the
		// control under test.
		//
		// Same UON-not-JSON payload reasoning as the other content= tests: RestRequest rewrites the content type
		// to UON's own, so a JSON payload would be refused as malformed and this would pass while proving
		// nothing. Asserted on the store, not only the status, because the claim is integrity loss.
		try (var c = hardenedConfigClient();
			var res = c.post("/apache?content=" + java.net.URLEncoder.encode(
				"(account=" + KEYCHAIN_FREE_ACCOUNT + ",secret=hijacked-via-url)",
				java.nio.charset.StandardCharsets.UTF_8)).run()) {
			assertNotEquals(200, res.getStatusCode(), "content= parameter was still honoured with disableContentParam=true");
		}
		assertEquals("the-real-password", storedSecret(), "content= parameter still reached the store on the hardened twin");
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
		var r = mock(jakarta.servlet.http.HttpServletRequest.class);
		when(r.getMethod()).thenReturn(method);
		when(r.getContentType()).thenReturn(contentType);
		headers.forEach((k, v) -> when(r.getHeader(k)).thenReturn(v));
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
		assertEquals(LoopbackBoundary.Reason.CONTENT_TYPE_NOT_JSON, res.reason(), "refused for the wrong reason");
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

	// -----------------------------------------------------------------------------------------------------------
	// The absent-Content-Type contract, pinned as a standing regression guard
	// -----------------------------------------------------------------------------------------------------------

	@Test
	void absentContentTypeOnAWriteIsRefusedAsNotJson() {
		// A state-changing request that is otherwise entirely in order -- right Host and Origin, same-origin
		// Sec-Fetch-Site, valid CSRF token -- but carries no Content-Type at all. It is refused 415, on the
		// content-type check, because isJson(null) is false: the check is an allowlist (base type must equal
		// application/json) rather than a denylist of the form-encodable types, and an allowlist refuses the
		// header's absence by construction.
		//
		// This is asserted for the sake of the *next* change, not this one. The safe behaviour here depends
		// entirely on the allowlist shape; a future relaxation toward a denylist ("refuse the three form-encodable
		// types") would silently let a Content-Type-less write through, and nothing on isJson's own side would
		// object -- the coupling between "allowlist" and "typeless is refused" is invisible from there. This
		// assertion is the only thing that would fail if that shape were quietly changed, so the reason is pinned
		// explicitly and not merely the status.
		var res = boundary().check(req("POST", null, Map.of(
			"Host", "127.0.0.1:8790",
			"Origin", "http://127.0.0.1:8790",
			"Sec-Fetch-Site", "same-origin",
			"X-Csrf-Token", "t0ken")));
		assertFalse(res.isAllowed());
		assertEquals(415, res.status());
		assertEquals(LoopbackBoundary.Reason.CONTENT_TYPE_NOT_JSON, res.reason());
	}

	// -----------------------------------------------------------------------------------------------------------
	// Test subject for route (b)'s reproduction: BasicRestResource defaults, disableContentParam left at false
	// -----------------------------------------------------------------------------------------------------------

	/**
	 * A single credential write carrying {@code BasicRestResource}'s defaults unchanged &mdash; in particular
	 * {@code disableContentParam} at its default {@code false}, so the {@code &content=} query route is live. This
	 * is the honest subject for reproducing route (b) of F1: the real {@code CredentialRest} sets
	 * {@code disableContentParam="true"}, so the {@code content=} route against it can only ever demonstrate the
	 * fix. The write mirrors {@link CredentialRest#set(String, CredentialRest.StoreRequest)} and reuses its
	 * {@link CredentialRest.StoreRequest} body so the only relevant difference from the real resource, on this
	 * route, is the one property under test.
	 */
	@Rest
	public static class DefaultConfigCredentialResource extends BasicRestResource {

		private final CredentialService service;

		/**
		 * Constructor.
		 *
		 * @param service The credential service the write lands in.
		 */
		public DefaultConfigCredentialResource(CredentialService service) {
			this.service = service;
		}

		/**
		 * Store/update a credential, exactly as {@link CredentialRest#set(String, CredentialRest.StoreRequest)}.
		 *
		 * @param name The credential name (path variable).
		 * @param body The {@code {account?, secret}} body.
		 * @return The updated status for the named credential.
		 */
		@Mutating("replaces a stored credential in the Keychain")
		@RestPost("/{name}")
		public CredentialStatus set(@org.apache.juneau.http.Path("name") String name, @Content CredentialRest.StoreRequest body) {
			service.store(name, body.account, body.secret);
			return service.status().stream().filter(c -> c.name.equals(name)).findFirst().orElseThrow();
		}
	}

	// -----------------------------------------------------------------------------------------------------------
	// Test subject for route (b)'s remedy: DefaultConfigCredentialResource with only disableContentParam flipped
	// -----------------------------------------------------------------------------------------------------------

	/**
	 * {@link DefaultConfigCredentialResource} with the single control under test flipped on. Every other property
	 * &mdash; parser list, method, path, body shape &mdash; is copy-identical, so this is the honest subject for
	 * asserting route (b)'s remedy: the only difference between the reproduction subject
	 * ({@link DefaultConfigCredentialResource}) and this remedy subject is {@code disableContentParam} itself,
	 * which is what {@link #contentQueryParameterIsRefusedByTheTwinWithDisableContentParamTrue()} relies on.
	 */
	@Rest(disableContentParam = "true")
	public static class HardenedConfigCredentialResource extends BasicRestResource {

		private final CredentialService service;

		/**
		 * Constructor.
		 *
		 * @param service The credential service the write lands in.
		 */
		public HardenedConfigCredentialResource(CredentialService service) {
			this.service = service;
		}

		/**
		 * Store/update a credential, exactly as {@link DefaultConfigCredentialResource#set(String, CredentialRest.StoreRequest)}.
		 *
		 * @param name The credential name (path variable).
		 * @param body The {@code {account?, secret}} body.
		 * @return The updated status for the named credential.
		 */
		@Mutating("replaces a stored credential in the Keychain")
		@RestPost("/{name}")
		public CredentialStatus set(@org.apache.juneau.http.Path("name") String name, @Content CredentialRest.StoreRequest body) {
			service.store(name, body.account, body.secret);
			return service.status().stream().filter(c -> c.name.equals(name)).findFirst().orElseThrow();
		}
	}
}
