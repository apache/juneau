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
package org.apache.juneau.console.bus.websocket;

import java.net.*;
import java.util.*;

import org.apache.juneau.console.bus.*;
import org.apache.juneau.rest.server.filter.*;

import jakarta.websocket.*;
import jakarta.websocket.server.*;

/**
 * The handshake guards of the bus's WebSocket endpoint (Origin, Host, fetch-site and capability checks).  Not for
 * direct use: it is public only because the container calls it.  {@link BusWebSockets#register} installs it.
 *
 * <ul>
 * 	<li>{@link #checkOrigin(String)}: runs before any upgrade, and a refusal is the container's {@code 403}.  Under a
 * 		{@link LoopbackBoundary} or {@code allowedOrigins} it is an exact match.  Under a {@link CsrfBoundary} it can
 * 		only see the {@code Origin} value, so it refuses a missing value, {@code null}, and anything that is not a
 * 		well-formed http(s) origin; a well-formed cross-site value passes it and is refused by the next step.
 * 	<li>{@link #modifyHandshake}: under a {@link LoopbackBoundary}, Host against the boundary's authority; under a
 * 		{@link CsrfBoundary}, {@link CsrfBoundary#checkHandshake(String, HandshakeHosts, Map)} with the scheme of the
 * 		handshake's request URI and its headers (so
 * 		{@link CsrfBoundary.Builder#trustedForwarding trusted forwarding}, when set, sees the forwarded headers).
 * 		Over HTTP/2 (RFC 8441), where the handshake has no {@code Host}, {@code Sec-WebSocket-Key} or
 * 		{@code Upgrade}, the request URI's authority stands in for {@code Host} ({@link HandshakeHosts}).  Otherwise
 * 		{@code Sec-Fetch-Site} (when present) must be {@code same-origin}.  Last, the capability and principal.  A
 * 		failure is recorded in the per-connection user properties, and {@link BusWebSocketEndpoint#onOpen} closes at
 * 		once with {@code 4403} ({@code bus:refused}) or {@code 4401} ({@code bus:unknown-session}).
 * </ul>
 *
 * <p>
 * The session id is read from the request path only.  It is never logged.
 *
 * <p>
 * The refusal and session are stored in the user-properties map of the handshake, which relies on the container giving
 * each handshake its own copy of that map (WebSocket 2.1 and later; Jetty and Tomcat do).  A container that shares one
 * map across handshakes would mix up concurrent connections.
 *
 * <p>
 * With {@link org.apache.juneau.rest.server.filter.CsrfBoundary.Builder#trustedProxies(String...) trustedProxies} set, the handshake's peer is read from the
 * Jetty user property {@code jakarta.websocket.endpoint.remoteAddress}.  Jakarta WebSocket has no standard peer
 * address; a container that records none (Tomcat) leaves the peer unknown, and an unknown peer is not trusted, so a
 * handshake that carries a forwarded host is refused there.  The property is believable only because Jetty writes it
 * into the per-handshake copy of the user properties; a container that copied client input into it would break that.
 *
 * <p>
 * Trusted forwarding selects a hop by counting entries from the right, which relies on the container returning the
 * repeated lines of a forwarded header in the order they arrived on the wire.  Jetty and Tomcat both do.  A container
 * that reorders them would select the wrong hop.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 *   <jc>// Do not construct this class; register the endpoint instead:</jc>
 *   BusWebSockets.<jsm>register</jsm>(<jv>servletContext</jv>, JobsRest.<jsf>BUS</jsf>);
 * </p>
 *
 * @since 10.0.0
 */
public final class BusWebSocketConfigurator extends ServerEndpointConfig.Configurator {

	/** User-property key of the {@link BusSession} a good handshake resolved. */
	static final String SESSION_PROPERTY = "juneau.bus.session";

	/** User-property key of the {@link Refusal} a failed handshake recorded. */
	static final String REFUSAL_PROPERTY = "juneau.bus.refusal";

	/**
	 * User-property key under which Jetty records the TCP peer of a handshake.  Non-standard.  It is trusted only
	 * because Jetty writes it into the per-handshake copy of the user properties after the client's headers are read, so
	 * a client cannot supply it.
	 */
	static final String PEER_PROPERTY = "jakarta.websocket.endpoint.remoteAddress";

	/** A handshake failure that {@code onOpen} turns into a close frame. */
	record Refusal(int code, String reason) {}

	/** Bad capability, principal mismatch or wrong transport: the browser re-POSTs the session. */
	static final Refusal UNKNOWN_SESSION = new Refusal(4401, "bus:unknown-session");

	/** Bad Host, {@code Origin} or {@code Sec-Fetch-Site}: the browser fails the bridge. */
	static final Refusal REFUSED = new Refusal(4403, "bus:refused");

	private final ServerBus bus;
	private final Set<String> origins;
	private final LoopbackBoundary loopback;
	private final CsrfBoundary csrf;

	BusWebSocketConfigurator(ServerBus bus, Set<String> origins) {
		this.bus = bus;
		this.origins = Set.copyOf(origins);
		var boundary = bus.policy().boundary();
		this.loopback = boundary instanceof LoopbackBoundary lb ? lb : null;
		this.csrf = boundary instanceof CsrfBoundary cb ? cb : null;
	}

	/**
	 * The exact Origin values the policy allows: a {@link LoopbackBoundary}'s origin, nothing for a
	 * {@link CsrfBoundary} (which matches each handshake against its own Host instead), else {@code allowedOrigins}.
	 */
	static Set<String> allowedOrigins(BusPolicy policy) {
		var boundary = policy.boundary();
		if (boundary instanceof LoopbackBoundary lb)
			return Set.of(lb.origin());
		if (boundary instanceof CsrfBoundary)
			return Set.of();
		return Set.copyOf(policy.allowedOrigins());
	}

	@Override /* Configurator */
	public boolean checkOrigin(String originHeaderValue) {
		if (csrf != null)
			return csrf.acceptsHandshakeOrigin(originHeaderValue);
		return originHeaderValue != null && origins.contains(originHeaderValue);
	}

	@Override /* Configurator */
	public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
		// Jakarta WebSocket 2.1+ hands each handshake its own copy of the user properties; the removes are belt and braces.
		var props = sec.getUserProperties();
		props.remove(SESSION_PROPERTY);
		props.remove(REFUSAL_PROPERTY);
		var refusal = check(request, props);
		if (refusal != null)
			props.put(REFUSAL_PROPERTY, refusal);
	}

	@SuppressWarnings({
		"java:S3776" // Handshake gate: boundary variants and session lookup read best as one sequential check
	})
	private Refusal check(HandshakeRequest request, Map<String,Object> props) {
		var uri = request.getRequestURI();
		if (csrf != null) {
			var hosts = HandshakeHosts.of(request.getHeaders(), uri);
			var peer = csrf.trustedProxies().isEmpty() ? null : peer(props);
			var result = csrf.checkHandshake(uri == null ? null : uri.getScheme(), hosts, request.getHeaders(), peer);
			if (! result.isAllowed())
				return REFUSED;
		} else {
			if (loopback != null && ! loopback.acceptsHandshakeHost(HandshakeHosts.of(request.getHeaders(), uri), uri == null ? null : uri.getScheme()))
				return REFUSED;
			var site = header(request, "Sec-Fetch-Site");
			if (site != null && ! "same-origin".equals(site))
				return REFUSED;
		}
		var id = sessionId(uri);
		if (id == null)
			return UNKNOWN_SESSION;
		var session = bus.session(id, request.getUserPrincipal()).filter(s -> "websocket".equals(s.transport()));
		if (session.isEmpty())
			return UNKNOWN_SESSION;
		props.put(SESSION_PROPERTY, session.get());
		return null;
	}

	/**
	 * The TCP peer Jetty records in the handshake's user properties, or {@code null} when there is none to trust.
	 *
	 * <p>
	 * Only a resolved {@link InetSocketAddress} counts, and only its address bytes are used: never a host name, so
	 * nothing is looked up.  Tomcat records nothing, so the peer is unknown there and is not trusted.
	 */
	private static InetAddress peer(Map<String,Object> props) {
		return props.get(PEER_PROPERTY) instanceof InetSocketAddress a && ! a.isUnresolved() ? a.getAddress() : null;
	}

	/** The last path segment after {@link BusWebSockets#PREFIX}, or {@code null} when the path is malformed. */
	static String sessionId(URI uri) {
		var path = uri == null ? null : uri.getRawPath();
		var prefix = BusWebSockets.PREFIX + "/";
		var i = path == null ? -1 : path.lastIndexOf(prefix);
		if (i < 0)
			return null;
		var id = path.substring(i + prefix.length());
		return id.isEmpty() || id.indexOf('/') >= 0 ? null : id;
	}

	private static String header(HandshakeRequest request, String name) {
		var headers = request.getHeaders();
		if (headers != null)
			for (var e : headers.entrySet())
				if (name.equalsIgnoreCase(e.getKey()) && e.getValue() != null && ! e.getValue().isEmpty())
					return e.getValue().get(0);
		return null;
	}

	@Override /* Configurator */
	public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
		if (endpointClass != BusWebSocketEndpoint.class)
			throw new InstantiationException("BusWebSocketConfigurator: unexpected endpoint class " + endpointClass.getName());
		return endpointClass.cast(new BusWebSocketEndpoint(bus));
	}
}
