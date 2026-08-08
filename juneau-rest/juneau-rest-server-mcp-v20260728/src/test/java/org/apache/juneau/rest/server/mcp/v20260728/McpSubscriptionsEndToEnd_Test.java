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
package org.apache.juneau.rest.server.mcp.v20260728;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.mcp.BasicMcpSubscriptionBroker;
import org.apache.juneau.rest.server.mcp.McpChangeKind;
import org.apache.juneau.rest.server.mcp.McpExchange;
import org.apache.juneau.rest.server.mcp.McpResponseResult;
import org.apache.juneau.rest.server.mcp.McpServerConfig;
import org.apache.juneau.rest.server.mcp.McpSubscription;
import org.apache.juneau.rest.server.mcp.McpSubscriptionBroker;
import org.apache.juneau.rest.server.mcp.McpSubscriptionFilter;
import org.apache.juneau.rest.server.mcp.McpSubscriptions;
import org.apache.juneau.rest.server.mcp.McpToolHandler;
import org.apache.juneau.rest.server.mcp.McpToolOutcome;
import org.apache.juneau.rest.server.mcp.McpToolSpec;
import org.junit.jupiter.api.*;

/**
 * Proves Phase 1 (neutral broker) and Phase 3 (BeanStore wiring) compose correctly through a REAL v2
 * {@link McpRevision} dispatch (not a fake): a tool handler registered on an ordinary
 * {@link McpServerConfig} can fetch the injected {@link McpSubscriptionBroker}/{@link McpSubscriptions}
 * beans from its {@link BeanStore} argument, and a change published by one handler invocation reaches a
 * subscription registered by an earlier invocation sharing the same request-scoped {@link BeanStore}.
 */
class McpSubscriptionsEndToEnd_Test {

	private static Object validMeta() {
		return JsonMap.of(
			RequestMeta.KEY_PROTOCOL_VERSION, "2026-07-28",
			RequestMeta.KEY_CLIENT_CAPABILITIES, JsonMap.of());
	}

	private static JsonRpcRequest toolCallRequest(String toolName, Map<String,Object> arguments) {
		var params = JsonMap.of("name", toolName, "arguments", arguments);
		params.put("_meta", validMeta());
		return new JsonRpcRequest().setJsonrpc(McpProtocol.JSON_RPC_2_0).setId(1).setMethod("tools/call")
			.setParams(params);
	}

	private static Map<String,String> toolCallHeaders(String toolName) {
		return Map.of("Mcp-Method", "tools/call", "Mcp-Name", toolName);
	}

	@SuppressWarnings({
		"resource" // Each .addBean(...) link returns 'this' (a Closeable) already owned by the enclosing try-with-resources; Eclipse JDT @Owning warning on the intermediate chain links is by design.
	})
	@Test void a01_toolHandler_publishReachesSubscriptionRegisteredThroughTheSameRequestScopedBroker() throws Exception {
		var broker = new BasicMcpSubscriptionBroker(4);
		var capturedSub = new AtomicReference<McpSubscription>();

		var subscribeTool = new McpToolHandler() {
			@Override public McpToolSpec descriptor() { return new McpToolSpec().setName("subscribe"); }
			@Override public McpToolOutcome call(Map<String,Object> arguments, BeanStore ctx) {
				var b = ctx.getBean(McpSubscriptionBroker.class)
					.orElseThrow(() -> new IllegalStateException("McpSubscriptionBroker bean missing from request BeanStore"));
				var filter = new McpSubscriptionFilter(false, false, false, Set.of(String.valueOf(arguments.get("uri"))));
				capturedSub.set(b.register("sub-1", filter));
				return McpToolOutcome.text("subscribed");
			}
		};
		var touchTool = new McpToolHandler() {
			@Override public McpToolSpec descriptor() { return new McpToolSpec().setName("touch"); }
			@Override public McpToolOutcome call(Map<String,Object> arguments, BeanStore ctx) {
				var subs = ctx.getBean(McpSubscriptions.class)
					.orElseThrow(() -> new IllegalStateException("McpSubscriptions bean missing from request BeanStore"));
				subs.resourceUpdated(String.valueOf(arguments.get("uri")));
				return McpToolOutcome.text("published");
			}
		};

		var config = new McpServerConfig().setName("test").setVersion("1.0.0")
			.addTool(subscribeTool).addTool(touchTool);
		var rev = new McpRevision(null, new McpCacheConfig(), null);
		try (var ctx = new BasicBeanStore()
				.addBean(McpSubscriptionBroker.class, broker)
				.addBean(McpSubscriptions.class, broker)) {

			var subscribeResult = rev.dispatch(
				new McpExchange(toolCallRequest("subscribe", Map.of("uri", "file:///a.txt")), toolCallHeaders("subscribe")::get),
				config, ctx);
			assertInstanceOf(McpResponseResult.class, subscribeResult);
			assertNull(((McpResponseResult) subscribeResult).response().getError());
			var sub = capturedSub.get();
			assertNotNull(sub);
			assertEquals(1, broker.activeCount());
			try {
				var touchResult = rev.dispatch(
					new McpExchange(toolCallRequest("touch", Map.of("uri", "file:///a.txt")), toolCallHeaders("touch")::get),
					config, ctx);
				assertInstanceOf(McpResponseResult.class, touchResult);
				assertNull(((McpResponseResult) touchResult).response().getError());

				var event = sub.take();
				assertEquals(McpChangeKind.RESOURCE_UPDATED, event.kind());
				assertEquals("file:///a.txt", event.resourceUri());
			} finally {
				sub.close();
			}
		}
	}
}
