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
package org.apache.juneau.rest.client.mcp;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.*;

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.commons.inject.BasicBeanStore;
import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;

class McpDuplexDispatcher_Test {

	@Test
	void a01_dispatch_passesRawRequestWithoutTypedProjection() {
		var d = new McpDuplexDispatcher();
		var seenReq = new AtomicReference<JsonRpcRequest>();
		var seenParamsType = new AtomicReference<Class<?>>();
		d.register((request, ctx) -> {
			seenReq.set(request);
			seenParamsType.set(request.getParams().getClass());
			return JsonMap.of("ok", true);
		});

		var params = JsonMap.of(
			"name", "opaque-tool",
			"arguments", JsonMap.of("k1", "v1"),
			"_meta", JsonMap.of("traceparent", "00-abc-def-01"));
		var req = new JsonRpcRequest().setJsonrpc("2.0").setId("9").setMethod("sampling/createMessage").setParams(params);
		var out = d.dispatch(req, BasicBeanStore.INSTANCE);

		assertSame(req, seenReq.get(), "dispatcher must hand the original request object through");
		assertEquals(JsonMap.class, seenParamsType.get(), "params must stay generic JSON map, not typed beans");
		assertEquals(JsonMap.of("ok", true), out);
	}

	@Test
	void a02_dispatch_doesNotMutateInboundPayload() {
		var d = new McpDuplexDispatcher();
		d.register((request, ctx) -> JsonMap.of("ack", true));

		var params = JsonMap.of(
			"name", "opaque",
			"nested", JsonMap.of("a", 1, "b", JsonList.of(2, 3)),
			"experimental", JsonMap.of("x-key", "x-value"));
		var before = params.toString();
		var req = new JsonRpcRequest().setJsonrpc("2.0").setId("44").setMethod("elicitation/create").setParams(params);
		d.dispatch(req, BasicBeanStore.INSTANCE);
		var after = req.getParams().toString();

		assertEquals(before, after, "payload must be byte-equivalent through dispatch");
	}

	@Test
	void a03_noHandler_returnsNullForNotifications() {
		var d = new McpDuplexDispatcher();
		var req = new JsonRpcRequest().setJsonrpc("2.0").setMethod("notifications/message").setParams(JsonMap.of("k", "v"));
		assertNull(d.dispatch(req, BasicBeanStore.INSTANCE));
	}

	@Test
	void a04_noHandler_forRequests_throwsMcpException() {
		var d = new McpDuplexDispatcher();
		var req = new JsonRpcRequest().setJsonrpc("2.0").setId("7").setMethod("sampling/createMessage").setParams(JsonMap.of());
		var e = assertThrows(McpException.class, () -> d.dispatch(req, BasicBeanStore.INSTANCE));
		assertEquals(-32601, e.getCode());
	}

	@Test
	void a05_handlerThrowsGenericException_wrapsAsInternalError() {
		var d = new McpDuplexDispatcher();
		d.register((request, ctx) -> {
			throw new IllegalStateException("boom");
		});
		var req = new JsonRpcRequest().setJsonrpc("2.0").setId("8").setMethod("sampling/createMessage").setParams(JsonMap.of());
		var e = assertThrows(McpException.class, () -> d.dispatch(req, BasicBeanStore.INSTANCE));
		assertEquals(-32603, e.getCode());
		assertNotNull(e.getMessage());
		assertTrue(e.getMessage().contains("sampling/createMessage"));
		assertInstanceOf(IllegalStateException.class, e.getCause());
	}

	@Test
	void a06_dispatch_nullRequest_throwsIllegalArgumentException() {
		var d = new McpDuplexDispatcher();
		var e = assertThrows(IllegalArgumentException.class, () -> d.dispatch(null, BasicBeanStore.INSTANCE));
		assertEquals("Argument 'request' cannot be null.", e.getMessage());
	}
}
