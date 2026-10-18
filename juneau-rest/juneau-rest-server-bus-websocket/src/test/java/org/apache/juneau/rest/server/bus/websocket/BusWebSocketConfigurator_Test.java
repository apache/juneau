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
package org.apache.juneau.rest.server.bus.websocket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.net.*;
import java.security.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.bus.*;
import org.apache.juneau.rest.server.filter.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

import jakarta.websocket.*;
import jakarta.websocket.server.*;

/**
 * The WebSocket handshake guards: an exact Origin match, a Host match against the boundary,
 * {@code Sec-Fetch-Site}, and the capability plus principal.
 *
 * <p>
 * Runs against a real {@link ServerBus}; sessions come from the real session POST.  Only the jakarta.websocket
 * interfaces are mocked.
 */
@SuppressWarnings({
	"resource" // The shared MockRestClient lives for the whole class; each test's bus is closed in @AfterEach.
})
class BusWebSocketConfigurator_Test extends TestBase {

	@Rest
	public static class R extends BasicRestServlet implements BusEventsMixin {
		private static final long serialVersionUID = 1L;
		static volatile ServerBus bus;
		@Override public ServerBus serverBus() { return bus; }
	}

	private static final MockRestClient C = MockRestClient.buildLax(R.class);

	private static final String AUTHORITY = "127.0.0.1:8080";
	private static final String ORIGIN = "http://" + AUTHORITY;
	private static final SynchronizerToken TOKEN = SynchronizerToken.of("the-real-token");
	private static final Principal JB = () -> "jb";
	private static final List<String> DOWN = List.of("ops.jobs");
	private static final List<String> UP = List.of("ops.cancel-all");

	private ServerBus bus;

	private static BusPolicy.Builder policy() {
		return BusPolicy.create().downstream("ops.jobs", true).upstream("ops.cancel-all", (s, t, p) -> {});
	}

	private void use(BusPolicy p) {
		bus = ServerBus.create(p);
		bus.enableWebSocket(BusWebSockets.PREFIX);
		R.bus = bus;
	}

	@AfterEach void tearDown() {
		if (bus != null)
			bus.close();
	}

	private BusWebSocketConfigurator withBoundary() {
		use(policy().boundary(LoopbackBoundary.create().authority(AUTHORITY).token(TOKEN).build()).allowedOrigins("https://ignored.example.com").build());
		return new BusWebSocketConfigurator(bus, BusWebSocketConfigurator.allowedOrigins(bus.policy()));
	}

	private BusWebSocketConfigurator withOrigins(String...origins) {
		use(policy().allowedOrigins(origins).build());
		return new BusWebSocketConfigurator(bus, BusWebSocketConfigurator.allowedOrigins(bus.policy()));
	}

	/** Opens a websocket session as {@code principal}; the boundary, when set, needs its headers on the POST. */
	private String open(Principal principal, String transport, boolean boundary) throws Exception {
		var up = "websocket".equals(transport) ? UP : List.<String>of();
		return boundary
			? BusFixture.open(C, principal, transport, DOWN, up, "Host", AUTHORITY, "Origin", ORIGIN, "Sec-Fetch-Site", "same-origin", "X-Csrf-Token", TOKEN.value())
			: BusFixture.open(C, principal, transport, DOWN, up);
	}

	private static HandshakeRequest request(String path, Principal principal, String...headerPairs) {
		var r = mock(HandshakeRequest.class);
		var h = new LinkedHashMap<String,List<String>>();
		for (var i = 0; i < headerPairs.length; i += 2)
			h.put(headerPairs[i], List.of(headerPairs[i + 1]));
		when(r.getHeaders()).thenReturn(h);
		when(r.getRequestURI()).thenReturn(URI.create("ws://" + AUTHORITY + path));
		when(r.getUserPrincipal()).thenReturn(principal);
		return r;
	}

	private static Map<String,Object> handshake(BusWebSocketConfigurator c, HandshakeRequest r) {
		var props = new HashMap<String,Object>();
		handshake(c, r, props);
		return props;
	}

	private static void handshake(BusWebSocketConfigurator c, HandshakeRequest r, Map<String,Object> props) {
		var sec = mock(ServerEndpointConfig.class);
		when(sec.getUserProperties()).thenReturn(props);
		c.modifyHandshake(sec, r, mock(HandshakeResponse.class));
	}

	private static int refusalCode(Map<String,Object> props) {
		var r = (BusWebSocketConfigurator.Refusal) props.get(BusWebSocketConfigurator.REFUSAL_PROPERTY);
		assertNotNull(r, () -> "expected a refusal, got " + props);
		assertNull(props.get(BusWebSocketConfigurator.SESSION_PROPERTY), () -> "a refused handshake must not carry a session: " + props);
		return r.code();
	}

	private static String path(String id) {
		return BusWebSockets.PREFIX + "/" + id;
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) checkOrigin: exact match only, before any upgrade (the container answers 403)
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_boundaryOriginExactMatch() {
		var c = withBoundary();
		assertTrue(c.checkOrigin(ORIGIN));
		assertFalse(c.checkOrigin(ORIGIN + "/"));
		assertFalse(c.checkOrigin("http://localhost:8080"));
		assertFalse(c.checkOrigin("HTTP://127.0.0.1:8080"));
		assertFalse(c.checkOrigin("http://127.0.0.1:8081"));
		assertFalse(c.checkOrigin(""));
		assertFalse(c.checkOrigin(null), "a missing Origin is refused");
	}

	@Test void a02_boundaryWinsOverAllowedOrigins() {
		var c = withBoundary();
		assertFalse(c.checkOrigin("https://ignored.example.com"), "with a boundary, allowedOrigins is not consulted");
	}

	@Test void a03_allowedOriginsExactMatch() {
		var c = withOrigins("https://ops.example.com", "https://ops2.example.com");
		assertTrue(c.checkOrigin("https://ops.example.com"));
		assertTrue(c.checkOrigin("https://ops2.example.com"));
		assertFalse(c.checkOrigin("https://evil.example.com"));
		assertFalse(c.checkOrigin("https://ops.example.com.evil.example.com"));
		assertFalse(c.checkOrigin(null));
	}

	@Test void a04_noOriginRuleYieldsEmptySet() {
		use(policy().build());
		assertEquals(Set.of(), BusWebSocketConfigurator.allowedOrigins(bus.policy()));
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) modifyHandshake: Host -> 4403, Sec-Fetch-Site -> 4403, capability / principal / transport -> 4401
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_validHandshakeRecordsSession() throws Exception {
		var c = withBoundary();
		var id = open(JB, "websocket", true);
		var props = handshake(c, request(path(id), JB, "Host", AUTHORITY, "Origin", ORIGIN));
		assertNull(props.get(BusWebSocketConfigurator.REFUSAL_PROPERTY));
		var s = assertInstanceOf(BusSession.class, props.get(BusWebSocketConfigurator.SESSION_PROPERTY));
		assertEquals("jb", s.principalName());
		assertEquals("websocket", s.transport());
	}

	@Test void b02_contextPathIsAllowed() throws Exception {
		var c = withOrigins("https://ops.example.com");
		var id = open(null, "websocket", false);
		var props = handshake(c, request("/app" + path(id), null));
		assertNotNull(props.get(BusWebSocketConfigurator.SESSION_PROPERTY));
	}

	@Test void b03_hostMismatchIs4403_andNoLookup() throws Exception {
		var c = withBoundary();
		var id = open(JB, "websocket", true);
		assertEquals(4403, refusalCode(handshake(c, request(path(id), JB, "Host", "rebind.example.com:8080"))));
	}

	@Test void b04_missingHostWithBoundaryIs4403() throws Exception {
		var c = withBoundary();
		var id = open(JB, "websocket", true);
		// A Sec-WebSocket-Key marks an RFC 6455 handshake, which must carry a Host; without the key it would be RFC 8441 (section g).
		assertEquals(4403, refusalCode(handshake(c, request(path(id), JB, "Sec-WebSocket-Key", "k"))));
	}

	@Test void b05_hostHeaderNameIsCaseInsensitive() throws Exception {
		var c = withBoundary();
		var id = open(JB, "websocket", true);
		assertNotNull(handshake(c, request(path(id), JB, "host", AUTHORITY)).get(BusWebSocketConfigurator.SESSION_PROPERTY));
	}

	@Test void b06_noBoundaryMeansNoHostCheck() throws Exception {
		var c = withOrigins("https://ops.example.com");
		var id = open(JB, "websocket", false);
		assertNotNull(handshake(c, request(path(id), JB, "Host", "anything:1")).get(BusWebSocketConfigurator.SESSION_PROPERTY));
	}

	@Test void b07_secFetchSiteMustBeSameOriginWhenPresent() throws Exception {
		var c = withBoundary();
		var id = open(JB, "websocket", true);
		for (var site : List.of("cross-site", "same-site", "none"))
			assertEquals(4403, refusalCode(handshake(c, request(path(id), JB, "Host", AUTHORITY, "Sec-Fetch-Site", site))), site);
		var ok = handshake(c, request(path(id), JB, "Host", AUTHORITY, "Sec-Fetch-Site", "same-origin"));
		assertNotNull(ok.get(BusWebSocketConfigurator.SESSION_PROPERTY));
	}

	@Test void b08_unknownCapabilityIs4401() {
		var c = withBoundary();
		assertEquals(4401, refusalCode(handshake(c, request(path("0".repeat(64)), JB, "Host", AUTHORITY))));
	}

	@Test void b09_principalMismatchIs4401() throws Exception {
		var c = withBoundary();
		var id = open(JB, "websocket", true);
		Principal other = () -> "mallory";
		assertEquals(4401, refusalCode(handshake(c, request(path(id), other, "Host", AUTHORITY))));
		assertEquals(4401, refusalCode(handshake(c, request(path(id), null, "Host", AUTHORITY))), "anonymous is not jb");
	}

	@Test void b10_sseSessionOnWebSocketIs4401() throws Exception {
		var c = withBoundary();
		var id = open(JB, "sse", true);
		assertEquals(4401, refusalCode(handshake(c, request(path(id), JB, "Host", AUTHORITY))));
	}

	@Test void b11_malformedPathIs4401() throws Exception {
		var c = withBoundary();
		var id = open(JB, "websocket", true);
		assertEquals(4401, refusalCode(handshake(c, request(BusWebSockets.PREFIX + "/", JB, "Host", AUTHORITY))));
		assertEquals(4401, refusalCode(handshake(c, request(path(id) + "/extra", JB, "Host", AUTHORITY))));
		assertEquals(4401, refusalCode(handshake(c, request("/elsewhere/" + id, JB, "Host", AUTHORITY))));
	}

	@Test void b12_staleRefusalIsClearedOnAGoodHandshake() throws Exception {
		var c = withBoundary();
		var id = open(JB, "websocket", true);
		var props = new HashMap<String,Object>(Map.of(BusWebSocketConfigurator.REFUSAL_PROPERTY, BusWebSocketConfigurator.REFUSED));
		handshake(c, request(path(id), JB, "Host", AUTHORITY), props);
		assertNull(props.get(BusWebSocketConfigurator.REFUSAL_PROPERTY));
		assertNotNull(props.get(BusWebSocketConfigurator.SESSION_PROPERTY));
	}

	@Test void b13_staleSessionIsClearedOnARefusedHandshake() throws Exception {
		var c = withBoundary();
		var id = open(JB, "websocket", true);
		var props = handshake(c, request(path(id), JB, "Host", AUTHORITY));
		assertNotNull(props.get(BusWebSocketConfigurator.SESSION_PROPERTY));
		handshake(c, request(path(id), JB, "Host", "rebind.example.com:8080"), props);
		assertEquals(4403, refusalCode(props));
	}

	@Test void b14_refusalReasons() {
		assertEquals("bus:unknown-session", BusWebSocketConfigurator.UNKNOWN_SESSION.reason());
		assertEquals("bus:refused", BusWebSocketConfigurator.REFUSED.reason());
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) getEndpointInstance
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_endpointInstancePerConnection() throws Exception {
		var c = withBoundary();
		var a = c.getEndpointInstance(BusWebSocketEndpoint.class);
		var b = c.getEndpointInstance(BusWebSocketEndpoint.class);
		assertNotNull(a);
		assertNotSame(a, b);
	}

	@Test void c02_otherEndpointClassRefused() {
		var c = withBoundary();
		assertThrows(InstantiationException.class, () -> c.getEndpointInstance(Object.class));
	}

	//------------------------------------------------------------------------------------------------------------------
	// CsrfBoundary fixtures
	//------------------------------------------------------------------------------------------------------------------

	private static final String APP = "app.example";
	private static final String APP_ORIGIN = "http://" + APP;

	private CsrfBoundary csrf;

	private BusWebSocketConfigurator withCsrf() {
		return withCsrf(CsrfBoundary.create().tokens(CsrfTokens.perProcess()));
	}

	private BusWebSocketConfigurator withCsrf(CsrfBoundary.Builder b) {
		csrf = b.build();
		use(policy().boundary(csrf).build());
		return new BusWebSocketConfigurator(bus, BusWebSocketConfigurator.allowedOrigins(bus.policy()));
	}

	/** Opens a websocket session through the session POST, which the CsrfBoundary guards. */
	private String openCsrf(Principal principal) throws Exception {
		var token = csrf.tokenFor(mock(jakarta.servlet.http.HttpServletRequest.class));
		return BusFixture.open(C, principal, "websocket", DOWN, UP, "Host", APP, "Origin", APP_ORIGIN, "X-Csrf-Token", token);
	}

	/** A handshake request for {@code uri}, which may be <jk>null</jk> or relative. */
	private static HandshakeRequest requestAt(String uri, Principal principal, String...headerPairs) {
		var r = request("", principal, headerPairs);
		when(r.getRequestURI()).thenReturn(uri == null ? null : URI.create(uri));
		return r;
	}

	private static boolean allowed(Map<String,Object> props) {
		return props.get(BusWebSocketConfigurator.REFUSAL_PROPERTY) == null && props.get(BusWebSocketConfigurator.SESSION_PROPERTY) != null;
	}

	//------------------------------------------------------------------------------------------------------------------
	// d) checkOrigin with a CsrfBoundary: well-formed only, since the Host is not visible yet
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_csrfCheckOriginRejectsMissingOrigin() {
		assertFalse(withCsrf().checkOrigin(null));
	}

	@Test void d02_csrfCheckOriginRejectsBlank() {
		var c = withCsrf();
		assertFalse(c.checkOrigin(""));
		assertFalse(c.checkOrigin("   "));
	}

	@Test void d03_csrfCheckOriginRejectsNull() {
		var c = withCsrf();
		assertFalse(c.checkOrigin("null"));
		assertFalse(c.checkOrigin("NULL"));
	}

	@Test void d04_csrfCheckOriginRejectsUnparseable() {
		var c = withCsrf();
		for (var o : List.of("app.example", "http://", "http://app.example/", "http://app.example/path", "http://user@app.example",
				"http://app.example?x", "http://app.example:0", "http://app.example:080", "http://app .example", " http://app.example",
				"http://[::1", "http://[fe80::1%25en0]"))
			assertFalse(c.checkOrigin(o), o);
	}

	@Test void d05_csrfCheckOriginRejectsNonHttpScheme() {
		var c = withCsrf();
		for (var o : List.of("file://", "file://app.example", "chrome-extension://abcdefghijklmnop", "ws://app.example", "ftp://app.example"))
			assertFalse(c.checkOrigin(o), o);
	}

	@Test void d06_csrfCheckOriginPassesWellFormed() {
		var c = withCsrf();
		for (var o : List.of(APP_ORIGIN, "https://other.example", "HTTP://App.Example:8080", "http://[::1]:8080", "https://10.0.0.1"))
			assertTrue(c.checkOrigin(o), o);
	}

	@Test void d07_loopbackCheckOriginUnchanged() {
		var c = withBoundary();
		assertTrue(c.checkOrigin(ORIGIN));
		assertFalse(c.checkOrigin("http://localhost:8080"), "well-formed is not enough for a loopback boundary");
		assertFalse(c.checkOrigin("https://other.example"));
		assertEquals(Set.of(ORIGIN), BusWebSocketConfigurator.allowedOrigins(bus.policy()));
	}

	//------------------------------------------------------------------------------------------------------------------
	// e) modifyHandshake with a CsrfBoundary: Origin against the handshake's own Host and scheme
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_handshakeSameOriginWsMapsToHttp() throws Exception {
		var c = withCsrf();
		var id = openCsrf(JB);
		assertTrue(allowed(handshake(c, requestAt("ws://" + APP + path(id), JB, "Host", APP, "Origin", APP_ORIGIN))));
		assertEquals(4403, refusalCode(handshake(c, requestAt("ws://" + APP + path(id), JB, "Host", APP, "Origin", "https://" + APP))));
	}

	@Test void e02_wssMapsToHttps() throws Exception {
		var c = withCsrf();
		var id = openCsrf(JB);
		assertTrue(allowed(handshake(c, requestAt("wss://" + APP + path(id), JB, "Host", APP, "Origin", "https://" + APP))));
		assertEquals(4403, refusalCode(handshake(c, requestAt("wss://" + APP + path(id), JB, "Host", APP, "Origin", APP_ORIGIN))));
	}

	@Test void e03_httpAndHttpsUriUsedAsIs() throws Exception {
		var c = withCsrf();
		var id = openCsrf(JB);
		assertTrue(allowed(handshake(c, requestAt("http://" + APP + path(id), JB, "Host", APP, "Origin", APP_ORIGIN))));
		assertTrue(allowed(handshake(c, requestAt("https://" + APP + path(id), JB, "Host", APP, "Origin", "https://" + APP))));
		assertTrue(allowed(handshake(c, requestAt("WSS://" + APP + path(id), JB, "Host", APP, "Origin", "https://" + APP))), "the scheme is case-insensitive");
	}

	@Test void e04_crossSiteOriginRefused() throws Exception {
		var c = withCsrf();
		var id = openCsrf(JB);
		var uri = "ws://" + APP + path(id);
		for (var o : List.of("http://evil.example", "http://app.example.evil.example", "http://evil.app.example", "http://app.example."))
			assertEquals(4403, refusalCode(handshake(c, requestAt(uri, JB, "Host", APP, "Origin", o))), o);
		assertEquals(4403, refusalCode(handshake(c, requestAt(uri, JB, "Host", APP))), "no Origin at all");
		assertEquals(4403, refusalCode(handshake(c, requestAt(uri, JB, "Host", APP, "Origin", "null"))));
		var two = requestAt(uri, JB, "Host", APP);
		when(two.getHeaders()).thenReturn(Map.of("Host", List.of(APP), "Origin", List.of(APP_ORIGIN, "http://evil.example")));
		assertEquals(4403, refusalCode(handshake(c, two)), "two Origin values");
	}

	@Test void e05_sameSiteFetchSiteRefused() throws Exception {
		var c = withCsrf();
		var id = openCsrf(JB);
		assertEquals(4403, refusalCode(handshake(c, requestAt("ws://" + APP + path(id), JB, "Host", APP, "Origin", APP_ORIGIN, "Sec-Fetch-Site", "same-site"))));
	}

	@Test void e06_fetchSiteCrossSiteRefused() throws Exception {
		var c = withCsrf();
		var id = openCsrf(JB);
		for (var site : List.of("cross-site", "none", "Same-Origin", " same-origin"))
			assertEquals(4403, refusalCode(handshake(c, requestAt("ws://" + APP + path(id), JB, "Host", APP, "Origin", APP_ORIGIN, "Sec-Fetch-Site", site))), site);
		assertTrue(allowed(handshake(c, requestAt("ws://" + APP + path(id), JB, "Host", APP, "Origin", APP_ORIGIN, "Sec-Fetch-Site", "same-origin"))));
	}

	@Test void e07_fetchSiteAbsentAllowedWithMatchingOrigin() throws Exception {
		var c = withCsrf();
		var id = openCsrf(JB);
		assertTrue(allowed(handshake(c, requestAt("ws://" + APP + path(id), JB, "Host", APP, "Origin", APP_ORIGIN))));
	}

	@Test void e08_ipv6Host() throws Exception {
		var c = withCsrf();
		var id = openCsrf(JB);
		var uri = "ws://[::1]:8080" + path(id);
		assertTrue(allowed(handshake(c, requestAt(uri, JB, "Host", "[::1]:8080", "Origin", "http://[::1]:8080"))));
		assertTrue(allowed(handshake(c, requestAt(uri, JB, "Host", "[::1]:8080", "Origin", "http://[::1]:8080".toUpperCase(Locale.ROOT)))));
		assertEquals(4403, refusalCode(handshake(c, requestAt(uri, JB, "Host", "[::1]:8080", "Origin", "http://[0:0:0:0:0:0:0:1]:8080"))),
			"no re-canonicalization");
		assertEquals(4403, refusalCode(handshake(c, requestAt(uri, JB, "Host", "::1:8080", "Origin", "http://[::1]:8080"))), "unbracketed Host");
	}

	@Test void e09_defaultPortEquivalence() throws Exception {
		var c = withCsrf();
		var id = openCsrf(JB);
		assertTrue(allowed(handshake(c, requestAt("ws://" + APP + path(id), JB, "Host", APP + ":80", "Origin", APP_ORIGIN))));
		assertTrue(allowed(handshake(c, requestAt("ws://" + APP + path(id), JB, "Host", APP, "Origin", APP_ORIGIN + ":80"))));
		assertTrue(allowed(handshake(c, requestAt("wss://" + APP + path(id), JB, "Host", APP + ":443", "Origin", "https://" + APP))));
		assertEquals(4403, refusalCode(handshake(c, requestAt("ws://" + APP + path(id), JB, "Host", APP + ":443", "Origin", APP_ORIGIN))));
	}

	@Test void e10_portMismatchRefused() throws Exception {
		var c = withCsrf();
		var id = openCsrf(JB);
		assertEquals(4403, refusalCode(handshake(c, requestAt("ws://" + APP + ":8080" + path(id), JB, "Host", APP + ":8080", "Origin", APP_ORIGIN + ":8081"))));
	}

	@Test void e11_schemeMismatchRefused() throws Exception {
		var c = withCsrf();
		var id = openCsrf(JB);
		assertEquals(4403, refusalCode(handshake(c, requestAt("http://" + APP + path(id), JB, "Host", APP, "Origin", "https://" + APP))));
		assertEquals(4403, refusalCode(handshake(c, requestAt("ws://" + APP + path(id), JB, "Host", APP, "Origin", "https://" + APP))));
	}

	@Test void e12_allowedHostsMissRefused() throws Exception {
		var c = withCsrf(CsrfBoundary.create().tokens(CsrfTokens.perProcess()).allowedHosts(APP));
		var id = openCsrf(JB);
		assertEquals(4403, refusalCode(handshake(c, requestAt("ws://rebind.example" + path(id), JB, "Host", "rebind.example", "Origin", "http://rebind.example"))));
		assertTrue(allowed(handshake(c, requestAt("ws://" + APP + path(id), JB, "Host", APP + ":8080", "Origin", APP_ORIGIN + ":8080"))), "ports are ignored");
	}

	@Test void e13_xForwardedHostIgnored() throws Exception {
		var c = withCsrf();
		var id = openCsrf(JB);
		var r = requestAt("ws://internal:8080" + path(id), JB, "Host", "internal:8080", "Origin", "https://" + APP,
			"X-Forwarded-Host", APP, "X-Forwarded-Proto", "https", "Forwarded", "host=" + APP + ";proto=https");
		assertEquals(4403, refusalCode(handshake(c, r)));
		assertEquals(4403, refusalCode(handshake(c, requestAt("ws://" + APP + path(id), JB, "X-Forwarded-Host", APP, "Origin", APP_ORIGIN,
			"Sec-WebSocket-Key", "k"))), "no Host at all");
	}

	/** Opens a websocket session through a POST that carries the forwarded headers the trusting boundary needs. */
	private String openForwarded(Principal principal) throws Exception {
		var token = csrf.tokenFor(mock(jakarta.servlet.http.HttpServletRequest.class));
		return BusFixture.open(C, principal, "websocket", DOWN, UP, "Host", "internal:8080", "Origin", "https://" + APP,
			"X-Forwarded-Host", APP, "X-Forwarded-Proto", "https", "X-Csrf-Token", token);
	}

	private BusWebSocketConfigurator withForwarding() {
		return withCsrf(CsrfBoundary.create().tokens(CsrfTokens.perProcess()).trustedForwarding(CsrfBoundary.ForwardedSource.X_FORWARDED));
	}

	@Test void h01_offIgnoresForwarded() throws Exception {
		// e13 again, named for the opt-in: the default boundary never reads the forwarded headers.
		var c = withCsrf();
		var id = openCsrf(JB);
		var r = requestAt("ws://internal:8080" + path(id), JB, "Host", "internal:8080", "Origin", "https://" + APP,
			"X-Forwarded-Host", APP, "X-Forwarded-Proto", "https");
		assertEquals(4403, refusalCode(handshake(c, r)));
	}

	@Test void h02_onAdmitsRewrittenHostSameOrigin() throws Exception {
		var c = withForwarding();
		var id = openForwarded(JB);
		var r = requestAt("ws://internal:8080" + path(id), JB, "Host", "internal:8080", "Origin", "https://" + APP,
			"X-Forwarded-Host", APP, "X-Forwarded-Proto", "https");
		assertTrue(allowed(handshake(c, r)));
	}

	@Test void h03_forwardedHostNotMatchingOriginRefused() throws Exception {
		var c = withForwarding();
		var id = openForwarded(JB);
		var r = requestAt("ws://internal:8080" + path(id), JB, "Host", "internal:8080", "Origin", "https://evil.example",
			"X-Forwarded-Host", APP, "X-Forwarded-Proto", "https");
		assertEquals(4403, refusalCode(handshake(c, r)));
		var noHeaders = requestAt("ws://internal:8080" + path(id), JB, "Host", "internal:8080", "Origin", "https://" + APP);
		assertEquals(4403, refusalCode(handshake(c, noHeaders)));
	}

	@Test void h04_twoSpellingsRefused() throws Exception {
		var c = withForwarding();
		var id = openForwarded(JB);
		var r = requestAt("ws://internal:8080" + path(id), JB, "Host", "internal:8080", "Origin", "https://" + APP,
			"X-Forwarded-Host", APP, "x-forwarded-host", APP, "X-Forwarded-Proto", "https");
		assertEquals(4403, refusalCode(handshake(c, r)));
	}

	@Test void h05_usesTheHeaderMapOverload() throws Exception {
		// The list-based checkHandshake forms always refuse a trusting boundary; the configurator must use the map form.
		var c = withForwarding();
		var id = openForwarded(JB);
		var r = requestAt("ws://internal:8080" + path(id), JB, "Host", "internal:8080", "Origin", "https://" + APP,
			"X-Forwarded-Host", APP, "X-Forwarded-Proto", "https", "Sec-Fetch-Site", "same-origin");
		assertTrue(allowed(handshake(c, r)));
	}

	@Test void e14_untrustworthyRequestUriFailsClosed() throws Exception {
		var c = withCsrf();
		var id = openCsrf(JB);
		for (var uri : Arrays.asList(path(id), "ftp://" + APP + path(id), "//" + APP + path(id), null))
			assertEquals(4403, refusalCode(handshake(c, requestAt(uri, JB, "Host", APP, "Origin", APP_ORIGIN))), String.valueOf(uri));
	}

	@Test void e15_refusalSetsRefusalPropertyWithoutEchoingValues() throws Exception {
		var c = withCsrf();
		var id = openCsrf(JB);
		var props = handshake(c, requestAt("ws://" + APP + path(id), JB, "Host", APP, "Origin", "http://evil-marker.example"));
		assertSame(BusWebSocketConfigurator.REFUSED, props.get(BusWebSocketConfigurator.REFUSAL_PROPERTY));
		assertEquals(Set.of(BusWebSocketConfigurator.REFUSAL_PROPERTY), props.keySet());
		assertFalse(props.toString().contains("evil-marker"), props::toString);
	}

	//------------------------------------------------------------------------------------------------------------------
	// g) Handshakes over HTTP/2 (RFC 8441): no Host, no Sec-WebSocket-Key, no Upgrade; the request URI's authority stands in
	//------------------------------------------------------------------------------------------------------------------

	@Test void g01_csrfNoHostNoKeyIsJudgedOnTheUri() throws Exception {
		var c = withCsrf();
		var id = openCsrf(JB);
		assertTrue(allowed(handshake(c, requestAt("http://" + APP + path(id), JB, "Origin", APP_ORIGIN))));
		assertTrue(allowed(handshake(c, requestAt("http://" + APP + path(id), JB, "origin", APP_ORIGIN, "sec-fetch-site", "same-origin"))));
	}

	@Test void g02_aSecWebSocketKeyMeansRfc6455AndNeedsAHost() throws Exception {
		// The mutation row: deleting the key/Upgrade evidence in HandshakeHosts must fail these.
		var c = withCsrf();
		var id = openCsrf(JB);
		var uri = "http://" + APP + path(id);
		assertEquals(4403, refusalCode(handshake(c, requestAt(uri, JB, "Origin", APP_ORIGIN, "Sec-WebSocket-Key", "k"))));
		assertEquals(4403, refusalCode(handshake(c, requestAt(uri, JB, "Origin", APP_ORIGIN, "sec-websocket-key", "k"))));
		assertEquals(4403, refusalCode(handshake(c, requestAt(uri, JB, "Origin", APP_ORIGIN, "Sec-WebSocket-Key", ""))));
	}

	@Test void g03_upgradeMeansRfc6455AndNeedsAHost() throws Exception {
		var c = withCsrf();
		var id = openCsrf(JB);
		var uri = "http://" + APP + path(id);
		assertEquals(4403, refusalCode(handshake(c, requestAt(uri, JB, "Origin", APP_ORIGIN, "Upgrade", "websocket"))));
		assertEquals(4403, refusalCode(handshake(c, requestAt(uri, JB, "Origin", APP_ORIGIN, "upgrade", "websocket"))));
	}

	@Test void g04_foreignOriginOrHostIsRefused() throws Exception {
		var c = withCsrf();
		var id = openCsrf(JB);
		assertEquals(4403, refusalCode(handshake(c, requestAt("http://" + APP + path(id), JB, "Origin", "http://evil.example"))));
		assertEquals(4403, refusalCode(handshake(c, requestAt("http://" + APP + path(id), JB))), "no Origin");
	}

	@Test void g04b_allowedHostsAppliesToTheUriHost() throws Exception {
		var listed = withCsrf(CsrfBoundary.create().tokens(CsrfTokens.perProcess()).allowedHosts(APP));
		var id = openCsrf(JB);
		assertEquals(4403, refusalCode(handshake(listed, requestAt("http://rebind.example" + path(id), JB, "Origin", "http://rebind.example"))));
		assertTrue(allowed(handshake(listed, requestAt("http://" + APP + path(id), JB, "Origin", APP_ORIGIN))));
	}

	@Test void g05_aRealHostIsJudgedInsteadOfTheUri() throws Exception {
		var c = withCsrf();
		var id = openCsrf(JB);
		var uri = "http://" + APP + path(id);
		assertTrue(allowed(handshake(c, requestAt(uri, JB, "Host", "other.example", "Origin", "http://other.example"))));
		assertEquals(4403, refusalCode(handshake(c, requestAt(uri, JB, "Host", "other.example", "Origin", APP_ORIGIN))));
	}

	@Test void g06_theUriPortAndSchemeAreKept() throws Exception {
		var c = withCsrf();
		var id = openCsrf(JB);
		var uri = "https://" + APP + ":8443" + path(id);
		assertTrue(allowed(handshake(c, requestAt(uri, JB, "Origin", "https://" + APP + ":8443"))));
		assertEquals(4403, refusalCode(handshake(c, requestAt(uri, JB, "Origin", "https://" + APP))));
		assertEquals(4403, refusalCode(handshake(c, requestAt("https://" + APP + path(id), JB, "Origin", "https://" + APP + ":8443"))));
		assertTrue(allowed(handshake(c, requestAt("https://" + APP + path(id), JB, "Origin", "https://" + APP))));
	}

	@Test void g07_aMissingUriIsRefused() throws Exception {
		var c = withCsrf();
		openCsrf(JB);
		assertEquals(4403, refusalCode(handshake(c, requestAt(null, JB, "Origin", APP_ORIGIN))));
	}

	@Test void g08_loopbackNoHostNoKeyIsJudgedOnTheUri() throws Exception {
		var c = withBoundary();
		var id = open(JB, "websocket", true);
		assertTrue(allowed(handshake(c, request(path(id), JB, "Origin", ORIGIN))));
		assertEquals(4403, refusalCode(handshake(c, requestAt("ws://127.0.0.1:8081" + path(id), JB, "Origin", ORIGIN))), "another port");
		assertEquals(4403, refusalCode(handshake(c, requestAt("ws://localhost:8080" + path(id), JB, "Origin", ORIGIN))), "another host");
		assertEquals(4403, refusalCode(handshake(c, request(path(id), JB, "Origin", ORIGIN, "Sec-WebSocket-Key", "k"))), "RFC 6455 needs a Host");
		assertEquals(4403, refusalCode(handshake(c, request(path(id), JB, "Origin", ORIGIN, "upgrade", "websocket"))), "RFC 6455 needs a Host");
	}

	@Test void g09_loopbackDefaultPortsAreFilled() throws Exception {
		use(policy().boundary(LoopbackBoundary.create().authority("127.0.0.1").token(TOKEN).build()).build());
		var c = new BusWebSocketConfigurator(bus, BusWebSocketConfigurator.allowedOrigins(bus.policy()));
		var id = BusFixture.open(C, JB, "websocket", DOWN, UP, "Host", "127.0.0.1", "Origin", "http://127.0.0.1", "Sec-Fetch-Site", "same-origin",
			"X-Csrf-Token", TOKEN.value());
		assertTrue(allowed(handshake(c, requestAt("ws://127.0.0.1" + path(id), JB, "Origin", "http://127.0.0.1"))));
		assertTrue(allowed(handshake(c, requestAt("http://127.0.0.1:80" + path(id), JB, "Origin", "http://127.0.0.1"))));
		assertEquals(4403, refusalCode(handshake(c, requestAt("https://127.0.0.1" + path(id), JB, "Origin", "http://127.0.0.1"))), "wss defaults to 443");
		// A real Host keeps the string compare: "127.0.0.1:80" is not "127.0.0.1".
		assertEquals(4403, refusalCode(handshake(c, requestAt("ws://127.0.0.1" + path(id), JB, "Host", "127.0.0.1:80", "Origin", "http://127.0.0.1"))));
		assertTrue(allowed(handshake(c, requestAt("ws://127.0.0.1" + path(id), JB, "Host", "127.0.0.1", "Origin", "http://127.0.0.1"))));
	}

	//------------------------------------------------------------------------------------------------------------------
	// i) Peer allow-list: the peer is the address Jetty puts in the per-handshake user properties
	//------------------------------------------------------------------------------------------------------------------

	private static final String PEER_PROPERTY = "jakarta.websocket.endpoint.remoteAddress";
	private static final Object ABSENT = new Object();

	/** The session POST is judged on its own Host (the mock client's peer is untrusted), the handshake on the forwarded origin. */
	private BusWebSocketConfigurator withProxies(String...ranges) {
		return withCsrf(CsrfBoundary.create().tokens(CsrfTokens.perProcess()).trustedForwarding(CsrfBoundary.ForwardedSource.X_FORWARDED)
			.trustedProxies(ranges));
	}

	private static HandshakeRequest forwardedAt(String id, String...extra) {
		var headers = new ArrayList<>(List.of("Host", "internal:8080", "Origin", "https://" + APP));
		headers.addAll(List.of(extra));
		return requestAt("ws://internal:8080" + path(id), JB, headers.toArray(new String[0]));
	}

	private static Map<String,Object> handshakeFrom(BusWebSocketConfigurator c, HandshakeRequest r, Object peer) {
		var props = new HashMap<String,Object>();
		if (peer != ABSENT)
			props.put(PEER_PROPERTY, peer);
		handshake(c, r, props);
		return props;
	}

	private static InetSocketAddress addr(String ip) throws Exception {
		return new InetSocketAddress(InetAddress.getByName(ip), 5000);
	}

	private static final String[] FORWARDED = { "X-Forwarded-Host", APP, "X-Forwarded-Proto", "https" };

	@Test void i01_jettyPeerPropertyTrusted() throws Exception {
		var c = withProxies("10.0.0.0/8");
		var id = openCsrf(JB);
		assertTrue(allowed(handshakeFrom(c, forwardedAt(id, FORWARDED), addr("10.1.2.3"))));
	}

	@Test void i02_untrustedPeerRefused() throws Exception {
		var c = withProxies("10.0.0.0/8");
		var id = openCsrf(JB);
		assertEquals(4403, refusalCode(handshakeFrom(c, forwardedAt(id, FORWARDED), addr("203.0.113.7"))));
	}

	@Test void i03_absentOrForeignPropertyIsUnknown() throws Exception {
		var c = withProxies("10.0.0.0/8");
		var id = openCsrf(JB);
		var r = forwardedAt(id, FORWARDED);
		assertEquals(4403, refusalCode(handshakeFrom(c, r, ABSENT)));
		assertEquals(4403, refusalCode(handshakeFrom(c, r, "10.1.2.3")));
		assertEquals(4403, refusalCode(handshakeFrom(c, r, InetSocketAddress.createUnresolved("10.1.2.3", 5000))));
		assertEquals(4403, refusalCode(handshakeFrom(c, r, UnixDomainSocketAddress.of("/tmp/x.sock"))));
		assertEquals(4403, refusalCode(handshakeFrom(c, r, null)));
		// Without the forwarded header an unknown peer is judged on the container view, which here is "internal:8080" over http.
		var plain = requestAt("ws://internal:8080" + path(id), JB, "Host", "internal:8080", "Origin", "http://internal:8080");
		assertTrue(allowed(handshakeFrom(c, plain, ABSENT)));
	}

	@Test void i04_propertyNotReadWhenUnset() throws Exception {
		var c = withForwarding();
		var id = openForwarded(JB);
		var r = forwardedAt(id, FORWARDED);
		assertTrue(allowed(handshakeFrom(c, r, ABSENT)));
		assertTrue(allowed(handshakeFrom(c, r, "not an address")));
		assertTrue(allowed(handshakeFrom(c, r, addr("203.0.113.7"))));
	}

	@Test void i05_noReverseLookup() throws Exception {
		var c = withProxies("10.0.0.0/8");
		var id = openCsrf(JB);
		var named = new InetSocketAddress(InetAddress.getByAddress("proxy.example", new byte[] { 10, 1, 2, 3 }), 5000);
		assertTrue(allowed(handshakeFrom(c, forwardedAt(id, FORWARDED), named)));
	}

	//------------------------------------------------------------------------------------------------------------------
	// f) onOpen after a CsrfBoundary handshake
	//------------------------------------------------------------------------------------------------------------------

	/** Runs onOpen with the handshake's user properties and returns the close code, checking no frame was sent first. */
	private int openWith(Map<String,Object> props) throws Exception {
		var ws = mock(Session.class);
		var config = mock(EndpointConfig.class);
		when(config.getUserProperties()).thenReturn(props);
		new BusWebSocketEndpoint(bus).onOpen(ws, config);
		var reason = org.mockito.ArgumentCaptor.forClass(CloseReason.class);
		verify(ws).close(reason.capture());
		verify(ws, never()).getAsyncRemote();
		verify(ws, never()).getBasicRemote();
		verify(ws, never()).addMessageHandler(eq(String.class), org.mockito.ArgumentMatchers.<MessageHandler.Whole<String>>any());
		return reason.getValue().getCloseCode().getCode();
	}

	@Test void f01_onOpenClosesWithRefusedBeforeAnyFrame() throws Exception {
		var c = withCsrf();
		var id = openCsrf(JB);
		var props = handshake(c, requestAt("ws://" + APP + path(id), JB, "Host", APP, "Origin", "http://evil.example"));
		assertEquals(4403, openWith(props));
		assertFalse(bus.session(id, JB).orElseThrow().isConnected());
	}

	@Test void f02_unknownSessionStill4401() throws Exception {
		var c = withCsrf();
		var props = handshake(c, requestAt("ws://" + APP + path("0".repeat(64)), JB, "Host", APP, "Origin", APP_ORIGIN));
		assertEquals(4401, refusalCode(props));
		assertEquals(4401, openWith(props));
	}

	@Test void f03_csrfBoundarySessionRequired() throws Exception {
		var c = withCsrf();
		openCsrf(JB);
		var props = handshake(c, requestAt("ws://" + APP + BusWebSockets.PREFIX + "/", JB, "Host", APP, "Origin", APP_ORIGIN));
		assertEquals(4401, refusalCode(props));
		assertEquals(4401, openWith(props));
	}
}
