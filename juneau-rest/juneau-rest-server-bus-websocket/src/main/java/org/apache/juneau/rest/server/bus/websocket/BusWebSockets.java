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

import org.apache.juneau.rest.server.bus.*;

import jakarta.servlet.*;
import jakarta.websocket.*;
import jakarta.websocket.server.*;

/**
 * Registers the bus's WebSocket endpoint on the servlet container (Jakarta WebSocket 2.2).
 *
 * <p>
 * The endpoint is context-relative ({@value #PATH}) because WebSocket endpoints are not servlet-mapped.  Origin is
 * the CSRF control for the handshake, so registration refuses a policy with no exact Origin rule: the rule is
 * {@code LoopbackBoundary.origin()} when the policy has a boundary, otherwise {@code BusPolicy.allowedOrigins}.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 *   <jc>// In a ServletContextListener (or the microservice's start hook):</jc>
 *   <jk>public void</jk> contextInitialized(ServletContextEvent <jv>e</jv>) {
 *     BusWebSockets.<jsm>register</jsm>(<jv>e</jv>.getServletContext(), JobsRest.<jsf>BUS</jsf>);   <jc>// "/juneau-bus/ws/{sessionId}"</jc>
 *   }
 *
 *   <jc>// Jetty 12 (ee11), when you hold the ServletContextHandler:</jc>
 *   JakartaWebSocketServletContainerInitializer.<jsm>configure</jsm>(<jv>context</jv>,
 *     (<jv>servletContext</jv>, <jv>container</jv>) -&gt; BusWebSockets.<jsm>register</jsm>(<jv>container</jv>, JobsRest.<jsf>BUS</jsf>));
 * </p>
 *
 * @since 10.0.0
 */
public final class BusWebSockets {

	/** The context-relative endpoint path. */
	public static final String PATH = "/juneau-bus/ws/{sessionId}";

	/** {@link #PATH} without its path parameter; passed to {@link ServerBus#enableWebSocket(String)}. */
	public static final String PREFIX = "/juneau-bus/ws";

	static final String NO_CONTAINER = "BusWebSockets.register: the container exposes no jakarta.websocket ServerContainer (add the container's WebSocket module)";
	static final String NO_ORIGIN = "BusWebSockets.register: the policy has neither a boundary nor allowedOrigins; WebSocket bridges need an exact Origin rule";

	private BusWebSockets() {}

	/**
	 * Registers the endpoint on the container found in the servlet context.
	 *
	 * @param ctx The servlet context.
	 * @param bus The bus whose sessions the endpoint serves.
	 * @throws IllegalStateException no {@link ServerContainer} attribute, or no Origin rule.
	 */
	public static void register(ServletContext ctx, ServerBus bus) {
		if (! (ctx.getAttribute(ServerContainer.class.getName()) instanceof ServerContainer container))
			throw new IllegalStateException(NO_CONTAINER);
		register(container, bus);
	}

	/**
	 * Registers the endpoint on a container.
	 *
	 * <p>
	 * Enables the bus's WebSocket transport only after the container accepted the endpoint, so a session POST never
	 * advertises a path that is not served.
	 *
	 * @param container The container.
	 * @param bus The bus whose sessions the endpoint serves.
	 * @throws IllegalStateException no Origin rule, or the container refused the endpoint.
	 */
	public static void register(ServerContainer container, ServerBus bus) {
		var origins = BusWebSocketConfigurator.allowedOrigins(bus.policy());
		if (origins.isEmpty())
			throw new IllegalStateException(NO_ORIGIN);
		var config = ServerEndpointConfig.Builder.create(BusWebSocketEndpoint.class, PATH)
			.configurator(new BusWebSocketConfigurator(bus, origins))
			.build();
		try {
			container.addEndpoint(config);
		} catch (DeploymentException e) {
			throw new IllegalStateException(String.format("BusWebSockets.register: could not add endpoint '%s': %s", PATH, e.getMessage()), e);
		}
		bus.enableWebSocket(PREFIX);
	}
}
