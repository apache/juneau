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

import java.net.*;
import java.util.*;

import org.apache.juneau.rest.server.bus.*;
import org.apache.juneau.rest.server.filter.*;

import jakarta.websocket.*;
import jakarta.websocket.server.*;

/**
 * The handshake guards of the bus's WebSocket endpoint (Origin, Host, fetch-site and capability checks).  Not for direct use: it is public only
 * because the container calls it.  {@link BusWebSockets#register} installs it.
 *
 * <ul>
 * 	<li>{@link #checkOrigin(String)}: an exact match, so the container answers {@code 403} before any upgrade.
 * 	<li>{@link #modifyHandshake}: Host against the boundary's authority, {@code Sec-Fetch-Site} (when present) must be
 * 		{@code same-origin}, then the capability and principal.  A failure is recorded in the per-connection user
 * 		properties, and {@link BusWebSocketEndpoint#onOpen} closes at once with {@code 4403} ({@code bus:refused}) or
 * 		{@code 4401} ({@code bus:unknown-session}).
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

	/** A handshake failure that {@code onOpen} turns into a close frame. */
	record Refusal(int code, String reason) {}

	/** Bad capability, principal mismatch or wrong transport: the browser re-POSTs the session. */
	static final Refusal UNKNOWN_SESSION = new Refusal(4401, "bus:unknown-session");

	/** Bad Host or {@code Sec-Fetch-Site}: the browser fails the bridge. */
	static final Refusal REFUSED = new Refusal(4403, "bus:refused");

	private final ServerBus bus;
	private final Set<String> origins;
	private final String authority;

	BusWebSocketConfigurator(ServerBus bus, Set<String> origins) {
		this.bus = bus;
		this.origins = Set.copyOf(origins);
		this.authority = Optional.ofNullable(bus.policy().boundary()).map(LoopbackBoundary::authority).orElse(null);
	}

	/** The exact Origin values the policy allows: the boundary's origin when set, else {@code allowedOrigins}. */
	static Set<String> allowedOrigins(BusPolicy policy) {
		return Optional.ofNullable(policy.boundary())
			.map(b -> Set.of(b.origin()))
			.orElseGet(() -> Set.copyOf(policy.allowedOrigins()));
	}

	@Override /* Configurator */
	public boolean checkOrigin(String originHeaderValue) {
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

	private Refusal check(HandshakeRequest request, Map<String,Object> props) {
		if (authority != null && ! authority.equalsIgnoreCase(header(request, "Host")))
			return REFUSED;
		var site = header(request, "Sec-Fetch-Site");
		if (site != null && ! "same-origin".equals(site))
			return REFUSED;
		var id = sessionId(request.getRequestURI());
		if (id == null)
			return UNKNOWN_SESSION;
		var session = bus.session(id, request.getUserPrincipal()).filter(s -> "websocket".equals(s.transport()));
		if (session.isEmpty())
			return UNKNOWN_SESSION;
		props.put(SESSION_PROPERTY, session.get());
		return null;
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
