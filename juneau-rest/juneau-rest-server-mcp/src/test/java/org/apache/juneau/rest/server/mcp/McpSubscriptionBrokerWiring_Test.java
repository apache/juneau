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
package org.apache.juneau.rest.server.mcp;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for the neutral opt-in subscription-broker hook (Phase 3, spec §"Neutral core" BeanStore-injection
 * requirement): default {@code null} means no broker beans are added; an override wires
 * {@link McpSubscriptionBroker} and {@link McpSubscriptions} into the request-scoped {@link BeanStore} as the
 * same instance, on both the servlet and mixin entrypoints.
 */
@SuppressWarnings({
	"resource" // MockRestClient is a Closeable test helper; lifetime is bounded by the test method.
})
class McpSubscriptionBrokerWiring_Test {

	static class RecordingRevision implements McpRevision {
		BeanStore lastCtx;
		@Override public String protocolVersion() { return "0000-00-00"; }
		@Override public McpDispatchResult dispatch(McpExchange exchange, McpServerConfig config, BeanStore ctx) {
			lastCtx = ctx;
			return new McpResponseResult(JsonRpcResponse.ok(exchange.request().getId(), Map.of("ok", true)));
		}
		@Override public int errorCode(McpErrorKind kind) { return -1; }
	}

	static final RecordingRevision NO_BROKER_SERVLET_REVISION = new RecordingRevision();
	static final RecordingRevision BROKER_SERVLET_REVISION = new RecordingRevision();
	static final RecordingRevision NO_BROKER_MIXIN_REVISION = new RecordingRevision();
	static final RecordingRevision BROKER_MIXIN_REVISION = new RecordingRevision();
	static final McpSubscriptionBroker SERVLET_BROKER = new BasicMcpSubscriptionBroker(4);
	static final McpSubscriptionBroker MIXIN_BROKER = new BasicMcpSubscriptionBroker(4);

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class NoBrokerServlet extends AbstractMcpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected McpServerConfig createMcpConfig() { return new McpServerConfig(); }
		@Override protected McpRevision revision() { return NO_BROKER_SERVLET_REVISION; }
	}

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class BrokerServlet extends AbstractMcpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected McpServerConfig createMcpConfig() { return new McpServerConfig(); }
		@Override protected McpRevision revision() { return BROKER_SERVLET_REVISION; }
		@Override protected McpSubscriptionBroker getSubscriptionBroker() { return SERVLET_BROKER; }
	}

	@Rest(path = "/api", serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class NoBrokerMixin extends BasicRestServlet implements McpEndpointMixin {
		private static final long serialVersionUID = 1L;
		@Override public McpServerConfig getMcpConfig() { return new McpServerConfig(); }
		@Override public McpRevision revision() { return NO_BROKER_MIXIN_REVISION; }
	}

	@Rest(path = "/api", serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class BrokerMixin extends BasicRestServlet implements McpEndpointMixin {
		private static final long serialVersionUID = 1L;
		@Override public McpServerConfig getMcpConfig() { return new McpServerConfig(); }
		@Override public McpRevision revision() { return BROKER_MIXIN_REVISION; }
		@Override public McpSubscriptionBroker subscriptionBroker() { return MIXIN_BROKER; }
	}

	private static MockRestClient client(Class<?> c) {
		return MockRestClient.create(c).json().contentType("application/json").accept("application/json").build();
	}

	@Test void a01_defaultServletHook_addsNoBrokerBeans() throws Exception {
		client(NoBrokerServlet.class).post("/")
			.contentString("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"anything\"}").run().assertStatus(200);
		assertFalse(NO_BROKER_SERVLET_REVISION.lastCtx.hasBean(McpSubscriptionBroker.class));
		assertFalse(NO_BROKER_SERVLET_REVISION.lastCtx.hasBean(McpSubscriptions.class));
	}

	@Test void a02_overriddenServletHook_addsBothBeanAliasesAsTheSameInstance() throws Exception {
		client(BrokerServlet.class).post("/")
			.contentString("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"anything\"}").run().assertStatus(200);
		var ctx = BROKER_SERVLET_REVISION.lastCtx;
		assertSame(SERVLET_BROKER, ctx.getBean(McpSubscriptionBroker.class).orElse(null));
		assertSame(SERVLET_BROKER, ctx.getBean(McpSubscriptions.class).orElse(null));
	}

	@Test void a03_defaultMixinHook_addsNoBrokerBeans() throws Exception {
		client(NoBrokerMixin.class).post("/mcp")
			.contentString("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"anything\"}").run().assertStatus(200);
		assertFalse(NO_BROKER_MIXIN_REVISION.lastCtx.hasBean(McpSubscriptionBroker.class));
		assertFalse(NO_BROKER_MIXIN_REVISION.lastCtx.hasBean(McpSubscriptions.class));
	}

	@Test void a04_overriddenMixinHook_addsBothBeanAliasesAsTheSameInstance() throws Exception {
		client(BrokerMixin.class).post("/mcp")
			.contentString("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"anything\"}").run().assertStatus(200);
		var ctx = BROKER_MIXIN_REVISION.lastCtx;
		assertSame(MIXIN_BROKER, ctx.getBean(McpSubscriptionBroker.class).orElse(null));
		assertSame(MIXIN_BROKER, ctx.getBean(McpSubscriptions.class).orElse(null));
	}

	@Test void a05_servletDefaultHook_returnsNull() {
		assertNull(new NoBrokerServlet().getSubscriptionBroker());
	}

	@Test void a06_mixinDefaultHook_returnsNull() {
		assertNull(new NoBrokerMixin().subscriptionBroker());
	}
}
