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

import static java.nio.charset.StandardCharsets.*;

import java.util.concurrent.*;
import java.util.logging.*;

import org.apache.juneau.rest.server.bus.*;

import jakarta.websocket.*;

/**
 * The per-connection endpoint of the bus's WebSocket transport.  Not for direct use: it is public only because the
 * container instantiates it through {@link BusWebSocketConfigurator}.
 *
 * <p>
 * It closes a refused handshake at once ({@code 4401} / {@code 4403}).  Otherwise it adapts the
 * {@link Session} to a {@link FrameSink}, connects it to the {@link ServerBus} (which sends the resync), forwards
 * inbound text frames to {@link ServerBus#receive} (which refuses anything but a {@code pub} frame with a JSON object
 * payload), and closes with {@code 1009} ({@code bus:frame-too-large}) on a frame over
 * {@code BusPolicy.maxFrameBytes}.  Nothing it logs contains the session id.
 *
 * <p>
 * It reads the per-connection user properties of its {@link EndpointConfig}.  The container gives each handshake its
 * own copy of that map (WebSocket 2.1 and later; Jetty and Tomcat do), which is what keeps concurrent connections apart.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 *   <jc>// Do not construct this class; register the endpoint instead:</jc>
 *   BusWebSockets.<jsm>register</jsm>(<jv>servletContext</jv>, JobsRest.<jsf>BUS</jsf>);
 * </p>
 *
 * @since 10.0.0
 */
public final class BusWebSocketEndpoint extends Endpoint {

	private static final Logger LOGGER = Logger.getLogger(BusWebSocketEndpoint.class.getName());

	private final ServerBus bus;
	private final Executor closer;
	private volatile BusSession busSession;
	private volatile WsFrameSink sink;

	BusWebSocketEndpoint(ServerBus bus) {
		this(bus, null);
	}

	/** @param closer Runs the sink's socket closes; {@code null} for the default (a daemon pool). */
	BusWebSocketEndpoint(ServerBus bus, Executor closer) {
		this.bus = bus;
		this.closer = closer;
	}

	@Override /* Endpoint */
	public void onOpen(Session session, EndpointConfig config) {
		var props = config.getUserProperties();
		if (props.get(BusWebSocketConfigurator.REFUSAL_PROPERTY) instanceof BusWebSocketConfigurator.Refusal r) {
			WsFrameSink.closeQuietly(session, r.code(), r.reason());
			return;
		}
		if (! (props.get(BusWebSocketConfigurator.SESSION_PROPERTY) instanceof BusSession s)) {
			var r = BusWebSocketConfigurator.UNKNOWN_SESSION;
			WsFrameSink.closeQuietly(session, r.code(), r.reason());
			return;
		}
		var policy = bus.policy();
		var maxBytes = policy.maxFrameBytes();
		session.setMaxTextMessageBufferSize(maxBytes);
		var k = closer == null ? new WsFrameSink(session, policy.maxQueuedFrames()) : new WsFrameSink(session, policy.maxQueuedFrames(), closer);
		busSession = s;
		sink = k;
		try {
			bus.connect(s, k);
		} catch (RuntimeException e) {
			LOGGER.log(Level.WARNING, "bus websocket connect failed: {0}", e.getClass().getName());
			k.markClosed();
			try {
				bus.disconnect(s, k);
			} catch (RuntimeException e2) {
				// The same fault that failed the connect; the sink is already marked closed.
			}
			WsFrameSink.closeQuietly(session, 1011, "bus:connect-failed");
			return;
		}
		session.addMessageHandler(String.class, text -> onText(session, s, text, maxBytes));
	}

	private void onText(Session session, BusSession s, String text, int maxBytes) {
		// A char is at least one byte, so a long enough string needs no encoding to be refused.
		if (text.length() > maxBytes || text.getBytes(UTF_8).length > maxBytes) {
			WsFrameSink.closeQuietly(session, CloseReason.CloseCodes.TOO_BIG.getCode(), "bus:frame-too-large");
			return;
		}
		bus.receive(s, text);
	}

	@Override /* Endpoint */
	public void onClose(Session session, CloseReason closeReason) {
		var k = sink;
		if (k != null) {
			k.markClosed();
			bus.disconnect(busSession, k);
		}
	}

	@Override /* Endpoint */
	public void onError(Session session, Throwable thr) {
		// Only the class: a message or stack can carry the request path, which holds the session id.
		LOGGER.log(Level.FINE, "bus websocket transport error: {0}", thr == null ? null : thr.getClass().getName());
	}
}
