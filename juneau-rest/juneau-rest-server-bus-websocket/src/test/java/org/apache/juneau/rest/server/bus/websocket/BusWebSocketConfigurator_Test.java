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
		assertEquals(4403, refusalCode(handshake(c, request(path(id), JB))));
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
}
