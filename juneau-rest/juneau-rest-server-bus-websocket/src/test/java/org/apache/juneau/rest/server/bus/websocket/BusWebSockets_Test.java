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

import java.security.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.bus.*;
import org.junit.jupiter.api.*;
import org.mockito.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.websocket.*;
import jakarta.websocket.server.*;

/** {@link BusWebSockets#register}: the missing-container and missing-Origin-rule errors in both forms, the endpoint config, and {@link ServerBus#enableWebSocket}. */
class BusWebSockets_Test extends TestBase {

	private ServerBus bus;
	private ServerContainer container;

	private static BusPolicy.Builder policy() {
		return BusPolicy.create().downstream("ops.jobs", true);
	}

	private void use(BusPolicy p) {
		bus = ServerBus.create(p);
		container = mock(ServerContainer.class);
	}

	@BeforeEach void setUp() {
		use(policy().allowedOrigins("https://ops.example.com").build());
	}

	@AfterEach void tearDown() {
		bus.close();
	}

	/** Whether the bus accepts a websocket session request right now. */
	private boolean websocketEnabled() throws BusRefusal {
		var req = (HttpServletRequest) java.lang.reflect.Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(),
			new Class<?>[]{HttpServletRequest.class}, (proxy, method, args) -> method.getReturnType() == boolean.class ? (Object) false : null);
		try {
			bus.openSession(req, JsonMap.of("bridge", "ops", "transport", "websocket", "downstream", List.of("ops.jobs")));
			return true;
		} catch (BusRefusal e) {
			assertEquals(501, e.status());
			return false;
		}
	}

	@Test void a01_noServerContainerIsRefused() throws Exception {
		var ctx = mock(ServletContext.class);
		var e = assertThrows(IllegalStateException.class, () -> BusWebSockets.register(ctx, bus));
		assertEquals("BusWebSockets.register: the container exposes no jakarta.websocket ServerContainer (add the container's WebSocket module)", e.getMessage());
		assertFalse(websocketEnabled());
	}

	@Test void a02_noOriginRuleIsRefused() throws Exception {
		bus.close();
		use(policy().build());
		var e = assertThrows(IllegalStateException.class, () -> BusWebSockets.register(container, bus));
		assertEquals("BusWebSockets.register: the policy has neither a boundary nor allowedOrigins; WebSocket bridges need an exact Origin rule", e.getMessage());
		verify(container, never()).addEndpoint(any(ServerEndpointConfig.class));
		assertFalse(websocketEnabled());
	}

	@Test void b01_registersTheEndpoint_thenEnablesWebSocket() throws Exception {
		doAnswer(i -> {
			assertFalse(websocketEnabled(), "the transport is enabled only after the container accepted the endpoint");
			return null;
		}).when(container).addEndpoint(any(ServerEndpointConfig.class));
		BusWebSockets.register(container, bus);
		var c = ArgumentCaptor.forClass(ServerEndpointConfig.class);
		verify(container).addEndpoint(c.capture());
		assertEquals("/juneau-bus/ws/{sessionId}", c.getValue().getPath());
		assertEquals(BusWebSocketEndpoint.class, c.getValue().getEndpointClass());
		assertInstanceOf(BusWebSocketConfigurator.class, c.getValue().getConfigurator());
		assertTrue(websocketEnabled());
	}

	@Test void b02_servletContextFormFindsTheContainerAttribute() throws Exception {
		var ctx = mock(ServletContext.class);
		when(ctx.getAttribute(ServerContainer.class.getName())).thenReturn(container);
		BusWebSockets.register(ctx, bus);
		verify(container).addEndpoint(any(ServerEndpointConfig.class));
		assertTrue(websocketEnabled());
	}

	@Test void b03_deploymentFailureIsWrapped_andWebSocketStaysDisabled() throws Exception {
		doThrow(new DeploymentException("duplicate path")).when(container).addEndpoint(any(ServerEndpointConfig.class));
		var e = assertThrows(IllegalStateException.class, () -> BusWebSockets.register(container, bus));
		assertEquals("BusWebSockets.register: could not add endpoint '/juneau-bus/ws/{sessionId}': duplicate path", e.getMessage());
		assertFalse(websocketEnabled());
	}

	@Test void b04_theBoundaryAloneIsAnOriginRule() throws Exception {
		bus.close();
		use(policy().boundary(org.apache.juneau.rest.server.filter.LoopbackBoundary.create().authority("127.0.0.1:8080")
			.token(org.apache.juneau.rest.server.filter.SynchronizerToken.of("t")).build()).build());
		BusWebSockets.register(container, bus);
		verify(container).addEndpoint(any(ServerEndpointConfig.class));
	}

	@Test void c01_pathConstants() {
		assertEquals(BusWebSockets.PREFIX + "/{sessionId}", BusWebSockets.PATH);
		assertEquals(BusEventsMixin.BUS_PREFIX + "/ws", BusWebSockets.PREFIX);
	}
}
